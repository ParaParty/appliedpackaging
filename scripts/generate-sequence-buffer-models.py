from __future__ import annotations

import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_ROOT = ROOT / "src/main/resources/assets/appliedpackaging/models/block/sequence_buffer"
GENERATED_ROOT = MODEL_ROOT / "generated"
BLOCKSTATE_PATH = (
    ROOT / "src/main/resources/assets/appliedpackaging/blockstates/sequence_buffer.json"
)
REPORT_PATH = (
    ROOT
    / "build/asset-reference/sequence-buffer/model-orientations/orientation-manifest.json"
)

DIRECTIONS = ("down", "up", "north", "south", "west", "east")
OPPOSITE = {
    "down": "up",
    "up": "down",
    "north": "south",
    "south": "north",
    "west": "east",
    "east": "west",
}
AXIS_DIRECTIONS = {
    "x": ("west", "east"),
    "y": ("down", "up"),
    "z": ("north", "south"),
}
POSITIVE_AXIS_DIRECTION = {"x": "east", "y": "up", "z": "south"}
AXIS_BY_DIRECTION = {
    direction: axis
    for axis, directions in AXIS_DIRECTIONS.items()
    for direction in directions
}

# Minecraft 1.20.1 BlockElement/BlockFaceUV world directions for texture +U and +V.
# The source sheet's directed-side arrow points along texture +U. Every generated
# side face rotates that +U arrow toward the block's own facing direction.
UV_DIRECTIONS = {
    "down": {
        0: ("east", "north"),
        90: ("north", "west"),
        180: ("west", "south"),
        270: ("south", "east"),
    },
    "up": {
        0: ("east", "south"),
        90: ("south", "west"),
        180: ("west", "north"),
        270: ("north", "east"),
    },
    "north": {
        0: ("west", "down"),
        90: ("down", "east"),
        180: ("east", "up"),
        270: ("up", "west"),
    },
    "south": {
        0: ("east", "down"),
        90: ("down", "west"),
        180: ("west", "up"),
        270: ("up", "east"),
    },
    "west": {
        0: ("south", "down"),
        90: ("down", "north"),
        180: ("north", "up"),
        270: ("up", "south"),
    },
    "east": {
        0: ("north", "down"),
        90: ("down", "south"),
        180: ("south", "up"),
        270: ("up", "north"),
    },
}

TEXTURE_ROOT = "appliedpackaging:block/sequence_buffer/faces"
TEXTURES = {
    name: f"{TEXTURE_ROOT}/{name}"
    for name in (
        "undirected_unformed",
        "undirected_formed_middle_side",
        "undirected_formed_edge_side",
        "controller_back",
        "directed_front_unformed",
        "directed_front_formed_middle_side",
        "directed_front_formed_edge_side",
        "formed_middle_side_edge_occluded",
        "directed_side_unformed",
        "directed_side_formed_middle_side",
        "directed_side_formed_edge_side",
        "controller_side",
        "directed_back_unformed",
        "directed_back_formed_middle_side",
        "directed_back_formed_edge_side",
        "tail_back",
    )
}


def rotation_for(face: str, *, u_direction: str | None = None, v_direction: str | None = None) -> int:
    if (u_direction is None) == (v_direction is None):
        raise ValueError("Specify exactly one of u_direction or v_direction")
    matches = [
        rotation
        for rotation, (u_axis, v_axis) in UV_DIRECTIONS[face].items()
        if (u_direction is not None and u_axis == u_direction)
        or (v_direction is not None and v_axis == v_direction)
    ]
    if len(matches) != 1:
        raise ValueError(
            f"Expected one UV rotation for face={face}, u={u_direction}, v={v_direction}; got {matches}"
        )
    return matches[0]


def natural_down(direction: str) -> str:
    return "down" if AXIS_BY_DIRECTION[direction] != "y" else "south"


def face(texture: str, face_direction: str, rotation: int = 0) -> dict[str, object]:
    result: dict[str, object] = {
        "texture": TEXTURES[texture],
        "cullface": face_direction,
    }
    if rotation:
        result["rotation"] = rotation
    return result


def oriented_face(
    texture: str,
    face_direction: str,
    *,
    u_direction: str | None = None,
    v_direction: str | None = None,
) -> dict[str, object]:
    return face(
        texture,
        face_direction,
        rotation_for(face_direction, u_direction=u_direction, v_direction=v_direction),
    )


