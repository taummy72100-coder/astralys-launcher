package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/** Cote joueur : donnees de metiers recues, inventaire facon grimoire (decor anime) et ses 3 boutons. */
final class ClientMetiers {
    static Metiers.Vue vue;

    static void recevoir(Metiers.Vue v) { vue = v; Verrous.xpClient = v.xp(); }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::inventaire);
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::suivre);
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::decor);
        MinecraftForge.EVENT_BUS.addListener(ClientMetiers::sansLivre);
    }

    // Pas de livre de recettes dans l'inventaire : ferme avant l'ouverture (la position du panneau en depend), bouton retire apres.
    private static void sansLivre(ScreenEvent.Init.Pre e) {
        var j = Minecraft.getInstance().player;
        if (e.getScreen() instanceof InventoryScreen && j != null) j.getRecipeBook().setOpen(net.minecraft.world.inventory.RecipeBookType.CRAFTING, false);
    }

    private static final List<Bouton> boutons = new ArrayList<>();

    private static void inventaire(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof EffectRenderingInventoryScreen<?> s)) return;
        if (s instanceof InventoryScreen)
            for (var w : List.copyOf(e.getListenersList())) if (w instanceof net.minecraft.client.gui.components.ImageButton) e.removeListener(w);
        boutons.clear();
        boutons.add(new Bouton("MÉTIERS", "niveaux & quêtes", 0xE8322B, -1, MetiersScreen::new));
        boutons.add(new Bouton("BOUTIQUE", "acheter & vendre", 0xF0B429, 1, ShopScreen::new));
        boutons.add(new Bouton("FACTION", "alliés & défis", 0xB46CFF, 2, FactionScreen::new));
        boutons.add(new Bouton("MARCHÉ", "ventes entre joueurs", 0x3DD6C5, 3, HdvScreen::new));
        boutons.add(new Bouton("PETS", "compagnons", 0x7FE0FF, 4, PetsScreen::new));
        boutons.forEach(e::addListener);
        placer(s);
    }

    // A droite du panneau ; a gauche s'il n'y a pas la place (livre de recettes ouvert sur petit ecran).
    private static void placer(EffectRenderingInventoryScreen<?> s) {
        boolean droite = s.getGuiLeft() + 176 + 6 + Bouton.L <= s.width;
        int x = droite ? s.getGuiLeft() + 176 + 6 : s.getGuiLeft() - 6 - Bouton.L;
        for (int i = 0; i < boutons.size(); i++) boutons.get(i).setPosition(x, s.getGuiTop() + 8 + i * 30);
    }

    private static void suivre(ScreenEvent.Render.Pre e) {
        if (e.getScreen() instanceof EffectRenderingInventoryScreen<?> s) placer(s);
    }

    // ---------------------------------------------------------------- decor anime sous le panneau
    private static final Random rnd = new Random();
    private static final List<float[]> motes = new ArrayList<>();   // x, y relatifs au panneau, vie, duree, couleur
    private static long dernier;

    private static void decor(ScreenEvent.BackgroundRendered e) {
        if (!(e.getScreen() instanceof InventoryScreen s)) return;
        GuiGraphics g = e.getGuiGraphics();
        long now = System.nanoTime();
        float dt = dernier == 0 ? 0 : Math.min(.1f, (now - dernier) / 1e9f), t = (now % 100_000_000_000L) / 1e9f;
        dernier = now;
        int x0 = s.getGuiLeft(), y0 = s.getGuiTop();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float puls = .5f + .5f * (float) Math.sin(t * 1.6f);
        halo(g, x0 + 88, y0 + 83, 150, 0x7A4CFF, .28f + .1f * puls);
        halo(g, x0 + 88, y0 + 83, 105, 0xC9A0FF, .12f + .06f * puls);
        // etincelles qui montent le long des bords
        if (rnd.nextFloat() < dt * 22) {
            int cote = rnd.nextInt(4);
            float px = cote < 2 ? rnd.nextFloat() * 176 : (cote == 2 ? -3 : 179), py = cote >= 2 ? rnd.nextFloat() * 166 : (cote == 0 ? -3 : 169);
            motes.add(new float[] {px, py, 0, 1.6f + rnd.nextFloat() * 1.6f, rnd.nextInt(3)});
        }
        int[] cols = {0xC9A0FF, 0xF0D28A, 0x8FD8FF};
        motes.removeIf(m -> {
            m[2] += dt; m[1] -= dt * 10;
            float k = m[2] / m[3], al = k < .2f ? k / .2f : 1 - (k - .2f) / .8f;
            halo(g, x0 + m[0], y0 + m[1], 5, cols[(int) m[4]], al * .9f);
            return m[2] > m[3];
        });
        RenderSystem.defaultBlendFunc();
        // etoiles qui scintillent aux quatre coins
        for (int i = 0; i < 4; i++) {
            float sc = .8f + .4f * (float) Math.sin(t * 3 + i * 1.7f);
            float cx = x0 + (i % 2 == 0 ? -4 : 180), cy = y0 + (i < 2 ? -4 : 170);
            g.pose().pushPose(); g.pose().translate(cx, cy, 0); g.pose().scale(sc, sc, 1);
            g.drawCenteredString(Minecraft.getInstance().font, "✦", 0, -4, 0xF0D28A);
            g.pose().popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void halo(GuiGraphics g, float cx, float cy, float r, int c, float al) {
        MetiersScreen.teinte(c, al);
        g.pose().pushPose(); g.pose().translate(cx - r, cy - r, 0); g.pose().scale(r * 2 / 64f, r * 2 / 64f, 1);
        g.blit(EcranAstralys.HALO, 0, 0, 0, 0, 64, 64, 64, 64);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- boutons
    private static final class Bouton extends Button {
        static final int L = 104, H = 26;
        private static final ResourceLocation LOS = new ResourceLocation(AstralysCarte.MODID, "textures/gui/losange.png");
        private static final ItemStack[] ICONES = {null, new ItemStack(Items.EMERALD), new ItemStack(Items.WHITE_BANNER), new ItemStack(Items.CHEST), Metiers.pile("astralyscarte:oeuf_fee")};
        private final String titre, sous;
        private final int couleur, icone;
        private float survol;

        Bouton(String titre, String sous, int couleur, int icone, Supplier<Screen> ecran) {
            super(Button.builder(Component.literal(titre), b -> Minecraft.getInstance().setScreen(ecran.get())).bounds(0, 0, L, H));
            this.titre = titre; this.sous = sous; this.couleur = couleur; this.icone = icone;
        }

        @Override
        public void playDownSound(net.minecraft.client.sounds.SoundManager m) {
            m.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            survol += ((isHovered() ? 1 : 0) - survol) * .25f;
            float t = (System.nanoTime() % 100_000_000_000L) / 1e9f;
            int x = getX() + Math.round(survol * 3), y = getY();
            var font = Minecraft.getInstance().font;
            RenderSystem.enableBlend();
            // lueur de survol
            if (survol > .02f) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                halo(g, x + L / 2f, y + H / 2f, L * .7f, couleur, .35f * survol);
                RenderSystem.defaultBlendFunc();
            }
            // plaque : bord noir, filet dore, fond indigo en degrade, barre de couleur a gauche
            g.fill(x - 1, y - 1, x + L + 1, y + H + 1, 0xFF0A0714);
            g.fill(x, y, x + L, y + H, EcranAstralys.argb(EcranAstralys.melange(0xE2B65C, 0xFFFFFF, survol * .4f), 1));
            g.fillGradient(x + 1, y + 1, x + L - 1, y + H - 1, 0xFF2C1F52, 0xFF1A1233);
            g.fill(x + 1, y + 1, x + L - 1, y + 2, 0x18FFFFFF);
            g.fill(x + 1, y + 1, x + 3, y + H - 1, 0xFF000000 | couleur);
            // gemme en losange avec l'icone
            float gx = x + 15, gy = y + H / 2f;
            float pulse = 1 + .06f * (float) Math.sin(t * 3) * survol;
            tex(g, LOS, gx - 12 * pulse, gy - 12 * pulse, 24 * pulse, 0x0A0714);
            tex(g, LOS, gx - 10 * pulse, gy - 10 * pulse, 20 * pulse, couleur);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (icone < 0) MetiersScreen.illustration(g, 0, gx, gy, 16, 1);
            else g.renderItem(ICONES[icone], Math.round(gx - 8), Math.round(gy - 8));
            // textes
            g.drawString(font, Component.literal(titre).withStyle(st -> st.withBold(true)), x + 31, y + 5, 0xFF000000 | EcranAstralys.melange(couleur, 0xFFFFFF, .25f + survol * .5f), true);
            g.pose().pushPose(); g.pose().translate(x + 31, y + 16, 0); g.pose().scale(.7f, .7f, 1);
            g.drawString(font, sous, 0, 0, 0xFFB8AEDA, false);
            g.pose().popPose();
            // fleche qui apparait au survol
            if (survol > .05f) g.drawString(font, "›", x + L - 9, y + 9, EcranAstralys.argb(0xF0D28A, survol), false);
        }

        private static void tex(GuiGraphics g, ResourceLocation r, float x, float y, float t, int c) {
            RenderSystem.enableBlend();
            MetiersScreen.teinte(c, 1);
            g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(t / 128f, t / 128f, 1);
            g.blit(r, 0, 0, 0, 0, 128, 128, 128, 128);
            g.pose().popPose();
        }
    }

    private ClientMetiers() {}
}
