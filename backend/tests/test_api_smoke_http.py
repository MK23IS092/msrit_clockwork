from fastapi import FastAPI
from fastapi.testclient import TestClient

from delivery import api_routes


class _Agent:
    def __init__(self, name: str):
        self.name = name
        self.status = "running"
        self.trends = {}

    def get_health(self):
        return {"name": self.name, "status": self.status, "running": True}


class _VectorStore:
    def get_collection_count(self):
        return 0

    def get_all_payloads(self, limit=200):
        return []


class _Database:
    def get_signals_by_source(self, source: str):
        return 0

class _PipelineGenerator:
    generated_pipelines = {}

    def dataset_candidates(self, technique_name: str, description: str = "", task_type=None, top_k: int = 8):
        return [
            {"name": "ag_news", "source": "huggingface", "url": "https://huggingface.co/datasets/ag_news", "downloads": 100}
        ]

class _ExperimentDesigner:
    async def design_experiment(self, technique_name: str, brief: str = ""):
        return {
            "technique_name": technique_name,
            "hypothesis": "test",
            "dataset_suggestion": "ag_news",
            "model_suggestion": "distilbert-base-uncased",
            "key_metric": "accuracy",
            "baseline": "0.8",
            "notebook_content": "content",
        }

class _ReasoningAgentWithTrend:
    status = "running"

    def __init__(self):
        from ingestion.schema import TrendEntry
        trend = TrendEntry(
            rank=1,
            technique_name="Sparse Attention",
            description="desc",
            emergence_score=0.81,
            novelty_score=0.72,
            impact_score=0.83,
            mainstream_eta_months=6,
            confidence=0.92,
            source_signals={"arxiv_papers": 3},
            paper_count=3,
            github_stars=900,
            signal_ids=["s1"],
        )
        self.trends = {trend.id: trend}

    def get_health(self):
        return {"name": "reasoning", "status": self.status, "running": True}


def test_health_and_stats_smoke(monkeypatch):
    monkeypatch.setattr(api_routes, "ingestion_agent", _Agent("ingestion"))
    monkeypatch.setattr(api_routes, "reasoning_agent", _Agent("reasoning"))
    monkeypatch.setattr(api_routes, "memory_agent", _Agent("memory"))
    monkeypatch.setattr(api_routes, "vector_store", _VectorStore())
    monkeypatch.setattr(api_routes, "database", _Database())
    monkeypatch.setattr(api_routes, "blueprint_engine", type("B", (), {"generated_blueprints": {}})())
    monkeypatch.setattr(api_routes, "pipeline_generator", _PipelineGenerator())
    monkeypatch.setattr(api_routes, "experiment_designer", _ExperimentDesigner())
    monkeypatch.setattr(api_routes, "reasoning_agent", _ReasoningAgentWithTrend())
    monkeypatch.setattr(api_routes, "telegram_bot", None)

    app = FastAPI()
    app.include_router(api_routes.router)
    client = TestClient(app)

    health = client.get("/api/health")
    assert health.status_code == 200
    assert health.json()["status"] == "healthy"

    stats = client.get("/api/stats")
    assert stats.status_code == 200
    body = stats.json()
    assert "total_signals" in body
    assert "agents_status" in body

    candidates = client.post(
        "/api/pipelines/dataset-candidates",
        json={"technique_name": "Sparse Attention", "description": "NLP"},
    )
    assert candidates.status_code == 200
    assert candidates.json()["count"] >= 1

    experiment = client.post(
        "/api/experiments/design",
        json={"technique_name": "Sparse Attention", "brief": "NLP"},
    )
    assert experiment.status_code == 200
    assert experiment.json()["technique_name"] == "Sparse Attention"

    premium_context = client.get("/api/dashboard/premium-context")
    assert premium_context.status_code == 200
    assert premium_context.json()["focus"] == "Sparse Attention"
