"""REST API Routes — FastAPI endpoints for VectorMinds.

Provides full programmatic access to all platform capabilities:
trends, blueprints, pipelines, ingestion, stats, and vector map.
Includes WebSocket for real-time dashboard updates.
"""

from __future__ import annotations

import asyncio
import json
import logging
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query, HTTPException, Header
from pydantic import BaseModel

from agents.ingestion_agent import IngestionAgent
from agents.reasoning_agent import ReasoningAgent
from agents.memory_agent import MemoryAgent
from embeddings.engine import EmbeddingEngine
from embeddings.vector_store import VectorStore
from intelligence.blueprint_engine import BlueprintEngine
from intelligence.experiment_designer import ExperimentDesigner
from intelligence.pipeline_generator import PipelineGenerator
from intelligence.pipeline_executor import PipelineExecutor
from delivery.telegram_bot import TelegramBot
from db.database import Database
import config

logger = logging.getLogger("vectorminds.api")

router = APIRouter(prefix="/api")

# ─── Global instances (set by main.py on startup) ────────────
ingestion_agent: Optional[IngestionAgent] = None
reasoning_agent: Optional[ReasoningAgent] = None
memory_agent: Optional[MemoryAgent] = None
blueprint_engine: Optional[BlueprintEngine] = None
pipeline_generator: Optional[PipelineGenerator] = None
pipeline_executor: Optional[PipelineExecutor] = None
experiment_designer: Optional[ExperimentDesigner] = None
telegram_bot: Optional[TelegramBot] = None
embedding_engine: Optional[EmbeddingEngine] = None
vector_store: Optional[VectorStore] = None
database: Optional[Database] = None

# WebSocket connections for live updates
ws_connections: list[WebSocket] = []


# ─── Request / Response Models ────────────────────────────────
class IngestRequest(BaseModel):
    source: str = "all"  # 'arxiv', 'github', or 'all'
    category: Optional[str] = None  # e.g. 'cs.LG'
    # When True, kick the ingestion run off in the background and return
    # immediately with status="started". Lets mobile clients show a banner
    # without holding open a 60–120s HTTP request that arXiv often pushes
    # past their OkHttp read timeout.
    background: bool = False


# In-memory tracker for the most recent ingestion run. Keeps the UI honest
# about what's happening on the server even when the call is fire-and-forget.
_ingestion_status: dict = {
    "state": "idle",  # 'idle' | 'running' | 'completed' | 'failed'
    "started_at": None,
    "finished_at": None,
    "signals_ingested": 0,
    "trends_updated": 0,
    "error": None,
}


class BlueprintRequest(BaseModel):
    trend_id: str
    additional_context: str = ""


class PipelineRequest(BaseModel):
    technique_name: str
    description: str = ""
    task_type: Optional[str] = None

class PipelineDatasetCandidatesRequest(BaseModel):
    technique_name: str
    description: str = ""
    task_type: Optional[str] = None
    top_k: int = 8


class PipelineRunRequest(BaseModel):
    timeout_seconds: int = config.PIPELINE_RUN_TIMEOUT_SECONDS
    wait_for_completion: bool = False

class ExperimentDesignRequest(BaseModel):
    technique_name: str
    brief: str = ""

class DashboardPremiumContextResponse(BaseModel):
    location: str
    focus: str
    next_meeting: str
    author_name: str
    papers_count: int
    confidence: float
    reasoning_points: list[str]
    source_modes: dict


class FeedbackRequest(BaseModel):
    target_id: str
    target_type: str = "trend"  # 'trend' or 'blueprint'
    action: str = "upvote"  # 'upvote' or 'downvote'


class SearchRequest(BaseModel):
    query: str
    top_k: int = 10
    source_filter: Optional[str] = None


def _assert_admin_api_key(x_api_key: Optional[str]):
    if not config.API_ADMIN_KEY:
        return
    if x_api_key != config.API_ADMIN_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")


def _apply_run_snapshot_to_pipeline(pipeline, run: dict):
    if run["status"] in ("completed", "failed", "timeout"):
        pipeline.status = run["status"]
    else:
        pipeline.status = "training"
    metrics = dict(pipeline.metrics or {})
    metrics["last_run"] = {
        "run_id": run["run_id"],
        "status": run["status"],
        "started_at": run.get("started_at"),
        "finished_at": run.get("finished_at"),
        "exit_code": run.get("exit_code"),
        "duration_seconds": run.get("duration_seconds"),
        "log_path": run.get("log_path"),
        "artifacts_dir": run.get("artifacts_dir"),
        "retry_count": run.get("retry_count", 0),
        "max_retries": run.get("max_retries", 0),
    }
    pipeline.metrics = metrics


