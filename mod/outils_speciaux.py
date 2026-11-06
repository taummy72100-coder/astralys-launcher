"""Objets speciaux d'Astralys : mages, armes, elixirs, armures, materiaux de boss, coffres, cles, blocs,
couches d'armure (64x32) et cosmetiques (64x32, meme format que les armures).

Usage : python3 outils_speciaux.py <dossier assets/astralyscarte> [apercu.png]
"""
import json, math, os, sys
import numpy as np
from PIL import Image
from outils_objets import T, manche, lame, BOIS, OR, VIOLET, ACIER, LUNE

rng = np.random.default_rng(5)
FEU = [(150, 40, 10), (240, 110, 30), (255, 220, 120)]
GIVRE = [(60, 120, 190), (140, 210, 250), (235, 250, 255)]
VERT = [(30, 110, 60), (80, 200, 110), (190, 255, 200)]
NUIT = [(24, 18, 44), (60, 44, 110), (130, 100, 200)]
ROUGE = [(110, 20, 30), (200, 40, 50), (255, 140, 140)]
ROSE = [(150, 30, 120), (240, 80, 200), (255, 190, 240)]
GRIS = [(80, 80, 90), (150, 150, 160), (220, 220, 228)]
FER = [(90, 96, 110), (160, 166, 178), (226, 230, 238)]
NETH = [(40, 34, 38), (80, 70, 76), (130, 120, 126)]
RARETES = {'commun': GRIS, 'rare': [(30, 80, 190), (70, 140, 255), (180, 215, 255)], 'epique': VIOLET, 'legendaire': OR, 'special': ROSE}


def baton(gemme, tete=OR, bois=BOIS):
    def f():
        t = T(); manche(t, 5, 29, 21, 11, bois, tete)
        t.disque(23.5, 8.5, 5, tete[0]); t.disque(23.5, 8.5, 4, (0, 0, 0, 0))     # anneau
        for a in range(0, 360, 60):
            r = math.radians(a); t.px(23.5 + math.cos(r) * 5, 8.5 + math.sin(r) * 5, tete[2])
        t.disque(23.5, 8.5, 3, gemme[1]); t.disque(22.8, 7.8, 1.5, gemme[2]); t.px(22, 7, (255, 255, 255))
        t.contour()
        for x, y in ((29, 3), (17, 4), (29, 15)): t.etincelle(x, y, (255, 255, 255), gemme[2])
        return t
    return f


def orbe():
    t = T(); t.disque(16, 15, 10, NUIT[0]); t.disque(16, 15, 9, NUIT[1])
    for k in range(40):
        a = k * .5; r = k / 40 * 8
        t.px(16 + math.cos(a) * r, 15 + math.sin(a) * r, (170, 140, 255) if k % 3 else (255, 255, 255))
    t.disque(12.5, 11.5, 2, (220, 210, 255)); t.px(12, 11, (255, 255, 255))
    t.poly([(9, 25), (23, 25), (21, 29), (11, 29)], OR[1]); t.poly([(9, 25), (23, 25), (22, 26), (10, 26)], OR[2])
    t.contour(); t.etincelle(27, 5, (255, 255, 255), VIOLET[2]); t.etincelle(4, 20, (255, 255, 255), VIOLET[2])
    return t


def baguette():
    t = T(); t.ligne(6, 27, 22, 11, (240, 236, 220), 2); t.ligne(5.5, 26.5, 21.5, 10.5, (255, 255, 250))
    for k in (.3, .55, .8): t.px(6 + 16 * k, 27 - 16 * k, OR[1])
    t.ligne(4, 29, 8, 25, OR[0], 2)
    for a in range(0, 360, 45):
        r = math.radians(a); l = 4 if a % 90 == 0 else 2
        t.ligne(24, 9, 24 + math.cos(r) * l, 9 + math.sin(r) * l, VERT[2] if a % 90 else (255, 255, 255))
    t.disque(24, 9, 1.4, VERT[1])
    t.contour(); t.etincelle(28, 18, (255, 255, 255), VERT[2]); t.etincelle(15, 4, (255, 255, 255), VERT[2])
    return t


