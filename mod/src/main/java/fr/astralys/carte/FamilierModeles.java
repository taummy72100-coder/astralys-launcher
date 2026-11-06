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
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 7.0F, 6.0F).texOffs(56, 0).addBox(-3.0F, 3.0F, -2.5F, 6.0F, 2.0F, 5.0F).texOffs(44, 13).addBox(-1.5F, -2.0F, -3.6F, 3.0F, 3.0F, 1.0F).texOffs(78, 0).addBox(-6.0F, -4.5F, -2.0F, 2.0F, 3.0F, 4.0F).texOffs(90, 0).addBox(4.0F, -4.5F, -2.0F, 2.0F, 3.0F, 4.0F).texOffs(68, 13).addBox(-6.0F, -6.5F, -1.0F, 1.0F, 2.0F, 1.0F).texOffs(72, 13).addBox(5.0F, -6.5F, 0.0F, 1.0F, 2.0F, 1.0F).texOffs(102, 0).addBox(-3.0F, -9.0F, 1.0F, 2.0F, 5.0F, 2.0F).texOffs(48, 0).addBox(0.0F, -10.0F, 1.5F, 2.0F, 6.0F, 2.0F).texOffs(52, 13).addBox(2.0F, -7.0F, 0.5F, 1.0F, 3.0F, 1.0F).texOffs(56, 13).addBox(-1.0F, -7.0F, 2.5F, 1.0F, 3.0F, 1.0F).texOffs(76, 13).addBox(-4.0F, -6.0F, 0.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F).texOffs(80, 13).addBox(-3.0F, -4.5F, -3.0F, 6.0F, 1.0F, 2.0F).texOffs(96, 13).addBox(-2.0F, -6.0F, -1.0F, 1.0F, 2.0F, 1.0F).texOffs(100, 13).addBox(1.0F, -6.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, -4.0F, -1.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(110, 0).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F).texOffs(0, 13).addBox(-1.5F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F).texOffs(108, 13).addBox(0.0F, 6.5F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(5.0F, 14.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(16, 13).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F).texOffs(28, 13).addBox(-2.5F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F).texOffs(112, 13).addBox(-1.0F, 6.5F, -2.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-5.0F, 14.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("debris", CubeListBuilder.create().texOffs(60, 13).addBox(6.0F, -6.0F, 2.0F, 2.0F, 2.0F, 2.0F).texOffs(116, 13).addBox(-8.0F, -1.0F, 3.0F, 1.0F, 1.0F, 1.0F).texOffs(104, 13).addBox(6.0F, 3.0F, -4.0F, 1.0F, 2.0F, 1.0F).texOffs(120, 13).addBox(-7.0F, -7.0F, -3.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
        return LayerDefinition.create(m, 128, 32);
    }

    private static LayerDefinition bourdon_dore() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, -2.5F, -2.0F, 5.0F, 5.0F, 4.0F).texOffs(0, 0).addBox(-3.0F, -2.0F, 2.0F, 6.0F, 5.0F, 6.0F).texOffs(56, 0).addBox(-2.0F, -1.5F, 8.0F, 4.0F, 4.0F, 1.0F).texOffs(48, 11).addBox(-0.5F, 0.0F, 9.0F, 1.0F, 1.0F, 2.0F).texOffs(122, 0).addBox(-2.0F, 2.5F, -1.0F, 1.0F, 3.0F, 1.0F).texOffs(0, 11).addBox(-2.0F, 2.5F, 1.0F, 1.0F, 3.0F, 1.0F).texOffs(4, 11).addBox(-2.0F, 2.5F, 3.0F, 1.0F, 3.0F, 1.0F).texOffs(8, 11).addBox(1.0F, 2.5F, -1.0F, 1.0F, 3.0F, 1.0F).texOffs(12, 11).addBox(1.0F, 2.5F, 1.0F, 1.0F, 3.0F, 1.0F).texOffs(16, 11).addBox(1.0F, 2.5F, 3.0F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(42, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 3.0F).texOffs(66, 0).addBox(-3.0F, -1.5F, -2.5F, 1.0F, 3.0F, 2.0F).texOffs(72, 0).addBox(2.0F, -1.5F, -2.5F, 1.0F, 3.0F, 2.0F).texOffs(54, 11).addBox(-1.5F, -4.0F, -2.5F, 1.0F, 2.0F, 1.0F).texOffs(58, 11).addBox(0.5F, -4.0F, -2.5F, 1.0F, 2.0F, 1.0F).texOffs(62, 11).addBox(-2.0F, -5.0F, -3.0F, 1.0F, 1.0F, 1.0F).texOffs(66, 11).addBox(1.0F, -5.0F, -3.0F, 1.0F, 1.0F, 1.0F).texOffs(70, 11).addBox(-1.0F, 1.5F, -3.5F, 2.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(78, 0).addBox(0.0F, -0.5F, -1.0F, 7.0F, 1.0F, 4.0F).texOffs(20, 11).addBox(0.0F, -0.4F, 3.0F, 4.0F, 1.0F, 3.0F), PartPose.offset(1.5F, 13.5F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(100, 0).addBox(-7.0F, -0.5F, -1.0F, 7.0F, 1.0F, 4.0F).texOffs(34, 11).addBox(-4.0F, -0.4F, 3.0F, 4.0F, 1.0F, 3.0F), PartPose.offset(-1.5F, 13.5F, 0.0F));
        return LayerDefinition.create(m, 128, 16);
    }

    private static LayerDefinition faucon_spectral() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.5F, -3.0F, 4.0F, 5.0F, 6.0F).texOffs(16, 11).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 4.0F, 1.0F).texOffs(60, 0).addBox(-2.5F, -3.0F, -1.0F, 5.0F, 1.0F, 5.0F).texOffs(54, 11).addBox(-1.5F, 2.5F, -1.0F, 1.0F, 2.0F, 1.0F).texOffs(58, 11).addBox(0.5F, 2.5F, -1.0F, 1.0F, 2.0F, 1.0F).texOffs(62, 11).addBox(-2.0F, 4.0F, -2.0F, 2.0F, 1.0F, 2.0F).texOffs(70, 11).addBox(0.0F, 4.0F, -2.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(80, 0).addBox(-1.5F, -3.0F, -2.5F, 3.0F, 3.0F, 3.0F).texOffs(78, 11).addBox(-0.5F, -1.8F, -4.0F, 1.0F, 1.0F, 2.0F).texOffs(92, 11).addBox(-0.5F, -1.0F, -4.5F, 1.0F, 1.0F, 1.0F).texOffs(48, 11).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 2.0F, 2.0F), PartPose.offset(0.0F, -2.5F, -3.0F));
        PartDefinition p2 = p0.addOrReplaceChild("queue", CubeListBuilder.create().texOffs(92, 0).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 1.0F, 5.0F).texOffs(84, 11).addBox(-1.0F, -0.2F, 4.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(0.0F, -1.0F, 3.0F));
        PartDefinition p3 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(20, 0).addBox(0.0F, 0.0F, -2.0F, 4.0F, 1.0F, 6.0F).texOffs(110, 0).addBox(4.0F, 0.2F, -1.5F, 3.0F, 1.0F, 5.0F).texOffs(24, 11).addBox(7.0F, 0.4F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(2.0F, 14.0F, -1.0F));
        PartDefinition p4 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 1.0F, 6.0F).texOffs(0, 11).addBox(-7.0F, 0.2F, -1.5F, 3.0F, 1.0F, 5.0F).texOffs(36, 11).addBox(-9.0F, 0.4F, -1.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(-2.0F, 14.0F, -1.0F));
        return LayerDefinition.create(m, 128, 32);
    }

    private static LayerDefinition fee_ether() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("corps", CubeListBuilder.create().texOffs(102, 0).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 2.0F).texOffs(112, 0).addBox(-2.0F, 0.0F, -1.5F, 4.0F, 2.0F, 3.0F).texOffs(52, 0).addBox(-2.5F, 2.0F, -2.0F, 5.0F, 2.0F, 4.0F).texOffs(4, 8).addBox(-1.0F, 4.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(8, 8).addBox(0.0F, 4.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(12, 8).addBox(-2.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(16, 8).addBox(1.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(0, 8).addBox(-3.5F, -6.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(44, 8).addBox(-4.0F, -7.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("tete", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F).texOffs(70, 0).addBox(-2.5F, -4.5F, -2.5F, 5.0F, 1.0F, 5.0F).texOffs(16, 0).addBox(-2.5F, -4.0F, 2.0F, 5.0F, 6.0F, 1.0F).texOffs(20, 8).addBox(-1.0F, -6.5F, 0.0F, 2.0F, 2.0F, 2.0F).texOffs(90, 0).addBox(-2.5F, -3.0F, -2.5F, 1.0F, 4.0F, 2.0F).texOffs(96, 0).addBox(1.5F, -3.0F, -2.5F, 1.0F, 4.0F, 2.0F).texOffs(52, 8).addBox(-0.5F, -5.0F, -2.6F, 1.0F, 1.0F, 1.0F).texOffs(56, 8).addBox(-3.0F, -2.5F, 0.0F, 1.0F, 1.0F, 1.0F).texOffs(60, 8).addBox(2.0F, -2.5F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("aile_g", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -5.0F, 0.0F, 5.0F, 6.0F, 1.0F).texOffs(28, 8).addBox(0.0F, 1.0F, 0.0F, 3.0F, 3.0F, 1.0F), PartPose.offset(0.5F, 12.5F, 1.2F));
        PartDefinition p3 = r.addOrReplaceChild("aile_d", CubeListBuilder.create().texOffs(40, 0).addBox(-5.0F, -5.0F, 0.0F, 5.0F, 6.0F, 1.0F).texOffs(36, 8).addBox(-3.0F, 1.0F, 0.0F, 3.0F, 3.0F, 1.0F), PartPose.offset(-0.5F, 12.5F, 1.2F));
        PartDefinition p4 = r.addOrReplaceChild("debris", CubeListBuilder.create().texOffs(64, 8).addBox(4.0F, -6.0F, 2.0F, 1.0F, 1.0F, 1.0F).texOffs(68, 8).addBox(-5.0F, -2.0F, -3.0F, 1.0F, 1.0F, 1.0F).texOffs(72, 8).addBox(3.0F, 3.0F, -4.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 16);
    }

    private FamilierModeles() {}
}
