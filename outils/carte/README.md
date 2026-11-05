# Carte des mondes d'Astralys

Version actuelle (facon Paladium, rendu 3D Blender) :

1. `p_monde.py` — construit le monde bloc par bloc (Pyrelune, Sylvaria, Azurea, Sunfalls) -> `p_monde.npz`.
2. `p_rendu.py` — rendu Cycles avec le moteur de Blender (`pip install bpy`, Python 3.11) :
   `python p_rendu.py 3344 1882 40 p_final.png` puis la passe des lumieres `python p_rendu.py 1672 941 12 p_emission.png emission`.
3. `p_compose.py p_final.png` — lueurs, logo, badges et noms -> `p_carte.png` (texture du mod).

Les anciennes versions (`carte3.py`, `compose3.py`) sont gardees pour memoire.
Les chemins en tete des scripts pointent vers le dossier de travail : a adapter avant de relancer.
