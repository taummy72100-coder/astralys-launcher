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
        rect(g, 0, 0, VW, VH, argb(0x1B1B1F, .97f * a));
        rect(g, 0, 0, VW, 2, argb(0x2C2C33, a));
        MetiersScreen.hexa(g, -34, 128, 66, 0x0E0E10, a);
        MetiersScreen.hexa(g, -28, 134, 54, couleur, a);
        RenderSystem.setShaderColor(1, 1, 1, a);
        texte(g, "✦", -1, 150, 2.4f, 0xFFFFFF, 0, true);
        texte(g, titre.toUpperCase(java.util.Locale.FRANCE), 38, 26, 3.2f, couleur, -1, true);
        boolean surX = dans(604, 16, 622, 34);
        rect(g, 604, 16, 622, 34, argb(surX ? 0xFF5A50 : 0xE8322B, a));
        texte(g, "✕", 613, 21, 1, 0xFFFFFF, 0, true);
        contenu(g);
        // etincelles de la couleur de l'ecran qui montent du bas
        if (rnd.nextFloat() < dt * 18) etincelles.add(new float[] {rnd.nextFloat() * VW, VH - rnd.nextFloat() * 40, 0, 2 + rnd.nextFloat() * 2, 1.5f + rnd.nextFloat() * 1.5f, rnd.nextInt(4) == 0 ? 1 : 0});
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        etincelles.removeIf(p -> {
            p[2] += dt; p[1] -= dt * 14;
            float kk = p[2] / p[3], al = (kk < .2f ? kk / .2f : 1 - (kk - .2f) / .8f) * a * .8f;
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
        if (dans(604, 16, 622, 34)) { onClose(); return true; }
        return clic(b, hasShiftDown()) || super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
