"""
Placeholder textures for the research book, drawn from gradients, noise and simple shapes.

They stand in until hand-drawn art replaces them: keep the file names and aspect ratios, since the GUI stretches each
one over its area, and the book picks the new art up as it is. The pages themselves are the mod's own left_page.png
and right_page.png. Seals, glow and ribbon are light grey, tinted in the GUI.

Run from the repository root:  python tools/book_textures.py
"""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join('src', 'main', 'resources', 'assets', 'minefantasy2', 'textures', 'gui', 'knowledge', 'book')
# Drawn at twice the GUI size so they stay crisp at GUI scale 2
S = 2
# The mean colour of the mod's page texture, which the map's edge fades into
PAGE = (197, 190, 165)

random.seed(1917)


def ribbon():
    """A bookmark ribbon lying across, its swallowtail end to the right."""
    w, h = 60 * S, 24 * S
    img = Image.new('RGBA', (w, h))
    d = ImageDraw.Draw(img)
    notch = 7 * S
    d.polygon([(0, 0), (w - 1, 0), (w - 1 - notch, h // 2), (w - 1, h - 1), (0, h - 1)], fill=(235, 235, 235, 255))
    px = img.load()
    for y in range(h):
        # Rounded cloth: lighter along the middle, darker at the folds
        k = 1 - 0.35 * abs(y - h / 2) / (h / 2)
        for x in range(w):
            r, g, b, a = px[x, y]
            if a:
                k2 = k + random.uniform(-0.03, 0.03)
                px[x, y] = (int(r * k2), int(g * k2), int(b * k2), a)
    img.save(os.path.join(OUT, 'ribbon.png'))


def shaded(mask, rim):
    """Lights a greyscale shape as if pressed from metal or wax: a darker rim, light from the top left."""
    w, h = mask.size
    inner = mask.filter(ImageFilter.MinFilter(rim * 2 + 1))
    img = Image.new('RGBA', (w, h))
    px, mp, ip = img.load(), mask.load(), inner.load()
    for y in range(h):
        for x in range(w):
            if not mp[x, y]:
                continue
            light = 1 - 0.25 * ((x + y) / (w + h))
            if ip[x, y]:
                v = 225 * light + random.uniform(-6, 6)
            else:
                # The rim, lit on the upper left edge and shadowed on the lower right
                v = (250 if x + y < w else 150) * light
            v = max(0, min(255, int(v)))
            px[x, y] = (v, v, v, mp[x, y])
    return img


def seals():
    """Frames for map entries: a round seal, a scalloped sun for special entries, a rounded diamond for perks."""
    size = 26 * S
    c = size / 2
    r = 12 * S

    mask = Image.new('L', (size, size))
    ImageDraw.Draw(mask).ellipse((c - r, c - r, c + r, c + r), fill=255)
    shaded(mask, 2 * S).save(os.path.join(OUT, 'seal.png'))

    mask = Image.new('L', (size, size))
    points = []
    for i in range(48):
        a = math.pi * 2 * i / 48
        rr = r * (1.0 if i % 4 < 2 else 0.86)
        points.append((c + rr * math.cos(a), c + rr * math.sin(a)))
    ImageDraw.Draw(mask).polygon(points, fill=255)
    shaded(mask, 2 * S).save(os.path.join(OUT, 'seal_special.png'))

    mask = Image.new('L', (size, size))
    d = ImageDraw.Draw(mask)
    k = r * 0.95
    d.polygon([(c, c - k - S), (c + k + S, c), (c, c + k + S), (c - k - S, c)], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(S)).point(lambda v: 255 if v > 110 else 0)
    shaded(mask, 2 * S).save(os.path.join(OUT, 'seal_perk.png'))


def seal_mask(shape, size, r):
    """The outline of a seal's shape, filled, centred in a square of the given size."""
    c = size / 2
    mask = Image.new('L', (size, size))
    d = ImageDraw.Draw(mask)
    if shape == 'round':
        d.ellipse((c - r, c - r, c + r, c + r), fill=255)
    elif shape == 'special':
        points = []
        for i in range(48):
            a = math.pi * 2 * i / 48
            rr = r * (1.0 if i % 4 < 2 else 0.86)
            points.append((c + rr * math.cos(a), c + rr * math.sin(a)))
        d.polygon(points, fill=255)
    else:
        k = r * 1.03
        d.polygon([(c, c - k), (c + k, c), (c, c + k), (c - k, c)], fill=255)
    return mask


def outlines():
    """
    Rings around a seal in each seal's shape, for the pinned entry and search matches: a thin smooth line just
    outside the seal with a soft halo either side. White, tinted in the GUI; drawn at four times the GUI size, and
    filtered smoothly when drawn, so they stay clean at any zoom.
    """
    k = 4
    size = 34 * k
    for shape, name in (('round', 'outline'), ('special', 'outline_special'), ('perk', 'outline_perk')):
        # The seal is 26 across in a 34 square; the line runs a little outside it
        inner = seal_mask(shape, size, 12.6 * k)
        outer = seal_mask(shape, size, 14.4 * k)
        line = ImageChops.subtract(outer, inner).filter(ImageFilter.GaussianBlur(0.6 * k))
        halo = ImageChops.subtract(seal_mask(shape, size, 16.5 * k), seal_mask(shape, size, 11.5 * k))
        halo = halo.filter(ImageFilter.GaussianBlur(2.2 * k)).point(lambda v: int(v * 0.45))
        alpha = ImageChops.lighter(line, halo)
        img = Image.new('RGBA', (size, size), (255, 255, 255, 0))
        img.putalpha(alpha)
        img.save(os.path.join(OUT, name + '.png'))


def glow():
    """A soft ring of light around an entry that can be learned now."""
    size = 40 * S
    img = Image.new('RGBA', (size, size))
    px = img.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            a = max(0.0, 1 - abs(d - 0.62) / 0.36)
            px[x, y] = (255, 255, 255, int(255 * a ** 2))
    img.save(os.path.join(OUT, 'glow.png'))


def spark():
    """A soft round point of light, for sparks and the light that runs along links."""
    size = 16 * S
    img = Image.new('RGBA', (size, size))
    px = img.load()
    c = size / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            px[x, y] = (255, 255, 255, int(255 * max(0.0, 1 - d) ** 2))
    img.save(os.path.join(OUT, 'spark.png'))


def vignette():
    """Page colour fading in from the edges of the map, so the map melts into the page instead of stopping short."""
    w, h = 164 * S, 205 * S
    edge = 20 * S
    img = Image.new('RGBA', (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            d = min(x, w - 1 - x, y, h - 1 - y)
            a = 0 if d >= edge else int(255 * (1 - d / edge) ** 1.2)
            px[x, y] = PAGE + (a,)
    img.save(os.path.join(OUT, 'vignette.png'))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    ribbon()
    seals()
    glow()
    outlines()
    spark()
    vignette()
    print('written to', OUT)
