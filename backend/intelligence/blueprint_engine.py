"""Product Blueprint Engine — Generates startup-ready product briefs.

Takes a high-scoring technique and generates a complete product blueprint
using LLM (Gemini) or mock data for demo.
"""

from __future__ import annotations

import asyncio
import json
import logging
from datetime import datetime
from typing import Optional

import httpx

from ingestion.schema import ProductBlueprint, TrendEntry
import config

logger = logging.getLogger("vectorminds.blueprint")


def _coerce_text(value, indent: int = 0) -> str:
    """Coerce a Gemini value (string|list|dict|number|None) into a readable string.

    Lists become bullet lines; dicts become "Key:\\n  - item" sections. Used because
    the LLM sometimes returns nested objects for fields the schema types as ``str``.
    """
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if isinstance(value, (int, float, bool)):
        return str(value)
    pad = "  " * indent
    if isinstance(value, list):
        return "\n".join(f"{pad}- {_coerce_text(v, indent + 1).lstrip()}" for v in value)
    if isinstance(value, dict):
        chunks = []
        for k, v in value.items():
            label = str(k).replace("_", " ").strip()
            inner = _coerce_text(v, indent + 1)
            if "\n" in inner or (isinstance(v, (list, dict))):
                chunks.append(f"{pad}{label}:\n{inner}")
            else:
                chunks.append(f"{pad}{label}: {inner}")
        return "\n".join(chunks)
    return str(value)


def _coerce_str_list(value) -> list[str]:
    """Force a value into a list[str], coercing nested dicts/lists to readable lines."""
    if value is None:
        return []
    if isinstance(value, list):
        return [_coerce_text(v).strip() for v in value if v is not None]
    if isinstance(value, dict):
        return [f"{k}: {_coerce_text(v).strip()}" for k, v in value.items()]
    return [str(value)]


def _repair_truncated_json(text: str) -> Optional[dict]:
    """Best-effort recovery of a truncated JSON object from a Gemini response.

    Closes any open string, then closes any unbalanced ``[`` / ``{`` brackets,
    in stack order. Returns ``None`` if the result still does not parse.
    """
    if not text:
        return None
    s = text.strip()
    if not s.startswith("{"):
        start = s.find("{")
        if start == -1:
            return None
        s = s[start:]

    in_str = False
    escape = False
    stack: list[str] = []
    for ch in s:
        if escape:
            escape = False
            continue
        if ch == "\\":
            escape = True
            continue
        if ch == '"':
            in_str = not in_str
            continue
        if in_str:
            continue
        if ch in "{[":
            stack.append("}" if ch == "{" else "]")
        elif ch in "}]" and stack and stack[-1] == ch:
            stack.pop()

    repaired = s
    if in_str:
        repaired += '"'
    while stack:
        repaired += stack.pop()

    repaired = repaired.rstrip(",")
    try:
        return json.loads(repaired)
    except Exception:
        return None

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

        if config.USE_MOCK_LLM or not config.LLM_API_KEY:
            blueprint = self._generate_mock_blueprint(trend)
        else:
            blueprint = await self._generate_llm_blueprint(trend, additional_context)

        self.generated_blueprints[blueprint.id] = blueprint
        logger.info(f"Blueprint generated: {blueprint.id}")
        return blueprint

    async def _generate_llm_blueprint(
        self, trend: TrendEntry, context: str
    ) -> ProductBlueprint:
        """Generate blueprint using Gemini LLM."""
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
                full_prompt = (
                    "You are a senior product strategist and AI architect. "
                    "Generate detailed, actionable product blueprints.\n"
                    "Return ONLY valid JSON with these keys: "
                    "problem_statement, market_size, technical_implementation, "
                    "architecture_decisions, differentiation_strategy, dataset_requirements, "
                    "go_to_market, risk_assessment, first_90_day_milestones, suggested_stack.\n\n"
                    f"{prompt}"
                )
                payload = {
                    "contents": [{"parts": [{"text": full_prompt}]}],
                    "generationConfig": {
                        "temperature": 0.7,
                        "maxOutputTokens": 8192,
                        "responseMimeType": "application/json",
                    },
                }

                # Gemini occasionally returns 503/overloaded. Retry with simple
                # exponential backoff (3 attempts, 1.5s/3s/6s) before falling
                # back to the mock template.
                resp = None
                last_exc: Optional[Exception] = None
                for attempt, delay in enumerate((1.5, 3.0, 6.0), start=1):
                    try:
                        resp = await client.post(
                            f"{config.GEMINI_BASE_URL}/models/{config.LLM_MODEL}:generateContent",
                            params={"key": config.LLM_API_KEY},
                            headers={"Content-Type": "application/json"},
                            json=payload,
                        )
                        if resp.status_code in (500, 502, 503, 504, 429):
                            logger.warning(
                                "Gemini blueprint attempt %s returned %s; retrying",
                                attempt,
                                resp.status_code,
                            )
                            last_exc = httpx.HTTPStatusError(
                                f"Gemini transient {resp.status_code}",
                                request=resp.request,
                                response=resp,
                            )
                            await asyncio.sleep(delay)
                            continue
                        last_exc = None
                        break
                    except (httpx.ConnectError, httpx.ReadTimeout, httpx.RemoteProtocolError) as e:
                        last_exc = e
                        logger.warning("Gemini blueprint network error attempt %s: %s", attempt, e)
                        await asyncio.sleep(delay)
                        continue
                if resp is None or last_exc is not None:
                    if last_exc is not None:
                        raise last_exc
                    raise RuntimeError("Gemini blueprint: no response")
                resp.raise_for_status()
                data = resp.json()
                candidates = data.get("candidates", [])
                if not candidates:
                    raise ValueError("No Gemini candidates returned")
                finish_reason = candidates[0].get("finishReason", "")
                parts = candidates[0].get("content", {}).get("parts", [])
                content = "".join(p.get("text", "") for p in parts if isinstance(p, dict)).strip()
                if not content:
                    raise ValueError("Empty Gemini response text")
                try:
                    bp_data = json.loads(content)
                except json.JSONDecodeError as je:
                    if finish_reason == "MAX_TOKENS":
                        logger.warning(
                            "Gemini blueprint truncated by MAX_TOKENS; attempting repair"
                        )
                    bp_data = _repair_truncated_json(content)
                    if bp_data is None:
                        raise je

                return ProductBlueprint(
                    technique_name=trend.technique_name,
                    trend_id=trend.id,
                    problem_statement=_coerce_text(bp_data.get("problem_statement", "")),
                    market_size=_coerce_text(bp_data.get("market_size", "")),
                    technical_implementation=_coerce_text(bp_data.get("technical_implementation", "")),
                    architecture_decisions=_coerce_str_list(bp_data.get("architecture_decisions", [])),
                    differentiation_strategy=_coerce_text(bp_data.get("differentiation_strategy", "")),
                    dataset_requirements=_coerce_text(bp_data.get("dataset_requirements", "")),
                    go_to_market=_coerce_text(bp_data.get("go_to_market", "")),
                    risk_assessment=_coerce_text(bp_data.get("risk_assessment", "")),
                    first_90_day_milestones=_coerce_str_list(bp_data.get("first_90_day_milestones", [])),
                    suggested_stack=_coerce_str_list(bp_data.get("suggested_stack", [])),
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
