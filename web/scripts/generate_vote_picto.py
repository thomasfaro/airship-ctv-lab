#!/usr/bin/env python3
"""Build the Message Center list icons for the Top Chef audience vote message.

Nothing is photographic here: the four chefs are drawn with the same gradients
and initials the message body uses, so the row artwork and the vote screen read
as one piece. The 3:2 icon is authored at 368 x 248 because the Android inbox
draws it in a 92 x 62 dp box (184 x 124 px on a 1080p TV) and crops it to fill.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

GOLD = (246, 196, 83)
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets" / "airship" / "top-chef-vote"
BOLD = Path("/usr/share/fonts/truetype/croscore/Arimo-Bold.ttf")
SIZES = {"list-icon.png": (368, 248), "list-icon-square.png": (300, 300)}

# Initials and gradient of each chef, matching the .maya / .noah / .sofia / .liam
# portraits in docs/top-chef-vote.html.
CHEFS = [
    ("MC", (217, 99, 69), (100, 39, 50)),
    ("NW", (63, 138, 122), (21, 61, 71)),
    ("SR", (143, 97, 173), (53, 41, 88)),
    ("LO", (203, 148, 72), (85, 64, 30)),
]


def gradient(size: tuple[int, int], start: tuple[int, int, int], end: tuple[int, int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size)
    pixels = image.load()
    span = max(1, width + height - 2)
    for y in range(height):
        for x in range(width):
            ratio = (x + y) / span
            pixels[x, y] = tuple(
                round(start[channel] + (end[channel] - start[channel]) * ratio) for channel in range(3)
            )
    return image


def band(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, (20, 20, 20))
    draw = ImageDraw.Draw(image)
    edges = [round(index * width / len(CHEFS)) for index in range(len(CHEFS) + 1)]
    initials = ImageFont.truetype(str(BOLD), 44)

    for index, (label, start, end) in enumerate(CHEFS):
        left, right = edges[index], edges[index + 1]
        image.paste(gradient((right - left, height), start, end), (left, 0))
        # The initials sit low so the badge never lands on top of one.
        draw.text(
            ((left + right) / 2, height * 0.62),
            label,
            anchor="mm",
            fill=(255, 255, 255),
            font=initials,
        )
        if index:
            draw.rectangle((left - 1, 0, left + 1, height), fill=(12, 12, 12))

    return image


def scrim(size: tuple[int, int]) -> Image.Image:
    overlay = Image.new("RGBA", size)
    pixels = overlay.load()
    for y in range(size[1]):
        alpha = round(96 * (1 - y / size[1]) ** 2)
        for x in range(size[0]):
            pixels[x, y] = (0, 0, 0, alpha)
    return overlay


def draw_icon(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.alpha_composite(band(size).convert("RGBA"), scrim(size))
    draw = ImageDraw.Draw(image)

    badge = (18, 18, round(width * 0.42), 74)
    draw.rounded_rectangle(badge, radius=6, fill=GOLD)
    draw.text(
        ((badge[0] + badge[2]) / 2, (badge[1] + badge[3]) / 2 + 1),
        "VOTE",
        anchor="mm",
        fill=(26, 18, 8),
        font=ImageFont.truetype(str(BOLD), 38),
    )

    draw.rectangle((0, height - 8, width, height), fill=GOLD)
    return image.convert("RGB")


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    for name, size in SIZES.items():
        target = ASSETS / name
        draw_icon(size).save(target, optimize=True)
        print(f"Wrote {target} ({target.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
