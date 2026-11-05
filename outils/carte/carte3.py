"""Carte 3D (voxels isometriques) des trois mondes d'Astralys, avec cadre et etiquettes."""
import json, math, random, sys
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

random.seed(7); np.random.seed(7)
ICI = '/tmp/claude-0/-home-claude/317e8ecb-ac12-5492-80dc-b9e6c187b40c/scratchpad/'
N = 540
TW, TH, ZH = 7.0, 3.5, 3.5


def P(x, y, z):
    return ((x - y) * TW / 2, (x + y) * TH / 2 - z * ZH)


def noise(s, n=N):
    a = cv2.GaussianBlur(np.random.rand(n, n).astype(np.float32), (0, 0), s)
    return (a - a.min()) / (a.max() - a.min())


n1, n2, n3 = noise(12), noise(4), noise(1.2)
xx, yy = np.mgrid[0:N, 0:N].astype(float)

hm = np.full((N, N), -999, int)   # sommet de la colonne
bot = np.zeros((N, N), int)       # bas de la colonne (ile flottante)
top = np.zeros((N, N, 3), np.uint8)
sid = np.zeros((N, N, 3), np.uint8)   # couleur de la roche
dirt = np.zeros((N, N, 3), np.uint8)  # bande de terre sous le dessus
land = np.zeros((N, N), bool)
vox = {}


def j(c, a=9):
    e = random.randint(-a, a)
    return tuple(max(0, min(255, v + e)) for v in c)


def S(x, y, z, c):
    if 0 <= x < N and 0 <= y < N:
        vox[(x, y, z)] = c


def disk(cx, cy, z, r, c, hollow=False, a=8):
    R = int(r + 1)
    for dx in range(-R, R + 1):
        for dy in range(-R, R + 1):
            d = math.hypot(dx, dy)
            if d <= r and (not hollow or d > r - 1.6):
                S(cx + dx, cy + dy, z, j(c, a))


def ile(cx, cy, r, base, prof, pal, roche, terre, bosse=12, rugueux=0.55):
    d = np.hypot(xx - cx, yy - cy) / r
    f = 1 - d + (n1 - .5) * rugueux
    m = f > 0
    h = (base + f * bosse + (n1 - .5) * 3 + (n2 - .5) * 1.2).astype(int)
    fc = np.clip(f, 0, 1)
    b = (base - 2 - np.power(fc, .9) * prof * 1.5 - n3 * 10 * np.power(fc, .3) - (n2 - .5) * 6 * fc).astype(int)
    sel = m & (h > hm)
    hm[sel] = h[sel]; bot[sel] = b[sel]; land[sel] = True
    idx = np.clip((n3 * len(pal)).astype(int), 0, len(pal) - 1)
    t = np.array(pal, np.uint8)[idx]
    top[sel] = t[sel]; sid[sel] = roche; dirt[sel] = terre
    return f, sel


# --- Iles principales -----------------------------------------------------------
VOL = (165, 160, 118)
FOR = (195, 400, 120)
GLA = (405, 190, 118)
CEN = (345, 352, 52)

GRASS = [(92, 158, 60), (78, 142, 54), (108, 172, 66), (86, 150, 58)]
SNOW = [(240, 246, 252), (226, 236, 250), (214, 228, 248)]
ASH = [(78, 66, 74), (64, 54, 62), (92, 76, 80)]
PAVE = [(176, 170, 160), (160, 154, 146), (190, 184, 172)]

ff, fsel = ile(*FOR, 46, 95, GRASS, (112, 106, 112), (122, 88, 58))
gf, gsel = ile(*GLA, 48, 95, SNOW, (150, 160, 188), (214, 226, 242), bosse=6)
vf, vsel = ile(*VOL, 52, 105, ASH, (72, 60, 66), (66, 54, 60), bosse=6)
cf, csel = ile(*CEN, 36, 70, PAVE, (110, 104, 112), (140, 134, 128), bosse=2, rugueux=.25)

