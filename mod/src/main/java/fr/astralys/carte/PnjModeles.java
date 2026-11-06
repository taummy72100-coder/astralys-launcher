package fr.astralys.carte;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometrie des PNJ. FICHIER GENERE par outils_modeles.py : ne pas modifier a la main. */
final class PnjModeles {
    static LayerDefinition couche(String skin) {
        return switch (skin) {
            case "marchand" -> marchand();
            case "maitre_metiers" -> maitre_metiers();
            case "heraut" -> heraut();
            case "commissaire" -> commissaire();
            case "guide" -> guide();
            case "archimage" -> archimage();
            case "maitre_forge" -> maitre_forge();
            case "enchanteresse" -> enchanteresse();
            default -> throw new IllegalArgumentException(skin);
        };
    }

    private static LayerDefinition marchand() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(102, 29).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 1.0F, 1.0F).texOffs(28, 16).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 3.0F, 9.0F).texOffs(84, 16).addBox(-3.5F, -11.0F, -3.5F, 7.0F, 2.0F, 7.0F).texOffs(90, 29).addBox(-1.0F, -10.0F, -5.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(112, 16).addBox(-0.5F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -11.0F, -3.0F, -0.3F, 0.0F, 0.25F));
        PartDefinition p2 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(0, 16).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 8.0F, 5.0F).texOffs(0, 29).addBox(-4.5F, 7.5F, -2.5F, 9.0F, 2.0F, 5.0F).texOffs(96, 29).addBox(2.0F, 9.5F, -3.5F, 2.0F, 2.0F, 1.0F).texOffs(64, 16).addBox(-3.5F, 1.0F, 2.5F, 7.0F, 8.0F, 3.0F).texOffs(68, 29).addBox(-4.5F, -0.5F, 3.0F, 9.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(88, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(28, 29).addBox(-2.5F, 10.0F, -3.0F, 5.0F, 2.0F, 5.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p7 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(104, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(48, 29).addBox(-2.5F, 10.0F, -3.0F, 5.0F, 2.0F, 5.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition maitre_metiers() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(72, 28).addBox(-3.0F, -2.0F, -5.0F, 6.0F, 4.0F, 1.0F).texOffs(28, 16).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 2.0F, 9.0F).texOffs(96, 16).addBox(-3.5F, -10.0F, -3.5F, 7.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(124, 16).addBox(0.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -10.0F, 1.0F, 0.0F, 0.0F, -0.45F));
        PartDefinition p2 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(64, 16).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 3.0F, 6.0F).texOffs(0, 28).addBox(-5.0F, 10.0F, -3.0F, 10.0F, 2.0F, 6.0F).texOffs(0, 16).addBox(-4.5F, 12.0F, -2.5F, 9.0F, 7.0F, 5.0F).texOffs(86, 28).addBox(-3.0F, 3.0F, -2.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(32, 28).addBox(-3.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(52, 28).addBox(-1.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(88, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p7 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(104, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition heraut() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(32, 16).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 2.0F, 9.0F).texOffs(86, 32).addBox(-1.0F, -10.0F, -5.0F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(68, 16).addBox(0.0F, -7.0F, 0.0F, 1.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(2.0F, -9.0F, 2.0F, 0.3F, 0.0F, -0.4F));
        PartDefinition p2 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(56, 0).addBox(-3.5F, 1.0F, -3.0F, 7.0F, 15.0F, 1.0F).texOffs(72, 0).addBox(-3.5F, 1.0F, 2.0F, 7.0F, 15.0F, 1.0F).texOffs(48, 32).addBox(-5.0F, 10.0F, -3.0F, 10.0F, 2.0F, 6.0F).texOffs(80, 32).addBox(4.5F, 8.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(88, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(74, 16).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(96, 16).addBox(-4.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(104, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(0, 32).addBox(-1.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(22, 32).addBox(-1.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p7 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition commissaire() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(60, 31).addBox(-3.0F, -3.0F, -4.5F, 6.0F, 1.0F, 1.0F).texOffs(32, 16).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 1.0F, 10.0F).texOffs(0, 16).addBox(-4.0F, -16.0F, -4.0F, 8.0F, 7.0F, 8.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(74, 31).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 1.0F, 1.0F).texOffs(32, 31).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 1.0F, 5.0F).texOffs(72, 16).addBox(-4.5F, 12.0F, -2.5F, 9.0F, 6.0F, 5.0F).texOffs(82, 31).addBox(2.0F, 6.0F, -2.5F, 1.0F, 1.0F, 1.0F).texOffs(0, 31).addBox(-5.0F, 10.0F, -3.0F, 10.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(72, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(88, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(104, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition guide() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(56, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(20, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F).texOffs(32, 39).addBox(-2.0F, -7.0F, 4.5F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(88, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(88, 23).addBox(-5.0F, -0.5F, -3.0F, 10.0F, 4.0F, 6.0F).texOffs(0, 0).addBox(-4.5F, 0.0F, 2.5F, 9.0F, 22.0F, 1.0F).texOffs(0, 39).addBox(-5.0F, 9.0F, -3.0F, 10.0F, 2.0F, 6.0F).texOffs(52, 39).addBox(-4.5F, 10.5F, -3.5F, 2.0F, 2.0F, 1.0F).texOffs(44, 39).addBox(3.5F, 10.0F, -2.0F, 2.0F, 3.0F, 2.0F).texOffs(120, 23).addBox(-1.5F, 2.0F, 3.5F, 3.0F, 9.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(112, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 23).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(48, 23).addBox(-2.5F, 5.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(32, 23).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(68, 23).addBox(-2.5F, 5.0F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition archimage() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(96, 44).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 7.0F, 1.0F).texOffs(72, 17).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F).texOffs(0, 33).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 2.0F, 9.0F).texOffs(36, 33).addBox(-4.0F, -14.0F, -4.0F, 8.0F, 3.0F, 8.0F).texOffs(68, 33).addBox(-3.0F, -17.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("pointe", CubeListBuilder.create().texOffs(32, 53).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F).texOffs(48, 53).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(56, 53).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -17.0F, 0.0F, -0.35F, 0.0F, 0.12F));
        PartDefinition p2 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(60, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(92, 33).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 3.0F, 6.0F).texOffs(0, 53).addBox(-5.0F, 10.0F, -3.0F, 10.0F, 2.0F, 6.0F).texOffs(0, 0).addBox(-4.5F, 12.0F, -2.5F, 9.0F, 12.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(84, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(0, 44).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(22, 44).addBox(-4.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F).texOffs(32, 17).addBox(-3.5F, 3.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(100, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(48, 44).addBox(-1.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(70, 44).addBox(-1.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F).texOffs(52, 17).addBox(-1.5F, 3.0F, -2.5F, 5.0F, 9.0F, 5.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 17).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p7 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 17).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition maitre_forge() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(18, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(62, 17).addBox(-4.5F, -6.5F, -4.5F, 9.0F, 1.0F, 9.0F).texOffs(40, 42).addBox(-3.5F, -7.5F, -5.0F, 2.0F, 2.0F, 1.0F).texOffs(46, 42).addBox(1.5F, -7.5F, -5.0F, 2.0F, 2.0F, 1.0F).texOffs(16, 17).addBox(-1.0F, -4.0F, 4.0F, 2.0F, 10.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(50, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(0, 0).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 16.0F, 1.0F).texOffs(0, 42).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 2.0F, 5.0F).texOffs(28, 42).addBox(4.5F, 9.0F, -1.0F, 1.0F, 5.0F, 2.0F).texOffs(34, 42).addBox(-5.5F, 9.0F, -1.0F, 1.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(74, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(98, 17).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(0, 33).addBox(-4.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F).texOffs(22, 17).addBox(-3.5F, 5.0F, -2.5F, 5.0F, 6.0F, 5.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(90, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(26, 33).addBox(-1.0F, -3.0F, -3.0F, 5.0F, 3.0F, 6.0F).texOffs(48, 33).addBox(-1.5F, -4.0F, -3.5F, 6.0F, 2.0F, 7.0F).texOffs(42, 17).addBox(-1.5F, 5.0F, -2.5F, 5.0F, 6.0F, 5.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(106, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(74, 33).addBox(-2.5F, 8.0F, -2.5F, 5.0F, 4.0F, 5.0F).texOffs(52, 42).addBox(-2.5F, 10.0F, -3.5F, 5.0F, 2.0F, 1.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 17).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(94, 33).addBox(-2.5F, 8.0F, -2.5F, 5.0F, 4.0F, 5.0F).texOffs(64, 42).addBox(-2.5F, 10.0F, -3.5F, 5.0F, 2.0F, 1.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private static LayerDefinition enchanteresse() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F).texOffs(32, 18).addBox(-4.0F, -7.0F, 4.0F, 8.0F, 13.0F, 1.0F).texOffs(50, 18).addBox(-5.0F, -6.0F, -3.0F, 1.0F, 9.0F, 3.0F).texOffs(58, 18).addBox(4.0F, -6.0F, -3.0F, 1.0F, 9.0F, 3.0F).texOffs(0, 34).addBox(-4.5F, -7.5F, -4.5F, 9.0F, 1.0F, 9.0F).texOffs(68, 44).addBox(-1.0F, -9.0F, -5.0F, 2.0F, 2.0F, 1.0F).texOffs(82, 44).addBox(-2.5F, -8.5F, -4.8F, 5.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(60, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F).texOffs(0, 44).addBox(-4.5F, -0.5F, -2.5F, 9.0F, 2.0F, 5.0F).texOffs(36, 34).addBox(-5.0F, 9.0F, -3.0F, 10.0F, 2.0F, 6.0F).texOffs(74, 44).addBox(-3.0F, 11.0F, -3.5F, 1.0F, 2.0F, 1.0F).texOffs(78, 44).addBox(2.0F, 11.0F, -3.5F, 1.0F, 2.0F, 1.0F).texOffs(94, 44).addBox(-1.0F, 11.0F, -3.5F, 1.0F, 1.0F, 1.0F).texOffs(0, 0).addBox(-4.5F, 11.0F, -2.5F, 9.0F, 13.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p3 = r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(84, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(66, 18).addBox(-3.5F, 4.0F, -2.5F, 5.0F, 7.0F, 5.0F).texOffs(68, 34).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 2.0F, 6.0F).texOffs(28, 44).addBox(-3.5F, 8.0F, -2.5F, 5.0F, 1.0F, 5.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p4 = r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(100, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F).texOffs(86, 18).addBox(-1.5F, 4.0F, -2.5F, 5.0F, 7.0F, 5.0F).texOffs(90, 34).addBox(-1.0F, -3.0F, -3.0F, 5.0F, 2.0F, 6.0F).texOffs(48, 44).addBox(-1.5F, 8.0F, -2.5F, 5.0F, 1.0F, 5.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p5 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p6 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 128, 128);
    }

    private PnjModeles() {}
}
