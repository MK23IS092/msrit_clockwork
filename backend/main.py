"""VectorMind — GenAI Research Intelligence Platform.

Main FastAPI application entry point.
Initializes all subsystems: vector store, embedding engine, agents,
intelligence engines, and delivery layer.
"""

from __future__ import annotations

import asyncio
import logging
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

import config
from agents.ingestion_agent import IngestionAgent
from agents.reasoning_agent import ReasoningAgent
from agents.memory_agent import MemoryAgent
from agents.retraining_agent import RetrainingAgent
from agents.message_bus import MessageBus
from embeddings.engine import EmbeddingEngine
from embeddings.vector_store import VectorStore
from intelligence.blueprint_engine import BlueprintEngine
from intelligence.experiment_designer import ExperimentDesigner
from intelligence.pipeline_generator import PipelineGenerator
from intelligence.pipeline_executor import PipelineExecutor
from delivery.telegram_bot import TelegramBot
from delivery import api_routes
from db.database import Database

# ─── Logging ──────────────────────────────────────────────────
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(name)-30s | %(levelname)-7s | %(message)s",
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)
logger = logging.getLogger("vectormind")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan — startup and shutdown."""
    logger.info("=" * 60)
    logger.info("  VectorMind — GenAI Research Intelligence Platform")
    logger.info("  Powered by OpenClaw Multi-Agent Orchestration")
    logger.info("=" * 60)

    # ── 1. Database ──────────────────────────────
    logger.info("Initializing database...")
    db = Database.get_instance()
    db.initialize()
    api_routes.database = db

    # ── 2. Embedding Engine ──────────────────────
    logger.info("Loading embedding model...")
    embed_engine = EmbeddingEngine.get_instance()
    embed_engine.load_model()
    api_routes.embedding_engine = embed_engine

    # ── 3. Vector Store ──────────────────────────
    logger.info("Initializing vector store...")
    vs = VectorStore.get_instance()
    vs.initialize()
    api_routes.vector_store = vs

    # ── 4. Message Bus ───────────────────────────
    logger.info("Starting message bus...")
    bus = MessageBus.get_instance()
    await bus.start()

    # ── 5. Agents ────────────────────────────────
    logger.info("Starting agents...")

    ing_agent = IngestionAgent()
    reas_agent = ReasoningAgent()
    mem_agent = MemoryAgent()
    retrain_agent = RetrainingAgent()

    await ing_agent.start()
    await reas_agent.start()
    await mem_agent.start()
    await retrain_agent.start()

    api_routes.ingestion_agent = ing_agent
    api_routes.reasoning_agent = reas_agent
    api_routes.memory_agent = mem_agent
    api_routes.retraining_agent = retrain_agent

    # ── 6. Intelligence Engines ──────────────────
    logger.info("Initializing intelligence engines...")
    bp_engine = BlueprintEngine()
    pl_generator = PipelineGenerator()

    async def _on_pipeline_run_state_change(run_data: dict):
        if api_routes.database:
            api_routes.database.save_pipeline_run(run_data)
        pipeline_id = run_data.get("pipeline_id", "")
        if pipeline_id and api_routes.pipeline_generator:
            pipeline = api_routes.pipeline_generator.get_pipeline(pipeline_id)
            if pipeline:
                if run_data.get("status") in ("completed", "failed", "timeout"):
                    pipeline.status = run_data["status"]
                else:
                    pipeline.status = "training"
                metrics = dict(pipeline.metrics or {})
                metrics["last_run"] = {
                    "run_id": run_data.get("run_id"),
                    "status": run_data.get("status"),
                    "started_at": run_data.get("started_at"),
                    "finished_at": run_data.get("finished_at"),
                    "exit_code": run_data.get("exit_code"),
                    "duration_seconds": run_data.get("duration_seconds"),
                    "log_path": run_data.get("log_path"),
                    "artifacts_dir": run_data.get("artifacts_dir"),
                    "retry_count": run_data.get("retry_count", 0),
                    "max_retries": run_data.get("max_retries", 0),
                }
                pipeline.metrics = metrics
                api_routes.pipeline_generator.update_pipeline(pipeline)
                if api_routes.database:
                    api_routes.database.save_pipeline(pipeline.model_dump(mode="json"))
        await api_routes.broadcast_ws("pipeline_run_status", run_data)

    pl_executor = PipelineExecutor(on_state_change=_on_pipeline_run_state_change)
    exp_designer = ExperimentDesigner()

    api_routes.blueprint_engine = bp_engine
    api_routes.pipeline_generator = pl_generator
    api_routes.pipeline_executor = pl_executor
    api_routes.experiment_designer = exp_designer

    # ── 6b. Hydrate persisted state from DB ──
    # Signals → re-embed and upsert into Qdrant (in-memory) so semantic search
    #   and novelty scoring work after a restart.
    # Trends  → repopulate ReasoningAgent.trends so the leaderboard isn't empty
    #   until the next ingestion run.
    # Blueprints / pipelines → restore in-memory caches.
    try:
        from ingestion.schema import ProductBlueprint, MLPipeline, TrendEntry

        # ── Signals ──────────────────────────────────────────
        try:
            persisted_signals = db.list_signals(limit=2000)
        except Exception as e:
            logger.warning("list_signals failed: %s", e)
            persisted_signals = []

        rehydrated_signal_count = 0
        if persisted_signals:
            ids: list[str] = []
            texts: list[str] = []
            payloads: list[dict] = []
            for s in persisted_signals:
                sid = s.get("id") or ""
                text = s.get("raw_text") or s.get("title") or ""
                if not sid or not text:
                    continue
                ids.append(sid)
                texts.append(text)
                payloads.append(
                    {
                        "id": sid,
                        "title": s.get("title", ""),
                        "source": s.get("source", ""),
                        "url": s.get("url", ""),
                        "categories": s.get("categories", []),
                        "novelty_score": float(s.get("novelty_score") or 0.0),
                        "impact_score": float(s.get("impact_score") or 0.0),
                        "metadata": s.get("metadata", {}),
                    }
                )
            if ids:
                try:
                    embeddings = embed_engine.embed_batch(texts)
                    vs.upsert_batch(ids=ids, embeddings=embeddings, payloads=payloads)
                    rehydrated_signal_count = len(ids)
                except Exception as e:
                    logger.warning("Signal re-embedding failed: %s", e)

        # ── Trends ───────────────────────────────────────────
        # The trends table accumulates a row per ingestion run; deduplicate by
        # technique_name (case-insensitive), keep the highest emergence_score
        # per technique, and cap to the top 30. Anything else is overwritten by
        # the next ReasoningAgent pass.
        rehydrated_trend_count = 0
        # Names from the legacy extractor that should never surface as
        # "techniques" (source labels and arXiv class codes).
        BAD_TREND_NAMES = {
            "arxiv", "github", "patents", "patent", "startup", "startups",
            "social", "blog", "blogs", "hacker-news", "rss", "funding",
        }
        try:
            best_per_technique: dict[str, TrendEntry] = {}
            for t_data in db.list_trends():
                try:
                    trend = TrendEntry(**t_data)
                except Exception:
                    continue
                raw_name = (trend.technique_name or "").strip()
                key = raw_name.lower()
                if not key:
                    continue
                if key in BAD_TREND_NAMES:
                    continue
                # arXiv subject codes such as cs.LG / stat.ML.
                if "." in key and len(key) <= 8 and key.split(".")[0].isalpha():
                    continue
                existing = best_per_technique.get(key)
                if existing is None or trend.emergence_score > existing.emergence_score:
                    best_per_technique[key] = trend

            top = sorted(
                best_per_technique.values(),
                key=lambda t: t.emergence_score,
                reverse=True,
            )[:30]
            for trend in top:
                reas_agent.trends[trend.id] = trend
                rehydrated_trend_count += 1
        except Exception as e:
            logger.warning("list_trends failed: %s", e)

        # ── Blueprints ───────────────────────────────────────
        for bp_data in db.list_blueprints():
            try:
                bp = ProductBlueprint(**bp_data)
                bp_engine.generated_blueprints[bp.id] = bp
            except Exception:
                continue

        # ── Pipelines ────────────────────────────────────────
        for pl_data in db.list_pipelines():
            try:
                pl = MLPipeline(**pl_data)
                pl_generator.generated_pipelines[pl.id] = pl
            except Exception:
                continue

        logger.info(
            "Hydrated %s signals, %s trends, %s blueprints, %s pipelines from database",
            rehydrated_signal_count,
            rehydrated_trend_count,
            len(bp_engine.generated_blueprints),
            len(pl_generator.generated_pipelines),
        )
    except Exception as e:
        logger.warning("Hydration from DB failed: %s", e)

    # ── 7. Telegram Bot ──────────────────────────
    logger.info("Initializing Telegram bot...")
    tg_bot = TelegramBot(database=db)
    api_routes.telegram_bot = tg_bot

    async def _stats_provider() -> dict:
        if not api_routes.vector_store:
            return {}
        vs_count = api_routes.vector_store.get_collection_count()
        agents_status = {
            "ingestion": api_routes.ingestion_agent.status if api_routes.ingestion_agent else "offline",
            "reasoning": api_routes.reasoning_agent.status if api_routes.reasoning_agent else "offline",
            "memory": api_routes.memory_agent.status if api_routes.memory_agent else "offline",
        }
        payloads = api_routes.vector_store.get_all_payloads(limit=200)
        novelty = [float(p.get("novelty_score", 0) or 0) for p in payloads]
        avg_novelty = round(sum(novelty) / max(len(novelty), 1), 3)
        return {
            "total_signals": vs_count,
            "active_trends": len(api_routes.reasoning_agent.trends) if api_routes.reasoning_agent else 0,
            "blueprints_generated": len(api_routes.blueprint_engine.generated_blueprints) if api_routes.blueprint_engine else 0,
            "pipelines_launched": len(api_routes.pipeline_generator.generated_pipelines) if api_routes.pipeline_generator else 0,
            "avg_novelty_score": avg_novelty,
            "agents_status": agents_status,
        }

    async def _trends_provider(limit: int) -> list[dict]:
        if not api_routes.reasoning_agent:
            return []
        trends = sorted(
            api_routes.reasoning_agent.trends.values(),
            key=lambda t: t.emergence_score,
            reverse=True,
        )
        return [t.model_dump(mode="json") for t in trends[: max(1, limit)]]

    async def _pipelines_provider(limit: int) -> list[dict]:
        if not api_routes.pipeline_generator:
            return []
        return [
            p.model_dump(mode="json")
            for p in api_routes.pipeline_generator.list_pipelines()[: max(1, limit)]
        ]

    tg_bot.attach_providers(
        stats=_stats_provider,
        trends=_trends_provider,
        pipelines=_pipelines_provider,
    )
    if tg_bot.enabled:
        await tg_bot.start_polling()

    logger.info("=" * 60)
    logger.info("  All systems operational. Ready to serve.")
    logger.info(f"  API: http://localhost:{config.API_PORT}/docs")
    logger.info(f"  Dashboard: http://localhost:5173")
    logger.info("=" * 60)

    yield  # ── Application runs ──

    # ── Shutdown ─────────────────────────────────
    logger.info("Shutting down agents...")
    if tg_bot.enabled:
        await tg_bot.stop_polling()
    await ing_agent.stop()
    await reas_agent.stop()
    await mem_agent.stop()
    await retrain_agent.stop()
    await bus.stop()
    db.close()
    logger.info("VectorMind shutdown complete.")


# ─── Create FastAPI App ───────────────────────────────────────
app = FastAPI(
    title="VectorMind",
    description=(
        "GenAI Research Intelligence Platform — "
        "Autonomous prediction and productization engine for AI research."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

# ─── CORS ─────────────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.CORS_ORIGINS,
    # We don't authenticate via cookies, so credentials=False lets us keep a
    # wildcard origin (browsers refuse `*` together with credentials=True).
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def public_health():
    """Lightweight readiness probe used by Hugging Face Spaces and the
    Android app's status pill. Stays free of database I/O so it answers
    instantly even while background ingestion is busy."""
    return {"status": "ok", "service": "vectormind-backend"}

# ─── Routes ───────────────────────────────────────────────────
app.include_router(api_routes.router)


@app.get("/")
async def root():
    return {
        "name": "VectorMind",
        "tagline": "GenAI Research Intelligence Platform",
        "version": "1.0.0",
        "docs": "/docs",
        "api": "/api",
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=config.API_HOST,
        port=config.API_PORT,
        reload=True,
    )
