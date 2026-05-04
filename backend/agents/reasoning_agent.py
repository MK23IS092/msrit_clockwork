"""Reasoning Agent — Core intelligence agent of VectorMinds.

Performs cross-source correlation, novelty scoring, impact prediction,
and chain-of-thought summarization using LLM.
"""

from __future__ import annotations

import json
import logging
import random
from datetime import datetime
from typing import Optional

import httpx

from agents.base_agent import BaseAgent
from embeddings.vector_store import VectorStore
from ingestion.schema import AgentEvent, TrendEntry
import config

logger = logging.getLogger("vectorminds.reasoning_agent")


# ─── Mock LLM Responses (for demo when no API key) ───────────
MOCK_BRIEFS = [
    {
        "technique": "State Space Models (Mamba)",
        "brief": "A new class of sequence models that achieve transformer-quality results with linear-time complexity. Mamba introduces selective state spaces that can dynamically filter information, enabling 5x faster inference and the ability to process sequences of unlimited length. Key insight: by making the state space parameters input-dependent, the model can selectively remember or forget information.",
        "impact": "Could fundamentally reshape the efficiency of language models, making GPT-4-class models runnable on consumer hardware.",
        "competes_with": ["Transformers", "RWKV", "Hyena"],
    },
    {
        "technique": "Mixture of Experts (MoE) Scaling",
        "brief": "Sparse MoE architectures activate only a fraction of parameters per token, enabling models with trillions of parameters to run at the cost of much smaller dense models. Recent innovations in expert routing and load balancing have made MoE practical for production deployment.",
        "impact": "Enables 10x parameter scaling without proportional compute increase. Key enabler for next-generation foundation models.",
        "competes_with": ["Dense Transformers", "Distillation", "Pruning"],
    },
    {
        "technique": "Retrieval-Augmented Generation (RAG) 2.0",
        "brief": "Next-generation RAG systems move beyond simple vector similarity search. They incorporate graph-based retrieval, multi-hop reasoning chains, and learned retrieval strategies that adapt to the query complexity. Self-RAG and Corrective RAG add self-reflection loops that verify retrieved context relevance.",
        "impact": "Dramatically reduces hallucination in production LLM applications. Enables enterprise AI deployment with verifiable sources.",
        "competes_with": ["Fine-tuning", "In-context Learning", "Knowledge Graphs"],
    },
    {
        "technique": "Diffusion Transformers (DiT)",
        "brief": "Replacing the U-Net backbone in diffusion models with transformer architectures. DiT achieves state-of-the-art image generation quality while being more scalable and amenable to the same scaling laws that power language models.",
        "impact": "Unifies the image and language generation paradigms under a single architecture family, enabling multimodal foundation models.",
        "competes_with": ["U-Net Diffusion", "GANs", "Autoregressive Image Models"],
    },
    {
        "technique": "Constitutional AI & RLHF Alternatives",
        "brief": "New alignment approaches that reduce dependence on expensive human feedback. DPO (Direct Preference Optimization) eliminates the reward model entirely. Constitutional AI uses AI-generated critiques to self-improve. KTO (Kahneman-Tversky Optimization) requires only binary good/bad labels.",
        "impact": "Democratizes model alignment — any team can align models without a large annotation workforce.",
        "competes_with": ["RLHF", "PPO", "Manual Prompt Engineering"],
    },
    {
        "technique": "Multi-Agent LLM Systems",
        "brief": "Frameworks where multiple LLM instances collaborate as specialized agents to solve complex tasks. Each agent has defined roles, tools, and memory. Key innovations: hierarchical planning agents, critic/validator agents, and shared workspace protocols.",
        "impact": "Moves AI from single-turn Q&A to autonomous multi-step problem solving. Foundation for AI software engineers and research assistants.",
        "competes_with": ["Single-agent CoT", "Function Calling", "Manual Workflows"],
    },
    {
        "technique": "LoRA & Parameter-Efficient Fine-Tuning",
        "brief": "Low-Rank Adaptation enables fine-tuning large models by training only small adapter matrices. QLoRA extends this with 4-bit quantization, enabling fine-tuning of 65B models on a single GPU. DoRA and LoRA+ improve convergence and final quality.",
        "impact": "Every organization can now customize foundation models for their domain at minimal cost. Enables private, specialized AI.",
        "competes_with": ["Full Fine-tuning", "Prompt Tuning", "In-context Learning"],
    },
    {
        "technique": "Vision-Language Models (VLMs)",
        "brief": "Models that natively understand both images and text, enabling visual question answering, image-grounded dialogue, and document understanding. LLaVA, Qwen-VL, and GPT-4V demonstrate that vision encoders can be efficiently fused with language models.",
        "impact": "Unlocks multimodal AI applications: automated document processing, visual inspection, accessibility tools, and creative design assistants.",
        "competes_with": ["OCR + LLM Pipelines", "CLIP", "Specialized Vision Models"],
    },
    {
        "technique": "Structured Output Generation",
        "brief": "Constrained decoding techniques that guarantee LLM outputs conform to a specified schema (JSON, SQL, code). Grammar-guided generation and token masking ensure 100% format compliance without post-processing.",
        "impact": "Makes LLMs reliable for production software integration. Eliminates the 'parsing problem' that plagues LLM-powered applications.",
        "competes_with": ["Regex Post-processing", "Retry Loops", "Fine-tuning for Format"],
    },
    {
        "technique": "Speculative Decoding & Inference Optimization",
        "brief": "Techniques that accelerate LLM inference without quality loss. Speculative decoding uses a small draft model to propose tokens that a large model verifies in parallel. Combined with KV-cache optimization, flash attention, and quantization, these achieve 3-5x speedup.",
        "impact": "Makes large model deployment economically viable. Reduces per-token cost to enable real-time conversational AI at scale.",
        "competes_with": ["Model Distillation", "Pruning", "Smaller Models"],
    },
]


