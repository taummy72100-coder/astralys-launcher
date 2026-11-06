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
        carte(g, x, y, L, 280, c);
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

        // oeuf
        ly = y + 178;
        objet(g, pile(Familiers.OEUFS[t]), x + 8, ly, 1);
        texte(g, pile(Familiers.OEUFS[t]).getHoverName().getString(), x + 28, ly + 2, .58f, 0xFFFFFF, -1, true);
        int nv = niveau(t);
        texte(g, Metiers.NOMS[t] + " niv. " + Familiers.NIVEAU + "  (toi : " + nv + ")", x + 28, ly + 10, .52f, nv >= Familiers.NIVEAU ? 0x22C55E : 0xF0645A, -1, false);

        // construction d'eclosion vue du dessus : socle 3x3, coins, incubateur au centre
        float gx = x + 10, gy = y + 202, k = 14;
        texte(g, "ÉCLOSION", x + 64, gy + 2, .55f, c, -1, true);
        ItemStack socle = pile(Familiers.SOCLE[t]), coin = pile(Familiers.COIN[t]), inc = pile("incubateur_astral");
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                rect(g, gx + i * k, gy + j * k, gx + i * k + k - 1, gy + j * k + k - 1, argb(0x0A0714, .7f * a));
                boolean estCoin = i != 1 && j != 1, centre = i == 1 && j == 1;
                objet(g, centre ? inc : estCoin ? coin : socle, gx + i * k + 1, gy + j * k + 1, .75f);
            }
        String[] mots = Familiers.CONSTRUCTION[t].split(" \\+ ");
        texte(g, mots[0], x + 64, gy + 12, .45f, 0xC8C8D0, -1, false);
        texte(g, "+ " + mots[1], x + 64, gy + 19, .45f, 0xC8C8D0, -1, false);
        texte(g, "puis clic droit avec l'œuf", x + 64, gy + 28, .45f, 0x9A9AA4, -1, false);

        // bouton
        int e = etat(t);
        boolean s = e > 0 && dans(x + 8, y + 256, x + L - 8, y + 274);
        int bc = e == 0 ? 0x3B2F5E : e == 1 ? 0xF0B429 : e == 2 ? 0x22C55E : 0xE8322B;
        rect(g, x + 7, y + 255, x + L - 7, y + 275, argb(0x0A0714, a));
        g.fillGradient(Math.round(x + 8), Math.round(y + 256), Math.round(x + L - 8), Math.round(y + 274), argb(s ? EcranAstralys.melange(bc, 0xFFFFFF, .25f) : bc, a), argb(EcranAstralys.melange(bc, 0, .4f), a));
        String b = switch (e) { case 0 -> "🔒 NIVEAU " + Familiers.NIVEAU + " REQUIS"; case 1 -> "ŒUF — " + Economie.format(Familiers.PRIX_OEUF) + " $"; case 2 -> "FAIRE VENIR"; default -> "RANGER"; };
        texte(g, b, cx, y + 261, .62f, e == 0 ? 0x8A8A92 : 0x0A0714, 0, true);
    }

    @Override
    boolean clic(int b, boolean maj) {
        for (int t = 0; t < Familiers.IDS.length; t++) {
            float x = X0 + t * PAS;
            int e = etat(t);
            if (e > 0 && dans(x + 8, Y0 + 256, x + L - 8, Y0 + 274)) {
                Metiers.CANAL.sendToServer(new Familiers.Action(e == 1 ? 1 : e == 2 ? 2 : 3, t));
                son(e == 1 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.UI_BUTTON_CLICK.value(), 1.2f);
                return true;
            }
        }
        return false;
    }
}
