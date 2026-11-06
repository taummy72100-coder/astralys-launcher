"""Skins 64x64 des PNJ d'Astralys (format joueur classique, ouvrable dans Blockbench ou tout editeur de skin).

Usage : python3 outils_pnj.py <dossier textures/entity/pnj> [apercu.png]
"""
import os, sys
import numpy as np
from PIL import Image

rng = np.random.default_rng(7)


def faces(u, v, w, h, d):
    """Rectangles (x, y, l, h) des 6 faces d'une boite du skin : dessus, dessous, droite, devant, gauche, dos."""
    return {'haut': (u + d, v, w, d), 'bas': (u + d + w, v, w, d), 'droite': (u, v + d, d, h),
            'avant': (u + d, v + d, w, h), 'gauche': (u + d + w, v + d, d, h), 'dos': (u + 2 * d + w, v + d, w, h)}


TETE, CHAPEAU = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
CORPS, VESTE = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
BRAS_D, MANCHE_D = (40, 16, 4, 12, 4), (40, 32, 4, 12, 4)
BRAS_G, MANCHE_G = (32, 48, 4, 12, 4), (48, 48, 4, 12, 4)
JAMBE_D, JAMBE_G = (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)
OMBRE = {'haut': 1.1, 'bas': .7, 'droite': .85, 'avant': 1, 'gauche': .85, 'dos': .8}


class Skin:
    def __init__(s): s.p = np.zeros((64, 64, 4), np.uint8)

    def boite(s, b, c, grain=10, sauf=()):
        for f, (x, y, w, h) in faces(*b).items():
            if f in sauf: continue
            k = OMBRE[f]
            for j in range(h):
                for i in range(w):
                    n = rng.integers(-grain, grain + 1)
                    s.p[y + j, x + i] = (*[int(max(0, min(255, ch * k + n))) for ch in c], 255)

    def px(s, x, y, c): s.p[y, x] = (*c, 255) if len(c) == 3 else c

    def zone(s, x, y, w, h, c):
        for j in range(h):
            for i in range(w): s.px(x + i, y + j, c)

    def bande(s, b, y0, h, c):
        """Bande horizontale autour d'une boite (ceinture, bordure) a la ligne y0 de la hauteur."""
        for f, (x, y, w, hh) in faces(*b).items():
            if f in ('haut', 'bas'): continue
            s.zone(x, y + y0, w, h, c)

    def avant(s, b): return faces(*b)['avant'][:2]


PEAU = (222, 172, 132)
YEUX_BLANC = (240, 240, 240)


def visage(s, yeux, bouche=(150, 90, 70), peau=PEAU):
    s.boite(TETE, peau, 6)
    x, y = s.avant(TETE)
    s.px(x + 1, y + 4, YEUX_BLANC); s.px(x + 2, y + 4, yeux); s.px(x + 5, y + 4, yeux); s.px(x + 6, y + 4, YEUX_BLANC)
    s.zone(x + 3, y + 6, 2, 1, bouche)
    s.zone(x + 3, y + 5, 2, 1, tuple(int(c * .9) for c in peau))  # nez


def cheveux(s, c, frange=2, cotes=True):
    x, y, w, h = faces(*TETE)['haut']; s.zone(x, y, w, h, c)
    for f in ('avant', 'droite', 'gauche', 'dos'):
        fx, fy, fw, fh = faces(*TETE)[f]
        s.zone(fx, fy, fw, frange if f == 'avant' else (fh if f == 'dos' else 4), c)
    if cotes:
        x, y = s.avant(TETE); s.zone(x, y + 2, 1, 2, c); s.zone(x + 7, y + 2, 1, 2, c)


def jambes(s, c, chaussure=(50, 34, 24)):
    for b in (JAMBE_D, JAMBE_G):
        s.boite(b, c)
        s.bande(b, 10, 2, chaussure)
        x, y, w, h = faces(*b)['bas']; s.zone(x, y, w, h, chaussure)


