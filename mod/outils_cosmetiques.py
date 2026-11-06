"""Cosmetiques 3D d'Astralys (chapeaux, casques, jambieres, chaussures) et leurs variantes, mannequin gris, rubis.

Lancer depuis mod/ :  python3 outils_cosmetiques.py . <dossier_blockbench>
Genere : textures/cosmetique/3d/*.png, CosmetiqueModeles.java (geometrie, client), CatalogueCosm.java (catalogue, commun),
textures/entity/mannequin.png, modeles et textures des rubis, et les .bbmodel pour Blockbench.
"""
import json, math, os, sys
import numpy as np
from PIL import Image
import outils_modeles as M
from outils_modeles import Os, Cube, atlas, java_modele, bbmodel_entite, modele_json, clair, mel, P, uni, tissu, metal, cuir, gemme, verre, fourrure, plumes, _base

R = np.random.default_rng(23)
M.R = R   # les matieres tirent leur bruit ici : sortie stable quoi qu'on ajoute dans outils_modeles


def C(mn, t, m, g=0, **k):
    c = Cube(mn, t, m, **k); c.gonfle = g; return c


def ecailles(c):
    def p(w, h, f):
        a = _base(w, h, c, 5)
        for y in range(h):
            for x in range(w):
                if (x + (y // 2) % 2 * 1) % 2 == 0 and y % 2 == 0: a[y, x, :3] *= .78
                elif (x + y) % 3 == 0: a[y, x, :3] *= 1.12
        return a
    return p


def tartan(c, c2, c3):
    def p(w, h, f):
        a = _base(w, h, c, 3)
        for y in range(h):
            for x in range(w):
                if x % 4 == 1 or y % 4 == 1: a[y, x, :3] = np.array(mel(a[y, x, :3], c2, .6), float)
                if x % 4 == 1 and y % 4 == 1: a[y, x, :3] = np.array(c3, float)
        return a
    return p


def lignes_h(c, ys, x0=0, x1=None):
    def d(a):
        for y in ys:
            if 0 <= y < a.shape[0]: a[y, x0:x1, :3] = c
    return d


def deco(*fs):
    def d(a):
        for f in fs: f(a)
    return d


# ====================================================================== chapeaux (os « head »)
def chapeau_classieux(p):
    h = Os('head', (0, 0, 0))
    h.c((-6, -9.5, -6), (12, 1, 12), tissu(p['f'], plis=False))
    h.cubes.append(C((-4.5, -15.5, -4.5), (9, 6, 9), tissu(p['f'], plis=False)))
    h.cubes.append(C((-4.5, -11, -4.5), (9, 1, 9), uni(p['b']), g=.2))
    h.cubes.append(C((3.9, -11.5, -1.5), (1, 2, 2), gemme(p['g']), lum=True))
    pl = h.os('plume', (4.6, -11, 0), (-.5, 0, .25))
    pl.c((-.5, -6, -.5), (1, 6, 1), plumes(p['p'], barres=False)).c((-.5, -8, 0), (1, 3, 1), plumes(clair(p['p'], .9), barres=False))
    pl.c((-.4, -5, .5), (1, 4, 1), plumes(clair(p['p'], 1.05), barres=False))
    return [h]


def casquette(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -9, -4), (8, 3, 8), tissu(p['f'], plis=False), g=.4))
    h.cubes.append(C((-4, -7.2, -8.6), (8, 1, 4), uni(p['v']), g=.05))
    h.cubes.append(C((-.5, -10, -.5), (1, 1, 1), uni(p['v'])))
    h.cubes.append(C((-1.5, -8.6, -4.55), (3, 2, 1), uni(p['l']), lum=p.get('lum', False)))
    return [h]


def antennes_papillon(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8.5, -1), (8, 1, 2), uni(p['b']), g=.35))
    for s in (-1, 1):
        a = h.os('antenne_' + ('g' if s > 0 else 'd'), (s * 2, -8.8, 0), (-.25, 0, s * .35))
        a.c((-.5, -5, -.5), (1, 5, 1), uni(p['t']))
        a.c((-.5 + s * .5, -6, -.5), (1, 1, 1), uni(p['t']))
        a.cubes.append(C((-1 + s * 1.2, -8, -1), (2, 2, 2), gemme(p['g']), lum=True))
    return [h]


def chapeau_sorcier(p):
    h = Os('head', (0, 0, 0))
    h.c((-6.5, -9.5, -6.5), (13, 1, 13), tissu(p['f'], motif='etoiles', mc=p['e'], dens=.03, plis=False))
    h.c((-4, -12.5, -4), (8, 3, 8), tissu(p['f'], motif='etoiles', mc=p['e'], dens=.04, plis=False))
    h.cubes.append(C((-4, -10.5, -4), (8, 1, 8), uni(p['b']), g=.2))
    h.cubes.append(C((-1, -11, -4.6), (2, 2, 1), gemme(p['e']), lum=True))
    h.c((-3, -15.5, -3), (6, 3, 6), tissu(p['f'], motif='etoiles', mc=p['e'], dens=.05, plis=False))
    b = h.os('pointe', (0, -15.5, 0), (-.45, 0, .1))
    b.c((-2, -3, -2), (4, 3, 4), tissu(p['f'], plis=False))
    b2 = b.os('bout', (0, -3, 0), (-.5, 0, 0))
    b2.c((-1, -2.5, -1), (2, 3, 2), tissu(clair(p['f'], .9), plis=False)).c((-.5, -4, -.5), (1, 2, 1), tissu(clair(p['f'], .8), plis=False))
    b2.cubes.append(C((-.5, -5, -.5), (1, 1, 1), gemme(p['e']), lum=True))
    return [h]


