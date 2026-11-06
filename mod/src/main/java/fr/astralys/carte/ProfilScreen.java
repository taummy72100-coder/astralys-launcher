package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Profil du joueur : banniere personnalisable (fond, motif, embleme), grade, badges de quetes et emotes. Options payantes en rubis bleus. */
public class ProfilScreen extends EcranAstralys {
    private static final int CYAN = 0x7FE0FF, BLEU = 0x4FA8FF;
    private static final String[] ONGLETS = {"BANNIÈRE", "GRADE", "BADGES", "EMOTES"};
    int onglet;
    private String choisi, apercuEmote;
    private long choisiA;
    private Profil.Option survol, survolEmote;
    private Profil.Badge survolBadge;

    public ProfilScreen() {
        super("Profil", CYAN);
        if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Cosmetiques.Action(0, ""));
    }

    private static Cosmetiques.Vue vue() { return ClientCosm.vue; }
    private static boolean a(Profil.Option o) { return o.prix() == 0 || (vue() != null && vue().possedes().contains(o.cle())); }
    private static String choix(int type) { return vue() == null ? Profil.defaut(type) : vue().choix(type); }
    private static int quetes() { return ClientMetiers.vue == null ? 0 : ClientMetiers.vue.quetesFaites(); }
    private static int niveau() { return ClientMetiers.vue == null ? 0 : Metiers.niveauJoueur(ClientMetiers.vue.xpJoueur()); }
    private static int[] metiers() {
        int[] n = new int[4];
        if (ClientMetiers.vue != null) for (int i = 0; i < 4; i++) n[i] = Metiers.niveau(ClientMetiers.vue.xp()[i]);
        return n;
    }

    private static List<Profil.Badge> badges() {
        List<Profil.Badge> l = new ArrayList<>();
        for (Profil.Badge b : Profil.BADGES) if (Profil.obtenu(b, quetes(), niveau(), metiers())) l.add(b);
        return l;
    }

    @Override
    void contenu(GuiGraphics g) {
        String s = Economie.format(CosmetiqueScreen.rubis());
        texte(g, s, 528, 22, 1.3f, BLEU, 1, true);
        objet(g, CosmetiqueScreen.RUBIS_BLEU, 528 - font.width(Component.literal(s).withStyle(st -> st.withBold(true))) * 1.3f - 18, 18, 1);
        if (choisi != null && System.currentTimeMillis() - choisiA > 4000) choisi = null;
        String nom = minecraft.player == null ? "Joueur" : minecraft.player.getGameProfile().getName();
        banniere(g, 38, 64, 564, 86, Profil.get(0, choix(0)), Profil.get(1, choix(1)), Profil.get(2, choix(2)), Profil.get(3, choix(3)), nom, niveau(), badges());
        survol = null; survolBadge = null;
        for (int i = 0; i < ONGLETS.length; i++) {
            float y = 160 + i * 28;
            boolean sel = onglet == i, sur = dans(38, y, 150, y + 24);
            rect(g, 38, y, 150, y + 24, argb(sel ? 0x1E3A4A : sur ? 0x33265C : 0x2A1F4D, a));
            if (sel) rect(g, 38, y, 41, y + 24, argb(CYAN, a));
            texte(g, ONGLETS[i], 50, y + 8, .8f, sel ? CYAN : 0xE8E8EE, -1, true);
        }
        texte(g, "Purement visuel :", 94, 290, .5f, 0x8A8A9A, 0, false);
        texte(g, "aucun avantage en jeu", 94, 298, .5f, 0x8A8A9A, 0, false);
        texte(g, "Emote favorite : touche B", 94, 316, .5f, 0x8A8A9A, 0, false);
        texte(g, "ou /emote <nom>", 94, 324, .5f, 0x8A8A9A, 0, false);
        switch (onglet) {
            case 0 -> { ligneOptions(g, 0, "Fond", 160, 42); ligneOptions(g, 1, "Motif", 214, 62); ligneOptions(g, 2, "Emblème", 268, 36); }
            case 1 -> grades(g);
            case 2 -> grilleBadges(g);
            default -> emotes(g);
        }
    }

    // ---------------------------------------------------------------- la banniere (aussi utilisee pour les captures)
    void banniere(GuiGraphics g, float x, float y, float w, float h, Profil.Option fond, Profil.Option motif, Profil.Option emb, Profil.Option grade,
                  String nom, int nv, List<Profil.Badge> obtenus) {
        rect(g, x - 1, y - 1, x + w + 1, y + h + 1, argb(0x0A0714, a));
        int n = 64;
        for (int i = 0; i < n; i++) rect(g, x + w * i / n, y, x + w * (i + 1) / n + 1, y + h, argb(EcranAstralys.melange(fond.c1(), fond.c2(), i / (float) (n - 1)), a));
        g.fillGradient(Math.round(x), Math.round(y + h * .55f), Math.round(x + w), Math.round(y + h), argb(0, 0), argb(0x000000, .45f * a));
        motif(g, motif, x, y, w, h);
        rect(g, x, y, x + w, y + 1, argb(0xFFFFFF, .25f * a));
        // embleme dans son cadre
        float ex = x + 12, ey = y + h / 2 - 30;
        rect(g, ex - 2, ey - 2, ex + 62, ey + 62, argb(0xE2B65C, a));
        g.fillGradient(Math.round(ex), Math.round(ey), Math.round(ex + 60), Math.round(ey + 60), argb(0x2C1F55, a), argb(0x0A0714, a));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, ex + 30, ey + 30, 34, fond.c1(), .5f * a);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        objet(g, Metiers.pile(emb.objet()), ex + 6, ey + 6 + (float) Math.sin(t * 2) * 1.5f, 3);
        // nom, grade, niveau
        float tx = ex + 76;
        if (grade.prix() > 0) {
            String gt = grade.nom().toUpperCase(Locale.FRANCE);
            float gw = font.width(Component.literal(gt).withStyle(st -> st.withBold(true))) * .7f + 10;
            rect(g, tx, y + 14, tx + gw, y + 26, argb(grade.c1(), .9f * a));
            texte(g, gt, tx + 5, y + 16.5f, .7f, 0x0A0714, -1, true);
            tx += gw + 6;
        }
        texte(g, nom, tx, y + 12, 1.6f, 0xFFFFFF, -1, true);
        texte(g, "Niveau " + nv + "  •  " + quetes() + " quêtes terminées", ex + 76, y + 34, .65f, 0xE8E8EE, -1, false);
        // badges obtenus
        for (int i = 0; i < Math.min(12, obtenus.size()); i++) {
            Profil.Badge b = obtenus.get(i);
            float bx = ex + 84 + i * 22, by = y + h - 20;
            MetiersScreen.losange(g, bx, by, 16, b.couleur(), a);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            texte(g, b.symbole(), bx, by - 3.5f, .75f, 0x0A0714, 0, true);
        }
        if (obtenus.isEmpty()) texte(g, "Termine des quêtes pour gagner tes premiers badges", ex + 76, y + h - 24, .55f, 0xC8C8D0, -1, false);
    }

    private void motif(GuiGraphics g, Profil.Option m, float x, float y, float w, float h) {
        if (m == null || m.id().equals("aucun")) return;
        Random r = new Random(m.id().hashCode());
        int c = m.c1();
        RenderSystem.enableBlend();
        switch (m.id()) {
            case "etoiles" -> { for (int i = 0; i < 70; i++) { float px = x + r.nextFloat() * w, py = y + r.nextFloat() * h, k = .3f + .5f * (float) Math.abs(Math.sin(t * 1.5 + i)); rect(g, px, py, px + 1, py + 1, argb(c, k * a)); if (i % 7 == 0) texte(g, "✦", px, py - 3, .5f, c, 0, false); } }
            case "vagues" -> { for (int l = 0; l < 4; l++) for (int i = 0; i < w; i += 2) { float py = y + 14 + l * 20 + (float) Math.sin(i * .05 + l + t) * 4; rect(g, x + i, py, x + i + 2, py + 1, argb(c, .18f * a)); } }
            case "runes" -> { String[] rs = {"ᚠ", "ᚢ", "ᚦ", "ᚨ", "ᚱ", "ᚲ", "ᚷ", "ᚹ"}; for (int i = 0; i < 26; i++) texte(g, rs[r.nextInt(rs.length)], x + 90 + r.nextFloat() * (w - 100), y + 6 + r.nextFloat() * (h - 16), 1.2f, c, 0, false); }
            case "flammes" -> { for (int i = 0; i < w; i += 6) { float fh = 10 + (float) Math.abs(Math.sin(i * .37 + t * 3)) * 20 + r.nextFloat() * 6; g.fillGradient(Math.round(x + i), Math.round(y + h - fh), Math.round(x + i + 4), Math.round(y + h), argb(c, 0), argb(c, .4f * a)); } }
            case "cristaux" -> { for (int i = 0; i < 16; i++) { float px = x + 100 + r.nextFloat() * (w - 110), py = y + 10 + r.nextFloat() * (h - 20); MetiersScreen.losange(g, px, py, 6 + r.nextFloat() * 10, c, .35f * a); } RenderSystem.setShaderColor(1, 1, 1, 1); }
            default -> {   // constellation : etoiles reliees
                float[][] p = new float[9][];
                for (int i = 0; i < p.length; i++) p[i] = new float[] {x + 110 + i * (w - 140) / 8f, y + 12 + r.nextFloat() * (h - 30)};
                for (int i = 0; i + 1 < p.length; i++) {
                    int st = 12;
                    for (int k = 0; k < st; k++) { float px = p[i][0] + (p[i + 1][0] - p[i][0]) * k / st, py = p[i][1] + (p[i + 1][1] - p[i][1]) * k / st; rect(g, px, py, px + 1, py + 1, argb(c, .35f * a)); }
                }
                for (float[] q : p) texte(g, "✦", q[0], q[1] - 3.5f, .8f, c, 0, true);
            }
        }
    }

    // ---------------------------------------------------------------- options de banniere
    private void ligneOptions(GuiGraphics g, int type, String titre, float y, float pas) {
        section(g, titre, 162, y, CYAN);
        List<Profil.Option> l = Profil.du(type);
        for (int i = 0; i < l.size(); i++) {
            Profil.Option o = l.get(i);
            float x = 162 + i * pas, w = pas - 4;
            boolean sur = dans(x, y + 12, x + w, y + 46), sel = o.id().equals(choix(type)), conf = o.cle().equals(choisi);
            if (sur) survol = o;
            rect(g, x - 1, y + 11, x + w + 1, y + 47, argb(sel ? 0x22C55E : conf ? 0xF0B429 : sur ? CYAN : 0x0A0714, a));
            rect(g, x, y + 12, x + w, y + 46, argb(0x1B1530, a));
            if (type == 0) for (int k = 0; k < 8; k++) rect(g, x + 2 + (w - 4) * k / 8, y + 14, x + 2 + (w - 4) * (k + 1) / 8 + 1, y + 34, argb(EcranAstralys.melange(o.c1(), o.c2(), k / 7f), a));
            else if (type == 1) {
                rect(g, x + 2, y + 14, x + w - 2, y + 34, argb(0x2C1F55, a));
                if (!o.id().equals("aucun")) texte(g, switch (o.id()) { case "etoiles" -> "✦ ✧"; case "vagues" -> "≈≈"; case "runes" -> "ᚠᚱ"; case "flammes" -> "▲▲"; case "cristaux" -> "◆◇"; default -> "✦-✦"; },
                        x + w / 2, y + 20, 1, o.c1(), 0, true);
                else texte(g, "—", x + w / 2, y + 20, 1, 0x8A8A92, 0, true);
            } else objet(g, Metiers.pile(o.objet()), x + w / 2 - 8, y + 16, 1);
            if (sel) texte(g, "✔", x + w / 2, y + 37, .6f, 0x22C55E, 0, true);
            else if (a(o)) texte(g, "choisir", x + w / 2, y + 37, .45f, 0xB8AEDA, 0, false);
            else if (conf) texte(g, "OK ?", x + w / 2, y + 37, .55f, 0xF0B429, 0, true);
            else texte(g, Economie.format(o.prix()), x + w / 2, y + 37, .5f, CosmetiqueScreen.rubis() >= o.prix() ? BLEU : 0xF0645A, 0, true);
        }
    }

    // ---------------------------------------------------------------- grades
    private void grades(GuiGraphics g) {
        section(g, "Grades", 162, 160, CYAN);
        texte(g, "Ton grade s'affiche devant ton pseudo dans le chat et la liste des joueurs", 162, 172, .55f, 0x9A9AA4, -1, false);
        List<Profil.Option> l = Profil.du(3);
        String nom = minecraft.player == null ? "Joueur" : minecraft.player.getGameProfile().getName();
        for (int i = 0; i < l.size(); i++) {
            Profil.Option o = l.get(i);
            float x = 162 + i * 88, y = 186;
            boolean sur = dans(x, y, x + 84, y + 150), sel = o.id().equals(choix(3)), conf = o.cle().equals(choisi);
            if (sur) survol = o;
            carte(g, x, y, 84, 150, sel ? 0x22C55E : o.c1());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            halo(g, x + 42, y + 42, sur ? 34 : 28, o.c1(), .4f * a);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            MetiersScreen.losange(g, x + 42, y + 42, 34, o.c1(), a);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            texte(g, "★".repeat(Math.max(1, i)), x + 42, y + 38, .8f, 0x0A0714, 0, true);
            texte(g, o.nom().toUpperCase(Locale.FRANCE), x + 42, y + 70, .8f, o.c1(), 0, true);
            // exemple de message
            rect(g, x + 4, y + 86, x + 80, y + 108, argb(0x000000, .45f * a));
            String pre = o.prix() > 0 ? "[" + o.nom().toUpperCase(Locale.FRANCE) + "] " : "";
            texte(g, pre, x + 7, y + 90, .45f, o.c1(), -1, true);
            texte(g, "<" + nom + "> Salut !", x + 7 + font.width(Component.literal(pre).withStyle(st -> st.withBold(true))) * .45f, y + 90, .45f, 0xFFFFFF, -1, false);
            texte(g, "dans le chat", x + 7, y + 99, .4f, 0x8A8A92, -1, false);
            String et = sel ? "✔ ÉQUIPÉ" : a(o) ? "ÉQUIPER" : conf ? "CONFIRMER ?" : Economie.format(o.prix());
            texte(g, et, x + 42, y + 124, .75f, sel ? 0x22C55E : a(o) ? 0x3DD6C5 : conf ? 0xF0B429 : CosmetiqueScreen.rubis() >= o.prix() ? 0xFFFFFF : 0xF0645A, 0, true);
            if (!a(o) && !conf) objet(g, CosmetiqueScreen.RUBIS_BLEU, x + 34, y + 133, .6f);
        }
    }

    // ---------------------------------------------------------------- badges
    private void grilleBadges(GuiGraphics g) {
        section(g, "Badges de quêtes", 162, 160, CYAN);
        texte(g, "Ils se gagnent en jouant : quêtes, niveaux, métiers. Ils s'affichent sur ta bannière.", 162, 172, .55f, 0x9A9AA4, -1, false);
        int[] m = metiers();
        for (int i = 0; i < Profil.BADGES.size(); i++) {
            Profil.Badge b = Profil.BADGES.get(i);
            float x = 162 + (i % 6) * 74, y = 186 + (i / 6) * 76;
            boolean ok = Profil.obtenu(b, quetes(), niveau(), m), sur = dans(x, y, x + 70, y + 72);
            if (sur) survolBadge = b;
            carte(g, x, y, 70, 72, ok ? b.couleur() : 0);
            int c = ok ? b.couleur() : 0x4A4A66;
            if (ok) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                halo(g, x + 35, y + 24, 22 + 2 * (float) Math.sin(t * 2 + i), c, .45f * a);
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            MetiersScreen.losange(g, x + 35, y + 24, 28, c, a);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            texte(g, ok ? b.symbole() : "🔒", x + 35, y + 19, 1.1f, ok ? 0x0A0714 : 0x8A8A92, 0, true);
            texte(g, b.nom(), x + 35, y + 44, .5f, ok ? 0xFFFFFF : 0x8A8A92, 0, true);
            int val = switch (b.genre()) { case 0 -> quetes(); case 1 -> niveau(); case 2 -> java.util.Arrays.stream(m).max().orElse(0); default -> java.util.Arrays.stream(m).min().orElse(0); };
            barre(g, x + 8, y + 56, 54, 3, Math.min(1, val / (float) b.seuil()), c);
            texte(g, Math.min(val, b.seuil()) + " / " + b.seuil(), x + 35, y + 62, .42f, 0x9A9AA4, 0, false);
        }
    }

    // ---------------------------------------------------------------- emotes
    private void emotes(GuiGraphics g) {
        carte(g, 162, 160, 120, 180, CYAN);
        texte(g, "APERÇU", 170, 166, .6f, CYAN, -1, true);
        ClientCosm.Mannequin mq = ClientCosm.mannequin();
        String vu = survolEmote != null ? survolEmote.id() : apercuEmote;
        List<Profil.Option> l = Profil.du(4);
        if (mq != null) {
            mq.habiller();
            String e = vu != null ? vu : choix(4);
            if (!e.equals(mq.emote)) { mq.emote = e; mq.emoteDebut = System.currentTimeMillis(); }
            decoupe(g, 163, 161, 281, 339);
            ClientCosm.dessiner(g, 222, 318, 62, (float) Math.sin(t * .5f) * .4f, mq);
            g.disableScissor();
            Profil.Option o = Profil.get(4, mq.emote);
            if (o != null) texte(g, o.nom(), 222, 326, .7f, o.c1(), 0, true);
        }
        survolEmote = null;
        for (int i = 0; i < l.size(); i++) {
            Profil.Option o = l.get(i);
            float x = 290 + (i % 3) * 104, y = 160 + (i / 3) * 60;
            boolean sur = dans(x, y, x + 100, y + 56), fav = o.id().equals(choix(4)), conf = o.cle().equals(choisi);
            if (sur) { survol = o; survolEmote = o; }
            carte(g, x, y, 100, 56, fav ? 0xF0B429 : sur ? o.c1() : 0);
            texte(g, o.nom(), x + 8, y + 8, .75f, o.c1(), -1, true);
            if (fav) texte(g, "★ favorite", x + 92, y + 8, .5f, 0xF0B429, 1, true);
            if (a(o)) {
                texte(g, "clic : jouer", x + 8, y + 24, .55f, 0x3DD6C5, -1, false);
                texte(g, "maj + clic : favorite", x + 8, y + 34, .45f, 0x9A9AA4, -1, false);
            } else if (conf) texte(g, "CONFIRMER ?", x + 8, y + 28, .8f, 0xF0B429, -1, true);
            else {
                texte(g, Economie.format(o.prix()), x + 8, y + 30, .8f, CosmetiqueScreen.rubis() >= o.prix() ? 0xFFFFFF : 0xF0645A, -1, true);
                objet(g, CosmetiqueScreen.RUBIS_BLEU, x + 10 + font.width(Component.literal(Economie.format(o.prix())).withStyle(st -> st.withBold(true))) * .8f, y + 26, .7f);
            }
        }
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survolBadge != null) {
            g.renderComponentTooltip(font, List.of(Component.literal(survolBadge.nom()).withStyle(st -> st.withColor(survolBadge.couleur()).withBold(true)),
                    Component.literal(survolBadge.desc()).withStyle(st -> st.withColor(0xC8C8D0))), mx, my);
            return;
        }
        if (survol == null || onglet == 3) return;
        g.renderComponentTooltip(font, List.of(Component.literal(survol.nom()).withStyle(st -> st.withColor(survol.c1() == 0 ? 0xFFFFFF : survol.c1()).withBold(true)),
                Component.literal(a(survol) ? "Débloqué • clic pour choisir" : Economie.format(survol.prix()) + " rubis bleus • 2 clics pour acheter").withStyle(st -> st.withColor(0x9A9AA4))), mx, my);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (b != 0) return false;
        for (int i = 0; i < ONGLETS.length; i++)
            if (dans(38, 160 + i * 28, 150, 184 + i * 28)) { onglet = i; son(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f); return true; }
        if (survol == null) return false;
        Profil.Option o = survol;
        if (a(o)) {
            if (o.type() == 4 && !maj) { Metiers.CANAL.sendToServer(new Cosmetiques.Action(8, o.id())); apercuEmote = o.id(); son(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f); return true; }
            Metiers.CANAL.sendToServer(new Cosmetiques.Action(5, o.cle()));
            son(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
            return true;
        }
        // deux clics pour acheter : jamais d'achat par erreur
        if (!o.cle().equals(choisi)) { choisi = o.cle(); choisiA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); return true; }
        choisi = null;
        Metiers.CANAL.sendToServer(new Cosmetiques.Action(4, o.cle()));
        son(SoundEvents.PLAYER_LEVELUP, 1.4f);
        return true;
    }
}
