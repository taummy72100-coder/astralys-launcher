"""Skins 64x64 des PNJ d'Astralys : forgerons et mages (format joueur classique, ouvrable dans Blockbench).

Usage : python3 outils_pnj.py <dossier textures/entity/pnj> [apercu.png]
Chaque partie du corps est peinte face par face par une fonction (face, i, j, w, h) -> couleur,
ce qui permet ceintures, bordures, runes et plis de tissu qui font le tour du personnage.
"""
import os, sys
import numpy as np
from PIL import Image

rng = np.random.default_rng(11)


def faces(u, v, w, h, d):
    return {'haut': (u + d, v, w, d), 'bas': (u + d + w, v, w, d), 'droite': (u, v + d, d, h),
            'avant': (u + d, v + d, w, h), 'gauche': (u + d + w, v + d, d, h), 'dos': (u + 2 * d + w, v + d, w, h)}


TETE, CHAPEAU = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
CORPS, VESTE = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
BRAS_D, MANCHE_D = (40, 16, 4, 12, 4), (40, 32, 4, 12, 4)
BRAS_G, MANCHE_G = (32, 48, 4, 12, 4), (48, 48, 4, 12, 4)
JAMBE_D, PANT_D = (0, 16, 4, 12, 4), (0, 32, 4, 12, 4)
JAMBE_G, PANT_G = (16, 48, 4, 12, 4), (0, 48, 4, 12, 4)
OMBRE = {'haut': 1.08, 'bas': .72, 'droite': .86, 'avant': 1, 'gauche': .86, 'dos': .8}
VIDE = None


def mul(c, k): return tuple(int(max(0, min(255, x * k))) for x in c[:3])
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


