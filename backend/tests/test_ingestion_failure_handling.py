import asyncio

from agents.ingestion_agent import IngestionAgent
from ingestion.schema import ResearchSignal, SignalSource


class _StubEmbedder:
    def embed_batch(self, texts):
        return [[0.1, 0.2, 0.3] for _ in texts]


class _StubVectorStore:
    def compute_novelty_score(self, embedding):
        return 0.66

    def upsert_signal(self, **kwargs):
        return None


def test_patent_failure_uses_simulated_fallback(monkeypatch):
    agent = IngestionAgent()
    agent.embedding_engine = _StubEmbedder()
    agent.vector_store = _StubVectorStore()

    async def _fail_patents(*args, **kwargs):
        raise RuntimeError("connector down")

    async def _noop_publish(*args, **kwargs):
        return None

    monkeypatch.setattr(agent.patent_crawler, "fetch_recent_patents", _fail_patents)
    monkeypatch.setattr(agent, "publish", _noop_publish)
    monkeypatch.setattr("config.ENABLE_PATENTS_REAL", True)
    monkeypatch.setattr("config.ALLOW_SIMULATED_SOURCES", True)

    signals = asyncio.run(agent.run_ingestion(source="patents"))

    assert len(signals) == 1
    assert signals[0].source == SignalSource.PATENT
    assert signals[0].metadata.get("simulated") is True
    assert agent.get_health()["source_health"]["patents"]["status"] == "unhealthy"


def test_startup_failure_without_fallback_returns_empty(monkeypatch):
    agent = IngestionAgent()
    agent.embedding_engine = _StubEmbedder()
    agent.vector_store = _StubVectorStore()

    async def _fail_startups(*args, **kwargs):
        raise RuntimeError("connector down")

    async def _noop_publish(*args, **kwargs):
        return None

    monkeypatch.setattr(agent.startup_crawler, "fetch_startup_signals", _fail_startups)
    monkeypatch.setattr(agent, "publish", _noop_publish)
    monkeypatch.setattr("config.ENABLE_STARTUPS_REAL", True)
    monkeypatch.setattr("config.ALLOW_SIMULATED_SOURCES", False)

    signals = asyncio.run(agent.run_ingestion(source="startups"))

    assert signals == []
    assert agent.get_health()["source_health"]["startups"]["status"] == "unhealthy"


def test_blog_real_success_sets_healthy(monkeypatch):
    agent = IngestionAgent()
    agent.embedding_engine = _StubEmbedder()
    agent.vector_store = _StubVectorStore()

    async def _ok_blogs(*args, **kwargs):
        return [
            ResearchSignal(
                source=SignalSource.BLOG,
                source_id="blog-1",
                title="AI systems update",
                raw_text="new release",
            )
        ]

    async def _noop_publish(*args, **kwargs):
        return None

    monkeypatch.setattr(agent.blog_crawler, "fetch_blog_signals", _ok_blogs)
    monkeypatch.setattr(agent, "publish", _noop_publish)
    monkeypatch.setattr("config.ENABLE_BLOG_REAL", True)

    signals = asyncio.run(agent.run_ingestion(source="blog"))
    assert len(signals) == 1
    assert signals[0].source == SignalSource.BLOG
    assert agent.get_health()["source_health"]["blog"]["status"] == "healthy"
