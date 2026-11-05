"""Monde en blocs facon Paladium : Pyrelune (fond), Sylvaria (gauche), Azurea (droite), Sunfalls (devant).

Sortie : p_monde.npz (grille 3D d'indices de couleur) + p_palette.json (couleur, emission).
"""
import json, math, random
import cv2
import numpy as np

random.seed(21); np.random.seed(21)
ICI = '/tmp/claude-0/-home-claude/317e8ecb-ac12-5492-80dc-b9e6c187b40c/scratchpad/'
NX = NY = 210
NZ = 96
MER = 6

PAL = []          # (r, g, b, emission)
IDX = {}


def c(nom, rgb, emi=0.0):
    IDX[nom] = len(PAL) + 1
    PAL.append((*rgb, emi))
    return IDX[nom]


for nom, rgb, e in [
    ('herbe', (98, 166, 66), 0), ('herbe2', (82, 150, 58), 0), ('herbe3', (118, 178, 74), 0),
    ('terre', (126, 88, 58), 0), ('terre2', (110, 76, 50), 0),
    ('pierre', (128, 124, 128), 0), ('pierre2', (112, 108, 114), 0), ('pierre3', (144, 138, 140), 0),
    ('sable', (222, 204, 150), 0), ('gravier', (150, 140, 132), 0),
    ('neige', (240, 246, 252), 0), ('neige2', (222, 234, 248), 0), ('glace', (160, 200, 240), 0), ('glacebleue', (120, 170, 236), 0),
    ('roche_glace', (150, 162, 190), 0), ('roche_glace2', (132, 146, 178), 0),
    ('basalte', (70, 62, 70), 0), ('basalte2', (56, 50, 58), 0), ('basalte3', (88, 76, 82), 0), ('cendre', (98, 86, 90), 0),
    ('lave', (255, 120, 24), 6.0), ('lave2', (255, 170, 40), 8.0), ('magma', (190, 60, 20), 1.2),
    ('eau', (62, 128, 186), 0), ('eau_claire', (104, 176, 214), 0), ('eau_cascade', (170, 222, 250), 0.6),
    ('tronc', (96, 68, 44), 0), ('tronc_bouleau', (214, 210, 196), 0), ('tronc_sombre', (64, 46, 32), 0),
    ('feuille', (56, 128, 52), 0), ('feuille2', (70, 146, 60), 0), ('feuille3', (44, 108, 46), 0), ('feuille_bouleau', (112, 168, 70), 0),
    ('sapin', (40, 88, 62), 0), ('champi_pied', (234, 228, 214), 0), ('champi', (164, 86, 220), 0.0), ('champi_point', (240, 220, 255), 1.5),
    ('planche', (176, 132, 82), 0), ('planche2', (150, 108, 64), 0), ('toit_rouge', (176, 62, 46), 0), ('toit_rouge2', (146, 48, 38), 0),
    ('mur_clair', (226, 214, 188), 0), ('mur_brique', (178, 108, 82), 0), ('or', (240, 192, 80), 0.3),
    ('chateau_glace', (226, 236, 248), 0), ('toit_bleu', (66, 108, 210), 0), ('toit_bleu2', (52, 88, 180), 0),
    ('fort_mur', (62, 52, 60), 0), ('fort_toit', (44, 36, 44), 0), ('fenetre_feu', (255, 160, 50), 5.0),
    ('fenetre_glace', (150, 230, 255), 3.0), ('fenetre_or', (255, 214, 120), 3.0),
    ('cristal_bleu', (90, 170, 255), 3.5), ('cristal_violet', (170, 100, 255), 3.5), ('cristal_rouge', (255, 90, 80), 3.5),
    ('portail', (170, 90, 255), 7.0), ('magie', (190, 130, 255), 4.0), ('lanterne', (255, 210, 110), 6.0),
    ('fleur_r', (230, 70, 80), 0), ('fleur_j', (250, 220, 70), 0), ('fleur_v', (190, 110, 230), 0), ('fleur_b', (240, 240, 250), 0),
    ('pave', (168, 162, 154), 0), ('pave2', (150, 146, 140), 0),
]:
    c(nom, rgb, e)

G = np.zeros((NX, NY, NZ), np.uint8)
I = IDX


def noise(s):
    a = cv2.GaussianBlur(np.random.rand(NX, NY).astype(np.float32), (0, 0), s)
    return (a - a.min()) / (a.max() - a.min())


