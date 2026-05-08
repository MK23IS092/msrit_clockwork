"""Pipeline execution engine.

Runs generated pipeline scripts as background subprocesses, captures logs,
and exposes runtime status/artifact locations.
"""

from __future__ import annotations

import asyncio
import os
import sys
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Awaitable, Callable, Optional

import config
from ingestion.schema import MLPipeline


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


@dataclass
class PipelineRun:
    run_id: str
    pipeline_id: str
    status: str = "queued"  # queued|running|completed|failed|timeout
    created_at: str = field(default_factory=_now_iso)
    started_at: Optional[str] = None
    finished_at: Optional[str] = None
    exit_code: Optional[int] = None
    duration_seconds: Optional[float] = None
    error: str = ""
    run_dir: str = ""
    script_path: str = ""
    log_path: str = ""
    artifacts_dir: str = ""
    command: list[str] = field(default_factory=list)
    retry_count: int = 0
    max_retries: int = 0

    def to_dict(self) -> dict:
        return {
            "run_id": self.run_id,
            "pipeline_id": self.pipeline_id,
            "status": self.status,
            "created_at": self.created_at,
            "started_at": self.started_at,
            "finished_at": self.finished_at,
            "exit_code": self.exit_code,
            "duration_seconds": self.duration_seconds,
            "error": self.error,
            "run_dir": self.run_dir,
            "script_path": self.script_path,
            "log_path": self.log_path,
            "artifacts_dir": self.artifacts_dir,
            "command": self.command,
            "retry_count": self.retry_count,
            "max_retries": self.max_retries,
        }


