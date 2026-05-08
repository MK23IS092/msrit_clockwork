import sys
import types

import pytest

# Lightweight stubs so importing API routes does not require heavy optional deps.
if "sentence_transformers" not in sys.modules:
    st_mod = types.ModuleType("sentence_transformers")

    class _SentenceTransformer:  # pragma: no cover - import shim only
        def __init__(self, *args, **kwargs):
            pass

        def encode(self, *args, **kwargs):
            return [0.0]

    st_mod.SentenceTransformer = _SentenceTransformer
    sys.modules["sentence_transformers"] = st_mod

if "qdrant_client" not in sys.modules:
    qc_mod = types.ModuleType("qdrant_client")
    qcm_mod = types.ModuleType("qdrant_client.models")

    class _QdrantClient:  # pragma: no cover - import shim only
        def __init__(self, *args, **kwargs):
            pass

    class _Dummy:  # pragma: no cover - import shim only
        def __init__(self, *args, **kwargs):
            pass

    qc_mod.QdrantClient = _QdrantClient
    qcm_mod.Distance = _Dummy
    qcm_mod.PointStruct = _Dummy
    qcm_mod.VectorParams = _Dummy
    qcm_mod.Filter = _Dummy
    qcm_mod.FieldCondition = _Dummy
    qcm_mod.MatchValue = _Dummy
    sys.modules["qdrant_client"] = qc_mod
    sys.modules["qdrant_client.models"] = qcm_mod

if "arxiv" not in sys.modules:
    ax_mod = types.ModuleType("arxiv")

    class _Search:  # pragma: no cover - import shim only
        def __init__(self, *args, **kwargs):
            pass

        def results(self):
            return []

    ax_mod.Search = _Search
    sys.modules["arxiv"] = ax_mod

from delivery import api_routes
from ingestion.schema import ProductBlueprint, ResearchSignal, SignalSource, TrendEntry


class _FakeIngestionAgent:
    status = "running"

    async def run_ingestion(self, source: str = "all", category: str | None = None):
        return [
            ResearchSignal(
                source=SignalSource.ARXIV,
                source_id="arxiv-1",
                title="Sparse Attention Meshes",
                raw_text="A new attention architecture for efficient long-context reasoning.",
                authors=["A. Researcher"],
                categories=["cs.LG"],
                url="https://arxiv.org/abs/0000.00001",
                novelty_score=0.72,
            ),
            ResearchSignal(
                source=SignalSource.GITHUB,
                source_id="owner/repo",
                title="sparse-attention-mesh",
                raw_text="Reference implementation",
                authors=["owner"],
                categories=["machine-learning"],
                url="https://github.com/owner/repo",
                novelty_score=0.68,
                metadata={"stars": 420},
            ),
        ]

    def get_health(self):
        return {"name": "ingestion", "status": "running"}


class _FakeReasoningAgent:
    status = "running"

    def __init__(self):
        self.trends: dict[str, TrendEntry] = {}

    async def analyze_trends(self):
        trend = TrendEntry(
            rank=1,
            technique_name="Sparse Attention Meshes",
            description="Efficient long-context modeling architecture.",
            emergence_score=0.81,
            novelty_score=0.70,
            impact_score=0.77,
            mainstream_eta_months=9,
            confidence=0.89,
            source_signals={"arxiv_papers": 1, "github_repos": 1, "total_github_stars": 420},
            paper_count=1,
            github_stars=420,
            signal_ids=["arxiv-1", "owner/repo"],
        )
        self.trends = {trend.id: trend}
        return [trend]

    def get_health(self):
        return {"name": "reasoning", "status": "running"}


class _FakeBlueprintEngine:
    def __init__(self):
        self.generated_blueprints = {}

    async def generate_blueprint(self, trend: TrendEntry, additional_context: str = ""):
        bp = ProductBlueprint(
            technique_name=trend.technique_name,
            trend_id=trend.id,
            problem_statement="Problem",
            market_size="Large",
            technical_implementation="Implementation",
            architecture_decisions=["Decision A"],
            differentiation_strategy="Differentiate",
            dataset_requirements="Dataset",
            go_to_market="GTM",
            risk_assessment="Risk",
            first_90_day_milestones=["M1"],
            suggested_stack=["FastAPI"],
        )
        self.generated_blueprints[bp.id] = bp
        return bp


class _FakeMemoryAgent:
    status = "running"

    def __init__(self):
        self.saved_blueprints = {}

    def store_blueprint(self, blueprint_id: str, blueprint_data: dict):
        self.saved_blueprints[blueprint_id] = blueprint_data

    def get_health(self):
        return {"name": "memory", "status": "running"}


class _FakeDatabase:
    def __init__(self):
        self.signals = []
        self.trends = []

    def save_signal(self, signal_data: dict):
        self.signals.append(signal_data)

    def save_trend(self, trend_data: dict):
        self.trends.append(trend_data)


@pytest.mark.asyncio
async def test_ingest_to_trend_to_blueprint_flow(monkeypatch):
    ingestion = _FakeIngestionAgent()
    reasoning = _FakeReasoningAgent()
    memory = _FakeMemoryAgent()
    blueprints = _FakeBlueprintEngine()
    db = _FakeDatabase()

    monkeypatch.setattr(api_routes, "ingestion_agent", ingestion)
    monkeypatch.setattr(api_routes, "reasoning_agent", reasoning)
    monkeypatch.setattr(api_routes, "memory_agent", memory)
    monkeypatch.setattr(api_routes, "blueprint_engine", blueprints)
    monkeypatch.setattr(api_routes, "database", db)
    monkeypatch.setattr(api_routes, "telegram_bot", None)
    monkeypatch.setattr(api_routes, "vector_store", None)

    ingest_resp = await api_routes.trigger_ingestion(api_routes.IngestRequest(source="all"))
    assert ingest_resp["status"] == "success"
    assert ingest_resp["signals_ingested"] == 2
    assert ingest_resp["trends_updated"] == 1
    assert len(db.signals) == 2
    assert len(db.trends) == 1

    trends_resp = await api_routes.get_trends(limit=10)
    assert trends_resp["count"] == 1
    trend_id = trends_resp["trends"][0]["id"]

    bp_resp = await api_routes.generate_blueprint(
        api_routes.BlueprintRequest(trend_id=trend_id)
    )
    assert bp_resp["technique_name"] == "Sparse Attention Meshes"
    assert bp_resp["id"] in memory.saved_blueprints
