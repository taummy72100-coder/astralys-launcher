package fr.astralys.carte;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu des PNJ : un modele 3D par type (geometrie dans PnjModeles, generee depuis outils_modeles.py) et sa texture 128x128. */
final class PnjRendu extends HumanoidMobRenderer<PnjEntity, HumanoidModel<PnjEntity>> {
    static final ModelLayerLocation[] COUCHES = new ModelLayerLocation[PnjEntity.SKINS.length];
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[PnjEntity.SKINS.length];
    static {
        for (int i = 0; i < COUCHES.length; i++) {
            COUCHES[i] = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "pnj_" + PnjEntity.SKINS[i]), "main");
            TEXTURES[i] = new ResourceLocation(AstralysCarte.MODID, "textures/entity/pnj/" + PnjEntity.SKINS[i] + ".png");
        }
    }

    private final java.util.List<HumanoidModel<PnjEntity>> modeles = new java.util.ArrayList<>();

    PnjRendu(EntityRendererProvider.Context c) {
        super(c, new HumanoidModel<>(c.bakeLayer(COUCHES[0])), .5f);
        for (ModelLayerLocation l : COUCHES) modeles.add(new HumanoidModel<>(c.bakeLayer(l)));
    }

    @Override
    public void render(PnjEntity e, float lacet, float pt, PoseStack ps, MultiBufferSource b, int lumiere) {
        model = modeles.get(e.sorte());
        super.render(e, lacet, pt, ps, b, lumiere);
    }

    @Override public ResourceLocation getTextureLocation(PnjEntity e) { return TEXTURES[e.sorte()]; }
}
