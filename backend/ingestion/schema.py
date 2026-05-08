"""Research Signal canonical schema.

All ingested data is normalized into this schema regardless of source.
"""

from __future__ import annotations

import uuid
from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class SignalSource(str, Enum):
    ARXIV = "arxiv"
    GITHUB = "github"
    PATENT = "patent"
    STARTUP = "startup"
    SOCIAL = "social"
    BLOG = "blog"


class ResearchSignal(BaseModel):
    """Canonical Research Signal — the universal data unit in VectorMinds."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    source: SignalSource
    source_id: str = ""  # e.g. arXiv paper ID, GitHub repo full_name
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    title: str
    raw_text: str  # abstract or description
    authors: list[str] = Field(default_factory=list)
    categories: list[str] = Field(default_factory=list)
    url: str = ""
    embedding: list[float] = Field(default_factory=list)
    novelty_score: float = 0.0
    impact_score: float = 0.0
    metadata: dict = Field(default_factory=dict)

    # Computed fields (populated by Reasoning Agent)
    technique_name: str = ""
    technical_brief: str = ""
    cross_source_signals: dict = Field(default_factory=dict)


class TrendEntry(BaseModel):
    """A ranked entry in the Trend Leaderboard."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    rank: int = 0
    technique_name: str
    description: str = ""
    emergence_score: float = 0.0
    novelty_score: float = 0.0
    impact_score: float = 0.0
    mainstream_eta_months: int = 12
    confidence: float = 0.0
    source_signals: dict = Field(default_factory=dict)
    competitive_landscape: list[str] = Field(default_factory=list)
    risk_factors: list[str] = Field(default_factory=list)
    related_techniques: list[str] = Field(default_factory=list)
    paper_count: int = 0
    github_stars: int = 0
    first_seen: datetime = Field(default_factory=datetime.utcnow)
    last_updated: datetime = Field(default_factory=datetime.utcnow)
    signal_ids: list[str] = Field(default_factory=list)


class ProductBlueprint(BaseModel):
    """A complete product blueprint generated from a high-scoring technique."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    technique_name: str
    trend_id: str = ""
    created_at: datetime = Field(default_factory=datetime.utcnow)

    problem_statement: str = ""
    market_size: str = ""
    technical_implementation: str = ""
    architecture_decisions: list[str] = Field(default_factory=list)
    differentiation_strategy: str = ""
    dataset_requirements: str = ""
    go_to_market: str = ""
    risk_assessment: str = ""
    first_90_day_milestones: list[str] = Field(default_factory=list)
    suggested_stack: list[str] = Field(default_factory=list)


class MLPipeline(BaseModel):
    """A generated ML training pipeline."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    technique_name: str
    blueprint_id: str = ""
    task_type: str = ""  # one of SUPPORTED_TASK_CATEGORIES
    created_at: datetime = Field(default_factory=datetime.utcnow)

    dataset_name: str = ""
    dataset_source: str = ""
    model_architecture: str = ""
    notebook_content: str = ""
    colab_url: str = ""
    status: str = "generated"  # generated, training, completed, failed
    metrics: dict = Field(default_factory=dict)
    model_card: str = ""


class AgentEvent(BaseModel):
    """Event message passed between agents via the message bus."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    topic: str
    source_agent: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    payload: dict = Field(default_factory=dict)


class UserFeedback(BaseModel):
    """User feedback on a prediction or blueprint."""

    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    target_id: str  # trend_id or blueprint_id
    target_type: str  # "trend" or "blueprint"
    action: str  # "upvote" or "downvote"
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class PlatformStats(BaseModel):
    """Live platform statistics for the dashboard."""

    total_papers: int = 0
    total_github_repos: int = 0
    total_signals: int = 0
    active_trends: int = 0
    blueprints_generated: int = 0
    pipelines_launched: int = 0
    avg_novelty_score: float = 0.0
    novelty_distribution: list[float] = Field(default_factory=list)
    agents_status: dict = Field(default_factory=dict)
    last_ingestion: Optional[datetime] = None
