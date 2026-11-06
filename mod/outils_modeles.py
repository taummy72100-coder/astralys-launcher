"""Modeles 3D d'Astralys facon Blockbench : PNJ (geometrie detaillee + texture 128x128), coffres, caisse, batons, armes, elixirs.

Usage : python3 outils_modeles.py <dossier mod> <dossier Blockbench> [dossier apercus]
Ecrit : textures + modeles JSON (objets, caisse), PnjModeles.java (geometrie des PNJ), et des .bbmodel ouvrables dans Blockbench.
Une seule description par modele sert au jeu ET a Blockbench : on retouche ici, on relance, tout suit.
"""
import base64, io, json, math, os, sys, uuid
import numpy as np
from PIL import Image

R = np.random.default_rng(11)


def clair(c, k): return tuple(int(max(0, min(255, v * k))) for v in c[:3])
def mel(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ====================================================================== matieres : f(w, h, face) -> tableau (h, w, 4)
def _base(w, h, c, grain):
    # bruit par amas de 2x2 (facon pixel art peint a la main) + un peu de grain fin
    a = np.zeros((h, w, 4)); a[..., :3] = np.array(c[:3], float)
    amas = np.repeat(np.repeat(R.normal(0, grain * 1.1, ((h + 1) // 2, (w + 1) // 2, 1)), 2, 0), 2, 1)[:h, :w]
    a[..., :3] += amas + R.normal(0, grain * .35, (h, w, 1)); a[..., 3] = 255
    return a


def uni(c, grain=4): return lambda w, h, f: _base(w, h, c, grain)


def peau(c): return uni(c, 3)


def tissu(c, grain=4, motif=None, mc=(240, 210, 110), dens=.05, plis=True):
    def p(w, h, f):
        a = _base(w, h, c, grain)
        if plis and f not in ('haut', 'bas'):
            ph = R.uniform(0, 6)
            for x in range(w): a[:, x, :3] *= 1 + .07 * math.sin(x * 1.9 + ph) - (.09 if R.random() < .2 else 0)
            for y in range(h): a[y, :, :3] *= 1.04 - .1 * y / max(1, h - 1)
        if motif == 'etoiles':
            for _ in range(max(1, int(w * h * dens))):
                x, y = R.integers(w), R.integers(h); a[y, x, :3] = mc
                if R.random() < .3 and 0 < x < w - 1: a[y, x - 1, :3] = mel(mc, c, .5); a[y, x + 1, :3] = mel(mc, c, .5)
        elif motif == 'rayures':
            for x in range(0, w, 3): a[:, x, :3] = mel(mc, c, .3)
        elif motif == 'losanges':
            for y in range(h):
                for x in range(w):
                    if (x + y) % 4 == 0 or (x - y) % 4 == 0: a[y, x, :3] = mel(mc, a[y, x, :3], .75)
        elif motif == 'mailles':
            for y in range(h):
                for x in range(w):
                    if (x + (y % 2)) % 2 == 0: a[y, x, :3] *= .78
        return a
    return p


def cheveux(c):
    def p(w, h, f):
        a = _base(w, h, c, 5)
        for x in range(w): a[:, x, :3] *= R.choice([.82, .95, 1.05, 1.15])
        if f not in ('haut', 'bas'):
            for x in range(w):
                for y in range(h):
                    if R.random() < .12: a[y, x, :3] *= 1.25
        return a
    return p


def fourrure(c):
    def p(w, h, f):
        a = _base(w, h, c, 16)
        for y in range(h):
            for x in range(w):
                if R.random() < .25: a[y, x, :3] *= .8
        return a
    return p


def metal(c, rivets=False, reflet=True):
    def p(w, h, f):
        a = _base(w, h, c, 4)
        a[..., :3] *= np.linspace(1.22, .78, h)[:, None, None]
        if reflet and w > 2:
            for i in range(h): a[i, (i + w // 3) % w, :3] *= 1.22
        if rivets and w > 3 and h > 3:
            for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)): a[y, x, :3] = clair(c, 1.5)
        return a
    return p


OR_C = (214, 164, 40)


def dore(rivets=False):
    base = metal(OR_C, rivets)
    def p(w, h, f):
        a = base(w, h, f)
        for _ in range(max(1, w * h // 14)):
            x, y = R.integers(w), R.integers(h); a[y, x, :3] = (255, 240, 170)
        return a
    return p


def bois(c, sens='h', planche=4):
    def p(w, h, f):
        a = _base(w, h, c, 5)
        n = h if sens == 'h' else w
        for i in range(n):
            k = 1 + R.uniform(-.08, .08)
            if sens == 'h': a[i, :, :3] *= k
            else: a[:, i, :3] *= k
        for i in range(planche - 1, n, planche):
            if sens == 'h': a[i, :, :3] *= .62
            else: a[:, i, :3] *= .62
        for _ in range(max(1, w * h // 10)):   # veines
            x, y = R.integers(w), R.integers(h); a[y, x, :3] *= .78
        return a
    return p


def cuir(c, couture=True):
    def p(w, h, f):
        a = _base(w, h, c, 7)
        if couture and h > 4 and f not in ('haut', 'bas'):
            for x in range(0, w, 2): a[1, x, :3] = clair(c, 1.45); a[h - 2, x, :3] = clair(c, 1.45)
        return a
    return p


def gemme(c):
    def p(w, h, f):
        a = _base(w, h, c, 3)
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.hypot((xx - (w - 1) / 2) / max(w / 2, 1), (yy - (h - 1) / 2) / max(h / 2, 1))
        a[..., :3] *= (1.5 - .75 * d)[..., None]
        if w > 1 and h > 1: a[0 if h < 3 else 1, 0 if w < 3 else 1, :3] = (255, 255, 255)
        return a
    return p


def verre(c):
    def p(w, h, f):
        a = _base(w, h, c, 3)
        if w > 2 and f not in ('haut', 'bas'):
            a[1:max(2, h - 2), 1, :3] = (250, 250, 255)
            if h > 4: a[2:h - 3, 2, :3] = mel(c, (255, 255, 255), .6)
        return a
    return p


def bandes(*liste):
    """Bandes horizontales de haut en bas : (nombre de lignes, matiere) ; None = le reste."""
    def p(w, h, f):
        if f == 'haut': return liste[0][1](w, h, f)
        if f == 'bas': return liste[-1][1](w, h, f)
        fixes = sum(n for n, _ in liste if n); a = np.zeros((h, w, 4)); y = 0
        for n, m in liste:
            k = max(0, min(h - y, n if n else h - fixes))
            if k: a[y:y + k] = m(w, k, f); y += k
        if y < h: a[y:] = liste[-1][1](w, h - y, f)
        return a
    return p


def faces_seules(defaut, **par_face):
    """Matiere differente selon la face (avant, dos, droite, gauche, haut, bas)."""
    return lambda w, h, f: par_face.get(f, defaut)(w, h, f)


# ====================================================================== petites retouches de pixels (deco)
def P(a, x, y, c):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]: a[y, x] = (*c[:3], 255 if len(c) == 3 else c[3])


def trou(a, x0, y0, x1, y1):
    a[max(0, y0):y1, max(0, x0):x1, 3] = 0


def visage(yeux, peau_c, sourcils=None, bouche=(150, 84, 74), cils=False, lueur=False, taches=False, rides=False):
    def d(a):
        sc = sourcils or clair(peau_c, .55)
        for x in (1, 2, 5, 6): P(a, x, 3, sc)
        blanc = (240, 240, 245)
        P(a, 1, 4, blanc); P(a, 2, 4, yeux); P(a, 5, 4, yeux); P(a, 6, 4, blanc)
        if lueur: P(a, 2, 4, clair(yeux, 1.3)); P(a, 5, 4, clair(yeux, 1.3))
        if cils: P(a, 0, 4, (30, 20, 20)); P(a, 7, 4, (30, 20, 20))
        P(a, 3, 5, clair(peau_c, .82)); P(a, 4, 5, clair(peau_c, .9))
        P(a, 3, 6, bouche); P(a, 4, 6, clair(bouche, .85))
        P(a, 1, 5, mel(peau_c, (230, 120, 120), .3)); P(a, 6, 5, mel(peau_c, (230, 120, 120), .3))
        if taches:
            for x, y in ((1, 5), (2, 6), (5, 6), (6, 5)): P(a, x, y, clair(peau_c, .78))
        if rides: P(a, 0, 5, clair(peau_c, .75)); P(a, 7, 5, clair(peau_c, .75))
    return d


def tete(peau_c, chev_c, frange=2, cotes=5, chauve=False):
    pe, ch = peau(peau_c), cheveux(chev_c)
    def p(w, h, f):
        if f == 'haut': return pe(w, h, f) if chauve else ch(w, h, f)
        if f == 'bas': return pe(w, h, f)
        a = pe(w, h, f)
        n = 0 if chauve else {'avant': frange, 'dos': h, 'droite': cotes, 'gauche': cotes}[f]
        if n: a[:n] = ch(w, n, f)
        if f in ('droite', 'gauche') and not chauve and n < h: a[n:n + 1, :2] = ch(2, 1, f)   # favoris
        return a
    return p


def ligne_v(c, x0, larg=1, y0=0, y1=None):
    def d(a):
        for x in range(x0, x0 + larg): a[y0:y1, x, :3] = c
    return d


def etoile(c, cx, cy, grand=True):
    def d(a):
        P(a, cx, cy, (255, 255, 240))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): P(a, cx + dx, cy + dy, c)
        if grand:
            for dx, dy in ((2, 0), (-2, 0), (0, 2), (0, -2)): P(a, cx + dx, cy + dy, clair(c, .8))
    return d


def tout(*fs):
    def d(a):
        for f in fs: f(a)
    return d


# ====================================================================== cubes, os, emballage
class Cube:
    def __init__(s, mn, t, mat, deco=None, contour=True, lum=False, dechire=0):
        s.mn, s.t, s.mat, s.deco, s.contour, s.lum, s.dechire = tuple(mn), tuple(int(v) for v in t), mat, deco or {}, contour, lum, dechire
        assert all(v == int(v) for v in t), t

    def faces(s, u, v):
        w, h, d = s.t
        return {'haut': (u + d, v, w, d), 'bas': (u + d + w, v, w, d), 'droite': (u, v + d, d, h), 'avant': (u + d, v + d, w, h),
                'gauche': (u + d + w, v + d, d, h), 'dos': (u + 2 * d + w, v + d, w, h)}

    def peindre(s, img, u, v):
        for f, (x, y, w, h) in s.faces(u, v).items():
            if w <= 0 or h <= 0: continue
            a = s.mat(w, h, f)
            if 'cotes' in s.deco and f in ('avant', 'dos', 'droite', 'gauche'): s.deco['cotes'](a)
            if f in s.deco: s.deco[f](a)
            a[..., :3] *= {'haut': 1.12, 'bas': .7}.get(f, 1)
            if s.contour and w > 1 and h > 1:
                # biseau : arete du haut eclairee, bas et cotes dans l'ombre (volume lisible meme de loin)
                if f in ('haut', 'bas'): a[0, :, :3] *= 1.12; a[-1, :, :3] *= .82; a[:, 0, :3] *= .9; a[:, -1, :3] *= .9
                else:
                    a[0, :, :3] *= 1.18; a[-1, :, :3] *= .74
                    if w > 2: a[:, 0, :3] *= .88; a[:, -1, :3] *= .88
                    if h > 4: a[1:h // 3, :, :3] *= 1.04; a[-(h // 3):-1, :, :3] *= .9
            if s.dechire and f not in ('haut', 'bas'):
                for i in range(w):
                    k = int(R.integers(0, s.dechire + 1)) if i % 2 == 0 else max(0, k - 1)
                    if k: a[h - k:, i, 3] = 0
            img[y:y + h, x:x + w] = np.clip(a, 0, 255)


def emballer(cubes, larg):
    ordre = sorted(range(len(cubes)), key=lambda i: -(cubes[i].t[2] + cubes[i].t[1]))
    x = y = hl = 0; pos = {}
    for i in ordre:
        w, h, d = cubes[i].t; bw, bh = 2 * (d + w), d + h
        assert bw <= larg, (cubes[i].t, larg)
        if x + bw > larg: x = 0; y += hl; hl = 0
        pos[i] = (x, y); x += bw; hl = max(hl, bh)
    return pos, y + hl


def atlas(cubes, larg, haut=None):
    pos, h = emballer(cubes, larg)
    if haut is None: haut = 1 << max(4, (h - 1).bit_length())
    assert h <= haut, (h, haut)
    img = np.zeros((haut, larg, 4))
    for i, c in enumerate(cubes): c.peindre(img, *pos[i])
    return pos, img.astype(np.uint8)


class Os:
    def __init__(s, nom, pivot=(0, 0, 0), rot=(0, 0, 0)):
        s.nom, s.pivot, s.rot, s.cubes, s.enfants = nom, pivot, rot, [], []

    def c(s, mn, t, mat, **k): s.cubes.append(Cube(mn, t, mat, **k)); return s

    def os(s, nom, pivot, rot=(0, 0, 0)): o = Os(nom, pivot, rot); s.enfants.append(o); return o

    def tous(s):
        yield from s.cubes
        for e in s.enfants: yield from e.tous()


def png_b64(img):
    b = io.BytesIO(); Image.fromarray(img, 'RGBA').save(b, 'PNG'); return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def u(): return str(uuid.UUID(int=int(R.integers(0, 2 ** 63)) << 64 | int(R.integers(0, 2 ** 63))))


# ====================================================================== export PNJ : Java + Blockbench
def fl(v): return f'{float(v):g}F' if '.' in f'{float(v):g}' or 'e' in f'{float(v):g}' else f'{float(v):g}.0F'


def java_modele(nom, os_racine, pos_de, larg, haut):
    lignes, k = [], [0]
    def ecrire(o, parent):
        var = f'p{k[0]}'; k[0] += 1
        cl = 'CubeListBuilder.create()' + ''.join(
            f'.texOffs({pos_de[id(c)][0]}, {pos_de[id(c)][1]}).addBox({fl(c.mn[0])}, {fl(c.mn[1])}, {fl(c.mn[2])}, {fl(c.t[0])}, {fl(c.t[1])}, {fl(c.t[2])})' for c in o.cubes)
        px, py, pz = o.pivot
        pose = (f'PartPose.offsetAndRotation({fl(px)}, {fl(py)}, {fl(pz)}, {fl(o.rot[0])}, {fl(o.rot[1])}, {fl(o.rot[2])})' if any(o.rot)
                else f'PartPose.offset({fl(px)}, {fl(py)}, {fl(pz)})')
        lignes.append(f'        PartDefinition {var} = {parent}.addOrReplaceChild("{o.nom}", {cl}, {pose});')
        for e in o.enfants: ecrire(e, var)
    for o in os_racine: ecrire(o, 'r')
    return (f'    private static LayerDefinition {nom}() {{\n        MeshDefinition m = new MeshDefinition();\n        PartDefinition r = m.getRoot();\n'
            + '\n'.join(lignes) + f'\n        return LayerDefinition.create(m, {larg}, {haut});\n    }}\n')


def bbmodel_entite(nom, os_racine, pos_de, img):
    elements, outliner = [], []
    def grp(o, A):
        A = tuple(A[i] + o.pivot[i] for i in range(3))
        orig = [-A[0], 24 - A[1], A[2]]
        g = {'name': o.nom, 'origin': orig, 'rotation': [-math.degrees(o.rot[0]), -math.degrees(o.rot[1]), math.degrees(o.rot[2])],
             'color': 0, 'uuid': u(), 'export': True, 'mirror_uv': False, 'isOpen': False, 'locked': False, 'visibility': True, 'autouv': 0, 'children': []}
        for c in o.cubes:
            (w, h, d), (uu, vv) = c.t, pos_de[id(c)]
            to = [-(A[0] + c.mn[0]), 24 - (A[1] + c.mn[1]), A[2] + c.mn[2] + d]
            fr = [to[0] - w, to[1] - h, A[2] + c.mn[2]]
            faces = {'north': [uu + d, vv + d, uu + d + w, vv + d + h], 'east': [uu, vv + d, uu + d, vv + d + h],
                     'south': [uu + 2 * d + w, vv + d, uu + 2 * d + 2 * w, vv + d + h], 'west': [uu + d + w, vv + d, uu + 2 * d + w, vv + d + h],
                     'up': [uu + d + w, vv + d, uu + d, vv], 'down': [uu + d + 2 * w, vv, uu + d + w, vv + d]}
            e = {'name': o.nom, 'box_uv': True, 'rescale': False, 'locked': False, 'render_order': 'default', 'allow_mirror_modeling': True,
                 'from': fr, 'to': to, 'autouv': 0, 'color': len(elements) % 8, 'origin': orig, 'uv_offset': [uu, vv],
                 'faces': {k: {'uv': v, 'texture': 0} for k, v in faces.items()}, 'type': 'cube', 'uuid': u()}
            elements.append(e); g['children'].append(e['uuid'])
        for en in o.enfants: g['children'].append(grp(en, A))
        return g
    for o in os_racine: outliner.append(grp(o, (0, 0, 0)))
    return {'meta': {'format_version': '4.5', 'model_format': 'modded_entity', 'box_uv': True}, 'name': nom, 'model_identifier': nom,
            'modded_entity_version': '1.17', 'modded_entity_flip_y': True, 'visible_box': [1, 2, 0], 'resolution': {'width': img.shape[1], 'height': img.shape[0]},
            'elements': elements, 'outliner': outliner,
            'textures': [{'path': '', 'name': nom + '.png', 'folder': '', 'namespace': '', 'id': '0', 'particle': False, 'render_mode': 'default',
                          'visible': True, 'mode': 'bitmap', 'saved': False, 'uuid': u(), 'source': png_b64(img),
                          'width': img.shape[1], 'height': img.shape[0], 'uv_width': img.shape[1], 'uv_height': img.shape[0]}]}


# ====================================================================== corps humanoide de base
def humanoide(tete_m, tete_d, corps_m, corps_d, bras_m, jambe_m, bras_d=None, jambe_d=None):
    B = {n: Os(n, p) for n, p in (('head', (0, 0, 0)), ('hat', (0, 0, 0)), ('body', (0, 0, 0)), ('right_arm', (-5, 2, 0)),
                                  ('left_arm', (5, 2, 0)), ('right_leg', (-1.9, 12, 0)), ('left_leg', (1.9, 12, 0)))}
    B['head'].c((-4, -8, -4), (8, 8, 8), tete_m, deco={'avant': tete_d}, contour=False)
    B['body'].c((-4, 0, -2), (8, 12, 4), corps_m, deco=corps_d)
    B['right_arm'].c((-3, -2, -2), (4, 12, 4), bras_m, deco=bras_d)
    B['left_arm'].c((-1, -2, -2), (4, 12, 4), bras_m, deco=bras_d)
    B['right_leg'].c((-2, 0, -2), (4, 12, 4), jambe_m, deco=jambe_d)
    B['left_leg'].c((-2, 0, -2), (4, 12, 4), jambe_m, deco=jambe_d)
    return B


def manches(tissu_m, main_c, manchette=None, n_main=3):
    l = [(12 - n_main - (1 if manchette else 0), tissu_m)]
    if manchette: l.append((1, manchette))
    l.append((None, peau(main_c)))
    return bandes(*l)


def jambes(pant_m, bottes_m, n_bottes=4):
    return bandes((12 - n_bottes, pant_m), (None, bottes_m))


def epaulettes(B, m, deco=None, larg=5, h=3, etages=1, m2=None):
    B['right_arm'].c((-4, -3, -3), (larg, h, 6), m, deco=deco)
    B['left_arm'].c((-1, -3, -3), (larg, h, 6), m, deco=deco)
    if etages > 1:   # plaque superieure plus large, posee sur la premiere
        B['right_arm'].c((-4.5, -4, -3.5), (larg + 1, 2, 7), m2 or m, deco=deco)
        B['left_arm'].c((-1.5, -4, -3.5), (larg + 1, 2, 7), m2 or m, deco=deco)


def manches_larges(B, m, y=2, h=10, dechire=2, deco=None):
    """Manches evasees de mage : tombent sous la main, bas ouvert et dechire."""
    vide = lambda a: trou(a, 0, 0, a.shape[1], a.shape[0])
    d = dict(deco or {}); d['bas'] = vide
    B['right_arm'].c((-3.5, y, -2.5), (5, h, 5), m, deco=d, dechire=dechire)
    B['left_arm'].c((-1.5, y, -2.5), (5, h, 5), m, deco=d, dechire=dechire)


def poignets(B, m, y=5, h=4, deco=None):
    B['right_arm'].c((-3.5, y, -2.5), (5, h, 5), m, deco=deco)
    B['left_arm'].c((-1.5, y, -2.5), (5, h, 5), m, deco=deco)


def jupe(B, m, y=12, h=12, deco=None, dechire=0):
    B['body'].c((-4.5, y, -2.5), (9, h, 5), m, deco=deco, dechire=dechire)


def ceinture(B, m, y=10, h=2, boucle=None):
    B['body'].c((-5, y, -3), (10, h, 6), m, deco={'avant': boucle} if boucle else None)


def boucle(c, gem=None):
    def d(a):
        cx = a.shape[1] // 2
        for x in range(cx - 1, cx + 1):
            for y in range(a.shape[0]): P(a, x, y, c)
        if gem: P(a, cx - 1, a.shape[0] // 2, gem); P(a, cx, a.shape[0] // 2, clair(gem, 1.3))
    return d


# ====================================================================== les 8 PNJ
NUIT, OR2, BLANC_B = (38, 40, 104), (240, 200, 90), (228, 228, 236)


def archimage():
    pc, yeux = (214, 170, 136), (110, 190, 255)
    robe = tissu(NUIT, motif='etoiles', mc=OR2, dens=.035)
    liser = dore()
    B = humanoide(tete(pc, BLANC_B, 1, 5), visage(yeux, pc, sourcils=BLANC_B, lueur=True, rides=True),
                  bandes((2, liser), (None, robe)), {'avant': tout(ligne_v(OR2, 3, 2), etoile(OR2, 4, 6, False))},
                  manches(robe, pc, liser), jambes(robe, cuir((50, 40, 60))))
    h = B['head']
    # barbe : moustache au-dessus de la bouche, pointe qui descend sur la poitrine
    def barbe_d(a):
        trou(a, 2, 0, 4, 1); trou(a, 0, 5, 1, 7); trou(a, 5, 5, 6, 7); trou(a, 0, 6, 2, 7); trou(a, 4, 6, 6, 7)
        P(a, 2, 1, (200, 200, 210)); P(a, 3, 1, (200, 200, 210))
    h.c((-3, -3, -5), (6, 7, 1), cheveux(BLANC_B), deco={'avant': barbe_d, 'dos': barbe_d}, contour=False)
    # grand chapeau pointu : bord, bandeau dore a gemmes, trois etages, pointe penchee avec etoile
    h.c((-6, -9, -6), (12, 1, 12), tissu((30, 32, 86), plis=False))
    h.c((-4.5, -11, -4.5), (9, 2, 9), liser, deco={'avant': tout(lambda a: P(a, 4, 0, (90, 170, 255)), lambda a: P(a, 4, 1, (60, 120, 230)))})
    h.c((-4, -14, -4), (8, 3, 8), tissu(NUIT, motif='etoiles', mc=OR2, dens=.06))
    h.c((-3, -17, -3), (6, 3, 6), tissu(NUIT, motif='etoiles', mc=OR2, dens=.06))
    pt = h.os('pointe', (0, -17, 0), (-.35, 0, .12))
    pt.c((-2, -3, -2), (4, 3, 4), tissu(NUIT)).c((-1, -6, -1), (2, 3, 2), tissu(NUIT)).c((-1, -7, -1), (2, 1, 2), gemme(OR2), lum=True)
    b = B['body']
    b.c((-5, -1, -3), (10, 3, 6), bandes((2, tissu((30, 32, 86))), (None, liser)), deco={'avant': lambda a: (P(a, 4, 1, (90, 170, 255)), P(a, 5, 1, (60, 120, 230)))})
    ceinture(B, cuir((70, 40, 90)), boucle=boucle(OR2, (90, 170, 255)))
    jupe(B, bandes((9, robe), (1, liser), (None, robe)), deco={'avant': ligne_v(OR2, 4, 1, 0, 9)}, dechire=2)
    epaulettes(B, bandes((2, tissu((30, 32, 86))), (None, liser)), etages=2, m2=bandes((1, liser), (None, tissu((48, 50, 120)))),
               deco={'avant': lambda a: P(a, a.shape[1] // 2, 0, (90, 170, 255))})
    manches_larges(B, bandes((6, robe), (1, liser), (None, robe)), y=3, h=9)
    return B


def enchanteresse():
    pc, yeux, chev = (236, 196, 166), (60, 210, 150), (150, 62, 40)
    em, arg = (28, 124, 88), (214, 222, 232)
    robe = tissu(em, motif='losanges', mc=(120, 220, 170))
    liser = metal(arg)
    B = humanoide(tete(pc, chev, 2, 6), visage(yeux, pc, bouche=(196, 96, 110), cils=True),
                  bandes((1, liser), (None, robe)), {'avant': tout(ligne_v(arg, 3, 2, 0, 4), etoile((120, 255, 190), 3, 6, False))},
                  manches(robe, pc, liser), jambes(robe, cuir((40, 70, 50))))
    h = B['head']
    h.c((-4, -7, 4), (8, 13, 1), cheveux(chev), deco={'dos': lambda a: trou(a, 0, 11, 1, 13) or trou(a, 7, 11, 8, 13)})   # longue chevelure
    h.c((-5, -6, -3), (1, 9, 3), cheveux(chev)).c((4, -6, -3), (1, 9, 3), cheveux(chev))                              # meches
    h.c((-4.5, -7.5, -4.5), (9, 1, 9), liser)                                                                          # diademe
    h.c((-1, -9, -5), (2, 2, 1), gemme((80, 255, 170)), lum=True)
    h.c((-2.5, -8.5, -4.8), (5, 1, 1), liser)
    b = B['body']
    b.c((-4.5, -0.5, -2.5), (9, 2, 5), bandes((1, tissu((20, 90, 64))), (None, liser)))
    ceinture(B, metal(arg), y=9, boucle=boucle((250, 250, 255), (80, 255, 170)))
    b.c((-3, 11, -3.5), (1, 2, 1), verre((240, 90, 170))).c((2, 11, -3.5), (1, 2, 1), verre((90, 160, 255))).c((-1, 11, -3.5), (1, 1, 1), verre((250, 220, 80)))
    jupe(B, bandes((10, robe), (1, liser), (None, robe)), y=11, h=13, deco={'avant': ligne_v((120, 220, 170), 4, 1, 0, 10)}, dechire=2)
    manches_larges(B, bandes((5, robe), (1, liser), (None, tissu((20, 90, 64)))), y=4, h=7, dechire=2)
    epaulettes(B, bandes((1, liser), (None, tissu((20, 90, 64)))), h=2)
    for bras in ('right_arm', 'left_arm'): B[bras].c((-3.5 if bras == 'right_arm' else -1.5, 8, -2.5), (5, 1, 5), dore())
    return B


def maitre_forge():
    pc, yeux, chev = (210, 150, 110), (100, 64, 34), (204, 88, 36)
    chemise, tablier, acier = (126, 84, 60), (110, 72, 40), (150, 156, 168)
    B = humanoide(tete(pc, chev, 2, 4), visage(yeux, pc, bouche=(170, 84, 70), taches=True),
                  bandes((None, tissu(chemise, motif='rayures', mc=(100, 64, 44)))), None,
                  manches(tissu(chemise), pc, n_main=3), jambes(tissu((60, 52, 50)), cuir((50, 36, 26)), 3))
    h = B['head']
    h.c((-4.5, -6.5, -4.5), (9, 1, 9), cuir((70, 48, 30), False))                                         # sangle des lunettes
    lunette = tout(lambda a: P(a, 0, 0, (220, 180, 80)), lambda a: P(a, 1, 1, (220, 180, 80)), lambda a: P(a, 1, 0, (170, 230, 255)), lambda a: P(a, 0, 1, (120, 190, 230)))
    h.c((-3.5, -7.5, -5), (2, 2, 1), dore(), deco={'avant': lunette}).c((1.5, -7.5, -5), (2, 2, 1), dore(), deco={'avant': lunette})
    h.c((-1, -4, 4), (2, 10, 1), cheveux(chev), deco={'dos': lambda a: [P(a, x, y, (220, 180, 80)) for x in (0, 1) for y in (8,)]})  # tresse
    b = B['body']
    def tablier_d(a):
        a[4:8, 2:6, :3] *= .82                                                       # poche
        for x in range(2, 6): P(a, x, 4, clair(tablier, 1.3))
        for x, y in ((1, 9), (5, 11), (6, 2), (2, 13)): P(a, x, y, (60, 50, 46))     # traces de suie
        P(a, 0, 0, (220, 180, 80)); P(a, 7, 0, (220, 180, 80))                        # rivets
    b.c((-4, 1, -3), (8, 16, 1), cuir(tablier), deco={'avant': tablier_d})
    b.c((-4.5, 9, -2.5), (9, 2, 5), cuir((60, 40, 26)))
    b.c((4.5, 9, -1), (1, 5, 2), metal(acier))                                       # pinces a la ceinture
    b.c((-5.5, 9, -1), (1, 4, 2), bois((120, 80, 40), 'v'))                           # manche de marteau
    epaulettes(B, metal(acier, rivets=True), etages=2, m2=metal(clair(acier, 1.1), rivets=True), deco={'haut': lambda a: [P(a, x, 0, OR_C) for x in range(a.shape[1])]})
    poignets(B, cuir((90, 60, 36)), y=5, h=6, deco={'avant': lambda a: [P(a, x, 1, (150, 110, 70)) for x in range(a.shape[1])]})
    for j in ('right_leg', 'left_leg'):
        B[j].c((-2.5, 8, -2.5), (5, 4, 5), cuir((56, 40, 28))).c((-2.5, 10, -3.5), (5, 2, 1), metal(acier))
    return B


def maitre_metiers():
    pc, yeux = (204, 152, 118), (70, 110, 160)
    rouge = (150, 30, 34)
    manteau = tissu(rouge, motif='losanges', mc=(190, 60, 60))
    B = humanoide(tete(pc, (170, 170, 176), 1, 4), visage(yeux, pc, sourcils=(200, 200, 205), rides=True),
                  bandes((None, manteau)), {'avant': tout(ligne_v(OR2, 3, 2), lambda a: [P(a, 5, y, (250, 220, 110)) for y in (3, 6, 9)])},
                  manches(manteau, pc, dore()), jambes(tissu((70, 50, 40)), cuir((60, 36, 24))))
    h = B['head']
    def barbe_d(a): trou(a, 2, 0, 4, 1); trou(a, 0, 3, 1, 4); trou(a, 5, 3, 6, 4)
    h.c((-3, -2, -5), (6, 4, 1), cheveux((200, 200, 206)), deco={'avant': barbe_d}, contour=False)
    h.c((-4.5, -9, -4.5), (9, 2, 9), tissu(rouge, plis=False), deco={'avant': lambda a: [P(a, x, 1, OR_C) for x in range(a.shape[1])]})
    h.c((-3.5, -10, -3.5), (7, 1, 7), tissu(clair(rouge, .85), plis=False))
    pl = h.os('plume', (3, -10, 1), (0, 0, -.45))
    pl.c((0, -7, 0), (1, 7, 1), bandes((5, uni((240, 240, 240))), (None, dore())))
    b = B['body']
    b.c((-5, -1, -3), (10, 3, 6), fourrure((236, 232, 224)))                         # col de fourrure
    ceinture(B, cuir((60, 36, 24)), boucle=boucle(OR2))
    jupe(B, bandes((5, manteau), (None, fourrure((236, 232, 224)))), h=7, deco={'avant': ligne_v((90, 18, 20), 4, 1)})
    poignets(B, fourrure((236, 232, 224)), y=7, h=2)
    b.c((-3, 3, -2.5), (2, 2, 1), dore(), deco={'avant': lambda a: P(a, 0, 0, (255, 80, 80))})   # medaille
    return B


def marchand():
    pc, yeux = (204, 150, 104), (70, 46, 26)
    gilet, chemise = (110, 28, 40), (232, 220, 190)
    B = humanoide(tete(pc, (40, 28, 20), 1, 3), visage(yeux, pc, sourcils=(40, 28, 20)),
                  bandes((None, tissu(chemise))), None,
                  manches(tissu(chemise), pc, dore()), jambes(tissu((40, 90, 70), motif='rayures', mc=(60, 120, 90)), cuir((90, 50, 30)), 3))
    h = B['head']
    h.c((-3, -3, -4.5), (6, 1, 1), cheveux((40, 28, 20)), deco={'avant': lambda a: trou(a, 2, 0, 4, 1)})   # moustache
    turban = tissu((236, 226, 196), motif='rayures', mc=(190, 40, 40))
    h.c((-4.5, -9, -4.5), (9, 3, 9), turban).c((-3.5, -11, -3.5), (7, 2, 7), turban)
    h.c((-1, -10, -5), (2, 2, 1), gemme((60, 200, 120)), lum=True)
    pl = h.os('plume', (0, -11, -3), (-.3, 0, .25))
    pl.c((-0.5, -6, 0), (1, 6, 1), bandes((4, uni((60, 160, 220))), (None, uni((30, 90, 160)))))
    b = B['body']
    def gilet_d(a): trou(a, 3, 0, 5, 8); [P(a, x, y, OR2) for x in (2, 5) for y in range(8)]
    b.c((-4.5, -0.5, -2.5), (9, 8, 5), tissu(gilet, motif='losanges', mc=(160, 60, 70)), deco={'avant': gilet_d})
    b.c((-4.5, 7.5, -2.5), (9, 2, 5), tissu((200, 40, 40)), deco={'avant': boucle(OR2)})
    b.c((2, 9.5, -3.5), (2, 2, 1), cuir((110, 70, 36), False), deco={'avant': lambda a: P(a, 1, 1, (255, 220, 80))})   # bourse
    b.c((-3.5, 1, 2.5), (7, 8, 3), cuir((120, 80, 44)), deco={'dos': tout(ligne_v((80, 50, 26), 1), ligne_v((80, 50, 26), 5))})   # sac
    b.c((-4.5, -0.5, 3), (9, 2, 2), tissu((170, 40, 40)))                            # couverture roulee
    for j in ('right_leg', 'left_leg'): B[j].c((-2.5, 10, -3), (5, 2, 5), cuir((110, 60, 30)), deco={'avant': lambda a: P(a, 2, 0, OR2)})
    return B


def heraut():
    pc, yeux = (226, 182, 150), (60, 140, 80)
    pourpre = (88, 36, 140)
    mailles = tissu((150, 154, 166), motif='mailles', plis=False)
    B = humanoide(tete(pc, (60, 40, 26), 2, 4), visage(yeux, pc),
                  mailles, None, manches(mailles, (240, 240, 240), n_main=3), jambes(tissu(pourpre), cuir((40, 30, 50))))
    h = B['head']
    h.c((-4.5, -9, -4.5), (9, 2, 9), tissu(pourpre, plis=False), deco={'avant': lambda a: [P(a, x, 1, OR_C) for x in range(a.shape[1])]})
    h.c((-1, -10, -5), (2, 2, 1), dore(), deco={'avant': lambda a: P(a, 0, 0, (220, 40, 60))})
    pl = h.os('plume', (2, -9, 2), (.3, 0, -.4))
    pl.c((0, -7, 0), (1, 7, 2), bandes((3, uni((250, 250, 250))), (2, uni((200, 60, 200))), (None, uni((250, 250, 250)))))
    tab = tissu(pourpre, motif='losanges', mc=(130, 70, 190))
    def embleme(a):
        for y in range(a.shape[0]): P(a, 0, y, OR_C); P(a, a.shape[1] - 1, y, OR_C)
        etoile(OR2, 3, 5)(a)
    b = B['body']
    b.c((-3.5, 1, -3), (7, 15, 1), bandes((13, tab), (None, dore())), deco={'avant': embleme})
    b.c((-3.5, 1, 2), (7, 15, 1), bandes((13, tab), (None, dore())), deco={'dos': embleme})
    ceinture(B, dore(), boucle=boucle((255, 240, 170), (220, 40, 60)))
    b.c((4.5, 8, -1), (1, 3, 2), dore())                                             # cor du heraut
    epaulettes(B, dore(True), etages=2, m2=bandes((1, dore()), (None, tissu(pourpre))))
    return B


def commissaire():
    pc, yeux = (230, 190, 160), (60, 110, 170)
    sarc, noir = (24, 116, 116), (26, 26, 32)
    def monocle(a): P(a, 5, 3, OR2); P(a, 6, 3, OR2); P(a, 7, 4, OR2); P(a, 5, 4, (190, 230, 250)); P(a, 7, 5, OR2); P(a, 7, 6, OR2)
    B = humanoide(tete(pc, (30, 30, 34), 1, 4), tout(visage(yeux, pc, sourcils=(30, 30, 34)), monocle),
                  bandes((None, tissu(sarc))), {'avant': ligne_v((236, 236, 240), 3, 2, 0, 7)},
                  manches(tissu(sarc), (245, 245, 245), metal((236, 236, 240))), jambes(tissu((50, 50, 58), motif='rayures', mc=(80, 80, 90)), metal(noir), 2))
    h = B['head']
    h.c((-3, -3, -4.5), (6, 1, 1), cheveux((30, 30, 34)), deco={'avant': lambda a: trou(a, 2, 0, 4, 1)})
    h.c((-5, -9, -5), (10, 1, 10), tissu(noir, plis=False))                        # haut-de-forme
    h.c((-4, -16, -4), (8, 7, 8), bandes((4, tissu(noir)), (2, tissu(sarc)), (None, tissu(noir))))
    b = B['body']
    b.c((-1.5, -0.5, -3), (3, 1, 1), tissu((180, 30, 40)), deco={'avant': lambda a: P(a, 1, 0, (120, 20, 30))})   # noeud papillon
    b.c((-4.5, -0.5, -2.5), (9, 1, 5), tissu(clair(sarc, .8)))                      # revers
    jupe(B, bandes((5, tissu(sarc)), (None, dore())), h=6, deco={'avant': tout(lambda a: trou(a, 3, 0, 6, 6))})
    b.c((2, 6, -2.5), (1, 1, 1), dore())                                             # chaine de montre
    ceinture(B, cuir(noir), y=10, boucle=boucle(OR2))
    return B


def guide():
    pc, yeux = (200, 156, 120), (120, 200, 255)
    cape, cuir_c = (40, 66, 140), (110, 74, 40)
    B = humanoide(tete(pc, (200, 180, 120), 2, 4), visage(yeux, pc),
                  bandes((None, tissu((70, 110, 90)))), None,
                  manches(tissu((70, 110, 90)), pc, cuir(cuir_c, False)), jambes(tissu((90, 76, 56)), cuir((80, 52, 30)), 5))
    h = B['head']
    def capuche_avant(a): trou(a, 1, 2, 8, 9)
    h.c((-4.5, -8.5, -4.5), (9, 9, 9), tissu(cape, motif='etoiles', mc=(200, 210, 240), dens=.02), deco={'avant': capuche_avant, 'bas': lambda a: trou(a, 1, 1, 8, 8)})
    h.c((-2, -7, 4.5), (4, 4, 2), tissu(cape))                                       # pointe de la capuche
    b = B['body']
    b.c((-5, -0.5, -3), (10, 4, 6), tissu(clair(cape, .9)), deco={'avant': lambda a: (P(a, 4, 1, (230, 230, 240)), P(a, 5, 1, (200, 200, 210)))})
    b.c((-4.5, 0, 2.5), (9, 22, 1), bandes((19, tissu(cape, motif='etoiles', mc=(200, 210, 240), dens=.02)), (1, metal((200, 205, 215))), (None, tissu(cape))), dechire=2)
    ceinture(B, cuir(cuir_c), y=9, boucle=boucle((200, 200, 210)))
    b.c((-4.5, 10.5, -3.5), (2, 2, 1), cuir((90, 60, 30), False))                   # sacoche
    b.c((3.5, 10, -2), (2, 3, 2), bandes((1, dore()), (1, gemme((255, 220, 120))), (None, dore())), lum=True)   # lanterne
    b.c((-1.5, 2, 3.5), (3, 9, 1), bois((200, 180, 140), 'v', 3))                     # carte roulee dans le dos
    for j in ('right_leg', 'left_leg'): B[j].c((-2.5, 5, -2.5), (5, 7, 5), cuir((80, 52, 30)), deco={'avant': lambda a: [P(a, x, 1, (150, 110, 70)) for x in range(a.shape[1])]})
    return B


PNJ = {'marchand': marchand, 'maitre_metiers': maitre_metiers, 'heraut': heraut, 'commissaire': commissaire, 'guide': guide,
       'archimage': archimage, 'maitre_forge': maitre_forge, 'enchanteresse': enchanteresse}


# ====================================================================== objets 3D (modeles JSON Java)
FACES_JSON = {'haut': 'up', 'bas': 'down', 'avant': 'north', 'dos': 'south', 'droite': 'west', 'gauche': 'east'}


def modele_json(cubes, tex, larg=64, parent=None, display=None):
    pos, img = atlas(cubes, larg)
    H, W = img.shape[:2]
    els = []
    for i, c in enumerate(cubes):
        fa = {}
        for f, (x, y, w, h) in c.faces(*pos[i]).items():
            uv = [x * 16 / W, y * 16 / H, (x + w) * 16 / W, (y + h) * 16 / H]
            fa[FACES_JSON[f]] = {'uv': [round(v, 4) for v in uv], 'texture': '#t'}
        e = {'from': list(c.mn), 'to': [c.mn[k] + c.t[k] for k in range(3)], 'faces': fa}
        if c.lum: e['shade'] = False; e['forge_data'] = {'block_light': 15, 'sky_light': 15}
        els.append(e)
    m = {'textures': {'t': tex, 'particle': tex}, 'elements': els}
    if parent: m = {'parent': parent, **m}
    if display: m['display'] = display
    return m, img


# axes des cubes d'objet : x, y (haut), z ; « avant » = face nord
TENU = {'thirdperson_righthand': {'rotation': [90, 90, 0], 'translation': [0, 1, 4], 'scale': [.85, .85, .85]},
        'thirdperson_lefthand': {'rotation': [90, 90, 0], 'translation': [0, 1, 4], 'scale': [.85, .85, .85]},
        'firstperson_righthand': {'rotation': [0, -60, 20], 'translation': [1.13, 1.5, 1.13], 'scale': [.42, .42, .42]},
        'firstperson_lefthand': {'rotation': [0, -60, 20], 'translation': [1.13, 1.5, 1.13], 'scale': [.42, .42, .42]},
        'gui': {'rotation': [25, -35, -40], 'translation': [0, 0, 0], 'scale': [.62, .62, .62]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.4, .4, .4]},
        'fixed': {'rotation': [0, 0, -45], 'translation': [0, 0, 0], 'scale': [.6, .6, .6]},
        'head': {'rotation': [0, 0, 0], 'translation': [0, 8, 0], 'scale': [.8, .8, .8]}}
PETIT = {'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.45, .45, .45]},
         'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.45, .45, .45]},
         'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
         'firstperson_lefthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
         'gui': {'rotation': [25, 35, 0], 'translation': [0, 1, 0], 'scale': [.9, .9, .9]},
         'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
         'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [.9, .9, .9]}}


def C(mn, t, m, **k): return Cube(mn, t, m, **k)


def fut(m, y0, y1, x=7.5, larg=1): return C((x, y0, x), (larg, y1 - y0, larg), m)


def bague(m, y, h=1, larg=2): return C((8 - larg / 2, y, 8 - larg / 2), (larg, h, larg), m)


def baton_foudre():
    bleu = (90, 180, 255)
    l = [fut(bois((60, 40, 70), 'v', 30), -6, 19), bague(dore(), -6), bague(cuir((90, 50, 30)), 1, 5), bague(dore(), 10), bague(dore(), 6),
         C((6.5, 18, 6.5), (3, 1, 3), dore())]
    for mn in ((6, 19, 7.5), (9, 19, 7.5), (7.5, 19, 6), (7.5, 19, 9)): l.append(C(mn, (1, 4, 1), dore()))
    l += [C((6.5, 19.5, 6.5), (3, 3, 3), gemme(bleu), lum=True), C((7.5, 23, 7.5), (1, 1, 1), gemme((220, 240, 255)), lum=True)]
    return l, TENU


def sceptre_givre():
    glace = (150, 220, 255)
    l = [fut(metal((190, 205, 225)), -5, 17), bague(dore(), -5), bague(metal((120, 140, 170)), 2, 4), bague(dore(), 9),
         C((6.5, 17, 6.5), (3, 1, 3), metal((210, 225, 240))),
         C((7, 18, 7), (2, 6, 2), gemme(glace), lum=True), C((5.5, 18, 7.5), (1, 4, 1), gemme(glace), lum=True), C((9.5, 18, 7.5), (1, 4, 1), gemme(glace), lum=True),
         C((7.5, 18, 5.5), (1, 3, 1), gemme(glace), lum=True), C((7.5, 18, 9.5), (1, 3, 1), gemme(glace), lum=True), C((7.5, 24, 7.5), (1, 1, 1), gemme((240, 250, 255)), lum=True)]
    return l, TENU


def baguette_soins():
    l = [fut(bois((236, 226, 200), 'v', 30), -2, 13), bague(dore(), -2), bague(dore(), 4), bague(dore(), 8),
         C((7, 13, 7), (2, 1, 2), dore()),
         C((7, 14, 7), (2, 3, 2), gemme((255, 120, 170)), lum=True),
         C((6, 15, 7.5), (1, 2, 1), uni((90, 200, 110))), C((9, 15, 7.5), (1, 2, 1), uni((90, 200, 110))),
         C((7.5, 17, 7.5), (1, 1, 1), gemme((255, 220, 240)), lum=True)]
    return l, TENU


def baton_braise():
    braise = (255, 120, 30)
    l = [fut(bois((40, 30, 28), 'v', 30), -6, 17), bague(metal((80, 76, 80)), -6), bague(metal((80, 76, 80)), 8), bague(cuir((60, 30, 20)), 1, 5),
         C((6, 17, 6), (4, 1, 4), metal((70, 66, 70)))]
    for mn in ((6, 18, 6), (9, 18, 6), (6, 18, 9), (9, 18, 9)): l.append(C(mn, (1, 4, 1), metal((70, 66, 70))))
    l += [C((6.5, 18, 6.5), (3, 3, 3), gemme(braise), lum=True), C((7, 21, 7), (2, 2, 2), gemme((255, 200, 60)), lum=True),
          C((6, 22, 6), (4, 1, 4), metal((70, 66, 70)))]
    return l, TENU


def marteau_forgeron():
    acier = (150, 156, 168)
    l = [fut(bois((110, 72, 40), 'v', 30), -5, 14), C((7, -6, 7), (2, 1, 2), dore()), bague(cuir((70, 40, 24)), -3, 6),
         C((4, 14, 6), (8, 5, 4), metal(acier, rivets=True)), C((3, 14.5, 6.5), (1, 4, 3), metal(clair(acier, .8))), C((12, 14.5, 6.5), (1, 4, 3), metal(clair(acier, .8))),
         C((4, 16, 5.5), (8, 1, 5), dore()), C((7, 15.5, 5.2), (2, 2, 1), gemme((255, 120, 40)), lum=True)]
    return l, TENU


def hache_titan():
    l = [fut(bois((80, 60, 50), 'v', 30), -5, 19), C((7, -6, 7), (2, 1, 2), dore()), bague(cuir((60, 40, 30)), -2, 7),
         C((7, 9, 7), (2, 10, 2), metal((90, 80, 76), rivets=True)),
         C((9, 9, 7.5), (5, 10, 1), metal((130, 120, 116))), C((14, 8, 7.5), (1, 12, 1), metal((210, 210, 220))),
         C((5, 13, 7.5), (2, 3, 1), metal((110, 100, 96))), C((7, 13, 6.5), (2, 2, 1), gemme((255, 90, 50)), lum=True),
         C((7.5, 19, 7.5), (1, 2, 1), metal((210, 210, 220)))]
    return l, TENU


def epee_runique():
    lame = (180, 200, 230)
    def runes(a):
        for y in range(1, a.shape[0] - 1, 2): P(a, 0, y, (120, 220, 255))
    l = [C((7, 4, 7.5), (2, 18, 1), metal(lame), deco={'avant': runes, 'dos': runes}, lum=False), C((7.5, 22, 7.5), (1, 2, 1), metal(lame)),
         C((4, 3, 7), (8, 1, 2), dore()), C((3, 2.5, 7), (1, 2, 2), dore()), C((12, 2.5, 7), (1, 2, 2), dore()),
         C((7, 3, 6.5), (2, 2, 3), gemme((90, 200, 255)), lum=True),
         fut(cuir((50, 30, 60)), -2, 3), C((7, -3, 7), (2, 1, 2), gemme((90, 200, 255)), lum=True)]
    return l, TENU


def orbe_transfert():
    v = (150, 80, 230)
    l = [C((5, 0, 5), (6, 1, 6), dore(True)), C((7, 1, 7), (2, 3, 2), dore()), C((6, 4, 6), (4, 1, 4), dore()),
         C((5, 6, 6), (6, 4, 4), gemme(v), lum=True), C((6, 5, 6), (4, 6, 4), gemme(v), lum=True), C((6, 6, 5), (4, 4, 6), gemme(v), lum=True),
         C((7, 7, 4.6), (2, 2, 1), gemme((240, 200, 255)), lum=True)]
    return l, PETIT


def elixir(liquide):
    def f():
        verre_c = (220, 236, 245)
        def etiquette(a):
            a[2:5, 1:5, :3] = (236, 222, 180); P(a, 2, 3, clair(liquide, .7)); P(a, 3, 3, clair(liquide, .7))
        l = [C((5, 0, 5), (6, 6, 6), verre(liquide), deco={'avant': etiquette}), C((5.5, 6, 5.5), (5, 1, 5), verre(verre_c)),
             C((6.5, 7, 6.5), (3, 2, 3), verre(verre_c)), C((6, 8, 6), (4, 1, 4), dore()), C((7, 9, 7), (2, 2, 2), bois((150, 110, 70), 'v', 9)),
             C((6.5, 11, 6.5), (3, 1, 3), uni(clair(liquide, .8)))]
        return l, PETIT
    return f


RARETES = {'commun': ((126, 86, 50), (146, 146, 156), None), 'rare': ((70, 52, 42), (70, 124, 210), (90, 170, 255)),
           'epique': ((64, 38, 82), (150, 92, 214), (210, 130, 255)), 'legendaire': ((96, 44, 32), OR_C, (255, 196, 70)),
           'special': ((36, 26, 58), (255, 80, 200), (255, 140, 235))}


def coffre(r):
    """Coffre en deux parties : le corps et le couvercle (charniere a l'arriere, y=9 z=14) pour l'animation d'ouverture."""
    def f():
        bois_c, metal_c, gem = RARETES[r]
        mm = dore(True) if r == 'legendaire' else metal(metal_c, rivets=True)
        def serrure(a):
            P(a, 1, 0, (20, 16, 20)); P(a, 1, 1, (20, 16, 20)); P(a, 0, 0, clair(metal_c, 1.4)); P(a, 2, 2, clair(metal_c, 1.4))
        corps = [C((1, 1, 2), (14, 8, 12), bois(bois_c, 'h', 3)),
                 C((3, 0.5, 1.5), (2, 9, 13), mm), C((11, 0.5, 1.5), (2, 9, 13), mm), C((0.5, 7.5, 1.5), (15, 1, 13), mm),
                 C((6.5, 5.5, 1), (3, 3, 1), dore(), deco={'avant': serrure})]
        couv = [C((1, 9, 2), (14, 5, 12), bois(clair(bois_c, 1.1), 'h', 3)), C((1.5, 14, 2.5), (13, 1, 11), bois(clair(bois_c, 1.2), 'v', 4)),
                C((3, 9.5, 1.5), (2, 5, 13), mm), C((11, 9.5, 1.5), (2, 5, 13), mm), C((6.5, 9.5, 1), (3, 2, 1), dore())]
        for x in (1, 13):
            for z in (2.5, 11.5): corps.append(C((x, 0, z), (2, 1, 2), mm))   # pieds
        if gem:
            couv.append(C((7, 15, 7), (2, 1, 2), gemme(gem), lum=True))
            couv.append(C((7.5, 10.5, 0.6), (1, 1, 1), gemme(gem), lum=True))
        if r in ('legendaire', 'special'):
            for x in (0.5, 13.5):
                for z in (1.5, 12.5): corps.append(C((x, 0.5, z), (2, 2, 2), dore())); couv.append(C((x, 12.5, z), (2, 2, 2), dore()))
        if r == 'legendaire':
            couv += [C((5, 15, 6), (6, 1, 4), dore()), C((6, 16, 7), (4, 1, 2), dore()), C((7.5, 17, 7.5), (1, 1, 1), gemme(gem), lum=True)]
        if r == 'special':
            couv += [C((4, 15, 5), (2, 4, 2), gemme(gem), lum=True), C((9, 15, 8), (2, 5, 2), gemme(gem), lum=True), C((6.5, 15, 9.5), (1, 3, 1), gemme((255, 220, 250)), lum=True),
                     C((10.5, 15, 4.5), (1, 2, 1), gemme(gem), lum=True)]
        for c in couv: c.couvercle = True
        return corps + couv, None
    return f


CLE = {'thirdperson_righthand': {'rotation': [90, 90, 0], 'translation': [0, 0, 2], 'scale': [.55, .55, .55]},
       'thirdperson_lefthand': {'rotation': [90, 90, 0], 'translation': [0, 0, 2], 'scale': [.55, .55, .55]},
       'firstperson_righthand': {'rotation': [0, -60, 20], 'translation': [1.13, 2, 1.13], 'scale': [.5, .5, .5]},
       'firstperson_lefthand': {'rotation': [0, -60, 20], 'translation': [1.13, 2, 1.13], 'scale': [.5, .5, .5]},
       'gui': {'rotation': [15, -25, -40], 'translation': [0, 0, 0], 'scale': [.95, .95, .95]},
       'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.5, .5, .5]},
       'fixed': {'rotation': [0, 0, -45], 'translation': [0, 0, 0], 'scale': [.9, .9, .9]}}


def cle(r):
    """Cle de coffre : anneau ouvrage avec gemme, tige et panneton, aux couleurs de la rarete."""
    def f():
        _, metal_c, gem = RARETES[r]
        mm = dore(True) if r == 'legendaire' else metal(clair(metal_c, 1.1))
        g = gem or (200, 210, 225)
        l = [C((7.5, 1, 7.5), (1, 8, 1), mm), C((7, 8, 7), (2, 1, 2), dore() if r != 'legendaire' else metal((250, 230, 150))),
             C((8.5, 1, 7.5), (2, 1, 1), mm), C((8.5, 3, 7.5), (1, 1, 1), mm), C((8.5, 5, 7.5), (2, 1, 1), mm),   # panneton
             C((5.5, 9, 7.5), (5, 1, 1), mm), C((5.5, 14, 7.5), (5, 1, 1), mm), C((5, 10, 7.5), (1, 4, 1), mm), C((10, 10, 7.5), (1, 4, 1), mm),   # anneau
             C((7, 11, 7), (2, 2, 2), gemme(g), lum=True), C((7.5, 15, 7.5), (1, 1, 1), gemme(g), lum=True)]
        if r in ('epique', 'legendaire', 'special'):
            l += [C((4, 11, 7.5), (1, 2, 1), mm), C((11, 11, 7.5), (1, 2, 1), mm)]   # ailettes
        if r == 'special':
            l += [C((7.5, 16, 7.5), (1, 1, 1), gemme((255, 230, 250)), lum=True), C((3, 11.5, 7.5), (1, 1, 1), gemme(g), lum=True), C((12, 11.5, 7.5), (1, 1, 1), gemme(g), lum=True)]
        return l, CLE
    return f


def caisse():
    vio, orc = (54, 30, 86), OR_C
    def runes(a):
        for y in range(2, a.shape[0] - 2, 3):
            for x in range(2, a.shape[1] - 2, 3): P(a, x, y, (200, 140, 255)); P(a, x + 1, y, (150, 90, 230))
    l = [C((1, 1, 1), (14, 14, 14), bois(vio, 'h', 4), deco={k: runes for k in ('avant', 'dos', 'droite', 'gauche')})]
    for x in (0, 14):
        for z in (0, 14): l.append(C((x, 0, z), (2, 16, 2), dore(True)))
    for y in (0, 14):
        l += [C((2, y, 0), (12, 2, 2), dore()), C((2, y, 14), (12, 2, 2), dore()), C((0, y, 2), (2, 2, 12), dore()), C((14, y, 2), (2, 2, 12), dore())]
    l += [C((6, 6, 0.5), (4, 4, 1), gemme((200, 120, 255)), lum=True), C((6, 6, 14.5), (4, 4, 1), gemme((200, 120, 255)), lum=True),
          C((0.5, 6, 6), (1, 4, 4), gemme((200, 120, 255)), lum=True), C((14.5, 6, 6), (1, 4, 4), gemme((200, 120, 255)), lum=True),
          C((5, 16, 5), (6, 1, 6), dore()), C((6.5, 17, 6.5), (3, 3, 3), gemme((230, 170, 255)), lum=True)]
    return l, None


OBJETS = {'baton_foudre': baton_foudre, 'sceptre_givre': sceptre_givre, 'baguette_soins': baguette_soins, 'baton_braise': baton_braise,
          'marteau_forgeron': marteau_forgeron, 'hache_titan': hache_titan, 'epee_runique': epee_runique, 'orbe_transfert': orbe_transfert,
          'elixir_force': elixir((210, 40, 40)), 'elixir_titan': elixir((140, 130, 120)), 'elixir_ombre': elixir((50, 30, 90)),
          'elixir_celerite': elixir((90, 190, 255)), 'elixir_mineur': elixir((240, 190, 40)), 'elixir_phenix': elixir((255, 110, 30)),
          'elixir_vie': elixir((240, 80, 180))}
OBJETS.update({'coffre_' + r: coffre(r) for r in RARETES})
OBJETS.update({'cle_' + r: cle(r) for r in RARETES})


# ====================================================================== principal
if __name__ == '__main__':
    mod, bb = sys.argv[1], sys.argv[2]
    apercus = sys.argv[3] if len(sys.argv) > 3 else None
    A = f'{mod}/src/main/resources/assets/astralyscarte'
    for d in (f'{A}/textures/entity/pnj', f'{A}/textures/item/3d', f'{A}/textures/block', f'{bb}/pnj', f'{bb}/objets'):
        os.makedirs(d, exist_ok=True)
    java = []
    for nom, f in PNJ.items():
        B = f()
        racine = [B[k] for k in ('head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg')]
        cubes = [c for o in racine for c in o.tous()]
        pos, img = atlas(cubes, 128, 128)
        pos_de = {id(c): pos[i] for i, c in enumerate(cubes)}
        Image.fromarray(img, 'RGBA').save(f'{A}/textures/entity/pnj/{nom}.png')
        Image.fromarray(img, 'RGBA').save(f'{bb}/pnj/{nom}.png')
        json.dump(bbmodel_entite(nom, racine, pos_de, img), open(f'{bb}/pnj/{nom}.bbmodel', 'w'))
        java.append(java_modele(nom, racine, pos_de, 128, 128))
    noms = list(PNJ)
    src = ('package fr.astralys.carte;\n\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.CubeListBuilder;\n'
           'import net.minecraft.client.model.geom.builders.LayerDefinition;\nimport net.minecraft.client.model.geom.builders.MeshDefinition;\n'
           'import net.minecraft.client.model.geom.builders.PartDefinition;\n\n'
           '/** Geometrie des PNJ. FICHIER GENERE par outils_modeles.py : ne pas modifier a la main. */\nfinal class PnjModeles {\n'
           '    static LayerDefinition couche(String skin) {\n        return switch (skin) {\n'
           + ''.join(f'            case "{n}" -> {n}();\n' for n in noms)
           + '            default -> throw new IllegalArgumentException(skin);\n        };\n    }\n\n' + '\n'.join(java) + '\n    private PnjModeles() {}\n}\n')
    open(f'{mod}/src/main/java/fr/astralys/carte/PnjModeles.java', 'w').write(src)

    planche = []
    for nom, f in OBJETS.items():
        cubes, disp = f()
        m, img = modele_json(cubes, f'astralyscarte:item/3d/{nom}', 64, parent=None if disp else 'minecraft:block/block', display=disp)
        Image.fromarray(img, 'RGBA').save(f'{A}/textures/item/3d/{nom}.png')
        json.dump(m, open(f'{A}/models/item/{nom}.json', 'w'), indent=1)
        if nom.startswith('coffre_'):   # corps et couvercle a part, pour l'animation d'ouverture
            for part, garder in (('corps', False), ('couvercle', True)):
                els = [e for e, c in zip(m['elements'], cubes) if getattr(c, 'couvercle', False) == garder]
                json.dump({'textures': m['textures'], 'elements': els}, open(f'{A}/models/item/{nom}_{part}.json', 'w'), indent=1)
        mb = json.loads(json.dumps(m)); mb['textures'] = {'t': nom, 'particle': nom}
        json.dump(mb, open(f'{bb}/objets/{nom}.json', 'w'), indent=1)
        Image.fromarray(img, 'RGBA').save(f'{bb}/objets/{nom}.png')
        planche.append(img)
    cubes, _ = caisse()
    m, img = modele_json(cubes, 'astralyscarte:block/caisse_astrale_3d', 64, parent='minecraft:block/block')
    Image.fromarray(img, 'RGBA').save(f'{A}/textures/block/caisse_astrale_3d.png')
    json.dump(m, open(f'{A}/models/block/caisse_astrale.json', 'w'), indent=1)
    mb = json.loads(json.dumps(m)); mb['textures'] = {'t': 'caisse_astrale', 'particle': 'caisse_astrale'}
    json.dump(mb, open(f'{bb}/objets/caisse_astrale.json', 'w'), indent=1)
    Image.fromarray(img, 'RGBA').save(f'{bb}/objets/caisse_astrale.png')
    if apercus:
        for nom in PNJ:
            Image.open(f'{A}/textures/entity/pnj/{nom}.png').resize((512, 512), Image.NEAREST).save(f'{apercus}/tex_{nom}.png')
    print('ok', len(PNJ), 'PNJ,', len(OBJETS) + 1, 'modeles 3D')
