import asyncio

from agents.retraining_agent import RetrainingAgent


def test_trigger_retraining_updates_status_and_version_event():
    agent = RetrainingAgent()
    published = []

    async def fake_publish(topic, payload):
        published.append((topic, payload))

    agent.publish = fake_publish  # type: ignore[method-assign]
    asyncio.run(
        agent.trigger_retraining(candidate_metrics={"accuracy": 0.82, "f1": 0.79, "latency_ms": 140})
    )

    assert agent.status == "monitoring"
    assert agent.last_retraining is not None
    assert published
    assert published[0][0] == "model.promoted"
    assert "new_version" in published[0][1]


def test_trigger_retraining_rejects_candidate_when_quality_gates_fail():
    agent = RetrainingAgent()
    published = []

    async def fake_publish(topic, payload):
        published.append((topic, payload))

    agent.publish = fake_publish  # type: ignore[method-assign]
    asyncio.run(
        agent.trigger_retraining(candidate_metrics={"accuracy": 0.72, "f1": 0.70, "latency_ms": 260})
    )

    assert agent.status == "monitoring"
    assert agent.last_retraining is not None
    assert published
    assert published[0][0] == "model.retraining_failed"
    assert published[0][1]["reasons"]
