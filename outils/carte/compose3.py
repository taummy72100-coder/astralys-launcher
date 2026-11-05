"""Assemble la carte finale : ciel, iles, lueurs, cadre, titre et blasons. Sortie 1672x941."""
import json, math, random
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

random.seed(3)
ICI = '/tmp/claude-0/-home-claude/317e8ecb-ac12-5492-80dc-b9e6c187b40c/scratchpad/'
F = ICI + 'fonts/'
W, H = 3344, 1882
CADRE = 46
OR, OR_F, BRUN = (240, 197, 118), (150, 104, 46), (52, 32, 24)

iles = Image.open(ICI + 'carte3_iles.png')
glow = np.load(ICI + 'carte3_glow.npy').astype(np.float32)
meta = json.load(open(ICI + 'carte3_meta.json'))

# Ciel
y = np.linspace(0, 1, H)[:, None]
ciel = (np.array([112, 164, 230]) * (1 - y) + np.array([198, 214, 248]) * y)[:, None, :].repeat(W, 1).reshape(H, W, 3)
fond = Image.fromarray(ciel.astype(np.uint8)).convert('RGBA')
nuages = Image.new('L', (W, H), 0)
nd = ImageDraw.Draw(nuages)
for cx, cy, n in ((260, 1650, 14), (3050, 1620, 14), (1700, 1840, 18), (3000, 560, 8), (300, 520, 8), (1050, 1780, 9), (2350, 1800, 10)):
    for _ in range(n):
        rx, ry = random.randint(90, 230), random.randint(50, 110)
        x, y0 = cx + random.randint(-260, 260), cy + random.randint(-70, 60)
        nd.ellipse((x - rx, y0 - ry, x + rx, y0 + ry), fill=random.randint(170, 235))
nuages = nuages.filter(ImageFilter.GaussianBlur(28))
blanc = Image.new('RGBA', (W, H), (255, 250, 255, 255))
fond = Image.composite(blanc, fond, nuages)

# Iles
zone_w, zone_h = W - 2 * CADRE - 40, H - 2 * CADRE - 150
s = min(zone_w / iles.width, zone_h / iles.height)
iw, ih = int(iles.width * s), int(iles.height * s)
ox, oy = (W - iw) // 2, CADRE + 130 + (zone_h - ih) // 2
ombre = iles.resize((iw, ih), Image.LANCZOS)
sh = Image.new('RGBA', (iw, ih), (40, 30, 90, 0))
sh.putalpha(ombre.split()[3].point(lambda a: a * 0.35).filter(ImageFilter.GaussianBlur(30)))
fond.alpha_composite(sh, (ox + 30, oy + 60))
fond.alpha_composite(ombre, (ox, oy))

# Fumee du volcan
ax, ay = meta['anchors']['pyrelune']
px, py = ox + ax * s, oy + ay * s
fumee = Image.new('RGBA', (W, H), (0, 0, 0, 0))
fd = ImageDraw.Draw(fumee)
for k in range(16):
    r = 26 + k * 7
    fd.ellipse((px - r + k * 14, py - 40 - k * 26 - r, px + r + k * 14, py - 40 - k * 26 + r), fill=(110, 100, 110, max(0, 150 - k * 9)))
fond.alpha_composite(fumee.filter(ImageFilter.GaussianBlur(14)))

# Lueurs (lave, cristaux, portails)
a = np.array(fond).astype(np.float32)
g = cv2.resize(glow, (iw, ih), interpolation=cv2.INTER_AREA)
lum = np.zeros((H, W, 3), np.float32)
x1, y1 = min(W, ox + iw), min(H, oy + ih)
lum[oy:y1, ox:x1] = g[:y1 - oy, :x1 - ox]
a[..., :3] = 255 - (255 - a[..., :3]) * (255 - np.clip(lum * 1.1, 0, 255)) / 255
# Etincelles magiques
for _ in range(260):
    x, y = random.randrange(CADRE, W - CADRE), random.randrange(CADRE, H - CADRE)
    r = random.choice([2, 2, 3, 4])
    c = random.choice([(255, 240, 200), (220, 190, 255), (200, 230, 255)])
    cv2.circle(a, (x, y), r, (*c, 255), -1)
img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
dr = ImageDraw.Draw(img)

# Cadre facon blocs de pierre + filets d'or
pierre = [(70, 66, 82), (84, 80, 96), (58, 54, 70), (96, 90, 108)]
B = 23
for x in range(0, W, B):
    for y in range(0, H, B):
        if x < CADRE or x >= W - CADRE or y < CADRE or y >= H - CADRE:
            c = random.choice(pierre)
            dr.rectangle((x, y, x + B - 1, y + B - 1), fill=c)
            dr.line((x, y, x + B - 1, y), fill=tuple(min(255, v + 22) for v in c), width=2)
            dr.line((x, y + B - 1, x + B - 1, y + B - 1), fill=tuple(max(0, v - 22) for v in c), width=2)
for i, (w_, c) in enumerate(((7, OR_F), (4, OR), (2, (255, 240, 200)))):
    e = CADRE - 2 + i * 3
    dr.rectangle((e, e, W - e, H - e), outline=c, width=w_)