class Skin:
    def __init__(s): s.p = np.zeros((64, 64, 4), np.uint8)

    def peindre(s, b, f, grain=7, plis=True):
        """f(face, i, j, w, h) -> (r,g,b) | (r,g,b,a) | None (laisse tel quel)."""
        for nom, (x, y, w, h) in faces(*b).items():
            for j in range(h):
                for i in range(w):
                    c = f(nom, i, j, w, h)
                    if c is None: continue
                    a = c[3] if len(c) == 4 else 255
                    if a == 0: s.p[y + j, x + i] = 0; continue
                    k = OMBRE[nom]
                    if plis and nom in ('avant', 'dos', 'droite', 'gauche') and h > 8: k *= (1 - .07 * ((i + j // 5) % 3 == 0))
                    n = rng.integers(-grain, grain + 1)
                    s.p[y + j, x + i] = (*[int(max(0, min(255, ch * k + n))) for ch in c[:3]], a)

    def px(s, x, y, c): s.p[y, x] = (*c[:3], c[3] if len(c) == 4 else 255)

    def avant(s, b): return faces(*b)['avant'][:2]


# ------------------------------------------------------------------ palettes
PEAU, PEAU_M, PEAU_F = (226, 176, 136), (196, 140, 102), (150, 100, 72)
OR, OR_C, OR_F = (214, 164, 40), (252, 220, 110), (150, 104, 20)
CUIR, CUIR_F, CUIR_C = (118, 72, 38), (78, 46, 24), (156, 102, 58)
FER, FER_C, FER_F = (150, 156, 166), (206, 212, 222), (92, 96, 108)
SUIE = (40, 34, 32)
BLANC = (236, 236, 240)


def tete(s, peau=PEAU, yeux=(60, 110, 200), bouche=(156, 92, 76), cils=None):
    def f(n, i, j, w, h):
        if n == 'avant':
            if j == 4 and i in (1, 6): return BLANC
            if j == 4 and i in (2, 5): return yeux
            if j == 3 and i in (1, 2, 5, 6) and cils: return cils
            if j == 5 and i in (3, 4): return mul(peau, .86)
            if j == 6 and i in (3, 4): return bouche
            if j == 7: return mul(peau, .93)
        return peau
    s.peindre(TETE, f, 5, False)


def cheveux(s, c, frange=2, long=0, chauve=False):
    c2 = mul(c, .78)
    def f(n, i, j, w, h):
        if chauve: return mix(PEAU, c, .1) if n == 'haut' else None
        if n == 'haut': return c if (i + j) % 3 else c2
        if n == 'avant': return (c if i % 2 else c2) if j < frange or (j < 4 + long and i in (0, 7)) else None
        if n in ('droite', 'gauche'): return (c if j % 2 else c2) if j < 4 + long and not (n == 'droite' and i == 7 and j > 3) and not (n == 'gauche' and i == 0 and j > 3) else None
        if n == 'dos': return c if (i + j) % 4 else c2
        return None
    s.peindre(TETE, f, 4, False)


def barbe(s, c, longueur=3, moustache=True):
    x, y = s.avant(TETE)
    for i in range(1, 7):
        for j in range(5, 5 + min(3, longueur)):
            if j == 6 and i in (3, 4) and longueur < 3: continue
            s.px(x + i, y + j, c if (i + j) % 2 else mul(c, .82))
    if moustache: [s.px(x + i, y + 5, mul(c, .9)) for i in (2, 3, 4, 5)]
    s.px(x + 3, y + 6, (110, 60, 50)); s.px(x + 4, y + 6, (110, 60, 50))
    for f in ('droite', 'gauche'):
        fx, fy, fw, fh = faces(*TETE)[f]
        for j in range(5, 8): s.px(fx + (fw - 1 if f == 'droite' else 0), fy + j, c)
    if longueur > 3:
        bx, by = s.avant(CORPS)
        for j in range(longueur - 3):
            for i in range(2 + j // 3, 6 - j // 3): s.px(bx + i, by + j, c if (i + j) % 2 else mul(c, .82))


def jambes(s, pantalon, botte=CUIR_F, haut_botte=4, revers=None):
    for b in (JAMBE_D, JAMBE_G):
        def f(n, i, j, w, h):
            if n == 'bas': return mul(botte, .7)
            if n == 'haut': return pantalon
            if j >= 12 - haut_botte:
                if revers and j == 12 - haut_botte: return revers
                if j == 11: return mul(botte, .7)
                return botte if i % 3 else mul(botte, 1.15)
            return pantalon
        s.peindre(b, f)


def bras(s, manche, main=PEAU, manchette=None, retroussee=False, gant=None, brassard=None):
    for b in (BRAS_D, BRAS_G):
        def f(n, i, j, w, h):
            if n == 'bas': return gant or main
            if n == 'haut': return manche
            if j >= 10: return gant or main
            if retroussee and j >= 5:
                if j == 5: return mul(manche, .75)
                return brassard if brassard and j in (7, 8) else mul(main, .96)
            if manchette and j == 9: return manchette
            if brassard and j in (7, 8): return brassard if (i + j) % 3 else mul(brassard, 1.25)
            return manche
        s.peindre(b, f)


def runes(s, b, couleur, face='avant', pas=3, lignes=range(0, 12), alpha=255):
    """Petits signes lumineux brodes (couche exterieure de preference)."""
    x, y, w, h = faces(*b)[face]
    for j in lignes:
        for i in range(w):
            if (i * 7 + j * 5) % (pas * 3) == 0: s.px(x + i, y + j, (*couleur, alpha))


# ------------------------------------------------------------------ forgerons
def tablier(s, cuir, bretelle=CUIR_F, outils=True):
    def f(n, i, j, w, h):
        if n == 'avant':
            if j < 5 and i in (1, 6): return bretelle
            if j >= 4 and 1 <= i <= 6:
                if j == 4: return mul(cuir, .8)
                if i in (1, 6): return mul(cuir, .85)
                if (i + j) % 5 == 0: return SUIE
                return cuir
            return None
        if n == 'dos':
            return bretelle if (j - i) in (0, 1) or (j + i) in (7, 8) else (bretelle if j == 6 else None)
        if n in ('droite', 'gauche'): return bretelle if j == 6 else None
        return None
    s.peindre(VESTE, f, 6, False)
    if outils:
        x, y = s.avant(VESTE)
        s.px(x + 2, y + 8, FER_C); s.px(x + 2, y + 9, FER); s.px(x + 3, y + 8, FER_F)       # marteau dans la poche
        s.px(x + 5, y + 7, OR); s.px(x + 5, y + 8, CUIR_F); s.px(x + 5, y + 9, CUIR_F)      # pince


def ceinture(s, b, y0, c=CUIR_F, boucle=FER_C, bourse=None):
    def f(n, i, j, w, h):
        if j != y0 and j != y0 + 1: return None
        if n == 'avant' and i in (3, 4): return boucle
        if j == y0 + 1: return mul(c, .8)
        return c
    s.peindre(b, f, 3, False)
    if bourse:
        x, y = s.avant(b); s.px(x + 6, y + y0 + 2, bourse); s.px(x + 6, y + y0 + 3, mul(bourse, .8)); s.px(x + 7, y + y0 + 2, mul(bourse, .9))


def marchand():
    """Forgeron-marchand : chemise ecrue, tablier de cuir brule, bandana rouge, bourse d'or."""
    s = Skin(); tete(s, PEAU_M, (70, 50, 30)); cheveux(s, (70, 44, 24), 2)
    barbe(s, (80, 50, 28), 2)
    chemise = (210, 196, 160)
    s.peindre(CORPS, lambda n, i, j, w, h: (mul(chemise, .85) if n == 'avant' and j < 3 and i in (3, 4) else chemise))
    tablier(s, (110, 66, 34))
    ceinture(s, CORPS, 7, bourse=OR)
    bras(s, chemise, PEAU_M, retroussee=True, brassard=CUIR)
    jambes(s, (66, 52, 40), CUIR_F, 4)
    # bandana rouge noue (chapeau)
    def b(n, i, j, w, h):
        if n == 'haut': return (170, 36, 36) if (i + j) % 2 else (140, 26, 26)
        if j < 2: return (170, 36, 36) if (i + j) % 3 else (200, 60, 50)
        if n == 'dos' and j < 4 and i in (3, 4): return (150, 30, 30)
        return (0, 0, 0, 0)
    s.peindre(CHAPEAU, b, 4, False)
    return s


def maitre_metiers():
    """Maitre forgeron : crane rase, grande barbe tressee rousse, tablier lourd, gants de forge, brassards de fer."""
    s = Skin(); tete(s, PEAU_M, (50, 90, 150), cils=(60, 30, 20)); cheveux(s, (150, 70, 30), chauve=True)
    barbe(s, (176, 84, 36), 7)
    s.peindre(CORPS, lambda n, i, j, w, h: (64, 56, 52) if j < 7 else (54, 46, 42))
    barbe(s, (176, 84, 36), 7)       # repeinte par-dessus la chemise
    x, y = s.avant(CORPS); s.px(x + 3, y + 3, OR_C); s.px(x + 4, y + 3, OR_C)  # anneaux de la tresse
    tablier(s, (92, 54, 28), CUIR_F)
    ceinture(s, CORPS, 8, bourse=FER)
    bras(s, (64, 56, 52), PEAU_M, retroussee=True, gant=(70, 44, 26), brassard=FER)
    jambes(s, (52, 44, 40), (44, 36, 32), 5, revers=FER_F)
    # epaulieres de fer (manches)
    for m in (MANCHE_D, MANCHE_G):
        s.peindre(m, lambda n, i, j, w, h: (FER_C if j == 0 else FER if (i + j) % 3 else FER_F) if j < 3 or n == 'haut' else (0, 0, 0, 0), 4, False)
    return s


def maitre_forge():
    """Forgeronne : tresse blonde, bandeau, tablier aux reflets de braise, epauliere doree."""
    s = Skin(); tete(s, PEAU, (40, 140, 90), (180, 90, 90), cils=(40, 30, 20)); cheveux(s, (230, 196, 110), 2, 2)
    x, y, w, h = faces(*TETE)['dos']
    for j in range(8): s.px(x + 3, y + j, (200, 160, 80)); s.px(x + 4, y + j, (240, 210, 130))   # tresse
    bx, by, bw, bh = faces(*CORPS)['dos']
    for j in range(5): s.px(bx + 3 + j % 2, by + j, (230, 196, 110))
    s.peindre(CORPS, lambda n, i, j, w, h: (120, 40, 36) if j < 8 else (90, 30, 28))
    tablier(s, (84, 50, 30), CUIR_F)
    x, y = s.avant(VESTE)
    for j in range(8, 12): s.px(x + 2 + j % 4, y + j, (255, 140, 40))         # braises sur le tablier
    ceinture(s, CORPS, 8, boucle=OR_C)
    bras(s, (120, 40, 36), PEAU, retroussee=True, gant=CUIR_F, brassard=OR)
    jambes(s, (60, 46, 40), CUIR_F, 5, revers=OR)
    s.peindre(MANCHE_D, lambda n, i, j, w, h: (OR_C if j == 0 else OR if (i + j) % 3 else OR_F) if j < 3 or n == 'haut' else (0, 0, 0, 0), 4, False)
    def bandeau(n, i, j, w, h):
        return ((60, 110, 180) if (i + j) % 3 else (90, 140, 210)) if j == 1 and n != 'haut' else (0, 0, 0, 0)
    s.peindre(CHAPEAU, bandeau, 3, False)
    return s


# ------------------------------------------------------------------ mages
def robe(s, c, bord=OR, motif=None, ouverte=None, fente=False):
    """Robe longue : corps + couche veste ; bordure doree, motif brode, pans qui descendent sur les jambes."""
    c2 = mul(c, .82)
    def corps(n, i, j, w, h):
        if n == 'avant' and ouverte and 2 < i < 5: return ouverte
        if n == 'avant' and i in (3, 4) and j in (2, 5): return OR_C
        return c if (i + j) % 4 else c2
    s.peindre(CORPS, corps)
    def veste(n, i, j, w, h):
        if n == 'haut': return (0, 0, 0, 0)
        if n == 'bas': return c2
        if j == 0 or j == h - 1: return bord
        if n == 'avant' and i in (0, w - 1): return bord
        if motif and n in ('avant', 'dos') and motif(i, j): return motif.c
        return (0, 0, 0, 0)
    s.peindre(VESTE, veste, 5, False)
    for p in (PANT_D, PANT_G):   # pans de robe sur les jambes
        def pan(n, i, j, w, h):
            if n in ('haut', 'bas'): return (0, 0, 0, 0)
            if fente and n == 'avant' and ((p is PANT_D and i == w - 1) or (p is PANT_G and i == 0)): return (0, 0, 0, 0)
            if j > 8: return (0, 0, 0, 0)
            if j == 8: return bord
            return c if (i + j) % 4 else c2
        s.peindre(p, pan, 6, False)


class Motif:
    def __init__(s, f, c): s.f, s.c = f, c
    def __call__(s, i, j): return s.f(i, j)


ETOILES = lambda i, j: (i * 3 + j * 7) % 11 == 0


def chapeau_mage(s, c, bord=OR, gemme=(90, 220, 255), plume=None):
    c2 = mul(c, .8)
    def f(n, i, j, w, h):
        if n == 'bas': return (0, 0, 0, 0)
        if n == 'haut': return c if (i + j) % 3 else c2
        if j == 3: return bord
        if j < 3: return c if (i + j) % 3 else c2
        if j == 4 and n == 'avant' and 0 < i < 7 and i % 3 == 0: return mul(c, .6)
        return (0, 0, 0, 0)
    s.peindre(CHAPEAU, f, 5, False)
    x, y = s.avant(CHAPEAU); s.px(x + 3, y + 3, gemme); s.px(x + 4, y + 3, mul(gemme, .8)); s.px(x + 3, y + 2, OR_C); s.px(x + 4, y + 2, OR_C)
    if plume:
        x, y, w, h = faces(*CHAPEAU)['gauche']; [s.px(x + 4 + k, y + 2 - k // 2, plume) for k in range(3)]


def heraut():
    """Mage heraut : robe pourpre brodee d'or, cape, couronne a joyaux."""
    s = Skin(); tete(s, PEAU, (40, 120, 70), cils=(30, 20, 14)); cheveux(s, (44, 28, 18), 1, 1)
    robe(s, (88, 36, 146), OR, Motif(lambda i, j: 2 < j < 10 and (i + j) % 5 == 0, OR_C), ouverte=(236, 226, 206))
    x, y = s.avant(CORPS)
    for j in range(3, 7):
        for i in range(2, 6): s.px(x + i, y + j, (236, 226, 206) if (i in (2, 5) or j in (3, 6)) else (140, 30, 50))   # blason
    s.px(x + 3, y + 4, OR_C); s.px(x + 4, y + 5, OR_C)
    ceinture(s, CORPS, 8, (60, 20, 100), OR_C)
    bras(s, (88, 36, 146), PEAU, manchette=OR)
    jambes(s, (40, 22, 70), (40, 26, 20), 3)
    def couronne(n, i, j, w, h):
        if n in ('haut', 'bas'): return (0, 0, 0, 0)
        if j == 1: return OR
        if j == 0 and i % 2 == 0: return OR_C
        if j == 2 and n == 'avant' and i in (2, 5): return (220, 40, 60)
        if j == 2: return OR_F if i % 2 else (0, 0, 0, 0)
        return (0, 0, 0, 0)
    s.peindre(CHAPEAU, couronne, 3, False)
    x, y = s.avant(CHAPEAU); s.px(x + 3, y + 1, (60, 200, 255)); s.px(x + 4, y + 1, (60, 200, 255))
    return s


def commissaire():
    """Mage marchand : redingote turquoise a revers dores, haut-de-forme, monocle, chaine de montre."""
    s = Skin(); tete(s, PEAU, (50, 50, 50)); cheveux(s, (34, 34, 40), 2)
    x, y = s.avant(TETE)
    s.px(x + 5, y + 4, (190, 230, 250)); s.px(x + 6, y + 3, OR_C); s.px(x + 6, y + 5, OR_C); s.px(x + 4, y + 4, OR)   # monocle
    for i in (1, 2, 5, 6): s.px(x + i, y + 5, (34, 34, 40))                                                     # moustache en croc
    s.px(x + 0, y + 4, (34, 34, 40)); s.px(x + 7, y + 4, (34, 34, 40))
    robe(s, (22, 116, 116), OR, Motif(lambda i, j: False, OR), ouverte=(236, 236, 236))
    x, y = s.avant(CORPS)
    s.px(x + 3, y + 1, (160, 30, 40)); s.px(x + 4, y + 1, (160, 30, 40)); s.px(x + 3, y + 2, (120, 20, 30)); s.px(x + 4, y + 2, (120, 20, 30))  # noeud
    for j in (4, 6, 8): s.px(x + 2, y + j, OR_C)
    for k in range(4): s.px(x + 5 + k % 2, y + 5 + k, OR if k % 2 else OR_C)           # chaine de montre
    bras(s, (22, 116, 116), PEAU, manchette=BLANC)
    jambes(s, (30, 30, 40), (24, 24, 28), 3)
    def hdf(n, i, j, w, h):
        if n == 'bas': return (0, 0, 0, 0)
        if n == 'haut': return (26, 26, 30)
        if j < 2: return (30, 30, 36) if (i + j) % 3 else (20, 20, 24)
        if j == 2: return (22, 116, 116)
        return (0, 0, 0, 0)
    s.peindre(CHAPEAU, hdf, 3, False)
    x, y = s.avant(CHAPEAU); s.px(x + 6, y + 2, OR_C)
    return s


def guide():
    """Mage voyageur : cape bleu nuit semee d'etoiles, capuche, sacoche et carte roulee."""
    s = Skin(); tete(s, (204, 160, 124), (90, 200, 255), cils=(60, 50, 30)); cheveux(s, (200, 180, 120), 2)
    nuit = (34, 52, 120)
    s.peindre(CORPS, lambda n, i, j, w, h: (60, 80, 150) if j < 8 else (44, 60, 120))
    x, y = s.avant(CORPS)
    for j in range(12): s.px(x + 3, y + j, (30, 44, 100)); s.px(x + 4, y + j, (30, 44, 100))
    for j in (2, 4, 6): s.px(x + 3, y + j, OR_C)
    ceinture(s, CORPS, 7, CUIR, OR, bourse=CUIR_C)
    def cape(n, i, j, w, h):
        if n == 'haut': return (0, 0, 0, 0)
        if n == 'avant': return (nuit if (i in (0, 1, 6, 7)) else (0, 0, 0, 0)) if j < 11 else (0, 0, 0, 0)
        if n == 'bas': return nuit
        if ETOILES(i, j): return (220, 230, 255)
        return nuit if (i + j) % 4 else mul(nuit, .8)
    s.peindre(VESTE, cape, 5, False)
    x, y = s.avant(VESTE); s.px(x + 1, y + 1, OR_C); s.px(x + 6, y + 1, OR_C)   # fermoirs
    bras(s, (60, 80, 150), (204, 160, 124), manchette=OR, gant=CUIR)
    jambes(s, (70, 58, 44), (60, 40, 24), 5, revers=CUIR_C)
    def capuche(n, i, j, w, h):
        if n == 'bas': return (0, 0, 0, 0)
        if n == 'avant': return (nuit if (j < 2 or i in (0, 7)) else (0, 0, 0, 0))
        if ETOILES(i, j): return (220, 230, 255)
        return nuit if (i + j) % 3 else mul(nuit, .8)
    s.peindre(CHAPEAU, capuche, 4, False)
    x, y = s.avant(CHAPEAU); s.px(x + 3, y + 0, OR_C); s.px(x + 4, y + 0, OR_C)
    return s


def archimage():
    """Archimage : longue barbe argentee, chapeau etoile, robe nuit brodee d'etoiles et de runes, cristaux a la ceinture."""
    s = Skin(); tete(s, (214, 168, 132), (120, 200, 255), cils=(220, 220, 228)); cheveux(s, (214, 214, 222), 1, 3)
    x, y = s.avant(TETE)
    for i in (1, 2, 5, 6): s.px(x + i, y + 3, (230, 230, 236))     # sourcils broussailleux
    robe(s, (36, 30, 96), OR, Motif(lambda i, j: ETOILES(i, j) or (j in (4, 9) and i % 2 == 0), (200, 220, 255)), fente=True)
    barbe(s, (224, 224, 232), 9)
    ceinture(s, CORPS, 9, (80, 40, 140), OR_C)
    x, y = s.avant(CORPS); s.px(x + 1, y + 10, (90, 230, 255)); s.px(x + 6, y + 10, (240, 120, 255)); s.px(x + 6, y + 11, (200, 80, 240))
    bras(s, (36, 30, 96), (214, 168, 132), manchette=OR)
    for m in (MANCHE_D, MANCHE_G):   # manches evasees
        s.peindre(m, lambda n, i, j, w, h: (OR if j == 10 else (36, 30, 96) if (i + j) % 4 else (28, 24, 76)) if 7 <= j <= 10 and n not in ('haut', 'bas') else (0, 0, 0, 0), 4, False)
    jambes(s, (30, 26, 70), (30, 24, 40), 3)
    chapeau_mage(s, (40, 34, 110), OR, (120, 220, 255))
    runes(s, CHAPEAU, (200, 220, 255), 'dos', 2, range(0, 3))
    return s


def enchanteresse():
    """Enchanteresse : longs cheveux noirs, diademe a emeraude, robe emeraude a runes lumineuses, voile."""
    s = Skin(); tete(s, PEAU, (60, 200, 140), (170, 70, 90), cils=(20, 20, 20)); cheveux(s, (24, 22, 30), 2, 4)
    x, y, w, h = faces(*TETE)['dos']
    robe(s, (20, 110, 80), (220, 230, 240), Motif(lambda i, j: (i * 5 + j * 3) % 7 == 0, (140, 255, 200)), ouverte=(230, 240, 236))
    bx, by, bw, bh = faces(*CORPS)['dos']
    for j in range(7):
        for i in range(1, 7): s.px(bx + i, by + j, (24, 22, 30) if (i + j) % 3 else (40, 36, 48))   # cheveux dans le dos
    ceinture(s, CORPS, 6, (220, 230, 240), (60, 220, 160))
    bras(s, (20, 110, 80), PEAU, manchette=(220, 230, 240))
    jambes(s, (16, 80, 60), (40, 40, 44), 2)
    def diademe(n, i, j, w, h):
        if n in ('haut', 'bas'): return (0, 0, 0, 0)
        if j == 1: return (220, 230, 240) if i % 2 else (190, 200, 214)
        return (0, 0, 0, 0)
    s.peindre(CHAPEAU, diademe, 2, False)
    x, y = s.avant(CHAPEAU); s.px(x + 3, y + 1, (40, 230, 150)); s.px(x + 4, y + 1, (40, 230, 150)); s.px(x + 3, y + 2, (220, 230, 240)); s.px(x + 4, y + 2, (220, 230, 240))
    return s


PNJ = {'marchand': marchand, 'maitre_metiers': maitre_metiers, 'heraut': heraut, 'commissaire': commissaire, 'guide': guide,
       'archimage': archimage, 'maitre_forge': maitre_forge, 'enchanteresse': enchanteresse}

if __name__ == '__main__':
    d = sys.argv[1]; os.makedirs(d, exist_ok=True)
    planche = Image.new('RGBA', (len(PNJ) * 68, 68), (40, 32, 64, 255))
    for i, (nom, f) in enumerate(PNJ.items()):
        im = Image.fromarray(f().p, 'RGBA'); im.save(f'{d}/{nom}.png')
        planche.alpha_composite(im, (i * 68 + 2, 2))
    if len(sys.argv) > 2: planche.resize((planche.width * 4, planche.height * 4), Image.NEAREST).save(sys.argv[2])
