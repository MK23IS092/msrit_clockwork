"""Print Telegram subscribers persisted in the configured database."""

from __future__ import annotations

import sys
from pathlib import Path

_BACKEND_ROOT = Path(__file__).resolve().parent.parent
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

from db.database import Database


def main() -> None:
    db = Database.get_instance()
    db.initialize()
    db.ensure_telegram_subscribers_table()
    ids = db.list_telegram_subscriber_ids()
    print(f"subscribers: {len(ids)}")
    for cid in ids:
        print(f"  - {cid}")
    db.close()


if __name__ == "__main__":
    main()