dr.rectangle((8, 8, W - 8, H - 8), outline=OR_F, width=6)
for cx, cy in ((CADRE, CADRE), (W - CADRE, CADRE), (CADRE, H - CADRE), (W - CADRE, H - CADRE)):
    for r, c in ((40, OR_F), (32, OR), (18, (150, 90, 230)), (9, (230, 210, 255))):
        dr.polygon([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=c)

# Bandeau de titre
f_titre = ImageFont.truetype(F + 'cinzel-decorative-latin-900-normal.ttf', 112)
f_sous = ImageFont.truetype(F + 'cinzel-latin-700-normal.ttf', 40)
f_nom = ImageFont.truetype(F + 'cinzel-latin-900-normal.ttf', 64)


def plaque(cx, cy, w, h, rayon=18):
    dr.rounded_rectangle((cx - w / 2 - 10, cy - h / 2 - 10, cx + w / 2 + 10, cy + h / 2 + 10), rayon + 6, fill=OR_F)
    dr.rounded_rectangle((cx - w / 2 - 5, cy - h / 2 - 5, cx + w / 2 + 5, cy + h / 2 + 5), rayon + 3, fill=OR)
    dr.rounded_rectangle((cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), rayon, fill=BRUN)
    dr.rounded_rectangle((cx - w / 2 + 8, cy - h / 2 + 8, cx + w / 2 - 8, cy + h / 2 - 8), rayon - 4, outline=(110, 74, 40), width=3)


def texte(cx, cy, t, f, c=(255, 244, 222)):
    bb = dr.textbbox((0, 0), t, font=f)
    x, y = cx - (bb[2] - bb[0]) / 2 - bb[0], cy - (bb[3] - bb[1]) / 2 - bb[1]
    dr.text((x + 4, y + 5), t, font=f, fill=(20, 10, 10))
    dr.text((x, y), t, font=f, fill=c)


plaque(W / 2, 104, 980, 150, 24)
texte(W / 2, 92, 'ASTRALYS', f_titre)
texte(W / 2, 156, '✦  Le royaume des trois mondes  ✦'.replace('✦', '*'), f_sous, OR)


def blason(cx, cy, couleur, icone):
    w, h = 120, 140
    pts = [(cx - w / 2, cy - h / 2), (cx + w / 2, cy - h / 2), (cx + w / 2, cy + 10), (cx, cy + h / 2), (cx - w / 2, cy + 10)]
    big = [((x - cx) * 1.16 + cx, (y - cy) * 1.16 + cy) for x, y in pts]
    dr.polygon(big, fill=OR_F)
    dr.polygon([((x - cx) * 1.08 + cx, (y - cy) * 1.08 + cy) for x, y in pts], fill=OR)
    dr.polygon(pts, fill=couleur)
    blanc = (255, 248, 230)
    if icone == 'arbre':
        dr.rectangle((cx - 8, cy, cx + 8, cy + 34), fill=(120, 80, 40))
        for ox_, oy_, r in ((0, -18, 30), (-22, 2, 20), (22, 2, 20)):
            dr.ellipse((cx + ox_ - r, cy + oy_ - r, cx + ox_ + r, cy + oy_ + r), fill=(120, 220, 110))
    elif icone == 'flamme':
        dr.polygon([(cx, cy - 48), (cx + 30, cy - 4), (cx + 26, cy + 26), (cx, cy + 42), (cx - 26, cy + 26), (cx - 30, cy - 4)], fill=(255, 200, 70))
        dr.polygon([(cx, cy - 14), (cx + 14, cy + 12), (cx, cy + 32), (cx - 14, cy + 12)], fill=(255, 250, 200))
    else:
        for k in range(6):
            t = math.radians(k * 60)
            dr.line((cx, cy - 4, cx + 40 * math.cos(t), cy - 4 + 40 * math.sin(t)), fill=blanc, width=8)
            for d in (22,):
                bx, by = cx + d * math.cos(t), cy - 4 + d * math.sin(t)
                for sgn in (-1, 1):
                    dr.line((bx, by, bx + 12 * math.cos(t + sgn * 0.9), by + 12 * math.sin(t + sgn * 0.9)), fill=blanc, width=6)


zones = {}
for cle, nom, couleur, icone, (dx, dy) in (('sylvaria', 'SYLVARIA', (52, 128, 60), 'arbre', (120, 90)),
                                          ('pyrelune', 'PYRÉLUNE', (170, 40, 30), 'flamme', (-420, 420)),
                                          ('azurea', 'AZURÉA', (50, 96, 190), 'neige', (330, 230))):
    ax, ay = meta['anchors'][cle]
    cx, cy = ox + ax * s + dx, oy + ay * s + dy
    bb = dr.textbbox((0, 0), nom, font=f_nom)
    tw = bb[2] - bb[0] + 90
    plaque(cx, cy, tw, 96)
    texte(cx, cy - 2, nom, f_nom)
    blason(cx, cy - 140, couleur, icone)
    # zone cliquable (repere 1672x941) : autour du blason, de l'etiquette et du coeur de l'ile
    mx, my = (cx + ox + ax * s) / 2, (cy - 70 + oy + ay * s) / 2  # entre l'etiquette et le coeur de l'ile
    zones[cle] = [int(mx / 2 - 170), int(my / 2 - 140), 340, 280]

img = img.resize((1672, 941), Image.LANCZOS)
img.save(ICI + 'carte3_finale.png')
img.convert('RGB').save(ICI + 'carte3_finale.jpg', quality=92)
json.dump(zones, open(ICI + 'carte3_zones.json', 'w'))
print(zones)
