"""Objets d'Astralys : textures pixel art 32x32 + modeles Java (ouvrables dans Blockbench).

Usage : python3 outils_objets.py <dossier assets/astralyscarte> [apercu.png]
Les outils sont dessines comme ceux du jeu : manche en bas a gauche, tete en haut a droite.
"""
import json, math, os, sys
import numpy as np
from PIL import Image

N = 32
CONTOUR = (20, 12, 30, 255)


class T:
    def __init__(s): s.p = np.zeros((N, N, 4), np.uint8)

    def px(s, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < N and 0 <= y < N: s.p[y, x] = (*c, 255) if len(c) == 3 else c

    def ligne(s, x0, y0, x1, y1, c, ep=1):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(n + 1):
            t = i / n; x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for dx in range(ep):
                for dy in range(ep): s.px(x + dx - ep // 2, y + dy - ep // 2, c)

    def poly(s, pts, c):
        for y in range(N):
            for x in range(N):
                if dedans(x + .5, y + .5, pts): s.px(x, y, c)

    def disque(s, cx, cy, r, c):
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r: s.px(x, y, c)

    def contour(s):
        a = s.p[..., 3] > 0
        o = np.zeros_like(a)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): o |= np.roll(np.roll(a, dy, 0), dx, 1)
        s.p[o & ~a] = CONTOUR

    def etincelle(s, x, y, c1, c2):
        s.px(x, y, c1)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): s.px(x + dx, y + dy, c2)


def dedans(x, y, pts):
    r = False; j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]; xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi: r = not r
        j = i
    return r


BOIS = [(80, 48, 26), (128, 82, 42), (172, 116, 60)]
OR = [(150, 100, 20), (226, 176, 50), (255, 232, 140)]
VIOLET = [(96, 48, 170), (150, 96, 230), (214, 180, 255)]
ACIER = [(90, 104, 130), (156, 172, 196), (226, 234, 248)]
LUNE = [(70, 110, 190), (130, 180, 245), (220, 240, 255)]


def manche(t, x0, y0, x1, y1, pal=BOIS, bague=OR):
    t.ligne(x0, y0, x1, y1, pal[0], 2)
    t.ligne(x0 - .5, y0 - .5, x1 - .5, y1 - .5, pal[2])
    for k in (0.35, 0.65): t.px(x0 + (x1 - x0) * k, y0 + (y1 - y0) * k, bague[1])


def tete_pioche(t, cx, cy, pal, larg=12):
    # tete en arc perpendiculaire au manche (diagonale bas-gauche -> haut-droite)
    for i in range(-larg, larg + 1):
        u = i / 2
        x, y = cx - u * .707 + abs(u) ** 2 * .05, cy - u * .707 * -1 + abs(u) ** 2 * .05
        x, y = cx + u * .72 + (u * u) * .035, cy + u * .72 - (u * u) * .035
        c = pal[2] if abs(u) < 2 else pal[1] if abs(u) < 4.5 else pal[0]
        for e in range(3): t.px(x - e * .7, y + e * .7, c if e < 2 else pal[0])


def pioche_cristal():
    t = T(); manche(t, 4, 28, 20, 12)
    # tete : deux pointes de cristal violet
    t.poly([(14, 6), (19, 4), (23, 9), (27, 13), (25, 17), (20, 13)], VIOLET[1])
    t.poly([(19, 4), (23, 9), (21, 10), (17, 6)], VIOLET[2])
    t.poly([(14, 6), (8, 5), (11, 9), (17, 9)], VIOLET[0])
    t.poly([(25, 17), (27, 24), (23, 19)], VIOLET[1])
    t.poly([(8, 5), (5, 3), (10, 6)], VIOLET[1])
    t.px(20, 7, (255, 255, 255)); t.px(26, 20, (255, 255, 255))
    t.contour(); t.etincelle(28, 5, (255, 255, 255), VIOLET[2])
    return t


