package fr.astralys.carte;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

/**
 * Vie, nourriture, armure et experience au style du grimoire (comme la barre d'action) : memes places que le jeu,
 * barres dans un cadre noir et dore, boucliers d'armure en gemmes violettes, niveau en or.
 */
final class HudMagie {
    private static final ResourceLocation TEX = new ResourceLocation(AstralysCarte.MODID, "textures/gui/hud_magie.png");
    private static final int NOIR = 0xFF0A0714, OR = 0xFFE2B65C, OR_S = 0xFF845C28, FOND = 0xFF150F2C;

    static void init() { MinecraftForge.EVENT_BUS.addListener(HudMagie::dessiner); }

    private static void dessiner(RenderGuiOverlayEvent.Pre e) {
        NamedGuiOverlay o = e.getOverlay();
        boolean vie = o.id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()), armure = o.id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id()),
                faim = o.id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()), xp = o.id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id());
        if (!vie && !armure && !faim && !xp) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui instanceof ForgeGui gui) || !(mc.getCameraEntity() instanceof Player p) || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        if (xp && mc.player != null && mc.player.jumpableVehicle() != null) return;              // a cheval : le jeu montre la barre de saut a cet endroit
        if (faim && p.getVehicle() instanceof LivingEntity) return;   // sur une monture : sa vie remplace la nourriture
        e.setCanceled(true);
        GuiGraphics g = e.getGuiGraphics();
        int cx = e.getWindow().getGuiScaledWidth() / 2, h = e.getWindow().getGuiScaledHeight();
        float t = (System.nanoTime() % 100_000_000_000L) / 1e9f;
        RenderSystem.enableBlend();
        if (vie) {
            int y = h - gui.leftHeight;
            float max = Math.max(1, p.getMaxHealth()), abs = p.getAbsorptionAmount(), total = max + abs;
            int c1 = 0xFF4A5A, c2 = 0xB0182A;
            if (p.hasEffect(MobEffects.POISON)) { c1 = 0x9AD94A; c2 = 0x4E7A1E; }
            else if (p.hasEffect(MobEffects.WITHER)) { c1 = 0x6A6070; c2 = 0x2A2430; }
            else if (p.isFullyFrozen()) { c1 = 0xBFF0FF; c2 = 0x4FA8D8; }
            boolean danger = p.getHealth() <= max * .2f;
            barre(g, cx - 91, y, p.getHealth() / total, abs / total, c1, c2, false, danger ? t : -1, p.hurtTime > 0, (int) (total / 2));
            g.blit(TEX, cx - 91 - 1, y, 27, 0, 9, 9, 64, 16);
            gui.leftHeight += 10;
        } else if (armure) {
            int a = p.getArmorValue();
            if (a > 0) {
                int y = h - gui.leftHeight;
                for (int i = 0; i < 10; i++) g.blit(TEX, cx - 91 + i * 8, y, i * 2 + 1 < a ? 0 : i * 2 + 1 == a ? 9 : 18, 0, 9, 9, 64, 16);
                gui.leftHeight += 10;
            }
        } else if (faim) {
            int y = h - gui.rightHeight;
            var f = p.getFoodData();
            boolean faimEffet = p.hasEffect(MobEffects.HUNGER);
            barre(g, cx + 91 - 81, y, f.getFoodLevel() / 20f, 0, faimEffet ? 0x9AD94A : 0xF0A040,
                    faimEffet ? 0x4E7A1E : 0x9A4A14, true, f.getFoodLevel() <= 6 ? t : -1, false, 10);
            g.blit(TEX, cx + 91 - 8, y, 36, 0, 9, 9, 64, 16);
            gui.rightHeight += 10;
        } else experience(g, mc, p, cx, h, t);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * Barre de 81 px de large et 9 de haut ; l'icone (9 px) occupe un bout. part : rempli (0 a 1), bonus : absorption doree apres le rempli,
     * inverse : rempli depuis la droite (nourriture), alerte : temps pour faire pulser le cadre en rouge (-1 : non), coups : nombre de traits.
     */
    private static void barre(GuiGraphics g, int x, int y, float part, float bonus, int c1, int c2, boolean inverse, float alerte, boolean blesse, int coups) {
        int x1 = inverse ? x : x + 9, x2 = inverse ? x + 72 : x + 81;
        g.fill(x1 - 1, y, x2 + 1, y + 9, NOIR);
        int cadre = alerte >= 0 && Math.sin(alerte * 8) > 0 ? 0xFFE8322B : OR;
        g.fill(x1, y + 1, x2, y + 2, cadre); g.fill(x1, y + 7, x2, y + 8, alerte >= 0 ? cadre : OR_S);
        g.fill(x1, y + 2, x1 + 1, y + 7, cadre); g.fill(x2 - 1, y + 2, x2, y + 7, OR_S);
        int ix1 = x1 + 1, ix2 = x2 - 1, w = ix2 - ix1;
        g.fill(ix1, y + 2, ix2, y + 7, FOND);
        int n = Math.round(w * Math.max(0, Math.min(1, part))), b = Math.round(w * Math.max(0, Math.min(1 - part, bonus)));
        int a1 = inverse ? ix2 - n : ix1, a2 = inverse ? ix2 : ix1 + n;
        if (n > 0) {
            g.fillGradient(a1, y + 2, a2, y + 7, 0xFF000000 | EcranAstralys.melange(c1, 0xFFFFFF, .15f), 0xFF000000 | c2);
            g.fill(a1, y + 2, a2, y + 3, 0x55FFFFFF);   // reflet
            g.fill(inverse ? a1 : a2 - 1, y + 2, inverse ? a1 + 1 : a2, y + 7, 0xAAFFFFFF);   // pointe lumineuse
        }
        if (b > 0) g.fillGradient(a2, y + 2, a2 + b, y + 7, 0xFFFFE08A, 0xFFC88A10);
        if (blesse) g.fill(ix1, y + 2, ix2, y + 7, 0x66FFFFFF);
        for (int k = 1; k < coups; k++) {   // graduations : un trait par coeur (2 points de vie)
            int tx = ix1 + Math.round(w * k / (float) coups);
            g.fill(tx, y + 5, tx + 1, y + 7, 0x40000000);
        }
    }

    private static void experience(GuiGraphics g, Minecraft mc, Player p, int cx, int h, float t) {
        int x = cx - 91, y = h - 29;
        g.fill(x, y, x + 182, y + 5, NOIR);
        g.fill(x + 1, y + 1, x + 181, y + 4, FOND);
        g.fill(x + 1, y + 1, x + 181, y + 2, 0xFF2C1F52);
        int n = Math.round(180 * p.experienceProgress);
        if (n > 0) {
            g.fillGradient(x + 1, y + 1, x + 1 + n, y + 4, 0xFFC9A0FF, 0xFF6E3CC8);
            g.fill(x + 1, y + 1, x + 1 + n, y + 2, 0x66FFFFFF);
            float puls = .5f + .5f * (float) Math.sin(t * 4);
            g.fill(x + n - 1, y, x + n + 2, y + 5, ((int) (120 + 120 * puls) << 24) | 0xFFECAA);   // etincelle au bout
        }
        for (int k = 1; k < 18; k++) g.fill(x + 1 + k * 10, y + 1, x + 2 + k * 10, y + 4, 0x50000000);   // 18 alveoles
        g.fill(x - 1, y + 1, x, y + 4, OR); g.fill(x + 182, y + 1, x + 183, y + 4, OR);   // embouts dores
        if (p.experienceLevel > 0) {
            String s = String.valueOf(p.experienceLevel);
            int tx = cx - mc.font.width(s) / 2, ty = h - 35;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) g.drawString(mc.font, s, tx + d[0], ty + d[1], 0x2A1A00, false);
            g.drawString(mc.font, s, tx, ty, 0xFFE08A, false);
        }
    }

    private HudMagie() {}
}