class PipelineExecutor:
    """Execute generated pipelines in managed local run directories."""

    def __init__(
        self,
        max_concurrent_runs: int | None = None,
        max_retries: int | None = None,
        retry_backoff_seconds: int | None = None,
        on_state_change: Optional[Callable[[dict], Awaitable[None] | None]] = None,
    ):
        self.base_dir = Path(config.DATA_DIR) / "pipeline_runs"
        self.base_dir.mkdir(parents=True, exist_ok=True)
        self.runs_by_pipeline: dict[str, dict[str, PipelineRun]] = {}
        self.tasks: dict[str, asyncio.Task] = {}
        self.max_concurrent_runs = max_concurrent_runs or config.PIPELINE_MAX_CONCURRENT_RUNS
        self.max_retries = max_retries if max_retries is not None else config.PIPELINE_MAX_RETRIES
        self.retry_backoff_seconds = (
            retry_backoff_seconds
            if retry_backoff_seconds is not None
            else config.PIPELINE_RETRY_BACKOFF_SECONDS
        )
        self._semaphore = asyncio.Semaphore(max(1, self.max_concurrent_runs))
        self.on_state_change = on_state_change

    def _register_run(self, run: PipelineRun):
        bucket = self.runs_by_pipeline.setdefault(run.pipeline_id, {})
        bucket[run.run_id] = run

    async def _emit_state(self, run: PipelineRun):
        if not self.on_state_change:
            return
        result = self.on_state_change(run.to_dict())
        if asyncio.iscoroutine(result):
            await result

    def list_runs(self, pipeline_id: str) -> list[dict]:
        runs = list(self.runs_by_pipeline.get(pipeline_id, {}).values())
        runs.sort(key=lambda r: r.created_at, reverse=True)
        return [r.to_dict() for r in runs]

    def get_run(self, pipeline_id: str, run_id: str) -> Optional[dict]:
        run = self.runs_by_pipeline.get(pipeline_id, {}).get(run_id)
        return run.to_dict() if run else None

    def _prepare_run_files(self, pipeline: MLPipeline, run_id: str) -> PipelineRun:
        run_dir = self.base_dir / pipeline.id / run_id
        run_dir.mkdir(parents=True, exist_ok=True)

        script_path = run_dir / "pipeline.py"
        log_path = run_dir / "run.log"
        artifacts_dir = run_dir / "artifacts"
        artifacts_dir.mkdir(parents=True, exist_ok=True)

        script_header = (
            "import os\n"
            "import subprocess\n"
            f"os.environ.setdefault('VECTOR_MINDS_ARTIFACT_DIR', r'{artifacts_dir.as_posix()}')\n\n"
        )
        script_body = self._transpile_notebook_to_python(pipeline.notebook_content)
        script_path.write_text(script_header + script_body, encoding="utf-8")

        return PipelineRun(
            run_id=run_id,
            pipeline_id=pipeline.id,
            run_dir=str(run_dir),
            script_path=str(script_path),
            log_path=str(log_path),
            artifacts_dir=str(artifacts_dir),
        )

    def _transpile_notebook_to_python(self, text: str) -> str:
        """Convert lightweight notebook magics to runnable Python."""
        out_lines: list[str] = []
        for raw in text.splitlines():
            stripped = raw.lstrip()
            indent = raw[: len(raw) - len(stripped)]
            if stripped.startswith("!"):
                shell_cmd = stripped[1:].strip().replace("\\", "\\\\").replace('"', '\\"')
                out_lines.append(
                    f'{indent}subprocess.check_call("{shell_cmd}", shell=True)'
                )
                continue
            if stripped.startswith("%"):
                out_lines.append(f"{indent}# skipped notebook magic: {stripped}")
                continue
            out_lines.append(raw)
        return "\n".join(out_lines) + "\n"

    async def _run_subprocess_once(self, run: PipelineRun, timeout_seconds: int):
        run.started_at = _now_iso()
        run.status = "running"
        start = datetime.now(timezone.utc)
        await self._emit_state(run)

        cmd = [sys.executable, "-u", run.script_path]
        run.command = cmd

        env = os.environ.copy()
        env["PYTHONUNBUFFERED"] = "1"
        env["VECTOR_MINDS_RUN_ID"] = run.run_id
        env["VECTOR_MINDS_PIPELINE_ID"] = run.pipeline_id
        env["VECTOR_MINDS_ARTIFACT_DIR"] = run.artifacts_dir

        with open(run.log_path, "w", encoding="utf-8") as log_file:
            process = await asyncio.create_subprocess_exec(
                *cmd,
                cwd=run.run_dir,
                stdout=log_file,
                stderr=log_file,
                env=env,
            )
            try:
                await asyncio.wait_for(process.wait(), timeout=timeout_seconds)
                run.exit_code = process.returncode
                run.status = "completed" if process.returncode == 0 else "failed"
            except asyncio.TimeoutError:
                process.kill()
                await process.wait()
                run.exit_code = process.returncode
                run.status = "timeout"
                run.error = f"Execution exceeded timeout ({timeout_seconds}s)"

        end = datetime.now(timezone.utc)
        run.finished_at = _now_iso()
        run.duration_seconds = round((end - start).total_seconds(), 3)
        await self._emit_state(run)

    async def _run_with_retry(self, run: PipelineRun, timeout_seconds: int):
        run.max_retries = max(0, self.max_retries)
        async with self._semaphore:
            while True:
                await self._run_subprocess_once(run, timeout_seconds=timeout_seconds)
                if run.status == "completed":
                    return
                if run.retry_count >= run.max_retries:
                    return
                run.retry_count += 1
                run.status = "queued"
                run.error = ""
                run.exit_code = None
                run.started_at = None
                run.finished_at = None
                run.duration_seconds = None
                await self._emit_state(run)
                await asyncio.sleep(max(1, self.retry_backoff_seconds))

    async def execute_pipeline(
        self,
        pipeline: MLPipeline,
        timeout_seconds: int = 1800,
    ) -> dict:
        run_id = str(uuid.uuid4())
        run = self._prepare_run_files(pipeline, run_id)
        self._register_run(run)
        await self._run_with_retry(run, timeout_seconds=timeout_seconds)
        return run.to_dict()

    def execute_pipeline_async(
        self,
        pipeline: MLPipeline,
        timeout_seconds: int = 1800,
    ) -> dict:
        run_id = str(uuid.uuid4())
        run = self._prepare_run_files(pipeline, run_id)
        self._register_run(run)
        run.max_retries = max(0, self.max_retries)

        task = asyncio.create_task(self._run_with_retry(run, timeout_seconds=timeout_seconds))
        self.tasks[run_id] = task

        def _cleanup(_):
            self.tasks.pop(run_id, None)

        task.add_done_callback(_cleanup)
        return run.to_dict()
