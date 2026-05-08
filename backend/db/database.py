"""Database — SQLite metadata storage layer.

Replaces PostgreSQL for the hackathon MVP. Stores structured metadata,
prediction records, user feedback, and agent state.
"""

from __future__ import annotations

import json
import logging
import sqlite3
from typing import Optional

import config

logger = logging.getLogger("vectorminds.database")

try:
    import psycopg2
except Exception:  # pragma: no cover - optional dependency
    psycopg2 = None


class Database:
    """Metadata store for VectorMinds (SQLite or PostgreSQL)."""

    _instance: Optional["Database"] = None

    @classmethod
    def get_instance(cls) -> "Database":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self.db_path = str(config.DB_PATH)
        self.backend = config.DB_BACKEND
        self._conn: Optional[object] = None

    def initialize(self):
        """Create database and tables."""
        if self.backend == "postgres":
            if psycopg2 is None:
                raise RuntimeError(
                    "DB_BACKEND=postgres requires psycopg2-binary dependency."
                )
            self._conn = psycopg2.connect(config.POSTGRES_DSN)
            self._conn.autocommit = False
        else:
            self._conn = sqlite3.connect(self.db_path, check_same_thread=False)
            self._conn.row_factory = sqlite3.Row
        self._create_tables()
        logger.info(
            "Database initialized using %s backend",
            self.backend,
        )

    def _execute(self, query: str, params: tuple = ()):
        cursor = self._conn.cursor()
        cursor.execute(query, params)
        return cursor

    def _commit(self):
        self._conn.commit()

    def _placeholder(self) -> str:
        return "%s" if self.backend == "postgres" else "?"

    def _create_tables(self):
        cursor = self._conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS research_signals (
                id TEXT PRIMARY KEY,
                source TEXT NOT NULL,
                source_id TEXT,
                title TEXT NOT NULL,
                raw_text TEXT,
                authors TEXT,
                categories TEXT,
                url TEXT,
                novelty_score REAL DEFAULT 0,
                impact_score REAL DEFAULT 0,
                metadata TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS trends (
                id TEXT PRIMARY KEY,
                rank INTEGER,
                technique_name TEXT NOT NULL,
                description TEXT,
                emergence_score REAL DEFAULT 0,
                novelty_score REAL DEFAULT 0,
                impact_score REAL DEFAULT 0,
                mainstream_eta_months INTEGER DEFAULT 12,
                confidence REAL DEFAULT 0,
                data TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS blueprints (
                id TEXT PRIMARY KEY,
                technique_name TEXT NOT NULL,
                trend_id TEXT,
                data TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS pipelines (
                id TEXT PRIMARY KEY,
                technique_name TEXT NOT NULL,
                task_type TEXT,
                status TEXT DEFAULT 'generated',
                data TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS pipeline_runs (
                run_id TEXT PRIMARY KEY,
                pipeline_id TEXT NOT NULL,
                status TEXT NOT NULL,
                data TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user_feedback (
                id TEXT PRIMARY KEY,
                target_id TEXT NOT NULL,
                target_type TEXT NOT NULL,
                action TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS agent_state (
                agent_name TEXT PRIMARY KEY,
                state TEXT,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS telegram_subscribers (
                chat_id BIGINT PRIMARY KEY,
                username TEXT DEFAULT '',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)

        self._commit()

    def save_signal(self, signal_data: dict):
        if self.backend == "postgres":
            upsert = """
            INSERT INTO research_signals
            (id, source, source_id, title, raw_text, authors, categories, url,
             novelty_score, impact_score, metadata)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
              source = EXCLUDED.source,
              source_id = EXCLUDED.source_id,
              title = EXCLUDED.title,
              raw_text = EXCLUDED.raw_text,
              authors = EXCLUDED.authors,
              categories = EXCLUDED.categories,
              url = EXCLUDED.url,
              novelty_score = EXCLUDED.novelty_score,
              impact_score = EXCLUDED.impact_score,
              metadata = EXCLUDED.metadata
            """
        else:
            upsert = """INSERT OR REPLACE INTO research_signals
            (id, source, source_id, title, raw_text, authors, categories, url,
             novelty_score, impact_score, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""

        self._execute(
            upsert,
            (
                signal_data.get("id", ""),
                signal_data.get("source", ""),
                signal_data.get("source_id", ""),
                signal_data.get("title", ""),
                signal_data.get("raw_text", ""),
                json.dumps(signal_data.get("authors", [])),
                json.dumps(signal_data.get("categories", [])),
                signal_data.get("url", ""),
                signal_data.get("novelty_score", 0),
                signal_data.get("impact_score", 0),
                json.dumps(signal_data.get("metadata", {})),
            ),
        )
        self._commit()

    def save_trend(self, trend_data: dict):
        if self.backend == "postgres":
            upsert = """
            INSERT INTO trends
            (id, rank, technique_name, description, emergence_score,
             novelty_score, impact_score, mainstream_eta_months, confidence, data)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
              rank = EXCLUDED.rank,
              technique_name = EXCLUDED.technique_name,
              description = EXCLUDED.description,
              emergence_score = EXCLUDED.emergence_score,
              novelty_score = EXCLUDED.novelty_score,
              impact_score = EXCLUDED.impact_score,
              mainstream_eta_months = EXCLUDED.mainstream_eta_months,
              confidence = EXCLUDED.confidence,
              data = EXCLUDED.data,
              updated_at = CURRENT_TIMESTAMP
            """
        else:
            upsert = """INSERT OR REPLACE INTO trends
            (id, rank, technique_name, description, emergence_score,
             novelty_score, impact_score, mainstream_eta_months, confidence, data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""

        self._execute(
            upsert,
            (
                trend_data.get("id", ""),
                trend_data.get("rank", 0),
                trend_data.get("technique_name", ""),
                trend_data.get("description", ""),
                trend_data.get("emergence_score", 0),
                trend_data.get("novelty_score", 0),
                trend_data.get("impact_score", 0),
                trend_data.get("mainstream_eta_months", 12),
                trend_data.get("confidence", 0),
                json.dumps(trend_data),
            ),
        )
        self._commit()

    def get_signals_count(self) -> int:
        cursor = self._execute("SELECT COUNT(*) FROM research_signals")
        return cursor.fetchone()[0]

    def get_signals_by_source(self, source: str) -> int:
        ph = self._placeholder()
        cursor = self._execute(
            f"SELECT COUNT(*) FROM research_signals WHERE source = {ph}",
            (source,),
        )
        return cursor.fetchone()[0]

    def save_feedback(self, feedback_data: dict):
        ph = self._placeholder()
        self._execute(
            f"""INSERT INTO user_feedback (id, target_id, target_type, action)
            VALUES ({ph}, {ph}, {ph}, {ph})""",
            (
                feedback_data.get("id", ""),
                feedback_data.get("target_id", ""),
                feedback_data.get("target_type", ""),
                feedback_data.get("action", ""),
            ),
        )
        self._commit()

    def save_pipeline(self, pipeline_data: dict):
        """Persist generated/updated pipeline snapshot."""
        if self.backend == "postgres":
            upsert = """
            INSERT INTO pipelines (id, technique_name, task_type, status, data)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
              technique_name = EXCLUDED.technique_name,
              task_type = EXCLUDED.task_type,
              status = EXCLUDED.status,
              data = EXCLUDED.data
            """
        else:
            upsert = """INSERT OR REPLACE INTO pipelines
            (id, technique_name, task_type, status, data)
            VALUES (?, ?, ?, ?, ?)"""
        self._execute(
            upsert,
            (
                pipeline_data.get("id", ""),
                pipeline_data.get("technique_name", ""),
                pipeline_data.get("task_type", ""),
                pipeline_data.get("status", "generated"),
                json.dumps(pipeline_data),
            ),
        )
        self._commit()

    def save_pipeline_run(self, run_data: dict):
        """Persist pipeline run state."""
        if self.backend == "postgres":
            upsert = """
            INSERT INTO pipeline_runs (run_id, pipeline_id, status, data)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (run_id) DO UPDATE SET
              pipeline_id = EXCLUDED.pipeline_id,
              status = EXCLUDED.status,
              data = EXCLUDED.data,
              updated_at = CURRENT_TIMESTAMP
            """
        else:
            upsert = """INSERT OR REPLACE INTO pipeline_runs
            (run_id, pipeline_id, status, data)
            VALUES (?, ?, ?, ?)"""
        self._execute(
            upsert,
            (
                run_data.get("run_id", ""),
                run_data.get("pipeline_id", ""),
                run_data.get("status", "queued"),
                json.dumps(run_data),
            ),
        )
        self._commit()

    def get_pipeline_runs(self, pipeline_id: str) -> list[dict]:
        """Fetch all runs for a pipeline, newest first."""
        ph = self._placeholder()
        cursor = self._execute(
            f"SELECT data FROM pipeline_runs WHERE pipeline_id = {ph} ORDER BY created_at DESC",
            (pipeline_id,),
        )
        rows = cursor.fetchall()
        out = []
        for row in rows:
            payload = row["data"] if isinstance(row, sqlite3.Row) else row[0]
            out.append(json.loads(payload))
        return out

    def get_pipeline_run(self, run_id: str) -> Optional[dict]:
        """Fetch one pipeline run by id."""
        ph = self._placeholder()
        cursor = self._execute(
            f"SELECT data FROM pipeline_runs WHERE run_id = {ph}",
            (run_id,),
        )
        row = cursor.fetchone()
        if not row:
            return None
        payload = row["data"] if isinstance(row, sqlite3.Row) else row[0]
        return json.loads(payload)

    # ── Blueprints ──────────────────────────────────────────

    def save_blueprint(self, blueprint_data: dict):
        """Persist a generated blueprint."""
        if self.backend == "postgres":
            upsert = """
            INSERT INTO blueprints (id, technique_name, trend_id, data)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
              technique_name = EXCLUDED.technique_name,
              trend_id = EXCLUDED.trend_id,
              data = EXCLUDED.data
            """
        else:
            upsert = """INSERT OR REPLACE INTO blueprints
            (id, technique_name, trend_id, data)
            VALUES (?, ?, ?, ?)"""
        self._execute(
            upsert,
            (
                blueprint_data.get("id", ""),
                blueprint_data.get("technique_name", ""),
                blueprint_data.get("trend_id", ""),
                json.dumps(blueprint_data),
            ),
        )
        self._commit()

    def list_blueprints(self) -> list[dict]:
        cursor = self._execute(
            "SELECT data FROM blueprints ORDER BY created_at DESC"
        )
        rows = cursor.fetchall()
        out = []
        for row in rows:
            payload = row["data"] if isinstance(row, sqlite3.Row) else row[0]
            try:
                out.append(json.loads(payload))
            except Exception:
                continue
        return out

    # ── Signals & Trends listing (for startup hydration) ────

    def list_signals(self, limit: int = 1000) -> list[dict]:
        """Return persisted research signals as plain dicts (no embedding).

        Used at startup to rehydrate the in-memory vector store. Embeddings
        are recomputed from ``raw_text`` because the column doesn't store
        them.
        """
        ph = self._placeholder()
        cursor = self._execute(
            f"""SELECT id, source, source_id, title, raw_text, authors,
                       categories, url, novelty_score, impact_score, metadata
                FROM research_signals
                ORDER BY created_at DESC
                LIMIT {ph}""",
            (int(limit),),
        )
        rows = cursor.fetchall()
        out: list[dict] = []
        for row in rows:
            if isinstance(row, sqlite3.Row):
                d = dict(row)
            else:
                cols = [
                    "id", "source", "source_id", "title", "raw_text", "authors",
                    "categories", "url", "novelty_score", "impact_score", "metadata",
                ]
                d = dict(zip(cols, row))
            try:
                d["authors"] = json.loads(d.get("authors") or "[]")
            except Exception:
                d["authors"] = []
            try:
                d["categories"] = json.loads(d.get("categories") or "[]")
            except Exception:
                d["categories"] = []
            try:
                d["metadata"] = json.loads(d.get("metadata") or "{}")
            except Exception:
                d["metadata"] = {}
            out.append(d)
        return out

    def list_trends(self) -> list[dict]:
        """Return persisted trends. Each row stores the full trend JSON in ``data``."""
        cursor = self._execute(
            "SELECT data FROM trends ORDER BY rank ASC"
        )
        rows = cursor.fetchall()
        out: list[dict] = []
        for row in rows:
            payload = row["data"] if isinstance(row, sqlite3.Row) else row[0]
            try:
                out.append(json.loads(payload))
            except Exception:
                continue
        return out

    def list_pipelines(self) -> list[dict]:
        cursor = self._execute(
            "SELECT data FROM pipelines ORDER BY created_at DESC"
        )
        rows = cursor.fetchall()
        out = []
        for row in rows:
            payload = row["data"] if isinstance(row, sqlite3.Row) else row[0]
            try:
                out.append(json.loads(payload))
            except Exception:
                continue
        return out

    # ── Telegram subscribers ────────────────────────────────

    def ensure_telegram_subscribers_table(self):
        """Idempotent ensure of the subscribers table for older deployments."""
        cursor = self._conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS telegram_subscribers (
                chat_id BIGINT PRIMARY KEY,
                username TEXT DEFAULT '',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self._commit()

    def upsert_telegram_subscriber(self, chat_id: int, username: str = ""):
        if self.backend == "postgres":
            sql = """
            INSERT INTO telegram_subscribers (chat_id, username)
            VALUES (%s, %s)
            ON CONFLICT (chat_id) DO UPDATE SET
              username = EXCLUDED.username,
              updated_at = CURRENT_TIMESTAMP
            """
        else:
            sql = """INSERT OR REPLACE INTO telegram_subscribers (chat_id, username)
            VALUES (?, ?)"""
        self._execute(sql, (int(chat_id), username or ""))
        self._commit()

    def delete_telegram_subscriber(self, chat_id: int):
        ph = self._placeholder()
        self._execute(
            f"DELETE FROM telegram_subscribers WHERE chat_id = {ph}",
            (int(chat_id),),
        )
        self._commit()

    def list_telegram_subscriber_ids(self) -> list[int]:
        cursor = self._execute("SELECT chat_id FROM telegram_subscribers ORDER BY created_at ASC")
        rows = cursor.fetchall()
        out = []
        for row in rows:
            cid = row["chat_id"] if isinstance(row, sqlite3.Row) else row[0]
            try:
                out.append(int(cid))
            except Exception:
                continue
        return out

    def close(self):
        if self._conn:
            self._conn.close()
