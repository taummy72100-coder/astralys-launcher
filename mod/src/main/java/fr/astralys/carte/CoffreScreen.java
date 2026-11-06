package fr.astralys.carte;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Ouverture d'un coffre : la cle arrive et tourne dans la serrure, le coffre 3D tremble, le couvercle s'ouvre dans un jet de lumiere,
 * puis la bande d'objets defile et ralentit, et le lot gagne se revele dans la couleur de sa rarete.
 */
public class CoffreScreen extends EcranAstralys {
    private static final float DUREE = 5.5f, PAS = 66, CX = 320, Y = 132;
    // temps de l'introduction (secondes) : arrivee de la cle, tour de cle, tremblement, ouverture, envol du coffre
    private static final float CLE = .8f, TOUR = 1.1f, TREMBLE = 1.9f, OUVRE = 2.5f, INTRO = 3.1f;
    private final Coffres.Roulette r;
    private final float decalage;
    private float avance;   // secondes gagnees quand on clique pour accelerer
    private int dernier = -1, etape;
    private boolean revele;

    public CoffreScreen(Coffres.Roulette r) {
        super("Coffre " + Coffres.NOMS[r.rarete()], Coffres.COULEURS[r.rarete()]);
        this.r = r;
        decalage = (float) (Math.random() * 36 - 18);   // ne s'arrete jamais pile au milieu de la carte
    }

    static ResourceLocation modele(String rarete, String partie) { return new ResourceLocation(AstralysCarte.MODID, "item/coffre_" + rarete + "_" + partie); }

    private float tt() { return t + avance; }

    private float progres() { return Math.min(1, (tt() - INTRO - .2f) / DUREE); }

    private static float lisse(float x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }

    @Override
    void contenu(GuiGraphics g) {
        int cr = Coffres.COULEURS[r.rarete()];
        texte(g, "✦ " + Coffres.NOMS[r.rarete()].toUpperCase(java.util.Locale.FRANCE) + " ✦", 600, 40, 1, cr, 1, true);
        if (tt() < INTRO) { introduction(g, cr); return; }
        roulette(g, cr);
    }

    // ---------------------------------------------------------------- l'ouverture du coffre
    private void introduction(GuiGraphics g, int cr) {
        float s = tt();
        // sons, une fois chacun
        if (etape == 0 && s >= CLE) { etape = 1; son(SoundEvents.CHAIN_PLACE, 1.4f); }
        if (etape == 1 && s >= TOUR) { etape = 2; son(SoundEvents.IRON_TRAPDOOR_OPEN, 1.6f); }
        if (etape == 2 && s >= TREMBLE) { etape = 3; son(SoundEvents.CHEST_OPEN, .8f); son(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f); }
        if (etape == 3 && s >= OUVRE) { etape = 4; son(r.rarete() >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.3f); }

        float ouverture = 110 * lisse((s - TREMBLE) / (OUVRE - TREMBLE));
        float envol = lisse((s - OUVRE) / (INTRO - OUVRE));
        float cx = 320, cy = 214 - 40 * envol, taille = 120 * (1 - envol * .85f);
        float tremble = s > TOUR && s < TREMBLE ? (float) Math.sin(s * 60) * 4 * lisse((s - TOUR) / .4f) : 0;
        float lacet = 205 + (float) Math.sin(s * 1.3f) * 6;

        // lumiere : halo qui grandit pendant le tremblement, rayons a l'ouverture
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float charge = lisse((s - TOUR) / (TREMBLE - TOUR)), jet = lisse((s - TREMBLE) / .35f) * (1 - envol);
        halo(g, cx, cy - 10, 90 + 50 * charge + 80 * jet, cr, (.25f + .3f * charge + .4f * jet) * a);
        if (jet > 0) {
            for (int i = 0; i < 12; i++) {
                g.pose().pushPose();
                g.pose().translate(cx, cy - 30, 0);
                g.pose().mulPose(Axis.ZP.rotationDegrees(i * 30 + s * 25));
                g.fillGradient(-3, -200, 3, 0, argb(cr, 0), argb(0xFFFFFF, .35f * jet * a));
                g.pose().popPose();
            }
            halo(g, cx, cy - 34, 60 * jet, 0xFFFFFF, .5f * jet * a);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (taille > 2) coffre3d(g, cx, cy, taille, ouverture, lacet, tremble);

        // la cle : glisse depuis la gauche jusqu'a la serrure, tourne d'un quart de tour, puis s'efface
        if (s < TREMBLE) {
            float arrivee = lisse(s / CLE), tour = lisse((s - CLE) / (TOUR - CLE)), fondu = 1 - lisse((s - TOUR) / .5f);
            float kx = 90 + (cx - 58 - 90) * arrivee, ky = cy - 6 + (float) Math.sin(s * 9) * 3 * (1 - arrivee);
            ItemStack cle = Metiers.pile("astralyscarte:cle_" + Coffres.RARETES[r.rarete()]);
            g.pose().pushPose();
            g.pose().translate(kx, ky, 300);
            g.pose().mulPose(Axis.ZP.rotationDegrees(-45 + 90 * tour));
            g.pose().scale(fondu, fondu, 1);
            objet(g, cle, -28, -28, 3.5f);
            g.pose().popPose();
        }
        String txt = s < TOUR ? "La clé tourne dans la serrure…" : s < TREMBLE ? "Le coffre s'éveille…" : "Le coffre s'ouvre !";
        texte(g, txt, 320, 300, 1, 0xC8C8D0, 0, false);
        texte(g, "Clique pour passer", 320, 316, .7f, 0x6E6E78, 0, false);
    }

    /** Le coffre en 3D, couvercle pivote autour de sa charniere arriere (y = 9, z = 14 dans le modele). */
    private void coffre3d(GuiGraphics g, float cx, float cy, float taille, float ouverture, float lacet, float tremble) {
        Minecraft mc = Minecraft.getInstance();
        String id = Coffres.RARETES[r.rarete()];
        BakedModel corps = mc.getModelManager().getModel(modele(id, "corps")), couv = mc.getModelManager().getModel(modele(id, "couvercle"));
        ItemStack pile = new ItemStack(net.minecraft.world.item.Items.STICK);   // pile neutre : pas de reflet d'enchantement sur le modele
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 150);
        ps.mulPoseMatrix(new Matrix4f().scaling(1, -1, 1));
        ps.scale(taille, taille, taille);
        ps.mulPose(Axis.XP.rotationDegrees(24));
        ps.mulPose(Axis.ZP.rotationDegrees(tremble));
        ps.mulPose(Axis.YP.rotationDegrees(lacet));
        Lighting.setupFor3DItems();
        var buf = g.bufferSource();
        mc.getItemRenderer().render(pile, ItemDisplayContext.NONE, false, ps, buf, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, corps);
        float hy = 9 / 16f - .5f, hz = 14 / 16f - .5f;
        ps.translate(0, hy, hz);
        ps.mulPose(Axis.XP.rotationDegrees(ouverture));
        ps.translate(0, -hy, -hz);
        mc.getItemRenderer().render(pile, ItemDisplayContext.NONE, false, ps, buf, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, couv);
        g.flush();
        ps.popPose();
    }