def pioche_celeste():
    t = T(); manche(t, 4, 28, 19, 13, OR, VIOLET)
    t.poly([(10, 5), (18, 3), (24, 8), (29, 14), (28, 21), (23, 13), (17, 9)], LUNE[0])
    t.poly([(18, 3), (24, 8), (22, 9), (16, 5)], LUNE[1])
    t.poly([(24, 8), (29, 14), (27, 15), (22, 10)], LUNE[2])
    t.poly([(10, 5), (6, 4), (13, 7)], LUNE[1])
    for x, y in ((14, 6), (20, 8), (25, 13), (27, 18)): t.px(x, y, (255, 246, 190))
    t.disque(18.5, 9.5, 1.6, OR[1]); t.px(18, 9, OR[2])
    t.contour()
    for x, y in ((4, 6), (29, 26), (11, 15)): t.etincelle(x, y, (255, 250, 220), OR[2])
    return t


def houe_doree():
    t = T(); manche(t, 4, 28, 21, 11)
    t.poly([(19, 7), (27, 5), (28, 9), (23, 11), (21, 13)], OR[1])
    t.poly([(19, 7), (27, 5), (27, 7), (21, 9)], OR[2])
    t.poly([(23, 11), (28, 9), (28, 11), (25, 13)], OR[0])
    # feuille enroulee autour du manche
    for k, (x, y) in enumerate(((12, 19), (13, 18), (14, 19), (11, 20), (13, 20))): t.px(x, y, (90, 170, 70) if k % 2 else (60, 130, 50))
    t.contour(); t.etincelle(27, 15, (255, 250, 200), OR[2])
    return t


def faux_moissons():
    t = T(); manche(t, 4, 29, 17, 6)
    # grande lame en croissant accrochee en haut du manche
    t.poly([(15, 5), (21, 2), (26, 4), (29, 9), (29, 15), (27, 20), (26, 13), (23, 8), (18, 7)], OR[1])
    t.poly([(15, 5), (21, 2), (26, 4), (29, 9), (28, 10), (25, 6), (20, 4)], OR[2])
    t.poly([(26, 13), (23, 8), (18, 7), (19, 8), (24, 10)], OR[0])
    t.disque(16.5, 6, 1.5, (200, 40, 40))
    # gerbe de ble nouee sur le manche
    for dx in (-1, 0, 1): t.ligne(9 + dx, 23, 7 + dx * 2, 18, (214, 176, 60))
    t.px(7, 17, OR[2]); t.px(9, 17, OR[2]); t.px(5, 18, OR[2])
    t.ligne(8, 22, 11, 22, (200, 40, 40))
    t.contour(); t.etincelle(27, 26, (255, 250, 200), OR[2])
    return t


def lame(t, x0, y0, x1, y1, pal, larg=1.6):
    dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy); ux, uy = dx / L, dy / L; nx, ny = -uy, ux
    pts = [(x0 + nx * larg, y0 + ny * larg), (x1, y1), (x0 - nx * larg, y0 - ny * larg)]
    t.poly(pts, pal[1])
    t.ligne(x0 + nx * .4, y0 + ny * .4, x1 - ux * 1.5, y1 - uy * 1.5, pal[2])
    t.ligne(x0 - nx * larg * .8, y0 - ny * larg * .8, x1 - ux * 2, y1 - uy * 2, pal[0])


def dague_chasseur():
    t = T()
    lame(t, 13, 19, 25, 7, ACIER, 2.2)
    t.ligne(10, 16, 16, 22, OR[1], 2)                       # garde
    t.ligne(6, 26, 12, 20, (70, 44, 28), 2)                  # poignee cuir
    for k in range(3): t.px(7 + k * 2, 25 - k * 2, (120, 80, 50))
    t.disque(5, 27, 1.3, OR[1])
    t.contour(); t.etincelle(27, 4, (230, 240, 255), ACIER[2])
    return t


def lame_lunaire():
    t = T()
    lame(t, 11, 21, 29, 3, LUNE, 2.8)
    # croissant de lune sur la garde
    t.disque(10, 18, 3.2, LUNE[2]); t.disque(11.2, 16.8, 2.6, (0, 0, 0, 0))
    t.ligne(7, 15, 14, 22, (200, 210, 230), 2)
    t.ligne(4, 28, 10, 22, (40, 50, 90), 2)
    t.disque(3.5, 28.5, 1.5, LUNE[1])
    t.px(9, 19, (255, 255, 255))
    t.contour()
    for x, y in ((22, 4), (28, 13), (15, 6)): t.etincelle(x, y, (240, 248, 255), LUNE[1])
    return t