def model(particle_texture: str, faces: dict[str, dict[str, object]]) -> dict[str, object]:
    if set(faces) != set(DIRECTIONS):
        raise ValueError(f"Model must define all six faces, got {sorted(faces)}")
    textures = {"particle": TEXTURES[particle_texture]}
    referenced_faces: dict[str, dict[str, object]] = {}
    for direction in DIRECTIONS:
        textures[direction] = str(faces[direction]["texture"])
        referenced_faces[direction] = dict(faces[direction])
        referenced_faces[direction]["texture"] = f"#{direction}"
    return {
        "textures": textures,
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": referenced_faces,
            }
        ],
    }


def unformed_directed_model(facing: str) -> dict[str, object]:
    faces = {}
    for direction in DIRECTIONS:
        if direction == facing:
            faces[direction] = oriented_face(
                "directed_front_unformed",
                direction,
                v_direction=natural_down(facing),
            )
        elif direction == OPPOSITE[facing]:
            faces[direction] = oriented_face(
                "directed_back_unformed",
                direction,
                v_direction=natural_down(facing),
            )
        else:
            faces[direction] = oriented_face(
                "directed_side_unformed",
                direction,
                u_direction=facing,
            )
    return model("directed_side_unformed", faces)


def endpoint_model(sequence_direction: str) -> dict[str, object]:
    faces = {}
    for direction in DIRECTIONS:
        if direction == sequence_direction:
            faces[direction] = face("formed_middle_side_edge_occluded", direction)
        elif direction == OPPOSITE[sequence_direction]:
            faces[direction] = oriented_face(
                "controller_back",
                direction,
                v_direction=natural_down(sequence_direction),
            )
        else:
            faces[direction] = oriented_face(
                "controller_side",
                direction,
                v_direction=sequence_direction,
            )
    return model("controller_side", faces)


def member_model(axis: str) -> dict[str, object]:
    axis_directions = AXIS_DIRECTIONS[axis]
    positive_direction = POSITIVE_AXIS_DIRECTION[axis]
    faces = {}
    for direction in DIRECTIONS:
        if direction in axis_directions:
            faces[direction] = face("formed_middle_side_edge_occluded", direction)
        else:
            faces[direction] = oriented_face(
                "undirected_formed_middle_side",
                direction,
                v_direction=positive_direction,
            )
    return model("undirected_formed_middle_side", faces)


def tail_model(sequence_direction: str) -> dict[str, object]:
    faces = {}
    for direction in DIRECTIONS:
        if direction == sequence_direction:
            faces[direction] = oriented_face(
                "tail_back",
                direction,
                v_direction=natural_down(sequence_direction),
            )
        elif direction == OPPOSITE[sequence_direction]:
            faces[direction] = face("formed_middle_side_edge_occluded", direction)
        else:
            faces[direction] = oriented_face(
                "undirected_formed_edge_side",
                direction,
                v_direction=sequence_direction,
            )
    return model("undirected_formed_edge_side", faces)


def member_directed_model(axis: str, facing: str) -> dict[str, object]:
    if AXIS_BY_DIRECTION[facing] == axis:
        raise ValueError(f"Member facing {facing} must be perpendicular to axis {axis}")
    axis_directions = AXIS_DIRECTIONS[axis]
    positive_direction = POSITIVE_AXIS_DIRECTION[axis]
    faces = {}
    for direction in DIRECTIONS:
        if direction in axis_directions:
            faces[direction] = face("formed_middle_side_edge_occluded", direction)
        elif direction == facing:
            faces[direction] = oriented_face(
                "directed_front_formed_middle_side",
                direction,
                v_direction=positive_direction,
            )
        elif direction == OPPOSITE[facing]:
            faces[direction] = oriented_face(
                "directed_back_formed_middle_side",
                direction,
                v_direction=positive_direction,
            )
        else:
            faces[direction] = oriented_face(
                "directed_side_formed_middle_side",
                direction,
                u_direction=facing,
            )
    return model("directed_side_formed_middle_side", faces)


def tail_directed_model(sequence_direction: str, facing: str) -> dict[str, object]:
    if AXIS_BY_DIRECTION[facing] == AXIS_BY_DIRECTION[sequence_direction]:
        raise ValueError(
            f"Tail facing {facing} must be perpendicular to sequence direction {sequence_direction}"
        )
    faces = {}
    for direction in DIRECTIONS:
        if direction == sequence_direction:
            faces[direction] = oriented_face(
                "tail_back",
                direction,
                v_direction=natural_down(sequence_direction),
            )
        elif direction == OPPOSITE[sequence_direction]:
            faces[direction] = face("formed_middle_side_edge_occluded", direction)
        elif direction == facing:
            faces[direction] = oriented_face(
                "directed_front_formed_edge_side",
                direction,
                v_direction=sequence_direction,
            )
        elif direction == OPPOSITE[facing]:
            faces[direction] = oriented_face(
                "directed_back_formed_edge_side",
                direction,
                v_direction=sequence_direction,
            )
        else:
            faces[direction] = oriented_face(
                "directed_side_formed_edge_side",
                direction,
                u_direction=facing,
            )
    return model("directed_side_formed_edge_side", faces)


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def model_id(relative_path: str) -> str:
    return f"appliedpackaging:block/sequence_buffer/generated/{relative_path}"