def bras(s, c, main=PEAU, manchette=None):
    for b in (BRAS_D, BRAS_G):
        s.boite(b, c)
        s.bande(b, 10, 2, main)
        x, y, w, h = faces(*b)['bas']; s.zone(x, y, w, h, main)
        if manchette: s.bande(b, 9, 1, manchette)


def couche(s, b, c, alpha=255, lignes=None):
    """Remplit la couche exterieure (veste, chapeau...) sur certaines lignes de la hauteur."""
    for f, (x, y, w, h) in faces(*b).items():
        if f in ('haut', 'bas'):
            if lignes is None: s.zone(x, y, w, h, (*c, alpha))
            continue
        for j in (range(h) if lignes is None else lignes):
            for i in range(w):
                n = rng.integers(-8, 9)
                s.p[y + j, x + i] = (*[int(max(0, min(255, ch * OMBRE[f] + n))) for ch in c], alpha)


OR, OR_C = (214, 164, 40), (250, 214, 100)


def marchand():
    s = Skin(); visage(s, (70, 50, 30)); cheveux(s, (92, 58, 30))
    x, y = s.avant(TETE); s.zone(x + 2, y + 5, 4, 1, (92, 58, 30))       # moustache
    s.boite(CORPS, (120, 74, 38)); s.bande(CORPS, 7, 1, (60, 38, 20))
    fx, fy = s.avant(CORPS); s.px(fx + 3, fy + 7, OR_C); s.px(fx + 4, fy + 7, OR_C)
    s.zone(fx + 2, fy + 8, 4, 4, (226, 210, 170))                        # tablier
    s.zone(fx + 3, fy + 9, 2, 2, OR)                                     # bourse
    s.zone(fx, fy, 8, 1, OR); s.zone(fx + 3, fy + 1, 2, 5, (230, 220, 190))
    bras(s, (120, 74, 38), manchette=OR)
    jambes(s, (70, 52, 36))
    couche(s, CHAPEAU, (120, 30, 30), lignes=range(0, 2))               # beret rouge
    x, y, w, h = faces(*CHAPEAU)['haut']; s.zone(x, y, w, h, (140, 36, 36)); s.px(x + 4, y + 3, OR_C)
    return s


def maitre_metiers():
    s = Skin(); visage(s, (60, 90, 160), peau=(214, 164, 126)); cheveux(s, (190, 190, 196))
    x, y = s.avant(TETE)
    s.zone(x + 1, y + 5, 6, 3, (214, 214, 220)); s.zone(x + 3, y + 6, 2, 1, (150, 90, 70))   # barbe grise
    s.zone(x + 1, y + 3, 2, 1, (214, 214, 220)); s.zone(x + 5, y + 3, 2, 1, (214, 214, 220))  # sourcils
    s.boite(CORPS, (150, 32, 32))
    fx, fy = s.avant(CORPS)
    s.zone(fx + 2, fy, 4, 3, (214, 214, 220))                            # barbe longue
    s.zone(fx + 3, fy + 3, 2, 1, (200, 200, 206))
    s.bande(CORPS, 6, 1, OR); s.px(fx + 4, fy + 6, OR_C)
    s.zone(fx + 3, fy + 7, 1, 5, (110, 22, 22))
    bras(s, (150, 32, 32), manchette=OR)
    jambes(s, (120, 26, 26), (60, 30, 20))
    couche(s, VESTE, (150, 32, 32), lignes=range(8, 12))                # bas de robe
    return s


def heraut():
    s = Skin(); visage(s, (40, 110, 60)); cheveux(s, (40, 28, 20), frange=1)
    s.boite(CORPS, (92, 40, 150))
    fx, fy = s.avant(CORPS)
    s.zone(fx + 3, fy, 2, 12, OR)                                        # bande royale
    s.zone(fx + 2, fy + 3, 4, 3, (240, 230, 210)); s.zone(fx + 3, fy + 4, 2, 1, (92, 40, 150))  # blason
    s.bande(CORPS, 8, 1, (40, 20, 70))
    bras(s, (92, 40, 150), manchette=OR)
    jambes(s, (40, 24, 70))
    # couronne doree sur la couche chapeau
    for f in ('avant', 'droite', 'gauche', 'dos'):
        x, y, w, h = faces(*CHAPEAU)[f]
        s.zone(x, y + 1, w, 1, OR)
        for i in range(0, w, 2): s.px(x + i, y, OR_C)
    x, y = s.avant(CHAPEAU); s.px(x + 3, y + 1, (220, 40, 60)); s.px(x + 4, y + 1, (220, 40, 60))
    return s