class ReasoningAgent(BaseAgent):
    """Agent that performs cross-source analysis, scoring, and summarization."""

    def __init__(self):
        super().__init__("ReasoningAgent")
        self.vector_store = VectorStore.get_instance()
        self.trends: dict[str, TrendEntry] = {}
        self._mock_brief_idx = 0

    def setup(self):
        self.subscribe("ingestion.new_signal")

    async def process_event(self, event: AgentEvent):
        """Process new research signals — score and cluster them."""
        if event.topic == "ingestion.new_signal":
            signal_data = event.payload
            logger.debug(
                f"Processing signal: {signal_data.get('title', 'unknown')}"
            )
            # Impact scoring happens in batch via analyze_trends()

    async def analyze_trends(self) -> list[TrendEntry]:
        """Analyze all stored signals and produce a ranked trend leaderboard.

        This is the core intelligence function that:
        1. Clusters related signals
        2. Scores each cluster for emergence and impact
        3. Generates technical briefs
        4. Returns ranked trends

        Returns:
            Sorted list of TrendEntry objects
        """
        self._status = "analyzing"
        logger.info("Running trend analysis...")

        payloads = self.vector_store.get_all_payloads(limit=500)
        if not payloads:
            self._status = "running"
            return []

        # Group signals by technique/topic (simplified clustering via categories)
        technique_clusters: dict[str, list[dict]] = {}
        for p in payloads:
            # Extract primary technique from categories and title
            categories = p.get("categories", [])
            title = p.get("title", "")

            # Use first category as cluster key, or title keywords
            key = self._extract_technique_key(title, categories)
            if key not in technique_clusters:
                technique_clusters[key] = []
            technique_clusters[key].append(p)

        # Score each cluster
        trends = []
        for idx, (technique, signals) in enumerate(technique_clusters.items()):
            novelty_scores = [s.get("novelty_score", 0) for s in signals]
            avg_novelty = sum(novelty_scores) / len(novelty_scores) if novelty_scores else 0

            # Compute emergence score
            github_stars = sum(
                s.get("metadata", {}).get("stars", 0)
                for s in signals
                if s.get("source") == "github"
            )
            paper_count = sum(1 for s in signals if s.get("source") == "arxiv")

            emergence_score = self._compute_emergence_score(
                avg_novelty, len(signals), github_stars, paper_count
            )

            # Impact prediction (simplified heuristic for MVP)
            impact_score = self._predict_impact(
                avg_novelty, len(signals), github_stars
            )

            # Get or generate technical brief
            brief_data = self._get_mock_brief(idx)

            trend = TrendEntry(
                rank=0,  # Will be set after sorting
                technique_name=brief_data["technique"] if config.USE_MOCK_LLM else technique,
                description=brief_data["brief"],
                emergence_score=round(emergence_score, 3),
                novelty_score=round(avg_novelty, 3),
                impact_score=round(impact_score, 3),
                mainstream_eta_months=self._estimate_eta(impact_score),
                confidence=round(min(0.95, 0.5 + impact_score * 0.4), 2),
                source_signals={
                    "arxiv_papers": paper_count,
                    "github_repos": len(signals) - paper_count,
                    "total_github_stars": github_stars,
                },
                competitive_landscape=brief_data.get("competes_with", []),
                risk_factors=self._assess_risks(signals),
                related_techniques=brief_data.get("competes_with", [])[:3],
                paper_count=paper_count,
                github_stars=github_stars,
                signal_ids=[s.get("id", "") for s in signals],
            )
            trends.append(trend)

        # Sort by emergence score and assign ranks
        trends.sort(key=lambda t: t.emergence_score, reverse=True)
        for i, trend in enumerate(trends):
            trend.rank = i + 1

        # Store trends
        self.trends = {t.id: t for t in trends}

        self._status = "running"
        logger.info(f"Trend analysis complete: {len(trends)} techniques ranked")
        return trends[:20]  # Return top 20

    async def generate_technical_brief(
        self, technique: str, context: str = ""
    ) -> str:
        """Generate a detailed technical brief using LLM or mock.

        Args:
            technique: Technique name
            context: Additional context (paper abstracts, etc.)

        Returns:
            Formatted technical brief text
        """
        if config.USE_MOCK_LLM or not config.GROQ_API_KEY:
            return self._generate_mock_brief(technique)

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(
                    f"{config.GROQ_BASE_URL}/chat/completions",
                    headers={
                        "Authorization": f"Bearer {config.GROQ_API_KEY}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": config.LLM_MODEL,
                        "messages": [
                            {
                                "role": "system",
                                "content": (
                                    "You are a senior AI research analyst at VectorMinds. "
                                    "Generate a structured technical brief for an emerging "
                                    "AI technique. Include: core technique description, "
                                    "key insight, why it matters, what it enables, "
                                    "what it competes with, and 12-month impact prediction."
                                ),
                            },
                            {
                                "role": "user",
                                "content": (
                                    f"Generate a technical brief for: {technique}\n\n"
                                    f"Context from recent papers:\n{context[:2000]}"
                                ),
                            },
                        ],
                        "max_tokens": 1000,
                        "temperature": 0.7,
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"]
        except Exception as e:
            logger.error(f"LLM brief generation failed: {e}")
            return self._generate_mock_brief(technique)

    def _extract_technique_key(self, title: str, categories: list) -> str:
        """Extract a technique cluster key from title and categories."""
        title_lower = title.lower()

        # Common technique keywords to cluster by
        keywords = [
            "transformer", "attention", "diffusion", "gan", "rlhf",
            "reinforcement", "graph neural", "federated", "quantization",
            "pruning", "distillation", "moe", "mixture", "rag", "retrieval",
            "lora", "fine-tun", "vision", "multimodal", "agent",
            "state space", "mamba", "language model", "llm",
            "embedding", "contrastive", "self-supervised",
        ]

        for kw in keywords:
            if kw in title_lower:
                return kw.replace(" ", "_").title()

        # Fallback to primary category
        if categories:
            return categories[0] if isinstance(categories[0], str) else str(categories[0])
        return "general_ai"

    def _compute_emergence_score(
        self,
        avg_novelty: float,
        signal_count: int,
        github_stars: int,
        paper_count: int,
    ) -> float:
        """Compute emergence score (composite metric)."""
        # Weighted combination of multiple signals
        novelty_component = avg_novelty * 0.35
        volume_component = min(1.0, signal_count / 20) * 0.25
        github_component = min(1.0, github_stars / 5000) * 0.25
        academic_component = min(1.0, paper_count / 10) * 0.15

        return novelty_component + volume_component + github_component + academic_component

    def _predict_impact(
        self, avg_novelty: float, signal_count: int, github_stars: int
    ) -> float:
        """Simplified impact prediction (MVP heuristic)."""
        # In production, this would be an XGBoost ensemble
        base = avg_novelty * 0.4
        volume_signal = min(1.0, signal_count / 15) * 0.3
        community_signal = min(1.0, github_stars / 3000) * 0.3
        noise = random.uniform(-0.05, 0.05)
        return max(0.0, min(1.0, base + volume_signal + community_signal + noise))

    def _estimate_eta(self, impact_score: float) -> int:
        """Estimate months until mainstream adoption."""
        if impact_score > 0.8:
            return random.randint(3, 6)
        elif impact_score > 0.6:
            return random.randint(6, 12)
        elif impact_score > 0.4:
            return random.randint(12, 18)
        else:
            return random.randint(18, 36)

    def _assess_risks(self, signals: list[dict]) -> list[str]:
        """Generate risk factors for a technique cluster."""
        risks = []
        paper_count = sum(1 for s in signals if s.get("source") == "arxiv")

        if paper_count < 3:
            risks.append("Limited academic validation (few papers)")
        if not any(s.get("source") == "github" for s in signals):
            risks.append("No open-source implementations detected")

        # Default risks
        risks.extend([
            "Compute requirements may limit accessibility",
            "Dataset licensing considerations for commercial use",
        ])
        return risks[:4]

    def _get_mock_brief(self, idx: int) -> dict:
        """Get a pre-written mock brief for demo."""
        return MOCK_BRIEFS[idx % len(MOCK_BRIEFS)]

    def _generate_mock_brief(self, technique: str) -> str:
        """Generate a mock technical brief."""
        self._mock_brief_idx = (self._mock_brief_idx + 1) % len(MOCK_BRIEFS)
        brief = MOCK_BRIEFS[self._mock_brief_idx]
        return (
            f"## Technical Brief: {technique}\n\n"
            f"**Core Technique:** {brief['brief']}\n\n"
            f"**Impact:** {brief['impact']}\n\n"
            f"**Competes With:** {', '.join(brief['competes_with'])}"
        )