    // ---------------------------------------------------------------- la roulette
    private void roulette(GuiGraphics g, int cr) {
        float p = Math.max(0, progres()), e = 1 - (float) Math.pow(1 - p, 3.2);
        float pos = e * (r.gagnant() * PAS + decalage);
        float apparition = lisse((tt() - INTRO) / .4f);
        texte(g, "Le coffre est ouvert !", 320, 88, 1, 0xC8C8D0, 0, false);

        // bande des objets
        rect(g, 36, Y - 8, 604, Y + 88, argb(0x0A0714, .6f * a * apparition));
        for (int i = 0; i < r.objets().size(); i++) {
            float x = CX + i * PAS - pos - 30;
            if (x < 30 || x > 550) continue;
            float bord = Math.min(1, Math.min(x - 30, 550 - x) / 50) * apparition;
            int rc = Coffres.COULEURS[r.raretes().get(i)];
            boolean gagnant = revele && i == r.gagnant();
            carte(g, x, Y, 60, 80, 0);
            g.fillGradient(Math.round(x), Math.round(Y + 40), Math.round(x + 60), Math.round(Y + 80), argb(rc, 0), argb(rc, .45f * bord * a));
            rect(g, x, Y + 77, x + 60, Y + 80, argb(rc, bord * a));
            if (gagnant) { rect(g, x - 2, Y - 2, x + 62, Y, argb(rc, a)); rect(g, x - 2, Y + 80, x + 62, Y + 82, argb(rc, a)); }
            objet(g, r.objets().get(i), x + 10, Y + 14, 2.5f);
            int n = r.objets().get(i).getCount();
            if (n > 1) texte(g, "×" + n, x + 56, Y + 60, .8f, 0xFFFFFF, 1, true);
        }
        // aiguille doree au centre
        rect(g, CX - 1, Y - 14, CX + 1, Y + 94, argb(0xF0D28A, a));
        texte(g, "▼", CX, Y - 22, 1.2f, 0xF0D28A, 0, true);
        texte(g, "▲", CX, Y + 92, 1.2f, 0xF0D28A, 0, true);

        // tic a chaque carte qui passe sous l'aiguille
        int sous = Math.round(pos / PAS);
        if (sous != dernier && p < 1) { dernier = sous; son(SoundEvents.NOTE_BLOCK_HAT.value(), 1.2f + .8f * e); }

        if (p >= 1) {
            if (!revele) { revele = true; son(r.raretes().get(r.gagnant()) >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1); }
            ItemStack gain = r.objets().get(r.gagnant());
            int gc = Coffres.COULEURS[r.raretes().get(r.gagnant())];
            float puls = .5f + .5f * (float) Math.sin(t * 4);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            halo(g, 320, 272, 70 + 8 * puls, gc, .45f * a);
            halo(g, 320, 272, 36, 0xFFFFFF, .25f * a);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            objet(g, gain, 304, 256, 2);
            texte(g, Coffres.NOMS[r.raretes().get(r.gagnant())].toUpperCase(java.util.Locale.FRANCE), 320, 234, .8f, gc, 0, true);
            texte(g, (gain.getCount() > 1 ? gain.getCount() + "× " : "") + gain.getHoverName().getString(), 320, 296, 1.2f, 0xFFFFFF, 0, true);
            texte(g, "Ajouté à ton inventaire  •  clique pour fermer", 320, 316, .7f, 0x9A9AA4, 0, false);
        } else texte(g, "Clique pour accélérer", 320, 316, .7f, 0x6E6E78, 0, false);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (progres() >= 1) { onClose(); return true; }
        if (tt() < INTRO) avance = Math.max(avance, INTRO - t); else avance += 1.5f;
        return true;
    }
}