def commissaire():
    s = Skin(); visage(s, (50, 50, 50)); cheveux(s, (30, 30, 34))
    x, y = s.avant(TETE)
    s.px(x + 5, y + 4, (180, 220, 240)); s.px(x + 6, y + 3, OR_C); s.px(x + 6, y + 5, OR_C)   # monocle
    s.zone(x + 2, y + 5, 4, 1, (30, 30, 34))
    s.boite(CORPS, (24, 120, 120))
    fx, fy = s.avant(CORPS)
    s.zone(fx + 3, fy, 2, 6, (236, 236, 236)); s.px(fx + 3, fy + 1, (160, 30, 40)); s.px(fx + 4, fy + 1, (160, 30, 40))
    for k in range(3): s.px(fx + 5, fy + 6 + k * 2, OR_C)
    s.zone(fx + 1, fy + 3, 1, 1, OR)
    bras(s, (24, 120, 120), manchette=(236, 236, 236))
    jambes(s, (30, 30, 40), (20, 20, 24))
    couche(s, VESTE, (20, 100, 100), lignes=range(8, 12))               # pans du manteau
    # haut-de-forme : couche chapeau, bord + calotte
    couche(s, CHAPEAU, (26, 26, 30), lignes=range(0, 3))
    for f in ('avant', 'droite', 'gauche', 'dos'):
        x, y, w, h = faces(*CHAPEAU)[f]; s.zone(x, y + 2, w, 1, (24, 120, 120))
    return s


def guide():
    s = Skin(); visage(s, (90, 200, 255), peau=(200, 156, 120)); cheveux(s, (200, 180, 120))
    s.boite(CORPS, (40, 70, 150))
    fx, fy = s.avant(CORPS)
    s.zone(fx + 3, fy, 2, 12, (30, 50, 110)); s.px(fx + 3, fy + 1, OR_C); s.px(fx + 4, fy + 1, OR_C)
    s.bande(CORPS, 7, 1, (110, 76, 40)); s.zone(fx + 1, fy + 8, 2, 2, (110, 76, 40))   # ceinture + sacoche
    bras(s, (40, 70, 150), main=(200, 156, 120))
    jambes(s, (70, 58, 44), (60, 40, 24))
    couche(s, VESTE, (36, 60, 130), lignes=range(0, 12))                # cape
    x, y, w, h = faces(*VESTE)['avant']; s.zone(x + 2, y, 4, 12, (0, 0, 0, 0))
    # capuche : couche chapeau, visage degage
    couche(s, CHAPEAU, (36, 60, 130))
    x, y, w, h = faces(*CHAPEAU)['avant']; s.zone(x + 1, y + 2, 6, 6, (0, 0, 0, 0))
    x, y, w, h = faces(*CHAPEAU)['bas']; s.zone(x, y, w, h, (0, 0, 0, 0))
    return s


PNJ = {'marchand': marchand, 'maitre_metiers': maitre_metiers, 'heraut': heraut, 'commissaire': commissaire, 'guide': guide}

if __name__ == '__main__':
    d = sys.argv[1]; os.makedirs(d, exist_ok=True)
    planche = Image.new('RGBA', (len(PNJ) * 68, 68), (40, 32, 64, 255))
    for i, (nom, f) in enumerate(PNJ.items()):
        im = Image.fromarray(f().p, 'RGBA'); im.save(f'{d}/{nom}.png')
        planche.alpha_composite(im, (i * 68 + 2, 2))
    if len(sys.argv) > 2: planche.resize((planche.width * 4, planche.height * 4), Image.NEAREST).save(sys.argv[2])