# ─── Broadcast helper ────────────────────────────────────────
async def broadcast_ws(event_type: str, data: dict):
    """Send real-time update to all connected WebSocket clients."""
    message = json.dumps({"type": event_type, "data": data, "timestamp": datetime.now(timezone.utc).isoformat()})
    disconnected = []
    for ws in ws_connections:
        try:
            await ws.send_text(message)
        except Exception:
            disconnected.append(ws)
    for ws in disconnected:
        ws_connections.remove(ws)


# ─── Endpoints ────────────────────────────────────────────────

@router.get("/health")
async def health_check():
    """Platform health check."""
    agents_health = {}
    if ingestion_agent:
        agents_health["ingestion"] = ingestion_agent.get_health()
    if reasoning_agent:
        agents_health["reasoning"] = reasoning_agent.get_health()
    if memory_agent:
        agents_health["memory"] = memory_agent.get_health()

    return {
        "status": "healthy",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "agents": agents_health,
        "vector_store_count": vector_store.get_collection_count() if vector_store else 0,
        "infra": {
            "event_bus_backend": config.MESSAGE_BUS_BACKEND,
            "state_store_backend": config.STATE_STORE_BACKEND,
            "db_backend": config.DB_BACKEND,
            "vector_store_backend": "qdrant_in_memory" if not config.QDRANT_HOST else "qdrant_remote",
        },
    }


@router.get("/stats")
async def get_stats():
    """Get platform statistics for dashboard."""
    vs_count = vector_store.get_collection_count() if vector_store else 0
    db_papers = database.get_signals_by_source("arxiv") if database else 0
    db_repos = database.get_signals_by_source("github") if database else 0

    # Get novelty distribution from stored signals
    payloads = vector_store.get_all_payloads(limit=200) if vector_store else []
    novelty_scores = [p.get("novelty_score", 0) for p in payloads]

    return {
        "total_signals": vs_count,
        "total_papers": db_papers,
        "total_github_repos": db_repos,
        "active_trends": len(reasoning_agent.trends) if reasoning_agent else 0,
        "blueprints_generated": len(blueprint_engine.generated_blueprints) if blueprint_engine else 0,
        "pipelines_launched": len(pipeline_generator.generated_pipelines) if pipeline_generator else 0,
        "avg_novelty_score": round(sum(novelty_scores) / max(len(novelty_scores), 1), 3),
        "novelty_distribution": novelty_scores[:100],
        "agents_status": {
            "ingestion": ingestion_agent.status if ingestion_agent else "offline",
            "reasoning": reasoning_agent.status if reasoning_agent else "offline",
            "memory": memory_agent.status if memory_agent else "offline",
        },
        "source_modes": {
            "patents_real": config.ENABLE_PATENTS_REAL,
            "startups_real": config.ENABLE_STARTUPS_REAL,
            "social_real": config.ENABLE_SOCIAL_REAL,
            "blog_real": config.ENABLE_BLOG_REAL,
            "allow_simulated_fallback": config.ALLOW_SIMULATED_SOURCES,
        },
        "telegram": telegram_bot.get_stats() if telegram_bot else {},
        "last_updated": datetime.now(timezone.utc).isoformat(),
    }

@router.get("/dashboard/premium-context", response_model=DashboardPremiumContextResponse)
async def get_dashboard_premium_context():
    """Backend-derived context for premium dashboard panels."""
    trends = []
    if reasoning_agent and reasoning_agent.trends:
        trends = sorted(
            reasoning_agent.trends.values(),
            key=lambda t: t.emergence_score,
            reverse=True,
        )
    elif reasoning_agent:
        trends = await reasoning_agent.analyze_trends()

    top = trends[0] if trends else None
    technique = top.technique_name if top else "Autonomous Research Discovery"
    papers = top.paper_count if top else 0
    confidence = float(top.confidence if top else 0.74)

    location = (
        "Distributed Lab (Cloud + Device)"
        if config.STATE_STORE_BACKEND == "redis"
        else "Local Research Runtime"
    )
    next_meeting = f"Trend Review: {technique} ({'6' if top and top.mainstream_eta_months <= 6 else '12/24'} month horizon)"
    reasoning_points = [
        f"Top ranked technique is '{technique}' from live trend analysis",
        f"Cross-source evidence includes {papers} papers and {top.github_stars if top else 0} GitHub stars",
        f"Current backend mode: DB={config.DB_BACKEND}, Bus={config.MESSAGE_BUS_BACKEND}, State={config.STATE_STORE_BACKEND}",
    ]
    source_modes = {
        "patents_real": config.ENABLE_PATENTS_REAL,
        "startups_real": config.ENABLE_STARTUPS_REAL,
        "social_real": config.ENABLE_SOCIAL_REAL,
        "blog_real": config.ENABLE_BLOG_REAL,
        "allow_simulated_fallback": config.ALLOW_SIMULATED_SOURCES,
    }
    return DashboardPremiumContextResponse(
        location=location,
        focus=technique,
        next_meeting=next_meeting,
        author_name="Top Signal Cluster",
        papers_count=papers,
        confidence=confidence,
        reasoning_points=reasoning_points,
        source_modes=source_modes,
    )


