package fr.astralys.carte;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

/** Barre d'action au style du grimoire : meme place et memes dimensions que celle du jeu. */
final class BarreMagie {
    private static final ResourceLocation TEX = new ResourceLocation(AstralysCarte.MODID, "textures/gui/hotbar_magie.png");

    static void init() { MinecraftForge.EVENT_BUS.addListener(BarreMagie::dessiner); }

    private static void dessiner(RenderGuiOverlayEvent.Pre e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        Minecraft mc = Minecraft.getInstance();
        // spectateur : le jeu affiche son propre menu a cet endroit
        if (!(mc.getCameraEntity() instanceof Player p) || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        e.setCanceled(true);
        GuiGraphics g = e.getGuiGraphics();
        int cx = e.getWindow().getGuiScaledWidth() / 2, h = e.getWindow().getGuiScaledHeight();
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(0, 0, -90);
        g.blit(TEX, cx - 91, h - 22, 0, 0, 182, 22, 256, 64);
        float puls = .8f + .2f * (float) Math.sin(System.nanoTime() / 4e8);
        RenderSystem.setShaderColor(1, 1, 1, puls);
        g.blit(TEX, cx - 91 - 1 + p.getInventory().selected * 20, h - 22 - 1, 0, 22, 24, 24, 256, 64);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        ItemStack main2 = p.getOffhandItem();
        boolean gauche = p.getMainArm().getOpposite() == HumanoidArm.LEFT;
        if (!main2.isEmpty()) g.blit(TEX, gauche ? cx - 91 - 29 : cx + 91, h - 23, 24, 22, 29, 24, 256, 64);
        g.pose().popPose();
        for (int i = 0; i < 9; i++) objet(g, mc, p, p.getInventory().items.get(i), cx - 90 + i * 20 + 2, h - 19, i + 1);
        if (!main2.isEmpty()) objet(g, mc, p, main2, gauche ? cx - 91 - 26 : cx + 91 + 10, h - 19, 10);
        RenderSystem.disableBlend();
    }

    private static void objet(GuiGraphics g, Minecraft mc, Player p, ItemStack s, int x, int y, int graine) {
        if (s.isEmpty()) return;
        g.renderItem(p, s, x, y, graine);
        g.renderItemDecorations(mc.font, s, x, y);
    }

    private BarreMagie() {}
}
