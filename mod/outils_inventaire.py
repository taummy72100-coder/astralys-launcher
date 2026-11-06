"""Inventaire du joueur facon grimoire magique (remplace textures/gui/container/inventory.png, 256x256).

Les cases sont aux memes positions que l'inventaire de base de la 1.20.1 (InventoryMenu), le jeu reste identique.
Usage : python3 outils_inventaire.py <sortie.png> [apercu.png]
"""
import math, sys
import numpy as np
from PIL import Image

W, H = 176, 166
img = np.zeros((256, 256, 4), np.uint8)
rng = np.random.default_rng(7)

NUIT = np.array([22, 16, 42]); INDIGO = np.array([38, 26, 70]); VIOLET = np.array([96, 60, 170])
OR = np.array([226, 182, 92]); OR_SOMBRE = np.array([132, 92, 40]); OR_CLAIR = np.array([255, 232, 160])
NOIR = np.array([10, 7, 20])


def px(x, y, c, a=255):
    if 0 <= x < 256 and 0 <= y < 256: img[y, x] = (*np.clip(c, 0, 255).astype(int), a)


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1): px(x, y, c)


# ---- fond du panneau : indigo profond, degrade vertical, nebuleuse et etoiles
for y in range(H):
    for x in range(W):
        t = y / H
        n = math.sin(x * .09 + y * .05) * .5 + math.sin(x * .031 - y * .07) * .5
        c = INDIGO * (1 - t * .35) + VIOLET * .18 * max(0, n)
        px(x, y, c)
for _ in range(70):
    x, y = rng.integers(4, W - 4), rng.integers(4, H - 4)
    px(x, y, np.array([200, 190, 255]) * rng.uniform(.4, 1))

# ---- cadre : bord noir, filet dore, liseré clair en haut a gauche, sombre en bas a droite
for x in range(W):
    for y in range(H):
        bord = min(x, y, W - 1 - x, H - 1 - y)
        if bord == 0: px(x, y, NOIR)
        elif bord == 1: px(x, y, OR)
        elif bord == 2: px(x, y, OR_SOMBRE)
        elif bord == 3: px(x, y, NOIR * 1.5)
# coins arrondis (transparents)
for cx, cy in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
    for dx, dy in ((0, 0), (1 if cx == 0 else -1, 0), (0, 1 if cy == 0 else -1)):
        img[cy + dy, cx + dx] = 0
# ornements de coin : losanges dores
for cx, cy in ((5, 5), (W - 6, 5), (5, H - 6), (W - 6, H - 6)):
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if abs(dx) + abs(dy) <= 2: px(cx + dx, cy + dy, OR_CLAIR if abs(dx) + abs(dy) == 0 else OR)


def case(x, y, taille=18, grande=False):
    """Case enfoncee : bord sombre en haut a gauche, clair en bas a droite, fond nuit, lueur violette au centre."""
    x0, y0 = x - 1, y - 1
    for j in range(taille):
        for i in range(taille):
            if i == 0 or j == 0: c = NOIR
            elif i == taille - 1 or j == taille - 1: c = OR_SOMBRE * (1.2 if grande else .9)
            else:
                d = math.hypot(i - taille / 2 + .5, j - taille / 2 + .5) / (taille / 2)
                c = NUIT * (1 - .5 * max(0, 1 - d)) + VIOLET * .35 * max(0, 1 - d) ** 2
            px(x0 + i, y0 + j, c)
    if grande:   # anneau dore autour de la case de resultat
        for i in range(-1, taille + 1):     # anneau dore d'un pixel : ne touche pas le cadre du panneau
            for j in range(-1, taille + 1):
                if i in (-1, taille) or j in (-1, taille): px(x0 + i, y0 + j, OR)


# ---- fenetre du joueur : portail magique (26..75, 8..78)
for y in range(8, 79):
    for x in range(26, 76):
        bord = min(x - 26, y - 8, 75 - x, 78 - y)
        if bord == 0: px(x, y, NOIR); continue
        if bord == 1: px(x, y, OR_SOMBRE); continue
        d = math.hypot((x - 50.5) / 24, (y - 52) / 34)
        c = NOIR * .6 + VIOLET * .55 * max(0, 1 - d) ** 1.5 + np.array([60, 120, 220]) * .25 * max(0, 1 - d * 1.6)
        px(x, y, c)
# cercle de runes au sol du portail
for k in range(48):
    a = k / 48 * 2 * math.pi
    x, y = 50.5 + math.cos(a) * 19, 72 + math.sin(a) * 4.5
    px(int(round(x)), int(round(y)), OR if k % 4 else OR_CLAIR)

# ---- cases (positions de InventoryMenu)
for i in range(4): case(8, 8 + i * 18)            # armure
case(77, 62)                                       # main secondaire
for i in range(2):
    for j in range(2): case(98 + j * 18, 18 + i * 18)   # artisanat 2x2
case(154, 28, grande=True)                         # resultat
for i in range(3):
    for j in range(9): case(8 + j * 18, 84 + i * 18)    # inventaire
for j in range(9): case(8 + j * 18, 142)                 # barre d'action

# ---- fleche runique de l'artisanat
for k in range(12):
    for t in range(-1, 2): px(135 + k, 36 + t, OR if t == 0 else OR_SOMBRE)
for k in range(4):
    for t in range(-k, k + 1): px(149 - k, 36 + t, OR_CLAIR if t == 0 else OR)

# ---- bandeau de parchemin sous le titre "Artisanat" (le jeu ecrit en gris fonce a (97, 8))
for y in range(6, 17):
    for x in range(94, 160):
        bord = min(x - 94, y - 6, 159 - x, 16 - y)
        c = np.array([232, 214, 170]) if bord > 0 else OR_SOMBRE
        if bord > 0 and (x * 7 + y * 3) % 11 == 0: c = c * .93
        px(x, y, c)

# ---- separateur dore au-dessus de l'inventaire
for x in range(8, W - 8):
    px(x, 79, OR_SOMBRE); px(x, 80, OR if x % 6 else OR_CLAIR)

# ---- fonds des effets de potion (utilises par le jeu : 0,166 120x32 et 0,198 32x32)
for (x0, y0, w, h) in ((0, 166, 120, 32), (0, 198, 32, 32)):
    for y in range(h):
        for x in range(w):
            bord = min(x, y, w - 1 - x, h - 1 - y)
            c = NOIR if bord == 0 else OR if bord == 1 else INDIGO * (1 - y / h * .3)
            px(x0 + x, y0 + y, c)

out = Image.fromarray(img, 'RGBA')
out.save(sys.argv[1])
if len(sys.argv) > 2:
    big = out.crop((0, 0, 176, 166)).resize((176 * 4, 166 * 4), Image.NEAREST)
    fond = Image.new('RGBA', big.size, (40, 40, 40, 255)); fond.alpha_composite(big); fond.save(sys.argv[2])