async def _run_ingestion_pipeline(req: IngestRequest) -> dict:
    """Execute the full ingest → analyze → broadcast pipeline.

    Returns a result dict regardless of whether it was awaited inline or
    scheduled as a background task. Updates the shared `_ingestion_status`
    so polling clients can observe progress.
    """
    global _ingestion_status
    _ingestion_status = {
        "state": "running",
        "started_at": datetime.now(timezone.utc).isoformat(),
        "finished_at": None,
        "signals_ingested": 0,
        "trends_updated": 0,
        "error": None,
    }

    try:
        signals = await ingestion_agent.run_ingestion(
            source=req.source, category=req.category
        )

        if database:
            for s in signals:
                database.save_signal(s.model_dump(mode="json"))

        trends_count = 0
        if reasoning_agent:
            trends = await reasoning_agent.analyze_trends()
            trends_count = len(reasoning_agent.trends)
            if database:
                for t in trends:
                    database.save_trend(t.model_dump(mode="json"))
            if telegram_bot:
                for t in trends[:3]:
                    if t.impact_score >= config.IMPACT_HIGH_THRESHOLD:
                        await telegram_bot.send_trend_alert(
                            technique=t.technique_name,
                            score=t.emergence_score,
                            eta=t.mainstream_eta_months,
                        )

        await broadcast_ws("ingestion_complete", {
            "count": len(signals),
            "source": req.source,
            "signals": [
                {
                    "id": s.id,
                    "source": s.source.value,
                    "title": s.title,
                    "novelty_score": s.novelty_score,
                    "url": s.url,
                }
                for s in signals[:20]
            ],
        })

        if telegram_bot:
            arxiv_count = sum(1 for s in signals if s.source.value == "arxiv")
            github_count = sum(1 for s in signals if s.source.value == "github")
            await telegram_bot.send_ingestion_summary(arxiv_count, github_count)

        result = {
            "status": "success",
            "signals_ingested": len(signals),
            "trends_updated": trends_count,
        }
        _ingestion_status = {
            "state": "completed",
            "started_at": _ingestion_status["started_at"],
            "finished_at": datetime.now(timezone.utc).isoformat(),
            "signals_ingested": len(signals),
            "trends_updated": trends_count,
            "error": None,
        }
        return result
    except Exception as e:
        logger.exception("Ingestion pipeline failed")
        _ingestion_status = {
            "state": "failed",
            "started_at": _ingestion_status["started_at"],
            "finished_at": datetime.now(timezone.utc).isoformat(),
            "signals_ingested": 0,
            "trends_updated": 0,
            "error": str(e),
        }
        raise


@router.post("/ingest")
async def trigger_ingestion(req: IngestRequest):
    """Trigger a manual ingestion run.

    With `background=True`, schedules the run on the event loop and returns
    immediately with status="started" so the mobile UI can show a banner and
    poll `/api/ingest/status` for completion. Otherwise runs inline (used by
    integration tests and curl smoke tests).
    """
    if not ingestion_agent:
        raise HTTPException(status_code=503, detail="Ingestion agent not ready")

    if req.background:
        if _ingestion_status.get("state") == "running":
            return {
                "status": "already_running",
                "started_at": _ingestion_status.get("started_at"),
            }
        asyncio.create_task(_run_ingestion_pipeline(req))
        return {
            "status": "started",
            "message": "Ingestion is running in the background. Poll /api/ingest/status.",
        }

    return await _run_ingestion_pipeline(req)


