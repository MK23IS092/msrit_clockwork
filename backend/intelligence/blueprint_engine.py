"""Product Blueprint Engine — Generates startup-ready product briefs.

Takes a high-scoring technique and generates a complete product blueprint
using LLM (Groq free tier) or mock data for demo.
"""

from __future__ import annotations

import json
import logging
from datetime import datetime
from typing import Optional

import httpx

from ingestion.schema import ProductBlueprint, TrendEntry
import config

logger = logging.getLogger("vectorminds.blueprint")

# ─── Mock Blueprints for Demo ─────────────────────────────────
MOCK_BLUEPRINTS = {
    "default": {
        "problem_statement": (
            "Enterprise organizations process millions of documents daily — contracts, "
            "reports, emails, compliance filings — but existing NLP solutions are limited "
            "by transformer context windows (typically 4K-8K tokens). Documents exceeding "
            "this limit require chunking strategies that lose cross-section context, "
            "resulting in 15-30% accuracy degradation on long-document tasks. The market "
            "for intelligent document processing is $4.2B (2024) growing at 28% CAGR."
        ),
        "market_size": "$4.2B (2024), projected $12.1B by 2028 at 28% CAGR",
        "technical_implementation": (
            "Build a document intelligence API powered by the selected technique. "
            "Architecture: (1) Document ingestion service with OCR and layout detection, "
            "(2) Adaptive chunking engine that preserves cross-reference context, "
            "(3) Core inference engine using the technique for unlimited-context processing, "
            "(4) Structured output layer with JSON/XML schema enforcement, "
            "(5) REST API with streaming support for real-time processing. "
            "Deploy on AWS with auto-scaling GPU instances (A10G for inference)."
        ),
        "architecture_decisions": [
            "Use streaming inference to handle arbitrarily long documents",
            "Implement a hybrid retrieval + full-context approach for optimal accuracy",
            "Deploy as a containerized microservice for horizontal scaling",
            "Cache embeddings in Redis for repeated document access patterns",
        ],
        "differentiation_strategy": (
            "Unlike existing solutions (AWS Textract, Google Document AI, Azure Form "
            "Recognizer), this product handles documents of ANY length without chunking "
            "degradation. The core moat is the technique's linear-time complexity, "
            "enabling 100x longer context at 1/10th the cost. Additional moats: "
            "proprietary fine-tuning on 500K enterprise documents, and a self-improving "
            "feedback loop where corrections from users improve the model continuously."
        ),
        "dataset_requirements": (
            "Initial training: (1) DocVQA (50K document-question pairs), "
            "(2) SQuAD 2.0 for reading comprehension baseline, "
            "(3) Contract Understanding Atticus Dataset (CUAD) for legal domain, "
            "(4) FUNSD for form understanding. Proprietary data collection: "
            "Partner with 3 enterprise customers for anonymized document datasets. "
            "Synthetic data: Generate 100K long-document QA pairs using GPT-4."
        ),
        "go_to_market": (
            "Target: Legal tech firms and compliance teams (high document volume, "
            "high accuracy requirements). Channel: Direct sales to Top-50 law firms, "
            "integration partnerships with existing DMS providers (NetDocuments, iManage). "
            "Pricing: Usage-based API pricing — $0.01/page for standard, $0.05/page for "
            "premium with human-in-the-loop verification. First 90 days: 3 design partners, "
            "10K documents processed, 95%+ accuracy on standard benchmarks."
        ),
        "risk_assessment": (
            "Technical risks: (1) Technique may not generalize to all document types — "
            "mitigate with domain-specific fine-tuning. (2) Inference latency on very "
            "long documents (>100 pages) — mitigate with streaming and caching. "
            "Market risks: (1) Incumbent response — AWS/Google/Azure may adopt similar "
            "techniques within 12 months — speed to market is critical. "
            "Competitive risks: (1) Several well-funded startups in adjacent space — "
            "differentiate on long-context capability."
        ),
        "first_90_day_milestones": [
            "Week 1-2: Core model integration and API scaffold",
            "Week 3-4: Document ingestion pipeline with OCR",
            "Week 5-6: Fine-tune on DocVQA and CUAD datasets",
            "Week 7-8: API deployment with auth and rate limiting",
            "Week 9-10: First design partner onboarding",
            "Week 11-12: Benchmark publication and Product Hunt launch",
        ],
        "suggested_stack": [
            "PyTorch + HuggingFace Transformers",
            "FastAPI + Pydantic",
            "Redis for caching",
            "PostgreSQL for metadata",
            "AWS ECS + A10G GPU instances",
            "Stripe for billing",
        ],
    }
}


