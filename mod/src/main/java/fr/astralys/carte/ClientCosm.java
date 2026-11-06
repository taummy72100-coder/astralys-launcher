package fr.astralys.carte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.joml.Quaternionf;

/** Cote joueur des cosmetiques : ce que porte chacun, rendu 3D par-dessus le skin, mannequin gris des apercus, modele qui joue les emotes. */
final class ClientCosm {
    static Cosmetiques.Vue vue;
    static Map<UUID, List<String>> portes = Map.of();
    /** Apercu dans la boutique (survol d'une carte) : remplace la partie correspondante sur son propre joueur. */
    static final String[] apercu = new String[4];

    // pieds un peu plus gros que les jambes : les bottes passent par-dessus le pantalon sans scintiller
    private static final ModelLayerLocation COUCHE = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "cosmetique"), "main");
    private static final ModelLayerLocation PIEDS = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "cosmetique_pieds"), "main");
    private static final Map<String, ModelLayerLocation> COUCHES_3D = new HashMap<>();
    static final Map<String, ModelPart> MODELES = new HashMap<>();
    private static final String[] OS = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    static void init(IEventBus mod) {
        for (String b : CosmetiqueModeles.BASES) COUCHES_3D.put(b, new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "cosm_" + b), "main"));
        mod.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
            e.registerLayerDefinition(COUCHE, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.1f), 0), 64, 32));
            e.registerLayerDefinition(PIEDS, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.25f), 0), 64, 32));
            COUCHES_3D.forEach((b, l) -> e.registerLayerDefinition(l, () -> CosmetiqueModeles.couche(b)));
        });
        mod.addListener((EntityRenderersEvent.AddLayers e) -> {
            COUCHES_3D.forEach((b, l) -> MODELES.put(b, e.getEntityModels().bakeLayer(l)));
            for (String skin : e.getSkins()) {
                LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> r = e.getSkin(skin);
                if (r == null) continue;
                boolean fin = "slim".equals(skin);
                remplacerModele(r, new Emotes.Modele(e.getEntityModels().bakeLayer(fin ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), fin));
                r.addLayer(new Couche(r, new HumanoidModel<>(e.getEntityModels().bakeLayer(COUCHE)), new HumanoidModel<>(e.getEntityModels().bakeLayer(PIEDS))));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderNameTagEvent e) -> { if (e.getEntity() instanceof Mannequin) e.setResult(Event.Result.DENY); });
    }

    /** Le modele du joueur devient celui qui sait jouer les emotes (les couches existantes le suivent : elles lisent le modele a chaque image). */
    private static void remplacerModele(LivingEntityRenderer<?, ?> r, EntityModel<?> m) {
        try {
            for (var f : LivingEntityRenderer.class.getDeclaredFields())
                if (f.getType() == EntityModel.class) { f.setAccessible(true); f.set(r, m); return; }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            AstralysCarte.LOG.warn("Emotes indisponibles : modele du joueur non remplace ({})", ex.toString());
        }
    }

    static String porte(AbstractClientPlayer j, int partie) {
        if (j instanceof Mannequin m) return m.tenue[partie];
        UUID u = j.getUUID();
        var mc = Minecraft.getInstance();
        if (mc.player != null && u.equals(mc.player.getUUID()) && apercu[partie] != null) return apercu[partie].isEmpty() ? null : apercu[partie];
        List<String> l = portes.get(u);
        if (l == null && vue != null && mc.player != null && u.equals(mc.player.getUUID())) l = vue.portes();
        String id = l == null || l.size() <= partie ? null : l.get(partie);
        return id == null || id.isEmpty() ? null : id;
    }

    static final class Couche extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final HumanoidModel<AbstractClientPlayer> corps, pieds;

        Couche(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p, HumanoidModel<AbstractClientPlayer> corps, HumanoidModel<AbstractClientPlayer> pieds) {
            super(p); this.corps = corps; this.pieds = pieds;
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int lumiere, AbstractClientPlayer j, float a, float b, float pt, float c, float d, float e) {
            if (j.isInvisible()) return;
            PlayerModel<AbstractClientPlayer> parent = getParentModel();
            for (int partie = 0; partie < 4; partie++) {
                String id = porte(j, partie);
                Cosmetiques.Cosm cos = id == null ? null : Cosmetiques.get(id);
                if (cos == null) continue;
                if (cos.base() != null) {   // cosmetique 3D : chaque os suit la partie du corps du meme nom
                    ModelPart m = MODELES.get(cos.base());
                    if (m == null) continue;
                    var vc = buf.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation(AstralysCarte.MODID, "textures/cosmetique/3d/" + id + ".png")));
                    ModelPart[] corpsJ = {parent.head, parent.body, parent.rightArm, parent.leftArm, parent.rightLeg, parent.leftLeg};
                    for (int i = 0; i < OS.length; i++) {
                        if (!m.hasChild(OS[i])) continue;
                        ModelPart o = m.getChild(OS[i]);
                        o.copyFrom(corpsJ[i]);
                        o.render(ps, vc, lumiere, OverlayTexture.NO_OVERLAY);
                    }
                    continue;
                }
                HumanoidModel<AbstractClientPlayer> m = partie == 3 ? pieds : corps;
                parent.copyPropertiesTo(m);
                m.setAllVisible(false);
                switch (partie) {
                    case 0 -> { m.head.visible = true; m.hat.visible = true; }
                    case 1 -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
                    default -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
                }
                var vc = buf.getBuffer(RenderType.armorCutoutNoCull(new ResourceLocation(AstralysCarte.MODID, "textures/cosmetique/" + id + ".png")));
                m.renderToBuffer(ps, vc, lumiere, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            }
        }
    }

    // ------------------------------------------------------------------ mannequin gris des apercus (boutique, coffres, profil)
    static final class Mannequin extends RemotePlayer {
        private static final ResourceLocation PEAU = new ResourceLocation(AstralysCarte.MODID, "textures/entity/mannequin.png");
        final String[] tenue = new String[4];
        String emote;
        long emoteDebut;

        Mannequin(ClientLevel l) { super(l, new GameProfile(UUID.randomUUID(), "Mannequin")); }

        @Override public ResourceLocation getSkinTextureLocation() { return PEAU; }
        @Override public String getModelName() { return "default"; }
        @Override public boolean isInvisible() { return false; }

        Mannequin habiller(String... ids) {
            java.util.Arrays.fill(tenue, null);
            for (String id : ids) { Cosmetiques.Cosm c = id == null ? null : Cosmetiques.get(id); if (c != null) tenue[c.partie()] = id; }
            return this;
        }
    }

    private static Mannequin mannequin;

    static Mannequin mannequin() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (mannequin == null || mannequin.level() != mc.level) mannequin = new Mannequin(mc.level);
        return mannequin;
    }

    /** Dessine le mannequin (coordonnees de l'ecran courant), tourne de « angle » radians, avec la tenue donnee. */
    static void dessiner(GuiGraphics g, float x, float y, int taille, float angle, Mannequin m) {
        if (m == null) return;
        m.yBodyRot = m.yBodyRotO = 180; m.setYRot(180); m.yHeadRot = m.yHeadRotO = 180; m.setXRot(0); m.xRotO = 0;
        m.tickCount = (int) (System.currentTimeMillis() / 50);
        Quaternionf q = new Quaternionf().rotateZ((float) Math.PI).rotateY(angle);
        g.pose().pushPose();
        g.pose().translate(x - Math.round(x), y - Math.round(y), 0);
        InventoryScreen.renderEntityInInventory(g, Math.round(x), Math.round(y), taille, q, null, m);
        g.pose().popPose();
    }

    private ClientCosm() {}
}
