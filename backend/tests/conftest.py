import sys
import types


def _install_import_shims():
    if "sentence_transformers" not in sys.modules:
        st_mod = types.ModuleType("sentence_transformers")

        class _SentenceTransformer:
            def __init__(self, *args, **kwargs):
                pass

            def encode(self, value, *args, **kwargs):
                if isinstance(value, list):
                    return [[0.0] for _ in value]
                return [0.0]

        st_mod.SentenceTransformer = _SentenceTransformer
        sys.modules["sentence_transformers"] = st_mod

    if "qdrant_client" not in sys.modules:
        qc_mod = types.ModuleType("qdrant_client")
        qcm_mod = types.ModuleType("qdrant_client.models")

        class _QdrantClient:
            def __init__(self, *args, **kwargs):
                pass

        class _Dummy:
            def __init__(self, *args, **kwargs):
                pass

        qc_mod.QdrantClient = _QdrantClient
        qcm_mod.Distance = _Dummy
        qcm_mod.PointStruct = _Dummy
        qcm_mod.VectorParams = _Dummy
        qcm_mod.Filter = _Dummy
        qcm_mod.FieldCondition = _Dummy
        qcm_mod.MatchValue = _Dummy
        sys.modules["qdrant_client"] = qc_mod
        sys.modules["qdrant_client.models"] = qcm_mod

    if "arxiv" not in sys.modules:
        ax_mod = types.ModuleType("arxiv")

        class _Client:
            def __init__(self, *args, **kwargs):
                pass

            def results(self, *args, **kwargs):
                return []

        class _Search:
            def __init__(self, *args, **kwargs):
                pass

        class _SortCriterion:
            SubmittedDate = "SubmittedDate"

        class _SortOrder:
            Descending = "Descending"

        ax_mod.Client = _Client
        ax_mod.Search = _Search
        ax_mod.SortCriterion = _SortCriterion
        ax_mod.SortOrder = _SortOrder
        sys.modules["arxiv"] = ax_mod

    if "bs4" not in sys.modules:
        bs4_mod = types.ModuleType("bs4")

        class _BeautifulSoup:
            def __init__(self, *args, **kwargs):
                pass

            def find_all(self, *args, **kwargs):
                return []

        bs4_mod.BeautifulSoup = _BeautifulSoup
        sys.modules["bs4"] = bs4_mod


_install_import_shims()