def add_model(
    records: list[dict[str, object]],
    relative_path: str,
    state: dict[str, str],
    value: dict[str, object],
) -> None:
    write_json(GENERATED_ROOT / f"{relative_path}.json", value)
    records.append(
        {
            "model": model_id(relative_path),
            "state": state,
            "faces": value["elements"][0]["faces"],
        }
    )


def clean_generated_root() -> None:
    resolved_root = ROOT.resolve()
    resolved_model_root = MODEL_ROOT.resolve()
    resolved_generated_root = GENERATED_ROOT.resolve()
    if resolved_generated_root.parent != resolved_model_root or resolved_root not in resolved_generated_root.parents:
        raise RuntimeError(f"Refusing to clean unexpected generated model path: {resolved_generated_root}")
    if resolved_generated_root.exists():
        shutil.rmtree(resolved_generated_root)


def main() -> None:
    clean_generated_root()
    records: list[dict[str, object]] = []
    multipart: list[dict[str, object]] = [
        {
            "when": {"state": "unformed"},
            "apply": {"model": "appliedpackaging:block/sequence_buffer/unformed"},
        }
    ]

    for facing in DIRECTIONS:
        relative = f"unformed_directed/{facing}"
        state = {"state": "unformed_directed", "facing": facing}
        add_model(records, relative, state, unformed_directed_model(facing))
        multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    for sequence_direction in DIRECTIONS:
        relative = f"endpoint/{sequence_direction}"
        state = {"state": "endpoint", "sequence_direction": sequence_direction}
        add_model(records, relative, state, endpoint_model(sequence_direction))
        multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    for axis in AXIS_DIRECTIONS:
        relative = f"member/{axis}"
        state = {"state": "member", "tail": "false", "axis": axis}
        add_model(records, relative, state, member_model(axis))
        multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    for sequence_direction in DIRECTIONS:
        relative = f"tail/{sequence_direction}"
        state = {
            "state": "member",
            "tail": "true",
            "sequence_direction": sequence_direction,
        }
        add_model(records, relative, state, tail_model(sequence_direction))
        multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    for axis in AXIS_DIRECTIONS:
        for facing in DIRECTIONS:
            if AXIS_BY_DIRECTION[facing] == axis:
                continue
            relative = f"member_directed/{axis}/{facing}"
            state = {
                "state": "member_directed",
                "tail": "false",
                "axis": axis,
                "facing": facing,
            }
            add_model(records, relative, state, member_directed_model(axis, facing))
            multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    for sequence_direction in DIRECTIONS:
        for facing in DIRECTIONS:
            if AXIS_BY_DIRECTION[facing] == AXIS_BY_DIRECTION[sequence_direction]:
                continue
            relative = f"tail_directed/{sequence_direction}/{facing}"
            state = {
                "state": "member_directed",
                "tail": "true",
                "sequence_direction": sequence_direction,
                "facing": facing,
            }
            add_model(records, relative, state, tail_directed_model(sequence_direction, facing))
            multipart.append({"when": state, "apply": {"model": model_id(relative)}})

    if len(records) != 57 or len(multipart) != 58:
        raise RuntimeError(
            f"Unexpected orientation count: {len(records)} generated models, {len(multipart)} multipart entries"
        )
    write_json(BLOCKSTATE_PATH, {"multipart": multipart})
    write_json(
        REPORT_PATH,
        {
            "source_sheet_convention": {
                "directed_side_arrow": "texture +U points toward the block facing/front",
                "formed_side_length": "texture +V follows the sequence axis",
            },
            "generated_model_count": len(records),
            "multipart_entry_count": len(multipart),
            "models": records,
        },
    )
    print(f"Generated {len(records)} six-direction Sequence Buffer models at {GENERATED_ROOT}")
    print(f"Wrote {len(multipart)} blockstate entries to {BLOCKSTATE_PATH}")
    print(f"Orientation manifest: {REPORT_PATH}")


if __name__ == "__main__":
    main()
