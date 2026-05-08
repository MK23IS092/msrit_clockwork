"""Memory Agent — Long-horizon context and personalization.

Stores user interaction history, manages feedback signals,
and provides personalized scoring adjustments.
"""

from __future__ import annotations

import logging
from collections import defaultdict
from datetime import datetime
from typing import Optional

from agents.base_agent import BaseAgent
from ingestion.schema import AgentEvent, UserFeedback

logger = logging.getLogger("vectorminds.memory_agent")


class MemoryAgent(BaseAgent):
    """Agent that maintains persistent context and user preferences."""

    def __init__(self):
        super().__init__("MemoryAgent")

        # Working memory (replaces Redis)
        self.working_memory: dict[str, dict] = {}

        # Episodic memory — interaction history
        self.interaction_history: list[dict] = []

        # User preferences (learned from feedback)
        self.user_preferences: dict[str, float] = defaultdict(float)

        # Feedback store
        self.feedback_log: list[UserFeedback] = []

        # Blueprint cache
        self.blueprint_cache: dict[str, dict] = {}

        # Pipeline portfolio
        self.pipeline_portfolio: list[dict] = []

    def setup(self):
        self.subscribe("reasoning.scored")
        self.subscribe("delivery.feedback")

    async def process_event(self, event: AgentEvent):
        """Process scored signals and feedback events."""
        if event.topic == "reasoning.scored":
            # Store in episodic memory
            self.interaction_history.append({
                "type": "signal_scored",
                "timestamp": datetime.utcnow().isoformat(),
                "data": event.payload,
            })
            # Update working memory
            signal_id = event.payload.get("signal_id", "")
            if signal_id:
                self.working_memory[signal_id] = event.payload

        elif event.topic == "delivery.feedback":
            await self._process_feedback(event.payload)

    async def _process_feedback(self, payload: dict):
        """Process user feedback to update preferences."""
        feedback = UserFeedback(
            target_id=payload.get("target_id", ""),
            target_type=payload.get("target_type", "trend"),
            action=payload.get("action", "upvote"),
        )
        self.feedback_log.append(feedback)

        # Update user preferences based on feedback
        categories = payload.get("categories", [])
        weight = 1.0 if feedback.action == "upvote" else -0.5

        for category in categories:
            self.user_preferences[category] += weight

        logger.info(
            f"Feedback recorded: {feedback.action} on "
            f"{feedback.target_type}/{feedback.target_id}"
        )

    def get_preference_weight(self, categories: list[str]) -> float:
        """Get a personalization weight based on user preferences.

        Args:
            categories: List of categories for a signal/trend

        Returns:
            Weight multiplier (>1 = preferred, <1 = less preferred)
        """
        if not self.user_preferences:
            return 1.0

        scores = [self.user_preferences.get(c, 0) for c in categories]
        if not scores:
            return 1.0

        avg_pref = sum(scores) / len(scores)
        # Normalize to a multiplier around 1.0
        return max(0.5, min(1.5, 1.0 + avg_pref * 0.1))

    def store_blueprint(self, blueprint_id: str, data: dict):
        """Cache a generated blueprint."""
        self.blueprint_cache[blueprint_id] = {
            **data,
            "stored_at": datetime.utcnow().isoformat(),
        }

    def get_blueprint(self, blueprint_id: str) -> Optional[dict]:
        """Retrieve a cached blueprint."""
        return self.blueprint_cache.get(blueprint_id)

    def store_pipeline(self, pipeline_data: dict):
        """Add a pipeline to the portfolio."""
        self.pipeline_portfolio.append({
            **pipeline_data,
            "stored_at": datetime.utcnow().isoformat(),
        })

    def get_stats(self) -> dict:
        """Get memory agent statistics."""
        return {
            "working_memory_size": len(self.working_memory),
            "interaction_count": len(self.interaction_history),
            "feedback_count": len(self.feedback_log),
            "blueprints_cached": len(self.blueprint_cache),
            "pipelines_stored": len(self.pipeline_portfolio),
            "preference_categories": len(self.user_preferences),
            "upvotes": sum(
                1 for f in self.feedback_log if f.action == "upvote"
            ),
            "downvotes": sum(
                1 for f in self.feedback_log if f.action == "downvote"
            ),
        }

    def get_health(self) -> dict:
        health = super().get_health()
        health.update(self.get_stats())
        return health
