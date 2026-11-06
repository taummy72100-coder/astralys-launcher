package fr.astralys.carte;

import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ecran de faction : blason, chiffres cles, defis de la semaine, membres (tetes des joueurs) et classement. */
public class FactionScreen extends EcranAstralys {
    private static final int VIOLET = 0xB46CFF, OR = 0xF0B429;
    private static final int[] BLASONS = {0xB46CFF, 0x3DD6C5, 0xFF7A45, 0x4C8DFF, 0xFF4F8B, 0x7CE07C, 0xF0B429};
    private static final int[] MEDAILLES = {0xF0B429, 0xC8CDD4, 0xCD7F32};
    private static final ItemStack[] DEFI = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.WHEAT),
            new ItemStack(Items.BREWING_STAND), new ItemStack(Items.EMERALD), new ItemStack(Items.EXPERIENCE_BOTTLE)};
    private static final ItemStack[] STATS = {new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.NETHER_STAR), new ItemStack(Items.FILLED_MAP), new ItemStack(Items.EMERALD)};

    public FactionScreen() { super("Faction", VIOLET); ClientEco.demander(1); }

    private static ItemStack icone(String titre) {
        for (int i = 0; i < Economie.DEFIS.size(); i++) if (Economie.DEFIS.get(i).titre().equals(titre)) return DEFI[i];
        return DEFI[0];
    }

    static int couleur(String nom) { return BLASONS[Math.floorMod(nom.toLowerCase(Locale.ROOT).hashCode(), BLASONS.length)]; }

    private static ResourceLocation peau(String nom) {
        var co = Minecraft.getInstance().getConnection();
        PlayerInfo i = co == null ? null : co.getPlayerInfo(nom);
        return i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin();
    }

    private void blason(GuiGraphics g, float cx, float cy, float taille, int c, String texte) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, cx, cy, taille * .9f, c, (.3f + .15f * (float) Math.sin(t * 2)) * a);
        RenderSystem.defaultBlendFunc();
        hexBord(g, cx, cy, taille, c);
        MetiersScreen.teinte(EcranAstralys.melange(c, 0x000000, .45f), a);
        g.pose().pushPose(); g.pose().translate(cx - taille * .36f, cy - taille * .36f, 0); g.pose().scale(taille * .72f / 128f, taille * .72f / 128f, 1);
        RenderSystem.enableBlend();
        g.blit(new ResourceLocation(AstralysCarte.MODID, "textures/gui/hex.png"), 0, 0, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        RenderSystem.setShaderColor(1, 1, 1, a);
        texte(g, texte, cx, cy - 4 * taille / 46, 1.3f * taille / 46, 0xFFFFFF, 0, true);
    }

    private void classement(GuiGraphics g, Economie.FactionVue f, float x, float y, float w, int lignes) {
        for (int i = 0; i < Math.min(lignes, f.top().size()); i++) {
            String[] e = f.top().get(i).split("\\|");
            float ly = y + i * 13;
            boolean moi = e[0].equals(f.nom());
            if (moi) rect(g, x - 4, ly - 3, x + w + 4, ly + 10, argb(VIOLET, .15f * a));
            if (i < 3) { hexBord(g, x + 4, ly + 3, 9, MEDAILLES[i]); }
            else texte(g, String.valueOf(i + 1), x + 4, ly, .65f, 0x8A8A92, 0, true);
            texte(g, e[0], x + 14, ly, .72f, i < 3 ? MEDAILLES[i] : 0xE8E8EE, -1, i < 3);
            texte(g, e.length > 1 ? Economie.format(Long.parseLong(e[1])) + " pts" : "", x + w, ly, .65f, 0x9A9AA4, 1, false);
        }
        if (f.top().isEmpty()) texte(g, "Aucune faction pour l'instant", x + w / 2, y + 10, .7f, 0x8A8A92, 0, false);
    }

    @Override
    void contenu(GuiGraphics g) {
        texte(g, "✦ " + Economie.format(ClientEco.solde) + " $", 528, 21, 1.1f, OR, 1, true);
        Economie.FactionVue f = ClientEco.faction;
        if (f == null) { texte(g, "Chargement…", 320, 180, 1, 0x8A8A92, 0, false); return; }
        if (f.nom().isEmpty()) { sansFaction(g, f); return; }
        int fc = couleur(f.nom());
        int rang = -1;
        for (int i = 0; i < f.top().size(); i++) if (f.top().get(i).startsWith(f.nom() + "|")) rang = i + 1;
        long enLigne = f.enLigne().stream().filter(b -> b).count();

        // ---- en-tete
        carte(g, 38, 68, 584, 56, fc);
        blason(g, 72, 96, 44, fc, f.nom().substring(0, Math.min(2, f.nom().length())).toUpperCase(Locale.ROOT));
        texte(g, f.nom().toUpperCase(Locale.FRANCE), 104, 78, 2, 0xFFFFFF, -1, true);
        texte(g, "Chef : " + f.chef() + "   •   " + f.membres().size() + " membre" + (f.membres().size() > 1 ? "s" : "") + "   •   " + enLigne + " en ligne",
                104, 101, .72f, 0xB8B8C2, -1, false);
        // niveau et XP de la faction, gains du classement a minuit
        int nv = Economie.niveauFaction(f.xp());
        boolean maxi = nv >= Economie.NIVEAU_FACTION_MAX;
        hexBord(g, 372, 90, 26, OR);
        texte(g, String.valueOf(nv), 372, 86, 1, 0x2A1A00, 0, true);
        texte(g, "NIVEAU DE FACTION", 392, 76, .62f, OR, -1, true);
        barre(g, 392, 88, 150, 5, maxi ? 1 : Economie.resteFaction(f.xp()) / (float) Economie.coutFaction(nv), OR);
        texte(g, maxi ? "Niveau maximum" : Economie.format(Economie.resteFaction(f.xp())) + " / " + Economie.format(Economie.coutFaction(nv)) + " XP", 392, 97, .55f, 0xC8C8D0, -1, false);
        String gain = rang > 0 && rang <= Economie.GAIN_ARGENT.length ? "+" + Economie.format(Economie.GAIN_ARGENT[rang - 1]) + " $  •  +" + Economie.GAIN_XP[rang - 1] + " XP"
                : f.points() > 0 ? "+" + Economie.format(Economie.GAIN_ARGENT_AUTRES) + " $  •  +" + Economie.GAIN_XP_AUTRES + " XP" : "gagne des points pour être payé";
        texte(g, "À minuit : " + gain, 392, 108, .55f, 0x22C55E, -1, true);
        texte(g, rang > 0 ? "#" + rang : "—", 610, 76, 2.2f, rang == 1 ? OR : VIOLET, 1, true);
        texte(g, "AU CLASSEMENT", 610, 101, .6f, 0x8A8A92, 1, true);

        // ---- chiffres cles
        String[][] st = {{"BANQUE", Economie.format(f.banque()) + " $"}, {"POINTS", Economie.format(f.points())},
                {"CHUNKS", f.claims() + " / " + f.max()}, {"PROCHAIN CHUNK", Economie.format(f.prixClaim()) + " $"}};
        int[] sc = {OR, VIOLET, 0xFFFFFF, 0x22C55E};
        for (int i = 0; i < 4; i++) {
            float x = 38 + i * 148;
            carte(g, x, 134, 140, 36, 0);
            hexBord(g, x + 18, 152, 22, 0x3A2C66);
            objet(g, STATS[i], x + 10, 144, 1);
            texte(g, st[i][0], x + 36, 141, .58f, 0x8A8A92, -1, true);
            texte(g, st[i][1], x + 36, 152, 1.05f, sc[i], -1, true);
        }

        // ---- defis de la semaine
        carte(g, 38, 180, 372, 162, 0);
        section(g, "Défis de la semaine", 50, 188, VIOLET);
        texte(g, "renouvelés chaque lundi", 400, 189, .6f, 0x6E6E78, 1, false);
        for (int i = 0; i < f.defis().size(); i++) {
            Economie.DefiVue d = f.defis().get(i);
            float y = 204 + i * 45;
            boolean fini = d.avance() >= d.nombre(), s = dans(46, y, 402, y + 40);
            rect(g, 46, y, 402, y + 40, argb(s ? 0x3A2C66 : 0x211838, a));
            int dc = fini ? 0x22C55E : VIOLET;
            hexBord(g, 68, y + 20, 28, dc);
            objet(g, icone(d.titre()), 60, y + 12, 1);
            texte(g, d.titre().toUpperCase(Locale.FRANCE), 90, y + 6, .75f, 0xFFFFFF, -1, true);
            texte(g, d.desc(), 90, y + 16, .62f, 0xB8B8C2, -1, false);
            float p = Math.min(1, d.avance() / (float) Math.max(1, d.nombre()));
            barre(g, 90, y + 28, 214, 5, p, dc);
            texte(g, fini ? "✔ RÉUSSI" : Math.round(p * 100) + " %", 312, y + 27, .65f, fini ? 0x22C55E : 0xFFFFFF, -1, true);
            texte(g, "+" + Economie.format(d.argent()) + " $", 396, y + 6, .7f, OR, 1, true);
            texte(g, "+" + d.points() + " pts", 396, y + 17, .65f, VIOLET, 1, true);
            if (s) texte(g, Economie.format(d.avance()) + " / " + Economie.format(d.nombre()), 396, y + 28, .58f, 0x9A9AA4, 1, false);
        }

        // ---- membres
        carte(g, 420, 180, 202, 84, 0);
        section(g, "Membres", 432, 188, fc);
        for (int i = 0; i < Math.min(10, f.membres().size()); i++) {
            float x = 432 + (i / 5) * 96, y = 202 + (i % 5) * 12;
            boolean on = f.enLigne().get(i);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, on ? a : .4f * a);
            PlayerFaceRenderer.draw(g, peau(f.membres().get(i)), Math.round(x), Math.round(y), 9);
            RenderSystem.setShaderColor(1, 1, 1, a);
            rect(g, x + 7, y + 7, x + 10, y + 10, argb(on ? 0x22C55E : 0x55555E, a));
            boolean chef = f.membres().get(i).equals(f.chef());
            texte(g, (chef ? "★ " : "") + f.membres().get(i), x + 13, y + 1, .65f, on ? (chef ? OR : 0xFFFFFF) : 0x8A8A92, -1, chef);
        }
        if (f.membres().size() > 10) texte(g, "+" + (f.membres().size() - 10) + " autres", 612, 188, .6f, 0x8A8A92, 1, false);

        // ---- classement
        carte(g, 420, 272, 202, 70, 0);
        section(g, "Classement", 432, 279, OR);
        classement(g, f, 434, 293, 178, 4);
    }

    private void sansFaction(GuiGraphics g, Economie.FactionVue f) {
        carte(g, 38, 68, 372, 274, VIOLET);
        blason(g, 88, 118, 58, 0x4A4A55, "?");
        texte(g, "PAS ENCORE DE FACTION", 130, 98, 1.5f, 0xFFFFFF, -1, true);
        var vj = ClientMetiers.vue;
        int nvj = vj == null ? 0 : Metiers.niveauJoueur(vj.xpJoueur());
        if (nvj < Metiers.NIVEAU_FACTION)
            texte(g, "🔒 Débloqué au niveau " + Metiers.NIVEAU_FACTION + " du joueur (toi : " + nvj + ")", 130, 140, .75f, 0xF0645A, -1, true);
        texte(g, "Fonde la tienne ou rejoins tes amis :", 130, 118, .75f, 0xB8B8C2, -1, false);
        texte(g, "banque commune, chunks protégés, défis et classement.", 130, 129, .75f, 0xB8B8C2, -1, false);
        String[][] etapes = {{"Fonder ta faction", "/f create <nom>"}, {"Inviter des joueurs", "/f invite <joueur>"},
                {"Remplir la banque", "/f deposit <montant>"}, {"Acheter des chunks", "/f claim  (à Pyrélune ou Azuréa)"}};
        for (int i = 0; i < etapes.length; i++) {
            float y = 164 + i * 40;
            rect(g, 50, y, 398, y + 32, argb(0x211838, a));
            hexBord(g, 70, y + 16, 22, VIOLET);
            texte(g, String.valueOf(i + 1), 70, y + 12, .9f, 0xFFFFFF, 0, true);
            texte(g, etapes[i][0].toUpperCase(Locale.FRANCE), 92, y + 6, .75f, 0xFFFFFF, -1, true);
            float w = font.width(etapes[i][1]) * .72f;
            rect(g, 92, y + 17, 92 + w + 8, y + 28, argb(0x0E0E11, a));
            texte(g, etapes[i][1], 96, y + 19, .72f, 0xD8C8FF, -1, false);
        }
        texte(g, "Quelqu'un t'invite ? Clique sur [Rejoindre] dans le chat.", 224, 330, .62f, 0x8A8A92, 0, false);

        carte(g, 420, 68, 202, 274, OR);
        section(g, "Classement", 432, 78, OR);
        // podium des 3 premieres
        float[][] pod = {{521, 150, 1}, {465, 162, 0}, {577, 168, 2}};
        float[] haut = {54, 42, 34};
        for (float[] p : pod) {
            int i = (int) p[2];
            if (i >= f.top().size()) continue;
            String[] e = f.top().get(i).split("\\|");
            int c = couleur(e[0]);
            rect(g, p[0] - 24, 230 - haut[i], p[0] + 24, 230, argb(EcranAstralys.melange(MEDAILLES[i], 0x000000, .55f), a));
            rect(g, p[0] - 24, 230 - haut[i], p[0] + 24, 232 - haut[i], argb(MEDAILLES[i], a));
            texte(g, String.valueOf(i + 1), p[0], 236 - haut[i], 1.2f, MEDAILLES[i], 0, true);
            blason(g, p[0], 214 - haut[i] - 22, 30, c, e[0].substring(0, Math.min(2, e[0].length())).toUpperCase(Locale.ROOT));
            texte(g, e[0], p[0], 238 - haut[i] + 14, .62f, 0xFFFFFF, 0, true);
        }
        classement(g, f, 434, 252, 178, 6);
    }
}
