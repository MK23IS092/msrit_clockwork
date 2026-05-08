"""Production Telegram bot for VectorMinds.

Real two-way bot built on ``python-telegram-bot`` 21:
- subscribers register with ``/start`` (chat ids persisted in Postgres/SQLite)
- alerts (trend, ingestion summary, pipeline complete) broadcast to every subscriber
- live commands: ``/start /help /status /trends /pipelines /unsubscribe``

The bot has no mock fallback: when ``TELEGRAM_BOT_TOKEN`` is unset the module logs a
clear warning and ``send_*`` methods become no-ops returning ``False``. When a token
is set, every send is a real Telegram API call and failures are returned as ``False``.

The bot lifecycle (``start_polling`` / ``stop``) is managed from ``main.lifespan`` so
polling runs alongside FastAPI without blocking the event loop.
"""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime, timezone
from typing import Awaitable, Callable, Optional

from telegram import Update
from telegram.constants import ParseMode
from telegram.error import TelegramError
from telegram.ext import (
    Application,
    ApplicationBuilder,
    CommandHandler,
    ContextTypes,
)

import config

logger = logging.getLogger("vectorminds.telegram")


WELCOME = (
    "<b>VectorMinds</b> - GenAI Research Intelligence\n"
    "You are subscribed to live alerts.\n\n"
    "Commands:\n"
    "/help - show this help\n"
    "/status - platform stats\n"
    "/trends - top emerging techniques\n"
    "/pipelines - recent ML pipelines\n"
    "/unsubscribe - stop receiving alerts"
)

HELP_MESSAGE = (
    "<b>VectorMinds Bot</b>\n"
    "/start - subscribe and show this menu\n"
    "/help - show this help\n"
    "/status - platform stats (signals, trends, pipelines)\n"
    "/trends - top 5 ranked techniques\n"
    "/pipelines - recent ML pipelines\n"
    "/unsubscribe - stop receiving alerts"
)