def couronne(p):
    h = Os('head', (0, 0, 0))
    m = metal(p['m'], rivets=True)
    h.cubes.append(C((-4, -10, -4), (8, 2, 8), m, g=.5))
    for i in range(3):
        x = -3.5 + i * 3
        for z, dz in ((-4.6, 0), (3.6, 0)):
            h.c((x, -12.5, z), (1, 2, 1), m)
        h.c((-4.6, -12.5, x), (1, 2, 1), m).c((3.6, -12.5, x), (1, 2, 1), m)
    h.cubes.append(C((-1, -11, -4.9), (2, 2, 1), gemme(p['g']), lum=True))
    h.cubes.append(C((-4.9, -11, -.5), (1, 1, 1), gemme(p['g2']), lum=True))
    h.cubes.append(C((3.9, -11, -.5), (1, 1, 1), gemme(p['g2']), lum=True))
    h.cubes.append(C((-.5, -13.5, -4.6), (1, 1, 1), gemme(p['g']), lum=True))
    return [h]


def oreilles_renard(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8.5, -1.5), (8, 1, 3), uni(p['b']), g=.35))
    for s in (-1, 1):
        o = h.os('oreille_' + ('g' if s > 0 else 'd'), (s * 2.6, -8.8, 0), (0, 0, s * .2))
        o.c((-1.5, -3, -.5), (3, 3, 1), fourrure(p['f'])).c((-1, -4.5, -.5), (2, 2, 1), fourrure(p['f'])).c((-.5, -5.5, -.5), (1, 1, 1), fourrure(p['bout']))
        o.c((-1, -2.6, -.75), (2, 2, 1), uni(p['i']))
    return [h]


def aureole(p):
    h = Os('head', (0, 0, 0))
    a = h.os('anneau', (0, -12.5, 0), (.12, 0, 0))
    g = gemme(p['c'])
    a.cubes += [C((-3, 0, -4), (6, 1, 1), g, lum=True), C((-3, 0, 3), (6, 1, 1), g, lum=True), C((-4, 0, -3), (1, 1, 6), g, lum=True), C((3, 0, -3), (1, 1, 6), g, lum=True),
                C((-3.6, 0, -3.6), (1, 1, 1), g, lum=True), C((2.6, 0, -3.6), (1, 1, 1), g, lum=True), C((-3.6, 0, 2.6), (1, 1, 1), g, lum=True), C((2.6, 0, 2.6), (1, 1, 1), g, lum=True)]
    return [h]


# ====================================================================== casques (os « head »)
def fentes(c):
    def d(a):
        for y in (3, 4):
            if y < a.shape[0]: a[y, 1:a.shape[1] - 1, :3] = c
        for x in range(1, a.shape[1] - 1, 2):
            if a.shape[0] > 6: a[6, x, :3] = c
    return d


def casque_chevalier(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8, -4), (8, 8, 8), metal(p['m'], rivets=True), g=1.2, deco={'avant': fentes((20, 18, 24))}))
    h.cubes.append(C((-.5, -10.2, -5.2), (1, 1, 10), metal(clair(p['m'], 1.15))))
    h.cubes.append(C((-1.5, -9, -5.6), (3, 1, 1), metal(p['a'])))
    pl = h.os('plumet', (0, -10, 1), (-.35, 0, 0))
    pl.c((-.5, -5, -1), (1, 5, 3), plumes(p['p'], barres=False)).c((-.5, -4, 2), (1, 4, 3), plumes(clair(p['p'], .85), barres=False))
    pl.c((-.5, -2, 5), (1, 3, 2), plumes(clair(p['p'], .7), barres=False))
    return [h]


def casque_viking(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8, -4), (8, 5, 8), metal(p['m'], rivets=True), g=1.2))
    h.cubes.append(C((-4, -4, -4), (8, 1, 8), cuir(p['c']), g=1.25))
    h.cubes.append(C((-.5, -4.5, -5.5), (1, 3, 1), metal(clair(p['m'], 1.1))))
    for s in (-1, 1):
        c1 = h.os('corne_' + ('g' if s > 0 else 'd'), (s * 5, -6.5, 0), (0, 0, -s * .5))
        c1.c((-1 if s < 0 else 0, -1, -1), (2, 2, 2), uni(p['o']))
        c2 = c1.os('pointe', (s * 1.6, -.6, 0), (0, 0, -s * .7))
        c2.c((-1 if s < 0 else 0, -2.5, -.75), (1, 3, 1), uni(clair(p['o'], 1.05))).c((-1 if s < 0 else 0, -4, -.5), (1, 2, 1), uni(clair(p['o'], 1.15)))
    return [h]


