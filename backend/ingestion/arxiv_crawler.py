"""arXiv Crawler — Ingests research papers from arXiv API.

Fetches recent papers by category, parses metadata, and returns
normalized ResearchSignal objects.
"""

from __future__ import annotations

import logging
from datetime import datetime
from typing import Optional

import arxiv
import httpx

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.arxiv")


class ArxivCrawler:
    """Crawls arXiv for recent AI/ML papers."""

    def __init__(self, categories: list[str], max_results: int = 50):
        self.categories = categories
        self.max_results = max_results
        self.client = arxiv.Client()

    async def fetch_recent_papers(
        self,
        query: Optional[str] = None,
        category: Optional[str] = None,
        max_results: Optional[int] = None,
    ) -> list[ResearchSignal]:
        """Fetch recent papers from arXiv.

        Args:
            query: Optional search query (e.g. 'transformer attention')
            category: Specific arXiv category (e.g. 'cs.LG')
            max_results: Override default max results

        Returns:
            List of ResearchSignal objects
        """
        n = max_results or self.max_results

        # Build search query
        if query:
            search_query = query
        elif category:
            search_query = f"cat:{category}"
        else:
            # Search across all configured categories
            cat_query = " OR ".join(f"cat:{c}" for c in self.categories)
            search_query = cat_query

        logger.info(f"Fetching arXiv papers: query='{search_query}', max={n}")

        try:
            search = arxiv.Search(
                query=search_query,
                max_results=n,
                sort_by=arxiv.SortCriterion.SubmittedDate,
                sort_order=arxiv.SortOrder.Descending,
            )

            signals = []
            for result in self.client.results(search):
                signal = ResearchSignal(
                    source=SignalSource.ARXIV,
                    source_id=result.entry_id,
                    timestamp=result.published or datetime.utcnow(),
                    title=result.title.strip().replace("\n", " "),
                    raw_text=result.summary.strip().replace("\n", " "),
                    authors=[a.name for a in result.authors],
                    categories=[c for c in result.categories],
                    url=result.entry_id,
                    metadata={
                        "pdf_url": result.pdf_url or "",
                        "primary_category": result.primary_category,
                        "comment": result.comment or "",
                        "journal_ref": result.journal_ref or "",
                        "doi": result.doi or "",
                        "updated": (
                            result.updated.isoformat() if result.updated else ""
                        ),
                    },
                )
                signals.append(signal)

            logger.info(f"Fetched {len(signals)} papers from arXiv")
            return signals

        except Exception as e:
            logger.error(f"arXiv fetch failed: {e}")
            return []

    async def fetch_by_ids(self, paper_ids: list[str]) -> list[ResearchSignal]:
        """Fetch specific papers by their arXiv IDs."""
        try:
            search = arxiv.Search(id_list=paper_ids)
            signals = []
            for result in self.client.results(search):
                signal = ResearchSignal(
                    source=SignalSource.ARXIV,
                    source_id=result.entry_id,
                    timestamp=result.published or datetime.utcnow(),
                    title=result.title.strip().replace("\n", " "),
                    raw_text=result.summary.strip().replace("\n", " "),
                    authors=[a.name for a in result.authors],
                    categories=list(result.categories),
                    url=result.entry_id,
                    metadata={
                        "pdf_url": result.pdf_url or "",
                        "primary_category": result.primary_category,
                    },
                )
                signals.append(signal)
            return signals
        except Exception as e:
            logger.error(f"arXiv ID fetch failed: {e}")
            return []
