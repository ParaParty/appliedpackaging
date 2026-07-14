#!/usr/bin/env python3
"""Import the editable ME Packager Blockbench source into runtime resources.

The source model deliberately keeps the conveyor and curtain as separate cubes.
The generated world body excludes those cubes; the block entity renderer owns
their motion.  The item model includes all cubes in their rest pose.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import struct
from pathlib import Path
from typing import Any


TEXTURE_PATHS = {
    0: "appliedpackaging:block/me_packager/base",
    1: "appliedpackaging:block/me_packager/curtain",
    2: "appliedpackaging:block/me_packager/belt_scroll",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_directory", type=Path)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    return parser.parse_args()


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise ValueError(f"Not a PNG with a valid IHDR: {path}")
    return struct.unpack(">II", data[16:24])


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def scale_uv(uv: list[float], resolution: dict[str, float]) -> list[float]:
    x_scale = 16.0 / float(resolution["width"])
    y_scale = 16.0 / float(resolution["height"])
    return [
        round(float(uv[0]) * x_scale, 6),
        round(float(uv[1]) * y_scale, 6),
        round(float(uv[2]) * x_scale, 6),
        round(float(uv[3]) * y_scale, 6),
    ]


def convert_face(
    face: dict[str, Any],
    resolution: dict[str, float],
    *,
    keep_cullface: bool,
    force_enabled: bool = False,
) -> dict[str, Any] | None:
    texture = face.get("texture")
    if texture is None:
        return None
    if not force_enabled and face.get("enabled") is False:
        return None
    output: dict[str, Any] = {
        "uv": scale_uv(face["uv"], resolution),
        "texture": f"#{int(texture)}",
    }
    rotation = int(face.get("rotation", 0)) % 360
    if rotation:
        output["rotation"] = rotation
    if keep_cullface and face.get("cullface"):
        output["cullface"] = face["cullface"]
    return output


def convert_element(
    element: dict[str, Any],
    resolution: dict[str, float],
    *,
    keep_cullface: bool,
    force_faces: set[str] | None = None,
) -> dict[str, Any]:
    force_faces = force_faces or set()
    faces: dict[str, Any] = {}
    for direction, source_face in element["faces"].items():
        face = convert_face(
            source_face,
            resolution,
            keep_cullface=keep_cullface,
            force_enabled=direction in force_faces,
        )
        if face is not None:
            faces[direction] = face
    if not faces:
        raise ValueError(f"Element {element.get('name', '<unnamed>')} has no exportable faces")
    output: dict[str, Any] = {
        "name": element.get("name", "cube"),
        "from": element["from"],
        "to": element["to"],
        "faces": faces,
    }
    if element.get("shade") is False:
        output["shade"] = False
    return output


def model_json(
    textures: dict[int, str],
    elements: list[dict[str, Any]],
    *,
    parent: str | None = None,
) -> dict[str, Any]:
    result: dict[str, Any] = {
        "credit": "Imported from the user-authored Blockbench source",
        "ambientocclusion": True,
        "render_type": "minecraft:cutout_mipped",
        "textures": {str(index): value for index, value in textures.items()},
        "elements": elements,
    }
    if parent is not None:
        result["parent"] = parent
    result["textures"]["particle"] = textures[0]
    return result


def normalize_belt_uv(element: dict[str, Any]) -> None:
    """Use one exact 16px period: 15px top followed by the 1px outlet face."""
    element["faces"]["up"]["uv"] = [0.0, 6.0, 7.5, 0.0]
    element["faces"]["east"]["uv"] = [7.5, 6.0, 8.0, 0.0]


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_blockstate() -> dict[str, Any]:
    variants: dict[str, Any] = {}
    facing_rotations = {
        "east": {},
        "south": {"y": 90},
        "west": {"y": 180},
        "north": {"y": 270},
    }
    for facing, rotation in facing_rotations.items():
        variant = {"model": "appliedpackaging:block/me_packager/body", **rotation}
        variants[f"facing={facing}"] = variant
    return {"variants": variants}


def main() -> None:
    args = parse_args()
    source = args.source_directory.resolve()
    repo = args.repo.resolve()
    bbmodel_path = source / "model.bbmodel"
    texture_sources = {
        "base.png": source / "base.png",
        "curtain.png": source / "curtain.png",
        "belt_scroll.png": source / "belt_scroll.png",
    }
    missing = [str(path) for path in (bbmodel_path, *texture_sources.values()) if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing ME Packager source files: " + ", ".join(missing))

    expected_sizes = {
        "base.png": (64, 64),
        "curtain.png": (16, 16),
        "belt_scroll.png": (32, 32),
    }
    for name, path in texture_sources.items():
        actual = png_size(path)
        if actual != expected_sizes[name]:
            raise ValueError(f"{name} must be {expected_sizes[name]}, got {actual}")

    source_model = json.loads(bbmodel_path.read_text(encoding="utf-8"))
    resolution = source_model.get("resolution")
    if resolution != {"width": 32, "height": 32}:
        raise ValueError(f"model.bbmodel must use a 32x32 project UV grid, got {resolution}")
    elements = source_model.get("elements", [])
    if len(elements) != 11:
        raise ValueError(f"Expected 11 elements (6 body, 1 belt, 4 curtains), got {len(elements)}")

    body_elements = [
        convert_element(element, resolution, keep_cullface=True)
        for element in elements[:6]
    ]
    belt_element = convert_element(
        elements[6], resolution, keep_cullface=False, force_faces={"up", "east"}
    )
    normalize_belt_uv(belt_element)
    curtain_element = convert_element(elements[7], resolution, keep_cullface=False)
    item_elements = [
        convert_element(
            element,
            resolution,
            keep_cullface=False,
            force_faces={"up", "east"} if index == 6 else set(),
        )
        for index, element in enumerate(elements)
    ]
    normalize_belt_uv(item_elements[6])

    texture_dir = repo / "src/main/resources/assets/appliedpackaging/textures/block/me_packager"
    model_dir = repo / "src/main/resources/assets/appliedpackaging/models/block/me_packager"
    source_dir = repo / "docs/assets/source/me_packager"
    texture_dir.mkdir(parents=True, exist_ok=True)
    source_dir.mkdir(parents=True, exist_ok=True)
    for name, path in texture_sources.items():
        shutil.copyfile(path, texture_dir / name)
    shutil.copyfile(bbmodel_path, source_dir / "model.bbmodel")
    aseprite = source / "belt_scroll.aseprite"
    if aseprite.is_file():
        shutil.copyfile(aseprite, source_dir / aseprite.name)

    write_json(model_dir / "body.json", model_json({0: TEXTURE_PATHS[0]}, body_elements))
    write_json(model_dir / "belt.json", model_json({0: TEXTURE_PATHS[0], 2: TEXTURE_PATHS[2]}, [belt_element]))
    write_json(
        model_dir / "curtain_flap.json",
        model_json({0: TEXTURE_PATHS[0], 1: TEXTURE_PATHS[1]}, [curtain_element]),
    )
    write_json(
        model_dir / "item.json",
        model_json(TEXTURE_PATHS, item_elements, parent="minecraft:block/block"),
    )
    write_json(
        repo / "src/main/resources/assets/appliedpackaging/models/block/me_packager.json",
        {"parent": "appliedpackaging:block/me_packager/body"},
    )
    write_json(
        repo / "src/main/resources/assets/appliedpackaging/models/item/me_packager.json",
        {"parent": "appliedpackaging:block/me_packager/item"},
    )
    write_json(
        repo / "src/main/resources/assets/appliedpackaging/blockstates/me_packager.json",
        build_blockstate(),
    )

    report = {
        "source": str(source),
        "resolution": resolution,
        "element_split": {"body": 6, "belt": 1, "curtain_instances": 4},
        "belt_uv_pixels": {"top": 15, "outlet_face": 1, "period": 16, "texture_width": 32},
        "source_sha256": {
            name: sha256(path) for name, path in {"model.bbmodel": bbmodel_path, **texture_sources}.items()
        },
        "runtime_sha256": {
            name: sha256(texture_dir / name) for name in texture_sources
        },
    }
    write_json(source_dir / "import-report.json", report)
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
