"""Barre d'action facon grimoire (textures/gui/hotbar_magie.png, 256x64) :
barre 182x22 en (0,0), selection 24x24 en (0,22), case de main secondaire 29x24 en (24,22) - memes dimensions que le jeu."""
import math, sys
import numpy as np
from PIL import Image

a = np.zeros((64, 256, 4), np.uint8)
NOIR = (10, 7, 20); OR = (226, 182, 92); OR_S = (132, 92, 40); OR_C = (255, 236, 170); NUIT = np.array([22, 16, 42.]); VIOLET = np.array([96, 60, 170.])


def px(x, y, c, al=255): a[y, x] = (*[int(max(0, min(255, v))) for v in c], al)


def socle(x0, y0, w, h):
    for y in range(h):
        for x in range(w):
            b = min(x, y, w - 1 - x, h - 1 - y)
            if b == 0: px(x0 + x, y0 + y, NOIR)
            elif b == 1: px(x0 + x, y0 + y, OR if y < h / 2 else OR_S)
            else:
                d = math.hypot((x - w / 2) / (w / 2), (y - h / 2) / (h / 2))
                px(x0 + x, y0 + y, NUIT * 1.4 * (1 - y / h * .3) + VIOLET * .2 * max(0, 1 - d))


# barre : socle + 9 alveoles de 16x16 (pas de 20 px, comme le jeu)
socle(0, 0, 182, 22)
for i in range(9):
    x0, y0 = 3 + i * 20, 3
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 8
            px(x0 + x, y0 + y, NUIT * (1 - .5 * max(0, 1 - d)) + VIOLET * .35 * max(0, 1 - d) ** 2)
    for k in range(17): px(x0 - 1 + k, y0 - 1, NOIR); px(x0 - 1, y0 - 1 + k, NOIR); px(x0 + 16, y0 + k, OR_S); px(x0 + k, y0 + 16, OR_S)
# petits losanges dores entre les alveoles
for i in range(1, 9):
    x = 1 + i * 20
    for dy in (-1, 0, 1): px(x, 11 + dy, OR_C if dy == 0 else OR)

# selection 24x24 : double cadre dore lumineux, centre transparent, pointes en haut et en bas
for y in range(24):
    for x in range(24):
        b = min(x, y, 23 - x, 23 - y)
        if b == 0: px(x, 22 + y, NOIR)
        elif b == 1: px(x, 22 + y, OR_C)
        elif b == 2: px(x, 22 + y, OR)
        elif b == 3: px(x, 22 + y, (200, 150, 255), 110)
for dx in (-1, 0, 1):
    px(11 + dx, 22, OR_C); px(12 + dx, 22 + 23, OR_C)

# main secondaire 29x24 (le jeu y dessine l'objet en (3,4))
socle(24, 22, 22, 24)
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5) / 8
        px(24 + 3 + x, 22 + 4 + y, NUIT * (1 - .5 * max(0, 1 - d)) + VIOLET * .35 * max(0, 1 - d) ** 2)

Image.fromarray(a, 'RGBA').save(sys.argv[1])
