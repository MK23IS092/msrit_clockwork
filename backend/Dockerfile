# ─────────────────────────────────────────────────────────────────────────────
# VectorMind backend — Hugging Face Spaces (Docker SDK) image.
#
# Why this Dockerfile is the way it is:
#   • HF Spaces (Docker SDK) expect the app to listen on port 7860.
#   • HF builds are slow; we install CPU-only torch from the PyTorch CPU index
#     so we don't pull a multi-GB CUDA wheel that we'll never use.
#   • Persistence: HF Spaces give us /data (writable) but the rest of the FS
#     resets on rebuild. We point DATA_DIR there so SQLite + run logs survive
#     restarts even though they're wiped on space restart-with-rebuild.
#   • Everything heavy is opt-in via env vars (Postgres/Redis/Kafka). For the
#     demo the SQLite + in-memory paths are used — no external services
#     required to stand up the demo.
# ─────────────────────────────────────────────────────────────────────────────

FROM python:3.11-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    HF_HOME=/data/.cache/huggingface \
    TRANSFORMERS_CACHE=/data/.cache/huggingface \
    SENTENCE_TRANSFORMERS_HOME=/data/.cache/huggingface

# System deps:
#  • libgomp1 — required by torch (OpenMP runtime)
#  • git/curl — handy for runtime downloads of model weights
#  • build-essential not needed because we install slim binary wheels
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        git curl libgomp1 ca-certificates \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install CPU-only torch first so the heavy wheel is cached and we don't
# accidentally pull a 2GB CUDA build via a transitive dep.
RUN pip install --upgrade pip \
 && pip install torch==2.4.0 --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . /app

# HF Spaces conventions:
#   • port 7860
#   • /data is the only persistent writable mount
ENV API_HOST=0.0.0.0 \
    API_PORT=7860 \
    DB_BACKEND=sqlite \
    STATE_STORE_BACKEND=sqlite \
    MESSAGE_BUS_BACKEND=in_memory \
    USE_MOCK_LLM=false \
    HF_DEPLOYMENT=true

# /data is writable on HF; pre-create our subfolders so first-write succeeds.
RUN mkdir -p /data/cache /data/pipeline_runs \
 && chmod -R 777 /data

EXPOSE 7860

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "7860"]
