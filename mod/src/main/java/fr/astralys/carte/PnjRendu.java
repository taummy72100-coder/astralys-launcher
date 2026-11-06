package fr.astralys.carte;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Rendu des PNJ : modele de joueur classique + skin selon le type. */
final class PnjRendu extends HumanoidMobRenderer<PnjEntity, PlayerModel<PnjEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[PnjEntity.SKINS.length];
    static { for (int i = 0; i < SKINS.length; i++) SKINS[i] = new ResourceLocation(AstralysCarte.MODID, "textures/entity/pnj/" + PnjEntity.SKINS[i] + ".png"); }

    PnjRendu(EntityRendererProvider.Context c) { super(c, new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER), false), .5f); }

    @Override public ResourceLocation getTextureLocation(PnjEntity e) { return SKINS[e.sorte()]; }
}
