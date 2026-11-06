package fr.astralys.carte;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ecran du broyeur astral, au style du grimoire : entree, barre d'avancement doree, lingots et poussiere d'etoile. */
public class BroyeurScreen extends AbstractContainerScreen<Machines.BroyeurMenu> {
    private static final int OR = 0xF0B429;

    public BroyeurScreen(Machines.BroyeurMenu m, Inventory inv, Component titre) {
        super(m, inv, titre);
        imageWidth = 176; imageHeight = 166; inventoryLabelY = imageHeight - 94;
    }

    private void caseObjet(GuiGraphics g, int x, int y, int c) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0A0714);
        g.fill(x, y, x + 16, y + 16, 0xFF1A1233);
        g.fill(x, y + 15, x + 16, y + 16, 0xFF000000 | c);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        RenderSystem.enableBlend();
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF0A0714);
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFFE2B65C);
        g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xFF2C1F52, 0xFF150F2C);
        g.fill(x + 6, y + 70, x + imageWidth - 6, y + 71, 0x55E2B65C);
        // cases de la machine
        caseObjet(g, x + 44, y + 35, 0x9AA0B4);
        caseObjet(g, x + 116, y + 35, OR);
        caseObjet(g, x + 142, y + 35, 0xC9A0FF);
        // fleche d'avancement + braises
        float p = menu.progres();
        g.fill(x + 66, y + 41, x + 106, y + 45, 0xFF0A0714);
        g.fillGradient(x + 66, y + 41, x + 66 + Math.round(40 * p), y + 45, 0xFFFFE08A, 0xFFE07A20);
        g.fill(x + 106, y + 38, x + 108, y + 48, 0xFF0A0714);
        if (p > 0) {
            long t = System.currentTimeMillis();
            for (int i = 0; i < 4; i++) g.fill(x + 70 + i * 9, y + 50 + (int) ((t / 90 + i * 3) % 6), x + 72 + i * 9, y + 52 + (int) ((t / 90 + i * 3) % 6), 0xCCFF9A40);
        }
        // inventaire du joueur
        for (int r = 0; r < 3; r++) for (int k = 0; k < 9; k++) caseObjet(g, x + 8 + k * 18, y + 84 + r * 18, 0x3B2F5E);
        for (int k = 0; k < 9; k++) caseObjet(g, x + 8 + k * 18, y + 142, 0x3B2F5E);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "✦ " + title.getString().toUpperCase(java.util.Locale.FRANCE), 8, 7, OR, true);
        g.drawString(font, "minerai → lingots ×2", 8, 18, 0xB8AEDA, false);
        g.drawString(font, "poussière", 138, 55, 0xC9A0FF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC8C8D0, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
