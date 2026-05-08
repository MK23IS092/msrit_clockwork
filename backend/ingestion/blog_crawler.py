"""Blog crawler using public RSS feeds from AI labs."""

from __future__ import annotations

import logging
from datetime import datetime

import httpx
from bs4 import BeautifulSoup

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.blog")

FEEDS = [
    "https://openai.com/blog/rss.xml",
    "https://deepmind.google/discover/blog/rss.xml",
    "https://huggingface.co/blog/feed.xml",
]


class BlogCrawler:
    async def fetch_blog_signals(self, max_results: int = 20) -> list[ResearchSignal]:
        signals: list[ResearchSignal] = []
        try:
            async with httpx.AsyncClient(timeout=20.0, follow_redirects=True) as client:
                for feed_url in FEEDS:
                    resp = await client.get(feed_url)
                    if resp.status_code != 200:
                        continue
                    soup = BeautifulSoup(resp.text, "xml")
                    for item in soup.find_all(["item", "entry"]):
                        title = (item.find("title").text if item.find("title") else "").strip()
                        summary_node = item.find("description") or item.find("summary")
                        summary = (
                            summary_node.text.strip() if summary_node and summary_node.text else ""
                        )
                        link_node = item.find("link")
                        url = ""
                        if link_node:
                            url = link_node.get("href", "") or link_node.text or ""
                        date_node = item.find("pubDate") or item.find("updated")
                        ts = datetime.utcnow()
                        if date_node and date_node.text:
                            try:
                                ts = datetime.strptime(
                                    date_node.text[:25], "%a, %d %b %Y %H:%M:%S"
                                )
                            except Exception:
                                ts = datetime.utcnow()

                        if not title:
                            continue
                        signals.append(
                            ResearchSignal(
                                source=SignalSource.BLOG,
                                source_id=f"blog:{abs(hash(url or title))}",
                                timestamp=ts,
                                title=title,
                                raw_text=summary or title,
                                authors=["AI Blog"],
                                categories=["blog", "ai"],
                                url=url,
                                metadata={"feed_url": feed_url, "source_system": "rss"},
                            )
                        )
                        if len(signals) >= max_results:
                            return signals
            return signals
        except Exception as e:
            logger.error(f"Blog fetch failed: {e}")
            return []
