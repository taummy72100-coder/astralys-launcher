"""Inventaire creatif facon grimoire : repeint les formes des interfaces 1.20.1 (geometrie.json) au style d'Astralys.

geometrie.json vient du workflow 'Geometrie des interfaces' : une lettre par pixel
('.' vide, k noir, w clair, g ombre, p panneau, d bord de case, s fond de case, o/l autres).
Usage : python3 outils_creatif.py geometrie.json <dossier creative_inventory>
"""
import json, math, sys
import numpy as np
from PIL import Image
from scipy import ndimage

NUIT = np.array([22, 16, 42.]); INDIGO = np.array([38, 26, 70.]); VIOLET = np.array([96, 60, 170.])
OR = np.array([226, 182, 92.]); OR_SOMBRE = np.array([132, 92, 40.]); NOIR = np.array([10, 7, 20.])
PARCHEMIN = np.array([232, 214, 170.])


def peindre(carte, taille, bandeau=None):
    w, h = taille
    c = np.full((h, w), '.', dtype='<U1')
    for y, l in enumerate(carte):
        for x, ch in enumerate(l): c[y, x] = ch
    out = np.zeros((h, w, 4), np.float32)
    yy, xx = np.mgrid[0:h, 0:w]
    # fond de panneau : indigo, nebuleuse, etoiles
    n = np.sin(xx * .09 + yy * .05) * .5 + np.sin(xx * .031 - yy * .07) * .5
    panneau = INDIGO * (1 - (yy / 140.)[..., None] * .35) + VIOLET * .18 * np.clip(n, 0, 1)[..., None]
    etoiles = (np.random.default_rng(3).random((h, w)) > .985)
    panneau[etoiles] = [200, 190, 255]
    # zones de case : composantes de 's' (et 'o', 'l') assez grandes, avec lueur au centre
    fond = np.isin(c, ['s', 'o', 'l'])
    lab, nb = ndimage.label(fond)
    interieur = np.zeros((h, w), bool)
    case = np.zeros((h, w, 3), np.float32)
    for i, sl in enumerate(ndimage.find_objects(lab), 1):
        hh, ww = sl[0].stop - sl[0].start, sl[1].stop - sl[1].start
        if hh < 5 or ww < 5: continue
        m = lab[sl] == i
        cy, cx = (hh - 1) / 2, (ww - 1) / 2
        y, x = np.mgrid[0:hh, 0:ww]
        d = np.hypot((y - cy) / max(1, hh / 2), (x - cx) / max(1, ww / 2))
        g = np.clip(1 - d, 0, 1)[..., None]
        teinte = NUIT * (1 - .5 * g) + VIOLET * .35 * g ** 2
        if (c[sl][m] == 'o').mean() > .5: teinte = teinte * .7 + np.array([90, 20, 30.]) * .3   # case poubelle
        case[sl][m] = teinte[m]
        interieur[sl] |= m
    # grande zone noire (fenetre du joueur) : portail violet
    noir = c == 'k'
    plein = ndimage.binary_erosion(noir, iterations=2)
    lab2, _ = ndimage.label(ndimage.binary_dilation(plein, iterations=2) & noir)
    portail = np.zeros((h, w), bool)
    for i, sl in enumerate(ndimage.find_objects(lab2), 1):
        if sl is None: continue
        hh, ww = sl[0].stop - sl[0].start, sl[1].stop - sl[1].start
        if hh < 20 or ww < 20: continue
        m = lab2[sl] == i
        y, x = np.mgrid[0:hh, 0:ww]
        d = np.hypot((x - ww / 2) / (ww / 2), (y - hh * .62) / (hh / 2))
        t = NOIR * .6 + VIOLET * .55 * np.clip(1 - d, 0, 1)[..., None] ** 1.5
        case[sl][m] = t[m]
        portail[sl] |= m
    # voisinage : un 'w' qui touche une case est son bord bas/droit (sombre), sinon c'est le liseré du cadre (or)
    pres_case = ndimage.binary_dilation(np.isin(c, ['s', 'd', 'o', 'l']), iterations=1)
    for y in range(h):
        for x in range(w):
            ch = c[y, x]
            if ch == '.': continue
            if interieur[y, x] or portail[y, x]: col = case[y, x]
            elif ch == 'k': col = NOIR
            elif ch == 'w': col = OR_SOMBRE if pres_case[y, x] else OR
            elif ch == 'g': col = OR_SOMBRE * .8
            elif ch == 'd': col = NOIR * 1.3
            elif ch in 'sol': col = OR_SOMBRE
            else: col = panneau[y, x]
            out[y, x] = (*col, 255)
    if bandeau:   # parchemin sous le titre que le jeu ecrit en gris fonce
        x0, y0, x1, y1 = bandeau
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c[y, x] != 'p': continue
                bord = min(x - x0, y - y0, x1 - 1 - x, y1 - 1 - y)
                out[y, x] = (*(PARCHEMIN if bord > 0 else OR_SOMBRE), 255)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    g = json.load(open(sys.argv[1]))
    d = sys.argv[2]
    for nom, bandeau in (('tab_items.png', (5, 4, 120, 16)), ('tab_item_search.png', (5, 4, 78, 16)), ('tab_inventory.png', None), ('tabs.png', None)):
        im = peindre(g[nom]['carte'], g[nom]['taille'], bandeau)
        if nom == 'tabs.png':   # curseur de defilement (232,0) actif et (244,0) inactif : gemme doree / grise
            a = np.array(im)
            for x0, base in ((232, OR), (244, np.array([90, 84, 110.]))):
                for y in range(15):
                    for x in range(x0, x0 + 12):
                        bord = min(x - x0, y, x0 + 11 - x, 14 - y)
                        col = NOIR if bord == 0 else base * (1.25 if bord == 1 and (x - x0 < 6) else 1 - y / 40)
                        a[y, x] = (*np.clip(col, 0, 255).astype(int), 255)
            im = Image.fromarray(a, 'RGBA')
        im.save(f'{d}/{nom}')
