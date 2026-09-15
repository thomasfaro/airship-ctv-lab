#!/usr/bin/env python3
"""Generate Airship carousel assets matching the CTV Lab visual identity."""

from __future__ import annotations

import io
import json
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont


WIDTH, HEIGHT = 1800, 560
RED = (229, 9, 20)
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "assets" / "airship" / "monthly-releases"
FONT_ROOT = Path.home() / ".pyenv/versions/3.11.15/lib/python3.11/site-packages/matplotlib/mpl-data/fonts/ttf"
REGULAR = FONT_ROOT / "DejaVuSans.ttf"
BOLD = FONT_ROOT / "DejaVuSans-Bold.ttf"

FILMS = [
    {
        "id": "sintel",
        "title": "SINTEL",
        "meta": "FANTASY  •  2010  •  15 MIN",
        "copy": "Une quête bouleversante aux confins d’un monde oublié.",
        "image": "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Sintel_movie_4K.webm/960px--Sintel_movie_4K.webm.jpg",
    },
    {
        "id": "spring",
        "title": "SPRING",
        "meta": "DRAME  •  2019  •  8 MIN",
        "copy": "Une jeune bergère affronte l’esprit ancien qui veille sur les saisons.",
        "image": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Spring_-_Blender_Open_Movie.webm/960px--Spring_-_Blender_Open_Movie.webm.jpg",
    },
    {
        "id": "tears-of-steel",
        "title": "TEARS OF STEEL",
        "meta": "SCIENCE-FICTION  •  2012  •  12 MIN",
        "copy": "À Amsterdam, le futur de l’humanité se joue face aux machines.",
        "image": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Tears_of_Steel_1080p.webm/960px--Tears_of_Steel_1080p.webm.jpg",
    },
    {
        "id": "big-buck-bunny",
        "title": "BIG BUCK BUNNY",
        "meta": "ANIMATION  •  2008  •  10 MIN",
        "copy": "Un lapin pacifique prépare sa revanche dans une comédie culte.",
        "image": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c0/Big_Buck_Bunny_4K.webm/960px--Big_Buck_Bunny_4K.webm.jpg",
    },
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(BOLD if bold else REGULAR), size)


def download(url: str) -> Image.Image:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Airship-CTV-Lab/1.0 (banner asset generator; contact: thomas.farouil@airship.com)",
            "Referer": "https://airship-ctv-web-lab.netlify.app/",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return Image.open(io.BytesIO(response.read())).convert("RGB")