@router.get("/ingest/status")
async def get_ingestion_status():
    """Return the state of the last/in-flight ingestion run."""
    return _ingestion_status


@router.get("/trends")
async def get_trends(limit: int = Query(default=20, le=100)):
    """Get the trend leaderboard."""
    if not reasoning_agent or not reasoning_agent.trends:
        # Run analysis if no trends exist yet
        if reasoning_agent:
            trends = await reasoning_agent.analyze_trends()
        else:
            return {"trends": [], "count": 0}
    else:
        trends = sorted(
            reasoning_agent.trends.values(),
            key=lambda t: t.emergence_score,
            reverse=True,
        )

    trend_list = [t.model_dump(mode="json") for t in trends[:limit]]
    return {"trends": trend_list, "count": len(trend_list)}


@router.get("/trends/{trend_id}")
async def get_trend_detail(
    trend_id: str,
    include_brief: bool = Query(
        default=False,
        description="When true, runs Gemini to generate technical_brief (slow). "
        "Omit or false for instant scores + description.",
    ),
):
    """Get detailed view of a specific trend."""
    if not reasoning_agent:
        raise HTTPException(status_code=503, detail="Reasoning agent not ready")

    trend = reasoning_agent.trends.get(trend_id)
    if not trend:
        raise HTTPException(status_code=404, detail="Trend not found")

    result = trend.model_dump(mode="json")
    if include_brief:
        brief = await reasoning_agent.generate_technical_brief(
            trend.technique_name, trend.description
        )
        result["technical_brief"] = brief
    else:
        result["technical_brief"] = None
    return result


@router.post("/blueprints/generate")
async def generate_blueprint(req: BlueprintRequest):
    """Generate a product blueprint for a trend."""
    if not blueprint_engine or not reasoning_agent:
        raise HTTPException(status_code=503, detail="Services not ready")

    trend = reasoning_agent.trends.get(req.trend_id)
    if not trend:
        raise HTTPException(status_code=404, detail="Trend not found")

    blueprint = await blueprint_engine.generate_blueprint(
        trend, req.additional_context
    )

    # Store in memory agent
    if memory_agent:
        memory_agent.store_blueprint(blueprint.id, blueprint.model_dump(mode="json"))
    if database:
        try:
            database.save_blueprint(blueprint.model_dump(mode="json"))
        except Exception as e:
            logger.warning("blueprint persist failed: %s", e)

    await broadcast_ws("blueprint_generated", {
        "id": blueprint.id,
        "technique": blueprint.technique_name,
    })

    return blueprint.model_dump(mode="json")


@router.get("/blueprints")
async def list_blueprints():
    """List all generated blueprints."""
    if not blueprint_engine:
        return {"blueprints": [], "count": 0}
    bps = [b.model_dump(mode="json") for b in blueprint_engine.list_blueprints()]
    return {"blueprints": bps, "count": len(bps)}


@router.get("/blueprints/{blueprint_id}")
async def get_blueprint(blueprint_id: str):
    """Get a specific blueprint."""
    if not blueprint_engine:
        raise HTTPException(status_code=503, detail="Blueprint engine not ready")
    bp = blueprint_engine.get_blueprint(blueprint_id)
    if not bp:
        raise HTTPException(status_code=404, detail="Blueprint not found")
    return bp.model_dump(mode="json")


@router.post("/pipelines/generate")
async def generate_pipeline(req: PipelineRequest):
    """Generate an ML training pipeline."""
    if not pipeline_generator:
        raise HTTPException(status_code=503, detail="Pipeline generator not ready")

    pipeline = pipeline_generator.generate_pipeline(
        technique_name=req.technique_name,
        description=req.description,
        task_type=req.task_type,
    )

    if memory_agent:
        memory_agent.store_pipeline(pipeline.model_dump(mode="json"))
    if database:
        database.save_pipeline(pipeline.model_dump(mode="json"))

    await broadcast_ws("pipeline_generated", {
        "id": pipeline.id,
        "technique": pipeline.technique_name,
        "task_type": pipeline.task_type,
    })
    if telegram_bot:
        await telegram_bot.send_pipeline_complete(
            technique=pipeline.technique_name,
            task_type=pipeline.task_type,
            metrics=pipeline.metrics,
            colab_url=pipeline.colab_url,
        )

    return pipeline.model_dump(mode="json")


