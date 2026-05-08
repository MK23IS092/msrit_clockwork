"""Startup signal crawler using public startup/news RSS feeds."""

from __future__ import annotations

import logging
from datetime import datetime

import httpx
from bs4 import BeautifulSoup

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.startups")

STARTUP_FEEDS = [
    "https://techcrunch.com/category/startups/feed/",
    "https://www.ycombinator.com/blog/rss/",
]


class StartupCrawler:
    async def fetch_startup_signals(self, max_results: int = 20) -> list[ResearchSignal]:
        signals: list[ResearchSignal] = []
        try:
            async with httpx.AsyncClient(timeout=20.0, follow_redirects=True) as client:
                for feed_url in STARTUP_FEEDS:
                    resp = await client.get(feed_url)
                    if resp.status_code != 200:
                        continue
                    soup = BeautifulSoup(resp.text, "xml")
                    for item in soup.find_all(["item", "entry"]):
                        title = (item.find("title").text if item.find("title") else "").strip()
                        if not title:
                            continue
                        text = (
                            (item.find("description").text if item.find("description") else "")
                            .strip()
                            .lower()
                        )
                        if not any(
                            k in (title.lower() + " " + text)
                            for k in ("ai", "machine learning", "llm", "model")
                        ):
                            continue

                        link_node = item.find("link")
                        url = ""
                        if link_node:
                            url = link_node.get("href", "") or link_node.text or ""

                        signals.append(
                            ResearchSignal(
                                source=SignalSource.STARTUP,
                                source_id=f"startup:{abs(hash(url or title))}",
                                timestamp=datetime.utcnow(),
                                title=title,
                                raw_text=text or title,
                                authors=["startup-news"],
                                categories=["startup", "funding"],
                                url=url,
                                metadata={"feed_url": feed_url, "source_system": "rss"},
                            )
                        )
                        if len(signals) >= max_results:
                            return signals
            return signals
        except Exception as e:
            logger.error(f"Startup feed fetch failed: {e}")
            return []