def cover(image: Image.Image) -> Image.Image:
    scale = max(WIDTH / image.width, HEIGHT / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = (resized.width - WIDTH) // 2
    top = (resized.height - HEIGHT) // 2
    return resized.crop((left, top, left + WIDTH, top + HEIGHT))


def gradient_overlay() -> Image.Image:
    overlay = Image.new("RGBA", (WIDTH, HEIGHT))
    pixels = overlay.load()
    for x in range(WIDTH):
        horizontal = max(0, 225 - round(x / WIDTH * 300))
        for y in range(HEIGHT):
            bottom = max(0, round((y / HEIGHT) ** 4 * 185))
            pixels[x, y] = (0, 0, 0, min(245, horizontal + bottom))
    return overlay


def base_frame(source: Image.Image) -> Image.Image:
    image = cover(source)
    image = ImageEnhance.Color(image).enhance(0.82)
    image = ImageEnhance.Contrast(image).enhance(1.08)
    return Image.alpha_composite(image.convert("RGBA"), gradient_overlay())


def draw_brand(draw: ImageDraw.ImageDraw) -> None:
    draw.text((74, 44), "CTVLAB", fill=RED, font=font(30, bold=True))
    draw.text((228, 52), "LES SORTIES DU MOIS", fill=(255, 255, 255, 190), font=font(16, bold=True))


def draw_film_banner(source: Image.Image, film: dict[str, str]) -> Image.Image:
    image = base_frame(source)
    draw = ImageDraw.Draw(image)
    draw_brand(draw)
    draw.rectangle((74, 172, 80, 432), fill=RED)
    draw.text((114, 174), "NOUVEAUTÉ", fill=RED, font=font(18, bold=True))
    draw.text((114, 204), film["title"], fill="white", font=font(56, bold=True))
    draw.text((114, 280), film["meta"], fill=(205, 205, 205), font=font(20, bold=True))
    draw.multiline_text(
        (114, 322),
        film["copy"],
        fill=(245, 245, 245),
        font=font(24),
        spacing=9,
    )
    draw.rounded_rectangle((114, 384, 348, 448), radius=8, fill="white")
    draw.text((146, 403), "VOIR LE FILM  ▶", fill=(18, 18, 18), font=font(19, bold=True))
    draw.text((74, 498), "INCLUS DANS VOTRE ABONNEMENT", fill=(255, 255, 255, 160), font=font(15, bold=True))
    return image.convert("RGB")


INTRO_FRAMES = 12


def intro_background(sources: list[Image.Image]) -> Image.Image:
    # Four authentic stills form a strip behind the copy. It stays identical on every
    # frame: GIF stores each later frame as the changed rectangle only, so holding the
    # background still is what buys the palette and the sharpness back.
    image = Image.new("RGB", (WIDTH, HEIGHT), (20, 20, 20))
    cell_width = WIDTH // len(sources)
    for index, source in enumerate(sources):
        tile = cover(source).crop((index * cell_width, 0, (index + 1) * cell_width, HEIGHT))
        image.paste(tile, (index * cell_width, 0))
    image = ImageEnhance.Color(image).enhance(0.78)
    image = ImageEnhance.Brightness(image).enhance(0.74)
    image = Image.alpha_composite(image.convert("RGBA"), Image.new("RGBA", image.size, RED + (52,)))

    # A centre scrim keeps the copy readable without blurring the stills.
    scrim = Image.new("RGBA", image.size)
    pixels = scrim.load()
    for x in range(WIDTH):
        distance = abs(x - WIDTH / 2) / (WIDTH / 2)
        alpha = round(40 + 145 * (1 - distance**2))
        for y in range(HEIGHT):
            pixels[x, y] = (0, 0, 0, alpha)
    return Image.alpha_composite(image, scrim)


def intro_frame(background: Image.Image, phase: int) -> Image.Image:
    image = background.copy()
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw_brand(draw)

    # Never starts fully empty: a slide can be on screen for a single frame.
    reveal = min(1.0, (phase + 2) / 6)
    alpha = round(255 * reveal)
    draw.text(
        (WIDTH // 2, 196),
        "LES SORTIES",
        anchor="mm",
        fill=(255, 255, 255, alpha),
        font=font(64, bold=True),
    )
    draw.text(
        (WIDTH // 2, 268),
        "DU MOIS",
        anchor="mm",
        fill=(255, 255, 255, alpha),
        font=font(64, bold=True),
    )
    line_width = round(460 * reveal)
    draw.rectangle(
        (WIDTH // 2 - line_width // 2, 318, WIDTH // 2 + line_width // 2, 324),
        fill=RED + (alpha,),
    )
    draw.text(
        (WIDTH // 2, 368),
        "4 FILMS  •  4 UNIVERS  •  À DÉCOUVRIR MAINTENANT",
        anchor="mm",
        fill=(255, 255, 255, alpha),
        font=font(20, bold=True),
    )

    button = (WIDTH // 2 - 158, 414, WIDTH // 2 + 158, 478)
    # Once the copy is in, a red halo breathes around the CTA instead of the whole frame.
    if phase >= 5:
        halo = [0, 70, 130, 175, 130, 70, 0][(phase - 5) % 7]
        draw.rounded_rectangle(
            (button[0] - 9, button[1] - 9, button[2] + 9, button[3] + 9),
            radius=13,
            outline=RED + (halo,),
            width=5,
        )
    draw.rounded_rectangle(button, radius=8, fill=(255, 255, 255, alpha))
    draw.text(
        (WIDTH // 2, 446),
        "DÉCOUVRIR  ▶",
        anchor="mm",
        fill=(18, 18, 18, alpha),
        font=font(20, bold=True),
    )
    return Image.alpha_composite(image, layer).convert("RGB")


def intro_gif(sources: list[Image.Image]) -> list[Image.Image]:
    background = intro_background(sources)
    frames = [intro_frame(background, phase) for phase in range(INTRO_FRAMES)]
    # One palette built from a fully drawn frame, reused for all of them: per-frame
    # adaptive palettes shift colours between frames and make the strip crawl.
    palette = frames[-1].quantize(colors=256, method=Image.Quantize.MEDIANCUT)
    return [
        frame.quantize(palette=palette, dither=Image.Dither.FLOYDSTEINBERG) for frame in frames
    ]


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    sources = [download(film["image"]) for film in FILMS]

    manifest = []
    for film, source in zip(FILMS, sources):
        filename = f"{film['id']}.jpg"
        draw_film_banner(source, film).save(OUTPUT / filename, quality=91, optimize=True)
        manifest.append(
            {
                "file": filename,
                "film_id": film["id"],
                "cta_url": f"https://airship-ctv-web-lab.netlify.app/play/{film['id']}",
            }
        )

    frames = intro_gif(sources)
    frames[0].save(
        OUTPUT / "monthly-releases-intro.gif",
        save_all=True,
        append_images=frames[1:],
        duration=[150, 110, 110, 110, 130, 260, 150, 150, 150, 150, 150, 900],
        loop=0,
        optimize=True,
    )

    (OUTPUT / "manifest.json").write_text(
        json.dumps(
            {
                "dimensions": {"width": WIDTH, "height": HEIGHT},
                "intro": "monthly-releases-intro.gif",
                "slides": manifest,
            },
            indent=2,
            ensure_ascii=False,
        )
        + "\n",
        encoding="utf-8",
    )
    print(f"Generated {len(manifest) + 1} assets in {OUTPUT}")


if __name__ == "__main__":
    main()
