"""Icones du HUD facon grimoire (textures/gui/hud_magie.png, 64x16), 9x9 chacune :
bouclier plein (0,0), demi (9,0), vide (18,0), coeur (27,0), cuisse (36,0)."""
import sys
import numpy as np
from PIL import Image

a = np.zeros((16, 64, 4), np.uint8)
NOIR = (10, 7, 20); OR = (226, 182, 92); OR_S = (132, 92, 40); OR_C = (255, 236, 170)

BOUCLIER = [".#######.", "#########", "#########", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."]
COEUR = [".##...##.", "####.####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#....", "........."]
CUISSE = ["......bb.", ".....bbbb", ".....bbb.", "....b....", "..###....", ".#####...", "#######..", ".#####...", "..###...."]


def dedans(m, x, y): return 0 <= y < 9 and 0 <= x < 9 and m[y][x] == '#'


def icone(x0, m, couleur, sombre, clair, moitie=None):
    """Bord noir, filet dore sur les cotes, interieur en degrade vertical, reflet en haut a gauche."""
    for y in range(9):
        for x in range(9):
            if not dedans(m, x, y): continue
            bord = not all(dedans(m, x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            plein = moitie is None or (x < 5 if moitie else False)
            if bord: c = NOIR
            elif not all(dedans(m, x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1)): c = (OR if y < 4 else OR_S) if plein else (70, 56, 90)
            else:
                k = y / 8
                c = tuple(int(couleur[i] * (1 - k) + sombre[i] * k) for i in range(3)) if plein else (34, 26, 58)
            a[y, x0 + x] = (*c, 255)
    if moitie is None or moitie:
        for x, y in ((2, 2), (3, 2), (2, 3)):
            if dedans(m, x, y) and a[y, x0 + x, 3]: a[y, x0 + x, :3] = clair


icone(0, BOUCLIER, (170, 130, 255), (70, 40, 150), (235, 220, 255))
icone(9, BOUCLIER, (170, 130, 255), (70, 40, 150), (235, 220, 255), moitie=True)
icone(18, BOUCLIER, (170, 130, 255), (70, 40, 150), (235, 220, 255), moitie=False)
icone(27, COEUR, (255, 80, 96), (150, 16, 40), (255, 210, 215))
icone(36, CUISSE, (226, 130, 60), (130, 60, 24), (255, 220, 170))
for y in range(9):   # l'os de la cuisse en ivoire, sans contour (trop fin)
    for x in range(9):
        if CUISSE[y][x] == 'b': a[y, 36 + x] = (*((246, 238, 220) if x + y < 9 else (196, 184, 160)), 255)
Image.fromarray(a, 'RGBA').save(sys.argv[1])
