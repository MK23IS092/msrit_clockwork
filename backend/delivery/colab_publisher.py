"""Publish generated VectorMinds notebooks to a real Google Colab URL.

Strategy: create or update a public GitHub Gist (using the existing
``GITHUB_TOKEN``) holding the ``.ipynb`` file. Colab can open any public Gist via
``https://colab.research.google.com/gist/<owner>/<gist_id>/<filename>.ipynb``.

If no token is available or GitHub is unreachable, ``publish_notebook`` returns
``None`` so the pipeline still works with an in-memory notebook.
"""

from __future__ import annotations

import logging
from typing import Optional

import httpx

import config

logger = logging.getLogger("vectorminds.colab")

GITHUB_API = "https://api.github.com"


def _auth_headers() -> dict:
    h = {"Accept": "application/vnd.github+json", "User-Agent": "VectorMinds/1.0"}
    if config.GITHUB_TOKEN:
        h["Authorization"] = f"Bearer {config.GITHUB_TOKEN}"
    return h


def _resolve_owner() -> Optional[str]:
    """Return the GitHub username for the configured ``GITHUB_TOKEN``."""
    if not config.GITHUB_TOKEN:
        return None
    try:
        with httpx.Client(timeout=10.0, headers=_auth_headers()) as client:
            r = client.get(f"{GITHUB_API}/user")
            if r.status_code != 200:
                logger.warning("GitHub /user returned %s: %s", r.status_code, r.text[:200])
                return None
            return (r.json() or {}).get("login")
    except Exception as e:
        logger.warning("GitHub /user failed: %s", e)
        return None


def publish_notebook(
    notebook_payload: dict,
    filename: str,
    description: str = "",
    public: bool = True,
) -> Optional[dict]:
    """Create a Gist holding a single ``.ipynb`` and return ``{owner, gist_id, colab_url, gist_url}``.

    Returns ``None`` if publishing is not possible (no token, network failure).
    """
    if not config.GITHUB_TOKEN:
        logger.info("GitHub token not configured; skipping Colab gist publish.")
        return None
    import json

    body = {
        "description": description or "VectorMinds generated training pipeline",
        "public": bool(public),
        "files": {filename: {"content": json.dumps(notebook_payload, ensure_ascii=False, indent=2)}},
    }
    try:
        with httpx.Client(timeout=20.0, headers=_auth_headers()) as client:
            r = client.post(f"{GITHUB_API}/gists", json=body)
            if r.status_code not in (200, 201):
                logger.warning("Gist create failed %s: %s", r.status_code, r.text[:300])
                return None
            data = r.json()
    except Exception as e:
        logger.warning("Gist create exception: %s", e)
        return None

    gist_id = data.get("id")
    owner_login = (data.get("owner") or {}).get("login") or _resolve_owner()
    gist_url = data.get("html_url") or (
        f"https://gist.github.com/{owner_login}/{gist_id}" if (owner_login and gist_id) else ""
    )
    if not gist_id:
        return None
    if not owner_login:
        owner_login = _resolve_owner() or "anonymous"
    colab_url = (
        f"https://colab.research.google.com/gist/{owner_login}/{gist_id}/{filename}"
    )
    logger.info("Published Colab gist %s for %s", gist_id, filename)
    return {
        "owner": owner_login,
        "gist_id": gist_id,
        "gist_url": gist_url,
        "colab_url": colab_url,
        "filename": filename,
    }
