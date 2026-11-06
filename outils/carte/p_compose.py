"""Habillage facon Paladium : lueurs, logo Astralys, marqueurs hexagonaux et noms. Usage : python p_compose.py <rendu.png>"""
import json, math, sys
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ICI = '/tmp/claude-0/-home-claude/317e8ecb-ac12-5492-80dc-b9e6c187b40c/scratchpad/'
F = ICI + 'fonts/'
rendu = Image.open(sys.argv[1]).convert('RGB')
W, H = 3344, 1882
rendu = rendu.resize((W, H), Image.LANCZOS)
ancres = json.load(open(ICI + 'p_ancres.json'))
k = W / 3344

# Lueurs : lave, cristaux, portails, fenetres
a = np.asarray(rendu).astype(np.float32)
src = np.asarray(Image.open(ICI + 'p_emission.png').convert('RGB').resize((W, H), Image.BILINEAR)).astype(np.float32) * 1.4
bloom = cv2.GaussianBlur(src, (0, 0), 8) * .8 + cv2.GaussianBlur(src, (0, 0), 26) * .45
a = 255 - (255 - a) * (255 - np.clip(bloom, 0, 255)) / 255
img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert('RGBA')

calque = Image.new('RGBA', (W, H), (0, 0, 0, 0))
dr = ImageDraw.Draw(calque)
f_nom = ImageFont.truetype(F + 'pixelify-700.ttf', 66)


def hexa(cx, cy, r):
    return [(cx + r * math.cos(math.radians(60 * i - 30)), cy + r * math.sin(math.radians(60 * i - 30))) for i in range(6)]


def icone(cx, cy, sorte):
    b = (255, 255, 255, 255)
    if sorte == 'arbre':
        dr.rectangle((cx - 7, cy + 4, cx + 7, cy + 32), fill=b)
        dr.rectangle((cx - 30, cy - 26, cx + 30, cy + 8), fill=b)
        dr.rectangle((cx - 20, cy - 40, cx + 20, cy - 24), fill=b)
    elif sorte == 'flamme':
        dr.polygon([(cx, cy - 40), (cx + 26, cy - 4), (cx + 22, cy + 22), (cx, cy + 34), (cx - 22, cy + 22), (cx - 26, cy - 4)], fill=b)
        dr.polygon([(cx, cy - 6), (cx + 11, cy + 14), (cx, cy + 26), (cx - 11, cy + 14)], fill=(214, 70, 44, 255))
    elif sorte == 'neige':
        for i in range(6):
            t = math.radians(i * 60 + 90)
            dr.line((cx, cy, cx + 34 * math.cos(t), cy + 34 * math.sin(t)), fill=b, width=9)
            bx, by = cx + 20 * math.cos(t), cy + 20 * math.sin(t)
            for sg in (-1, 1):
                dr.line((bx, by, bx + 11 * math.cos(t + sg * .9), by + 11 * math.sin(t + sg * .9)), fill=b, width=7)
    else:  # chateau
        dr.rectangle((cx - 30, cy - 6, cx + 30, cy + 30), fill=b)
        for t in (-30, -12, 6, 24):
            dr.rectangle((cx + t, cy - 18, cx + t + 8, cy - 6), fill=b)
        dr.rectangle((cx - 8, cy + 10, cx + 8, cy + 30), fill=(120, 70, 200, 255))


def marqueur(cx, cy, nom, coul, sorte):
    # ombre portee
    dr.polygon(hexa(cx + 6, cy + 8, 62), fill=(0, 0, 0, 90))
    dr.polygon(hexa(cx, cy, 62), fill=(30, 22, 40, 255))
    dr.polygon(hexa(cx, cy, 56), fill=(255, 244, 220, 255))
    dr.polygon(hexa(cx, cy, 48), fill=coul)
    clair = tuple(min(255, c_ + 40) for c_ in coul[:3]) + (255,)
    dr.polygon(hexa(cx, cy - 4, 40), fill=clair)
    dr.polygon(hexa(cx, cy + 3, 40), fill=coul)
    icone(cx, cy, sorte)
    bb = dr.textbbox((0, 0), nom, font=f_nom)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    y0 = cy + 84
    dr.rounded_rectangle((cx - tw / 2 - 26, y0 - 14, cx + tw / 2 + 26, y0 + th + 22), 10, fill=(20, 16, 28, 150))
    x = cx - tw / 2 - bb[0]
    for ox, oy in ((-4, 0), (4, 0), (0, -4), (0, 6), (4, 6), (-4, 6)):
        dr.text((x + ox, y0 - bb[1] + oy), nom, font=f_nom, fill=(26, 18, 30, 255))
    dr.text((x, y0 - bb[1]), nom, font=f_nom, fill=(255, 255, 255, 255))
    return [cx, cy]


points = {}
for cle, nom, coul, sorte, (dx, dy) in (
        ('pyrelune', 'PYRÉLUNE', (214, 70, 44, 255), 'flamme', (0, 260)),
        ('sylvaria', 'SYLVARIA', (62, 150, 70, 255), 'arbre', (-30, -150)),
        ('azurea', 'AZURÉA', (60, 120, 214, 255), 'neige', (60, -170)),
        ('sunfalls', 'SUNFALLS', (150, 92, 230, 255), 'chateau', (0, -170))):
    ax, ay = ancres[cle]
    points[cle] = marqueur(ax * k + dx, ay * k + dy, nom, coul, sorte)

# Le logo ASTRALYS est grave sur le cartouche du cadre en bois (mod/outils_cadre.py), plus sur la carte.

img.alpha_composite(calque)
img = img.convert('RGB').resize((1672, 941), Image.LANCZOS)
img.save(ICI + 'p_carte.png')
img.save(ICI + 'p_carte.jpg', quality=93)
json.dump({c: [round(x / 2), round(y / 2)] for c, (x, y) in points.items()}, open(ICI + 'p_marqueurs.json', 'w'))
print(json.load(open(ICI + 'p_marqueurs.json')))