# Volcan : cone, cratere de lave, coulees
dc = np.hypot(xx - (VOL[0] - 8), yy - (VOL[1] - 8)) / 70
cone = np.clip(1 - dc, 0, 1) ** 1.15 * 120
m = vsel & (dc < 1)
hm[m] = np.maximum(hm[m], (52 + cone + (n2 - .5) * 10)[m].astype(int))
crat = vsel & (dc < .14)
hm[crat] = (52 + 120 * .86 ** 1.15 - 7 + n2 * 3)[crat].astype(int)
top[crat] = (255, 150, 40)
ang = np.arctan2(yy - VOL[1], xx - VOL[0])
riv = vsel & (np.abs(np.sin(ang * 4 + n1 * 6)) < .07 + .05 * (1 - np.clip(dc, 0, 1))) & (dc < 1.3)
top[riv] = (255, 118, 30)
glow = np.zeros((N, N), bool); glow |= crat | riv

# Plateau central : cercle magique
dcen = np.hypot(xx - CEN[0], yy - CEN[1])
for rr, c in ((22, (150, 96, 230)), (16, (196, 150, 255)), (10, (150, 96, 230)), (4, (220, 190, 255))):
    ring = csel & (np.abs(dcen - rr) < .9)
    top[ring] = c; glow |= ring
for k in range(8):
    a = k * math.pi / 4
    for t in np.arange(4, 22, .5):
        x, y = int(CEN[0] + t * math.cos(a)), int(CEN[1] + t * math.sin(a))
        top[x, y] = (196, 150, 255); glow[x, y] = True


# --- Constructions -----------------------------------------------------------------
def rtower(cx, cy, z0, r, h, wall, roof, win, cone=None, trim=None):
    for z in range(z0, z0 + h):
        disk(cx, cy, z, r, wall, True, 6)
        if (z - z0) % 8 == 4 and z < z0 + h - 3:
            for dx, dy in ((int(r), 0), (0, int(r))):
                for e in range(2):
                    S(cx + dx, cy + dy, z + e, win)
    disk(cx, cy, z0 + h, r + 1, trim or wall)
    for a in range(0, 360, 40):
        S(cx + round((r + 1) * math.cos(math.radians(a))), cy + round((r + 1) * math.sin(math.radians(a))), z0 + h + 1, wall)
    Hc = cone or int(r * 2.6)
    for i in range(Hc):
        disk(cx, cy, z0 + h + 1 + i, max((r + .4) * (1 - i / Hc), .6), roof, a=5)
    S(cx, cy, z0 + h + 1 + Hc, trim or win); S(cx, cy, z0 + h + 2 + Hc, trim or win)


def wallbox(x0, y0, x1, y1, z0, h, w, win=None, th=2):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            e = min(x - x0, x1 - x, y - y0, y1 - y)
            if e < th:
                for z in range(z0, z0 + h):
                    S(x, y, z, j(w, 6))
                if e == 0 and (x + y) % 4 < 2:
                    S(x, y, z0 + h, j(w, 6))
    if win:
        for z in range(z0 + 4, z0 + h - 3, 7):
            for x in range(x0 + 3, x1 - 2, 5):
                for e in range(3):
                    S(x, y1, z + e, win)
            for y in range(y0 + 3, y1 - 2, 5):
                for e in range(3):
                    S(x1, y, z + e, win)


def pyr(x0, y0, x1, y1, z, c):
    i = 0
    while x0 + i <= x1 - i and y0 + i <= y1 - i:
        for x in range(x0 + i, x1 - i + 1):
            for y in range(y0 + i, y1 - i + 1):
                if x in (x0 + i, x1 - i) or y in (y0 + i, y1 - i):
                    S(x, y, z + i, j(c, 5))
        i += 1


def H(x, y):
    return int(hm[x, y])


