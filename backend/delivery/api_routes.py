"""REST API Routes — FastAPI endpoints for VectorMinds.

Provides full programmatic access to all platform capabilities:
trends, blueprints, pipelines, ingestion, stats, and vector map.
Includes WebSocket for real-time dashboard updates.
"""

from __future__ import annotations

import asyncio
import json
import logging
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query, HTTPException
from pydantic import BaseModel

from agents.ingestion_agent import IngestionAgent
from agents.reasoning_agent import ReasoningAgent
from agents.memory_agent import MemoryAgent
from embeddings.engine import EmbeddingEngine
from embeddings.vector_store import VectorStore
from intelligence.blueprint_engine import BlueprintEngine
from intelligence.pipeline_generator import PipelineGenerator
from delivery.telegram_bot import TelegramBot
from db.database import Database

logger = logging.getLogger("vectorminds.api")

router = APIRouter(prefix="/api")

# ─── Global instances (set by main.py on startup) ────────────
ingestion_agent: Optional[IngestionAgent] = None
reasoning_agent: Optional[ReasoningAgent] = None
memory_agent: Optional[MemoryAgent] = None
blueprint_engine: Optional[BlueprintEngine] = None
pipeline_generator: Optional[PipelineGenerator] = None
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


class BlueprintRequest(BaseModel):
    trend_id: str
    additional_context: str = ""


class PipelineRequest(BaseModel):
    technique_name: str
    description: str = ""
    task_type: Optional[str] = None


class FeedbackRequest(BaseModel):
    target_id: str
    target_type: str = "trend"  # 'trend' or 'blueprint'
    action: str = "upvote"  # 'upvote' or 'downvote'


class SearchRequest(BaseModel):
    query: str
    top_k: int = 10
    source_filter: Optional[str] = None


# ─── Broadcast helper ────────────────────────────────────────
async def broadcast_ws(event_type: str, data: dict):
    """Send real-time update to all connected WebSocket clients."""
    message = json.dumps({"type": event_type, "data": data, "timestamp": datetime.utcnow().isoformat()})
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
        "timestamp": datetime.utcnow().isoformat(),
        "agents": agents_health,
        "vector_store_count": vector_store.get_collection_count() if vector_store else 0,
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
        "telegram": telegram_bot.get_stats() if telegram_bot else {},
        "last_updated": datetime.utcnow().isoformat(),
    }


@router.post("/ingest")
async def trigger_ingestion(req: IngestRequest):
    """Trigger a manual ingestion run."""
    if not ingestion_agent:
        raise HTTPException(status_code=503, detail="Ingestion agent not ready")

    # Run ingestion
    signals = await ingestion_agent.run_ingestion(
        source=req.source, category=req.category
    )

    # Save to database
    if database:
        for s in signals:
            database.save_signal(s.model_dump(mode="json"))

    # Run trend analysis after ingestion
    if reasoning_agent:
        trends = await reasoning_agent.analyze_trends()
        if database:
            for t in trends:
                database.save_trend(t.model_dump(mode="json"))

    # Broadcast to WebSocket clients
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

    # Send Telegram notification
    if telegram_bot:
        arxiv_count = sum(1 for s in signals if s.source.value == "arxiv")
        github_count = sum(1 for s in signals if s.source.value == "github")
        await telegram_bot.send_ingestion_summary(arxiv_count, github_count)

    return {
        "status": "success",
        "signals_ingested": len(signals),
        "trends_updated": len(reasoning_agent.trends) if reasoning_agent else 0,
    }


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
async def get_trend_detail(trend_id: str):
    """Get detailed view of a specific trend."""
    if not reasoning_agent:
        raise HTTPException(status_code=503, detail="Reasoning agent not ready")

    trend = reasoning_agent.trends.get(trend_id)
    if not trend:
        raise HTTPException(status_code=404, detail="Trend not found")

    # Generate technical brief
    brief = await reasoning_agent.generate_technical_brief(
        trend.technique_name, trend.description
    )

    result = trend.model_dump(mode="json")
    result["technical_brief"] = brief
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

    await broadcast_ws("pipeline_generated", {
        "id": pipeline.id,
        "technique": pipeline.technique_name,
        "task_type": pipeline.task_type,
    })

    return pipeline.model_dump(mode="json")


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
