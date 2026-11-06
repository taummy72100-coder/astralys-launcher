package fr.astralys.carte;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Detail d'un metier : badge, progression, ce qui rapporte de l'XP, paliers de niveaux et leurs recompenses. */
public class MetierScreen extends EcranAstralys {
    private final int m, c;
    private int palierSurvol = -1;

    public MetierScreen(int m) {
        super(Metiers.NOMS[m], Metiers.COULEURS[m]);
        this.m = m; this.c = Metiers.COULEURS[m];
        retour = MetiersScreen::new;
    }

    private static ItemStack pile(String id) {
        var it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

    @Override
    void contenu(GuiGraphics g) {
        Metiers.Vue v = ClientMetiers.vue;
        long xp = v == null ? 0 : v.xp()[m];
        int nv = Metiers.niveau(xp);
        float bonus = v == null ? 0 : v.bonus();
        boolean max = nv >= Metiers.NIVEAU_MAX;
        long reste = Metiers.reste(xp), cout = Metiers.cout(nv);

        // ---- colonne gauche : badge et progression
        carte(g, 38, 70, 208, 272, c);
        float puls = .5f + .5f * (float) Math.sin(t * 2.2f);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, 142, 140, 88, c, (.25f + .15f * puls) * a);
        RenderSystem.defaultBlendFunc();
        MetiersScreen.ecusson(g, 142, 138, 118, c, a);
        float bob = (float) Math.sin(t * 1.8f) * 2;
        MetiersScreen.illustration(g, m, 142, 134 + bob, 72, a);
        // niveau : medaillon sur la pointe basse
        MetiersScreen.medaillon(g, font, 142, 204, 21, c, "NIVEAU", String.valueOf(nv), a);
        // barre et chiffres
        texte(g, max ? "NIVEAU MAXIMUM" : "PROGRESSION", 50, 242, .65f, 0x9A9AA4, -1, true);
        texte(g, max ? "" : String.format(Locale.FRANCE, "%d %%", Math.round(100f * reste / cout)), 234, 242, .65f, c, 1, true);
        barre(g, 50, 252, 184, 6, max ? 1 : reste / (float) cout, c);
        texte(g, max ? "—" : Economie.format(reste) + " / " + Economie.format(cout) + " XP", 142, 263, .7f, 0xE8E8EE, 0, false);
        String[][] stats = {
                {"XP totale", Economie.format(xp)},
                {"Prochain niveau", max ? "—" : "+" + Economie.format(100L * (nv + 1)) + " $"},
                {"Bonus total", String.format(Locale.FRANCE, "+%.1f %%", bonus)}};
        for (int i = 0; i < stats.length; i++) {
            float y = 282 + i * 18;
            rect(g, 50, y + 12, 234, y + 13, argb(0xFFFFFF, .05f * a));
            texte(g, stats[i][0], 50, y, .75f, 0x9A9AA4, -1, false);
            texte(g, stats[i][1], 234, y, .8f, i == 1 ? 0xF0B429 : 0xFFFFFF, 1, true);
        }

        // ---- en haut a droite : ce qui rapporte de l'XP
        carte(g, 258, 70, 364, 146, 0);
        section(g, "Gagner de l'XP", 270, 78, c);
        texte(g, String.format(Locale.FRANCE, "multiplicateur x%.3f", 1 + bonus / 100), 610, 79, .65f, 0x9A9AA4, 1, false);
        Object[][] src = Metiers.SOURCES[m];
        for (int i = 0; i < src.length; i++) {
            float x = 270 + (i / 5) * 176, y = 94 + (i % 5) * 23;
            boolean s = dans(x - 2, y - 2, x + 170, y + 19);
            rect(g, x - 2, y - 2, x + 170, y + 19, argb(s ? 0x34343C : 0x1A1A1F, a));
            objet(g, pile((String) src[i][0]), x, y, 1);
            texte(g, (String) src[i][1], x + 21, y + 5, .75f, 0xE8E8EE, -1, false);
            long gain = Math.max(1, Math.round(((Number) src[i][2]).intValue() * (1 + bonus / 100)));
            texte(g, "+" + gain + " XP", x + 164, y + 5, .75f, c, 1, true);
        }
        if (m == Metiers.HUNTER) texte(g, "Monstres : PV max ÷ 3 (2 XP minimum)", 610, 202, .6f, 0x6E6E78, 1, false);

        // ---- en bas a droite : paliers
        carte(g, 258, 226, 364, 116, 0);
        section(g, "Paliers", 270, 234, c);
        texte(g, "chaque niveau : +100 $ × niveau  •  +0,1 % de bonus", 610, 235, .62f, 0x9A9AA4, 1, false);
        float x1 = 284, x2 = 600, yy = 282;
        rect(g, x1, yy - 1, x2, yy + 2, argb(0x111114, a));
        float f = Math.min(1, nv / 100f);
        g.fillGradient(Math.round(x1), Math.round(yy - 1), Math.round(x1 + (x2 - x1) * f), Math.round(yy + 2), argb(EcranAstralys.melange(c, 0xFFFFFF, .3f), a), argb(c, a));
        palierSurvol = -1;
        for (int i = 0; i < 10; i++) {
            int p = (i + 1) * 10;
            float x = x1 + (x2 - x1) * p / 100f;
            boolean ok = nv >= p, s = dans(x - 12, yy - 26, x + 12, yy + 30);
            if (s) palierSurvol = p;
            if (ok) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                halo(g, x, yy, 16, c, (.35f + .2f * puls) * a);
                RenderSystem.defaultBlendFunc();
            }
            MetiersScreen.losange(g, x, yy, s ? 22 : 18, ok ? c : 0x3A3A42, a);
            texte(g, ok ? "✔" : String.valueOf(i + 1), x, yy - 3, .6f, 0xFFFFFF, 0, true);
            texte(g, String.valueOf(p), x, yy - 22, .7f, ok ? c : 0x8A8A92, 0, true);
            texte(g, "+" + (p * 100 >= 1000 ? p / 10 + "k" : String.valueOf(p * 100)) + "$", x, yy + 16, .55f, ok ? 0xF0B429 : 0x6E6E78, 0, true);
        }
        // curseur du niveau actuel
        float xc = x1 + (x2 - x1) * f;
        texte(g, "▼", xc, yy - 12, .7f, 0xFFFFFF, 0, true);
        texte(g, max ? "Tu as atteint le sommet de ce métier !" : "Encore " + Economie.format(cout - reste) + " XP avant le niveau " + (nv + 1),
                440, 324, .7f, 0xC8C8D0, 0, false);
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (palierSurvol < 0) return;
        int p = palierSurvol;
        long total = 0;
        for (int n = 1; n <= p; n++) total += 100L * n;
        long xpTotal = 0;
        for (int n = 0; n < p; n++) xpTotal += Metiers.cout(n);
        g.renderComponentTooltip(font, List.of(
                Component.literal("Palier " + p).withStyle(st -> st.withColor(c).withBold(true)),
                Component.literal("Récompense du niveau : +" + Economie.format(100L * p) + " $").withStyle(st -> st.withColor(0xF0B429)),
                Component.literal("Argent cumulé jusqu'ici : " + Economie.format(total) + " $").withStyle(st -> st.withColor(0xE8E8EE)),
                Component.literal("Bonus apporté : +" + String.format(Locale.FRANCE, "%.1f", p * .1f) + " %").withStyle(st -> st.withColor(0xE8E8EE)),
                Component.literal("XP totale nécessaire : " + Economie.format(xpTotal)).withStyle(st -> st.withColor(0x9A9AA4))), mx, my);
    }
}
