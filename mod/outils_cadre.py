"""Cadre en bois sculpte feerique autour de la carte d'Astralys.

Sorties : cadre.png (le cadre), cadre_lueur.png (lueurs additives qui pulsent en jeu), feuille.png (4 feuilles qui tombent).
Tout est eclaire par une meme lumiere venant d'en haut a gauche (cartes de hauteur -> normales).
"""
import math, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os
from scipy import ndimage
from scipy.interpolate import PchipInterpolator

W, H, M = 1672, 941, 70
TW, TH = W + 2 * M, H + 2 * M
S = 2                                  # sur-echantillonnage
B = 88                                 # largeur du bois (deborde de 18 px sur la carte)
LUM = np.array([-.5, -.65, .75]); LUM /= np.linalg.norm(LUM)
DEMI = LUM + np.array([0, 0, 1.]); DEMI /= np.linalg.norm(DEMI)
rnd = random.Random(11)
rng = np.random.default_rng(11)


def bruit(h, w, cy, cx, seed):
    """Bruit lisse (valeurs 0..1) avec des cellules de cy x cx pixels."""
    g = np.random.default_rng(seed).random((int(h / cy) + 4, int(w / cx) + 4))
    z = ndimage.zoom(g, (cy, cx), order=3, mode='reflect')
    return z[:h, :w]


