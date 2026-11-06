package fr.astralys.carte;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Boutique de cosmetiques, facon vitrine : offre du mois (pack complet porte par un mannequin), essayage sur son personnage,
 * selection du jour renouvelee a minuit, et tout le catalogue par categorie. Prix en rubis bleus.
 */
public class CosmetiqueScreen extends EcranAstralys {
    private static final int ROSE = 0xFF4FD8, BLEU = 0x4FA8FF, CARTE = 0x1B1530, COLS = 6;
    static final ItemStack RUBIS_BLEU = Metiers.pile("astralyscarte:rubis_bleu");
    int onglet = -1;
    private int defile;   // -1 : selection du jour
    private String choisi;
    private long choisiA;
    private Cosmetiques.Cosm survol;
    private boolean surPack;

    public CosmetiqueScreen() {
        super("Cosmétiques", ROSE);
        retour = ShopScreen::new;
        if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Cosmetiques.Action(0, ""));
    }

    static long rubis() { return ClientCosm.vue == null ? 0 : ClientCosm.vue.eclats(); }
    private static List<String> possedes() { return ClientCosm.vue == null ? List.of() : ClientCosm.vue.possedes(); }
    private static boolean possede(Cosmetiques.Cosm c) { return possedes().contains(c.id()); }
    private static boolean porte(Cosmetiques.Cosm c) { return ClientCosm.vue != null && c.id().equals(ClientCosm.vue.portes().get(c.partie())); }
    private static LocalDate jour() { return LocalDate.now(Cosmetiques.PARIS); }

    /** Solde en rubis bleus, aligne a droite sur x. */
    void solde(GuiGraphics g, float x, float y) {
        String s = Economie.format(rubis());
        texte(g, s, x, y, 1.3f, BLEU, 1, true);
        objet(g, RUBIS_BLEU, x - font.width(Component.literal(s).withStyle(st -> st.withBold(true))) * 1.3f - 18, y - 4, 1);
    }

    /** Prix : nombre + rubis bleu + petit chariot. */
    void prix(GuiGraphics g, long p, float x, float y, float e, int c) {
        String s = Economie.format(p);
        texte(g, s, x, y, e, c, -1, true);
        float w = font.width(Component.literal(s).withStyle(st -> st.withBold(true))) * e;
        objet(g, RUBIS_BLEU, x + w + 1, y - 2, .55f * e / .7f);
    }

    void chariot(GuiGraphics g, float x, float y, int c) {
        int k = argb(c, a);
        rect(g, x, y, x + 2, y + 1, k); rect(g, x + 1, y + 1, x + 9, y + 2, k); rect(g, x + 2, y + 2, x + 9, y + 5, argb(c, .7f * a));
        rect(g, x + 2, y + 5, x + 9, y + 6, k); rect(g, x + 3, y + 7, x + 5, y + 9, k); rect(g, x + 7, y + 7, x + 9, y + 9, k);
    }

    /** Losange de rarete en haut a droite des cartes, qui scintille. */
    void gemme(GuiGraphics g, float cx, float cy, int r) {
        int c = Cosmetiques.COULEURS_RARETE[r];
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, cx, cy, 9 + (float) Math.sin(t * 3 + cx) * 1.5f, c, .45f * a);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        MetiersScreen.losange(g, cx, cy, 9, c, a);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        rect(g, cx - 1, cy - 2, cx, cy - 1, argb(0xFFFFFF, .8f * a));
    }

    private List<Cosmetiques.Cosm> liste() {
        if (onglet < 0) return Profil.selectionDuJour(jour());
        List<Cosmetiques.Cosm> l = new ArrayList<>();
        for (Cosmetiques.Cosm c : Cosmetiques.LISTE) if (c.categorie() == onglet) l.add(c);
        return l;
    }

    @Override
    void contenu(GuiGraphics g) {
        solde(g, 528, 22);
        texte(g, "Purement visuel • aucun avantage en jeu", 528, 40, .5f, 0x8A8A9A, 1, false);
        Arrays.fill(ClientCosm.apercu, null);
        if (choisi != null && System.currentTimeMillis() - choisiA > 4000) choisi = null;
        offre(g);
        // essayage : son propre personnage, avec la piece survolee
        carte(g, 406, 66, 196, 110, BLEU);
        texte(g, "ESSAYAGE", 414, 72, .6f, BLEU, -1, true);
        texte(g, survol != null ? survol.nom() : "survole un cosmétique", 504, 166, .5f, 0xB8AEDA, 0, false);
        if (survol != null) ClientCosm.apercu[survol.partie()] = survol.id();
        if (minecraft.player != null) {
            decoupe(g, 407, 67, 601, 175);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, 504, 160, 38, 504 - vx, 110 - vy, minecraft.player);
            g.disableScissor();
        }
        onglets(g);
        grille(g);
    }

    // ---------------------------------------------------------------- offre du mois
    private void offre(GuiGraphics g) {
        List<Cosmetiques.Cosm> pack = Profil.offreDuMois(jour());
        int c = pack.get(0).couleur();
        rect(g, 37, 65, 399, 177, argb(0x0A0714, a));
        g.fillGradient(38, 66, 398, 176, argb(EcranAstralys.melange(c, 0x1B1530, .35f), a), argb(EcranAstralys.melange(c, 0x0A0714, .75f), a));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, 130, 120, 70 + 4 * (float) Math.sin(t * 2), EcranAstralys.melange(c, 0xFFFFFF, .3f), .35f * a);
        for (int i = 0; i < 14; i++) {   // poussiere lumineuse
            float px = 40 + (i * 53 + t * 9 * (1 + i % 3)) % 356, py = 70 + (i * 37) % 100 + (float) Math.sin(t + i) * 3;
            halo(g, px, py, 3, 0xFFFFFF, .5f * a);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        texte(g, "OFFRE DU MOIS", 48, 74, .75f, 0xFFFFFF, -1, true);
        String mois = jour().getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.FRANCE);
        texte(g, "Ensemble de " + mois, 48, 85, .55f, 0xD8D8E0, -1, false);
        decoupe(g, 38, 66, 398, 176);
        ClientCosm.Mannequin m = ClientCosm.mannequin();
        if (m != null) {
            m.emote = null;
            m.habiller(pack.stream().map(Cosmetiques.Cosm::id).toArray(String[]::new));
            ClientCosm.dessiner(g, 130, 170, 40, (float) Math.sin(t * .6f) * .6f + .3f, m);
        }
        g.disableScissor();
        // encart prix
        float bx = 232, by = 92;
        surPack = dans(bx, by, bx + 156, by + 76);
        long prix = Cosmetiques.prixPack(pack, possedes()), plein = 0;
        for (Cosmetiques.Cosm x : pack) if (!possede(x)) plein += x.prix();
        rect(g, bx, by, bx + 156, by + 76, argb(0x0E0A1C, .92f * a));
        if (surPack) rect(g, bx, by, bx + 156, by + 1, argb(ROSE, a));
        for (int i = 0; i < pack.size(); i++) {
            Cosmetiques.Cosm x = pack.get(i);
            texte(g, (possede(x) ? "✔ " : "• ") + x.nom(), bx + 8, by + 7 + i * 9, .5f, possede(x) ? 0x22C55E : 0xE8E8EE, -1, false);
        }
        if (prix <= 0) texte(g, "✔ DÉJÀ À TOI", bx + 8, by + 50, 1, 0x22C55E, -1, true);
        else {
            texte(g, Economie.format(plein), bx + 8, by + 38, .55f, 0x8A8A9A, -1, false);
            rect(g, bx + 7, by + 41, bx + 9 + font.width(Economie.format(plein)) * .55f, by + 42, argb(0x8A8A9A, a));
            texte(g, "-" + (100 - Profil.REMISE_PACK) + " %", bx + 150, by + 38, .6f, 0x22C55E, 1, true);
            prix(g, prix, bx + 8, by + 52, 1.3f, 0xFFFFFF);
            boolean conf = "pack".equals(choisi);
            if (conf) texte(g, "CLIQUE POUR CONFIRMER", bx + 78, by + 68, .5f, 0xF0B429, 0, true);
            chariot(g, bx + 140, by + 55, conf ? 0xF0B429 : 0xFF9A3C);
        }
        texte(g, "★", 386, 70, 1.4f, 0xF0B429, 0, true);
    }

    // ---------------------------------------------------------------- onglets
    private float ox(int i) { return 38 + (i + 1) * 94; }

    private void onglets(GuiGraphics g) {
        for (int i = -1; i < Cosmetiques.CATEGORIES.length; i++) {
            float x = ox(i);
            boolean sel = onglet == i, s = dans(x, 184, x + 90, 200);
            rect(g, x, 184, x + 90, 200, argb(sel ? 0x3D2E5E : s ? 0x2E2452 : 0x211A3E, a));
            if (sel) rect(g, x, 199, x + 90, 200, argb(i < 0 ? 0xF0645A : ROSE, a));
            texte(g, i < 0 ? "SÉLECTION DU JOUR" : Cosmetiques.CATEGORIES[i].toUpperCase(Locale.FRANCE), x + 45, 189, .55f, sel ? 0xFFFFFF : 0xB8AEDA, 0, true);
        }
    }

    private static String compteARebours() {
        ZonedDateTime n = ZonedDateTime.now(Cosmetiques.PARIS);
        Duration d = Duration.between(n, n.toLocalDate().plusDays(1).atStartOfDay(Cosmetiques.PARIS));
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }

    // ---------------------------------------------------------------- cartes
    private void grille(GuiGraphics g) {
        List<Cosmetiques.Cosm> l = liste();
        survol = null;
        int lignes = onglet < 0 ? 1 : 2;
        defile = Math.max(0, Math.min(defile, (l.size() - 1) / COLS - lignes + 1));
        for (int i = defile * COLS; i < Math.min(l.size(), (defile + lignes) * COLS); i++) {
            int col = i % COLS, ran = i / COLS - defile;
            carteCosm(g, l.get(i), 38 + col * 95, 206 + ran * 70);
        }
        if (onglet < 0) {
            texte(g, "Nouvelle sélection dans", 320, 290, .7f, 0xB8AEDA, 0, false);
            texte(g, compteARebours(), 320, 302, 1.6f, 0xF0645A, 0, true);
            texte(g, "Survole pour essayer • clic : acheter (2 clics) ou équiper", 320, 330, .55f, 0x8A8A9A, 0, false);
        } else {
            int pages = (l.size() + COLS - 1) / COLS;
            if (pages > lignes) {
                float h = 134f * lignes / pages, y = 206 + 134f * defile / pages;
                rect(g, 606, 206, 609, 340, argb(0x33265C, a));
                rect(g, 606, y, 609, y + h, argb(ROSE, a));
                texte(g, l.size() + " cosmétiques • molette", 602, 343, .5f, 0x6E6E78, 1, false);
            }
        }
    }

    private void carteCosm(GuiGraphics g, Cosmetiques.Cosm c, float x, float y) {
        boolean s = dans(x, y, x + 88, y + 64), pos = possede(c), por = porte(c), sel = c.id().equals(choisi);
        if (s) survol = c;
        int rc = Cosmetiques.COULEURS_RARETE[c.rarete()];
        rect(g, x - 1, y - 1, x + 89, y + 65, argb(sel ? 0xF0B429 : s ? rc : 0x0A0714, a));
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + 88), Math.round(y + 44), argb(EcranAstralys.melange(CARTE, rc, s ? .22f : .1f), a), argb(CARTE, a));
        // mannequin cadre sur la partie portee
        decoupe(g, x, y, x + 88, y + 44);
        ClientCosm.Mannequin m = ClientCosm.mannequin();
        if (m != null) {
            m.emote = null; m.habiller(c.id());
            float ang = s ? (float) Math.sin(t * 2) * .8f + .45f : .45f;
            if (c.partie() == 0) ClientCosm.dessiner(g, x + 44, y + 92, 42, ang, m);
            else if (c.partie() >= 2) ClientCosm.dessiner(g, x + 44, y + 40, 34, ang, m);
            else ClientCosm.dessiner(g, x + 44, y + 70, 28, ang, m);
        }
        g.disableScissor();
        gemme(g, x + 80, y + 8, c.rarete());
        // bandeau du bas
        rect(g, x, y + 44, x + 88, y + 64, argb(0x120E22, a));
        String nom = c.nom();
        int tiret = nom.indexOf(" — ");
        texte(g, tiret > 0 ? nom.substring(0, tiret) : nom, x + 4, y + 47, .45f, 0xE8E8EE, -1, false);
        if (tiret > 0) texte(g, nom.substring(tiret + 3), x + 84, y + 47, .45f, rc, 1, true);
        if (por) texte(g, "PORTÉ ✔", x + 4, y + 55, .6f, 0x22C55E, -1, true);
        else if (pos) texte(g, "ÉQUIPER", x + 4, y + 55, .6f, 0x3DD6C5, -1, true);
        else if (sel) texte(g, "CONFIRMER ?", x + 4, y + 55, .6f, 0xF0B429, -1, true);
        else {
            prix(g, c.prix(), x + 4, y + 55, .6f, rubis() >= c.prix() ? 0xFFFFFF : 0xF0645A);
            chariot(g, x + 76, y + 53, 0xFF9A3C);
        }
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survol == null) return;
        g.renderComponentTooltip(font, List.of(Component.literal(survol.nom()).withStyle(st -> st.withColor(Cosmetiques.COULEURS_RARETE[survol.rarete()]).withBold(true)),
                Component.literal(Cosmetiques.RARETES[survol.rarete()] + " • " + Cosmetiques.CATEGORIES[survol.categorie()]).withStyle(st -> st.withColor(0x9A9AA4)),
                Component.literal("Purement visuel : aucun avantage en jeu").withStyle(st -> st.withColor(0x6E6E78).withItalic(true))), mx, my);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (b != 0) return false;
        for (int i = -1; i < Cosmetiques.CATEGORIES.length; i++)
            if (dans(ox(i), 184, ox(i) + 90, 200)) { onglet = i; defile = 0; son(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f); return true; }
        if (surPack && Cosmetiques.prixPack(Profil.offreDuMois(jour()), possedes()) > 0) {
            if (!"pack".equals(choisi)) { choisi = "pack"; choisiA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); return true; }
            choisi = null;
            Metiers.CANAL.sendToServer(new Cosmetiques.Action(6, ""));
            son(SoundEvents.PLAYER_LEVELUP, 1.4f);
            return true;
        }
        if (survol == null) return false;
        Cosmetiques.Cosm c = survol;
        int action;
        if (porte(c)) action = 3;
        else if (possede(c)) action = 2;
        else {
            // deux clics pour acheter : jamais d'achat par erreur
            if (!c.id().equals(choisi)) { choisi = c.id(); choisiA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); return true; }
            action = 1; choisi = null;
        }
        Metiers.CANAL.sendToServer(new Cosmetiques.Action(action, c.id()));
        son(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) { defile -= (int) Math.signum(d); return true; }

    @Override
    public void removed() { Arrays.fill(ClientCosm.apercu, null); super.removed(); }
}
