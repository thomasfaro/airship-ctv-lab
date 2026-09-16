#!/usr/bin/env python3
"""Inline the hosted images of a Message Center HTML body as data URIs.

An inbox message is pasted into the Airship dashboard on its own, so a body that
carries its own images needs nothing deployed alongside it. Each image is
re-encoded to the width it is actually drawn at before being embedded, which is
what keeps the body small enough to paste.

Usage: inline_message_body.py <input.html> <output.html> [--width 512]
"""

from __future__ import annotations

import argparse
import base64
import io
import re
from pathlib import Path

from PIL import Image

WEB_ROOT = Path(__file__).resolve().parents[1]
HOSTED = re.compile(r"https://airship-ctv-web-lab\.netlify\.app/(assets/[^\"' ]+)")


def data_uri(path: Path, width: int) -> str:
    image = Image.open(path).convert("RGB")
    if image.width > width:
        height = round(image.height * width / image.width)
        image = image.resize((width, height), Image.Resampling.LANCZOS)
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=78, optimize=True, progressive=True)
    encoded = base64.b64encode(buffer.getvalue()).decode("ascii")
    return f"data:image/jpeg;base64,{encoded}"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    parser.add_argument("--width", type=int, default=512)
    arguments = parser.parse_args()

    html = arguments.source.read_text(encoding="utf-8")
    inlined: dict[str, str] = {}

    def replace(match: re.Match[str]) -> str:
        relative = match.group(1)
        if relative not in inlined:
            inlined[relative] = data_uri(WEB_ROOT / relative, arguments.width)
        return inlined[relative]

    output = HOSTED.sub(replace, html)
    if not inlined:
        raise SystemExit(f"No hosted asset URL found in {arguments.source}")

    arguments.target.write_text(output, encoding="utf-8")
    print(f"Inlined {len(inlined)} images into {arguments.target}")
    for relative, uri in inlined.items():
        print(f"  {relative}: {len(uri) // 1024} KB")
    print(f"Body size: {len(output) // 1024} KB")


if __name__ == "__main__":
    main()
