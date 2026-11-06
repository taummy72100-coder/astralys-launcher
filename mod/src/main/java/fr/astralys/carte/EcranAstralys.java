package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Base des ecrans facon Paladium (boutique, faction) : repere virtuel 640x360, panneau sombre, titre, bouton fermer, etincelles. */
abstract class EcranAstralys extends Screen {
    static final ResourceLocation HALO = new ResourceLocation(AstralysCarte.MODID, "textures/gui/halo.png");
    static final int VW = 640, VH = 360;
    private final int couleur;
    private final String titre;
    protected float k, x0, y0, vx, vy, t, a;
    private long debut, dernier;
    private final List<float[]> etincelles = new ArrayList<>();   // x, y, vie, duree, taille, doree
    protected final Random rnd = new Random();

    EcranAstralys(String titre, int couleur) { super(Component.literal(titre)); this.titre = titre; this.couleur = couleur; }

    @Override
    protected void init() {
        k = Math.min(width * .94f / VW, height * .94f / VH);
        x0 = (width - VW * k) / 2; y0 = (height - VH * k) / 2;
        if (debut == 0) { debut = dernier = System.nanoTime(); son(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f); }
    }

    static void son(SoundEvent s, float h) { Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, h)); }

    static int argb(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }

    void rect(GuiGraphics g, float x1, float y1, float x2, float y2, int argb) { g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb); }

    void texte(GuiGraphics g, String s, float x, float y, float e, int rgb, int aligne, boolean gras) {
        Component c = Component.literal(s).withStyle(st -> st.withBold(gras));
        float w = font.width(c) * e;
        g.pose().pushPose();
        g.pose().translate(x - (aligne == 0 ? w / 2 : aligne > 0 ? w : 0), y, 0);
        g.pose().scale(e, e, 1);
        g.drawString(font, c, 0, 0, rgb, true);
        g.pose().popPose();
    }

    void objet(GuiGraphics g, ItemStack s, float x, float y, float e) {
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(e, e, 1); g.renderItem(s, 0, 0); g.pose().popPose();
    }

    boolean dans(float x1, float y1, float x2, float y2) { return vx >= x1 && vx <= x2 && vy >= y1 && vy <= y2; }

    /** Ecran precedent (bouton RETOUR et Echap), ou null. */
    protected java.util.function.Supplier<Screen> retour;

    @Override
    public void onClose() {
        if (retour != null) minecraft.setScreen(retour.get()); else super.onClose();
    }

    /** Bande de couleur qui s'estompe vers la droite. */
    void ligne(GuiGraphics g, float x, float y, float w, float h, int c, float al) {
        int n = 24;
        for (int i = 0; i < n; i++) rect(g, x + w * i / n, y, x + w * (i + 1) / n, y + h, argb(c, al * a * (1 - i / (float) n)));
    }

    /** Carte en relief : degrade, liseré clair en haut, ombre en bas, barre d'accent a gauche si accent != 0. */
    void carte(GuiGraphics g, float x, float y, float w, float h, int accent) {
        rect(g, x - 1, y - 1, x + w + 1, y + h + 1, argb(0x0E0E11, a));
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), argb(0x2A2A31, a), argb(0x202026, a));
        rect(g, x, y, x + w, y + 1, argb(0xFFFFFF, .06f * a));
        if (accent != 0) { rect(g, x, y, x + 2, y + h, argb(accent, a)); ligne(g, x + 2, y, w * .6f, 1, accent, .6f); }
    }

    /** Barre de progression : rail sombre, remplissage en degrade, reflet et point lumineux au bout. */
    void barre(GuiGraphics g, float x, float y, float w, float h, float f, int c) {
        f = Math.max(0, Math.min(1, f));
        rect(g, x, y, x + w, y + h, argb(0x111114, a));
        rect(g, x, y + h - 1, x + w, y + h, argb(0xFFFFFF, .04f * a));
        if (f <= 0) return;
        int clair = melange(c, 0xFFFFFF, .35f);
        g.fillGradient(Math.round(x), Math.round(y), Math.round(x + w * f), Math.round(y + h), argb(clair, a), argb(c, a));
        rect(g, x, y, x + w * f, y + 1, argb(0xFFFFFF, .25f * a));
        float puls = .5f + .5f * (float) Math.sin(t * 3);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        halo(g, x + w * f, y + h / 2, h * 3.5f, c, (.35f + .25f * puls) * a);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    void halo(GuiGraphics g, float cx, float cy, float r, int c, float al) {
        RenderSystem.enableBlend();
        MetiersScreen.teinte(c, al);
        g.pose().pushPose(); g.pose().translate(cx - r, cy - r, 0); g.pose().scale(r * 2 / 64f, r * 2 / 64f, 1);
        g.blit(HALO, 0, 0, 0, 0, 64, 64, 64, 64); g.pose().popPose();
    }

    /** Titre de section : petit losange de couleur + texte espace. */
    void section(GuiGraphics g, String s, float x, float y, int c) {
        texte(g, "◆", x, y, .8f, c, -1, true);
        texte(g, s.toUpperCase(java.util.Locale.FRANCE), x + 10, y, .85f, 0xFFFFFF, -1, true);
    }

    /** Hexagone avec bord sombre (pointe en haut). */
    void hexBord(GuiGraphics g, float cx, float cy, float t, int c) {
        MetiersScreen.hexa(g, cx - t / 2 - 3, cy - t / 2 - 3, t + 6, 0x0E0E10, a);
        MetiersScreen.hexa(g, cx - t / 2, cy - t / 2, t, c, a);
        RenderSystem.setShaderColor(1, 1, 1, a);
    }

    static int melange(int c1, int c2, float f) {
        int r = (int) (((c1 >> 16) & 255) * (1 - f) + ((c2 >> 16) & 255) * f), gg = (int) (((c1 >> 8) & 255) * (1 - f) + ((c2 >> 8) & 255) * f), b = (int) ((c1 & 255) * (1 - f) + (c2 & 255) * f);
        return (r << 16) | (gg << 8) | b;
    }

    /** Contenu de l'ecran, dans le repere virtuel. */
    abstract void contenu(GuiGraphics g);

    /** Infobulle eventuelle, en coordonnees ecran (apres le contenu). */
    void infobulle(GuiGraphics g, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(.1f, (now - dernier) / 1e9f); dernier = now;
        t = (now - debut) / 1e9f; a = Math.min(1, t / .25f);
        vx = (mx - x0) / k; vy = (my - y0) / k;
        renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(x0, y0 + (1 - a) * 12, 0);
        g.pose().scale(k, k, 1);
        RenderSystem.enableBlend();
        rect(g, -4, -4, VW + 4, VH + 4, argb(0x0B0B0D, .95f * a));
        g.fillGradient(0, 0, VW, VH, argb(0x212128, .97f * a), argb(0x151519, .97f * a));
        ligne(g, 0, 0, VW, 2, couleur, .9f);
        // fines lignes de fond, a peine visibles
        for (int y = 64; y < VH; y += 24) rect(g, 0, y, VW, y + 1, argb(0xFFFFFF, .012f * a));
        ligne(g, 38, 58, 584, 1, couleur, .35f);
        MetiersScreen.hexa(g, -34, 128, 66, 0x0E0E10, a);
        MetiersScreen.hexa(g, -28, 134, 54, couleur, a);
        RenderSystem.setShaderColor(1, 1, 1, a);
        texte(g, "✦", -1, 150, 2.4f, 0xFFFFFF, 0, true);
        texte(g, titre.toUpperCase(java.util.Locale.FRANCE), 38, 26, 3.2f, couleur, -1, true);
        if (retour != null) {
            boolean s = dans(536, 16, 598, 34);
            rect(g, 536, 16, 598, 34, argb(s ? 0x34343C : 0x26262D, a));
            texte(g, "‹ RETOUR", 567, 21, .8f, s ? 0xFFFFFF : 0xC8C8D0, 0, true);
        }
        boolean surX = dans(604, 16, 622, 34);
        rect(g, 604, 16, 622, 34, argb(surX ? 0xFF5A50 : 0xE8322B, a));
        texte(g, "✕", 613, 21, 1, 0xFFFFFF, 0, true);
        contenu(g);
        // etincelles de la couleur de l'ecran qui montent du bas
        if (rnd.nextFloat() < dt * 18) etincelles.add(new float[] {rnd.nextFloat() * VW, VH - rnd.nextFloat() * 40, 0, 2 + rnd.nextFloat() * 2, 1.5f + rnd.nextFloat() * 1.5f, rnd.nextInt(4) == 0 ? 1 : 0});
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        etincelles.removeIf(p -> {
            p[2] += dt; p[1] -= dt * 14;
            float kk = p[2] / p[3], al = (kk < .2f ? kk / .2f : 1 - (kk - .2f) / .8f) * a * .8f;
            RenderSystem.enableBlend();
            MetiersScreen.teinte(p[5] > 0 ? 0xFFF0C8 : couleur, al);
            g.pose().pushPose(); g.pose().translate(p[0] - p[4] * 4, p[1] - p[4] * 4, 0); g.pose().scale(p[4] * 8 / 64f, p[4] * 8 / 64f, 1);
            g.blit(HALO, 0, 0, 0, 0, 64, 64, 64, 64); g.pose().popPose();
            return p[2] > p[3];
        });
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.pose().popPose();
        infobulle(g, mx, my);
    }

    /** Clic dans le repere virtuel ; rend vrai s'il est traite. */
    boolean clic(int bouton, boolean maj) { return false; }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        vx = (float) (mx - x0) / k; vy = (float) (my - y0) / k;
        if (dans(604, 16, 622, 34)) { retour = null; onClose(); return true; }
        if (retour != null && dans(536, 16, 598, 34)) { onClose(); return true; }
        return clic(b, hasShiftDown()) || super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
