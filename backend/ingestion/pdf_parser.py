"""PDF Parser — Section-Aware Research Document Processing.

Extracts structured text from research PDFs and performs hierarchical
chunking for granular vector indexing.
"""

import re
import logging
from typing import Dict, List

logger = logging.getLogger("vectormind.ingestion")

class PDFParser:
    """Parses research PDFs into structured sections and chunks."""

    def __init__(self):
        # Section header patterns
        self.section_patterns = {
            "abstract": re.compile(r"^abstract", re.IGNORECASE),
            "introduction": re.compile(r"^1\.?\s+introduction|^introduction", re.IGNORECASE),
            "methodology": re.compile(r"^2\.?\s+methods|^methods|^methodology", re.IGNORECASE),
            "results": re.compile(r"^3\.?\s+results|^results", re.IGNORECASE),
            "discussion": re.compile(r"^4\.?\s+discussion|^discussion", re.IGNORECASE),
            "conclusion": re.compile(r"^5\.?\s+conclusion|^conclusion", re.IGNORECASE),
            "references": re.compile(r"^references", re.IGNORECASE),
        }

    def parse_text(self, text: str) -> Dict[str, str]:
        """Parse raw text into sections based on headers."""
        sections = {"abstract": "", "main_text": "", "full_text": text}
        lines = text.split('\n')
        
        current_section = "abstract"
        
        for line in lines:
            line_clean = line.strip()
            if not line_clean:
                continue
                
            # Check for section transition
            found_header = False
            for section_name, pattern in self.section_patterns.items():
                if pattern.match(line_clean):
                    current_section = section_name
                    found_header = True
                    break
            
            if not found_header:
                if current_section in sections:
                    sections[current_section] += line + "\n"
                else:
                    sections[current_section] = line + "\n"
                    
        return sections

    def hierarchical_chunking(self, sections: Dict[str, str], chunk_size: int = 1000) -> List[Dict]:
        """Create chunks at different granularity levels."""
        chunks = []
        
        # 1. Abstract level (high-level)
        if sections.get("abstract"):
            chunks.append({
                "level": "abstract",
                "text": sections["abstract"],
                "metadata": {"type": "summary"}
            })
            
        # 2. Section level (paragraph-aware)
        for name, content in sections.items():
            if name in ["full_text", "references"]: continue
            
            # Simple paragraph splitting
            paragraphs = content.split('\n\n')
            for i, p in enumerate(paragraphs):
                if len(p.strip()) < 50: continue
                chunks.append({
                    "level": "section",
                    "section": name,
                    "text": p.strip(),
                    "metadata": {"para_idx": i}
                })
                
        return chunks