def tome(couv, motif):
    def f():
        t = T()
        t.poly([(6, 6), (24, 4), (27, 25), (9, 28)], couv[0]); t.poly([(7, 7), (23, 5), (26, 24), (10, 26)], couv[1])
        t.poly([(9, 28), (27, 25), (27, 27), (9, 30)], (236, 226, 200)); t.ligne(9, 29, 27, 26, (180, 170, 150))
        t.poly([(6, 6), (9, 28), (8, 29), (5, 7)], couv[0])
        for x0, y0 in ((7, 7), (23, 5), (26, 24), (10, 26)): t.px(x0, y0, OR[2])
        motif(t)
        t.contour(); t.etincelle(28, 6, (255, 255, 255), couv[2]); t.etincelle(3, 20, (255, 255, 255), couv[2])
        return t
    return f


def eclair(t):
    t.poly([(18, 8), (13, 16), (16, 16), (13, 23), (20, 14), (17, 14), (20, 8)], (255, 240, 120))
    t.ligne(18, 8, 14, 15, (255, 255, 230))


def marteau():
    t = T(); manche(t, 5, 29, 18, 14, BOIS, FER)
    t.poly([(13, 9), (21, 3), (28, 10), (20, 17)], FER[1]); t.poly([(13, 9), (21, 3), (22, 5), (15, 10)], FER[2])
    t.poly([(20, 17), (28, 10), (28, 12), (21, 18)], FER[0])
    t.ligne(17, 6, 24, 13, OR[1]); t.px(20.5, 9.5, ROUGE[1]); t.px(21, 10, ROUGE[2])
    t.contour(); t.etincelle(5, 6, (255, 255, 255), OR[2])
    return t


def epee_runique():
    t = T(); lame(t, 11, 21, 28, 4, GIVRE, 2.6)
    for k in range(4): t.px(14 + k * 3.4, 18 - k * 3.4, (90, 255, 220))
    t.ligne(7, 15, 15, 23, OR[1], 2); t.px(11, 19, (90, 255, 220))
    t.ligne(4, 28, 10, 22, NUIT[1], 2); t.disque(3.5, 28.5, 1.6, OR[1])
    t.contour(); t.etincelle(25, 13, (255, 255, 255), (90, 255, 220)); t.etincelle(18, 4, (255, 255, 255), GIVRE[2])
    return t


def hache():
    t = T(); manche(t, 5, 29, 21, 9, NETH, OR)
    t.poly([(17, 6), (24, 2), (29, 8), (28, 17), (22, 15), (19, 11)], ROUGE[0])
    t.poly([(24, 2), (29, 8), (28, 17), (26, 15), (27, 9), (23, 4)], (200, 200, 210))
    t.poly([(17, 6), (24, 2), (23, 4), (18, 7)], ROUGE[1])
    t.px(21, 8, OR[2]); t.px(22, 9, OR[1])
    t.contour(); t.etincelle(9, 6, (255, 220, 200), ROUGE[2])
    return t


def elixir(liquide, bouchon=OR):
    def f():
        t = T()
        t.poly([(11, 13), (21, 13), (25, 19), (25, 27), (22, 30), (10, 30), (7, 27), (7, 19)], (200, 214, 232))
        t.poly([(12, 15), (20, 15), (23, 20), (23, 26), (21, 28), (11, 28), (9, 26), (9, 20)], liquide[0])
        t.poly([(9, 19), (23, 19), (23, 26), (21, 28), (11, 28), (9, 26)], liquide[1])
        t.disque(13, 23, 2, liquide[2]); t.px(12, 22, (255, 255, 255)); t.px(19, 25, liquide[2])
        t.poly([(13, 7), (19, 7), (19, 13), (13, 13)], (196, 210, 230)); t.ligne(14, 8, 14, 12, (240, 248, 255))
        t.poly([(12, 3), (20, 3), (20, 7), (12, 7)], bouchon[1]); t.ligne(12, 3, 20, 3, bouchon[2]); t.ligne(12, 7, 20, 7, bouchon[0])
        t.ligne(11, 13, 21, 13, OR[1])
        t.contour()
        for x, y in ((26, 9), (5, 14), (27, 25)): t.etincelle(x, y, (255, 255, 255), liquide[2])
        return t
    return f


