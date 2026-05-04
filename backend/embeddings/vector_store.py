"""Qdrant Vector Store — Semantic search and storage layer.

Wraps Qdrant client in in-memory mode for the hackathon MVP.
Handles collection management, upsert, k-NN search, and novelty computation.
"""

from __future__ import annotations

import logging
from datetime import datetime
from typing import Optional

import numpy as np
from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    PointStruct,
    VectorParams,
    Filter,
    FieldCondition,
    MatchValue,
)

import config

logger = logging.getLogger("vectormind.vectorstore")


class VectorStore:
    """Qdrant-based vector store for research signals."""

    _instance: Optional["VectorStore"] = None

    @classmethod
    def get_instance(cls) -> "VectorStore":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def __init__(self):
        self.client: Optional[QdrantClient] = None
        self.collection_name = config.QDRANT_COLLECTION
        self.concept_graph: dict[str, list[dict]] = {} # Concept -> [neighboring concepts with time]

    def initialize(self):
        """Initialize Qdrant client and create collection."""
        if config.QDRANT_HOST:
            self.client = QdrantClient(
                host=config.QDRANT_HOST, port=config.QDRANT_PORT
            )
        else:
            # In-memory mode for hackathon
            self.client = QdrantClient(":memory:")

        # Create collection if it doesn't exist
        collections = self.client.get_collections().collections
        exists = any(c.name == self.collection_name for c in collections)

        if not exists:
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(
                    size=config.EMBEDDING_DIM,
                    distance=Distance.COSINE,
                ),
            )
            logger.info(
                f"Created Qdrant collection '{self.collection_name}' "
                f"(dim={config.EMBEDDING_DIM})"
            )

    def upsert_signal(
        self,
        signal_id: str,
        embedding: list[float],
        payload: dict,
    ):
        """Store a research signal vector with metadata.

        Args:
            signal_id: Unique signal identifier
            embedding: Vector embedding
            payload: Metadata payload (title, source, scores, etc.)
        """
        self.client.upsert(
            collection_name=self.collection_name,
            points=[
                PointStruct(
                    id=signal_id.replace("-", "")[:32],  # Qdrant needs specific ID format
                    vector=embedding,
                    payload=payload,
                )
            ],
        )

    def upsert_batch(
        self,
        ids: list[str],
        embeddings: list[list[float]],
        payloads: list[dict],
    ):
        """Batch upsert multiple vectors."""
        points = [
            PointStruct(
                id=idx,
                vector=emb,
                payload=pay,
            )
            for idx, (emb, pay) in enumerate(zip(embeddings, payloads))
        ]
        if points:
            self.client.upsert(
                collection_name=self.collection_name,
                points=points,
            )
            logger.info(f"Upserted {len(points)} vectors")

    def search(
        self,
        query_vector: list[float],
        top_k: int = 10,
        source_filter: Optional[str] = None,
    ) -> list[dict]:
        """Semantic similarity search.

        Args:
            query_vector: Query embedding
            top_k: Number of results to return
            source_filter: Optional filter by source type

        Returns:
            List of dicts with score and payload
        """
        query_filter = None
        if source_filter:
            query_filter = Filter(
                must=[
                    FieldCondition(
                        key="source",
                        match=MatchValue(value=source_filter),
                    )
                ]
            )

        results = self.client.search(
            collection_name=self.collection_name,
            query_vector=query_vector,
            limit=top_k,
            query_filter=query_filter,
        )

        return [
            {
                "id": str(r.id),
                "score": r.score,
                "payload": r.payload,
            }
            for r in results
        ]

    def get_collection_count(self) -> int:
        """Get total number of vectors in the collection."""
        try:
            info = self.client.get_collection(self.collection_name)
            return info.points_count
        except Exception:
            return 0

    def compute_novelty_score(
        self,
        embedding: list[float],
        k: int = None,
    ) -> float:
        """Compute novelty score for a new embedding (Section 4.2 algorithm).

        Steps:
        1. Retrieve k nearest neighbors
        2. Compute mean distance (d_mean) and min distance (d_min)
        3. novelty = 0.6 * d_mean + 0.4 * d_min
        4. Normalize to [0, 1]

        Args:
            embedding: New signal embedding
            k: Number of neighbors (default from config)

        Returns:
            Novelty score in [0, 1]
        """
        if k is None:
            k = config.NOVELTY_K_NEIGHBORS

        count = self.get_collection_count()
        if count == 0:
            return 1.0  # First signal is maximally novel

        # Adjust k if we have fewer points
        actual_k = min(k, count)

        results = self.client.search(
            collection_name=self.collection_name,
            query_vector=embedding,
            limit=actual_k,
        )

        if not results:
            return 1.0

        # Cosine distance = 1 - cosine_similarity
        # Qdrant returns similarity scores for cosine distance
        distances = [1.0 - r.score for r in results]

        d_mean = float(np.mean(distances))
        d_min = float(np.min(distances))

        # Weighted combination
        raw_novelty = (
            config.NOVELTY_MEAN_WEIGHT * d_mean
            + config.NOVELTY_MIN_WEIGHT * d_min
        )

        # Normalize to [0, 1] using sigmoid-like mapping
        # In production, would use empirical distribution over last 30 days
        novelty = min(1.0, max(0.0, raw_novelty * 5.0))  # Scale factor for visibility

        return round(novelty, 4)

    def get_all_payloads(self, limit: int = 1000) -> list[dict]:
        """Retrieve all stored payloads (for trend computation)."""
        try:
            results = self.client.scroll(
                collection_name=self.collection_name,
                limit=limit,
                with_payload=True,
                with_vectors=False,
            )
            return [
                {"id": str(point.id), **point.payload}
                for point in results[0]
            ]
        except Exception as e:
            logger.error(f"Failed to scroll payloads: {e}")
            return []

    def get_vectors_for_projection(self, limit: int = 500) -> tuple[list, list]:
        """Get vectors and payloads for 2D projection (t-SNE/UMAP viz).

        Returns:
            Tuple of (vectors, payloads)
        """
        try:
            results = self.client.scroll(
                collection_name=self.collection_name,
                limit=limit,
                with_payload=True,
                with_vectors=True,
            )
            vectors = [point.vector for point in results[0]]
            payloads = [point.payload for point in results[0]]
            return vectors, payloads
        except Exception as e:
            logger.error(f"Failed to get vectors for projection: {e}")
            return [], []
    def build_temporal_graph(self) -> dict:
        """Construct a graph of concepts tracking evolution over time.
        
        Follows Section 5.4: Temporal graph construction.
        """
        payloads = self.get_all_payloads()
        if not payloads: return {}

        # 1. Group by category/tag
        graph = {}
        for p in payloads:
            tags = p.get("categories", [])
            for tag in tags:
                if tag not in graph: graph[tag] = []
                graph[tag].append({
                    "id": p.get("id"),
                    "title": p.get("title"),
                    "timestamp": p.get("timestamp"),
                    "score": p.get("novelty_score", 0)
                })
        
        self.concept_graph = graph
        logger.info(f"Temporal graph built with {len(graph)} concept nodes")
        return graph
