"""Async Message Bus — Inter-agent communication layer.

Replaces Apache Kafka with asyncio queues for the hackathon MVP.
Typed event system with pub/sub pattern.
"""

from __future__ import annotations

import asyncio
import logging
from collections import defaultdict
from datetime import datetime
from typing import Callable, Optional

from ingestion.schema import AgentEvent

logger = logging.getLogger("vectorminds.messagebus")


class MessageBus:
    """In-memory async message bus for agent communication."""

    _instance: Optional["MessageBus"] = None

    @classmethod
    def get_instance(cls) -> "MessageBus":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self._subscribers: dict[str, list[asyncio.Queue]] = defaultdict(list)
        self._handlers: dict[str, list[Callable]] = defaultdict(list)
        self._event_log: list[AgentEvent] = []
        self._running = False

    def subscribe(self, topic: str) -> asyncio.Queue:
        """Subscribe to a topic and get a queue for receiving events.

        Args:
            topic: Event topic (e.g. 'ingestion.new_signal')

        Returns:
            asyncio.Queue that will receive events for this topic
        """
        queue = asyncio.Queue()
        self._subscribers[topic].append(queue)
        logger.debug(f"New subscriber for topic '{topic}'")
        return queue

    def register_handler(self, topic: str, handler: Callable):
        """Register a handler function for a topic.

        Args:
            topic: Event topic
            handler: Async callable that processes AgentEvent
        """
        self._handlers[topic].append(handler)
        logger.debug(f"Handler registered for topic '{topic}'")

    async def publish(self, event: AgentEvent):
        """Publish an event to all subscribers of its topic.

        Args:
            event: The event to publish
        """
        self._event_log.append(event)

        # Send to queue subscribers
        for queue in self._subscribers.get(event.topic, []):
            await queue.put(event)

        # Call registered handlers
        for handler in self._handlers.get(event.topic, []):
            try:
                await handler(event)
            except Exception as e:
                logger.error(f"Handler error for topic '{event.topic}': {e}")

        logger.debug(
            f"Published event: topic='{event.topic}', "
            f"source='{event.source_agent}'"
        )

    async def publish_simple(self, topic: str, source: str, payload: dict):
        """Convenience method to publish an event with minimal boilerplate."""
        event = AgentEvent(
            topic=topic,
            source_agent=source,
            timestamp=datetime.utcnow(),
            payload=payload,
        )
        await self.publish(event)

    def get_recent_events(self, topic: str = None, limit: int = 50) -> list[dict]:
        """Get recent events, optionally filtered by topic."""
        events = self._event_log
        if topic:
            events = [e for e in events if e.topic == topic]
        return [e.model_dump() for e in events[-limit:]]

    def clear(self):
        """Clear all subscriptions and event log."""
        self._subscribers.clear()
        self._handlers.clear()
        self._event_log.clear()