@router.post("/pipelines/{pipeline_id}/run")
async def run_pipeline(pipeline_id: str, req: PipelineRunRequest, x_api_key: Optional[str] = Header(default=None)):
    """Execute a generated pipeline script."""
    _assert_admin_api_key(x_api_key)
    if not pipeline_generator or not pipeline_executor:
        raise HTTPException(status_code=503, detail="Pipeline services not ready")

    pipeline = pipeline_generator.get_pipeline(pipeline_id)
    if not pipeline:
        raise HTTPException(status_code=404, detail="Pipeline not found")

    timeout = min(max(req.timeout_seconds, 30), 7200)
    pipeline.status = "training"
    pipeline_generator.update_pipeline(pipeline)
    if database:
        database.save_pipeline(pipeline.model_dump(mode="json"))

    if req.wait_for_completion:
        run = await pipeline_executor.execute_pipeline(pipeline, timeout_seconds=timeout)
    else:
        run = pipeline_executor.execute_pipeline_async(pipeline, timeout_seconds=timeout)

    _apply_run_snapshot_to_pipeline(pipeline, run)
    pipeline_generator.update_pipeline(pipeline)
    if database:
        database.save_pipeline(pipeline.model_dump(mode="json"))
        database.save_pipeline_run(run)

    await broadcast_ws("pipeline_run_started", {
        "pipeline_id": pipeline.id,
        "run_id": run["run_id"],
        "status": run["status"],
        "technique": pipeline.technique_name,
    })

    return {
        "status": "accepted" if run["status"] in ("queued", "running") else "finished",
        "pipeline": pipeline.model_dump(mode="json"),
        "run": run,
    }

@router.post("/pipelines/dataset-candidates")
async def pipeline_dataset_candidates(req: PipelineDatasetCandidatesRequest):
    """Preview ranked dataset candidates before pipeline generation."""
    if not pipeline_generator:
        raise HTTPException(status_code=503, detail="Pipeline generator not ready")
    candidates = pipeline_generator.dataset_candidates(
        technique_name=req.technique_name,
        description=req.description,
        task_type=req.task_type,
        top_k=min(max(req.top_k, 1), 20),
    )
    return {"candidates": candidates, "count": len(candidates)}

@router.post("/experiments/design")
async def design_experiment(req: ExperimentDesignRequest):
    """Generate a minimal viable experiment design for a technique."""
    if not experiment_designer:
        raise HTTPException(status_code=503, detail="Experiment designer not ready")
    exp = await experiment_designer.design_experiment(
        technique_name=req.technique_name,
        brief=req.brief,
    )
    return exp


@router.get("/pipelines")
async def list_pipelines():
    """List all generated pipelines."""
    if not pipeline_generator:
        return {"pipelines": [], "count": 0}
    pls = [p.model_dump(mode="json") for p in pipeline_generator.list_pipelines()]
    return {"pipelines": pls, "count": len(pls)}


@router.get("/pipelines/{pipeline_id}")
async def get_pipeline(pipeline_id: str):
    """Get a specific pipeline."""
    if not pipeline_generator:
        raise HTTPException(status_code=503, detail="Pipeline generator not ready")
    pl = pipeline_generator.get_pipeline(pipeline_id)
    if not pl:
        raise HTTPException(status_code=404, detail="Pipeline not found")
    return pl.model_dump(mode="json")


@router.get("/pipelines/{pipeline_id}/runs")
async def list_pipeline_runs(pipeline_id: str):
    """List runs for a pipeline."""
    if not pipeline_generator or not pipeline_executor:
        raise HTTPException(status_code=503, detail="Pipeline services not ready")
    pipeline = pipeline_generator.get_pipeline(pipeline_id)
    if not pipeline:
        raise HTTPException(status_code=404, detail="Pipeline not found")
    runs = pipeline_executor.list_runs(pipeline_id)
    if not runs and database:
        runs = database.get_pipeline_runs(pipeline_id)
    return {"pipeline_id": pipeline_id, "runs": runs, "count": len(runs)}


