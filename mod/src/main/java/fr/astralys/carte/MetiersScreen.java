package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ecran des metiers : 4 ecussons d'Astralys (losanges a filet dore), niveaux, 3 quetes quotidiennes, particules magiques. */
public class MetiersScreen extends Screen {
    private static final ResourceLocation HEX = new ResourceLocation(AstralysCarte.MODID, "textures/gui/hex.png");
    private static final ResourceLocation HEX_DEG = new ResourceLocation(AstralysCarte.MODID, "textures/gui/hex_degrade.png");
    private static final ResourceLocation HALO = new ResourceLocation(AstralysCarte.MODID, "textures/gui/halo.png");
    // Repere virtuel de l'ecran, mis a l'echelle de la fenetre.
    private static final int VW = 640, VH = 360;
    private static final int[] CX = {110, 250, 390, 530};
    static final ItemStack[][] ART = {
            {new ItemStack(Items.GOLDEN_PICKAXE), new ItemStack(Items.IRON_ORE)},
            {new ItemStack(Items.GOLDEN_HOE), new ItemStack(Items.HAY_BLOCK)},
            {new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SPAWNER)},
            {new ItemStack(Items.DRAGON_BREATH), new ItemStack(Items.BREWING_STAND)}};
    private static final ItemStack[] PETITE = {new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.WHEAT), new ItemStack(Items.ARROW), new ItemStack(Items.POTION)};
    private static final ItemStack[] QUETE = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.WHEAT), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GLASS_BOTTLE)};

    private static final class Etincelle { float x, y, vx, vy, vie, duree, taille; int couleur; boolean glyphe; char c; }

    private final List<Etincelle> etincelles = new ArrayList<>();
    private final Random rnd = new Random();
    private float k, x0, y0;
    private long debut, dernier;

    public MetiersScreen() { super(Component.literal("Métiers")); }

    @Override
    protected void init() {
        k = Math.min(width * .94f / VW, height * .94f / VH);
        x0 = (width - VW * k) / 2; y0 = (height - VH * k) / 2;
        if (debut == 0) {
            debut = dernier = System.nanoTime();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.1f));
        }
    }

    static void teinte(int rgb, float a) {
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, a);
    }

    /** Hexagone plein (pointe en haut) de cote d'encadrement t, teinte rgb. */
    private static final ResourceLocation LOS = new ResourceLocation(AstralysCarte.MODID, "textures/gui/losange.png");
    private static final ResourceLocation LOS_DEG = new ResourceLocation(AstralysCarte.MODID, "textures/gui/losange_degrade.png");
    private static final ResourceLocation LOS_FILET = new ResourceLocation(AstralysCarte.MODID, "textures/gui/losange_filet.png");
    private static final ResourceLocation ROND = new ResourceLocation(AstralysCarte.MODID, "textures/gui/rond.png");
    private static final ResourceLocation ROND_DEG = new ResourceLocation(AstralysCarte.MODID, "textures/gui/rond_degrade.png");
    static final int FILET = 0xF0D28A;
    private static final ResourceLocation ILLUS = new ResourceLocation(AstralysCarte.MODID, "textures/gui/metiers.png");

    /** Illustration pixel art du metier m (planche 128x32), centree, de cote t. */
    static void illustration(GuiGraphics g, int m, float cx, float cy, float t, float a) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, a);
        g.pose().pushPose(); g.pose().translate(cx - t / 2, cy - t / 2, 0); g.pose().scale(t / 32f, t / 32f, 1);
        g.blit(ILLUS, 0, 0, m * 32, 0, 32, 32, 128, 32);
        g.pose().popPose();
    }

    /** Ecusson d'Astralys (losange) : bord sombre, anneau de couleur, filet dore, interieur en degrade. */
    static void ecusson(GuiGraphics g, float cx, float cy, float b, int c, float a) {
        tex(g, LOS, cx - b / 2 - 6, cy - b / 2 - 6, b + 12, 0x0E0E10, a);
        tex(g, LOS, cx - b / 2, cy - b / 2, b, c, a);
        tex(g, LOS_DEG, cx - b * .41f, cy - b * .41f, b * .82f, c, a);
        tex(g, LOS_FILET, cx - b * .45f, cy - b * .45f, b * .9f, FILET, a * .85f);
        RenderSystem.setShaderColor(1, 1, 1, a);
    }

    /** Petit losange plein avec bord sombre (icones de quetes, paliers). */
    static void losange(GuiGraphics g, float cx, float cy, float b, int c, float a) {
        tex(g, LOS, cx - b / 2 - 3, cy - b / 2 - 3, b + 6, 0x0E0E10, a);
        tex(g, LOS, cx - b / 2, cy - b / 2, b, c, a);
        tex(g, LOS_FILET, cx - b * .45f, cy - b * .45f, b * .9f, FILET, a * .6f);
        RenderSystem.setShaderColor(1, 1, 1, a);
    }

    /** Medaillon rond du niveau, pose sur la pointe basse de l'ecusson. */
    static void medaillon(GuiGraphics g, net.minecraft.client.gui.Font font, float cx, float cy, float r, int c, String haut, String nombre, float a) {
        tex(g, ROND, cx - r - 3, cy - r - 3, 2 * r + 6, 0x0E0E10, a);
        tex(g, ROND, cx - r, cy - r, 2 * r, FILET, a);
        tex(g, ROND_DEG, cx - r + 2, cy - r + 2, 2 * r - 4, c, a);
        RenderSystem.setShaderColor(1, 1, 1, a);
        float e = r / 17f;
        Component h = Component.literal(haut).withStyle(st -> st.withBold(true)), n = Component.literal(nombre).withStyle(st -> st.withBold(true));
        float en = Math.min(1.7f * e, (2 * r - 8) / Math.max(1, font.width(n)));
        float eh = Math.min(.6f * e, (2 * r - 12) / Math.max(1, font.width(h)));   // "NIVEAU" reste dans le cercle
        g.pose().pushPose(); g.pose().translate(cx - font.width(h) * eh / 2, cy - r * .58f, 0); g.pose().scale(eh, eh, 1);
        g.drawString(font, h, 0, 0, 0xFFFFFF, true); g.pose().popPose();
        g.pose().pushPose(); g.pose().translate(cx - font.width(n) * en / 2, cy - r * .22f, 0); g.pose().scale(en, en, 1);
        g.drawString(font, n, 0, 0, 0xFFFFFF, true); g.pose().popPose();
    }

    static void hexa(GuiGraphics g, float x, float y, float t, int rgb, float a) {
        tex(g, HEX, x, y, t, rgb, a);
    }

    private static void tex(GuiGraphics g, ResourceLocation r, float x, float y, float t, int rgb, float a) {
        RenderSystem.enableBlend();   // renderItem le coupe : sans lui, lueurs pleines et hexagones crenelés
        teinte(rgb, a);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(t / 128f, t / 128f, 1);
        g.blit(r, 0, 0, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
    }

    private void texte(GuiGraphics g, String s, float x, float y, float e, int rgb, boolean centre, boolean gras) {
        Component c = Component.literal(s).withStyle(st -> st.withBold(gras));
        g.pose().pushPose();
        g.pose().translate(x - (centre ? font.width(c) * e / 2 : 0), y, 0);
        g.pose().scale(e, e, 1);
        g.drawString(font, c, 0, 0, rgb, true);
        g.pose().popPose();
    }

    private void objet(GuiGraphics g, ItemStack s, float x, float y, float e) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(e, e, 1);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    private static int argb(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }

    private void rect(GuiGraphics g, float x1, float y1, float x2, float y2, int argb) {
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    // ------------------------------------------------------------------ particules magiques
    private void animer(float dt) {
        for (int m = 0; m < 4; m++) {
            if (rnd.nextFloat() < dt * 9) {
                Etincelle p = new Etincelle();
                double a = rnd.nextDouble() * Math.PI * 2; float r = 40 + rnd.nextFloat() * 14;
                p.x = CX[m] + (float) Math.cos(a) * r; p.y = 170 + (float) Math.sin(a) * r;
                p.vx = (rnd.nextFloat() - .5f) * 6; p.vy = -10 - rnd.nextFloat() * 16;
                p.duree = 1.4f + rnd.nextFloat() * 1.2f; p.taille = 1.5f + rnd.nextFloat() * 1.5f;
                p.couleur = rnd.nextInt(3) == 0 ? 0xFFF0C8 : Metiers.COULEURS[m];
                etincelles.add(p);
            }
            if (rnd.nextFloat() < dt * 2.5f) {
                Etincelle p = new Etincelle();
                p.glyphe = true; p.c = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾ✦✧".charAt(rnd.nextInt(12));
                p.x = CX[m] + (rnd.nextFloat() - .5f) * 90; p.y = 205 + rnd.nextFloat() * 20;
                p.vx = 0; p.vy = -14 - rnd.nextFloat() * 8; p.duree = 2 + rnd.nextFloat();
                p.couleur = Metiers.COULEURS[m];
                etincelles.add(p);
            }
        }
        etincelles.removeIf(p -> { p.vie += dt; p.x += p.vx * dt; p.y += p.vy * dt; return p.vie > p.duree; });
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(.1f, (now - dernier) / 1e9f); dernier = now;
        float t = (now - debut) / 1e9f, a = Math.min(1, t / .25f);
        animer(dt);
        renderBackground(g);
        float vx = (mx - x0) / k, vy = (my - y0) / k;

        g.pose().pushPose();
        g.pose().translate(x0, y0 + (1 - a) * 12, 0);
        g.pose().scale(k, k, 1);
        RenderSystem.enableBlend();

        // panneau
        rect(g, -4, -4, VW + 4, VH + 4, argb(0x0B0B0D, .95f * a));
        rect(g, 0, 0, VW, VH, argb(0x1B1B1F, .97f * a));
        rect(g, 0, 0, VW, 2, argb(0x2C2C33, a));
        // hexagone d'Astralys sur le bord gauche
        hexa(g, -34, 128, 66, 0x0E0E10, a);
        hexa(g, -28, 134, 54, 0xE8322B, a);
        RenderSystem.setShaderColor(1, 1, 1, a);
        texte(g, "✦", -1, 150, 2.4f, 0xFFFFFF, true, true);
        // titre
        texte(g, "MÉTIERS", 38, 26, 3.2f, 0xE8322B, false, true);
        // fermer
        boolean surX = vx >= 604 && vx <= 622 && vy >= 16 && vy <= 34;
        rect(g, 604, 16, 622, 34, argb(surX ? 0xFF5A50 : 0xE8322B, a));
        texte(g, "✕", 613, 21, 1, 0xFFFFFF, true, true);

        Metiers.Vue v = ClientMetiers.vue;
        int survol = -1;
        for (int m = 0; m < 4; m++) {
            int c = Metiers.COULEURS[m];
            long xp = v == null ? 0 : v.xp()[m];
            int nv = Metiers.niveau(xp);
            boolean s = Math.abs(vx - CX[m]) < 52 && vy > 80 && vy < 250;
            if (s) survol = m;
            float puls = (float) Math.sin(t * 2.2f + m) * .5f + .5f;
            texte(g, Metiers.NOMS[m], CX[m], 84, 2, 0xFFFFFF, true, true);
            // petite icone sur un rond sombre
            tex(g, ROND, CX[m] - 13, 103, 26, 0x111114, a);
            RenderSystem.setShaderColor(1, 1, 1, a);
            objet(g, PETITE[m], CX[m] - 8, 109, 1);
            // halo additif qui respire
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            tex(g, HALO, CX[m] - 70, 100, 140, c, (s ? .55f : .22f + puls * .12f) * a);
            RenderSystem.defaultBlendFunc();
            // ecusson d'Astralys et ses etoiles aux pointes
            float b = s ? 110 : 104;
            ecusson(g, CX[m], 168, b, c, a);
            for (int k = 0; k < 2; k++) {
                float ps = .5f + .5f * (float) Math.sin(t * 3 + m + k * 2);
                texte(g, "✦", CX[m] + (k == 0 ? -1 : 1) * (b / 2 + 10), 164, .8f + .3f * ps, argb(FILET, (.4f + .6f * ps) * a), true, false);
            }
            float bob = (float) Math.sin(t * 1.8f + m * 1.3f) * 2;
            illustration(g, m, CX[m], 166 + bob, 64, a);
            // medaillon du niveau sur la pointe basse
            medaillon(g, font, CX[m], 218, 17, c, "NIVEAU", String.valueOf(nv), a);
            // barre d'XP du niveau en cours
            float f = nv >= Metiers.NIVEAU_MAX ? 1 : Metiers.reste(xp) / (float) Metiers.cout(nv);
            rect(g, CX[m] - 40, 242, CX[m] + 40, 245, argb(0x34343B, a));
            rect(g, CX[m] - 40, 242, CX[m] - 40 + 80 * f, 245, argb(c, a));
        }

        // quetes quotidiennes
        texte(g, "Q U Ê T E S   Q U O T I D I E N N E S", 38, 262, 1, 0xFFFFFF, false, true);
        String bonus = v == null ? "0" : String.format(Locale.FRANCE, "%.1f", v.bonus());
        String droite = "BONUS QUÊTE   " + bonus + "%   |   MULTIPLICATEUR EXP. MÉTIER   " + bonus + "%";
        texte(g, droite, 604 - font.width(droite) * .8f, 263, .8f, 0xD8D8DE, false, false);
        for (int i = 0; i < 3; i++) {
            float x = 30 + i * 196, y = 280;
            rect(g, x, y, x + 188, y + 70, argb(0x222228, a));
            if (v == null || i >= v.quetes().size()) { texte(g, "Chargement…", x + 94, y + 30, 1, 0x8A8A92, true, false); continue; }
            Metiers.QueteVue q = v.quetes().get(i);
            int c = Metiers.COULEURS[q.metier()];
            boolean fini = q.avance() >= q.nombre();
            losange(g, x + 32, y + 35, 40, fini ? 0x22C55E : c, a);
            RenderSystem.setShaderColor(1, 1, 1, a);
            illustration(g, q.metier(), x + 32, y + 35, 24, a);
            String ti = q.titre().toUpperCase(Locale.FRANCE);
            float et = Math.min(.8f, 74f / Math.max(1, font.width(Component.literal(ti).withStyle(st -> st.withBold(true)))));
            texte(g, ti, x + 62, y + 12, et, 0xFFFFFF, false, true);
            List<net.minecraft.util.FormattedCharSequence> l = font.split(Component.literal(q.desc().toUpperCase(Locale.FRANCE)), 150);
            for (int j = 0; j < Math.min(2, l.size()); j++) {
                g.pose().pushPose(); g.pose().translate(x + 62, y + 25 + j * 8, 0); g.pose().scale(.75f, .75f, 1);
                g.drawString(font, l.get(j), 0, 0, 0xC8C8D0, false); g.pose().popPose();
            }
            float f = q.avance() / (float) q.nombre();
            rect(g, x + 62, y + 52, x + 152, y + 55, argb(0x3A3A42, a));
            rect(g, x + 62, y + 52, x + 62 + 90 * f, y + 55, argb(0x22C55E, a));
            texte(g, fini ? "✔" : q.avance() + "/" + q.nombre(), x + 158, y + 50, .75f, fini ? 0x22C55E : 0xFFFFFF, false, true);
            // recompense
            rect(g, x + 140, y + 6, x + 182, y + 18, argb(0x34343B, a));
            texte(g, "✦ " + q.recompense(), x + 161, y + 8, .7f, c, true, true);
        }

        // particules par-dessus
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (Etincelle p : etincelles) {
            float kk = p.vie / p.duree, al = (kk < .2f ? kk / .2f : 1 - (kk - .2f) / .8f) * a;
            if (!p.glyphe) tex(g, HALO, p.x - p.taille * 4, p.y - p.taille * 4, p.taille * 8, p.couleur, al * .9f);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        for (Etincelle p : etincelles) {
            float kk = p.vie / p.duree, al = (kk < .2f ? kk / .2f : 1 - (kk - .2f) / .8f) * a;
            if (p.glyphe) { if (al > .05f) texte(g, String.valueOf(p.c), p.x, p.y, .8f, argb(p.couleur, al * .8f), true, false); }
            else rect(g, p.x - p.taille / 2, p.y - p.taille / 2, p.x + p.taille / 2, p.y + p.taille / 2, argb(0xFFFFFF, al));
        }
        g.pose().popPose();

        if (survol >= 0 && v != null) {
            final int m = survol;
            long xp = v.xp()[m];
            int nv = Metiers.niveau(xp);
            List<Component> info = List.of(
                    Component.literal(Metiers.NOMS[m] + " — niveau " + nv).withStyle(st -> st.withColor(Metiers.COULEURS[m]).withBold(true)),
                    Component.literal(nv >= Metiers.NIVEAU_MAX ? "Niveau maximum !" : "XP : " + Metiers.reste(xp) + " / " + Metiers.cout(nv)).withStyle(st -> st.withColor(0xE8E8EE)),
                    Component.literal(COMMENT[m]).withStyle(st -> st.withColor(0x9A9AA4).withItalic(true)),
                    Component.literal("▶ Clique pour voir les détails des niveaux").withStyle(st -> st.withColor(Metiers.COULEURS[m])));
            g.renderComponentTooltip(font, info, mx, my);
        }
    }

    private static final String[] COMMENT = {"Casse des minerais (Sylvaria !)", "Récolte des cultures mûres", "Tue des créatures", "Prépare des potions"};

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        float vx = (float) (mx - x0) / k, vy = (float) (my - y0) / k;
        if (vx >= 604 && vx <= 622 && vy >= 16 && vy <= 34) { onClose(); return true; }
        for (int m = 0; m < 4; m++)
            if (Math.abs(vx - CX[m]) < 52 && vy > 80 && vy < 250) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
                Minecraft.getInstance().setScreen(new MetierScreen(m));
                return true;
            }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