def ouverture_t(c):
    def d(a):
        a[2:4, 1:a.shape[1] - 1, :3] = c
        a[4:, 3:5, :3] = c
    return d


def casque_spartiate(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8, -4), (8, 8, 8), metal(p['m'], rivets=False), g=1.2, deco={'avant': ouverture_t((22, 16, 12))}))
    h.cubes.append(C((-.5, -10.5, -5), (1, 1, 10), metal(clair(p['m'], .85))))
    h.c((-.5, -14, -6), (1, 4, 12), fourrure(p['c'])).c((-.5, -12.5, 6), (1, 4, 2), fourrure(clair(p['c'], .85)))
    return [h]


def casque_astronaute(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8, -4), (8, 8, 8), uni(p['m'], 3), g=1.4))
    h.cubes.append(C((-3.5, -6.5, -5.8), (7, 5, 1), verre(p['v']), lum=True))
    h.cubes.append(C((-4, -1.2, -4), (8, 1, 8), metal(p['a']), g=1.6))
    h.c((3.6, -12.5, 1), (1, 3, 1), metal(p['a']))
    h.cubes.append(C((3.6, -13.5, 1), (1, 1, 1), gemme(p['l']), lum=True))
    h.cubes.append(C((-5.9, -5, -1.5), (1, 3, 3), metal(p['a'])))
    h.cubes.append(C((4.9, -5, -1.5), (1, 3, 3), metal(p['a'])))
    return [h]


def masque_dragon(p):
    h = Os('head', (0, 0, 0))
    h.cubes.append(C((-4, -8, -4), (8, 8, 8), ecailles(p['e']), g=1.2))
    h.cubes.append(C((-3, -4.5, -8.6), (6, 3, 4), ecailles(clair(p['e'], 1.1))))
    h.cubes.append(C((-3, -2.2, -8.4), (6, 1, 4), uni(p['m'])))
    h.cubes.append(C((-2.5, -3.6, -8.8), (1, 1, 1), uni((240, 240, 230))))
    h.cubes.append(C((1.5, -3.6, -8.8), (1, 1, 1), uni((240, 240, 230))))
    h.cubes.append(C((-4.4, -7, -5.4), (2, 1, 1), gemme(p['y']), lum=True))
    h.cubes.append(C((2.4, -7, -5.4), (2, 1, 1), gemme(p['y']), lum=True))
    for s in (-1, 1):
        c = h.os('corne_' + ('g' if s > 0 else 'd'), (s * 3, -9, 2), (.6, 0, s * .15))
        c.c((-.5, -3, -.5), (1, 3, 1), uni(p['c'])).c((-.5, -5, .2), (1, 2, 1), uni(clair(p['c'], 1.2)))
    h.c((-.5, -10.8, -3), (1, 2, 1), uni(p['c'])).c((-.5, -10.4, 0), (1, 2, 1), uni(p['c'])).c((-.5, -9.6, 3), (1, 2, 1), uni(p['c']))
    return [h]


# ====================================================================== jambieres (os « body », « right_leg », « left_leg »)
def ceinture(p, b):
    b.cubes.append(C((-4, 10, -2), (8, 2, 4), cuir(p['c']), g=1.12))
    b.cubes.append(C((-1, 10, -3.4), (2, 2, 1), metal(p.get('boucle', (230, 200, 90)), rivets=False)))


def jambes(fabrique):
    l = []
    for nom, piv, s in (('right_leg', (-1.9, 12, 0), -1), ('left_leg', (1.9, 12, 0), 1)):
        o = Os(nom, piv); fabrique(o, s); l.append(o)
    return l


def jambieres_chevalier(p):
    b = Os('body', (0, 0, 0)); ceinture(p, b)
    def f(o, s):
        o.cubes.append(C((-2, 0, -2), (4, 12, 4), metal(p['m'], rivets=True), g=.62))
        o.cubes.append(C((-1.5, 4, -3.1), (3, 2, 1), metal(clair(p['m'], 1.2))))
        o.cubes.append(C((-2, 1.5, -2), (4, 1, 4), metal(p['a']), g=.72))
    return [b] + jambes(f)


def pantalon_pirate(p):
    b = Os('body', (0, 0, 0))
    b.cubes.append(C((-4, 9.5, -2), (8, 2, 4), tissu(p['s'], plis=False), g=1.15))
    b.cubes.append(C((2, 11, -3.5), (2, 4, 1), tissu(p['s'], plis=False)))
    def f(o, s):
        o.cubes.append(C((-2, 0, -2), (4, 10, 4), tissu(p['f'], motif='rayures', mc=p['r']), g=.6))
        o.cubes.append(C((-2, 9, -2), (4, 1, 4), uni(clair(p['f'], .7)), g=.75))
    return [b] + jambes(f)


