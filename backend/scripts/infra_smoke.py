import asyncio
import sys
from pathlib import Path

_BACKEND_ROOT = Path(__file__).resolve().parent.parent
if str(_BACKEND_ROOT) not in sys.path:
    sys.path.insert(0, str(_BACKEND_ROOT))

import config
import redis.asyncio as redis_async
from agents.message_bus import MessageBus
from db.database import Database


async def main():
    db = Database.get_instance()
    db.initialize()
    print("db_backend", config.DB_BACKEND)
    print("db_signals_count", db.get_signals_count())

    redis_client = redis_async.from_url(config.REDIS_URL, decode_responses=True)
    print("redis_ping", await redis_client.ping())
    await redis_client.close()

    bus = MessageBus.get_instance()
    await bus.start()
    await bus.publish_simple("infra.check", "infra_smoke", {"ok": True})
    await bus.stop()
    print("kafka_mirror", "ok")


if __name__ == "__main__":
    asyncio.run(main())
