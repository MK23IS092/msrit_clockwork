"""VectorMind Configuration Module.

Central configuration for all platform settings, loaded from environment
variables with sensible defaults for hackathon demo mode.
"""

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

# ─── Base Paths ───────────────────────────────────────────────
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(exist_ok=True)
DB_PATH = DATA_DIR / "vectormind.db"

# ─── API Keys (ALL FREE) ─────────────────────────────────────
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
HUGGINGFACE_TOKEN = os.getenv("HUGGINGFACE_TOKEN", "")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")  # optional, for higher rate limits

# ─── LLM Settings (Groq Free Tier — Llama 3.1 70B) ──────────
GROQ_BASE_URL = "https://api.groq.com/openai/v1"
LLM_MODEL = os.getenv("LLM_MODEL", "llama-3.1-70b-versatile")
USE_MOCK_LLM = os.getenv("USE_MOCK_LLM", "true").lower() == "true"

# ─── Embedding Model (Free — runs locally) ───────────────────
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-en-v1.5")
EMBEDDING_DIM = 384  # bge-small-en-v1.5 dimension

# ─── Qdrant Settings (In-Memory — Free) ──────────────────────
QDRANT_COLLECTION = "research_signals"
QDRANT_HOST = os.getenv("QDRANT_HOST", "")  # empty = in-memory mode
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))

# ─── Novelty Scoring (Section 4.2) ───────────────────────────
NOVELTY_K_NEIGHBORS = 50
NOVELTY_MEAN_WEIGHT = 0.6
NOVELTY_MIN_WEIGHT = 0.4
NOVELTY_TEMPORAL_DISCOUNT = 0.7
NOVELTY_TEMPORAL_WINDOW_HOURS = 72

# ─── Impact Prediction ───────────────────────────────────────
IMPACT_HIGH_THRESHOLD = 0.75
IMPACT_MEDIUM_THRESHOLD = 0.50

# ─── Ingestion Settings ──────────────────────────────────────
ARXIV_CATEGORIES = ["cs.LG", "cs.AI", "cs.CL", "cs.CV", "cs.NE"]
ARXIV_MAX_RESULTS = 50
GITHUB_TRENDING_LANGUAGES = ["python", "jupyter-notebook"]
GITHUB_MAX_RESULTS = 30
INGESTION_INTERVAL_SECONDS = 3600  # 1 hour

# ─── Deduplication ────────────────────────────────────────────
DEDUP_SIMILARITY_THRESHOLD = 0.95

# ─── Server Settings ─────────────────────────────────────────
API_HOST = os.getenv("API_HOST", "0.0.0.0")
API_PORT = int(os.getenv("API_PORT", "8000"))
CORS_ORIGINS = [
    "http://localhost:5173",
    "http://localhost:3000",
    "http://127.0.0.1:5173",
]

# ─── Pipeline Generator ──────────────────────────────────────
SUPPORTED_TASK_CATEGORIES = [
    "text-classification",
    "image-classification",
    "text-generation",
    "question-answering",
    "summarization",
]
