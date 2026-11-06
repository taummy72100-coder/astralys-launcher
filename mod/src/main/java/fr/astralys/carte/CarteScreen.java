package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Carte des mondes facon Paladium : particules, lueurs, sons. Un clic envoie /trigger au serveur. */
public class CarteScreen extends Screen {

    private static final ResourceLocation IMAGE = new ResourceLocation(AstralysCarte.MODID, "textures/gui/carte.png");
    private static final ResourceLocation HALO = new ResourceLocation(AstralysCarte.MODID, "textures/gui/halo.png");
    private static final ResourceLocation ANNEAU = new ResourceLocation(AstralysCarte.MODID, "textures/gui/anneau.png");
    // Cadre en bois feerique : la carte + M pixels de chaque cote.
    private static final ResourceLocation CADRE = new ResourceLocation(AstralysCarte.MODID, "textures/gui/cadre.png");
    private static final ResourceLocation LUEUR = new ResourceLocation(AstralysCarte.MODID, "textures/gui/cadre_lueur.png");
    private static final ResourceLocation FEUILLE = new ResourceLocation(AstralysCarte.MODID, "textures/gui/feuille.png");
    private static final int M = 70;
    // Repere de l'image (1672 x 941) : position des badges.
    private static final int W = 1672, H = 941;

    enum Effet { BRAISES, NEIGE, LUCIOLES, MAGIE }

    record Monde(String nom, String commande, int couleur, String texte, int x, int y, Effet effet) {}

    private static final List<Monde> MONDES = List.of(
            new Monde("Pyrélune", "trigger monde set 2", 0xFF7A3A, "Volcan, lave et forteresse de pierre noire.", 836, 182, Effet.BRAISES),
            new Monde("Sylvaria", "trigger monde set 1", 0x7CE07C, "Forêt enchantée, arbre géant et champignons.", 474, 408, Effet.LUCIOLES),
            new Monde("Azuréa", "trigger monde set 3", 0x8FD8FF, "Château de glace et cristaux bleus.", 1213, 380, Effet.NEIGE),
            new Monde("Sunfalls", "trigger spawn", 0xC9A0FF, "Le spawn : remparts, village et cercle magique.", 836, 593, Effet.MAGIE));

    private static final class Particule {
        float x, y, vx, vy, vie, duree, taille;
        int couleur;
        boolean lueur;
        int feuille = -1;       // feuille qui tombe : numero du dessin (0..3)
        float rot, vrot;
    }

    private final List<Particule> particules = new ArrayList<>();
    private final Random rnd = new Random();
    private int ox, oy, iw, ih;
    private float echelle;
    private Monde survole, choisi;
    private long debut, dernier, clicA;

    public CarteScreen() {
        super(Component.literal("Carte d'Astralys"));
    }

    @Override
    protected void init() {
        echelle = Math.min(width * .97f / (W + 2 * M), height * .97f / (H + 2 * M));
        iw = Math.round(W * echelle); ih = Math.round(H * echelle);
        ox = (width - iw) / 2; oy = (height - ih) / 2;
        if (debut == 0) {
            debut = dernier = System.nanoTime();
            son(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f);
            son(SoundEvents.BOOK_PAGE_TURN, 1f);
        }
    }

