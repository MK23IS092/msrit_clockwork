from fastapi import FastAPI
from fastapi.testclient import TestClient

from delivery import api_routes
from ingestion.schema import MLPipeline
from intelligence.pipeline_executor import PipelineExecutor


class _PipelineGeneratorForRun:
    def __init__(self):
        self.generated_pipelines = {}
        pipeline = MLPipeline(
            technique_name="Smoke Technique",
            task_type="text-classification",
            dataset_name="ag_news",
            dataset_source="huggingface-fallback",
            model_architecture="distilbert-base-uncased",
            notebook_content="print('pipeline-run-smoke')",
            colab_url="https://colab.research.google.com/#create=true",
            status="generated",
            metrics={},
            model_card="test",
        )
        self.generated_pipelines[pipeline.id] = pipeline
        self._pipeline_id = pipeline.id

    def get_pipeline(self, pipeline_id: str):
        return self.generated_pipelines.get(pipeline_id)

    def list_pipelines(self):
        return list(self.generated_pipelines.values())

    def update_pipeline(self, pipeline: MLPipeline):
        self.generated_pipelines[pipeline.id] = pipeline


def test_pipeline_run_lifecycle_endpoints(monkeypatch):
    generator = _PipelineGeneratorForRun()
    executor = PipelineExecutor()

    monkeypatch.setattr(api_routes, "pipeline_generator", generator)
    monkeypatch.setattr(api_routes, "pipeline_executor", executor)
    monkeypatch.setattr(api_routes, "memory_agent", None)
    monkeypatch.setattr(api_routes, "telegram_bot", None)

    app = FastAPI()
    app.include_router(api_routes.router)
    client = TestClient(app)

    run_resp = client.post(
        f"/api/pipelines/{generator._pipeline_id}/run",
        json={"wait_for_completion": True, "timeout_seconds": 120},
    )
    assert run_resp.status_code == 200
    body = run_resp.json()
    assert body["run"]["status"] == "completed"
    run_id = body["run"]["run_id"]

    get_run = client.get(f"/api/pipelines/{generator._pipeline_id}/runs/{run_id}")
    assert get_run.status_code == 200
    assert get_run.json()["status"] == "completed"

    list_runs = client.get(f"/api/pipelines/{generator._pipeline_id}/runs")
    assert list_runs.status_code == 200
    assert list_runs.json()["count"] >= 1

    log_resp = client.get(f"/api/pipelines/{generator._pipeline_id}/runs/{run_id}/log")
    assert log_resp.status_code == 200
    assert "pipeline-run-smoke" in log_resp.json()["log_tail"]


def test_executor_transpiles_notebook_bang_commands():
    executor = PipelineExecutor()
    text = "!echo hello\nprint('ok')\n"
    py = executor._transpile_notebook_to_python(text)
    assert "subprocess.check_call" in py
    assert "echo hello" in py
    assert "print('ok')" in py
