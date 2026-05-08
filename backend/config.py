"""VectorMind Configuration Module.

Central configuration for all platform settings, loaded from environment
variables with sensible defaults for hackathon demo mode.
"""

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(override=True)

# ─── Base Paths ───────────────────────────────────────────────
BASE_DIR = Path(__file__).resolve().parent

# When running on Hugging Face Spaces, /data is the only writable persistent
# mount. Falling back to backend/data keeps local dev unchanged.
_HF_DATA = Path("/data")
DATA_DIR = (
    _HF_DATA
    if os.getenv("HF_DEPLOYMENT", "").lower() == "true" and _HF_DATA.exists()
    else BASE_DIR / "data"
)
DATA_DIR.mkdir(parents=True, exist_ok=True)
DB_PATH = DATA_DIR / "vectormind.db"

# ─── API Keys (ALL FREE) ─────────────────────────────────────
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
# Backward-compatible fallback so older .env files still run.
LLM_API_KEY = GEMINI_API_KEY or os.getenv("GROQ_API_KEY", "")
TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
# Long-polling (getUpdates): only ONE process per bot token may poll. If you run
# Hugging Face + local backend with the same TELEGRAM_BOT_TOKEN, set this to
# "false" on one side (outbound send_message still works; /start and commands
# need exactly one poller).
TELEGRAM_ENABLE_POLLING = os.getenv("TELEGRAM_ENABLE_POLLING", "true").lower() == "true"
HUGGINGFACE_TOKEN = os.getenv("HUGGINGFACE_TOKEN", "")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")  # optional, for higher rate limits
KAGGLE_USERNAME = os.getenv("KAGGLE_USERNAME", "")
KAGGLE_KEY = os.getenv("KAGGLE_KEY", "")

# ─── LLM Settings (Gemini) ───────────────────────────────────
GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
LLM_MODEL = os.getenv("LLM_MODEL", "gemini-2.5-flash-preview-05-20")
USE_MOCK_LLM = os.getenv("USE_MOCK_LLM", "true").lower() == "true"

# ─── Embedding Model (Free — runs locally) ───────────────────
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-en-v1.5")
EMBEDDING_DIM = 384  # bge-small-en-v1.5 dimension

# ─── Qdrant Settings (In-Memory — Free) ──────────────────────
QDRANT_COLLECTION = "research_signals"
QDRANT_HOST = os.getenv("QDRANT_HOST", "")  # empty = in-memory mode
QDRANT_PORT = int(os.getenv("QDRANT_PORT", "6333"))

# ─── Distributed Infra (Docker/local) ────────────────────────
DB_BACKEND = os.getenv("DB_BACKEND", "sqlite").lower()  # sqlite|postgres
POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
POSTGRES_DB = os.getenv("POSTGRES_DB", "vectormind")
POSTGRES_USER = os.getenv("POSTGRES_USER", "vectormind")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "vectormind")
POSTGRES_DSN = os.getenv(
    "POSTGRES_DSN",
    f"postgresql://{POSTGRES_USER}:{POSTGRES_PASSWORD}@{POSTGRES_HOST}:{POSTGRES_PORT}/{POSTGRES_DB}",
)

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379/0")
STATE_STORE_BACKEND = os.getenv("STATE_STORE_BACKEND", "sqlite").lower()  # sqlite|redis

MESSAGE_BUS_BACKEND = os.getenv("MESSAGE_BUS_BACKEND", "in_memory").lower()  # in_memory|kafka_mirror
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_TOPIC_PREFIX = os.getenv("KAFKA_TOPIC_PREFIX", "vectormind")

# ─── Pipeline Runtime (production knobs) ─────────────────────
PIPELINE_RUN_TIMEOUT_SECONDS = int(os.getenv("PIPELINE_RUN_TIMEOUT_SECONDS", "1800"))
PIPELINE_MAX_CONCURRENT_RUNS = int(os.getenv("PIPELINE_MAX_CONCURRENT_RUNS", "2"))
PIPELINE_MAX_RETRIES = int(os.getenv("PIPELINE_MAX_RETRIES", "1"))
PIPELINE_RETRY_BACKOFF_SECONDS = int(os.getenv("PIPELINE_RETRY_BACKOFF_SECONDS", "5"))

