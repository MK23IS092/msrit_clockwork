"""Base Agent — Abstract base class for all OpenClaw agents.

Provides lifecycle management, event loop, health checking,
and state checkpointing.
"""

from __future__ import annotations

import asyncio
import logging
from abc import ABC, abstractmethod
from datetime import datetime
from typing import Optional

import config
from agents.message_bus import MessageBus
from ingestion.schema import AgentEvent

logger = logging.getLogger("vectormind.agent")

try:
    import redis.asyncio as redis_async
except Exception:  # pragma: no cover - optional dependency
    redis_async = None


class BaseAgent(ABC):
    """Abstract base class for VectorMind agents."""

    def __init__(self, name: str):
        self.name = name
        self.bus = MessageBus.get_instance()
        self._running = False
        self._task: Optional[asyncio.Task] = None
        self._last_heartbeat = datetime.utcnow()
        self._events_processed = 0
        self._status = "idle"
        self._subscribed_topics: list[str] = []
        self._queues: list[asyncio.Queue] = []
        self._state_store = None

    @property
    def status(self) -> str:
        return self._status

    @property
    def is_running(self) -> bool:
        return self._running

    def get_health(self) -> dict:
        """Return agent health status."""
        return {
            "name": self.name,
            "status": self._status,
            "running": self._running,
            "events_processed": self._events_processed,
            "last_heartbeat": self._last_heartbeat.isoformat(),
        }

    def subscribe(self, topic: str):
        """Subscribe to a message bus topic."""
        queue = self.bus.subscribe(topic)
        self._queues.append(queue)
        self._subscribed_topics.append(topic)
        logger.info(f"Agent '{self.name}' subscribed to '{topic}'")

    async def publish(self, topic: str, payload: dict):
        """Publish an event to the message bus."""
        await self.bus.publish_simple(topic, self.name, payload)

    async def start(self):
        """Start the agent's event loop."""
        self._running = True
        self._status = "running"
        if config.STATE_STORE_BACKEND == "redis" and redis_async is not None:
            self._state_store = redis_async.from_url(config.REDIS_URL, decode_responses=True)
        self.setup()
        logger.info(f"Agent '{self.name}' started")
        self._task = asyncio.create_task(self._run_loop())

    async def stop(self):
        """Stop the agent."""
        self._running = False
        self._status = "stopped"
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        if self._state_store is not None:
            await self._state_store.close()
        logger.info(f"Agent '{self.name}' stopped")

    async def _run_loop(self):
        """Main event processing loop."""
        while self._running:
            try:
                # Process events from all subscribed queues
                for queue in self._queues:
                    try:
                        event = queue.get_nowait()
                        await self.process_event(event)
                        self._events_processed += 1
                        self._last_heartbeat = datetime.utcnow()
                        await self._checkpoint_state()
                    except asyncio.QueueEmpty:
                        continue

                # Run periodic tasks
                await self.periodic_task()

                # Small sleep to prevent busy-waiting
                await asyncio.sleep(0.1)

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Agent '{self.name}' error: {e}")
                self._status = "error"
                await asyncio.sleep(1.0)
                self._status = "running"

    async def _checkpoint_state(self):
        """Persist light agent heartbeat/status to Redis when enabled."""
        if self._state_store is None:
            return
        try:
            key = f"vectormind:agent:{self.name}:state"
            await self._state_store.hset(
                key,
                mapping={
                    "status": self._status,
                    "events_processed": str(self._events_processed),
                    "last_heartbeat": self._last_heartbeat.isoformat(),
                },
            )
            await self._state_store.expire(key, 3600)
        except Exception as e:
            logger.error(f"Agent '{self.name}' checkpoint failed: {e}")

    def setup(self):
        """Optional setup hook called before the event loop starts."""
        pass

    @abstractmethod
    async def process_event(self, event: AgentEvent):
        """Process a single event from the message bus.

        Must be implemented by subclasses.
        """
        pass

    async def periodic_task(self):
        """Optional periodic task that runs each loop iteration.

        Override in subclasses for scheduled work.
        """
        pass
