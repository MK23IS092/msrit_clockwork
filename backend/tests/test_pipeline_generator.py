from intelligence.pipeline_generator import PipelineGenerator, _task_track


def test_task_track_routes_vision_and_nlp():
    assert _task_track("object-detection") == "vision_det"
    assert _task_track("image-classification") == "vision_cls"
    assert _task_track("text-classification") == "nlp"


def test_generate_pipeline_sets_expected_defaults():
    generator = PipelineGenerator()
    generator._pick_dataset = lambda **kwargs: {
        "name": "ag_news",
        "source": "huggingface-fallback",
        "url": "https://huggingface.co/datasets/ag_news",
        "downloads": 0,
    }
    pipeline = generator.generate_pipeline(
        technique_name="Sparse Attention Mesh",
        description="Vision object detection with distributed attention",
    )

    assert pipeline.technique_name == "Sparse Attention Mesh"
    assert pipeline.task_type == "object-detection"
    assert pipeline.status == "generated"
    assert "Optuna" in pipeline.notebook_content
    assert "onnx" in pipeline.notebook_content.lower()
    assert "Dataset" in pipeline.notebook_content
    assert pipeline.colab_url.startswith("https://colab.research.google.com/")


def test_generate_pipeline_object_detection_uses_detr_section():
    generator = PipelineGenerator()
    generator._pick_dataset = lambda **kwargs: {
        "name": "cppe-5",
        "source": "huggingface",
        "url": "https://huggingface.co/datasets/cppe-5",
        "downloads": 0,
    }
    pipeline = generator.generate_pipeline(
        technique_name="Mesh R-CNN variant",
        description="Vision object detection with DETR",
        task_type="object-detection",
    )
    low = pipeline.notebook_content.lower()
    assert "detr" in low or "cppe" in low
    assert "optuna" in low