def eclairer(hauteur, albedo, brillance=.25, durete=30, ambiant=.38):
    gy, gx = np.gradient(hauteur)
    n = np.dstack([-gx, -gy, np.ones_like(hauteur)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    dif = np.clip(n @ LUM, 0, 1)
    spec = np.clip(n @ DEMI, 0, 1) ** durete
    if np.ndim(brillance): brillance = brillance[..., None]
    return np.clip(albedo * (ambiant + .8 * dif[..., None]) + 255 * brillance * spec[..., None], 0, 255)


# ---------------------------------------------------------------- bois du cadre
def veinage(h, w, seed):
    """Bois a fil horizontal : cernes ondules + fibres fines + noeuds. Rend t (0 clair .. 1 sombre)."""
    y = np.arange(h, dtype=np.float32)[:, None]
    deforme = bruit(h, w, 26 * S, 240 * S, seed) * 7 + bruit(h, w, 6 * S, 60 * S, seed + 1) * 1.4
    cernes = .5 + .5 * np.sin((y / S) * .42 + deforme * 2 * math.pi)
    cernes = cernes ** 3
    fibres = bruit(h, w, 1.2 * S, 30 * S, seed + 2)
    tache = bruit(h, w, 40 * S, 160 * S, seed + 3)
    return np.clip(.55 * cernes + .3 * fibres + .35 * (tache - .5), 0, 1)


def bois(pal=((182, 124, 70), (128, 80, 42), (70, 40, 20))):
    h, w = TH * S, TW * S
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32) / S
    dl, dr, dt, db = xx, TW - 1 - xx, yy, TH - 1 - yy
    D = np.stack([dt, db, dl, dr])
    cote = D.argmin(0)                          # 0 haut 1 bas 2 gauche 3 droite
    v = D.min(0)                                # profondeur depuis le bord exterieur
    trie = np.sort(D, axis=0)
    onglet = np.exp(-((trie[1] - trie[0]) / .9) ** 2)   # joint d'onglet dans les coins

    th = veinage(h, w, 5)
    tv = veinage(w, h, 9).T
    t = np.where(cote < 2, th, tv)
    c0, c1, c2 = (np.array(c, np.float32) for c in pal)
    alb = np.where(t[..., None] < .5, c0 + (c1 - c0) * (t[..., None] / .5), c1 + (c2 - c1) * ((t[..., None] - .5) / .5))

    # profil de la moulure (0 = bord exterieur, 1 = carte)
    pts = [(0, 0), (.03, .55), (.09, 1), (.15, .92), (.2, .62), (.32, .40), (.44, .52), (.58, .6), (.66, .56),
           (.70, .46), (.735, .52), (.79, .82), (.845, .52), (.88, .36), (.94, .30), (.97, .18), (1, 0)]
    prof = PchipInterpolator([p for p, _ in pts], [q for _, q in pts])
    p = np.clip(v / B, 0, 1)
    hz = prof(p) * 26 * S
    # perles dorees sur la baguette
    u = np.where(cote < 2, xx, yy)
    perle = (p > .73) & (p < .85)
    hz = hz + perle * (np.cos(u * 2 * math.pi / 9) * .5 + .5) * 3 * S
    # cannelure gravee dans le plat
    hz = hz - np.exp(-((p - .5) / .012) ** 2) * 2.5 * S
    hz += t * .8 * S                           # le veinage accroche un peu la lumiere

    alb = alb * (1 - .45 * onglet[..., None])
    or_ = np.array([236, 190, 96], np.float32) * (.85 + .3 * bruit(h, w, 3 * S, 3 * S, 4))[..., None]
    alb = np.where(perle[..., None], or_, alb)
    filet = (p > .955) & (p < .985)
    alb = np.where(filet[..., None], np.array([214, 168, 80], np.float32), alb)
    brill = np.where(perle | filet, .9, .16 + .14 * (1 - t))
    alb = alb * (1 - .55 * np.exp(-(v / 1.6) ** 2))[..., None]   # arete exterieure sombre
    rgb = eclairer(hz, alb, brill, 26, .30)
    # ombre portee du cadre sur la carte (lumiere en haut a gauche)
    fort = np.choose(cote, [1., .35, 1., .35])
    ombre = np.where(v >= B, .62 * np.exp(-(v - B) / 8) * fort, 0)
    a = np.where(v < B, 1., ombre)
    rgb = np.where((v < B)[..., None], rgb, 0)
    # coins exterieurs arrondis
    coin = np.ones((h, w), np.float32)
    R = 16 * S
    for cy, cx in ((R, R), (R, w - R), (h - R, R), (h - R, w - R)):
        d = np.hypot(yy * S - cy, xx * S - cx)
        zone = ((yy * S < R) if cy == R else (yy * S > h - R)) & ((xx * S < R) if cx == R else (xx * S > w - R))
        coin = np.where(zone, np.clip(R - d, 0, 1), coin)
    return np.dstack([rgb, a * 255 * coin]).astype(np.float32)


# ---------------------------------------------------------------- vegetation (couche haute resolution)
class Couche:
    def __init__(s):
        s.rgba = np.zeros((TH * S, TW * S, 4), np.float32)
        s.lueur = Image.new('RGB', (TW * S, TH * S))
        s.dl = ImageDraw.Draw(s.lueur)

    def poser(s, x0, y0, rgb, a):
        """Colle un patch (rgb, alpha 0..1) en (x0, y0) par-dessus."""
        h, w = a.shape
        X0, Y0 = max(0, x0), max(0, y0)
        X1, Y1 = min(TW * S, x0 + w), min(TH * S, y0 + h)
        if X1 <= X0 or Y1 <= Y0: return
        pa = a[Y0 - y0:Y1 - y0, X0 - x0:X1 - x0, None]
        pr = rgb[Y0 - y0:Y1 - y0, X0 - x0:X1 - x0]
        dst = s.rgba[Y0:Y1, X0:X1]
        dst[..., :3] = pr * pa + dst[..., :3] * (1 - pa)
        dst[..., 3:] = pa * 255 + dst[..., 3:] * (1 - pa)

    def briller(s, x, y, r, col):
        s.dl.ellipse([x - r, y - r, x + r, y + r], fill=col)


def feuille_patch(L, larg, ang, teinte, courbe=0.):
    """Feuille eclairee : forme pointue, nervure centrale, nervures laterales, dome."""
    L, larg = L * S, larg * S
    taille = int(L * 1.15) + 6
    yy, xx = np.mgrid[-taille:taille + 1, -taille:taille + 1].astype(np.float32)
    c, si = math.cos(ang), math.sin(ang)
    sl = (xx * c + yy * si) / L                   # 0 = petiole, 1 = pointe
    tl = -xx * si + yy * c
    tl = tl - courbe * L * (sl ** 2 - sl) * .6    # feuille legerement arquee
    demi = larg * np.clip(np.sin(np.pi * np.clip(sl, 0, 1) ** .75), 0, 1) ** .9 * (1 - .15 * sl)
    rel = np.where(demi > 0, tl / np.maximum(demi, 1e-3), 9)
    dedans = (sl > 0) & (sl < 1) & (np.abs(rel) < 1)
    a = np.clip((demi - np.abs(tl)) / 1.2, 0, 1) * ((sl > 0) & (sl < 1))
    dome = np.sqrt(np.clip(1 - rel ** 2, 0, 1)) * larg * .35
    nerv = -np.exp(-(tl / (.9 * S)) ** 2) * 1.6 * S
    lat = np.sin((sl * L - np.abs(tl) * 1.1) / (2.3 * S)) ; lat = -np.clip(lat, 0, 1) ** 10 * .4 * S
    hz = np.where(dedans, dome + nerv + lat * (np.abs(rel) < .9), 0)
    base = np.array(teinte, np.float32)
    clair = np.minimum(base * 1.35 + 20, 255)
    k = np.clip(.5 + .5 * rel, 0, 1)[..., None] * .5 + np.clip(sl, 0, 1)[..., None] * .35
    alb = base * (1 - k) + clair * k
    alb = alb + (np.exp(-(tl / (.8 * S)) ** 2)[..., None]) * 35       # nervure plus claire
    rgb = eclairer(hz, alb, .35, 28, .42)
    return rgb, a, taille


VERTS = [(46, 112, 52), (62, 132, 58), (84, 150, 62), (38, 96, 58), (104, 162, 70), (54, 124, 84)]
OR_FEUILLE = [(196, 150, 58), (212, 120, 52)]


def feuille(c, x, y, ang, L, verts=VERTS, ombre=True):
    teinte = rnd.choice(verts) if rnd.random() > .07 else rnd.choice(OR_FEUILLE)
    teinte = tuple(min(255, max(0, v + rnd.randint(-10, 10))) for v in teinte)
    rgb, a, t = feuille_patch(L, L * rnd.uniform(.26, .34), ang, teinte, rnd.uniform(-.6, .6))
    X, Y = int(x * S) - t, int(y * S) - t
    if ombre:
        sh = ndimage.gaussian_filter(a, 2.5 * S) * .55
        c.poser(X + 4 * S, Y + 6 * S, np.zeros_like(rgb), sh)
    c.poser(X, Y, rgb, a)


def tige(c, pts, ep, col=(74, 92, 40)):
    """Liane/branche en tube eclaire le long d'une polyligne (coordonnees 1x)."""
    xs = [p[0] * S for p in pts]; ys = [p[1] * S for p in pts]
    x0, y0 = int(min(xs) - ep * S * 2), int(min(ys) - ep * S * 2)
    x1, y1 = int(max(xs) + ep * S * 2), int(max(ys) + ep * S * 2)
    m = Image.new('L', (x1 - x0, y1 - y0)); d = ImageDraw.Draw(m)
    d.line([(px - x0, py - y0) for px, py in zip(xs, ys)], fill=255, width=int(ep * S), joint='curve')
    mk = np.array(m, np.float32) / 255
    dist = ndimage.distance_transform_edt(mk > .5)
    r = ep * S / 2
    hz = np.sqrt(np.clip(1 - (1 - np.clip(dist / r, 0, 1)) ** 2, 0, 1)) * r
    rugo = bruit(*mk.shape, 2 * S, 7 * S, rnd.randint(0, 999)) * 1.2 * S
    alb = np.array(col, np.float32) * (.8 + .4 * bruit(*mk.shape, 5 * S, 5 * S, rnd.randint(0, 999)))[..., None]
    rgb = eclairer(hz + rugo * (mk > .5), alb, .12, 20)
    sh = ndimage.gaussian_filter(mk, 3 * S) * .5
    c.poser(x0 + 5 * S, y0 + 7 * S, np.zeros_like(rgb), sh)
    c.poser(x0, y0, rgb, np.clip(mk, 0, 1))


def sphere(c, x, y, r, col, lueur=None):
    r2 = int(r * S) + 2
    yy, xx = np.mgrid[-r2:r2 + 1, -r2:r2 + 1].astype(np.float32)
    d = np.hypot(xx, yy) / (r * S)
    a = np.clip((1 - d) * r * S, 0, 1)
    hz = np.sqrt(np.clip(1 - d ** 2, 0, 1)) * r * S
    rgb = eclairer(hz, np.broadcast_to(np.array(col, np.float32), (*d.shape, 3)).copy(), .7, 30, .55)
    c.poser(int(x * S) - r2, int(y * S) - r2, rgb, a)
    if lueur: c.briller(x * S, y * S, r * S * 2.2, lueur)


def fleur(c, x, y, r, col):
    """Fleur a 5 petales eclaires, coeur dore lumineux."""
    a0 = rnd.uniform(0, 2 * math.pi)
    for k in range(5):
        a = a0 + k * 2 * math.pi / 5
        rgb, al, t = feuille_patch(r, r * .55, a, col, 0)
        c.poser(int(x * S) - t, int(y * S) - t, rgb, al)
    sphere(c, x, y, r * .3, (255, 226, 130))
    c.briller(x * S, y * S, r * S * 1.6, tuple(int(v * .8) for v in col))


def guirlande(c, pts, densite=1., verts=VERTS, fleurs=True):
    """Liane le long de pts (1x), habillee de feuilles alternees, fleurs et baies lumineuses."""
    tige(c, pts, 4.5)
    long = 0
    for i in range(1, len(pts)):
        (xa, ya), (xb, yb) = pts[i - 1], pts[i]
        seg = math.hypot(xb - xa, yb - ya); long += seg
        direc = math.atan2(yb - ya, xb - xa)
        n = max(1, int(seg / 11 * densite))
        for k in range(n):
            f = (k + rnd.random()) / n
            x, y = xa + (xb - xa) * f, ya + (yb - ya) * f
            cote = 1 if rnd.random() < .5 else -1
            ang = direc + cote * rnd.uniform(.6, 1.5)
            L = rnd.uniform(20, 30)
            feuille(c, x, y, ang, L, verts)
        if fleurs and rnd.random() < .22:
            x, y = (xa + xb) / 2, (ya + yb) / 2
            fleur(c, x + rnd.uniform(-6, 6), y + rnd.uniform(-6, 6), rnd.uniform(9, 12), rnd.choice(FLEURS))
        if fleurs and rnd.random() < .18:
            x, y = xa + rnd.uniform(-8, 8), ya + rnd.uniform(-8, 8)
            col = rnd.choice(BAIES)
            for j in range(3):
                sphere(c, x + rnd.uniform(-5, 5), y + rnd.uniform(-5, 5), rnd.uniform(2.6, 3.6), col, col)


FLEURS = [(250, 160, 205), (196, 160, 255), (150, 214, 255), (255, 214, 150), (255, 250, 245)]
BAIES = [(150, 230, 255), (255, 170, 230), (210, 255, 150)]


def courbe(pts, n=60, ondul=8, freq=3., seed=0):
    """Polyligne lissee passant par pts, avec une ondulation perpendiculaire."""
    xs, ys = zip(*pts)
    t = np.linspace(0, 1, len(pts))
    fx, fy = PchipInterpolator(t, xs), PchipInterpolator(t, ys)
    tt = np.linspace(0, 1, n)
    X, Y = fx(tt), fy(tt)
    dX, dY = np.gradient(X), np.gradient(Y)
    nrm = np.hypot(dX, dY) + 1e-6
    o = np.sin(tt * freq * 2 * math.pi + seed) * ondul * np.sin(np.pi * tt) ** .5
    return list(zip(X - dY / nrm * o, Y + dX / nrm * o))


def medaillon(c, x, y, r, gemme):
    """Medaillon dore sculpte avec une gemme taillee au centre."""
    R = int(r * S) + 3
    yy, xx = np.mgrid[-R:R + 1, -R:R + 1].astype(np.float32)
    d = np.hypot(xx, yy) / (r * S)
    ang = np.arctan2(yy, xx)
    a = np.clip((1 - d) * r * S, 0, 1)
    hz = (np.clip(1 - d, 0, 1) ** .4) * 6 * S + (np.cos(ang * 12) * .5 + .5) * ((d > .62) & (d < .92)) * 2.5 * S
    hz -= np.exp(-((d - .6) / .03) ** 2) * 2 * S
    alb = np.broadcast_to(np.array([226, 178, 86], np.float32), (*d.shape, 3)).copy()
    gem = d < .46
    fac = np.floor((ang + math.pi) / (2 * math.pi) * 8)
    hz = np.where(gem, (1 - d / .46) * 7 * S + (fac % 2) * .8 * S, hz)
    alb = np.where(gem[..., None], np.array(gemme, np.float32) * (.75 + .1 * (fac % 3))[..., None], alb)
    rgb = eclairer(hz, alb, np.where(gem, 1., .75), 22, .45)
    sh = ndimage.gaussian_filter(a, 3 * S) * .6
    c.poser(int(x * S) - R + 4 * S, int(y * S) - R + 6 * S, np.zeros_like(rgb), sh)
    c.poser(int(x * S) - R, int(y * S) - R, rgb, a)
    c.briller(x * S, y * S, r * S * .9, gemme)


POLICE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'fonts', 'cinzel-decorative-latin-900-normal.ttf')


