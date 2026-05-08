"""Resolve TELEGRAM_CHAT_ID from recent messages to your bot.

Run from the backend directory:

    python scripts/discover_telegram_chat.py

Ensure TELEGRAM_BOT_TOKEN is set, then send any message (e.g. /start) to your bot in Telegram.
"""

from __future__ import annotations

import sys
from pathlib import Path

_BACKEND_ROOT = Path(__file__).resolve().parent.parent
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

import config
from delivery.telegram_bot import peek_chat_id_from_pending_updates


def main() -> None:
    token = (config.TELEGRAM_BOT_TOKEN or "").strip()
    if not token:
        print("Set TELEGRAM_BOT_TOKEN in backend/.env first.", file=sys.stderr)
        sys.exit(1)
    cid = peek_chat_id_from_pending_updates(token)
    if cid:
        print(f"TELEGRAM_CHAT_ID={cid}")
        print(f"Add to backend/.env: TELEGRAM_CHAT_ID={cid}")
    else:
        print(
            "No pending updates. Message your bot in Telegram, then run this script again."
        )
        sys.exit(2)


if __name__ == "__main__":
    main()