def armure(piece, pal, gemme):
    def f():
        t = T(); a, b, c = pal
        if piece == 'casque':
            t.poly([(7, 8), (12, 4), (20, 4), (25, 8), (26, 22), (21, 22), (21, 15), (11, 15), (11, 22), (6, 22)], b)
            t.poly([(7, 8), (12, 4), (20, 4), (25, 8), (24, 10), (8, 10)], c)
            t.poly([(6, 18), (11, 18), (11, 22), (6, 22)], a); t.poly([(21, 18), (26, 18), (26, 22), (21, 22)], a)
            t.disque(16, 9, 2, gemme[1]); t.px(15, 8, gemme[2])
            t.ligne(16, 4, 16, 1, gemme[2])
        elif piece == 'plastron':
            t.poly([(4, 6), (11, 4), (21, 4), (28, 6), (28, 13), (24, 14), (24, 28), (8, 28), (8, 14), (4, 13)], b)
            t.poly([(11, 4), (21, 4), (19, 8), (13, 8)], a)
            t.poly([(4, 6), (11, 4), (11, 7), (4, 9)], c); t.poly([(21, 4), (28, 6), (28, 9), (21, 7)], c)
            t.ligne(16, 9, 16, 27, a); t.ligne(8, 21, 24, 21, a)
            t.disque(16, 14, 2.6, gemme[1]); t.px(15, 13, gemme[2])
        elif piece == 'jambieres':
            t.poly([(7, 4), (25, 4), (25, 28), (18, 28), (17, 12), (15, 12), (14, 28), (7, 28)], b)
            t.poly([(7, 4), (25, 4), (25, 8), (7, 8)], a); t.ligne(10, 9, 10, 27, c); t.ligne(22, 9, 22, 27, c)
            t.disque(16, 6, 1.6, gemme[1])
        else:
            t.poly([(5, 10), (12, 10), (12, 22), (15, 26), (15, 28), (4, 28), (4, 22)], b)
            t.poly([(20, 10), (27, 10), (28, 22), (28, 28), (17, 28), (17, 26), (20, 22)], b)
            t.ligne(5, 10, 12, 10, c); t.ligne(20, 10, 27, 10, c)
            t.ligne(4, 27, 15, 27, a); t.ligne(17, 27, 28, 27, a)
            t.px(8, 15, gemme[1]); t.px(24, 15, gemme[1])
        t.contour(); t.etincelle(28, 3, (255, 255, 255), gemme[2])
        return t
    return f


def essence():
    t = T(); t.disque(16, 16, 9, NUIT[0])
    for k in range(60):
        a = rng.random() * 6.28; r = rng.random() * 8
        t.px(16 + math.cos(a) * r, 16 + math.sin(a) * r, NUIT[1] if r > 4 else (150, 90, 230))
    t.disque(16, 16, 2.5, (210, 170, 255)); t.px(15, 15, (255, 255, 255))
    for a in range(0, 360, 72):
        r = math.radians(a); t.ligne(16 + math.cos(r) * 9, 16 + math.sin(r) * 9, 16 + math.cos(r) * 12, 16 + math.sin(r) * 12, NUIT[1])
    t.contour(); return t


def coeur():
    t = T()
    t.disque(11.5, 12, 6, (110, 100, 96)); t.disque(20.5, 12, 6, (110, 100, 96))
    t.poly([(6, 14), (26, 14), (16, 27)], (110, 100, 96))
    t.disque(11.5, 12, 4.5, (150, 140, 134)); t.disque(20.5, 12, 4.5, (150, 140, 134)); t.poly([(8, 14), (24, 14), (16, 24)], (150, 140, 134))
    t.ligne(16, 9, 14, 15, (255, 120, 40)); t.ligne(14, 15, 18, 19, (255, 120, 40)); t.ligne(18, 19, 16, 23, (255, 180, 80))
    t.ligne(10, 11, 12, 15, (255, 120, 40)); t.px(11, 10, (210, 200, 196))
    t.contour(); t.etincelle(27, 5, (255, 220, 180), FEU[1]); return t


