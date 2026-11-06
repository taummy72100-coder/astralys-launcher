package fr.astralys.carte;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** Cote joueur des cosmetiques : ce que porte chacun, et la couche de rendu par-dessus le skin (et l'armure). */
final class ClientCosm {
    static Cosmetiques.Vue vue;
    static Map<UUID, List<String>> portes = Map.of();
    /** Apercu dans la boutique (survol d'une carte) : remplace la partie correspondante sur son propre joueur. */
    static final String[] apercu = new String[4];

    // pieds un peu plus gros que les jambes : les bottes passent par-dessus le pantalon sans scintiller
    private static final ModelLayerLocation COUCHE = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "cosmetique"), "main");
    private static final ModelLayerLocation PIEDS = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "cosmetique_pieds"), "main");

    static void init(IEventBus mod) {
        mod.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
            e.registerLayerDefinition(COUCHE, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.1f), 0), 64, 32));
            e.registerLayerDefinition(PIEDS, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.25f), 0), 64, 32));
        });
        mod.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (String skin : e.getSkins()) {
                LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> r = e.getSkin(skin);
                if (r != null) r.addLayer(new Couche(r, new HumanoidModel<>(e.getEntityModels().bakeLayer(COUCHE)), new HumanoidModel<>(e.getEntityModels().bakeLayer(PIEDS))));
            }
        });
    }

    static String porte(UUID u, int partie) {
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
            for (int partie = 0; partie < 4; partie++) {
                String id = porte(j.getUUID(), partie);
                if (id == null) continue;
                HumanoidModel<AbstractClientPlayer> m = partie == 3 ? pieds : corps;
                getParentModel().copyPropertiesTo(m);
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

    private ClientCosm() {}
}