def fiole_eternelle():
    t = T()
    t.disque(16, 20, 9, (210, 224, 240)); t.disque(16, 20, 8, (150, 170, 200))
    for y in range(N):
        for x in range(N):
            if (x - 16) ** 2 + (y - 20) ** 2 <= 7.5 ** 2 and y >= 17:
                d = math.hypot(x - 14, y - 23)
                t.px(x, y, (255, 160, 225) if d < 3 else (240, 90, 195) if d < 6 else (176, 40, 150))
    t.poly([(13.5, 5), (18.5, 5), (18.5, 12), (13.5, 12)], (196, 210, 230))
    t.poly([(12, 2), (20, 2), (20, 5), (12, 5)], OR[1]); t.ligne(12, 2, 20, 2, OR[2])
    t.ligne(12, 12, 20, 12, OR[1])                           # bague doree
    t.ligne(10.5, 17, 11.5, 14.5, (255, 255, 255))
    t.contour()
    for x, y in ((25, 8), (6, 11), (27, 23)): t.etincelle(x, y, (255, 240, 255), (240, 150, 230))
    return t


def grimoire():
    t = T()
    t.poly([(6, 6), (24, 4), (27, 25), (9, 28)], (70, 36, 120))     # couverture
    t.poly([(9, 28), (27, 25), (27, 27), (9, 30)], (236, 226, 200))  # pages
    t.ligne(9, 29, 27, 26, (180, 170, 150))
    t.poly([(6, 6), (9, 28), (8, 29), (5, 7)], (46, 22, 80))         # dos
    for x0, y0 in ((6, 6), (24, 4), (27, 25), (9, 28)): t.px(x0, y0, OR[2])
    t.ligne(6, 6, 24, 4, OR[1]); t.ligne(24, 4, 27, 25, OR[1])
    # etoile doree au centre
    cx, cy = 16.5, 15.5
    for k in range(4):
        a = k * math.pi / 2
        t.ligne(cx, cy, cx + math.cos(a) * 5, cy + math.sin(a) * 5, OR[1])
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        t.ligne(cx, cy, cx + math.cos(a) * 2.5, cy + math.sin(a) * 2.5, OR[2])
    t.px(cx, cy, (255, 255, 255))
    t.contour()
    for x, y in ((28, 6), (3, 20)): t.etincelle(x, y, (255, 250, 220), VIOLET[2])
    return t


OBJETS = {
    'pioche_cristal': (pioche_cristal, 'handheld'), 'pioche_celeste': (pioche_celeste, 'handheld'),
    'houe_doree': (houe_doree, 'handheld'), 'faux_moissons': (faux_moissons, 'handheld'),
    'dague_chasseur': (dague_chasseur, 'handheld'), 'lame_lunaire': (lame_lunaire, 'handheld'),
    'fiole_eternelle': (fiole_eternelle, 'generated'), 'grimoire_astralys': (grimoire, 'generated'),
}

if __name__ == '__main__':
    d = sys.argv[1]
    os.makedirs(f'{d}/textures/item', exist_ok=True); os.makedirs(f'{d}/models/item', exist_ok=True)
    planche = Image.new('RGBA', (len(OBJETS) * 36, 36), (40, 32, 64, 255))
    for i, (nom, (f, parent)) in enumerate(OBJETS.items()):
        im = Image.fromarray(f().p, 'RGBA')
        im.save(f'{d}/textures/item/{nom}.png')
        json.dump({'parent': f'minecraft:item/{parent}', 'textures': {'layer0': f'astralyscarte:item/{nom}'}}, open(f'{d}/models/item/{nom}.json', 'w'), indent=2)
        planche.alpha_composite(im, (i * 36 + 2, 2))
    if len(sys.argv) > 2: planche.resize((planche.width * 4, planche.height * 4), Image.NEAREST).save(sys.argv[2])
