package fr.astralys.carte;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/** Carte des trois mondes : un clic sur une zone envoie /trigger monde set N au serveur. */
public class CarteScreen extends Screen {

    private static final ResourceLocation IMAGE =
            new ResourceLocation(AstralysCarte.MODID, "textures/gui/carte.png");
    // Taille de l'image d'origine : les zones sont exprimees dans ce repere.
    private static final int W = 1672, H = 941;
    private static final int TEX_W = 1280, TEX_H = 720;

    record Monde(int id, String nom, int couleur, String texte, int x, int y, int w, int h) {}

    private static final List<Monde> MONDES = List.of(
            new Monde(1, "Sylvaria", 0x7CE07C, "Forêt enchantée, arbres géants et champignons.", 252, 314, 222, 140),
            new Monde(2, "Pyrélune", 0xFF8A3D, "Volcan et forteresse de pierre noire.", 730, 126, 230, 138),
            new Monde(3, "Azuréa", 0x8FD8FF, "Château de glace et cristaux bleus.", 1262, 410, 212, 130));

    private int ox, oy;
    private float echelle;
    private Monde survole;

    public CarteScreen() {
        super(Component.literal("Carte d'Astralys"));
    }

    @Override
    protected void init() {
        echelle = Math.min(width * 0.94f / W, (height - 40) * 0.94f / H);
        ox = (int) ((width - W * echelle) / 2);
        oy = (int) ((height - H * echelle) / 2) + 6;
        addRenderableWidget(Button.builder(Component.literal("Retour au spawn"), b -> envoyer("trigger spawn"))
                .bounds(width / 2 - 60, height - 24, 120, 20).build());
    }

    private int px(int x) { return ox + Math.round(x * echelle); }
    private int py(int y) { return oy + Math.round(y * echelle); }

    private Monde sous(double mx, double my) {
        for (Monde m : MONDES) {
            if (mx >= px(m.x) && mx < px(m.x + m.w) && my >= py(m.y) && my < py(m.y + m.h)) return m;
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.blit(IMAGE, ox, oy, Math.round(W * echelle), Math.round(H * echelle), 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        Monde m = sous(mx, my);
        if (m != survole && m != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f));
        }
        survole = m;
        if (m != null) {
            int x1 = px(m.x), y1 = py(m.y), x2 = px(m.x + m.w), y2 = py(m.y + m.h);
            g.fill(x1, y1, x2, y2, 0x30F0C576);
            int or = 0xFFF0C576;
            g.fill(x1, y1, x2, y1 + 2, or);
            g.fill(x1, y2 - 2, x2, y2, or);
            g.fill(x1, y1, x1 + 2, y2, or);
            g.fill(x2 - 2, y1, x2, y2, or);
        }

        g.drawCenteredString(font, Component.literal("✦ Choisis ton monde ✦").withStyle(s -> s.withColor(0xF0C576)),
                width / 2, 8, 0xFFFFFF);
        super.render(g, mx, my, pt);

        if (m != null) {
            g.renderComponentTooltip(font, List.of(
                    Component.literal(m.nom).withStyle(s -> s.withColor(TextColor.fromRgb(m.couleur)).withBold(true)),
                    Component.literal(m.texte).withStyle(s -> s.withColor(0xE8DFFF)),
                    Component.literal("Clique pour t'y téléporter").withStyle(s -> s.withColor(0xB9A8E8).withItalic(true))),
                    mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int bouton) {
        Monde m = sous(mx, my);
        if (bouton == 0 && m != null) {
            envoyer("trigger monde set " + m.id);
            return true;
        }
        return super.mouseClicked(mx, my, bouton);
    }

    private void envoyer(String commande) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(commande);
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
