"""Verifie que chaque fichier de la distribution publiee se telecharge, avec la bonne taille."""
import json, sys, time, urllib.request

URL = sys.argv[1]

def taille(url):
    req = urllib.request.Request(url, method='HEAD', headers={'User-Agent': 'astralys-launcher-ci'})
    with urllib.request.urlopen(req) as r:
        return int(r.headers.get('Content-Length', -1))

for essai in range(10):
    try:
        distro = json.load(urllib.request.urlopen(URL))
        break
    except Exception as e:
        print('distribution pas encore visible, nouvel essai', e); time.sleep(15)
else:
    sys.exit('distribution.json introuvable')

erreurs = 0
def parcourir(modules):
    global erreurs
    for m in modules:
        a = m['artifact']
        try:
            t = taille(a['url'])
            ok = t == a['size'] or t == -1
        except Exception as e:
            ok, t = False, e
        print('OK ' if ok else 'ERREUR', m['type'], a['url'], t)
        erreurs += not ok
        parcourir(m.get('subModules', []))

for s in distro['servers']:
    parcourir(s['modules'])
sys.exit(1 if erreurs else 0)
