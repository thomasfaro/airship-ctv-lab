#!/usr/bin/env python3
"""Build the Message Center list icons for the monthly releases message.

The Android inbox row draws the icon in a 92 x 62 dp box (184 x 124 px on a 1080p
TV) and crops it to fill, so the 3:2 icon is authored at twice that box and every
element is sized to survive the downscale. A square variant is kept for surfaces
that ask for a 1:1 list icon.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFont

RED = (229, 9, 20)
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets" / "airship" / "monthly-releases-inbox"
CARDS = ["cinema.jpg", "series.jpg", "reality-tv.jpg"]
BOLD = Path("/usr/share/fonts/truetype/croscore/Arimo-Bold.ttf")
SIZES = {"list-icon.png": (368, 248), "list-icon-square.png": (300, 300)}


def slice_of(card: Path, width: int, height: int) -> Image.Image:
    image = Image.open(card).convert("RGB")
    scale = max(width / image.width, height / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = (resized.width - width) // 2
    top = (resized.height - height) // 2
    return resized.crop((left, top, left + width, top + height))


def triptych(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size, (20, 20, 20))
    edges = [round(index * width / len(CARDS)) for index in range(len(CARDS) + 1)]
    for index, card in enumerate(CARDS):
        left, right = edges[index], edges[index + 1]
        image.paste(slice_of(ASSETS / card, right - left, height), (left, 0))
        if index:
            # A dark seam keeps the three stills readable as three titles.
            ImageDraw.Draw(image).rectangle((left - 1, 0, left + 1, height), fill=(12, 12, 12))
    image = ImageEnhance.Color(image).enhance(0.86)
    return ImageEnhance.Brightness(image).enhance(0.72)


def scrim(size: tuple[int, int]) -> Image.Image:
    overlay = Image.new("RGBA", size)
    pixels = overlay.load()
    for y in range(size[1]):
        alpha = round(30 + 130 * (y / size[1]) ** 3)
        for x in range(size[0]):
            pixels[x, y] = (0, 0, 0, alpha)
    return overlay


def draw_icon(size: tuple[int, int]) -> Image.Image:
    width, height = size
    image = Image.alpha_composite(triptych(size).convert("RGBA"), scrim(size))
    draw = ImageDraw.Draw(image)

    badge = (18, 18, round(width * 0.36), 74)
    draw.rounded_rectangle(badge, radius=6, fill=RED)
    draw.text(
        ((badge[0] + badge[2]) / 2, (badge[1] + badge[3]) / 2 + 1),
        "NEW",
        anchor="mm",
        fill="white",
        font=ImageFont.truetype(str(BOLD), 38),
    )

    # Reads as "three titles" once the icon is down at 92 dp.
    for index in range(3):
        left = 20 + index * 30
        draw.rounded_rectangle((left, height - 40, left + 20, height - 32), radius=4, fill="white")

    draw.rectangle((0, height - 8, width, height), fill=RED)
    return image.convert("RGB")


def main() -> None:
    for name, size in SIZES.items():
        target = ASSETS / name
        draw_icon(size).save(target, optimize=True)
        print(f"Wrote {target} ({target.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
