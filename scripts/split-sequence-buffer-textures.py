from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = Path(r"E:\resources\textures\appliedpackaging\ret\sequance_buffer_all.png")
DEFAULT_OUTPUT = (
    ROOT
    / "src/main/resources/assets/appliedpackaging/textures/block/sequence_buffer/faces"
)
DEFAULT_REPORT_DIR = ROOT / "build/asset-reference/sequence-buffer/user-sheet"

SHEET_SIZE = 64
TILE_SIZE = 16

# Row semantics, top to bottom:
#   undirected, directed front, directed side, directed back.
# Column semantics, left to right:
#   unformed, formed middle, formed edge, special.
TILES: tuple[tuple[str, ...], ...] = (
    (
        "undirected_unformed",
        "undirected_formed_middle_side",
        "undirected_formed_edge_side",
        "controller_back",
    ),
    (
        "directed_front_unformed",
        "directed_front_formed_middle_side",
        "directed_front_formed_edge_side",
        "formed_middle_side_edge_occluded",
    ),
    (
        "directed_side_unformed",
        "directed_side_formed_middle_side",
        "directed_side_formed_edge_side",
        "controller_side",
    ),
    (
        "directed_back_unformed",
        "directed_back_formed_middle_side",
        "directed_back_formed_edge_side",
        "tail_back",
    ),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Split the user-authored 4x4 Sequence Buffer sheet into 16 RGBA textures."
    )
    parser.add_argument("source", nargs="?", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--report-dir", type=Path, default=DEFAULT_REPORT_DIR)
    return parser.parse_args()


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def validate_source(source: Path, image: Image.Image) -> None:
    if not source.is_file():
        raise FileNotFoundError(f"Sequence Buffer source sheet does not exist: {source}")
    if image.size != (SHEET_SIZE, SHEET_SIZE):
        raise ValueError(
            f"Sequence Buffer source sheet must be {SHEET_SIZE}x{SHEET_SIZE}, got "
            f"{image.width}x{image.height}: {source}"
        )
    if image.mode != "RGBA":
        raise ValueError(
            f"Sequence Buffer source sheet must be RGBA without conversion, got {image.mode}: {source}"
        )


def split_sheet(image: Image.Image, output: Path) -> dict[str, Image.Image]:
    output.mkdir(parents=True, exist_ok=True)
    tiles: dict[str, Image.Image] = {}
    for row, names in enumerate(TILES):
        for column, name in enumerate(names):
            box = (
                column * TILE_SIZE,
                row * TILE_SIZE,
                (column + 1) * TILE_SIZE,
                (row + 1) * TILE_SIZE,
            )
            tile = image.crop(box)
            if tile.mode != "RGBA" or tile.size != (TILE_SIZE, TILE_SIZE):
                raise RuntimeError(f"Invalid split result for {name}: {tile.mode} {tile.size}")
            tile.save(output / f"{name}.png", format="PNG", optimize=False)
            tiles[name] = tile
    return tiles


def verify_round_trip(source: Image.Image, tiles: dict[str, Image.Image]) -> None:
    reconstructed = Image.new("RGBA", source.size)
    for row, names in enumerate(TILES):
        for column, name in enumerate(names):
            reconstructed.paste(tiles[name], (column * TILE_SIZE, row * TILE_SIZE))
    if reconstructed.tobytes() != source.tobytes():
        raise RuntimeError("Split textures do not reconstruct the source sheet pixel-for-pixel")


def write_proof_sheet(tiles: dict[str, Image.Image], report_dir: Path) -> None:
    report_dir.mkdir(parents=True, exist_ok=True)
    scale = 8
    gap = 4
    scaled_tile = TILE_SIZE * scale
    size = scaled_tile * 4 + gap * 5
    proof = Image.new("RGBA", (size, size), (16, 18, 20, 255))
    for row, names in enumerate(TILES):
        for column, name in enumerate(names):
            position = (gap + column * (scaled_tile + gap), gap + row * (scaled_tile + gap))
            proof.alpha_composite(
                tiles[name].resize((scaled_tile, scaled_tile), Image.Resampling.NEAREST),
                position,
            )
    proof.save(report_dir / "split-proof-sheet.png", format="PNG", optimize=False)


def write_manifest(source: Path, output: Path, report_dir: Path) -> None:
    manifest = {
        "source": str(source.resolve()),
        "source_sha256": sha256(source),
        "source_size": [SHEET_SIZE, SHEET_SIZE],
        "tile_size": [TILE_SIZE, TILE_SIZE],
        "output": str(output.resolve()),
        "tiles": [
            {
                "name": name,
                "row": row,
                "column": column,
                "box": [
                    column * TILE_SIZE,
                    row * TILE_SIZE,
                    (column + 1) * TILE_SIZE,
                    (row + 1) * TILE_SIZE,
                ],
                "sha256": sha256(output / f"{name}.png"),
            }
            for row, names in enumerate(TILES)
            for column, name in enumerate(names)
        ],
    }
    (report_dir / "split-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    args = parse_args()
    source = args.source.resolve()
    output = args.output.resolve()
    report_dir = args.report_dir.resolve()

    with Image.open(source) as opened:
        opened.load()
        validate_source(source, opened)
        source_image = opened.copy()

    tiles = split_sheet(source_image, output)
    verify_round_trip(source_image, tiles)
    write_proof_sheet(tiles, report_dir)
    write_manifest(source, output, report_dir)
    print(f"Split {source} into {len(tiles)} textures at {output}")
    print(f"Verified pixel-perfect round trip; report: {report_dir}")


if __name__ == "__main__":
    main()
