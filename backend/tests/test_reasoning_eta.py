from agents.reasoning_agent import ReasoningAgent


def test_estimate_eta_uses_6_12_24_buckets():
    agent = ReasoningAgent()
    assert agent._estimate_eta(0.9) == 6
    assert agent._estimate_eta(0.6) == 12
    assert agent._estimate_eta(0.2) == 24
