package fr.astralys.carte;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Coffres cosmetiques, payes en rubis bleus : uniquement des cosmetiques que tu n'as pas encore, avec les chances affichees. */
public class CoffresCosmScreen extends EcranAstralys {
    private static final int OR = 0xF0B429, BLEU = 0x4FA8FF;
    private int choisi = -1;
    private long choisiA;

    public CoffresCosmScreen() {
        super("Coffres", OR);
        retour = ShopScreen::new;
        if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Cosmetiques.Action(0, ""));
    }

    private static ItemStack modele(int r) { return Metiers.pile("astralyscarte:coffre_" + Coffres.RARETES[r]); }

    @Override
    void contenu(GuiGraphics g) {
        String s = Economie.format(CosmetiqueScreen.rubis());
        texte(g, s, 528, 22, 1.3f, BLEU, 1, true);
        objet(g, CosmetiqueScreen.RUBIS_BLEU, 528 - font.width(net.minecraft.network.chat.Component.literal(s).withStyle(st -> st.withBold(true))) * 1.3f - 18, 18, 1);
        texte(g, "Chaque coffre donne un cosmétique que tu n'as pas encore • aucun objet de jeu • chances garanties ci-dessous", 320, 50, .55f, 0x9A9AA4, 0, false);
        if (choisi >= 0 && System.currentTimeMillis() - choisiA > 4000) choisi = -1;
        List<String> pos = ClientCosm.vue == null ? List.of() : ClientCosm.vue.possedes();
        int restant = 0;
        for (Cosmetiques.Cosm c : CatalogueCosm.LISTE) if (!pos.contains(c.id())) restant++;
        for (int i = 0; i < Profil.COFFRES.size(); i++) coffre(g, i, 38 + i * 190, 66, restant);
    }

    private void coffre(GuiGraphics g, int i, float x, float y, int restant) {
        Profil.CoffreCosm co = Profil.COFFRES.get(i);
        int c = Coffres.COULEURS[co.modele()];
        boolean s = dans(x, y, x + 182, y + 274);
        carte(g, x, y, 182, 274, c);
        texte(g, co.nom().toUpperCase(Locale.FRANCE), x + 91, y + 10, 1, c, 0, true);
        texte(g, "coffre cosmétique", x + 91, y + 22, .55f, 0xB8AEDA, 0, false);
        // le coffre 3D qui flotte dans son halo
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, x + 91, y + 76, (s ? 58 : 48) + 4 * (float) Math.sin(t * 2 + i), c, (s ? .55f : .35f) * a);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        float bob = (float) Math.sin(t * 2.2f + i) * 3;
        objet(g, modele(co.modele()), x + 91 - 36, y + 40 + bob, 4.5f);
        // chances
        texte(g, "CHANCES", x + 12, y + 128, .6f, 0xFFFFFF, -1, true);
        for (int r = 0; r < 4; r++) {
            float ly = y + 141 + r * 18;
            int rc = Cosmetiques.COULEURS_RARETE[r];
            texte(g, Cosmetiques.RARETES[r], x + 12, ly, .55f, rc, -1, true);
            texte(g, co.chances()[r] + " %", x + 170, ly, .55f, co.chances()[r] > 0 ? 0xFFFFFF : 0x6E6E78, 1, true);
            barre(g, x + 12, ly + 8, 158, 3, co.chances()[r] / 100f, rc);
        }
        texte(g, restant + " cosmétiques encore à découvrir", x + 91, y + 218, .5f, 0x9A9AA4, 0, false);
        // bouton
        boolean sur = dans(x + 10, y + 234, x + 172, y + 262), conf = choisi == i;
        boolean assez = CosmetiqueScreen.rubis() >= co.prix() && restant > 0;
        int bc = !assez ? 0x3B2F5E : conf ? 0xF0B429 : c;
        rect(g, x + 9, y + 233, x + 173, y + 263, argb(0x0A0714, a));
        g.fillGradient(Math.round(x + 10), Math.round(y + 234), Math.round(x + 172), Math.round(y + 262),
                argb(sur && assez ? EcranAstralys.melange(bc, 0xFFFFFF, .25f) : bc, a), argb(EcranAstralys.melange(bc, 0, .45f), a));
        if (conf) texte(g, "CONFIRMER L'OUVERTURE", x + 91, y + 244, .7f, 0x0A0714, 0, true);
        else {
            String p = Economie.format(co.prix());
            texte(g, "OUVRIR", x + 18, y + 244, .7f, assez ? 0x0A0714 : 0x8A8A92, -1, true);
            texte(g, p, x + 150, y + 243, .9f, assez ? 0xFFFFFF : 0x8A8A92, 1, true);
            objet(g, CosmetiqueScreen.RUBIS_BLEU, x + 151, y + 240, .8f);
        }
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (b != 0) return false;
        for (int i = 0; i < Profil.COFFRES.size(); i++) {
            float x = 38 + i * 190;
            if (!dans(x + 10, 66 + 234, x + 172, 66 + 262)) continue;
            if (CosmetiqueScreen.rubis() < Profil.COFFRES.get(i).prix()) { son(SoundEvents.VILLAGER_NO, 1); return true; }
            // deux clics : jamais d'achat par erreur
            if (choisi != i) { choisi = i; choisiA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); return true; }
            choisi = -1;
            Metiers.CANAL.sendToServer(new Cosmetiques.Action(7, String.valueOf(i)));
            son(SoundEvents.CHEST_OPEN, 1);
            return true;
        }
        return false;
    }
}
