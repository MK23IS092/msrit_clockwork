"""Patent crawler using PatentsView public API."""

from __future__ import annotations

import logging
from datetime import datetime

import httpx

from ingestion.schema import ResearchSignal, SignalSource

logger = logging.getLogger("vectorminds.patents")

PATENTSVIEW_URL = "https://api.patentsview.org/patents/query"


class PatentCrawler:
    """Fetches recent AI-related patents from public PatentsView API."""

    async def fetch_recent_patents(self, max_results: int = 20) -> list[ResearchSignal]:
        query = {
            "_or": [
                {"_text_any": {"patent_title": "artificial intelligence"}},
                {"_text_any": {"patent_title": "machine learning"}},
                {"_text_any": {"patent_abstract": "neural network"}},
                {"_text_any": {"patent_abstract": "transformer model"}},
            ]
        }
        fields = [
            "patent_number",
            "patent_title",
            "patent_date",
            "patent_abstract",
            "patent_type",
            "assignee_organization",
        ]

        try:
            async with httpx.AsyncClient(timeout=25.0) as client:
                resp = await client.post(
                    PATENTSVIEW_URL,
                    json={
                        "q": query,
                        "f": fields,
                        "o": {"per_page": max_results},
                    },
                )
                resp.raise_for_status()
                data = resp.json()
        except Exception as e:
            logger.error(f"PatentsView fetch failed: {e}")
            return []

        patents = data.get("patents", [])
        signals: list[ResearchSignal] = []
        for p in patents:
            patent_number = p.get("patent_number", "")
            title = p.get("patent_title", "") or ""
            abstract = p.get("patent_abstract", "") or ""
            assignees = p.get("assignees", []) or []
            orgs = [
                a.get("assignee_organization", "")
                for a in assignees
                if isinstance(a, dict)
            ]
            patent_date = p.get("patent_date", "")
            try:
                ts = datetime.strptime(patent_date, "%Y-%m-%d")
            except Exception:
                ts = datetime.utcnow()

            signals.append(
                ResearchSignal(
                    source=SignalSource.PATENT,
                    source_id=patent_number or title[:64],
                    timestamp=ts,
                    title=title or f"Patent {patent_number}",
                    raw_text=abstract[:4000],
                    authors=orgs[:5],
                    categories=["patent", "ai"],
                    url=(
                        f"https://patents.google.com/patent/{patent_number}"
                        if patent_number
                        else ""
                    ),
                    metadata={
                        "patent_number": patent_number,
                        "patent_type": p.get("patent_type", ""),
                        "assignees": orgs[:10],
                        "source_system": "patentsview",
                    },
                )
            )
        return signals