def runes(c):
    def d(a):
        for y in range(1, a.shape[0] - 1, 3):
            P(a, a.shape[1] // 2, y, c); P(a, a.shape[1] // 2 - 1, y + 1, clair(c, .8))
    return d


def jambieres_runiques(p):
    b = Os('body', (0, 0, 0)); ceinture(p, b)
    def f(o, s):
        o.cubes.append(C((-2, 0, -2), (4, 12, 4), tissu(p['f'], motif='losanges', mc=clair(p['f'], 1.4), plis=False), g=.6))
        o.cubes.append(C((-.5, 1, -3.0), (1, 8, 1), gemme(p['r']), lum=True))
        o.cubes.append(C((s * 2.1 - .5, 2, -.5), (1, 6, 1), gemme(p['r']), lum=True))
    return [b] + jambes(f)


def kilt(p):
    b = Os('body', (0, 0, 0))
    b.cubes.append(C((-4, 10, -2), (8, 7, 4), tartan(p['f'], p['l'], p['x']), g=1.25))
    b.cubes.append(C((-4, 10, -2), (8, 1, 4), cuir((70, 45, 28)), g=1.35))
    b.cubes.append(C((-1.5, 11.5, -4), (3, 3, 1), fourrure((235, 230, 220))))
    def f(o, s):
        o.cubes.append(C((-2, 7, -2), (4, 4, 4), tissu(p['c'], plis=False), g=.6))
        o.cubes.append(C((-2, 7, -2), (4, 1, 4), uni(p['l']), g=.7))
    return [b] + jambes(f)


# ====================================================================== chaussures (os « right_leg », « left_leg », y de 0 a 12)
def bottes_cowboy(p):
    def f(o, s):
        o.cubes.append(C((-2, 6, -2), (4, 6, 4), cuir(p['c']), g=.9, deco={'avant': lignes_h(clair(p['c'], 1.4), (1,))}))
        o.cubes.append(C((-1.5, 10.2, -4.2), (3, 2, 2), cuir(clair(p['c'], .9), couture=False)))
        o.cubes.append(C((-1.5, 12.2, 1.5), (3, 1, 2), uni(clair(p['c'], .5))))
        o.cubes.append(C((-.5, 10.5, 3.3), (1, 1, 1), metal(p['e'])))
        o.cubes.append(C((-1, 10.5, 4.1), (2, 1, 1), metal(p['e'])))
    return jambes(f)


def baskets(p):
    def f(o, s):
        lac = lambda a: [P(a, x, 0 if a.shape[0] < 3 else 1, (250, 250, 250)) for x in range(1, a.shape[1] - 1, 2)]
        o.cubes.append(C((-2, 9, -2), (4, 3, 4), uni(p['f'], 3), g=.9, deco={'droite': lignes_h(p['a'], (1,)), 'gauche': lignes_h(p['a'], (1,))}))
        o.cubes.append(C((-2, 11.6, -2.6), (4, 1, 5), uni(p['s'], 2), g=.75))
        o.cubes.append(C((-1.5, 9.6, -3.9), (3, 2, 1), uni(p['f'], 3), deco={'haut': lac, 'avant': lac}))
        o.cubes.append(C((-1, 8.2, -3.1), (2, 1, 1), uni(p['a'])))
    return jambes(f)


def bottes_ailees(p):
    def f(o, s):
        o.cubes.append(C((-2, 7, -2), (4, 5, 4), cuir(p['c']), g=.9))
        o.cubes.append(C((-2, 7, -2), (4, 1, 4), metal(p['m']), g=1.0))
        a = o.os('aile', (s * 3.1, 8.5, .5), (0, -s * .45, -s * .35))
        a.c((-.5, -3, 0), (1, 3, 3), plumes(p['p'], barres=False)).c((-.5, -5, 1.5), (1, 3, 3), plumes(clair(p['p'], 1.05), barres=False))
        a.c((-.5, -6.5, 3), (1, 2, 2), plumes(clair(p['p'], 1.12), barres=False))
    return jambes(f)


def fissures(c):
    def d(a):
        for x in range(0, a.shape[1], 2):
            y = int(R.integers(0, a.shape[0])); P(a, x, y, c)
            if y + 1 < a.shape[0]: P(a, x + 1, y + 1, clair(c, .85))
    return d


def bottes_lave(p):
    def f(o, s):
        o.cubes.append(C((-2, 6, -2), (4, 6, 4), uni(p['f'], 6), g=.9, deco={k: fissures(p['l']) for k in ('avant', 'dos', 'droite', 'gauche')}))
        o.cubes.append(C((-2, 11.8, -2.4), (4, 1, 5), gemme(p['l']), g=.6, lum=True))
        o.cubes.append(C((-1, 9, -3.1), (2, 1, 1), gemme(p['l']), lum=True))
    return jambes(f)


# ====================================================================== catalogue : base -> (nom, categorie, partie, fabrique, variantes)
CATEGORIES = ['Chapeaux', 'Casques', 'Jambières', 'Chaussures']
PARTIE = [0, 0, 2, 3]
PRIX = [300, 600, 1200, 2000]   # en rubis bleus, par rarete (commun, rare, epique, legendaire)
N, B, Bl, O, A, Ro, V, Vi = (32, 28, 60), (40, 70, 170), (235, 235, 240), (226, 176, 52), (190, 196, 210), (200, 40, 40), (50, 150, 70), (120, 60, 190)

BASES = {
    'chapeau_classieux': ('Chapeau classieux', 0, chapeau_classieux, [
        ('nuit', 'Nuit', {'f': (34, 40, 96), 'b': (200, 170, 80), 'g': (120, 200, 255), 'p': Bl}, 1),
        ('bordeaux', 'Bordeaux', {'f': (110, 24, 40), 'b': (30, 20, 20), 'g': (255, 120, 120), 'p': (240, 220, 200)}, 1),
        ('noir', 'Ébène', {'f': (30, 28, 34), 'b': (170, 30, 40), 'g': (255, 80, 80), 'p': (40, 40, 44)}, 0),
        ('ivoire', 'Ivoire', {'f': (230, 222, 205), 'b': (40, 40, 50), 'g': (90, 200, 255), 'p': (90, 150, 230)}, 2)]),
    'casquette': ('Casquette de golfeur', 0, casquette, [
        ('blanc', 'Blanche', {'f': Bl, 'v': (200, 205, 215), 'l': (60, 170, 90)}, 0),
        ('rouge', 'Rouge', {'f': (196, 40, 40), 'v': (240, 240, 240), 'l': (250, 250, 250)}, 0),
        ('vert', 'Verte', {'f': (40, 120, 70), 'v': (30, 70, 40), 'l': (240, 210, 90)}, 0),
        ('neon', 'Néon', {'f': (30, 26, 40), 'v': (255, 60, 200), 'l': (80, 255, 220), 'lum': True}, 2)]),
    'antennes_papillon': ('Antennes de papillon', 0, antennes_papillon, [
        ('azur', 'Azur', {'b': (40, 50, 120), 't': (30, 30, 60), 'g': (80, 170, 255)}, 0),
        ('rose', 'Rose', {'b': (230, 120, 180), 't': (120, 40, 90), 'g': (255, 140, 220)}, 0),
        ('emeraude', 'Émeraude', {'b': (40, 110, 60), 't': (20, 50, 30), 'g': (90, 255, 140)}, 1),
        ('or', 'Or', {'b': O, 't': (120, 80, 20), 'g': (255, 230, 120)}, 2)]),
    'chapeau_sorcier': ('Chapeau de sorcier', 0, chapeau_sorcier, [
        ('violet', 'Améthyste', {'f': (70, 34, 120), 'b': (200, 160, 70), 'e': (255, 230, 140)}, 1),
        ('bleu', 'Saphir', {'f': (30, 50, 130), 'b': (200, 200, 220), 'e': (170, 230, 255)}, 1),
        ('noir', 'Nuit sans lune', {'f': (26, 22, 34), 'b': (120, 40, 160), 'e': (200, 120, 255)}, 2),
        ('foret', 'Forêt', {'f': (34, 80, 46), 'b': (140, 100, 50), 'e': (200, 255, 140)}, 1)]),
    'couronne': ('Couronne royale', 0, couronne, [
        ('or', 'Or', {'m': O, 'g': (220, 30, 60), 'g2': (60, 120, 255)}, 2),
        ('argent', 'Argent', {'m': (200, 205, 215), 'g': (60, 140, 255), 'g2': (160, 230, 255)}, 2),
        ('obsidienne', 'Obsidienne', {'m': (40, 30, 60), 'g': (190, 80, 255), 'g2': (255, 80, 200)}, 3),
        ('rose', 'Quartz rose', {'m': (230, 160, 190), 'g': (255, 250, 255), 'g2': (255, 120, 200)}, 3)]),
    'oreilles_renard': ('Oreilles de renard', 0, oreilles_renard, [
        ('roux', 'Roux', {'f': (210, 110, 40), 'bout': (40, 30, 26), 'i': (250, 220, 200), 'b': (60, 40, 30)}, 0),
        ('blanc', 'Arctique', {'f': (240, 240, 245), 'bout': (170, 190, 210), 'i': (250, 210, 220), 'b': (140, 160, 190)}, 1),
        ('noir', 'Ombre', {'f': (40, 36, 46), 'bout': (200, 60, 255), 'i': (120, 60, 160), 'b': (30, 26, 36)}, 1),
        ('gris', 'Cendre', {'f': (140, 140, 150), 'bout': (240, 240, 245), 'i': (230, 200, 200), 'b': (80, 80, 90)}, 0)]),
    'aureole': ('Auréole', 0, aureole, [
        ('doree', 'Dorée', {'c': (255, 220, 110)}, 2), ('celeste', 'Céleste', {'c': (140, 220, 255)}, 2),
        ('infernale', 'Infernale', {'c': (255, 80, 40)}, 3), ('spectrale', 'Spectrale', {'c': (190, 120, 255)}, 3)]),
    'casque_chevalier': ('Casque de chevalier', 1, casque_chevalier, [
        ('acier', 'Acier', {'m': (170, 176, 188), 'a': (110, 116, 128), 'p': (200, 30, 40)}, 1),
        ('royal', 'Royal', {'m': O, 'a': (230, 220, 200), 'p': (40, 80, 200)}, 2),
        ('noir', 'Chevalier noir', {'m': (50, 48, 58), 'a': (120, 40, 160), 'p': (150, 50, 220)}, 2),
        ('elfique', 'Elfique', {'m': (190, 210, 200), 'a': (70, 140, 90), 'p': (90, 220, 120)}, 3)]),
    'casque_viking': ('Casque viking', 1, casque_viking, [
        ('fer', 'Fer', {'m': (150, 150, 158), 'c': (90, 60, 36), 'o': (236, 226, 200)}, 0),
        ('jarl', 'Jarl', {'m': O, 'c': (110, 30, 30), 'o': (250, 245, 230)}, 2),
        ('givre', 'Givre', {'m': (170, 210, 240), 'c': (40, 60, 110), 'o': (220, 245, 255)}, 1)]),
    'casque_spartiate': ('Casque spartiate', 1, casque_spartiate, [
        ('bronze', 'Bronze', {'m': (176, 120, 60), 'c': (200, 30, 30)}, 1),
        ('argent', 'Argent', {'m': (200, 205, 215), 'c': (40, 80, 200)}, 1),
        ('nuit', 'Nuit', {'m': (44, 40, 54), 'c': O}, 2)]),
    'casque_astronaute': ('Casque d\'astronaute', 1, casque_astronaute, [
        ('blanc', 'Station', {'m': (236, 236, 240), 'v': (230, 170, 60), 'a': (150, 155, 170), 'l': (255, 60, 60)}, 1),
        ('orange', 'Mars', {'m': (230, 120, 40), 'v': (90, 180, 255), 'a': (90, 90, 100), 'l': (90, 255, 140)}, 2),
        ('nebuleuse', 'Nébuleuse', {'m': (40, 30, 70), 'v': (230, 90, 255), 'a': (200, 170, 80), 'l': (255, 230, 120)}, 3)]),
    'masque_dragon': ('Masque de dragon', 1, masque_dragon, [
        ('rouge', 'Braise', {'e': (170, 30, 30), 'm': (90, 16, 16), 'y': (255, 200, 40), 'c': (40, 30, 30)}, 2),
        ('emeraude', 'Émeraude', {'e': (40, 130, 70), 'm': (20, 60, 30), 'y': (255, 240, 90), 'c': (230, 220, 190)}, 2),
        ('abysse', 'Abysse', {'e': (40, 30, 70), 'm': (20, 14, 40), 'y': (170, 90, 255), 'c': (200, 190, 220)}, 3),
        ('dore', 'Doré', {'e': O, 'm': (140, 90, 20), 'y': (255, 60, 60), 'c': (250, 245, 230)}, 3)]),
    'jambieres_chevalier': ('Jambières de chevalier', 2, jambieres_chevalier, [
        ('acier', 'Acier', {'m': (170, 176, 188), 'a': (110, 116, 128), 'c': (90, 60, 36)}, 1),
        ('royal', 'Royal', {'m': O, 'a': (230, 220, 200), 'c': (110, 30, 30)}, 2),
        ('noir', 'Chevalier noir', {'m': (50, 48, 58), 'a': (120, 40, 160), 'c': (30, 26, 36), 'boucle': (180, 90, 255)}, 2)]),
    'pantalon_pirate': ('Pantalon de pirate', 2, pantalon_pirate, [
        ('rouge', 'Corsaire', {'f': (230, 225, 215), 'r': (190, 40, 40), 's': (180, 30, 30)}, 0),
        ('bleu', 'Marin', {'f': (230, 225, 215), 'r': (40, 70, 170), 's': (30, 50, 120)}, 0),
        ('noir', 'Capitaine', {'f': (40, 36, 44), 'r': (200, 170, 80), 's': (120, 30, 40)}, 1)]),
    'jambieres_runiques': ('Jambières runiques', 2, jambieres_runiques, [
        ('bleu', 'Arcane', {'f': (30, 30, 60), 'r': (90, 190, 255), 'c': (40, 30, 26)}, 1),
        ('violet', 'Néant', {'f': (34, 22, 50), 'r': (200, 110, 255), 'c': (30, 20, 40)}, 2),
        ('vert', 'Sylve', {'f': (24, 46, 30), 'r': (100, 255, 150), 'c': (60, 40, 24)}, 1),
        ('feu', 'Brasier', {'f': (50, 22, 18), 'r': (255, 150, 40), 'c': (30, 16, 12)}, 3)]),
    'kilt': ('Kilt des Highlands', 2, kilt, [
        ('rouge', 'Clan rouge', {'f': (170, 30, 36), 'l': (30, 40, 90), 'x': (240, 220, 100), 'c': (230, 225, 210)}, 0),
        ('vert', 'Clan vert', {'f': (30, 90, 50), 'l': (30, 30, 60), 'x': (230, 60, 60), 'c': (60, 90, 60)}, 0),
        ('bleu', 'Clan bleu', {'f': (40, 60, 140), 'l': (20, 20, 40), 'x': (250, 250, 250), 'c': (40, 40, 60)}, 1)]),
    'bottes_cowboy': ('Bottes de cow-boy', 3, bottes_cowboy, [
        ('marron', 'Ranch', {'c': (120, 72, 38), 'e': (200, 200, 210)}, 0),
        ('noir', 'Hors-la-loi', {'c': (40, 34, 34), 'e': O}, 1),
        ('blanc', 'Shérif', {'c': (230, 220, 200), 'e': O}, 1)]),
    'baskets': ('Baskets', 3, baskets, [
        ('rouge', 'Rouge et blanc', {'f': Bl, 's': (240, 240, 240), 'a': (210, 40, 40)}, 0),
        ('noir', 'Néon', {'f': (36, 34, 40), 's': (250, 250, 250), 'a': (80, 255, 140)}, 1),
        ('bleu', 'Soleil', {'f': (40, 80, 190), 's': (250, 250, 250), 'a': (255, 210, 60)}, 0),
        ('rose', 'Bonbon', {'f': (250, 170, 210), 's': (255, 255, 255), 'a': (140, 220, 255)}, 1)]),
    'bottes_ailees': ('Bottes ailées', 3, bottes_ailees, [
        ('or', 'Hermès', {'c': (150, 100, 50), 'm': O, 'p': (250, 250, 250)}, 2),
        ('argent', 'Zéphyr', {'c': (60, 70, 100), 'm': (200, 205, 215), 'p': (190, 225, 255)}, 2),
        ('ombre', 'Corbeau', {'c': (36, 32, 40), 'm': (130, 60, 180), 'p': (50, 46, 58)}, 3)]),
    'bottes_lave': ('Bottes de magma', 3, bottes_lave, [
        ('lave', 'Lave', {'f': (36, 26, 26), 'l': (255, 130, 30)}, 2),
        ('givre', 'Givre', {'f': (40, 60, 100), 'l': (150, 230, 255)}, 2),
        ('poison', 'Poison', {'f': (40, 26, 50), 'l': (120, 255, 80)}, 3)]),
}


def hexa(c): return '0x%02X%02X%02X' % c[:3]


# ====================================================================== rubis (argent du jeu / monnaie payante) et mannequin
GEM = {'gui': {'rotation': [25, 35, 0], 'translation': [0, 0, 0], 'scale': [.95, .95, .95]},
       'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
       'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [.9, .9, .9]},
       'thirdperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 1], 'scale': [.45, .45, .45]},
       'thirdperson_lefthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 1], 'scale': [.45, .45, .45]},
       'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
       'firstperson_lefthand': {'rotation': [0, 45, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]}}


def facettes(c):
    """Pierre taillee : facettes en losanges claires / sombres, eclats blancs, coeur lumineux."""
    def p(w, h, f):
        a = _base(w, h, c, 3)
        for y in range(h):
            for x in range(w):
                k = ((x + y) % 4 < 2) ^ ((x - y) % 4 < 2)
                a[y, x, :3] *= 1.18 if k else .86
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.hypot((xx - (w - 1) / 2) / max(w / 2, 1), (yy - (h - 1) / 2) / max(h / 2, 1))
        a[..., :3] *= (1.35 - .5 * d)[..., None]
        if f == 'haut' and w > 3: a[1, 1, :3] = (255, 255, 255); a[1, 2, :3] = mel(c, (255, 255, 255), .7); a[2, 1, :3] = mel(c, (255, 255, 255), .7)
        return a
    return p


def rubis(c):
    def f():
        fo, cl = clair(c, .7), clair(c, 1.25)
        return [C((7, 1, 7), (2, 1, 2), facettes(fo)), C((6, 2, 6), (4, 1, 4), facettes(fo)), C((5, 3, 5), (6, 1, 6), facettes(clair(c, .82))),
                C((4, 4, 4), (8, 2, 8), facettes(c)), C((3, 6, 3), (10, 1, 10), facettes(cl)), C((4, 7, 4), (8, 1, 8), facettes(c)),
                C((5, 8, 5), (6, 1, 6), facettes(cl), lum=True), C((6, 6.5, 6), (4, 2, 4), gemme((255, 255, 255)), lum=True),
                C((12.5, 9.5, 3), (1, 1, 1), gemme((255, 255, 255)), lum=True), C((2.5, 3, 12), (1, 1, 1), gemme((255, 255, 255)), lum=True)], GEM
    return f


def mannequin(img):
    """Skin 64x64 gris facon mannequin de vitrine : volumes biseautes, articulations marquees, pas de visage."""
    gris = (196, 198, 204)
    parts = [((0, 0), (8, 8, 8)), ((16, 16), (8, 12, 4)), ((40, 16), (4, 12, 4)), ((32, 48), (4, 12, 4)), ((0, 16), (4, 12, 4)), ((16, 48), (4, 12, 4))]
    for (u, v), t in parts:
        cu = Cube((0, 0, 0), t, uni(gris, 2), deco={'cotes': lambda a: lignes_h(clair(gris, .86), (a.shape[0] // 2,))(a) if a.shape[0] >= 12 else None})
        cu.peindre(img, u, v)


if __name__ == '__main__':
    mod, bb = sys.argv[1], sys.argv[2]
    A = f'{mod}/src/main/resources/assets/astralyscarte'
    os.makedirs(f'{A}/textures/cosmetique/3d', exist_ok=True); os.makedirs(f'{bb}/cosmetiques', exist_ok=True)
    java, cat = [], []
    for base, (nom, categorie, fab, variantes) in BASES.items():
        taille = None
        for k, (suf, vnom, pal, rar) in enumerate(variantes):
            racine = fab(pal)
            cubes = [c for o in racine for c in o.tous()]
            pos, img = atlas(cubes, 64)
            if taille is None:
                taille = img.shape
                pos_de = {id(c): pos[i] for i, c in enumerate(cubes)}
                java.append(java_modele(base, racine, pos_de, img.shape[1], img.shape[0]))
                json.dump(bbmodel_entite(base, racine, pos_de, img), open(f'{bb}/cosmetiques/{base}.bbmodel', 'w'))
            assert img.shape == taille, (base, suf)
            cid = f'{base}_{suf}'
            Image.fromarray(img, 'RGBA').save(f'{A}/textures/cosmetique/3d/{cid}.png')
            Image.fromarray(img, 'RGBA').save(f'{bb}/cosmetiques/{cid}.png')
            coul = next(iter(pal.values()))
            cat.append(f'            new Cosmetiques.Cosm("{cid}", {PARTIE[categorie]}, "{nom} — {vnom}", {PRIX[rar]}, {hexa(coul)}, "{base}", {categorie}, {rar})')
    entete = ('package fr.astralys.carte;\n\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.CubeDeformation;\n'
              'import net.minecraft.client.model.geom.builders.CubeListBuilder;\nimport net.minecraft.client.model.geom.builders.LayerDefinition;\n'
              'import net.minecraft.client.model.geom.builders.MeshDefinition;\nimport net.minecraft.client.model.geom.builders.PartDefinition;\n\n')
    open(f'{mod}/src/main/java/fr/astralys/carte/CosmetiqueModeles.java', 'w').write(
        entete + '/** Geometrie des cosmetiques 3D (cote joueur). FICHIER GENERE par outils_cosmetiques.py : ne pas modifier a la main. */\n'
        'final class CosmetiqueModeles {\n    static final String[] BASES = {' + ', '.join(f'"{b}"' for b in BASES) + '};\n\n'
        '    static LayerDefinition couche(String base) {\n        return switch (base) {\n'
        + ''.join(f'            case "{b}" -> {b}();\n' for b in BASES)
        + '            default -> throw new IllegalArgumentException(base);\n        };\n    }\n\n' + '\n'.join(java) + '\n    private CosmetiqueModeles() {}\n}\n')
    open(f'{mod}/src/main/java/fr/astralys/carte/CatalogueCosm.java', 'w').write(
        'package fr.astralys.carte;\n\nimport java.util.List;\n\n'
        '/** Catalogue des cosmetiques 3D et de leurs variantes. FICHIER GENERE par outils_cosmetiques.py : ne pas modifier a la main. */\n'
        'final class CatalogueCosm {\n    static final String[] CATEGORIES = {' + ', '.join(f'"{c}"' for c in CATEGORIES) + '};\n'
        '    static final List<Cosmetiques.Cosm> LISTE = List.of(\n' + ',\n'.join(cat) + ');\n\n    private CatalogueCosm() {}\n}\n')
    # rubis
    for nom, c in (('rubis_jaune', (255, 196, 30)), ('rubis_bleu', (50, 130, 255))):
        cubes, disp = rubis(c)()
        m, img = modele_json(cubes, f'astralyscarte:item/3d/{nom}', 64, display=disp)
        Image.fromarray(img, 'RGBA').save(f'{A}/textures/item/3d/{nom}.png')
        json.dump(m, open(f'{A}/models/item/{nom}.json', 'w'), indent=1)
        os.makedirs(f'{bb}/objets', exist_ok=True)
        Image.fromarray(img, 'RGBA').save(f'{bb}/objets/{nom}.png'); json.dump(m, open(f'{bb}/objets/{nom}.json', 'w'), indent=1)
    # mannequin
    im = np.zeros((64, 64, 4)); mannequin(im)
    os.makedirs(f'{A}/textures/entity', exist_ok=True)
    Image.fromarray(np.clip(im, 0, 255).astype(np.uint8), 'RGBA').save(f'{A}/textures/entity/mannequin.png')
    print('ok', len(BASES), 'modeles,', len(cat), 'cosmetiques')
