"""GitHub Crawler — Discovers trending ML/AI repositories.

Uses the GitHub REST API (unauthenticated / free) to find repositories
with high recent activity in machine learning topics.
"""

from __future__ import annotations

import logging
from datetime import datetime, timedelta
from typing import Optional

import httpx

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.github")

GITHUB_API = "https://api.github.com"


class GitHubCrawler:
    """Crawls GitHub for trending ML/AI repositories."""

    def __init__(
        self,
        languages: list[str] | None = None,
        max_results: int = 30,
        token: str = "",
    ):
        self.languages = languages or ["python"]
        self.max_results = max_results
        headers = {
            "Accept": "application/vnd.github.v3+json",
            "User-Agent": "VectorMinds-Research-Intelligence",
        }
        if token:
            headers["Authorization"] = f"token {token}"
        self.headers = headers

    async def fetch_trending_repos(
        self,
        topic: str = "machine-learning",
        days_back: int = 7,
        max_results: Optional[int] = None,
    ) -> list[ResearchSignal]:
        """Fetch trending ML repos from GitHub.

        Args:
            topic: GitHub topic to search (e.g. 'machine-learning', 'deep-learning')
            days_back: Look back window in days
            max_results: Override default max results

        Returns:
            List of ResearchSignal objects
        """
        n = max_results or self.max_results
        since_date = (datetime.utcnow() - timedelta(days=days_back)).strftime(
            "%Y-%m-%d"
        )

        # Build search query for trending ML repos
        lang_query = " ".join(f"language:{l}" for l in self.languages)
        query = f"topic:{topic} {lang_query} created:>{since_date} stars:>5"

        logger.info(f"Fetching GitHub repos: query='{query}', max={n}")

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.get(
                    f"{GITHUB_API}/search/repositories",
                    params={
                        "q": query,
                        "sort": "stars",
                        "order": "desc",
                        "per_page": min(n, 100),
                    },
                    headers=self.headers,
                )
                resp.raise_for_status()
                data = resp.json()

            signals = []
            for repo in data.get("items", [])[:n]:
                # Compute stars-per-day acceleration
                created = datetime.strptime(
                    repo["created_at"], "%Y-%m-%dT%H:%M:%SZ"
                )
                age_days = max((datetime.utcnow() - created).days, 1)
                stars_per_day = repo.get("stargazers_count", 0) / age_days

                signal = ResearchSignal(
                    source=SignalSource.GITHUB,
                    source_id=repo["full_name"],
                    timestamp=datetime.strptime(
                        repo["updated_at"], "%Y-%m-%dT%H:%M:%SZ"
                    ),
                    title=repo["full_name"],
                    raw_text=repo.get("description", "") or "",
                    authors=[repo["owner"]["login"]],
                    categories=repo.get("topics", []) or [],
                    url=repo["html_url"],
                    metadata={
                        "stars": repo.get("stargazers_count", 0),
                        "forks": repo.get("forks_count", 0),
                        "watchers": repo.get("watchers_count", 0),
                        "language": repo.get("language", ""),
                        "open_issues": repo.get("open_issues_count", 0),
                        "stars_per_day": round(stars_per_day, 2),
                        "license": (
                            repo.get("license", {}) or {}
                        ).get("spdx_id", ""),
                        "size_kb": repo.get("size", 0),
                        "created_at": repo["created_at"],
                    },
                )
                signals.append(signal)

            logger.info(f"Fetched {len(signals)} repos from GitHub")
            return signals

        except Exception as e:
            logger.error(f"GitHub fetch failed: {e}")
            return []

    async def fetch_repo_details(self, full_name: str) -> Optional[ResearchSignal]:
        """Fetch details for a specific repository."""
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                resp = await client.get(
                    f"{GITHUB_API}/repos/{full_name}",
                    headers=self.headers,
                )
                resp.raise_for_status()
                repo = resp.json()

            created = datetime.strptime(repo["created_at"], "%Y-%m-%dT%H:%M:%SZ")
            age_days = max((datetime.utcnow() - created).days, 1)
            stars_per_day = repo.get("stargazers_count", 0) / age_days

            return ResearchSignal(
                source=SignalSource.GITHUB,
                source_id=repo["full_name"],
                timestamp=datetime.strptime(
                    repo["updated_at"], "%Y-%m-%dT%H:%M:%SZ"
                ),
                title=repo["full_name"],
                raw_text=repo.get("description", "") or "",
                authors=[repo["owner"]["login"]],
                categories=repo.get("topics", []) or [],
                url=repo["html_url"],
                metadata={
                    "stars": repo.get("stargazers_count", 0),
                    "forks": repo.get("forks_count", 0),
                    "stars_per_day": round(stars_per_day, 2),
                    "language": repo.get("language", ""),
                },
            )
        except Exception as e:
            logger.error(f"GitHub repo fetch failed: {e}")
            return None