    private void son(SoundEvent s, float hauteur) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, hauteur));
    }

    private float sx(float x) { return ox + x * echelle; }
    private float sy(float y) { return oy + y * echelle; }

    private Monde sous(double mx, double my) {
        for (Monde m : MONDES) {
            if (mx >= sx(m.x - 95) && mx <= sx(m.x + 95) && my >= sy(m.y - 55) && my <= sy(m.y + 92)) return m;
        }
        return null;
    }

    // --- Particules ------------------------------------------------------------
    private void creer(Monde m, float dt) {
        float taux = switch (m.effet) { case BRAISES -> 26; case NEIGE -> 22; case LUCIOLES -> 9; case MAGIE -> 14; };
        if (m == survole) taux *= 2.2f;
        float n = taux * dt;
        while (n > 0) {
            if (n < 1 && rnd.nextFloat() > n) break;
            n -= 1;
            Particule p = new Particule();
            float r = 170;
            p.x = m.x + (rnd.nextFloat() - .5f) * r * 2;
            p.y = m.y + 60 + (rnd.nextFloat() - .5f) * r;
            p.vie = 0;
            switch (m.effet) {
                case BRAISES -> { p.vx = (rnd.nextFloat() - .5f) * 14; p.vy = -30 - rnd.nextFloat() * 40; p.duree = 1.4f + rnd.nextFloat();
                    p.couleur = rnd.nextBoolean() ? 0xFF8A2A : 0xFFC850; p.taille = 3 + rnd.nextInt(3); p.lueur = true; }
                case NEIGE -> { p.y -= 140; p.vx = (rnd.nextFloat() - .3f) * 12; p.vy = 18 + rnd.nextFloat() * 18; p.duree = 3 + rnd.nextFloat() * 2;
                    p.couleur = 0xF4FAFF; p.taille = 2 + rnd.nextInt(3); p.lueur = false; }
                case LUCIOLES -> { p.vx = (rnd.nextFloat() - .5f) * 20; p.vy = (rnd.nextFloat() - .5f) * 14; p.duree = 2.5f + rnd.nextFloat() * 2;
                    p.couleur = rnd.nextBoolean() ? 0xE8FF7A : 0xB6FF9A; p.taille = 3; p.lueur = true; }
                case MAGIE -> { p.vx = (rnd.nextFloat() - .5f) * 8; p.vy = -22 - rnd.nextFloat() * 20; p.duree = 2 + rnd.nextFloat();
                    p.couleur = rnd.nextBoolean() ? 0xC9A0FF : 0xF0C576; p.taille = 3 + rnd.nextInt(2); p.lueur = true; }
            }
            particules.add(p);
        }
    }

    private void animer(float dt) {
        for (Monde m : MONDES) creer(m, dt);
        // lucioles qui voltigent sur le cadre en bois
        if (rnd.nextFloat() < dt * 7) {
            Particule p = new Particule();
            float bord = -M / 2f, k = rnd.nextFloat();
            switch (rnd.nextInt(4)) {
                case 0 -> { p.x = k * W; p.y = bord; }
                case 1 -> { p.x = k * W; p.y = H - bord; }
                case 2 -> { p.x = bord; p.y = k * H; }
                default -> { p.x = W - bord; p.y = k * H; }
            }
            p.vx = (rnd.nextFloat() - .5f) * 16; p.vy = (rnd.nextFloat() - .5f) * 16; p.duree = 2.5f + rnd.nextFloat() * 2;
            p.couleur = rnd.nextBoolean() ? 0xE8FF7A : 0xB6FF9A; p.taille = 3; p.lueur = true;
            particules.add(p);
        }
        // feuilles qui se detachent du cadre et tombent en tournoyant
        if (rnd.nextFloat() < dt * 1.4f) {
            Particule p = new Particule();
            p.x = rnd.nextFloat() * W; p.y = -M / 2f;
            p.vx = (rnd.nextFloat() - .5f) * 16; p.vy = 20 + rnd.nextFloat() * 18; p.duree = 7 + rnd.nextFloat() * 4;
            p.feuille = rnd.nextInt(4); p.rot = rnd.nextFloat() * 360; p.vrot = (rnd.nextFloat() - .5f) * 120;
            particules.add(p);
        }
        // etincelles dorees sur toute la carte
        if (rnd.nextFloat() < dt * 10) {
            Particule p = new Particule();
            p.x = rnd.nextFloat() * W; p.y = rnd.nextFloat() * H; p.vx = 0; p.vy = -6; p.duree = 1.2f + rnd.nextFloat();
            p.couleur = 0xFFF0C8; p.taille = 3; p.lueur = true;
            particules.add(p);
        }
        particules.removeIf(p -> {
            p.vie += dt;
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.feuille >= 0) { p.rot += p.vrot * dt; p.x += (float) Math.sin(p.vie * 1.7f + p.vrot) * 18 * dt; }
            if (p.couleur == 0xE8FF7A || p.couleur == 0xB6FF9A) { p.vx += (rnd.nextFloat() - .5f) * 40 * dt; p.vy += (rnd.nextFloat() - .5f) * 40 * dt; }
            return p.vie >= p.duree;
        });
        if (particules.size() > 900) particules.subList(0, particules.size() - 900).clear();
    }

    private static int argb(int rgb, float a) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private void teinte(int rgb, float a) {
        RenderSystem.setShaderColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, a);
    }

    private void lueur(GuiGraphics g, ResourceLocation tex, float cx, float cy, float rayon, int rgb, float a) {
        int r = Math.max(1, Math.round(rayon));
        teinte(rgb, a);
        g.blit(tex, Math.round(cx) - r, Math.round(cy) - r, r * 2, r * 2, 0, 0, 64, 64, 64, 64);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = Math.min(.1f, (now - dernier) / 1e9f);
        dernier = now;
        float t = (now - debut) / 1e9f;
        animer(dt);

        // Apparition : fondu + leger zoom
        float a = Math.min(1, t / .35f);
        float z = .96f + .04f * a;
        g.fill(0, 0, width, height, argb(0x0B0820, .78f * a));
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0);
        g.pose().scale(z, z, 1);
        g.pose().translate(-width / 2f, -height / 2f, 0);

        RenderSystem.enableBlend();
        teinte(0xFFFFFF, a);
        g.blit(IMAGE, ox, oy, iw, ih, 0, 0, W, H, W, H);
        int m0 = Math.round(M * echelle);
        g.blit(CADRE, ox - m0, oy - m0, iw + 2 * m0, ih + 2 * m0, 0, 0, W + 2 * M, H + 2 * M, W + 2 * M, H + 2 * M);
        // lueurs des fleurs, baies et gemmes du cadre : elles respirent doucement
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        teinte(0xFFFFFF, (.45f + .3f * (float) Math.sin(t * 1.8f)) * a);
        g.blit(LUEUR, ox - m0, oy - m0, iw + 2 * m0, ih + 2 * m0, 0, 0, W + 2 * M, H + 2 * M, W + 2 * M, H + 2 * M);
        RenderSystem.defaultBlendFunc();
        teinte(0xFFFFFF, a);

        Monde m = sous(mx, my);
        if (m != survole && m != null) son(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f + rnd.nextFloat() * .3f);
        survole = m;

        // Lueurs additives : badges qui respirent, particules
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (Monde w : MONDES) {
            float puls = (float) Math.sin(t * 2.4 + w.x) * .5f + .5f;
            boolean s = w == survole;
            lueur(g, HALO, sx(w.x), sy(w.y), (s ? 95 : 62 + puls * 10) * echelle, w.couleur, (s ? .8f : .32f + puls * .12f) * a);
            if (s) {
                float onde = (t * 1.4f) % 1f;
                lueur(g, ANNEAU, sx(w.x), sy(w.y), (60 + onde * 90) * echelle, w.couleur, (1 - onde) * .9f * a);
                lueur(g, ANNEAU, sx(w.x), sy(w.y), (60 + ((onde + .5f) % 1f) * 90) * echelle, w.couleur, (1 - (onde + .5f) % 1f) * .9f * a);
            }
        }
        for (Particule p : particules) {
            float k = p.vie / p.duree;
            float al = (k < .2f ? k / .2f : 1 - (k - .2f) / .8f) * a;
            if (p.lueur && p.feuille < 0) lueur(g, HALO, sx(p.x), sy(p.y), p.taille * 3.2f * echelle * 1.6f, p.couleur, al * .55f);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        for (Particule p : particules) {
            if (p.feuille >= 0) continue;
            float k = p.vie / p.duree;
            float al = (k < .2f ? k / .2f : 1 - (k - .2f) / .8f) * a;
            int px = Math.round(sx(p.x)), py = Math.round(sy(p.y)), ts = Math.max(1, Math.round(p.taille * echelle * 1.4f));
            g.fill(px, py, px + ts, py + ts, argb(p.couleur, al));
        }

        for (Particule p : particules) {
            if (p.feuille < 0) continue;
            float k = p.vie / p.duree;
            int ts = Math.max(6, Math.round(32 * echelle * 1.3f));
            g.pose().pushPose();
            g.pose().translate(sx(p.x), sy(p.y), 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(p.rot));
            teinte(0xFFFFFF, Math.min(1, Math.min(k * 6, (1 - k) * 3)) * a);
            g.blit(FEUILLE, -ts / 2, -ts / 2, ts, ts, p.feuille * 32, 0, 32, 32, 128, 32);
            g.pose().popPose();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);

        panneau(g, a);
        g.pose().popPose();

        // Flash de teleportation
        if (choisi != null) {
            float f = (now - clicA) / 1e9f;
            g.fill(0, 0, width, height, argb(choisi.couleur, Math.max(0, .55f - f)));
            g.fill(0, 0, width, height, argb(0xFFFFFF, Math.max(0, .45f - f * 1.4f)));
            if (f > .45f) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) mc.player.connection.sendCommand(choisi.commande);
                onClose();
            }
        }
    }

    private void panneau(GuiGraphics g, float a) {
        int pw = Math.round(330 * echelle * 1.25f), ph = Math.round(110 * echelle * 1.25f);
        pw = Math.max(pw, 190); ph = Math.max(ph, 62);
        int x1 = ox + iw - pw - 10, y1 = oy + ih - ph - 10;
        g.fill(x1 - 2, y1 - 2, x1 + pw + 2, y1 + ph + 2, argb(0xF0C576, .9f * a));
        g.fill(x1, y1, x1 + pw, y1 + ph, argb(0x1A1426, .92f * a));
        int cx = x1 + pw / 2;
        if (survole == null) {
            g.drawCenteredString(font, Component.literal("✦ Rejoindre un monde ✦").withStyle(s -> s.withColor(0xF0C576).withBold(true)), cx, y1 + 12, 0xFFFFFF);
            g.drawCenteredString(font, Component.literal("Clique sur une icône pour").withStyle(s -> s.withColor(0xE8DFFF)), cx, y1 + 30, 0xFFFFFF);
            g.drawCenteredString(font, Component.literal("rejoindre un monde !").withStyle(s -> s.withColor(0xE8DFFF)), cx, y1 + 42, 0xFFFFFF);
        } else {
            Monde m = survole;
            g.drawCenteredString(font, Component.literal(m.nom).withStyle(s -> s.withColor(m.couleur).withBold(true)), cx, y1 + 10, 0xFFFFFF);
            g.drawWordWrap(font, Component.literal(m.texte).withStyle(s -> s.withColor(0xE8DFFF)), x1 + 8, y1 + 24, pw - 16, 0xFFFFFF);
            g.drawCenteredString(font, Component.literal("▶ Clique pour t'y téléporter").withStyle(s -> s.withColor(0xF0C576).withItalic(true)), cx, y1 + ph - 14, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int bouton) {
        Monde m = sous(mx, my);
        if (bouton == 0 && m != null && choisi == null) {
            choisi = m; clicA = System.nanoTime();
            son(SoundEvents.ENDERMAN_TELEPORT, 1.2f);
            son(SoundEvents.AMETHYST_CLUSTER_BREAK, .8f);
            for (int i = 0; i < 80; i++) {
                Particule p = new Particule();
                double ang = rnd.nextDouble() * Math.PI * 2; float v = 60 + rnd.nextFloat() * 160;
                p.x = m.x; p.y = m.y; p.vx = (float) Math.cos(ang) * v; p.vy = (float) Math.sin(ang) * v;
                p.duree = .5f + rnd.nextFloat() * .4f; p.couleur = rnd.nextBoolean() ? m.couleur : 0xFFF0C8; p.taille = 4; p.lueur = true;
                particules.add(p);
            }
            return true;
        }
        return super.mouseClicked(mx, my, bouton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
