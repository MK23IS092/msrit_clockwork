"""Telegram Bot — Push notification delivery.

Sends alerts when high-impact techniques are detected,
training pipelines complete, or other significant events occur.
Uses Telegram Bot API (free).
"""

from __future__ import annotations

import logging
from typing import Optional

import httpx

import config

logger = logging.getLogger("vectorminds.telegram")

TELEGRAM_API = "https://api.telegram.org"


class TelegramBot:
    """Telegram notification bot for VectorMinds alerts."""

    def __init__(self, token: str = "", chat_id: str = ""):
        self.token = token or config.TELEGRAM_BOT_TOKEN
        self.chat_id = chat_id or config.TELEGRAM_CHAT_ID
        self.enabled = bool(self.token and self.chat_id)
        self._sent_count = 0

        if not self.enabled:
            logger.warning(
                "Telegram bot disabled — set TELEGRAM_BOT_TOKEN and "
                "TELEGRAM_CHAT_ID env vars to enable"
            )

    async def send_message(self, text: str, parse_mode: str = "HTML") -> bool:
        """Send a text message via Telegram.

        Args:
            text: Message text (supports HTML formatting)
            parse_mode: 'HTML' or 'Markdown'

        Returns:
            True if sent successfully
        """
        if not self.enabled:
            logger.info(f"[TELEGRAM MOCK] {text[:100]}...")
            self._sent_count += 1
            return True

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                resp = await client.post(
                    f"{TELEGRAM_API}/bot{self.token}/sendMessage",
                    json={
                        "chat_id": self.chat_id,
                        "text": text,
                        "parse_mode": parse_mode,
                    },
                )
                resp.raise_for_status()
                self._sent_count += 1
                return True
        except Exception as e:
            logger.error(f"Telegram send failed: {e}")
            return False

    async def send_trend_alert(
        self, technique: str, score: float, eta: int
    ):
        """Send a high-impact trend alert."""
        msg = (
            f"🚀 <b>VectorMinds — New High-Impact Trend</b>\n\n"
            f"<b>Technique:</b> {technique}\n"
            f"<b>Emergence Score:</b> {score:.2f}\n"
            f"<b>Mainstream ETA:</b> {eta} months\n\n"
            f"📊 View on Dashboard →"
        )
        await self.send_message(msg)

    async def send_pipeline_complete(
        self, technique: str, task_type: str, metrics: dict
    ):
        """Send training pipeline completion alert."""
        metrics_str = ", ".join(f"{k}: {v}" for k, v in metrics.items())
        msg = (
            f"✅ <b>VectorMinds — Training Complete</b>\n\n"
            f"<b>Technique:</b> {technique}\n"
            f"<b>Task:</b> {task_type}\n"
            f"<b>Metrics:</b> {metrics_str}\n\n"
            f"📦 Model artifact ready for export"
        )
        await self.send_message(msg)

    async def send_ingestion_summary(self, paper_count: int, repo_count: int):
        """Send ingestion cycle summary."""
        msg = (
            f"📥 <b>VectorMinds — Ingestion Complete</b>\n\n"
            f"<b>New Papers:</b> {paper_count}\n"
            f"<b>New Repos:</b> {repo_count}\n"
            f"<b>Total Signals Processed:</b> {paper_count + repo_count}"
        )
        await self.send_message(msg)

    def get_stats(self) -> dict:
        return {
            "enabled": self.enabled,
            "messages_sent": self._sent_count,
        }