def plume():
    t = T(); t.ligne(6, 28, 24, 6, (230, 230, 240))
    for k in range(18):
        x, y = 8 + k, 26 - k; l = min(k, 18 - k) / 2.3
        t.ligne(x, y, x - l, y - l * .2 - 1, GIVRE[1] if k % 3 else GIVRE[2])
        t.ligne(x, y, x + l * .2 + 1, y + l, GIVRE[0] if k % 3 else GIVRE[1])
    t.ligne(5, 29, 8, 26, OR[1])
    t.contour(); t.etincelle(27, 4, (255, 255, 255), GIVRE[2]); t.etincelle(5, 10, (255, 255, 255), OR[2]); return t


def coffre(pal):
    def f():
        t = T(); a, b, c = pal
        bois = [(70, 44, 26), (110, 70, 40), (150, 100, 60)]
        t.poly([(4, 14), (28, 14), (28, 28), (4, 28)], bois[1])               # caisse
        for x in range(5, 28, 4): t.ligne(x, 15, x, 27, bois[0])
        t.poly([(4, 6), (28, 6), (28, 14), (4, 14)], bois[2])                 # couvercle
        t.poly([(5, 4), (27, 4), (28, 6), (4, 6)], bois[2])
        for y0 in (6, 13, 14, 27): t.ligne(4, y0, 28, y0, a)                  # cerclages de la rarete
        for x0 in (4, 28): t.ligne(x0, 6, x0, 28, b)
        t.ligne(9, 4, 9, 28, b); t.ligne(23, 4, 23, 28, b)
        t.ligne(4, 6, 28, 6, c)
        t.poly([(13, 11), (19, 11), (19, 18), (13, 18)], a); t.poly([(14, 12), (18, 12), (18, 17), (14, 17)], b)   # serrure
        t.px(16, 14, (20, 12, 30)); t.px(16, 15, (20, 12, 30))
        t.disque(16, 8.5, 1.5, c)
        t.contour()
        for x, y in ((29, 2), (2, 10), (30, 22)): t.etincelle(x, y, (255, 255, 255), c)
        return t
    return f


def cle(pal):
    def f():
        t = T(); a, b, c = pal
        t.disque(10, 10, 6, b); t.disque(10, 10, 3, (0, 0, 0, 0))
        for k in range(0, 360, 30):
            r = math.radians(k); t.px(10 + math.cos(r) * 5.5, 10 + math.sin(r) * 5.5, c if k % 60 else a)
        t.ligne(14, 14, 27, 27, b, 2); t.ligne(13.5, 13.5, 26.5, 26.5, c)
        t.ligne(21, 21, 18, 24, b, 2); t.ligne(25, 25, 22, 28, b, 2)
        t.disque(10, 10, 2, (255, 255, 255, 255)); t.disque(10, 10, 1.4, c)
        t.contour(); t.etincelle(25, 6, (255, 255, 255), c); return t
    return f


SPRITES = {
    'baton_foudre': baton(LUNE, OR), 'sceptre_givre': baton(GIVRE, FER), 'baton_braise': baton(FEU, OR, NETH),
    'orbe_transfert': orbe, 'baguette_soins': baguette, 'tome_tempete': tome(GIVRE, eclair),
    'marteau_forgeron': marteau, 'epee_runique': epee_runique, 'hache_titan': hache,
    'elixir_force': elixir(ROUGE), 'elixir_titan': elixir([(70, 60, 56), (140, 120, 110), (220, 200, 180)], FER),
    'elixir_ombre': elixir(NUIT, NETH), 'elixir_celerite': elixir(GIVRE), 'elixir_mineur': elixir([(160, 110, 20), (240, 190, 40), (255, 240, 150)], FER),
    'elixir_phenix': elixir(FEU), 'elixir_vie': elixir(ROSE),
    'essence_ombre': essence, 'coeur_titan': coeur, 'plume_celeste': plume,
}
ENSEMBLES = {'ombre': (NUIT, VIOLET), 'titan': ([(70, 60, 56), (130, 116, 108), (190, 176, 168)], FEU), 'celeste': (GIVRE, OR)}
for e, (pal, gem) in ENSEMBLES.items():
    for p in ('casque', 'plastron', 'jambieres', 'bottes'): SPRITES[f'{p}_{e}'] = armure(p, pal, gem)
