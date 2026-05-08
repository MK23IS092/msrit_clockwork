"""Ingestion Agent — Manages all data source crawlers.

Orchestrates arXiv and GitHub crawlers, monitors source health,
and publishes new research signals to the message bus.
"""

from __future__ import annotations

import asyncio
import logging
from datetime import datetime

from agents.base_agent import BaseAgent
from embeddings.engine import EmbeddingEngine
from embeddings.vector_store import VectorStore
from ingestion.arxiv_crawler import ArxivCrawler
from ingestion.blog_crawler import BlogCrawler
from ingestion.github_crawler import GitHubCrawler
from ingestion.patent_crawler import PatentCrawler
from ingestion.schema import AgentEvent, ResearchSignal, SignalSource
from ingestion.social_crawler import SocialCrawler
from ingestion.startup_crawler import StartupCrawler
import config

logger = logging.getLogger("vectormind.ingestion_agent")


class IngestionAgent(BaseAgent):
    """Agent that manages data ingestion from all sources."""

    def __init__(self):
        super().__init__("IngestionAgent")
        self.arxiv_crawler = ArxivCrawler(
            categories=config.ARXIV_CATEGORIES,
            max_results=config.ARXIV_MAX_RESULTS,
        )
        self.github_crawler = GitHubCrawler(
            languages=config.GITHUB_TRENDING_LANGUAGES,
            max_results=config.GITHUB_MAX_RESULTS,
            token=config.GITHUB_TOKEN,
        )
        self.patent_crawler = PatentCrawler()
        self.startup_crawler = StartupCrawler()
        self.social_crawler = SocialCrawler()
        self.blog_crawler = BlogCrawler()
        self.embedding_engine = EmbeddingEngine.get_instance()
        self.vector_store = VectorStore.get_instance()

        self._last_ingestion = None
        self._ingestion_count = 0
        self._source_health = {
            "arxiv": {"status": "healthy", "last_success": None, "failures": 0},
            "github": {"status": "healthy", "last_success": None, "failures": 0},
            "patents": {"status": "healthy", "last_success": None, "failures": 0},
            "startups": {"status": "healthy", "last_success": None, "failures": 0},
            "social": {"status": "healthy", "last_success": None, "failures": 0},
            "blog": {"status": "healthy", "last_success": None, "failures": 0},
        }

    def setup(self):
        self.subscribe("ingestion.trigger")

    async def process_event(self, event: AgentEvent):
        """Handle ingestion trigger events."""
        if event.topic == "ingestion.trigger":
            category = event.payload.get("category")
            source = event.payload.get("source", "all")
            await self.run_ingestion(source=source, category=category)

    async def run_ingestion(
        self,
        source: str = "all",
        category: str = None,
    ) -> list[ResearchSignal]:
        """Run a full ingestion cycle.

        Args:
            source: 'arxiv', 'github', or 'all'
            category: Optional arXiv category filter

        Returns:
            List of new research signals ingested
        """
        self._status = "ingesting"
        all_signals = []

        # Fetch from arXiv
        if source in ("all", "arxiv"):
            try:
                arxiv_signals = await self.arxiv_crawler.fetch_recent_papers(
                    category=category
                )
                all_signals.extend(arxiv_signals)
                self._source_health["arxiv"]["status"] = "healthy"
                self._source_health["arxiv"]["last_success"] = (
                    datetime.utcnow().isoformat()
                )
                self._source_health["arxiv"]["failures"] = 0
            except Exception as e:
                logger.error(f"arXiv ingestion failed: {e}")
                self._source_health["arxiv"]["failures"] += 1
                if self._source_health["arxiv"]["failures"] >= 3:
                    self._source_health["arxiv"]["status"] = "unhealthy"

        # Fetch from GitHub
        if source in ("all", "github"):
            try:
                for topic in ["machine-learning", "deep-learning", "transformers"]:
                    github_signals = await self.github_crawler.fetch_trending_repos(
                        topic=topic
                    )
                    all_signals.extend(github_signals)
                self._source_health["github"]["status"] = "healthy"
                self._source_health["github"]["last_success"] = (
                    datetime.utcnow().isoformat()
                )
                self._source_health["github"]["failures"] = 0
            except Exception as e:
                logger.error(f"GitHub ingestion failed: {e}")
                self._source_health["github"]["failures"] += 1
                if self._source_health["github"]["failures"] >= 3:
                    self._source_health["github"]["status"] = "unhealthy"
        # Fetch from Patents
        if source in ("all", "patents"):
            try:
                patent_signals = []
                if config.ENABLE_PATENTS_REAL:
                    patent_signals = await self.patent_crawler.fetch_recent_patents(
                        max_results=config.PATENTS_MAX_RESULTS
                    )
                if not patent_signals and config.ALLOW_SIMULATED_SOURCES:
                    patent_signals = [
                        ResearchSignal(
                            source=SignalSource.PATENT,
                            source_id="US-2026-0012345",
                            title="Distributed Multi-Agent Reasoning via Sparse Attention Meshes",
                            authors=["VectorMind R&D"],
                            raw_text="A method and system for optimizing multi-agent reasoning in decentralized networks...",
                            url="https://patents.google.com/patent/US20260012345A1",
                            metadata={
                                "patent_number": "US20260012345",
                                "assignee": "Samsung R&D",
                                "simulated": True,
                            },
                        )
                    ]
                all_signals.extend(patent_signals)
                self._source_health["patents"]["status"] = "healthy"
                self._source_health["patents"]["last_success"] = datetime.utcnow().isoformat()
            except Exception as e:
                logger.error(f"Patent ingestion failed: {e}")
                self._source_health["patents"]["status"] = "unhealthy"
                if config.ALLOW_SIMULATED_SOURCES:
                    all_signals.extend(self._simulated_patent_signals())

        # Fetch from Startup Ecosystem
        if source in ("all", "startups"):
            try:
                startup_signals = []
                if config.ENABLE_STARTUPS_REAL:
                    startup_signals = await self.startup_crawler.fetch_startup_signals(
                        max_results=config.STARTUPS_MAX_RESULTS
                    )
                if not startup_signals and config.ALLOW_SIMULATED_SOURCES:
                    startup_signals = [
                        ResearchSignal(
                            source=SignalSource.STARTUP,
                            source_id="YC-W26-VECT",
                            title="Seed Funding: NeuroForge AI (YC W26)",
                            authors=["YC"],
                            raw_text="NeuroForge AI raises $5M to commercialize sparse attention architectures.",
                            url="https://ycombinator.com/companies/neuroforge",
                            metadata={"funding_round": "Seed", "amount": "$5M", "simulated": True},
                        )
                    ]
                all_signals.extend(startup_signals)
                self._source_health["startups"]["status"] = "healthy"
                self._source_health["startups"]["last_success"] = datetime.utcnow().isoformat()
            except Exception:
                self._source_health["startups"]["status"] = "unhealthy"
                if config.ALLOW_SIMULATED_SOURCES:
                    all_signals.extend(self._simulated_startup_signals())

        # Fetch from Social (Hacker News)
        if source in ("all", "social"):
            try:
                social_signals = []
                if config.ENABLE_SOCIAL_REAL:
                    social_signals = await self.social_crawler.fetch_hn_signals(
                        max_results=config.SOCIAL_MAX_RESULTS
                    )
                if not social_signals and config.ALLOW_SIMULATED_SOURCES:
                    social_signals = [
                        ResearchSignal(
                            source=SignalSource.SOCIAL,
                            source_id="HN-4123456",
                            title="Show HN: VectorMind - Open Source Research Intelligence",
                            authors=["hn_user"],
                            raw_text="The first agentic platform for autonomous research...",
                            url="https://news.ycombinator.com/item?id=4123456",
                            metadata={"upvotes": 450, "comments": 82, "simulated": True},
                        )
                    ]
                all_signals.extend(social_signals)
                self._source_health["social"]["status"] = "healthy"
                self._source_health["social"]["last_success"] = datetime.utcnow().isoformat()
            except Exception:
                self._source_health["social"]["status"] = "unhealthy"
                if config.ALLOW_SIMULATED_SOURCES:
                    all_signals.extend(self._simulated_social_signals())

        # Fetch from Blogs (labs + ecosystem)
        if source in ("all", "blog"):
            try:
                blog_signals = []
                if config.ENABLE_BLOG_REAL:
                    blog_signals = await self.blog_crawler.fetch_blog_signals(
                        max_results=config.BLOG_MAX_RESULTS
                    )
                all_signals.extend(blog_signals)
                self._source_health["blog"]["status"] = "healthy"
                self._source_health["blog"]["last_success"] = datetime.utcnow().isoformat()
            except Exception:
                self._source_health["blog"]["status"] = "unhealthy"

        # Embed all signals
        if all_signals:
            texts = [
                f"{s.title}. {s.raw_text}" for s in all_signals
            ]
            embeddings = self.embedding_engine.embed_batch(texts)

            for signal, embedding in zip(all_signals, embeddings):
                signal.embedding = embedding

                # Compute novelty score
                signal.novelty_score = self.vector_store.compute_novelty_score(
                    embedding
                )

                # Store in vector store
                self.vector_store.upsert_signal(
                    signal_id=signal.id,
                    embedding=embedding,
                    payload={
                        "id": signal.id,
                        "source": signal.source.value,
                        "source_id": signal.source_id,
                        "title": signal.title,
                        "raw_text": signal.raw_text[:500],
                        "authors": signal.authors[:5],
                        "categories": signal.categories,
                        "url": signal.url,
                        "novelty_score": signal.novelty_score,
                        "timestamp": signal.timestamp.isoformat(),
                        "metadata": signal.metadata,
                    },
                )

                # Publish event for each new signal
                await self.publish(
                    "ingestion.new_signal",
                    {
                        "signal_id": signal.id,
                        "source": signal.source.value,
                        "title": signal.title,
                        "novelty_score": signal.novelty_score,
                    },
                )

        self._ingestion_count += len(all_signals)
        self._last_ingestion = datetime.utcnow()
        self._status = "running"

        logger.info(
            f"Ingestion complete: {len(all_signals)} signals "
            f"(total: {self._ingestion_count})"
        )

        return all_signals

    def get_health(self) -> dict:
        health = super().get_health()
        health.update({
            "source_health": self._source_health,
            "total_ingested": self._ingestion_count,
            "last_ingestion": (
                self._last_ingestion.isoformat() if self._last_ingestion else None
            ),
        })
        return health

    def _simulated_patent_signals(self) -> list[ResearchSignal]:
        return [
            ResearchSignal(
                source=SignalSource.PATENT,
                source_id="US-2026-0012345",
                title="Distributed Multi-Agent Reasoning via Sparse Attention Meshes",
                authors=["VectorMind R&D"],
                raw_text="A method and system for optimizing multi-agent reasoning in decentralized networks...",
                url="https://patents.google.com/patent/US20260012345A1",
                metadata={
                    "patent_number": "US20260012345",
                    "assignee": "Samsung R&D",
                    "simulated": True,
                },
            )
        ]

    def _simulated_startup_signals(self) -> list[ResearchSignal]:
        return [
            ResearchSignal(
                source=SignalSource.STARTUP,
                source_id="YC-W26-VECT",
                title="Seed Funding: NeuroForge AI (YC W26)",
                authors=["YC"],
                raw_text="NeuroForge AI raises $5M to commercialize sparse attention architectures.",
                url="https://ycombinator.com/companies/neuroforge",
                metadata={"funding_round": "Seed", "amount": "$5M", "simulated": True},
            )
        ]

    def _simulated_social_signals(self) -> list[ResearchSignal]:
        return [
            ResearchSignal(
                source=SignalSource.SOCIAL,
                source_id="HN-4123456",
                title="Show HN: VectorMind - Open Source Research Intelligence",
                authors=["hn_user"],
                raw_text="The first agentic platform for autonomous research...",
                url="https://news.ycombinator.com/item?id=4123456",
                metadata={"upvotes": 450, "comments": 82, "simulated": True},
            )
        ]