def titre(c, cx, cy, texte, haut_max, larg_max):
    """Logo grave sur le cartouche : lettres dorees en degrade, contour violet sombre, ombre et halo."""
    taille = 200
    while True:
        f = ImageFont.truetype(POLICE, taille * S)
        x0, y0, x1, y1 = f.getbbox(texte)
        if (y1 - y0) <= haut_max * S and (x1 - x0) <= larg_max * S: break
        taille -= 1
    w, h = x1 - x0 + 40 * S, y1 - y0 + 40 * S
    m = Image.new('L', (w, h)); ImageDraw.Draw(m).text((20 * S - x0, 20 * S - y0), texte, font=f, fill=255)
    mk = np.array(m, np.float32) / 255
    contour = np.clip(ndimage.maximum_filter(mk, size=int(3.5 * S)), 0, 1)
    X, Y = int(cx * S - w / 2), int(cy * S - h / 2)
    c.poser(X + 2 * S, Y + 3 * S, np.zeros((h, w, 3), np.float32), ndimage.gaussian_filter(contour, 1.5 * S) * .75)
    c.poser(X, Y, np.broadcast_to(np.array([46, 24, 70], np.float32), (h, w, 3)).copy(), contour)
    t = np.clip((np.arange(h, dtype=np.float32)[:, None] - 20 * S) / (y1 - y0), 0, 1)[..., None]
    alb = np.array([255, 240, 170], np.float32) * (1 - t) + np.array([226, 140, 44], np.float32) * t
    hz = ndimage.gaussian_filter(mk, .9 * S) * 3 * S        # lettres bombees : reflet sur l'arete haute
    rgb = eclairer(hz, np.broadcast_to(alb, (h, w, 3)).copy(), .8, 18, .7)
    c.poser(X, Y, rgb, mk)
    yy, xx = np.nonzero(mk > .5)
    for k in range(0, len(xx), max(1, len(xx) // 40)):
        c.briller(X + xx[k], Y + yy[k], 5 * S, (255, 200, 110))


def plaque(c, cx, cy, lw, lh):
    """Cartouche en bois sculpte (le titre est ecrit en jeu)."""
    w2, h2 = int(lw * S), int(lh * S)
    m = Image.new('L', (w2 + 8, h2 + 8)); d = ImageDraw.Draw(m)
    d.rounded_rectangle([4, 4, w2 + 3, h2 + 3], radius=h2 // 2, fill=255)
    mk = np.array(m, np.float32) / 255
    dist = ndimage.distance_transform_edt(mk > .5) / S
    hz = np.clip(dist / 6, 0, 1) ** .6 * 6 * S - np.exp(-((dist - 9) / 1.2) ** 2) * 1.5 * S
    t = veinage(*mk.shape, 21)
    pal = [np.array(v, np.float32) for v in ((150, 98, 52), (96, 58, 30), (60, 34, 18))]
    alb = np.where(t[..., None] < .5, pal[0] + (pal[1] - pal[0]) * (t[..., None] / .5), pal[1] + (pal[2] - pal[1]) * ((t[..., None] - .5) / .5))
    anneau = (dist > 2) & (dist < 5)
    alb = np.where(anneau[..., None], np.array([232, 186, 92], np.float32), alb)
    hz = hz + anneau * 1.5 * S
    rgb = eclairer(hz, alb * .82, np.where(anneau, .9, .15), 24)
    x0, y0 = int((cx - lw / 2) * S) - 4, int((cy - lh / 2) * S) - 4
    sh = ndimage.gaussian_filter(mk, 4 * S) * .65
    c.poser(x0 + 5 * S, y0 + 8 * S, np.zeros_like(rgb), sh)
    c.poser(x0, y0, rgb, np.clip(mk, 0, 1))


def main(dossier, carte=None):
    print('bois...')
    cadre = bois()
    c = Couche()
    print('vegetation...')
    # guirlandes : depuis chaque coin le long des deux cotes, plus deux autour du cartouche
    e = 34                                   # axe des lianes sur le bois
    coins = [(e, e, 1, 1), (TW - e, e, -1, 1), (e, TH - e, 1, -1), (TW - e, TH - e, -1, -1)]
    for i, (x, y, sx, sy) in enumerate(coins):
        lh_ = rnd.uniform(.30, .38) * TW; lv = rnd.uniform(.38, .48) * TH
        guirlande(c, courbe([(x, y), (x + sx * lh_ * .5, y + sy * 3), (x + sx * lh_, y)], 40, 7, 2.5, i))
        guirlande(c, courbe([(x, y), (x + 3 * sx, y + sy * lv * .5), (x, y + sy * lv)], 34, 7, 2., i + 4))
        # touffe dense dans le coin, un peu sur la carte
        for k in range(34):
            a = rnd.uniform(0, 2 * math.pi)
            rr = rnd.uniform(4, 40)
            px, py = x + sx * 10 + math.cos(a) * rr, y + sy * 10 + math.sin(a) * rr
            feuille(c, px, py, math.atan2(py - y, px - x) + rnd.uniform(-.7, .7), rnd.uniform(24, 36))
        for k in range(3):
            fleur(c, x + sx * rnd.uniform(28, 62), y + sy * rnd.uniform(28, 62), rnd.uniform(11, 14), rnd.choice(FLEURS))
    # lianes qui retombent du haut sur la carte, aux tiers
    for fx in (.34, .64):
        x0 = TW * fx
        pts = courbe([(x0 - 60, e), (x0, e + 52), (x0 + 60, e)], 30, 4, 1.5, fx)
        guirlande(c, pts, 1.2)
        for j, dx in enumerate((-22, 6, 30)):
            longueur = rnd.uniform(40, 80)
            pend = courbe([(x0 + dx, e + 40), (x0 + dx + 4, e + 40 + longueur / 2), (x0 + dx - 3, e + 40 + longueur)], 14, 3, 1, j)
            tige(c, pend, 2.2)
            for k, (px, py) in enumerate(pend[2::3]):
                feuille(c, px, py, math.pi / 2 + (1 if k % 2 else -1) * rnd.uniform(.5, 1.1), rnd.uniform(14, 20))
            px, py = pend[-1]
            sphere(c, px, py + 4, 4.2, (170, 235, 255), (120, 200, 255))
    # cartouche central + feuillage autour
    cx, cy = TW / 2, 40
    for side in (-1, 1):
        guirlande(c, courbe([(cx + side * 200, cy + 2), (cx + side * 300, e - 4), (cx + side * 420, e)], 30, 5, 2, side))
        for k in range(16):
            a = rnd.uniform(-1.3, 1.3) + (0 if side > 0 else math.pi)
            px, py = cx + side * 205 + math.cos(a) * rnd.uniform(0, 26), cy + math.sin(a) * rnd.uniform(0, 26)
            feuille(c, px, py, a, rnd.uniform(24, 34))
    plaque(c, cx, cy, 400, 58)
    titre(c, cx, cy + 1, 'ASTRALYS', 36, 330)
    for side in (-1, 1):
        fleur(c, cx + side * 212, cy, 13, (250, 160, 205))
    # medaillons dores dans les coins
    gemmes = [(150, 235, 140), (255, 140, 80), (140, 210, 255), (200, 160, 255)]
    for (x, y, sx, sy), g in zip(coins, gemmes):
        medaillon(c, x, y, 27, g)
    # medaillons au milieu des cotes
    for (x, y), g in zip(((TW / 2, TH - e), (e, TH / 2), (TW - e, TH / 2)), ((201, 160, 255), (124, 224, 124), (143, 216, 255))):
        for k in range(14):
            a = rnd.uniform(0, 2 * math.pi)
            feuille(c, x + math.cos(a) * 14, y + math.sin(a) * 14, a, rnd.uniform(22, 30))
        medaillon(c, x, y, 20, g)

    print('assemblage...')
    out = cadre.copy()
    va = c.rgba[..., 3:] / 255
    out[..., :3] = c.rgba[..., :3] * va + out[..., :3] * (1 - va)
    out[..., 3:] = c.rgba[..., 3:] + out[..., 3:] * (1 - va)
    # la vegetation ne doit pas faire de l'ombre noire opaque sur la carte : alpha deja gere par les patchs d'ombre
    im = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA').resize((TW, TH), Image.LANCZOS)
    im.save(f'{dossier}/cadre.png', optimize=True)

    lueur = c.lueur.filter(ImageFilter.GaussianBlur(7 * S)).resize((TW, TH), Image.LANCZOS)
    la = np.array(lueur, np.float32)
    al = np.clip(la.max(2) * .85, 0, 255)
    rgb = np.where(al[..., None] > 0, la / np.maximum(la.max(2, keepdims=True), 1) * 255, 0)
    Image.fromarray(np.dstack([rgb, al]).astype(np.uint8), 'RGBA').save(f'{dossier}/cadre_lueur.png', optimize=True)

    # feuilles qui tombent (32 x 32 chacune)
    planche = Image.new('RGBA', (128, 32))
    for i, teinte in enumerate(((84, 150, 62), (62, 132, 58), (196, 150, 58), (104, 162, 70))):
        rgb, a, t = feuille_patch(13, 4.2, -math.pi / 4 + .2 * i, teinte, .4)
        p = Image.fromarray(np.dstack([rgb, a * 255]).astype(np.uint8), 'RGBA').resize((32, 32), Image.LANCZOS)
        planche.paste(p, (i * 32, 0))
    planche.save(f'{dossier}/feuille.png')

    if carte:
        fond = Image.new('RGBA', (TW, TH), (11, 8, 32, 255))
        fond.paste(Image.open(carte).convert('RGBA'), (M, M))
        ap = Image.alpha_composite(fond, im)
        # lueur additive comme en jeu
        a = np.array(ap, np.float32); g = np.array(Image.open(f'{dossier}/cadre_lueur.png'), np.float32)
        a[..., :3] = np.clip(a[..., :3] + g[..., :3] * g[..., 3:] / 255 * .55, 0, 255)
        Image.fromarray(a.astype(np.uint8)).save(f'{dossier}/cadre_apercu.png')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
