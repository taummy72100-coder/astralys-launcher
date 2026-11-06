package fr.astralys.carte;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometrie des familiers. FICHIER GENERE par outils_modeles.py : ne pas modifier a la main. */
final class FamilierModeles {
    static LayerDefinition couche(String id) {
        return switch (id) {
            case "golem_cristal" -> golem_cristal();
            case "bourdon_dore" -> bourdon_dore();
            case "faucon_spectral" -> faucon_spectral();
            case "fee_ether" -> fee_ether();
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static LayerDefinition golem_cristal() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F).texOffs(24, 0).addBox(-2.0F, -6.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(48, 0).addBox(0.5F, -5.0F, 0.0F, 2.0F, 2.0F, 2.0F).texOffs(56, 0).addBox(-1.0F, -8.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(4.0F, 18.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(-4.0F, 18.0F, 0.0F));
        return LayerDefinition.create(m, 64, 64);
    }

    private static LayerDefinition bourdon_dore() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 6.0F).texOffs(22, 0).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 3.0F, 2.0F).texOffs(18, 10).addBox(-0.5F, -0.5F, 3.0F, 1.0F, 1.0F, 2.0F).texOffs(24, 10).addBox(-1.5F, -4.0F, -4.5F, 1.0F, 2.0F, 1.0F).texOffs(28, 10).addBox(0.5F, -4.0F, -4.5F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(34, 0).addBox(0.0F, 0.0F, -1.0F, 5.0F, 1.0F, 4.0F), PartPose.offset(1.0F, 15.0F, -0.5F));
        PartDefinition p2 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(0, 10).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 1.0F, 4.0F), PartPose.offset(-1.0F, 15.0F, -0.5F));
        return LayerDefinition.create(m, 64, 64);
    }

    private static LayerDefinition faucon_spectral() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F).texOffs(20, 0).addBox(-1.5F, -4.0F, -5.0F, 3.0F, 3.0F, 3.0F).texOffs(34, 10).addBox(-0.5F, -2.5F, -6.0F, 1.0F, 1.0F, 1.0F).texOffs(22, 10).addBox(-1.5F, -1.0F, 3.0F, 3.0F, 1.0F, 3.0F).texOffs(38, 10).addBox(-1.0F, 2.0F, -1.0F, 1.0F, 1.0F, 1.0F).texOffs(42, 10).addBox(0.0F, 2.0F, -1.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(32, 0).addBox(0.0F, 0.0F, -2.0F, 6.0F, 1.0F, 5.0F), PartPose.offset(2.0F, 15.5F, -1.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(0, 10).addBox(-6.0F, 0.0F, -2.0F, 6.0F, 1.0F, 5.0F), PartPose.offset(-2.0F, 15.5F, -1.0F));
        return LayerDefinition.create(m, 64, 64);
    }

    private static LayerDefinition fee_ether() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 3.0F, 3.0F).texOffs(32, 0).addBox(-2.0F, -6.5F, -2.0F, 4.0F, 1.0F, 4.0F).texOffs(48, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(0, 6).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F).texOffs(12, 6).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(12, 0).addBox(0.0F, -3.0F, 0.0F, 4.0F, 5.0F, 1.0F), PartPose.offset(1.0F, 13.0F, 1.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(22, 0).addBox(-4.0F, -3.0F, 0.0F, 4.0F, 5.0F, 1.0F), PartPose.offset(-1.0F, 13.0F, 1.0F));
        return LayerDefinition.create(m, 64, 64);
    }

    private FamilierModeles() {}
}
