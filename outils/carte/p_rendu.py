"""Rendu Blender (Cycles) du monde en blocs. Usage : python p_rendu.py <largeur> <hauteur> <echantillons> <sortie.png>"""
import json, math, sys
import bpy
import numpy as np
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Vector

ICI = '/tmp/claude-0/-home-claude/317e8ecb-ac12-5492-80dc-b9e6c187b40c/scratchpad/'
LW, LH, ECH, SORTIE = int(sys.argv[1]), int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
EMISSION = len(sys.argv) > 5 and sys.argv[5] == 'emission'  # passe des sources de lumiere seules

G = np.load(ICI + 'p_monde.npz')['G'].transpose(1, 0, 2).copy()  # Sylvaria a gauche, Azurea a droite
meta = json.load(open(ICI + 'p_palette.json'))
pal = meta['pal']
NX, NY, NZ = G.shape
rgb = np.array([[0, 0, 0]] + [p['rgb'] for p in pal], np.float32) / 255
lin = rgb ** 2.2
emi = np.array([0] + [p['emi'] for p in pal], np.float32)

solide = G > 0
# variation legere par bloc (aspect "texture")
rng = np.random.default_rng(5)
var = rng.uniform(.9, 1.06, G.shape).astype(np.float32)

quads, cols, emis = [], [], []
# camera du cote +x +y +z : seules ces faces sont visibles
for axe, (dx, dy, dz) in ((0, (1, 0, 0)), (1, (0, 1, 0)), (2, (0, 0, 1))):
    voisin = np.zeros_like(solide)
    if axe == 0:
        voisin[:-1] = solide[1:]
    elif axe == 1:
        voisin[:, :-1] = solide[:, 1:]
    else:
        voisin[:, :, :-1] = solide[:, :, 1:]
    x, y, z = np.where(solide & ~voisin)
    if axe == 0:
        v = np.stack([[x + 1, y, z], [x + 1, y + 1, z], [x + 1, y + 1, z + 1], [x + 1, y, z + 1]], 0)
        f = .80
    elif axe == 1:
        v = np.stack([[x, y + 1, z], [x, y + 1, z + 1], [x + 1, y + 1, z + 1], [x + 1, y + 1, z]], 0)
        f = .90
    else:
        v = np.stack([[x, y, z + 1], [x, y + 1, z + 1], [x + 1, y + 1, z + 1], [x + 1, y, z + 1]], 0)
        f = 1.0
    quads.append(np.transpose(v, (2, 0, 1)))       # (n, 4, 3)
    k = G[x, y, z]
    cols.append(lin[k] * var[x, y, z][:, None] * f)
    emis.append(emi[k])
quads = np.concatenate(quads).astype(np.float32)
cols = np.concatenate(cols); emis = np.concatenate(emis)
nf = len(quads)
print('faces', nf)

bpy.ops.wm.read_factory_settings(use_empty=True)
me = bpy.data.meshes.new('monde')
verts = quads.reshape(-1, 3)
me.vertices.add(len(verts))
me.vertices.foreach_set('co', verts.ravel())
me.loops.add(nf * 4)
me.loops.foreach_set('vertex_index', np.arange(nf * 4, dtype=np.int32))
me.polygons.add(nf)
me.polygons.foreach_set('loop_start', np.arange(0, nf * 4, 4, dtype=np.int32))
me.update(calc_edges=True)
ac = me.attributes.new('col', 'FLOAT_VECTOR', 'FACE'); ac.data.foreach_set('vector', cols.ravel())
ae = me.attributes.new('emi', 'FLOAT', 'FACE'); ae.data.foreach_set('value', emis)
ob = bpy.data.objects.new('monde', me)
bpy.context.scene.collection.objects.link(ob)

mat = bpy.data.materials.new('blocs'); mat.use_nodes = True
nt = mat.node_tree; nodes = nt.nodes
bsdf = nodes['Principled BSDF']
a1 = nodes.new('ShaderNodeAttribute'); a1.attribute_name = 'col'
a2 = nodes.new('ShaderNodeAttribute'); a2.attribute_name = 'emi'
nt.links.new(a1.outputs['Vector'], bsdf.inputs['Base Color'])
nt.links.new(a1.outputs['Vector'], bsdf.inputs['Emission Color'])
nt.links.new(a2.outputs['Fac'], bsdf.inputs['Emission Strength'])
bsdf.inputs['Roughness'].default_value = .82
if EMISSION:
    nt.links.remove(bsdf.inputs['Base Color'].links[0]); bsdf.inputs['Base Color'].default_value = (0, 0, 0, 1)
    bsdf.inputs['Roughness'].default_value = 1; bsdf.inputs['Specular IOR Level'].default_value = 0
