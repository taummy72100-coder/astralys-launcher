package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Rendu des pets : modele 3D par pet (geometrie generee dans FamilierModeles), ailes qui battent, flottement, toujours lumineux. */
final class FamilierRendu extends MobRenderer<Familiers.Pet, FamilierRendu.Modele> {
    static final ModelLayerLocation[] COUCHES = new ModelLayerLocation[Familiers.IDS.length];
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[Familiers.IDS.length];
    static {
        for (int i = 0; i < COUCHES.length; i++) {
            COUCHES[i] = new ModelLayerLocation(new ResourceLocation(AstralysCarte.MODID, "pet_" + Familiers.IDS[i]), "main");
            TEXTURES[i] = new ResourceLocation(AstralysCarte.MODID, "textures/entity/familier/" + Familiers.IDS[i] + ".png");
        }
    }

    private final List<Modele> modeles = new ArrayList<>();

    FamilierRendu(EntityRendererProvider.Context c) {
        super(c, new Modele(c.bakeLayer(COUCHES[0]), 0), .2f);
        for (int i = 0; i < COUCHES.length; i++) modeles.add(new Modele(c.bakeLayer(COUCHES[i]), i));
    }

    @Override
    public void render(Familiers.Pet e, float lacet, float pt, PoseStack ps, MultiBufferSource b, int lumiere) {
        model = modeles.get(e.sorte());
        super.render(e, lacet, pt, ps, b, lumiere);
    }

    @Override public ResourceLocation getTextureLocation(Familiers.Pet e) { return TEXTURES[e.sorte()]; }
    @Override protected int getBlockLightLevel(Familiers.Pet e, BlockPos p) { return 15; }
    @Override protected boolean shouldShowName(Familiers.Pet e) { return false; }
    @Override protected void scale(Familiers.Pet e, PoseStack ps, float pt) { float k = e.isVehicle() ? Familiers.TAILLE_MONTURE[e.sorte()] : 1f; ps.scale(k, k, k); }

    static final class Modele extends EntityModel<Familiers.Pet> {
        private final ModelPart racine, corps, ag, ad, debris;
        private final float ay, agz, adz;
        private final int sorte;

        Modele(ModelPart r, int sorte) {
            racine = r; this.sorte = sorte;
            corps = r.hasChild("corps") ? r.getChild("corps") : null;
            ag = r.hasChild("aile_g") ? r.getChild("aile_g") : null;
            ad = r.hasChild("aile_d") ? r.getChild("aile_d") : null;
            debris = r.hasChild("debris") ? r.getChild("debris") : null;
            ay = r.y; agz = ag == null ? 0 : ag.zRot; adz = ad == null ? 0 : ad.zRot;
        }

        @Override
        public void setupAnim(Familiers.Pet e, float a, float b, float age, float lacet, float tangage) {
            racine.y = ay + (float) Math.sin(age * .15f) * 1.2f;
            float vitesse = sorte == 1 ? 2.2f : sorte == 0 ? .3f : .8f, ampl = sorte == 0 ? .15f : .7f;
            float battement = (float) Math.sin(age * vitesse) * ampl;
            if (sorte == 3) {   // ailes de fee : verticales, elles battent d'avant en arriere
                if (ag != null) ag.yRot = -.35f - Math.abs(battement);
                if (ad != null) ad.yRot = .35f + Math.abs(battement);
            } else {
                if (ag != null) ag.zRot = agz - battement;
                if (ad != null) ad.zRot = adz + battement;
            }
            if (debris != null) debris.yRot = age * .06f;   // eclats et etincelles en orbite
            if (corps != null) corps.yRot = sorte == 0 ? age * .03f : 0;
        }

        @Override
        public void renderToBuffer(PoseStack ps, VertexConsumer vc, int lumiere, int ombre, float r, float g, float b, float a) {
            racine.render(ps, vc, lumiere, ombre, r, g, b, a);
        }
    }
}
