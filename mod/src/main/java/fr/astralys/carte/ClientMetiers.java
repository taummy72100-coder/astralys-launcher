package fr.astralys.carte;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/** Cote joueur : dernieres donnees de metiers recues, et le bouton hexagonal dans l'inventaire (E). */
final class ClientMetiers {
    static Metiers.Vue vue;

    static void recevoir(Metiers.Vue v) { vue = v; }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::inventaire);
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::suivre);
    }

    private static Bouton bouton;

    private static void inventaire(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof InventoryScreen s)) return;
        bouton = new Bouton(s.getGuiLeft() - 30, s.getGuiTop() + 6);
        e.addListener(bouton);
    }

    // Le livre de recettes deplace l'inventaire : le bouton suit.
    private static void suivre(ScreenEvent.Render.Pre e) {
        if (e.getScreen() instanceof InventoryScreen s && bouton != null) bouton.setPosition(s.getGuiLeft() - 30, s.getGuiTop() + 6);
    }

    private static final class Bouton extends Button {
        private static final ItemStack ICONE = new ItemStack(Items.GOLDEN_PICKAXE);

        Bouton(int x, int y) {
            super(Button.builder(Component.literal("Métiers"), b -> Minecraft.getInstance().setScreen(new MetiersScreen())).bounds(x, y, 26, 26));
            setTooltip(Tooltip.create(Component.literal("Métiers & quêtes").withStyle(st -> st.withColor(0xE8322B).withBold(true))));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            MetiersScreen.hexa(g, getX() - 2, getY() - 2, 30, 0x0E0E10, 1);
            MetiersScreen.hexa(g, getX(), getY(), 26, isHovered() ? 0xFF5A50 : 0xE8322B, 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.renderItem(ICONE, getX() + 5, getY() + 5);
        }
    }

    private ClientMetiers() {}
}