# ─── Novelty Scoring (Section 4.2) ───────────────────────────
NOVELTY_K_NEIGHBORS = 50
NOVELTY_MEAN_WEIGHT = 0.6
NOVELTY_MIN_WEIGHT = 0.4
NOVELTY_TEMPORAL_DISCOUNT = 0.7
NOVELTY_TEMPORAL_WINDOW_HOURS = 72

# ─── Impact Prediction ───────────────────────────────────────
IMPACT_HIGH_THRESHOLD = 0.75
IMPACT_MEDIUM_THRESHOLD = 0.50

# ─── Retraining Promotion Gates ───────────────────────────────
RETRAIN_DRIFT_THRESHOLD = float(os.getenv("RETRAIN_DRIFT_THRESHOLD", "0.30"))
RETRAIN_MIN_ACCURACY = float(os.getenv("RETRAIN_MIN_ACCURACY", "0.78"))
RETRAIN_MIN_F1 = float(os.getenv("RETRAIN_MIN_F1", "0.75"))
RETRAIN_MAX_LATENCY_MS = float(os.getenv("RETRAIN_MAX_LATENCY_MS", "180"))
RETRAIN_MIN_IMPROVEMENT = float(os.getenv("RETRAIN_MIN_IMPROVEMENT", "0.01"))

# ─── Ingestion Settings ──────────────────────────────────────
ARXIV_CATEGORIES = ["cs.LG", "cs.AI", "cs.CL", "cs.CV", "cs.NE"]
ARXIV_MAX_RESULTS = 50
GITHUB_TRENDING_LANGUAGES = ["python", "jupyter-notebook"]
GITHUB_MAX_RESULTS = 30
INGESTION_INTERVAL_SECONDS = 3600  # 1 hour
PATENTS_MAX_RESULTS = int(os.getenv("PATENTS_MAX_RESULTS", "20"))
STARTUPS_MAX_RESULTS = int(os.getenv("STARTUPS_MAX_RESULTS", "20"))
SOCIAL_MAX_RESULTS = int(os.getenv("SOCIAL_MAX_RESULTS", "30"))
BLOG_MAX_RESULTS = int(os.getenv("BLOG_MAX_RESULTS", "20"))

# External source controls
ENABLE_PATENTS_REAL = os.getenv("ENABLE_PATENTS_REAL", "true").lower() == "true"
ENABLE_STARTUPS_REAL = os.getenv("ENABLE_STARTUPS_REAL", "true").lower() == "true"
ENABLE_SOCIAL_REAL = os.getenv("ENABLE_SOCIAL_REAL", "true").lower() == "true"
ENABLE_BLOG_REAL = os.getenv("ENABLE_BLOG_REAL", "true").lower() == "true"
ALLOW_SIMULATED_SOURCES = os.getenv("ALLOW_SIMULATED_SOURCES", "true").lower() == "true"

# ─── Deduplication ────────────────────────────────────────────
DEDUP_SIMILARITY_THRESHOLD = 0.95

# ─── Server Settings ─────────────────────────────────────────
API_HOST = os.getenv("API_HOST", "0.0.0.0")
API_PORT = int(os.getenv("API_PORT", "8000"))
API_ADMIN_KEY = os.getenv("API_ADMIN_KEY", "")
# Allow any origin by default. Browsers enforce CORS, the Android client
# isn't a browser, and we don't use cookies for auth — so a permissive
# wildcard is fine and lets judges curl the API from anywhere.
CORS_ORIGINS = os.getenv("CORS_ORIGINS", "*").split(",")

# ─── Pipeline Generator ──────────────────────────────────────
SUPPORTED_TASK_CATEGORIES = [
    "text-classification",
    "image-classification",
    "text-generation",
    "question-answering",
    "summarization",
]
