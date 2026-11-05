"""Genere distribution.json pour le launcher Astralys avec Nebula (lance par GitHub Actions).

Les mods sont telecharges depuis Modrinth pour calculer leurs empreintes, puis leurs liens
sont remplaces par les liens officiels Modrinth : seuls les fichiers Forge sont publies ici.
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request

ICI = os.path.dirname(os.path.abspath(__file__))
NEBULA = os.environ.get('NEBULA_DIR', '/tmp/nebula')
ROOT = os.environ.get('NEBULA_ROOT', '/tmp/astralys-root')
SORTIE = os.environ.get('SORTIE', '/tmp/publication')
BASE_URL = os.environ['BASE_URL']
SERVEUR = 'astralys'
MC = '1.20.1'
FORGE = '47.4.10'


def nebula(*args):
    subprocess.run(['node', 'dist/index.js', *args], cwd=NEBULA, check=True)


def sha1(path):
    h = hashlib.sha1()
    with open(path, 'rb') as f:
        for bloc in iter(lambda: f.read(1 << 20), b''):
            h.update(bloc)
    return h.hexdigest()


def main():
    java = shutil.which('java')
    with open(os.path.join(NEBULA, '.env'), 'w') as f:
        f.write(f'JAVA_EXECUTABLE={java}\nROOT={ROOT}\nBASE_URL={BASE_URL}\n')

    nebula('init', 'root')
    nebula('g', 'server', SERVEUR, MC, '--forge', FORGE)
    dossier = os.path.join(ROOT, 'servers', f'{SERVEUR}-{MC}')

    # Mods : telechargement + verification de l'empreinte
    mods = json.load(open(os.path.join(ICI, 'mods.json')))['mods']
    requis = os.path.join(dossier, 'forgemods', 'required')
    os.makedirs(requis, exist_ok=True)
    liens = {}
    for m in mods:
        cible = os.path.join(requis, m['file'])
        req = urllib.request.Request(m['url'], headers={'User-Agent': 'astralys-launcher-ci'})
        with urllib.request.urlopen(req) as r, open(cible, 'wb') as f:
            shutil.copyfileobj(r, f)
        if sha1(cible) != m['sha1']:
            sys.exit(f"Empreinte incorrecte pour {m['file']}")
        liens[m['file']] = m['url']
        print('mod OK', m['file'])

    shutil.copy(os.path.join(ICI, 'icon.png'), os.path.join(dossier, 'icon.png'))

    meta_path = os.path.join(dossier, 'servermeta.json')
    meta = json.load(open(meta_path))
    meta['meta'].update(json.load(open(os.path.join(ICI, 'servermeta.json'))))
    meta['meta'].pop('discord', None)
    meta['meta'].pop('icon', None)
    json.dump(meta, open(meta_path, 'w'), indent=2, ensure_ascii=False)

    # L'installateur Forge ouvre une fenetre quand Nebula le lance : on le lance avant, sans fenetre,
    # dans le cache que Nebula reutilise ensuite.
    cache = os.path.join(ROOT, 'cache', 'forge', f'{MC}-{FORGE}')
    os.makedirs(cache, exist_ok=True)
    with open(os.path.join(cache, 'launcher_profiles.json'), 'w') as f:
        f.write('{}')
    installeur = os.path.join(cache, f'forge-{MC}-{FORGE}-installer.jar')
    subprocess.run(['curl', '-fsSL', '-A', 'Mozilla/5.0 astralys-launcher-ci', '-o', installeur,
                    f'https://maven.minecraftforge.net/net/minecraftforge/forge/{MC}-{FORGE}/forge-{MC}-{FORGE}-installer.jar'],
                   check=True)
    subprocess.run([java, '-Djava.awt.headless=true', '-jar', installeur, '--installClient', cache],
                   cwd=cache, check=True)

    nebula('g', 'distro')

    # Liens des mods -> Modrinth (rien n'est redistribue ici)
    distro_path = os.path.join(ROOT, 'distribution.json')
    distro = json.load(open(distro_path))
    vus = set()

    def parcourir(modules):
        for mod in modules:
            nom = urllib.parse.unquote(mod['artifact']['url'].rsplit('/', 1)[-1])
            if mod.get('type') == 'ForgeMod':
                if nom not in liens:
                    sys.exit(f'Mod inattendu dans la distribution : {nom}')
                mod['artifact']['url'] = liens[nom]
                vus.add(nom)
            parcourir(mod.get('subModules', []))

    for serveur in distro['servers']:
        parcourir(serveur['modules'])
    manquants = set(liens) - vus
    if manquants:
        sys.exit(f'Mods absents de la distribution : {manquants}')
    json.dump(distro, open(distro_path, 'w'), indent=2, ensure_ascii=False)

    # Publication : distribution.json + fichiers Forge, sans les mods ni le cache de travail
    shutil.rmtree(SORTIE, ignore_errors=True)
    os.makedirs(SORTIE)
    shutil.copy(distro_path, SORTIE)
    if os.path.isdir(os.path.join(ROOT, 'repo')):
        shutil.copytree(os.path.join(ROOT, 'repo'), os.path.join(SORTIE, 'repo'),
                        ignore=shutil.ignore_patterns('work'))
    shutil.copytree(os.path.join(ROOT, 'servers'), os.path.join(SORTIE, 'servers'),
                    ignore=shutil.ignore_patterns('forgemods'))
    print('Distribution prete :', len(vus), 'mods')


if __name__ == '__main__':
    main()
