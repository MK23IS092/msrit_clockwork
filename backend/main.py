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
from intelligence.pipeline_generator import PipelineGenerator
from delivery.telegram_bot import TelegramBot
from delivery import api_routes
from db.database import Database

# ─── Logging ──────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s │ %(name)-30s │ %(levelname)-7s │ %(message)s",
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

    api_routes.blueprint_engine = bp_engine
    api_routes.pipeline_generator = pl_generator

    # ── 7. Telegram Bot ──────────────────────────
    logger.info("Initializing Telegram bot...")
    tg_bot = TelegramBot()
    api_routes.telegram_bot = tg_bot

    logger.info("=" * 60)
    logger.info("  All systems operational. Ready to serve.")
    logger.info(f"  API: http://localhost:{config.API_PORT}/docs")
    logger.info(f"  Dashboard: http://localhost:5173")
    logger.info("=" * 60)

    yield  # ── Application runs ──

    # ── Shutdown ─────────────────────────────────
    logger.info("Shutting down agents...")
    await ing_agent.stop()
    await reas_agent.stop()
    await mem_agent.stop()
    await retrain_agent.stop()
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
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
