package fr.astralys.carte;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/** Les 4 pets : apercu 3D qui tourne, avantages / inconvenients, oeuf, construction d'eclosion, et le bouton du moment. */
public class PetsScreen extends EcranAstralys {
    static Familiers.Vue vue;
    private static final float L = 140, PAS = 148, X0 = 30, Y0 = 66;
    private final Familiers.Pet[] apercus = new Familiers.Pet[Familiers.IDS.length];

    public PetsScreen() {
        super("Pets", 0x7FE0FF);
        if (Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Familiers.Action(0, 0));
    }

    private static int niveau(int m) { return Verrous.xpClient == null ? 0 : Metiers.niveau(Verrous.xpClient[m]); }
    private static boolean possede(int t) { return vue != null && (vue.debloques() & (1 << t)) != 0; }
    private static ItemStack pile(String id) { return Metiers.pile(id.contains(":") ? id : AstralysCarte.MODID + ":" + id); }

    private Familiers.Pet apercu(int t) {
        var mc = Minecraft.getInstance();
        if (apercus[t] == null && mc.level != null) { apercus[t] = Objets.PET.get().create(mc.level); if (apercus[t] != null) apercus[t].sorte(t); }
        return apercus[t];
    }

    /** Etat du bouton : 0 niveau trop bas, 1 acheter l'oeuf, 2 sortir, 3 ranger. */
    private static int etat(int t) {
        if (possede(t)) return vue.actif() == t ? 3 : 2;
        return niveau(t) >= Familiers.NIVEAU ? 1 : 0;
    }

    @Override
    void contenu(GuiGraphics g) {
        texte(g, "Un pet par métier • il vole à tes côtés et change ta façon de jouer", 622, 40, .62f, 0x9A9AA4, 1, false);
        for (int t = 0; t < Familiers.IDS.length; t++) carteDuPet(g, t);
    }

    private void carteDuPet(GuiGraphics g, int t) {
        float x = X0 + t * PAS, y = Y0, cx = x + L / 2;
        int c = Metiers.COULEURS[t];
        carte(g, x, y, L, 284, c);
        texte(g, Familiers.NOMS[t].toUpperCase(java.util.Locale.FRANCE), cx, y + 7, .8f, c, 0, true);
        texte(g, "Pet du " + Metiers.NOMS[t], cx, y + 18, .6f, 0xB8AEDA, 0, false);

        // apercu 3D
        halo(g, cx, y + 62, 40, c, .25f * a);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);   // le halo laisse sa teinte : sinon tout le reste de la carte est grise
        var p = apercu(t);
        if (p != null) {
            p.tickCount = (int) (t * 7 + this.t * 20);
            Quaternionf q = new Quaternionf().rotateZ((float) Math.PI).rotateY(this.t * .8f + t);
            InventoryScreen.renderEntityInInventory(g, Math.round(cx), Math.round(y + 82), 46, q, null, p);
        }
        String et = possede(t) ? (vue.actif() == t ? "✦ À TES CÔTÉS" : "✔ DÉBLOQUÉ") : niveau(t) >= Familiers.NIVEAU ? "ŒUF DISPONIBLE" : "🔒 VERROUILLÉ";
        texte(g, et, cx, y + 98, .62f, possede(t) ? 0x22C55E : niveau(t) >= Familiers.NIVEAU ? 0xF0B429 : 0x8A8A92, 0, true);

        // avantages / inconvenients
        float ly = y + 112;
        texte(g, "AVANTAGES", x + 8, ly, .55f, 0x22C55E, -1, true); ly += 8;
        for (String s : Familiers.AVANTAGES[t]) { texte(g, "✚ " + s, x + 8, ly, .52f, 0xD8F5DF, -1, false); ly += 7; }
        ly += 3;
        texte(g, "INCONVÉNIENTS", x + 8, ly, .55f, 0xF0645A, -1, true); ly += 8;
        for (String s : Familiers.INCONVENIENTS[t]) { texte(g, "✖ " + s, x + 8, ly, .52f, 0xF8D8D6, -1, false); ly += 7; }

        // passifs debloques par le niveau du metier
        int nv = niveau(t);
        ly += 3;
        texte(g, "PASSIFS DU MÉTIER", x + 8, ly, .55f, c, -1, true); ly += 8;
        boolean mt = nv >= Familiers.NIVEAU_MONTURE;
        texte(g, (mt ? "✔ " : "🔒 ") + Familiers.NIVEAU_MONTURE + " • Monture (touche G)", x + 8, ly, .52f, mt ? 0xFFFFFF : 0x6A6A78, -1, false); ly += 7;
        for (int i = 0; i < 4; i++) {
            boolean ok = nv >= Familiers.NIVEAUX_PASSIFS[i];
            texte(g, (ok ? "✔ " : "🔒 ") + Familiers.NIVEAUX_PASSIFS[i] + " • " + Familiers.PASSIFS[t][i], x + 8, ly, .52f, ok ? 0xFFFFFF : 0x6A6A78, -1, false); ly += 7;
        }