ob.data.materials.append(mat)

sc = bpy.context.scene
# Mer infinie autour de la carte
pm = bpy.data.meshes.new('mer'); R = 4000; zm = meta['mer'] - .02
pm.from_pydata([(-R, -R, zm), (R, -R, zm), (R, R, zm), (-R, R, zm)], [], [(0, 1, 2, 3)])
po = bpy.data.objects.new('mer', pm); bpy.context.scene.collection.objects.link(po)
mm = bpy.data.materials.new('mer'); mm.use_nodes = True
mb = mm.node_tree.nodes['Principled BSDF']; mb.inputs['Base Color'].default_value = (*lin[meta['idx']['eau']], 1); mb.inputs['Roughness'].default_value = .82
po.data.materials.append(mm)
if EMISSION:
    mb.inputs['Base Color'].default_value = (0, 0, 0, 1)

# Soleil chaud + ciel
sun = bpy.data.lights.new('soleil', 'SUN'); sun.energy = 4.2; sun.angle = math.radians(6); sun.color = (1, .96, .9)
so = bpy.data.objects.new('soleil', sun); sc.collection.objects.link(so)
so.rotation_euler = (math.radians(48), math.radians(0), math.radians(-35))
w = bpy.data.worlds.new('ciel'); sc.world = w; w.use_nodes = True
wn = w.node_tree.nodes; bg = wn['Background']
bg.inputs['Color'].default_value = (.42, .62, .9, 1); bg.inputs['Strength'].default_value = .9
# vu par la camera : la couleur de la mer profonde
lp = wn.new('ShaderNodeLightPath'); bg2 = wn.new('ShaderNodeBackground'); mix = wn.new('ShaderNodeMixShader')
mer = lin[meta['idx']['eau']]
bg2.inputs['Color'].default_value = (*mer, 1); bg2.inputs['Strength'].default_value = 1.0
w.node_tree.links.new(lp.outputs['Is Camera Ray'], mix.inputs['Fac'])
w.node_tree.links.new(bg.outputs['Background'], mix.inputs[1])
w.node_tree.links.new(bg2.outputs['Background'], mix.inputs[2])
w.node_tree.links.new(mix.outputs['Shader'], wn['World Output'].inputs['Surface'])

# Camera isometrique orthographique
cible = Vector((NX / 2 + 2, NY / 2 + 2, 34))
cam = bpy.data.cameras.new('cam'); cam.type = 'ORTHO'; cam.ortho_scale = 300
co = bpy.data.objects.new('cam', cam); sc.collection.objects.link(co)
dirc = Vector((1, 1, 1.18)).normalized()
co.location = cible + dirc * 400
co.rotation_euler = (-dirc).to_track_quat('-Z', 'Y').to_euler()
cam.clip_end = 2000
sc.camera = co
if EMISSION:
    sun.energy = 0; bg.inputs['Strength'].default_value = 0; bg2.inputs['Color'].default_value = (0, 0, 0, 1)

sc.render.engine = 'CYCLES'
sc.cycles.device = 'CPU'
sc.cycles.samples = ECH
sc.cycles.use_adaptive_sampling = True
sc.cycles.use_denoising = True
sc.cycles.max_bounces = 4; sc.cycles.diffuse_bounces = 2; sc.cycles.glossy_bounces = 1; sc.cycles.transparent_max_bounces = 1
sc.render.threads_mode = 'FIXED'; sc.render.threads = 2
sc.render.resolution_x, sc.render.resolution_y = LW, LH
sc.render.resolution_percentage = 100
sc.view_settings.view_transform = 'Standard'
sc.view_settings.look = 'None'
sc.render.image_settings.file_format = 'PNG'
sc.render.filepath = SORTIE

# Position des marqueurs a l'ecran
ancres = {}
bpy.context.view_layer.update()
for nom, (y, x, z) in meta['centres'].items():
    p = world_to_camera_view(sc, co, Vector((x, y, z)))
    ancres[nom] = [p.x * LW, (1 - p.y) * LH]
json.dump(ancres, open(ICI + 'p_ancres.json', 'w'))
print('ancres', ancres)
bpy.ops.render.render(write_still=True)
print('rendu ok', SORTIE)