class TelegramBot:
    """Production Telegram delivery and command surface for VectorMinds."""

    def __init__(self, token: str = "", database=None):
        self.token = (token or config.TELEGRAM_BOT_TOKEN or "").strip()
        self.database = database
        self.enabled: bool = bool(self.token)
        self._sent_count = 0
        self._failed_count = 0
        self._app: Optional[Application] = None
        self._polling_started = False
        # Optional callable returning a fresh ``stats`` dict for ``/status``.
        self._stats_provider: Optional[Callable[[], Awaitable[dict]]] = None
        # Optional callable returning ``list[dict]`` of trends for ``/trends``.
        self._trends_provider: Optional[Callable[[int], Awaitable[list[dict]]]] = None
        # Optional callable returning ``list[dict]`` of pipelines for ``/pipelines``.
        self._pipelines_provider: Optional[Callable[[int], Awaitable[list[dict]]]] = None
        if not self.enabled:
            logger.warning(
                "Telegram bot disabled - set TELEGRAM_BOT_TOKEN to enable real delivery"
            )

    # ── lifecycle ─────────────────────────────────────────────

    async def start_polling(self) -> None:
        """Start the long-polling task. Safe to call once; idempotent on retry."""
        if not self.enabled:
            return
        if self._polling_started:
            return
        try:
            self._app = (
                ApplicationBuilder()
                .token(self.token)
                .concurrent_updates(True)
                .build()
            )
            self._app.add_handler(CommandHandler("start", self._cmd_start))
            self._app.add_handler(CommandHandler("help", self._cmd_help))
            self._app.add_handler(CommandHandler("status", self._cmd_status))
            self._app.add_handler(CommandHandler("trends", self._cmd_trends))
            self._app.add_handler(CommandHandler("pipelines", self._cmd_pipelines))
            self._app.add_handler(CommandHandler("unsubscribe", self._cmd_unsubscribe))

            await self._app.initialize()
            await self._app.start()
            await self._app.updater.start_polling(drop_pending_updates=False)
            self._polling_started = True
            me = await self._app.bot.get_me()
            count = self._subscriber_count()
            logger.info(
                "Telegram bot @%s online (subscribers=%s)", me.username, count
            )
        except Exception as e:
            logger.error("Telegram polling failed to start: %s", e)
            self.enabled = False
            self._app = None
            self._polling_started = False

    async def stop_polling(self) -> None:
        """Cleanly stop the polling task. Safe to call multiple times."""
        app = self._app
        if not app:
            return
        try:
            if app.updater and app.updater.running:
                await app.updater.stop()
            if app.running:
                await app.stop()
            await app.shutdown()
        except Exception as e:
            logger.warning("Telegram bot stop encountered: %s", e)
        finally:
            self._app = None
            self._polling_started = False

    def attach_providers(
        self,
        stats: Optional[Callable[[], Awaitable[dict]]] = None,
        trends: Optional[Callable[[int], Awaitable[list[dict]]]] = None,
        pipelines: Optional[Callable[[int], Awaitable[list[dict]]]] = None,
    ) -> None:
        """Wire callables that resolve dynamic data for ``/status``, ``/trends`` and ``/pipelines``."""
        if stats is not None:
            self._stats_provider = stats
        if trends is not None:
            self._trends_provider = trends
        if pipelines is not None:
            self._pipelines_provider = pipelines

    # ── command handlers ─────────────────────────────────────

    async def _cmd_start(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        user = update.effective_user
        if not chat:
            return
        self._upsert_subscriber(chat.id, user.username if user else None)
        await context.bot.send_message(
            chat_id=chat.id, text=WELCOME, parse_mode=ParseMode.HTML
        )

    async def _cmd_help(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        if not chat:
            return
        await context.bot.send_message(
            chat_id=chat.id, text=HELP_MESSAGE, parse_mode=ParseMode.HTML
        )

    async def _cmd_status(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        if not chat:
            return
        if not self._stats_provider:
            await context.bot.send_message(
                chat_id=chat.id, text="Stats not available right now."
            )
            return
        try:
            data = await self._stats_provider()
        except Exception as e:
            logger.warning("status provider failed: %s", e)
            await context.bot.send_message(chat_id=chat.id, text="Stats are temporarily unavailable.")
            return
        agents = data.get("agents_status") or {}
        text = (
            "<b>VectorMinds status</b>\n"
            f"Total signals: <b>{data.get('total_signals', 0)}</b>\n"
            f"Active trends: <b>{data.get('active_trends', 0)}</b>\n"
            f"Blueprints: <b>{data.get('blueprints_generated', 0)}</b>\n"
            f"Pipelines: <b>{data.get('pipelines_launched', 0)}</b>\n"
            f"Avg novelty: <b>{data.get('avg_novelty_score', 0)}</b>\n"
            f"Agents: ingestion=<b>{agents.get('ingestion', '?')}</b> "
            f"reasoning=<b>{agents.get('reasoning', '?')}</b> "
            f"memory=<b>{agents.get('memory', '?')}</b>"
        )
        await context.bot.send_message(chat_id=chat.id, text=text, parse_mode=ParseMode.HTML)

    async def _cmd_trends(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        if not chat:
            return
        if not self._trends_provider:
            await context.bot.send_message(chat_id=chat.id, text="Trend service is offline.")
            return
        try:
            trends = await self._trends_provider(5)
        except Exception as e:
            logger.warning("trend provider failed: %s", e)
            await context.bot.send_message(chat_id=chat.id, text="Trends temporarily unavailable.")
            return
        if not trends:
            await context.bot.send_message(
                chat_id=chat.id,
                text="No trends yet. Trigger an ingestion run from the API and try again.",
            )
            return
        lines = ["<b>Top Trends</b>"]
        for i, t in enumerate(trends, start=1):
            lines.append(
                f"{i}. <b>{t.get('technique_name', '?')}</b> | "
                f"emergence={float(t.get('emergence_score', 0)):.2f} | "
                f"ETA {t.get('mainstream_eta_months', '?')}mo"
            )
        await context.bot.send_message(
            chat_id=chat.id, text="\n".join(lines), parse_mode=ParseMode.HTML
        )

    async def _cmd_pipelines(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        if not chat:
            return
        if not self._pipelines_provider:
            await context.bot.send_message(chat_id=chat.id, text="Pipelines service is offline.")
            return
        try:
            pipelines = await self._pipelines_provider(5)
        except Exception as e:
            logger.warning("pipeline provider failed: %s", e)
            await context.bot.send_message(chat_id=chat.id, text="Pipelines temporarily unavailable.")
            return
        if not pipelines:
            await context.bot.send_message(
                chat_id=chat.id,
                text="No pipelines generated yet. Use /api/pipelines/generate.",
            )
            return
        lines = ["<b>Recent Pipelines</b>"]
        for i, p in enumerate(pipelines, start=1):
            colab = p.get("colab_url") or ""
            line = (
                f"{i}. <b>{p.get('technique_name', '?')}</b> "
                f"({p.get('task_type', '?')}, {p.get('status', '?')})"
            )
            if colab.startswith("https://"):
                line += f"\n  <a href=\"{colab}\">Open in Colab</a>"
            lines.append(line)
        await context.bot.send_message(
            chat_id=chat.id,
            text="\n".join(lines),
            parse_mode=ParseMode.HTML,
            disable_web_page_preview=True,
        )

    async def _cmd_unsubscribe(self, update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
        chat = update.effective_chat
        if not chat:
            return
        self._delete_subscriber(chat.id)
        await context.bot.send_message(
            chat_id=chat.id,
            text="You will no longer receive VectorMinds alerts. /start any time to subscribe again.",
        )

    # ── outbound delivery ─────────────────────────────────────

    async def _send_to(self, chat_id: int | str, text: str) -> bool:
        if not self.enabled or not self._app:
            return False
        try:
            await self._app.bot.send_message(
                chat_id=chat_id,
                text=text,
                parse_mode=ParseMode.HTML,
                disable_web_page_preview=True,
            )
            self._sent_count += 1
            return True
        except TelegramError as e:
            logger.warning("Telegram send to %s failed: %s", chat_id, e)
            self._failed_count += 1
            return False

    async def broadcast(self, text: str) -> int:
        """Send to every subscriber. Returns number of successful deliveries."""
        if not self.enabled or not self._app:
            return 0
        chat_ids = self._list_subscriber_ids()
        if not chat_ids:
            logger.info("Telegram broadcast skipped (no subscribers).")
            return 0
        results = await asyncio.gather(
            *(self._send_to(cid, text) for cid in chat_ids), return_exceptions=False
        )
        sent = sum(1 for ok in results if ok)
        return sent

    async def send_message(self, text: str, parse_mode: str = "HTML") -> bool:
        """Backward-compatible single-call entrypoint that broadcasts to all subscribers."""
        sent = await self.broadcast(text)
        return sent > 0

    async def send_trend_alert(self, technique: str, score: float, eta: int) -> bool:
        msg = (
            "<b>VectorMinds - New High-Impact Trend</b>\n\n"
            f"<b>Technique:</b> {technique}\n"
            f"<b>Emergence:</b> {score:.2f}\n"
            f"<b>Mainstream ETA:</b> {eta} months\n\n"
            "View in app or /trends"
        )
        return await self.send_message(msg)

    async def send_pipeline_complete(
        self, technique: str, task_type: str, metrics: dict, colab_url: str = ""
    ) -> bool:
        metrics_str = ", ".join(f"{k}: {v}" for k, v in (metrics or {}).items() if not isinstance(v, dict))
        msg = (
            "<b>VectorMinds - Training Pipeline Ready</b>\n\n"
            f"<b>Technique:</b> {technique}\n"
            f"<b>Task:</b> {task_type}\n"
            f"<b>Highlights:</b> {metrics_str}"
        )
        if colab_url:
            msg += f"\n\n<a href=\"{colab_url}\">Open in Colab</a>"
        return await self.send_message(msg)

    async def send_ingestion_summary(self, paper_count: int, repo_count: int) -> bool:
        msg = (
            "<b>VectorMinds - Ingestion Complete</b>\n\n"
            f"<b>New Papers:</b> {paper_count}\n"
            f"<b>New Repos:</b> {repo_count}\n"
            f"<b>Total Signals:</b> {paper_count + repo_count}"
        )
        return await self.send_message(msg)

    # ── subscriber persistence ────────────────────────────────

    def _ensure_table(self) -> bool:
        if not self.database:
            return False
        try:
            self.database.ensure_telegram_subscribers_table()
            return True
        except Exception as e:
            logger.warning("Telegram subscriber table not available: %s", e)
            return False

    def _upsert_subscriber(self, chat_id: int, username: Optional[str]) -> None:
        if not self._ensure_table():
            return
        try:
            self.database.upsert_telegram_subscriber(int(chat_id), username or "")
            logger.info("Telegram subscriber added: %s", chat_id)
        except Exception as e:
            logger.warning("Failed to persist subscriber %s: %s", chat_id, e)

    def _delete_subscriber(self, chat_id: int) -> None:
        if not self._ensure_table():
            return
        try:
            self.database.delete_telegram_subscriber(int(chat_id))
            logger.info("Telegram subscriber removed: %s", chat_id)
        except Exception as e:
            logger.warning("Failed to delete subscriber %s: %s", chat_id, e)

    def _list_subscriber_ids(self) -> list[int]:
        if not self._ensure_table():
            return []
        try:
            return list(self.database.list_telegram_subscriber_ids())
        except Exception as e:
            logger.warning("Failed to list subscribers: %s", e)
            return []

    def _subscriber_count(self) -> int:
        return len(self._list_subscriber_ids())

    # ── stats ────────────────────────────────────────────────

    def get_stats(self) -> dict:
        return {
            "enabled": self.enabled,
            "polling": self._polling_started,
            "subscribers": self._subscriber_count(),
            "messages_sent": self._sent_count,
            "messages_failed": self._failed_count,
            "now": datetime.now(timezone.utc).isoformat(),
        }