n1, n2, n3 = noise(14), noise(6), noise(2)
xx, yy = np.mgrid[0:NX, 0:NY].astype(float)


def blob(cx, cy, r):
    return 1 - np.hypot(xx - cx, yy - cy) / r + (n1 - .5) * .7 + (n2 - .5) * .22


PYR, SYL, AZU, SUN = (58, 58, 56), (62, 150, 56), (150, 62, 56), (138, 138, 40)
fp, fs, fa, fu = blob(*PYR), blob(*SYL), blob(*AZU), blob(*SUN)
reg = np.argmax(np.stack([fp, fs, fa, fu]), 0)      # 0 pyr, 1 syl, 2 azu, 3 sun
f = np.max(np.stack([fp, fs, fa, fu]), 0)
land = f > 0

# Hauteurs : falaises en terrasses vers la mer
hb = np.zeros((NX, NY))
hb += np.where(reg == 0, 14 + np.clip(f, 0, 1) * 10, 0)
hb += np.where(reg == 1, 11 + np.clip(f, 0, 1) * 14 + (n2 - .5) * 6, 0)
hb += np.where(reg == 2, 12 + np.clip(f, 0, 1) * 12 + (n2 - .5) * 4, 0)
hb += np.where(reg == 3, 10 + np.clip(f, 0, 1) * 3, 0)
# volcan
dv = np.hypot(xx - PYR[0], yy - PYR[1]) / 40
vol = np.clip(1 - dv, 0, 1)
hb += np.where(reg == 0, vol ** 1.3 * 46 + (n3 - .5) * 3 * vol, 0)
# montagnes enneigees
for cx, cy, hh, rr in ((132, 40, 30, 26), (168, 74, 22, 20)):
    dm = np.clip(1 - np.hypot(xx - cx, yy - cy) / rr, 0, 1)
    hb += np.where(reg == 2, dm ** 1.6 * hh, 0)
# colline/falaise de Sylvaria
dh = np.clip(1 - np.hypot(xx - 40, yy - 128) / 30, 0, 1)
hb += np.where(reg == 1, dh ** 1.2 * 14, 0)
# bord de cote : descente progressive
cote = np.clip(f / .035, 0, 1)  # falaises franches
h = np.where(land, MER + 1 + (hb - MER - 1) * (.35 + .65 * cote), 0).astype(int)
plage = land & (f < .02) & (reg == 1) & (n2 > .55)
h[plage] = MER + 1