        // oeuf + construction d'eclosion vue du dessus : socle 3x3, coins, incubateur au centre
        float gx = x + 8, gy = y + 222, k = 10;
        ItemStack socle = pile(Familiers.SOCLE[t]), coin = pile(Familiers.COIN[t]), inc = pile("incubateur_astral"), oeuf = pile(Familiers.OEUFS[t]);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                rect(g, gx + i * k, gy + j * k, gx + i * k + k - 1, gy + j * k + k - 1, argb(0x0A0714, .7f * a));
                boolean estCoin = i != 1 && j != 1, centre = i == 1 && j == 1;
                objet(g, centre ? inc : estCoin ? coin : socle, gx + i * k + 1, gy + j * k + 1, .5f);
            }
        float tx = x + 42;
        objet(g, oeuf, tx, gy - 1, .6f);
        texte(g, oeuf.getHoverName().getString(), tx + 12, gy + 1, .52f, 0xFFFFFF, -1, true);
        texte(g, Metiers.NOMS[t] + " niv. " + Familiers.NIVEAU + "  (toi : " + nv + ")", tx, gy + 10, .48f, nv >= Familiers.NIVEAU ? 0x22C55E : 0xF0645A, -1, false);
        String[] mots = Familiers.CONSTRUCTION[t].split(" \\+ ");
        texte(g, mots[0], tx, gy + 17, .42f, 0xC8C8D0, -1, false);
        texte(g, "+ " + mots[1], tx, gy + 23, .42f, 0xC8C8D0, -1, false);

        // boutons : venir / ranger, et monter
        int e = etat(t);
        bouton(g, x + 7, y + 262, possede(t) ? x + L / 2 + 14 : x + L - 7, e, switch (e) { case 0 -> "🔒 NIVEAU " + Familiers.NIVEAU + " REQUIS"; case 1 -> "ŒUF — " + Economie.format(Familiers.PRIX_OEUF) + " $"; case 2 -> "FAIRE VENIR"; default -> "RANGER"; },
                e == 0 ? 0x3B2F5E : e == 1 ? 0xF0B429 : e == 2 ? 0x22C55E : 0xE8322B);
        if (possede(t)) bouton(g, x + L / 2 + 17, y + 262, x + L - 7, mt && e == 3 ? 1 : 0, mt ? "MONTER" : "🔒 NIV. " + Familiers.NIVEAU_MONTURE, 0x7FE0FF);
    }

    private void bouton(GuiGraphics g, float x1, float y1, float x2, int e, String b, int bc) {
        boolean s = e > 0 && dans(x1 + 1, y1 + 1, x2 - 1, y1 + 17);
        if (e == 0) bc = 0x3B2F5E;
        rect(g, x1, y1, x2, y1 + 18, argb(0x0A0714, a));
        g.fillGradient(Math.round(x1 + 1), Math.round(y1 + 1), Math.round(x2 - 1), Math.round(y1 + 17), argb(s ? EcranAstralys.melange(bc, 0xFFFFFF, .25f) : bc, a), argb(EcranAstralys.melange(bc, 0, .4f), a));
        texte(g, b, (x1 + x2) / 2, y1 + 6, .6f, e == 0 ? 0x8A8A92 : 0x0A0714, 0, true);
    }

    @Override
    boolean clic(int b, boolean maj) {
        for (int t = 0; t < Familiers.IDS.length; t++) {
            float x = X0 + t * PAS;
            int e = etat(t);
            float fin = possede(t) ? x + L / 2 + 14 : x + L - 7;
            if (possede(t) && e == 3 && niveau(t) >= Familiers.NIVEAU_MONTURE && dans(x + L / 2 + 18, Y0 + 263, x + L - 8, Y0 + 279)) {
                Metiers.CANAL.sendToServer(new Familiers.Action(4, t));
                son(SoundEvents.HORSE_SADDLE, 1.2f);
                minecraft.setScreen(null);
                return true;
            }
            if (e > 0 && dans(x + 8, Y0 + 263, fin - 1, Y0 + 279)) {
                Metiers.CANAL.sendToServer(new Familiers.Action(e == 1 ? 1 : e == 2 ? 2 : 3, t));
                son(e == 1 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.UI_BUTTON_CLICK.value(), 1.2f);
                return true;
            }
        }
        return false;
    }
}
