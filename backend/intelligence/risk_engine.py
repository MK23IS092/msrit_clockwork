"""Risk Engine — Automated Research Risk Assessment.

Identifies potential pitfalls in research trends including patent thickets,
licensing restrictions, and compute requirements.
"""

import logging
from typing import List, Dict

logger = logging.getLogger("vectormind.intelligence")

class RiskEngine:
    """Analyzes technical, market, and legal risks for research trends."""

    def __init__(self):
        self.risk_categories = {
            "legal": ["patent", "license", "copyright", "trademark"],
            "technical": ["compute", "memory", "latency", "dataset"],
            "market": ["competition", "moat", "adoption", "cost"]
        }

    def analyze_risks(self, trend_title: str, description: str, metadata: Dict) -> List[Dict]:
        """Assess risks based on signal content and metadata."""
        risks = []
        text = (trend_title + " " + description).lower()
        
        # 1. Patent Risk
        if "patent" in text or metadata.get("patent_number"):
            risks.append({
                "type": "Legal",
                "severity": "High",
                "label": "Patent Thicket Detected",
                "detail": "Core technique may be covered by active patent filings."
            })
            
        # 2. Compute Risk
        if any(kw in text for kw in ["transformer", "large language model", "diffusion", "gpu"]):
            risks.append({
                "type": "Technical",
                "severity": "Medium",
                "label": "High Compute Requirement",
                "detail": "Likely requires A100/H100 clusters for full training."
            })
            
        # 3. Licensing Risk
        license_id = metadata.get("license", "").upper()
        if "GPL" in license_id or "CC-BY-NC" in license_id:
            risks.append({
                "type": "Legal",
                "severity": "Critical",
                "label": "Restrictive License",
                "detail": f"Dataset or code uses {license_id}, restricting commercial use."
            })
            
        return risks
