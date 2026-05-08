"""Database — SQLite metadata storage layer.

Replaces PostgreSQL for the hackathon MVP. Stores structured metadata,
prediction records, user feedback, and agent state.
"""

from __future__ import annotations

import json
import logging
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import Optional

import config

logger = logging.getLogger("vectorminds.database")


class Database:
    """SQLite-based metadata store for VectorMinds."""

    _instance: Optional["Database"] = None

    @classmethod
    def get_instance(cls) -> "Database":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self.db_path = str(config.DB_PATH)
        self._conn: Optional[sqlite3.Connection] = None

    def initialize(self):
        """Create database and tables."""
        self._conn = sqlite3.connect(self.db_path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._create_tables()
        logger.info(f"Database initialized at {self.db_path}")

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

        self._conn.commit()

    def save_signal(self, signal_data: dict):
        cursor = self._conn.cursor()
        cursor.execute(
            """INSERT OR REPLACE INTO research_signals
            (id, source, source_id, title, raw_text, authors, categories, url,
             novelty_score, impact_score, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
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
        self._conn.commit()

    def save_trend(self, trend_data: dict):
        cursor = self._conn.cursor()
        cursor.execute(
            """INSERT OR REPLACE INTO trends
            (id, rank, technique_name, description, emergence_score,
             novelty_score, impact_score, mainstream_eta_months, confidence, data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
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
        self._conn.commit()

    def get_signals_count(self) -> int:
        cursor = self._conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM research_signals")
        return cursor.fetchone()[0]

    def get_signals_by_source(self, source: str) -> int:
        cursor = self._conn.cursor()
        cursor.execute(
            "SELECT COUNT(*) FROM research_signals WHERE source = ?",
            (source,),
        )
        return cursor.fetchone()[0]

    def save_feedback(self, feedback_data: dict):
        cursor = self._conn.cursor()
        cursor.execute(
            """INSERT INTO user_feedback (id, target_id, target_type, action)
            VALUES (?, ?, ?, ?)""",
            (
                feedback_data.get("id", ""),
                feedback_data.get("target_id", ""),
                feedback_data.get("target_type", ""),
                feedback_data.get("action", ""),
            ),
        )
        self._conn.commit()

    def close(self):
        if self._conn:
            self._conn.close()
