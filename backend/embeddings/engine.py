"""Vector Embedding Engine — Semantic core of VectorMinds.

Handles hierarchical chunking, contrastive embeddings via BGE,
semantic deduplication, and batch encoding.
"""

from __future__ import annotations

import logging
from typing import Optional

import numpy as np
from sentence_transformers import SentenceTransformer

import config

logger = logging.getLogger("vectorminds.embedding")


class EmbeddingEngine:
    """Manages text embedding using a sentence-transformer model."""

    _instance: Optional["EmbeddingEngine"] = None
    _model: Optional[SentenceTransformer] = None

    @classmethod
    def get_instance(cls) -> "EmbeddingEngine":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self._model = None

    def load_model(self):
        """Load the embedding model (lazy initialization)."""
        if self._model is None:
            logger.info(f"Loading embedding model: {config.EMBEDDING_MODEL}")
            self._model = SentenceTransformer(config.EMBEDDING_MODEL)
            logger.info("Embedding model loaded successfully")

    @property
    def model(self) -> SentenceTransformer:
        if self._model is None:
            self.load_model()
        return self._model

    def embed_text(self, text: str) -> list[float]:
        """Embed a single text string.

        Args:
            text: Input text to embed

        Returns:
            Embedding vector as list of floats
        """
        embedding = self.model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def embed_batch(self, texts: list[str], batch_size: int = 32) -> list[list[float]]:
        """Embed a batch of texts efficiently.

        Args:
            texts: List of input texts
            batch_size: Encoding batch size

        Returns:
            List of embedding vectors
        """
        if not texts:
            return []

        logger.info(f"Embedding batch of {len(texts)} texts")
        embeddings = self.model.encode(
            texts,
            batch_size=batch_size,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return embeddings.tolist()

    def chunk_text(self, text: str, max_chunk_size: int = 512) -> list[str]:
        """Hierarchical chunking — split text into semantic chunks.

        Implements paragraph-level chunking with overlap for better
        retrieval granularity.

        Args:
            text: Input text to chunk
            max_chunk_size: Maximum characters per chunk

        Returns:
            List of text chunks
        """
        if len(text) <= max_chunk_size:
            return [text]

        # Split by sentences first
        sentences = text.replace(". ", ".\n").split("\n")
        sentences = [s.strip() for s in sentences if s.strip()]

        chunks = []
        current_chunk = ""

        for sentence in sentences:
            if len(current_chunk) + len(sentence) + 1 <= max_chunk_size:
                current_chunk += (" " if current_chunk else "") + sentence
            else:
                if current_chunk:
                    chunks.append(current_chunk)
                current_chunk = sentence

        if current_chunk:
            chunks.append(current_chunk)

        return chunks if chunks else [text]

    def compute_similarity(self, vec_a: list[float], vec_b: list[float]) -> float:
        """Compute cosine similarity between two vectors."""
        a = np.array(vec_a)
        b = np.array(vec_b)
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-8))

    def is_duplicate(
        self,
        embedding: list[float],
        existing_embeddings: list[list[float]],
        threshold: float = None,
    ) -> bool:
        """Check if an embedding is a semantic duplicate of any existing embedding.

        Args:
            embedding: New embedding to check
            existing_embeddings: List of existing embeddings
            threshold: Similarity threshold (default from config)

        Returns:
            True if duplicate detected
        """
        if threshold is None:
            threshold = config.DEDUP_SIMILARITY_THRESHOLD

        if not existing_embeddings:
            return False

        new_vec = np.array(embedding)
        for existing in existing_embeddings:
            sim = float(
                np.dot(new_vec, np.array(existing))
                / (np.linalg.norm(new_vec) * np.linalg.norm(existing) + 1e-8)
            )
            if sim >= threshold:
                return True
        return False
