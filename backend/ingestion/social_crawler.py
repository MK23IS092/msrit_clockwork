"""Social signal crawler (Hacker News public API)."""

from __future__ import annotations

import logging
from datetime import datetime

import httpx

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.social")

HN_TOP = "https://hacker-news.firebaseio.com/v0/topstories.json"
HN_ITEM = "https://hacker-news.firebaseio.com/v0/item/{item_id}.json"


class SocialCrawler:
    async def fetch_hn_signals(self, max_results: int = 30) -> list[ResearchSignal]:
        try:
            async with httpx.AsyncClient(timeout=25.0) as client:
                ids_resp = await client.get(HN_TOP)
                ids_resp.raise_for_status()
                top_ids = (ids_resp.json() or [])[: max_results * 2]

                signals: list[ResearchSignal] = []
                for item_id in top_ids:
                    item_resp = await client.get(HN_ITEM.format(item_id=item_id))
                    if item_resp.status_code != 200:
                        continue
                    item = item_resp.json() or {}
                    if item.get("type") != "story":
                        continue
                    title = (item.get("title") or "").lower()
                    text = (item.get("text") or "").lower()
                    if not any(
                        k in (title + " " + text)
                        for k in ("ai", "llm", "machine learning", "transformer", "agent")
                    ):
                        continue

                    ts = datetime.utcfromtimestamp(item.get("time", 0) or 0)
                    signals.append(
                        ResearchSignal(
                            source=SignalSource.SOCIAL,
                            source_id=f"HN-{item_id}",
                            timestamp=ts if ts.year > 2000 else datetime.utcnow(),
                            title=item.get("title", "HN story"),
                            raw_text=item.get("text", "") or item.get("title", ""),
                            authors=[item.get("by", "hn_user")],
                            categories=["hacker-news", "social"],
                            url=item.get("url", f"https://news.ycombinator.com/item?id={item_id}"),
                            metadata={
                                "hn_id": item_id,
                                "score": item.get("score", 0),
                                "descendants": item.get("descendants", 0),
                                "source_system": "hackernews",
                            },
                        )
                    )
                    if len(signals) >= max_results:
                        break
                return signals
        except Exception as e:
            logger.error(f"HN fetch failed: {e}")
            return []