@router.get("/pipelines/{pipeline_id}/runs/{run_id}")
async def get_pipeline_run(pipeline_id: str, run_id: str):
    """Get run status for a specific pipeline run."""
    if not pipeline_generator or not pipeline_executor:
        raise HTTPException(status_code=503, detail="Pipeline services not ready")
    pipeline = pipeline_generator.get_pipeline(pipeline_id)
    if not pipeline:
        raise HTTPException(status_code=404, detail="Pipeline not found")

    run = pipeline_executor.get_run(pipeline_id, run_id)
    if not run and database:
        run = database.get_pipeline_run(run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Run not found")

    _apply_run_snapshot_to_pipeline(pipeline, run)
    pipeline_generator.update_pipeline(pipeline)
    if database:
        database.save_pipeline(pipeline.model_dump(mode="json"))
        database.save_pipeline_run(run)

    return run


@router.get("/pipelines/{pipeline_id}/runs/{run_id}/log")
async def get_pipeline_run_log(pipeline_id: str, run_id: str, tail_lines: int = Query(default=200, ge=10, le=2000)):
    """Read latest log lines for a pipeline run."""
    if not pipeline_generator or not pipeline_executor:
        raise HTTPException(status_code=503, detail="Pipeline services not ready")
    pipeline = pipeline_generator.get_pipeline(pipeline_id)
    if not pipeline:
        raise HTTPException(status_code=404, detail="Pipeline not found")
    run = pipeline_executor.get_run(pipeline_id, run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Run not found")

    try:
        with open(run["log_path"], "r", encoding="utf-8") as f:
            lines = f.readlines()
    except FileNotFoundError:
        lines = []

    sliced = lines[-tail_lines:]
    return {
        "pipeline_id": pipeline_id,
        "run_id": run_id,
        "status": run["status"],
        "line_count": len(sliced),
        "log_tail": "".join(sliced),
    }


@router.post("/search")
async def semantic_search(req: SearchRequest):
    """Semantic search across all research signals."""
    if not embedding_engine or not vector_store:
        raise HTTPException(status_code=503, detail="Search not ready")

    query_embedding = embedding_engine.embed_text(req.query)
    results = vector_store.search(
        query_vector=query_embedding,
        top_k=req.top_k,
        source_filter=req.source_filter,
    )
    return {"results": results, "count": len(results), "query": req.query}


@router.get("/vector-map")
async def get_vector_map(limit: int = Query(default=200, le=500)):
    """Get 2D projection of vector space for visualization."""
    if not vector_store:
        return {"points": [], "count": 0}

    vectors, payloads = vector_store.get_vectors_for_projection(limit=limit)
    if not vectors:
        return {"points": [], "count": 0}

    # Simple 2D projection using PCA (fast for demo)
    import numpy as np
    from sklearn.decomposition import PCA

    vecs = np.array(vectors)
    if len(vecs) < 2:
        return {"points": [], "count": 0}

    n_components = min(2, len(vecs), vecs.shape[1])
    pca = PCA(n_components=n_components)
    projected = pca.fit_transform(vecs)

    points = []
    for i, (coords, payload) in enumerate(zip(projected, payloads)):
        points.append({
            "x": float(coords[0]) if len(coords) > 0 else 0,
            "y": float(coords[1]) if len(coords) > 1 else 0,
            "title": payload.get("title", ""),
            "source": payload.get("source", ""),
            "novelty_score": payload.get("novelty_score", 0),
            "categories": payload.get("categories", []),
        })

    return {
        "points": points,
        "count": len(points),
        "explained_variance": pca.explained_variance_ratio_.tolist(),
    }


@router.post("/feedback")
async def submit_feedback(req: FeedbackRequest):
    """Submit user feedback (upvote/downvote) on a prediction."""
    import uuid

    feedback_data = {
        "id": str(uuid.uuid4()),
        "target_id": req.target_id,
        "target_type": req.target_type,
        "action": req.action,
    }

    if database:
        database.save_feedback(feedback_data)

    if memory_agent:
        await memory_agent.bus.publish_simple(
            "delivery.feedback", "api", feedback_data
        )

    return {"status": "recorded", "feedback": feedback_data}


# ─── WebSocket for Live Updates ───────────────────────────────
@router.websocket("/ws/live")
async def websocket_live(ws: WebSocket):
    """WebSocket endpoint for real-time dashboard updates."""
    await ws.accept()
    ws_connections.append(ws)
    logger.info(f"WebSocket client connected (total: {len(ws_connections)})")

    try:
        while True:
            # Keep connection alive, handle incoming messages
            data = await ws.receive_text()
            # Echo or handle client commands
            if data == "ping":
                await ws.send_text(json.dumps({"type": "pong"}))
    except WebSocketDisconnect:
        ws_connections.remove(ws)
        logger.info(f"WebSocket client disconnected (total: {len(ws_connections)})")
    except Exception:
        if ws in ws_connections:
            ws_connections.remove(ws)
