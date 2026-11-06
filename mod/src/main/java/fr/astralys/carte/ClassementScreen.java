package fr.astralys.carte;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Classement : les 10 meilleurs joueurs de chaque metier. */
public class ClassementScreen extends EcranAstralys {
    private static final int[] MEDAILLES = {0xF0B429, 0xC8CDD4, 0xCD7F32};
    private static final int[] CX = {110, 250, 390, 530};

    public ClassementScreen() {
        super("Classement", 0xF0B429);
        retour = MetiersScreen::new;
        ClientEco.demander(2);
    }

    @Override
    void contenu(GuiGraphics g) {
        Economie.Classement c = ClientEco.classement;
        String moi = Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getGameProfile().getName();
        for (int m = 0; m < 4; m++) {
            int col = Metiers.COULEURS[m];
            float x = CX[m] - 64;
            carte(g, x, 72, 128, 270, col);
            MetiersScreen.ecusson(g, CX[m], 100, 38, col, a);
            MetiersScreen.illustration(g, m, CX[m], 99, 24, a);
            texte(g, Metiers.NOMS[m], CX[m], 128, 1.1f, 0xFFFFFF, 0, true);
            List<String> l = c == null || m >= c.metiers().size() ? List.of() : c.metiers().get(m);
            if (c == null) texte(g, "Chargement…", CX[m], 200, .75f, 0x8A8A92, 0, false);
            else if (l.isEmpty()) texte(g, "Personne pour l'instant", CX[m], 200, .7f, 0x8A8A92, 0, false);
            for (int i = 0; i < l.size(); i++) {
                String[] e = l.get(i).split("\\|");
                float y = 146 + i * 19;
                boolean soi = e[0].equals(moi);
                rect(g, x + 6, y - 2, x + 122, y + 15, argb(soi ? col : 0x211838, soi ? .35f * a : a));
                if (i < 3) { MetiersScreen.losange(g, x + 15, y + 6, 11, MEDAILLES[i], a); texte(g, String.valueOf(i + 1), x + 15, y + 3, .6f, 0x1A1233, 0, true); }
                else texte(g, String.valueOf(i + 1), x + 15, y + 3, .7f, 0x8A8A92, 0, true);
                texte(g, e[0], x + 26, y + 1, .7f, i < 3 ? MEDAILLES[i] : 0xE8E8EE, -1, i < 3);
                texte(g, "niv. " + (e.length > 1 ? e[1] : "?"), x + 118, y + 1, .62f, col, 1, true);
                if (e.length > 2) texte(g, Economie.format(Long.parseLong(e[2])) + " XP", x + 26, y + 9, .5f, 0x9A9AA4, -1, false);
            }
        }
    }
}