for r, pal in RARETES.items(): SPRITES[f'coffre_{r}'] = coffre(pal)
for r in ('rare', 'epique', 'legendaire'): SPRITES[f'cle_{r}'] = cle(RARETES[r])


# ------------------------------------------------------------------ blocs 16x16
def bloc(f):
    p = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16): p[y, x] = (*f(x, y), 255)
    return p


def bloc_fantome(x, y):
    joint = y % 8 == 0 or (x + (8 if (y // 8) % 2 else 0)) % 16 == 0
    n = int(rng.integers(-8, 9))
    base = (96, 92, 120) if joint else (150 + n, 146 + n, 176 + n)
    if (x * 3 + y * 5) % 23 == 0: base = (200, 180, 255)
    return base


def cristal(x, y):
    d = abs(x - 7.5) + abs(y - 7.5)
    c = (90, 50, 170) if (x + y) % 5 == 0 else (150, 110, 240) if d < 6 else (110, 70, 200)
    if (x - y) % 7 == 0 and d < 7: c = (230, 210, 255)
    return c


def caisse_cote(x, y):
    if x in (0, 15) or y in (0, 15): return (214, 164, 40) if (x + y) % 2 else (150, 104, 20)
    if x in (1, 14) or y in (1, 14): return (40, 26, 60)
    if 5 <= x <= 10 and 5 <= y <= 10:
        return (240, 80, 200) if (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)) else (110, 60, 190) if (x + y) % 2 else (80, 40, 150)
    return (96, 60, 36) if (x // 2 + y) % 3 else (76, 46, 28)


def caisse_dessus(x, y):
    if x in (0, 15) or y in (0, 15): return (214, 164, 40)
    if abs(x - 7.5) + abs(y - 7.5) < 4: return (255, 200, 255) if (x + y) % 2 else (200, 120, 255)
    return (60, 40, 100) if (x + y) % 2 else (50, 32, 86)


BLOCS = {'bloc_fantome': {'all': bloc_fantome}, 'cristal_astral': {'all': cristal},
         'caisse_astrale': {'side': caisse_cote, 'top': caisse_dessus, 'bottom': lambda x, y: (60, 40, 30)}}


# ------------------------------------------------------------------ couches 64x32 (armures et cosmetiques)
def faces(u, v, w, h, d):
    return {'haut': (u + d, v, w, d), 'avant': (u + d, v + d, w, h), 'droite': (u, v + d, d, h),
            'gauche': (u + d + w, v + d, d, h), 'dos': (u + 2 * d + w, v + d, w, h), 'bas': (u + d + w, v, w, d)}


TETE, CORPS, BRAS, JAMBE = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)


class Couche:
    def __init__(s): s.p = np.zeros((32, 64, 4), np.uint8)

    def peindre(s, b, f, grain=8, lignes=None):
        for nom, (x, y, w, h) in faces(*b).items():
            for j in range(h):
                if lignes is not None and nom not in ('haut', 'bas') and j not in lignes: continue
                for i in range(w):
                    c = f(nom, i, j, w, h)
                    if c is None: continue
                    k = {'haut': 1.08, 'bas': .7, 'droite': .86, 'gauche': .86, 'dos': .8}.get(nom, 1)
                    n = int(rng.integers(-grain, grain + 1))
                    s.p[y + j, x + i] = (*[max(0, min(255, int(ch * k + n))) for ch in c[:3]], 255)


def plaques(pal, gem, bord):
    """Motif d'armure : plaques, rivets, filet de couleur, gemme au centre de la face avant."""
    a, b, c = pal
    def f(n, i, j, w, h):
        if n == 'bas': return a
        if j == 0 or (n != 'haut' and j == h - 1): return bord
        if n == 'avant' and w == 8 and h == 8 and j >= 4 and 2 < i < 5: return None      # fente du casque laissee ouverte
        if n == 'avant' and w == 8 and i in (3, 4) and j in (3, 4) and h == 12: return gem[2] if (i + j) % 2 else gem[1]
        if (i + j * 2) % 7 == 0: return c
        if j % 4 == 3: return a
        return b
    return f


def armure_couche(pal, gem, bord):
    l1, l2 = Couche(), Couche()
    f = plaques(pal, gem, bord)
    l1.peindre(TETE, f); l1.peindre(CORPS, f); l1.peindre(BRAS, f); l1.peindre(JAMBE, f, lignes=range(7, 12))   # bottes
    l2.peindre(CORPS, f, lignes=range(8, 12)); l2.peindre(JAMBE, f, lignes=range(0, 9))                      # jambieres
    return l1, l2


COSMETIQUES = {}


def cosm(nom, partie, f, lignes=None):
    c = Couche()
    for b in partie: c.peindre(b, f, 6, lignes)
    COSMETIQUES[nom] = c


def couronne(n, i, j, w, h):
    if n in ('haut', 'bas'): return None
    if j == 0 and i % 2 == 0: return OR[2]
    if j == 1: return OR[1]
    if j == 2 and n == 'avant' and i in (2, 5): return (90, 220, 255)
    if j == 2 and n == 'avant' and i in (3, 4): return (240, 80, 200)
    if j == 2: return OR[0]
    return None


def chapeau(n, i, j, w, h):
    if n == 'bas': return None
    if n == 'haut': return (220, 230, 255) if (i * 3 + j * 5) % 11 == 0 else NUIT[1]
    if j == 3: return OR[1]
    if j < 3: return (220, 230, 255) if (i * 3 + j * 5) % 7 == 0 else NUIT[1]
    return None


def cornes(n, i, j, w, h):
    if n in ('haut',) and (i in (1, 6)) and j < 3: return (200, 120, 255)
    if n in ('droite', 'gauche') and j < 2 and i < 3: return (170, 90, 240) if j else (240, 200, 255)
    if n == 'avant' and j == 0 and i in (0, 1, 6, 7): return (170, 90, 240)
    return None


def robe(n, i, j, w, h):
    if n in ('haut', 'bas'): return NUIT[1]
    if j == h - 1 or (n == 'avant' and i in (0, w - 1) and w == 8): return OR[1]
    if (i * 3 + j * 7) % 11 == 0: return (220, 230, 255)
    return NUIT[1] if (i + j) % 4 else NUIT[0]


def runique(n, i, j, w, h):
    if n in ('haut', 'bas'): return GIVRE[0]
    if (i + j) % 6 == 0 or (n == 'avant' and w == 8 and i in (3, 4)): return (90, 255, 220)
    if j % 4 == 0: return GIVRE[1]
    return GIVRE[0] if (i + j) % 3 else (40, 90, 150)


def aurore(n, i, j, w, h):
    t = (j / max(1, h - 1))
    c = tuple(int(a * (1 - t) + b * t) for a, b in zip((80, 230, 180), (180, 80, 240)))
    if n in ('haut', 'bas'): return c
    if j == 0: return OR[2]
    return c if (i + j) % 5 else (255, 255, 255)


def nebuleuse(n, i, j, w, h):
    if n in ('haut', 'bas'): return ROSE[0]
    if (i * 7 + j * 3) % 13 == 0: return (255, 240, 255)
    return ROSE[0] if (i + j // 2) % 3 else NUIT[1]


def pans(n, i, j, w, h):
    if n in ('haut', 'bas'): return None
    if j == 8: return OR[1]
    return VIOLET[1] if (i + j) % 4 else VIOLET[0]


def comete(n, i, j, w, h):
    if n == 'bas': return (20, 20, 40)
    if j == 7: return OR[1]
    if n == 'dos' and j >= 8: return (255, 220, 120) if (i + j) % 2 else (255, 140, 60)
    return GIVRE[0] if (i + j) % 3 else GIVRE[1]


def lune(n, i, j, w, h):
    if n == 'bas': return (230, 230, 240)
    if j == 11 or (j == 9 and i % 2 == 0): return (230, 230, 240)
    if n == 'avant' and j == 10 and i in (1, 2): return (180, 220, 255)
    return None


def flamme(n, i, j, w, h):
    if n == 'bas': return NETH[0]
    if j >= 10: return NETH[1]
    if (i + j) % 3 == 0 or j == 7: return FEU[2]
    return FEU[1] if j > 8 else FEU[0]


cosm('couronne_astrale', [TETE], couronne)
cosm('chapeau_archimage', [TETE], chapeau)
cosm('cornes_nebuleuse', [TETE], cornes)
cosm('robe_etoilee', [CORPS, BRAS], robe)
cosm('plastron_runique', [CORPS, BRAS], runique)
cosm('manteau_aurore', [CORPS, BRAS], aurore)
cosm('pantalon_nebuleuse', [JAMBE], nebuleuse, range(0, 9))
cosm('jambieres_runiques', [JAMBE], runique, range(0, 9))
cosm('pans_astraux', [JAMBE], pans, range(0, 9))
cosm('bottes_comete', [JAMBE], comete, range(7, 12))
cosm('sandales_lune', [JAMBE], lune, range(9, 12))
cosm('bottes_flamme', [JAMBE], flamme, range(7, 12))


if __name__ == '__main__':
    d = sys.argv[1]
    for sous in ('textures/item', 'models/item', 'textures/block', 'models/block', 'blockstates', 'textures/models/armor', 'textures/cosmetique'):
        os.makedirs(f'{d}/{sous}', exist_ok=True)
    planche = Image.new('RGBA', (12 * 36, (len(SPRITES) // 12 + 1) * 36), (40, 32, 64, 255))
    for i, (nom, f) in enumerate(SPRITES.items()):
        im = Image.fromarray(f().p, 'RGBA'); im.save(f'{d}/textures/item/{nom}.png')
        parent = 'handheld' if nom.split('_')[0] in ('baton', 'sceptre', 'baguette', 'marteau', 'epee', 'hache') else 'generated'
        json.dump({'parent': f'minecraft:item/{parent}', 'textures': {'layer0': f'astralyscarte:item/{nom}'}}, open(f'{d}/models/item/{nom}.json', 'w'), indent=2)
        planche.alpha_composite(im, ((i % 12) * 36 + 2, (i // 12) * 36 + 2))
    for nom, tex in BLOCS.items():
        for k, f in tex.items(): Image.fromarray(bloc(f), 'RGBA').save(f'{d}/textures/block/{nom}{"" if k == "all" else "_" + k}.png')
        if 'all' in tex: modele = {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'astralyscarte:block/{nom}'}}
        else: modele = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {k: f'astralyscarte:block/{nom}_{k}' for k in tex}}
        json.dump(modele, open(f'{d}/models/block/{nom}.json', 'w'), indent=2)
        json.dump({'variants': {'': {'model': f'astralyscarte:block/{nom}'}}}, open(f'{d}/blockstates/{nom}.json', 'w'), indent=2)
        json.dump({'parent': f'astralyscarte:block/{nom}'}, open(f'{d}/models/item/{nom}.json', 'w'), indent=2)
    for e, (pal, gem) in ENSEMBLES.items():
        l1, l2 = armure_couche(pal, gem, OR[1] if e != 'titan' else (60, 50, 46))
        Image.fromarray(l1.p, 'RGBA').save(f'{d}/textures/models/armor/{e}_layer_1.png')
        Image.fromarray(l2.p, 'RGBA').save(f'{d}/textures/models/armor/{e}_layer_2.png')
    Image.fromarray(np.zeros((32, 64, 4), np.uint8), 'RGBA').save(f'{d}/textures/models/armor/vide.png')
    for nom, c in COSMETIQUES.items(): Image.fromarray(c.p, 'RGBA').save(f'{d}/textures/cosmetique/{nom}.png')
    if len(sys.argv) > 2: planche.resize((planche.width * 3, planche.height * 3), Image.NEAREST).save(sys.argv[2])
