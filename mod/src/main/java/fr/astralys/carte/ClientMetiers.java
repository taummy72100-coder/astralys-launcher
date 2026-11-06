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

    private static final java.util.List<Bouton> boutons = new java.util.ArrayList<>();

    private static void inventaire(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof InventoryScreen s)) return;
        boutons.clear();
        boutons.add(new Bouton("Métiers & quêtes", 0xE8322B, Items.GOLDEN_PICKAXE, () -> new MetiersScreen()));
        boutons.add(new Bouton("Boutique", 0xF0B429, Items.EMERALD, () -> new ShopScreen()));
        boutons.add(new Bouton("Faction", 0xB46CFF, Items.WHITE_BANNER, () -> new FactionScreen()));
        boutons.forEach(e::addListener);
        placer(s);
    }

    private static void placer(InventoryScreen s) {
        for (int i = 0; i < boutons.size(); i++) boutons.get(i).setPosition(s.getGuiLeft() - 30, s.getGuiTop() + 6 + i * 30);
    }

    // Le livre de recettes deplace l'inventaire : les boutons suivent.
    private static void suivre(ScreenEvent.Render.Pre e) {
        if (e.getScreen() instanceof InventoryScreen s) placer(s);
    }

    private static final class Bouton extends Button {
        private final ItemStack icone;
        private final int couleur;

        Bouton(String nom, int couleur, net.minecraft.world.item.Item icone, java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> ecran) {
            super(Button.builder(Component.literal(nom), b -> Minecraft.getInstance().setScreen(ecran.get())).bounds(0, 0, 26, 26));
            this.icone = new ItemStack(icone); this.couleur = couleur;
            setTooltip(Tooltip.create(Component.literal(nom).withStyle(st -> st.withColor(couleur).withBold(true))));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            MetiersScreen.hexa(g, getX() - 2, getY() - 2, 30, 0x0E0E10, 1);
            MetiersScreen.hexa(g, getX(), getY(), 26, isHovered() ? (couleur | 0x303030) : couleur, 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.renderItem(icone, getX() + 5, getY() + 5);
        }
    }

    private ClientMetiers() {}
}