top = np.zeros((NX, NY), np.uint8)
for x in range(NX):
    for y in range(NY):
        if not land[x, y]:
            continue
        z = h[x, y]; r = reg[x, y]
        # strates de roche
        for k in range(z):
            if r == 0:
                b = I['basalte'] if (k // 3) % 2 else I['basalte2']
            elif r == 2:
                b = I['roche_glace'] if (k // 3) % 2 else I['roche_glace2']
            else:
                b = I['pierre'] if (k // 3) % 2 else (I['pierre2'] if (k // 2) % 3 else I['pierre3'])
            G[x, y, k] = b
        if r in (1, 3):
            for k in range(max(0, z - 3), z):
                G[x, y, k] = I['terre'] if k % 2 else I['terre2']
        if plage[x, y]:
            t = I['sable']
        elif r == 0:
            t = random.choice([I['basalte'], I['basalte3'], I['cendre']])
        elif r == 1:
            t = random.choice([I['herbe'], I['herbe2'], I['herbe3']])
        elif r == 2:
            t = random.choice([I['neige'], I['neige'], I['neige2']])
        else:
            t = random.choice([I['herbe'], I['herbe2'], I['herbe3']])
        G[x, y, z - 1] = t
        top[x, y] = t

# Mer : claire pres des cotes
dist = cv2.distanceTransform((~land).astype(np.uint8), cv2.DIST_L2, 5)
for x in range(NX):
    for y in range(NY):
        if not land[x, y]:
            G[x, y, MER - 1] = I['eau_claire'] if dist[x, y] < 5 + n3[x, y] * 4 else I['eau']


def H(x, y):
    return int(h[x, y])


def S(x, y, z, k):
    if 0 <= x < NX and 0 <= y < NY and 0 <= z < NZ:
        G[x, y, z] = I[k] if isinstance(k, str) else k


def sphere(cx, cy, cz, r, ks, prob=1.0):
    R = int(r) + 1
    for dx in range(-R, R + 1):
        for dy in range(-R, R + 1):
            for dz in range(-R, R + 1):
                if dx * dx + dy * dy + dz * dz <= r * r + random.uniform(-1, .5) and random.random() < prob:
                    S(cx + dx, cy + dy, cz + dz, random.choice(ks))


def disque(cx, cy, z, r, k, creux=False):
    R = int(r) + 1
    for dx in range(-R, R + 1):
        for dy in range(-R, R + 1):
            d = math.hypot(dx, dy)
            if d <= r and (not creux or d > r - 1.2):
                S(cx + dx, cy + dy, z, k)


def arbre(x, y, sorte='chene'):
    z = H(x, y)
    if sorte == 'sapin':
        tz = z + random.randint(8, 12)
        for k in range(z, z + 2):
            S(x, y, k, 'tronc_sombre')
        for i, k in enumerate(range(z + 2, tz)):
            r = (tz - k) * .42 + .5
            disque(x, y, k, r, 'neige' if i % 3 == 2 else 'sapin')
        S(x, y, tz, 'neige')
        return
    tronc = {'chene': 'tronc', 'bouleau': 'tronc_bouleau', 'sombre': 'tronc_sombre'}[sorte]
    hh = random.randint(5, 8)
    for k in range(z, z + hh):
        S(x, y, k, tronc)
    feu = {'chene': ['feuille', 'feuille2'], 'bouleau': ['feuille_bouleau', 'feuille2'], 'sombre': ['feuille3', 'feuille']}[sorte]
    sphere(x, y, z + hh, random.choice([2.6, 3, 3.6]), [I[k] for k in feu], .95)


def arbre_geant(x, y):
    z = H(x, y)
    for k in range(z - 1, z + 16):
        disque(x, y, k, 1.8 if k < z + 3 else 1.3, 'tronc')
    for a in range(0, 360, 72):
        for s in range(2, 6):
            S(int(x + s * math.cos(math.radians(a))), int(y + s * math.sin(math.radians(a))), z, 'tronc')
    for dx, dy, dz, r in ((0, 0, 18, 7), (-5, 3, 15, 5), (4, -4, 16, 5), (3, 5, 14, 4.5), (0, 0, 23, 4.5)):
        sphere(x + dx, y + dy, z + dz, r, [I['feuille'], I['feuille2'], I['feuille3']], .97)
    for _ in range(30):
        vx, vy = x + random.randint(-7, 7), y + random.randint(-7, 7)
        for d in range(random.randint(2, 5)):
            S(vx, vy, z + 12 - d, 'feuille3')


def champignon(x, y, t=1):
    z = H(x, y)
    hh = 3 + t
    for k in range(z, z + hh):
        S(x, y, k, 'champi_pied')
    disque(x, y, z + hh, 1.6 + t * .6, 'champi')
    disque(x, y, z + hh + 1, .9 + t * .5, 'champi')
    for dx, dy in ((1, 1), (-1, 0), (0, -1), (1, -1)):
        S(x + dx, y + dy, z + hh + (1 if abs(dx) + abs(dy) < 2 else 0), 'champi_point')


def tour(cx, cy, z0, r, hh, mur, toit, fen, cone=None, pointe='or'):
    for k in range(z0, z0 + hh):
        disque(cx, cy, k, r, mur, creux=True)
        if (k - z0) % 4 == 2:
            for dx, dy in ((round(r), 0), (0, round(r))):
                S(cx + dx, cy + dy, k, fen)
    disque(cx, cy, z0 + hh, r + .8, mur)
    cone = cone or int(r * 2.4)
    for i in range(cone):
        disque(cx, cy, z0 + hh + 1 + i, max((r + 1) * (1 - i / cone), .4), toit)
    S(cx, cy, z0 + hh + 1 + cone, pointe)


def muraille(x0, y0, x1, y1, z0, hh, mur, ep=1, cren=True):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            e = min(x - x0, x1 - x, y - y0, y1 - y)
            if e < ep:
                for k in range(z0, z0 + hh):
                    S(x, y, k, mur)
                if cren and e == 0 and (x + y) % 2 == 0:
                    S(x, y, z0 + hh, mur)


def bloc(x0, y0, x1, y1, z0, z1, k):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1):
                S(x, y, z, k)


def toit_pyr(x0, y0, x1, y1, z, ks):
    i = 0
    while x0 + i <= x1 - i and y0 + i <= y1 - i:
        for x in range(x0 + i, x1 - i + 1):
            for y in range(y0 + i, y1 - i + 1):
                if x in (x0 + i, x1 - i) or y in (y0 + i, y1 - i):
                    S(x, y, z + i, ks[i % len(ks)])
        i += 1


def maison(x, y, w=5, d=5):
    z = max(H(x + a, y + b) for a in range(w) for b in range(d))
    bloc(x, y, x + w - 1, y + d - 1, z - 2, z, 'pave')
    muraille(x, y, x + w - 1, y + d - 1, z, 4, random.choice(['mur_clair', 'mur_brique', 'planche']), cren=False)
    for (a, b) in ((w // 2, d - 1), (w - 1, d // 2)):
        S(x + a, y + b, z + 2, 'fenetre_or')
    toit_pyr(x - 1, y - 1, x + w, y + d, z + 4, ['toit_rouge', 'toit_rouge2'])


def cristaux(x, y, k, n=4, L=(4, 9)):
    for _ in range(n):
        a, b = x + random.randint(-3, 3), y + random.randint(-3, 3)
        z = H(a, b)
        l = random.randint(*L)
        for i in range(l):
            r = (1 - i / l) * 1.3 + .2
            disque(a, b, z + i, r, k)


def arche(x, y, z, w, hh, pierre, coeur='portail'):
    for t in range(-w, w + 1):
        for k in range(hh):
            bord = abs(t) == w or k == hh - 1
            if bord or k < hh - 1:
                S(x + t, y - t, z + k, pierre if bord else coeur)


def cascade(x, y, z0, z1, k, w=2):
    for z in range(z1, z0):
        for i in range(w):
            S(x + i, y - i, z, k)


def lanterne(x, y):
    z = H(x, y)
    for k in range(z, z + 2):
        S(x, y, k, 'planche2')
    S(x, y, z + 2, 'lanterne')


def chemin(a, b, k='gravier', w=1):
    (x0, y0), (x1, y1) = a, b
    L = int(math.hypot(x1 - x0, y1 - y0))
    for i in range(L + 1):
        t = i / L
        x, y = int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t))
        for dx in range(-w, w + 1):
            for dy in range(-w, w + 1):
                if 0 <= x + dx < NX and 0 <= y + dy < NY and land[x + dx, y + dy]:
                    S(x + dx, y + dy, H(x + dx, y + dy) - 1, k)
                elif 0 <= x + dx < NX and 0 <= y + dy < NY:
                    S(x + dx, y + dy, MER, 'planche')   # pont sur l'eau


occ = np.zeros((NX, NY), bool)   # zones deja construites


def libre(x, y, r=3):
    return 0 <= x - r and x + r < NX and 0 <= y - r and y + r < NY and land[x, y] and not occ[x - r:x + r + 1, y - r:y + r + 1].any()


def reserve(x0, y0, x1, y1):
    occ[max(0, x0):x1 + 1, max(0, y0):y1 + 1] = True


# --- Pyrelune -----------------------------------------------------------------
cr = np.hypot(xx - PYR[0], yy - PYR[1])
for x, y in zip(*np.where((reg == 0) & (cr < 5.5))):
    z = H(x, y)
    for k in range(z - 5, z):
        S(x, y, k, 'basalte2')
    S(x, y, z - 4, random.choice(['lave', 'lave2']))
    h[x, y] = z - 3
ang = np.arctan2(yy - PYR[1], xx - PYR[0])
riv = (reg == 0) & land & (np.abs(np.sin(ang * 3 + n1 * 5)) < .08 + .04 * vol) & (cr < 44)
for x, y in zip(*np.where(riv)):
    S(x, y, H(x, y) - 1, random.choice(['lave', 'lave', 'magma']))
fx, fy = PYR[0] + 27, PYR[1] + 25
zs_ = sorted(H(fx + a, fy + b) for a in range(-8, 9) for b in range(-8, 9))
zf = zs_[len(zs_) // 2]
for a in range(-8, 9):
    for b in range(-8, 9):
        for k in range(MER, NZ):
            if k < zf:
                S(fx + a, fy + b, k, 'basalte2')
            elif G[fx + a, fy + b, k] and k < zf + 30:
                G[fx + a, fy + b, k] = 0
        S(fx + a, fy + b, zf - 1, 'basalte3'); h[fx + a, fy + b] = zf
muraille(fx - 8, fy - 8, fx + 8, fy + 8, zf, 7, 'fort_mur')
for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    tour(fx + sx * 8, fy + sy * 8, zf, 2.2, 11, 'fort_mur', 'fort_toit', 'fenetre_feu', cone=7, pointe='cristal_rouge')
muraille(fx - 4, fy - 4, fx + 4, fy + 4, zf + 7, 7, 'fort_mur')
for k in range(zf + 2, zf + 13, 3):
    for t in range(-3, 4, 2):
        S(fx + 4, fy + t, k, 'fenetre_feu'); S(fx + t, fy + 4, k, 'fenetre_feu')
tour(fx, fy, zf + 14, 2, 6, 'fort_mur', 'fort_toit', 'fenetre_feu', cone=9, pointe='cristal_rouge')
arche(fx + 9, fy + 9, zf, 2, 6, 'fort_mur')
reserve(fx - 10, fy - 10, fx + 10, fy + 10)
for (dx, dy) in ((30, -16), (-20, 30), (34, 20)):
    cristaux(PYR[0] + dx, PYR[1] + dy, 'cristal_rouge', 4)
# coulees de lave vers la mer
for a in (30, 75, 120, 200, 250):
    for s in range(30, 80):
        x, y = int(PYR[0] + s * math.cos(math.radians(a))), int(PYR[1] + s * math.sin(math.radians(a)))
        if not (0 <= x < NX and 0 <= y < NY) or not land[x, y]:
            xi, yi = int(PYR[0] + (s - 1) * math.cos(math.radians(a))), int(PYR[1] + (s - 1) * math.sin(math.radians(a)))
            if a in (30, 75):
                cascade(xi, yi, H(xi, yi), MER, 'lave', 2)
            break
for _ in range(20):
    x, y = random.randint(10, 110), random.randint(10, 110)
    if reg[x, y] == 0 and libre(x, y, 1) and cr[x, y] > 30:
        lanterne(x, y)

# --- Azurea -------------------------------------------------------------------
cx_, cy_ = AZU[0] + 2, AZU[1] + 12
zs_ = sorted(H(cx_ + a, cy_ + b) for a in range(-10, 11) for b in range(-10, 11))
zc = zs_[len(zs_) // 2]
for a in range(-10, 11):
    for b in range(-10, 11):
        for k in range(zc, min(NZ, zc + 30)):
            G[cx_ + a, cy_ + b, k] = 0
bloc(cx_ - 10, cy_ - 10, cx_ + 10, cy_ + 10, MER, zc, 'roche_glace')
for x in range(cx_ - 10, cx_ + 11):
    for y in range(cy_ - 10, cy_ + 11):
        S(x, y, zc - 1, 'neige2'); h[x, y] = zc
muraille(cx_ - 10, cy_ - 10, cx_ + 10, cy_ + 10, zc, 6, 'chateau_glace')
for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    tour(cx_ + sx * 10, cy_ + sy * 10, zc, 2.4, 11, 'chateau_glace', 'toit_bleu', 'fenetre_glace', cone=8, pointe='cristal_bleu')
muraille(cx_ - 5, cy_ - 5, cx_ + 4, cy_ + 4, zc + 6, 12, 'chateau_glace', ep=1, cren=False)
for k in range(zc + 8, zc + 17, 3):
    for t in range(-4, 4, 2):
        S(cx_ + 4, cy_ + t, k, 'fenetre_glace'); S(cx_ + t, cy_ + 4, k, 'fenetre_glace')
toit_pyr(cx_ - 6, cy_ - 6, cx_ + 5, cy_ + 5, zc + 18, ['toit_bleu', 'toit_bleu2'])
tour(cx_ + 4, cy_ + 4, zc + 6, 2.2, 18, 'chateau_glace', 'toit_bleu', 'fenetre_glace', cone=10, pointe='cristal_bleu')
tour(cx_ - 1, cy_ - 1, zc + 22, 1.8, 6, 'chateau_glace', 'toit_bleu', 'fenetre_glace', cone=11, pointe='cristal_bleu')
arche(cx_ + 11, cy_ + 11, zc, 2, 6, 'chateau_glace', 'fenetre_glace')
reserve(cx_ - 13, cy_ - 13, cx_ + 13, cy_ + 13)
for (dx, dy, k) in ((28, -2, 'cristal_bleu'), (-6, 34, 'cristal_violet'), (32, 30, 'cristal_bleu'), (-22, 18, 'cristal_violet'), (10, -30, 'cristal_bleu')):
    if 0 <= AZU[0] + dx < NX and 0 <= AZU[1] + dy < NY and land[AZU[0] + dx, AZU[1] + dy]:
        cristaux(AZU[0] + dx, AZU[1] + dy, k, 5)
# lac gele
lac = (np.hypot(xx - 176, yy - 40) < 9 + (n2 - .5) * 6) & (reg == 2)
for x, y in zip(*np.where(lac)):
    S(x, y, H(x, y) - 1, random.choice(['glace', 'glacebleue']))
for _ in range(90):
    x, y = random.randint(95, 205), random.randint(5, 120)
    if reg[x, y] == 2 and libre(x, y, 1) and f[x, y] > .06 and not lac[x, y]:
        arbre(x, y, 'sapin'); reserve(x - 1, y - 1, x + 1, y + 1)

# --- Sylvaria -----------------------------------------------------------------
gx, gy = SYL[0] - 12, SYL[1] - 4
arbre_geant(gx, gy); reserve(gx - 8, gy - 8, gx + 8, gy + 8)
px, py = SYL[0] + 16, SYL[1] + 14
zp = H(px, py)
bloc(px - 4, py - 4, px + 4, py + 4, zp - 1, zp, 'pave')
arche(px, py, zp, 3, 8, 'pierre3')
reserve(px - 4, py - 4, px + 4, py + 4)
tx, ty = SYL[0] + 20, SYL[1] - 22
tour(tx, ty, H(tx, ty), 2.5, 12, 'pierre3', 'feuille3', 'fenetre_or', cone=6)
reserve(tx - 4, ty - 4, tx + 4, ty + 4)
for _ in range(16):
    x, y = random.randint(15, 110), random.randint(100, 205)
    if reg[x, y] == 1 and libre(x, y, 2) and f[x, y] > .1:
        champignon(x, y, random.choice([1, 1, 2])); reserve(x - 2, y - 2, x + 2, y + 2)
for _ in range(170):
    x, y = random.randint(5, 120), random.randint(95, 205)
    if reg[x, y] == 1 and libre(x, y, 1) and f[x, y] > .05:
        arbre(x, y, random.choice(['chene', 'chene', 'bouleau', 'sombre'])); reserve(x - 2, y - 2, x + 2, y + 2)
for _ in range(500):
    x, y = random.randint(5, 120), random.randint(95, 205)
    if reg[x, y] == 1 and land[x, y] and G[x, y, H(x, y)] == 0:
        S(x, y, H(x, y), random.choice(['fleur_r', 'fleur_j', 'fleur_v', 'fleur_b', 'herbe3', 'herbe3']))
# cascades depuis la colline vers la mer
for a in (100, 150):
    for s in range(10, 90):
        x, y = int(40 + s * math.cos(math.radians(a))), int(128 + s * math.sin(math.radians(a)))
        if not (0 <= x < NX and 0 <= y < NY) or not land[x, y]:
            xi, yi = int(40 + (s - 1) * math.cos(math.radians(a))), int(128 + (s - 1) * math.sin(math.radians(a)))
            cascade(xi, yi, H(xi, yi), MER, 'eau_cascade', 3)
            break
for _ in range(14):
    x, y = random.randint(10, 115), random.randint(100, 200)
    if reg[x, y] == 1 and libre(x, y, 1):
        lanterne(x, y)

# --- Sunfalls (spawn) ----------------------------------------------------------
sx, sy = SUN[0], SUN[1]
zs = MER + 4
for x in range(sx - 24, sx + 25):
    for y in range(sy - 24, sy + 25):
        if land[x, y] and math.hypot(x - sx, y - sy) < 24:
            for k in range(MER, zs):
                S(x, y, k, 'pierre')
            for k in range(zs, NZ):
                G[x, y, k] = 0
            S(x, y, zs - 1, 'pave' if (x + y) % 3 else 'pave2'); h[x, y] = zs
for x in range(sx - 24, sx + 25):
    for y in range(sy - 24, sy + 25):
        d = math.hypot(x - sx, y - sy)
        if 22 <= d < 24 and land[x, y]:
            for k in range(zs, zs + 5):
                S(x, y, k, 'mur_clair')
            if (x + y) % 2 == 0:
                S(x, y, zs + 5, 'mur_clair')
for a in range(0, 360, 45):
    tx, ty = int(sx + 23 * math.cos(math.radians(a))), int(sy + 23 * math.sin(math.radians(a)))
    if land[tx, ty]:
        tour(tx, ty, zs, 2, 9, 'mur_clair', 'toit_rouge', 'fenetre_or', cone=6)
for rr, k in ((9, 'magie'), (6, 'pave2'), (3, 'magie')):
    for a in np.arange(0, 6.3, .02):
        S(int(round(sx + rr * math.cos(a))), int(round(sy + rr * math.sin(a))), zs - 1, k)
for a in range(0, 360, 60):
    for t in range(3, 9):
        S(int(round(sx + t * math.cos(math.radians(a)))), int(round(sy + t * math.sin(math.radians(a)))), zs - 1, 'magie')
for i in range(10):
    disque(sx, sy, zs + i, max(1.6 - i * .14, .3), 'cristal_violet')
for (dx, dy) in ((-14, -6), (-14, 6), (-6, -15), (6, -15), (12, -10), (-10, 12), (13, 4), (4, 13)):
    maison(sx + dx - 2, sy + dy - 2, random.choice([4, 5]), random.choice([4, 5]))
reserve(sx - 26, sy - 26, sx + 26, sy + 26)
for (a, b) in (((sx - 20, sy - 4), (SYL[0] + 18, SYL[1] + 6)), ((sx - 4, sy - 20), (AZU[0] + 10, AZU[1] + 16)), ((sx - 16, sy - 16), (PYR[0] + 26, PYR[1] + 24))):
    chemin(a, b)

# Ilots rocheux dans la mer
for _ in range(40):
    x, y = random.randint(6, NX - 7), random.randint(6, NY - 7)
    if land[max(0, x - 6):x + 7, max(0, y - 6):y + 7].any() or dist[x, y] > 22:
        continue
    r = random.uniform(1.5, 4.5); hh = random.randint(MER + 2, MER + 9)
    for dx in range(-5, 6):
        for dy in range(-5, 6):
            if math.hypot(dx, dy) <= r + random.uniform(-.6, .4):
                for k in range(0, hh - int(math.hypot(dx, dy))):
                    S(x + dx, y + dy, k, random.choice(['pierre', 'pierre2']))
                land[x + dx, y + dy] = True; h[x + dx, y + dy] = hh - int(math.hypot(dx, dy))
                S(x + dx, y + dy, h[x + dx, y + dy] - 1, 'herbe' if random.random() < .6 else 'pierre3')
    if r > 3 and random.random() < .6:
        arbre(x, y, 'chene')

# Petites iles magiques flottantes
for (x, y, z, r) in ((20, 20, 52, 4), (186, 186, 40, 4), (190, 120, 58, 3), (120, 190, 56, 3), (100, 20, 70, 3), (24, 92, 62, 3)):
    for dx in range(-r, r + 1):
        for dy in range(-r, r + 1):
            d = math.hypot(dx, dy)
            if d <= r:
                for k in range(z - int((r - d) * 1.4) - 1, z):
                    S(x + dx, y + dy, k, 'pierre2')
                S(x + dx, y + dy, z, 'herbe')
    S(x, y, z + 1, 'cristal_violet'); S(x, y, z + 2, 'cristal_violet')

np.savez_compressed(ICI + 'p_monde.npz', G=G)
pal = [{'rgb': p[:3], 'emi': p[3]} for p in PAL]
json.dump({'pal': pal, 'idx': IDX, 'mer': MER,
           'centres': {'pyrelune': [PYR[0], PYR[1], int(h[PYR[0], PYR[1]]) + 8], 'sylvaria': [SYL[0], SYL[1], 30],
                       'azurea': [AZU[0], AZU[1], 34], 'sunfalls': [SUN[0], SUN[1], 22]}},
          open(ICI + 'p_palette.json', 'w'))
print('blocs', int((G > 0).sum()))
