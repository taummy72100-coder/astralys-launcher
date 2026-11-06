"""Illustrations pixel art (32x32) des 4 metiers d'Astralys, avec contour sombre automatique et reflets."""
import math, sys
import numpy as np
from PIL import Image

N = 32
CONTOUR = (24, 16, 28, 255)


class Toile:
    def __init__(s):
        s.p = np.zeros((N, N, 4), np.uint8)

    def px(s, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < N and 0 <= y < N: s.p[y, x] = c if len(c) == 4 else (*c, 255)

    def ligne(s, x0, y0, x1, y1, c, ep=1):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(n + 1):
            t = i / n; x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for dx in range(ep):
                for dy in range(ep): s.px(x + dx - ep // 2, y + dy - ep // 2, c)

    def disque(s, cx, cy, r, c):
        for y in range(N):
            for x in range(N):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r: s.px(x, y, c)

    def poly(s, pts, c):
        for y in range(N):
            for x in range(N):
                if dedans(x + .5, y + .5, pts): s.px(x, y, c)

    def contour(s):
        a = s.p[..., 3] > 0
        o = np.zeros_like(a)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            o |= np.roll(np.roll(a, dy, 0), dx, 1)
        s.p[o & ~a] = CONTOUR

    def sauver(s, n):
        Image.fromarray(s.p, 'RGBA').save(n)
        return Image.fromarray(s.p, 'RGBA')


def dedans(x, y, pts):
    r = False; j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]; xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi: r = not r
        j = i
    return r


BOIS = [(92, 56, 30), (136, 86, 44), (176, 118, 62)]
FER = [(96, 104, 120), (160, 170, 186), (222, 228, 238)]
OR = [(168, 112, 20), (232, 176, 40), (255, 232, 130)]


def mineur():
    t = Toile()
    # deux pioches croisees : tete courbe perpendiculaire au manche
    for sens in (1, -1):
        x0, x1 = (9, 25) if sens > 0 else (23, 7)
        y0, y1 = 29, 8
        t.ligne(x0, y0, x1, y1, BOIS[0], 2)
        t.ligne(x0, y0 - .7, x1, y1 - .7, BOIS[2], 1)
        dx, dy = x1 - x0, y1 - y0; L = math.hypot(dx, dy); dx, dy = dx / L, dy / L
        px_, py_ = -dy, dx
        for i in range(-14, 15):
            u = i / 2
            hx = x1 + px_ * u - dx * (abs(u) ** 2) * .09
            hy = y1 + py_ * u - dy * (abs(u) ** 2) * .09
            col = FER[2] if abs(u) < 1.5 else FER[1] if abs(u) < 5.5 else FER[0]
            t.px(hx, hy, col); t.px(hx + dx * .9, hy + dy * .9, FER[0] if abs(u) > 2 else FER[1])
    # amas de cristaux d'Astralys devant
    cristaux = [((16, 30), (12.5, 20), (16, 13), (19.5, 20)), ((11, 30), (8.5, 24), (10.5, 19), (13, 24)), ((21, 30), (19, 24), (22, 18), (24, 24))]
    teintes = [((150, 92, 230), (196, 150, 255), (240, 220, 255)), ((110, 60, 200), (160, 110, 240), (220, 190, 255)), ((110, 60, 200), (170, 120, 245), (225, 200, 255))]
    for (b, l, h, r), (c0, c1, c2) in zip(cristaux[1:] + cristaux[:1], teintes[1:] + teintes[:1]):
        t.poly([b, l, h, r], c0)
        t.poly([b, (h[0], (b[1] + h[1]) / 2), h, r], c1)
        t.ligne(h[0], h[1] + 1, h[0] - .5, h[1] + 4, c2)
    t.px(16, 15, (255, 255, 255)); t.px(10, 21, (255, 255, 255))
    t.contour()
    # etincelles
    for x, y in ((4, 16), (27, 14)):
        t.px(x, y, (255, 240, 200)); t.px(x - 1, y, (240, 200, 120)); t.px(x + 1, y, (240, 200, 120)); t.px(x, y - 1, (240, 200, 120)); t.px(x, y + 1, (240, 200, 120))
    return t


def farmeur():
    t = Toile()
    # gerbe de ble doree
    for k, ang in enumerate((-22, -8, 6, 20)):
        a = math.radians(ang)
        bx, by = 16, 27
        hx, hy = bx + math.sin(a) * 19, by - math.cos(a) * 19
        t.ligne(bx, by, hx, hy, (170, 140, 40))
        # epi
        for j in range(6):
            f = .62 + j * .07
            ex, ey = bx + math.sin(a) * 19 * f, by - math.cos(a) * 19 * f
            for s in (-1, 1):
                t.px(ex + s * math.cos(a) * 1.2, ey + s * math.sin(a) * 1.2, OR[1] if j % 2 else OR[2])
            t.px(ex, ey, OR[0])
        t.px(hx, hy - 1, OR[2])
    # feuilles
    for s in (-1, 1):
        for j in range(7):
            t.px(16 + s * (2 + j * .9), 24 - j * .9 + (j * j) * .08, (70, 150, 60) if j < 5 else (110, 190, 80))
            t.px(16 + s * (2 + j * .9), 25 - j * .9 + (j * j) * .08, (46, 110, 46))
    # ruban rouge
    t.poly([(12, 20), (20, 20), (20, 23), (12, 23)], (200, 50, 50))
    t.ligne(12, 20, 20, 20, (240, 100, 90))
    t.poly([(15, 23), (13, 28), (15, 27)], (170, 36, 40)); t.poly([(17, 23), (19, 28), (17, 27)], (170, 36, 40))
    t.contour()
    # pousse de lumiere (petit soleil)
    for x, y in ((5, 6), (26, 9)):
        t.px(x, y, (255, 246, 190)); t.px(x - 1, y, (255, 214, 110)); t.px(x + 1, y, (255, 214, 110)); t.px(x, y - 1, (255, 214, 110)); t.px(x, y + 1, (255, 214, 110))
    return t


def hunter():
    t = Toile()
    # arc recourbe
    pts = []
    for i in range(41):
        u = i / 40; y = 3 + u * 26
        x = 9 + math.sin(u * math.pi) * -6 + (math.sin(u * math.pi * 2) * 1.2 if u < .15 or u > .85 else 0)
        pts.append((x + 6, y))
    for i, (x, y) in enumerate(pts):
        c = BOIS[2] if 12 < i < 28 else BOIS[1]
        t.px(x, y, c); t.px(x + 1, y, BOIS[0]); t.px(x - 1, y, BOIS[1])
    t.poly([(8, 13), (11, 13), (11, 19), (8, 19)], (60, 36, 26))   # poignee
    t.ligne(8.5, 13, 8.5, 19, (120, 70, 44))
    # corde tendue
    t.ligne(pts[0][0] + 1, pts[0][1], 21, 16, (230, 226, 210)); t.ligne(21, 16, pts[-1][0] + 1, pts[-1][1], (230, 226, 210))
    # fleche
    t.ligne(5, 16, 27, 16, (150, 110, 70))
    t.poly([(26, 13), (31, 16), (26, 19)], FER[1]); t.ligne(27, 15, 30, 16, FER[2])
    for k in range(3):
        t.px(19 + k, 14 - k * .3, (210, 50, 60)); t.px(19 + k, 18 + k * .3, (210, 50, 60))
        t.px(19 + k, 15, (240, 90, 90)); t.px(19 + k, 17, (240, 90, 90))
    t.contour()
    # plumes de chasseur et etincelle
    for x, y in ((24, 6), (26, 25)):
        t.px(x, y, (220, 236, 255)); t.px(x - 1, y, (120, 170, 255)); t.px(x + 1, y, (120, 170, 255)); t.px(x, y - 1, (120, 170, 255)); t.px(x, y + 1, (120, 170, 255))
    return t


def alchimiste():
    t = Toile()
    # fiole ronde
    t.disque(16, 20, 9, (210, 224, 240))
    t.disque(16, 20, 8, (150, 170, 200))
    # liquide rose lumineux
    for y in range(N):
        for x in range(N):
            if (x - 16) ** 2 + (y - 20) ** 2 <= 7.5 ** 2 and y >= 18 + math.sin(x * .8) * .8:
                d = math.hypot(x - 14, y - 24)
                t.px(x, y, (255, 150, 220) if d < 3 else (236, 84, 190) if d < 6 else (180, 40, 150))
    # col et bouchon
    t.poly([(13.5, 5), (18.5, 5), (18.5, 12), (13.5, 12)], (190, 205, 225))
    t.ligne(14, 6, 14, 12, (240, 246, 255))
    t.poly([(13, 2), (19, 2), (19, 5), (13, 5)], BOIS[1]); t.ligne(13, 2, 19, 2, BOIS[2])
    # reflet sur le verre
    t.ligne(10.5, 17, 11.5, 14.5, (255, 255, 255)); t.px(10, 19, (240, 248, 255))
    # bulles
    for x, y, c in ((18, 21, (255, 210, 240)), (15, 25, (255, 230, 248)), (19, 24, (255, 190, 235))): t.px(x, y, c)
    t.contour()
    # volutes magiques
    for i in range(10):
        a = i * .7; r = 1.5 + i * .55
        t.px(24 + math.cos(a) * r * .6, 9 - i * .7 + math.sin(a), (255, 170, 240) if i % 2 else (210, 140, 255))
    t.px(6, 10, (255, 240, 255)); t.px(5, 10, (230, 160, 255)); t.px(7, 10, (230, 160, 255)); t.px(6, 9, (230, 160, 255)); t.px(6, 11, (230, 160, 255))
    return t


if __name__ == '__main__':
    out = sys.argv[1]
    planche = Image.new('RGBA', (128, 32))
    for i, f in enumerate((mineur, farmeur, hunter, alchimiste)):
        planche.paste(f().sauver(f'{out}/metier_{i}.png'), (i * 32, 0))
    planche.save(f'{out}/metiers.png')
    fond = Image.new('RGBA', (128 * 4 + 40, 32 * 4 + 20), (27, 27, 31, 255))
    fond.alpha_composite(planche.resize((512, 128), Image.NEAREST), (20, 10))
    fond.save(f'{out}/apercu.png')