class BlueprintEngine:
    """Generates product blueprints from high-scoring research techniques."""

    def __init__(self):
        self.generated_blueprints: dict[str, ProductBlueprint] = {}

    async def generate_blueprint(
        self,
        trend: TrendEntry,
        additional_context: str = "",
    ) -> ProductBlueprint:
        """Generate a complete product blueprint for a technique.

        Args:
            trend: The trend entry to generate a blueprint for
            additional_context: Additional research context

        Returns:
            Complete ProductBlueprint
        """
        logger.info(f"Generating blueprint for: {trend.technique_name}")

        if config.USE_MOCK_LLM or not config.GROQ_API_KEY:
            blueprint = self._generate_mock_blueprint(trend)
        else:
            blueprint = await self._generate_llm_blueprint(trend, additional_context)

        self.generated_blueprints[blueprint.id] = blueprint
        logger.info(f"Blueprint generated: {blueprint.id}")
        return blueprint

    async def _generate_llm_blueprint(
        self, trend: TrendEntry, context: str
    ) -> ProductBlueprint:
        """Generate blueprint using Groq LLM."""
        prompt = (
            f"Generate a complete startup product blueprint based on this "
            f"emerging AI technique:\n\n"
            f"Technique: {trend.technique_name}\n"
            f"Description: {trend.description}\n"
            f"Emergence Score: {trend.emergence_score}\n"
            f"Impact Score: {trend.impact_score}\n"
            f"Mainstream ETA: {trend.mainstream_eta_months} months\n\n"
            f"Additional context:\n{context[:1500]}\n\n"
            f"Please provide a JSON response with these fields:\n"
            f"problem_statement, market_size, technical_implementation, "
            f"architecture_decisions (list), differentiation_strategy, "
            f"dataset_requirements, go_to_market, risk_assessment, "
            f"first_90_day_milestones (list), suggested_stack (list)"
        )

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
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
                                    "You are a senior product strategist and AI architect. "
                                    "Generate detailed, actionable product blueprints. "
                                    "Respond in valid JSON only."
                                ),
                            },
                            {"role": "user", "content": prompt},
                        ],
                        "max_tokens": 2000,
                        "temperature": 0.7,
                        "response_format": {"type": "json_object"},
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                bp_data = json.loads(content)

                return ProductBlueprint(
                    technique_name=trend.technique_name,
                    trend_id=trend.id,
                    problem_statement=bp_data.get("problem_statement", ""),
                    market_size=bp_data.get("market_size", ""),
                    technical_implementation=bp_data.get("technical_implementation", ""),
                    architecture_decisions=bp_data.get("architecture_decisions", []),
                    differentiation_strategy=bp_data.get("differentiation_strategy", ""),
                    dataset_requirements=bp_data.get("dataset_requirements", ""),
                    go_to_market=bp_data.get("go_to_market", ""),
                    risk_assessment=bp_data.get("risk_assessment", ""),
                    first_90_day_milestones=bp_data.get("first_90_day_milestones", []),
                    suggested_stack=bp_data.get("suggested_stack", []),
                )
        except Exception as e:
            logger.error(f"LLM blueprint generation failed: {e}")
            return self._generate_mock_blueprint(trend)

    def _generate_mock_blueprint(self, trend: TrendEntry) -> ProductBlueprint:
        """Generate a mock blueprint for demo purposes."""
        mock = MOCK_BLUEPRINTS["default"]
        return ProductBlueprint(
            technique_name=trend.technique_name,
            trend_id=trend.id,
            problem_statement=mock["problem_statement"],
            market_size=mock["market_size"],
            technical_implementation=mock["technical_implementation"],
            architecture_decisions=mock["architecture_decisions"],
            differentiation_strategy=mock["differentiation_strategy"],
            dataset_requirements=mock["dataset_requirements"],
            go_to_market=mock["go_to_market"],
            risk_assessment=mock["risk_assessment"],
            first_90_day_milestones=mock["first_90_day_milestones"],
            suggested_stack=mock["suggested_stack"],
        )

    def get_blueprint(self, blueprint_id: str) -> Optional[ProductBlueprint]:
        return self.generated_blueprints.get(blueprint_id)

    def list_blueprints(self) -> list[ProductBlueprint]:
        return list(self.generated_blueprints.values())
