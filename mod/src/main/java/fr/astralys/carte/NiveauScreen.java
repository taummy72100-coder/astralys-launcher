package fr.astralys.carte;

import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Niveau du joueur (1 a 100) : grand medaillon, barre d'XP, chemin des 10 paliers et leurs recompenses, factions au niveau 30. */
public class NiveauScreen extends EcranAstralys {
    private static final int CYAN = 0x7FE0FF, OR = 0xF0B429;
    private int survol = -1;

    public NiveauScreen() { super("Niveau", CYAN); }

    private static long xp() { return ClientMetiers.vue == null ? 0 : ClientMetiers.vue.xpJoueur(); }

    @Override
    void contenu(GuiGraphics g) {
        long xp = xp();
        int nv = Metiers.niveauJoueur(xp);
        long reste = Metiers.resteJoueur(xp), cout = Metiers.coutJoueur(nv);

        // ---- medaillon et barre
        carte(g, 30, 66, 170, 276, CYAN);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, 115, 150, 70 + 6 * (float) Math.sin(t * 2), CYAN, .35f * a);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        MetiersScreen.medaillon(g, font, 115, 150, 44, 0x2F6BFF, "NIVEAU", String.valueOf(nv), a);
        texte(g, nv >= Metiers.NIVEAU_MAX ? "LÉGENDE D'ASTRALYS" : "JOUEUR", 115, 204, .8f, nv >= Metiers.NIVEAU_MAX ? OR : 0xFFFFFF, 0, true);
        barre(g, 44, 220, 142, 8, nv >= Metiers.NIVEAU_MAX ? 1 : reste / (float) cout, CYAN);
        texte(g, nv >= Metiers.NIVEAU_MAX ? "Niveau maximum" : Economie.format(reste) + " / " + Economie.format(cout) + " XP", 115, 232, .62f, 0xC8C8D0, 0, false);
        section(g, "Gagner de l'XP", 44, 250, CYAN);
        String[] src = {"Toute l'XP de tes 4 métiers", "Quêtes du jour", "Boss vaincus : 3 000 à 5 000", "Temps de jeu : 30 / minute"};
        for (int i = 0; i < src.length; i++) texte(g, "✦ " + src[i], 46, 266 + i * 11, .6f, 0xD8D8E0, -1, false);
        boolean fac = nv >= Metiers.NIVEAU_FACTION;
        rect(g, 40, 314, 190, 334, argb(fac ? 0x14532D : 0x3B1220, .9f * a));
        texte(g, fac ? "✔ FACTIONS DÉBLOQUÉES" : "🔒 FACTIONS AU NIVEAU " + Metiers.NIVEAU_FACTION, 115, 320, .65f, fac ? 0x22C55E : 0xF0645A, 0, true);

        // ---- chemin des paliers : 2 rangees de 5
        survol = -1;
        texte(g, "PALIERS DU JOUEUR", 216, 70, .85f, 0xFFFFFF, -1, true);
        texte(g, "une récompense tous les 10 niveaux", 216, 81, .6f, 0x9A9AA4, -1, false);
        for (int p = 1; p <= 10; p++) {
            int col = (p - 1) % 5, ran = (p - 1) / 5;
            float x = 216 + col * 82, y = 96 + ran * 124;
            boolean atteint = nv >= p * 10, prochain = !atteint && nv >= (p - 1) * 10;
            int c = atteint ? OR : prochain ? CYAN : 0x4A4A66;
            if (dans(x, y, x + 76, y + 116)) survol = p;
            carte(g, x, y, 76, 116, c);
            if (prochain) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                halo(g, x + 38, y + 24, 30 + 3 * (float) Math.sin(t * 3), CYAN, .3f * a);
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            MetiersScreen.losange(g, x + 38, y + 24, 30, c, a);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            texte(g, String.valueOf(p * 10), x + 38, y + 20, 1, atteint ? 0x2A1A00 : 0xFFFFFF, 0, true);
            texte(g, atteint ? "✔ OBTENU" : prochain ? "PROCHAIN" : "NIVEAU " + p * 10, x + 38, y + 44, .55f, c, 0, true);
            texte(g, Economie.format(Metiers.ARGENT_JOUEUR[p - 1]) + " $", x + 38, y + 56, .62f, 0x22C55E, 0, true);
            String[] objets = Metiers.PALIERS_JOUEUR[p - 1];
            for (int k = 0; k < objets.length; k++) {
                ItemStack s = Metiers.pile(objets[k]);
                float ox = x + 38 - objets.length * 9 + k * 18;
                objet(g, s, ox + 1, y + 70, 1);
                if (s.getCount() > 1) texte(g, "×" + s.getCount(), ox + 16, y + 82, .5f, 0xFFFFFF, 1, true);
            }
            if (p * 10 == Metiers.NIVEAU_FACTION) texte(g, "+ FACTIONS", x + 38, y + 100, .55f, 0xB46CFF, 0, true);
            if (p == 10) texte(g, "+ titre Légende", x + 38, y + 100, .55f, OR, 0, true);
        }
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survol < 0) return;
        var l = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        l.add(net.minecraft.network.chat.Component.literal("Palier " + survol * 10).withStyle(st -> st.withColor(OR).withBold(true)));
        l.add(net.minecraft.network.chat.Component.literal(Economie.format(Metiers.ARGENT_JOUEUR[survol - 1]) + " $").withStyle(st -> st.withColor(0x22C55E)));
        for (String o : Metiers.PALIERS_JOUEUR[survol - 1]) { ItemStack s = Metiers.pile(o); l.add(net.minecraft.network.chat.Component.literal(s.getCount() + "× " + s.getHoverName().getString())); }
        if (survol * 10 == Metiers.NIVEAU_FACTION) l.add(net.minecraft.network.chat.Component.literal("Débloque les factions").withStyle(st -> st.withColor(0xB46CFF)));
        g.renderComponentTooltip(font, l, mx, my);
    }

    @Override boolean clic(int b, boolean maj) { return false; }
}