def tree(x, y, kind='oak', big=1.0):
    h = H(x, y)
    t = (92, 64, 42)
    L = {'oak': [(48, 112, 52), (60, 130, 58), (72, 146, 62)], 'dark': [(36, 92, 48), (44, 104, 52)]}[kind]
    Ht = int(random.randint(7, 11) * big)
    for z in range(h + 1, h + Ht):
        S(x, y, z, t)
        if big > 1.3:
            S(x + 1, y, z, t); S(x, y + 1, z, t); S(x + 1, y + 1, z, t)
    R = 4.5 * big
    for dx in range(-int(R) - 1, int(R) + 2):
        for dy in range(-int(R) - 1, int(R) + 2):
            for dz in range(-int(R * .6), int(R * .9) + 1):
                if dx * dx + dy * dy + (dz * 1.4) ** 2 < R * R + random.randint(-4, 3):
                    S(x + dx, y + dy, h + Ht + dz, j(random.choice(L), 10))


def giant_tree(x, y):
    h = H(x, y)
    t = (98, 70, 44)
    for z in range(h - 2, h + 54):
        r = 6.5 if z < h + 6 else 5
        disk(x, y, z, r, t, a=6)
    for a in range(0, 360, 60):  # racines
        for s in range(5, 14):
            xr, yr = int(x + s * math.cos(math.radians(a))), int(y + s * math.sin(math.radians(a)))
            S(xr, yr, H(xr, yr) + 1, j(t, 6))
    blobs = [(0, 0, 54, 26), (-18, 8, 44, 18), (16, -12, 46, 19), (10, 18, 42, 16), (-12, -18, 48, 15), (0, 0, 70, 16)]
    for bx, by, bz, br in blobs:
        for dx in range(-br, br + 1):
            for dy in range(-br, br + 1):
                for dz in range(-br // 2, br // 2 + 1):
                    if dx * dx + dy * dy + (dz * 1.6) ** 2 < br * br + random.randint(-30, 10):
                        S(x + bx + dx, y + by + dy, h + bz + dz, j(random.choice([(46, 120, 50), (58, 138, 56), (70, 150, 58), (40, 104, 46)]), 9))
    for _ in range(80):  # lianes
        vx, vy = x + random.randint(-20, 20), y + random.randint(-20, 20)
        for dz in range(random.randint(3, 12)):
            S(vx, vy, h + 38 - dz, (58, 120, 48))


def mushroom(x, y, big=1.0):
    h = H(x, y)
    hh = int(random.randint(6, 10) * big)
    for z in range(h + 1, h + hh):
        S(x, y, z, (232, 226, 214))
    r = 4 * big
    for i, z in enumerate(range(h + hh, h + hh + 3)):
        disk(x, y, z, r - i * 1.2, (156, 82, 214), a=10)
    for _ in range(int(6 * big)):
        a = random.random() * 6.28
        S(x + int(r * .7 * math.cos(a)), y + int(r * .7 * math.sin(a)), h + hh + 1, (240, 222, 255))
        glow_vox.add((x + int(r * .7 * math.cos(a)), y + int(r * .7 * math.sin(a)), h + hh + 1))


def pine(x, y):
    h = H(x, y)
    for z in range(h + 1, h + 4):
        S(x, y, z, (80, 58, 40))
    Hh = random.randint(12, 18)
    for i in range(Hh):
        r = (1 - i / Hh) * 4.2 + .3
        c = (236, 244, 252) if i % 4 == 0 else (40, 86, 64)
        disk(x, y, h + 3 + i, r, c, a=6)


def crystal(x, y, z, L, c):
    for i in range(L):
        rr = (1 - i / L) * 2.6 + .3
        disk(x, y, z + i, rr, c, a=12)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            glow_vox.add((x + dx, y + dy, z + i))


def cluster(x, y, c, n=5, L=(10, 24)):
    for _ in range(n):
        a, b = x + random.randint(-5, 5), y + random.randint(-5, 5)
        crystal(a, b, H(a, b), random.randint(*L), c)


def waterfall(x, y, z0, z1, c, w=3):
    for z in range(z1, z0):
        for k in range(w):
            S(x + k, y - k, z, j(c, 14))
            glow_vox.add((x + k, y - k, z))


def lantern(x, y):
    h = H(x, y)
    for z in range(h + 1, h + 5):
        S(x, y, z, (60, 50, 48))
    S(x, y, h + 5, (255, 214, 110)); glow_vox.add((x, y, h + 5))


def arch(x, y, z, w, hgt, pierre, coeur):
    """Arche face a la camera (plan suivant (1,-1))."""
    for t in range(-w, w + 1):
        for k in range(hgt):
            bord = abs(t) >= w - 1 or k >= hgt - 2
            c = j(pierre, 8) if bord else coeur
            if bord or (abs(t) < w - 1 and k < hgt - 2):
                S(x + t, y - t, z + k, c)
                if not bord:
                    glow_vox.add((x + t, y - t, z + k))


def bridge(a, b, za, zb, c=(140, 98, 60)):
    (x0, y0), (x1, y1) = a, b
    L = int(math.hypot(x1 - x0, y1 - y0))
    for i in range(L + 1):
        t = i / L
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        z = int(za + (zb - za) * t - math.sin(t * math.pi) * 3)
        nx, ny = -(y1 - y0) / L, (x1 - x0) / L
        for k in (-2, -1, 0, 1, 2):
            S(int(round(x + nx * k)), int(round(y + ny * k)), z, j(c, 12))
        if i % 6 == 0:
            for k in (-3, 3):
                S(int(round(x + nx * k)), int(round(y + ny * k)), z + 1, (96, 66, 44))
                S(int(round(x + nx * k)), int(round(y + ny * k)), z + 2, (96, 66, 44))


glow_vox = set()

# Sylvaria
giant_tree(FOR[0] - 38, FOR[1] - 10)
rtower(FOR[0] + 30, FOR[1] - 52, H(FOR[0] + 30, FOR[1] - 52) + 1, 6, 24, (128, 126, 130), (84, 120, 70), (255, 214, 110), cone=12, trim=(150, 150, 150))
gx, gy = FOR[0] + 18, FOR[1] + 24
arch(gx, gy, H(gx, gy) + 1, 7, 18, (118, 116, 122), (176, 96, 255))
for _ in range(150):
    x, y = FOR[0] + random.randint(-100, 100), FOR[1] + random.randint(-100, 100)
    if 0 <= x < N and 0 <= y < N and fsel[x, y] and ff[x, y] > .12 and math.hypot(x - FOR[0] + 38, y - FOR[1] + 10) > 26:
        tree(x, y, random.choice(['oak', 'oak', 'dark']), random.choice([1, 1, 1.25]))
for _ in range(14):
    x, y = FOR[0] + random.randint(-70, 70), FOR[1] + random.randint(-70, 70)
    if 0 <= x < N and 0 <= y < N and fsel[x, y] and ff[x, y] > .2:
        mushroom(x, y, random.choice([1, 1.4, 1.8]))
for _ in range(10):
    x, y = FOR[0] + random.randint(-80, 80), FOR[1] + random.randint(-80, 80)
    if 0 <= x < N and 0 <= y < N and fsel[x, y]:
        lantern(x, y)

for _ in range(900):
    x, y = FOR[0] + random.randint(-110, 110), FOR[1] + random.randint(-110, 110)
    if 0 <= x < N and 0 <= y < N and fsel[x, y] and (x, y, H(x, y) + 1) not in vox:
        S(x, y, H(x, y) + 1, random.choice([(70, 150, 56), (86, 166, 62), (200, 120, 220), (240, 220, 90), (230, 90, 120)]))

# Pyrelune : forteresse sombre sur le flanc
W3 = dict(wall=(58, 48, 56), roof=(44, 36, 44), win=(255, 150, 40), trim=(200, 90, 40))
fx, fy = VOL[0] + 34, VOL[1] + 30
b3 = H(fx, fy)
wallbox(fx - 20, fy - 20, fx + 20, fy + 20, b3, 18, W3['wall'], W3['win'])
for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    rtower(fx + sx * 20, fy + sy * 20, b3, 5, 26, W3['wall'], W3['roof'], W3['win'], cone=18, trim=W3['trim'])
wallbox(fx - 10, fy - 10, fx + 8, fy + 8, b3 + 18, 22, W3['wall'], W3['win'])
rtower(fx, fy, b3 + 40, 4, 16, W3['wall'], W3['roof'], W3['win'], cone=22, trim=W3['trim'])
for dx, dy in ((-8, 8), (8, -8)):
    rtower(fx + dx, fy + dy, b3 + 40, 3, 10, W3['wall'], W3['roof'], W3['win'], cone=14, trim=W3['trim'])
arch(fx + 22, fy + 22, b3, 6, 14, (70, 58, 64), (176, 96, 255))
cluster(VOL[0] + 70, VOL[1] - 40, (255, 110, 90), n=4, L=(12, 26))
for _ in range(6):
    x, y = VOL[0] + random.randint(-60, 60), VOL[1] + random.randint(-60, 60)
    if 0 <= x < N and 0 <= y < N and vsel[x, y]:
        lantern(x, y)

# Azurea : chateau de glace
W2 = dict(wall=(224, 236, 248), roof=(70, 110, 214), win=(140, 236, 255), trim=(196, 228, 252))
ax, ay = GLA[0] - 10, GLA[1] - 20
b2 = H(ax, ay)
wallbox(ax - 26, ay - 26, ax + 26, ay + 26, b2, 16, W2['wall'])
for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
    rtower(ax + sx * 26, ay + sy * 26, b2, 6, 28, W2['wall'], W2['roof'], W2['win'], cone=22, trim=W2['trim'])
wallbox(ax - 14, ay - 14, ax + 12, ay + 12, b2 + 16, 30, W2['wall'], W2['win'])
pyr(ax - 15, ay - 15, ax + 13, ay + 13, b2 + 46, W2['roof'])
rtower(ax + 12, ay + 12, b2 + 16, 6, 48, W2['wall'], W2['roof'], W2['win'], cone=30, trim=W2['trim'])
rtower(ax - 1, ay - 1, b2 + 46, 5, 26, W2['wall'], W2['roof'], W2['win'], cone=34, trim=W2['trim'])
arch(ax + 28, ay + 34, H(ax + 28, ay + 34) + 1, 6, 15, (196, 214, 236), (110, 200, 255))
for c, (dx, dy) in (((110, 170, 255), (52, 20)), ((168, 110, 255), (30, 60)), ((120, 200, 255), (-40, 60)), ((150, 120, 255), (60, -40))):
    cluster(GLA[0] + dx, GLA[1] + dy, c, n=5)
for _ in range(34):
    x, y = GLA[0] + random.randint(-100, 100), GLA[1] + random.randint(-100, 100)
    if 0 <= x < N and 0 <= y < N and gsel[x, y] and gf[x, y] > .1 and max(abs(x - ax), abs(y - ay)) > 36:
        pine(x, y)

# Plateau central : obelisque et cristal
crystal(CEN[0], CEN[1], H(CEN[0], CEN[1]) + 1, 30, (186, 130, 255))
for dx, dy in ((14, -14), (-14, 14)):
    for z in range(H(CEN[0] + dx, CEN[1] + dy) + 1, H(CEN[0] + dx, CEN[1] + dy) + 12):
        S(CEN[0] + dx, CEN[1] + dy, z, (90, 80, 96))
    for z in range(4):
        S(CEN[0] + dx, CEN[1] + dy + 1, H(CEN[0] + dx, CEN[1] + dy) + 7 + z, (120, 70, 200))

# Ponts
def bord(a, b):
    """Point de l'ile a vers l'ile b, au bord."""
    ax_, ay_ = a[0], a[1]
    dx, dy = b[0] - a[0], b[1] - a[1]
    L = math.hypot(dx, dy)
    for s in range(int(L)):
        x, y = int(ax_ + dx * s / L), int(ay_ + dy * s / L)
        if not land[x, y] or (s > 20 and hm[x, y] < 0):
            return (int(ax_ + dx * (s - 4) / L), int(ay_ + dy * (s - 4) / L))
    return (int(b[0]), int(b[1]))


for a, b in ((FOR, CEN), (CEN, GLA), (VOL, GLA), (VOL, CEN)):
    p0, p1 = bord(a, b), bord(b, a)
    bridge(p0, p1, H(*p0), H(*p1))

# Cascades
def cascade(isle, sel, ang, c, w=3):
    cx, cy = isle[0], isle[1]
    for s in range(200):
        x, y = int(cx + s * math.cos(ang)), int(cy + s * math.sin(ang))
        if not (0 <= x < N and 0 <= y < N) or not sel[x, y]:
            xi, yi = int(cx + (s - 1) * math.cos(ang)), int(cy + (s - 1) * math.sin(ang))
            for k in range(-w, w + 1):  # entaille dans le bord
                S(xi + k, yi - k, H(xi, yi), j(c, 10)); glow_vox.add((xi + k, yi - k, H(xi, yi)))
            waterfall(x, y, H(xi, yi) + 1, int(bot[xi, yi]) - 40, c, w)
            return


cascade(FOR, fsel, math.radians(95), (120, 190, 255))
cascade(FOR, fsel, math.radians(60), (120, 190, 255))
cascade(GLA, gsel, math.radians(70), (170, 220, 255), 4)
cascade(GLA, gsel, math.radians(30), (170, 220, 255))
cascade(VOL, vsel, math.radians(20), (255, 128, 30), 3)
cascade(VOL, vsel, math.radians(75), (255, 128, 30), 3)
cascade(CEN, csel, math.radians(45), (190, 120, 255), 6)
# fumee du volcan (dessinee a part)


# Petites iles flottantes
for _ in range(16):
    while True:
        x, y = random.randint(20, N - 20), random.randint(20, N - 20)
        if not land[max(0, x - 18):x + 18, max(0, y - 18):y + 18].any():
            break
    r = random.randint(5, 11)
    z = random.randint(30, 110)
    pal, roche = random.choice([(GRASS, (112, 106, 112)), (SNOW, (150, 160, 188)), (ASH, (72, 60, 66))])
    d = np.hypot(xx[x - r - 2:x + r + 3, y - r - 2:y + r + 3] - x, yy[x - r - 2:x + r + 3, y - r - 2:y + r + 3] - y) / r
    for (i, k), v in np.ndenumerate(d):
        if v < 1 - (n3[x + i - r - 2, y + k - r - 2] - .5) * .5:
            X, Y = x + i - r - 2, y + k - r - 2
            hm[X, Y] = z + int((1 - v) * 3); bot[X, Y] = z - int(max(0, 1 - v) ** .6 * r * 2.2 + n3[X, Y] * 4)
            land[X, Y] = True; top[X, Y] = random.choice(pal); sid[X, Y] = roche; dirt[X, Y] = roche
    if pal is GRASS and random.random() < .7:
        tree(x, y, 'oak', .8)

print('voxels', len(vox), 'colonnes', int(land.sum()))

# --- Rendu ---------------------------------------------------------------------------
xs, ys = [], []
for x, y in zip(*np.where(land)):
    for z in (hm[x, y], bot[x, y]):
        sx, sy = P(x, y, z); xs.append(sx); ys.append(sy)
for (x, y, z) in vox:
    sx, sy = P(x, y, z); xs.append(sx); ys.append(sy)
OX, OY = -min(xs) + 20, -min(ys) + 20
CW, CH = int(max(xs) + OX + 40), int(max(ys) + OY + 40)
print('toile', CW, CH)


def Q(x, y, z):
    sx, sy = P(x, y, z)
    return (sx + OX, sy + OY)


def shade(c, f):
    return (int(min(255, c[0] * f)), int(min(255, c[1] * f)), int(min(255, c[2] * f)), 255)


img = Image.new('RGBA', (CW, CH), (0, 0, 0, 0))
dr = ImageDraw.Draw(img)
gmask = Image.new('L', (CW, CH), 0)
gd = ImageDraw.Draw(gmask)

items = []
for x, y in zip(*np.where(land)):
    items.append((x + y, int(hm[x, y]), 0, x, y))
for (x, y, z), c in vox.items():
    if (x + 1, y, z) in vox and (x, y + 1, z) in vox and (x, y, z + 1) in vox:
        continue
    items.append((x + y, z, 1, x, y, z, c))
items.sort(key=lambda t: (t[0], t[1]))
print('elements', len(items))
for it in items:
    if it[2] == 0:
        _, h, _, x, y = it
        b = int(bot[x, y]); t = tuple(int(v) for v in top[x, y]); rk = tuple(int(v) for v in sid[x, y]); dt = tuple(int(v) for v in dirt[x, y])
        ln = (h - 3, h)
        tc = tuple(int(v * .82) for v in t)
        for lo, hi, col in ((b, h - 3, rk), (h - 3, h - 1, dt), (h - 1, h, tc)):
            if hi <= lo:
                continue
            # bandes de roche pour un aspect "blocs"
            dr.polygon([Q(x, y + 1, hi), Q(x + 1, y + 1, hi), Q(x + 1, y + 1, lo), Q(x, y + 1, lo)], fill=shade(col, .74 * (0.92 + 0.08 * ((x // 2) % 2))))
            dr.polygon([Q(x + 1, y, hi), Q(x + 1, y + 1, hi), Q(x + 1, y + 1, lo), Q(x + 1, y, lo)], fill=shade(col, .55 * (0.92 + 0.08 * ((y // 2) % 2))))
        poly = [Q(x, y, h), Q(x + 1, y, h), Q(x + 1, y + 1, h), Q(x, y + 1, h)]
        dr.polygon(poly, fill=shade(t, 1))
        if glow[x, y]:
            gd.polygon(poly, fill=255)
    else:
        _, _, _, x, y, z, c = it
        polys = ([Q(x, y + 1, z + 1), Q(x + 1, y + 1, z + 1), Q(x + 1, y + 1, z), Q(x, y + 1, z)], .74), \
                ([Q(x + 1, y, z + 1), Q(x + 1, y + 1, z + 1), Q(x + 1, y + 1, z), Q(x + 1, y, z)], .56), \
                ([Q(x, y, z + 1), Q(x + 1, y, z + 1), Q(x + 1, y + 1, z + 1), Q(x, y + 1, z + 1)], 1)
        g = (x, y, z) in glow_vox
        for poly, f in polys:
            dr.polygon(poly, fill=shade(c, f if not g else min(1.15, f + .25)))
            if g:
                gd.polygon(poly, fill=255)
        if not g:
            for poly, _ in polys:
                gd.polygon(poly, fill=0)

isl = np.array(img).astype(np.float32)
gm = np.array(gmask).astype(np.float32)[..., None] / 255
bl = cv2.GaussianBlur(isl[..., :3] * gm, (0, 0), 14) + cv2.GaussianBlur(isl[..., :3] * gm, (0, 0), 40) * .8
np.save(ICI + 'carte3_glow.npy', bl.astype(np.float16))
img.save(ICI + 'carte3_iles.png')
anchors = {k: Q(v[0], v[1], H(int(v[0]), int(v[1])) + 20) for k, v in (('sylvaria', FOR), ('pyrelune', (VOL[0] - 8, VOL[1] - 8)), ('azurea', GLA), ('centre', CEN))}
json.dump({'anchors': anchors, 'size': [CW, CH]}, open(ICI + 'carte3_meta.json', 'w'))
print('ok', anchors)
