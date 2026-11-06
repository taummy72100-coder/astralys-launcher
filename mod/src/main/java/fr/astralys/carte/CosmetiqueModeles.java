package fr.astralys.carte;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Geometrie des cosmetiques 3D (cote joueur). FICHIER GENERE par outils_cosmetiques.py : ne pas modifier a la main. */
final class CosmetiqueModeles {
    static final String[] BASES = {"chapeau_classieux", "casquette", "antennes_papillon", "chapeau_sorcier", "couronne", "oreilles_renard", "aureole", "casque_chevalier", "casque_viking", "casque_spartiate", "casque_astronaute", "masque_dragon", "jambieres_chevalier", "pantalon_pirate", "jambieres_runiques", "kilt", "bottes_cowboy", "baskets", "bottes_ailees", "bottes_lave"};

    static LayerDefinition couche(String base) {
        return switch (base) {
            case "chapeau_classieux" -> chapeau_classieux();
            case "casquette" -> casquette();
            case "antennes_papillon" -> antennes_papillon();
            case "chapeau_sorcier" -> chapeau_sorcier();
            case "couronne" -> couronne();
            case "oreilles_renard" -> oreilles_renard();
            case "aureole" -> aureole();
            case "casque_chevalier" -> casque_chevalier();
            case "casque_viking" -> casque_viking();
            case "casque_spartiate" -> casque_spartiate();
            case "casque_astronaute" -> casque_astronaute();
            case "masque_dragon" -> masque_dragon();
            case "jambieres_chevalier" -> jambieres_chevalier();
            case "pantalon_pirate" -> pantalon_pirate();
            case "jambieres_runiques" -> jambieres_runiques();
            case "kilt" -> kilt();
            case "bottes_cowboy" -> bottes_cowboy();
            case "baskets" -> baskets();
            case "bottes_ailees" -> bottes_ailees();
            case "bottes_lave" -> bottes_lave();
            default -> throw new IllegalArgumentException(base);
        };
    }

    private static LayerDefinition chapeau_classieux() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 15).addBox(-6.0F, -9.5F, -6.0F, 12.0F, 1.0F, 12.0F).texOffs(0, 0).addBox(-4.5F, -15.5F, -4.5F, 9.0F, 6.0F, 9.0F).texOffs(0, 28).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 1.0F, 9.0F, new CubeDeformation(0.2F)).texOffs(44, 28).addBox(3.9F, -11.5F, -1.5F, 1.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(36, 28).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F).texOffs(50, 28).addBox(-0.5F, -8.0F, 0.0F, 1.0F, 3.0F, 1.0F).texOffs(40, 28).addBox(-0.4F, -5.0F, 0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(4.6F, -11.0F, 0.0F, -0.5F, 0.0F, 0.25F));
        return LayerDefinition.create(m, 64, 64);
    }

    private static LayerDefinition casquette() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.4F)).texOffs(32, 0).addBox(-4.0F, -7.2F, -8.6F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.05F)).texOffs(0, 11).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(56, 0).addBox(-1.5F, -8.6F, -4.55F, 3.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition antennes_papillon() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-4.0F, -8.5F, -1.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.35F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("antenne_d", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F).texOffs(44, 0).addBox(-1.0F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(8, 0).addBox(-2.2F, -8.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, -8.8F, 0.0F, -0.25F, 0.0F, -0.35F));
        PartDefinition p2 = p0.addOrReplaceChild("antenne_g", CubeListBuilder.create().texOffs(4, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F).texOffs(48, 0).addBox(0.0F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(16, 0).addBox(0.2F, -8.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(2.0F, -8.8F, 0.0F, -0.25F, 0.0F, 0.35F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition chapeau_sorcier() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-6.5F, -9.5F, -6.5F, 13.0F, 1.0F, 13.0F).texOffs(0, 14).addBox(-4.0F, -12.5F, -4.0F, 8.0F, 3.0F, 8.0F).texOffs(32, 14).addBox(-4.0F, -10.5F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.2F)).texOffs(48, 25).addBox(-1.0F, -11.0F, -4.6F, 2.0F, 2.0F, 1.0F).texOffs(0, 25).addBox(-3.0F, -15.5F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("pointe", CubeListBuilder.create().texOffs(24, 25).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -15.5F, 0.0F, -0.45F, 0.0F, 0.1F));
        PartDefinition p2 = p1.addOrReplaceChild("bout", CubeListBuilder.create().texOffs(40, 25).addBox(-1.0F, -2.5F, -1.0F, 2.0F, 3.0F, 2.0F).texOffs(54, 25).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(58, 25).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.5F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 64);
    }

    private static LayerDefinition couronne() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.5F)).texOffs(32, 0).addBox(-3.5F, -12.5F, -4.6F, 1.0F, 2.0F, 1.0F).texOffs(36, 0).addBox(-3.5F, -12.5F, 3.6F, 1.0F, 2.0F, 1.0F).texOffs(40, 0).addBox(-4.6F, -12.5F, -3.5F, 1.0F, 2.0F, 1.0F).texOffs(44, 0).addBox(3.6F, -12.5F, -3.5F, 1.0F, 2.0F, 1.0F).texOffs(48, 0).addBox(-0.5F, -12.5F, -4.6F, 1.0F, 2.0F, 1.0F).texOffs(52, 0).addBox(-0.5F, -12.5F, 3.6F, 1.0F, 2.0F, 1.0F).texOffs(56, 0).addBox(-4.6F, -12.5F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(60, 0).addBox(3.6F, -12.5F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(0, 10).addBox(2.5F, -12.5F, -4.6F, 1.0F, 2.0F, 1.0F).texOffs(4, 10).addBox(2.5F, -12.5F, 3.6F, 1.0F, 2.0F, 1.0F).texOffs(8, 10).addBox(-4.6F, -12.5F, 2.5F, 1.0F, 2.0F, 1.0F).texOffs(12, 10).addBox(3.6F, -12.5F, 2.5F, 1.0F, 2.0F, 1.0F).texOffs(16, 10).addBox(-1.0F, -11.0F, -4.9F, 2.0F, 2.0F, 1.0F).texOffs(22, 10).addBox(-4.9F, -11.0F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(26, 10).addBox(3.9F, -11.0F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(30, 10).addBox(-0.5F, -13.5F, -4.6F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition oreilles_renard() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.5F, -1.5F, 8.0F, 1.0F, 3.0F, new CubeDeformation(0.35F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("oreille_d", CubeListBuilder.create().texOffs(22, 0).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F).texOffs(38, 0).addBox(-1.0F, -4.5F, -0.5F, 2.0F, 2.0F, 1.0F).texOffs(0, 4).addBox(-0.5F, -5.5F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(44, 0).addBox(-1.0F, -2.6F, -0.75F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-2.6F, -8.8F, 0.0F, 0.0F, 0.0F, -0.2F));
        PartDefinition p2 = p0.addOrReplaceChild("oreille_g", CubeListBuilder.create().texOffs(30, 0).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F).texOffs(50, 0).addBox(-1.0F, -4.5F, -0.5F, 2.0F, 2.0F, 1.0F).texOffs(4, 4).addBox(-0.5F, -5.5F, -0.5F, 1.0F, 1.0F, 1.0F).texOffs(56, 0).addBox(-1.0F, -2.6F, -0.75F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(2.6F, -8.8F, 0.0F, 0.0F, 0.0F, 0.2F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition aureole() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("anneau", CubeListBuilder.create().texOffs(28, 0).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 1.0F).texOffs(42, 0).addBox(-3.0F, 0.0F, 3.0F, 6.0F, 1.0F, 1.0F).texOffs(0, 0).addBox(-4.0F, 0.0F, -3.0F, 1.0F, 1.0F, 6.0F).texOffs(14, 0).addBox(3.0F, 0.0F, -3.0F, 1.0F, 1.0F, 6.0F).texOffs(56, 0).addBox(-3.6F, 0.0F, -3.6F, 1.0F, 1.0F, 1.0F).texOffs(60, 0).addBox(2.6F, 0.0F, -3.6F, 1.0F, 1.0F, 1.0F).texOffs(0, 7).addBox(-3.6F, 0.0F, 2.6F, 1.0F, 1.0F, 1.0F).texOffs(4, 7).addBox(2.6F, 0.0F, 2.6F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -12.5F, 0.0F, 0.12F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition casque_chevalier() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.2F)).texOffs(32, 0).addBox(-0.5F, -10.2F, -5.2F, 1.0F, 1.0F, 10.0F).texOffs(14, 16).addBox(-1.5F, -9.0F, -5.6F, 3.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("plumet", CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -5.0F, -1.0F, 1.0F, 5.0F, 3.0F).texOffs(0, 16).addBox(-0.5F, -4.0F, 2.0F, 1.0F, 4.0F, 3.0F).texOffs(8, 16).addBox(-0.5F, -2.0F, 5.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -10.0F, 1.0F, -0.35F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition casque_viking() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 5.0F, 8.0F, new CubeDeformation(1.2F)).texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(1.25F)).texOffs(0, 13).addBox(-0.5F, -4.5F, -5.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("corne_d", CubeListBuilder.create().texOffs(4, 13).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-5.0F, -6.5F, 0.0F, 0.0F, 0.0F, 0.5F));
        PartDefinition p2 = p1.addOrReplaceChild("pointe", CubeListBuilder.create().texOffs(12, 13).addBox(-1.0F, -2.5F, -0.75F, 1.0F, 3.0F, 1.0F).texOffs(28, 13).addBox(-1.0F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-1.6F, -0.6F, 0.0F, 0.0F, 0.0F, 0.7F));
        PartDefinition p3 = p0.addOrReplaceChild("corne_g", CubeListBuilder.create().texOffs(16, 13).addBox(0.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(5.0F, -6.5F, 0.0F, 0.0F, 0.0F, -0.5F));
        PartDefinition p4 = p3.addOrReplaceChild("pointe", CubeListBuilder.create().texOffs(24, 13).addBox(0.0F, -2.5F, -0.75F, 1.0F, 3.0F, 1.0F).texOffs(32, 13).addBox(0.0F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(1.6F, -0.6F, 0.0F, 0.0F, 0.0F, -0.7F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition casque_spartiate() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.2F)).texOffs(0, 16).addBox(-0.5F, -10.5F, -5.0F, 1.0F, 1.0F, 10.0F).texOffs(32, 0).addBox(-0.5F, -14.0F, -6.0F, 1.0F, 4.0F, 12.0F).texOffs(22, 16).addBox(-0.5F, -12.5F, 6.0F, 1.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition casque_astronaute() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.4F)).texOffs(0, 16).addBox(-3.5F, -6.5F, -5.8F, 7.0F, 5.0F, 1.0F).texOffs(32, 0).addBox(-4.0F, -1.2F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(1.6F)).texOffs(32, 16).addBox(3.6F, -12.5F, 1.0F, 1.0F, 3.0F, 1.0F).texOffs(36, 16).addBox(3.6F, -13.5F, 1.0F, 1.0F, 1.0F, 1.0F).texOffs(16, 16).addBox(-5.9F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F).texOffs(24, 16).addBox(4.9F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition masque_dragon() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.2F)).texOffs(32, 0).addBox(-3.0F, -4.5F, -8.6F, 6.0F, 3.0F, 4.0F).texOffs(0, 16).addBox(-3.0F, -2.2F, -8.4F, 6.0F, 1.0F, 4.0F).texOffs(48, 16).addBox(-2.5F, -3.6F, -8.8F, 1.0F, 1.0F, 1.0F).texOffs(52, 16).addBox(1.5F, -3.6F, -8.8F, 1.0F, 1.0F, 1.0F).texOffs(56, 16).addBox(-4.4F, -7.0F, -5.4F, 2.0F, 1.0F, 1.0F).texOffs(0, 21).addBox(2.4F, -7.0F, -5.4F, 2.0F, 1.0F, 1.0F).texOffs(28, 16).addBox(-0.5F, -10.8F, -3.0F, 1.0F, 2.0F, 1.0F).texOffs(32, 16).addBox(-0.5F, -10.4F, 0.0F, 1.0F, 2.0F, 1.0F).texOffs(36, 16).addBox(-0.5F, -9.6F, 3.0F, 1.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("corne_d", CubeListBuilder.create().texOffs(20, 16).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(40, 16).addBox(-0.5F, -5.0F, 0.2F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, -9.0F, 2.0F, 0.6F, 0.0F, -0.15F));
        PartDefinition p2 = p0.addOrReplaceChild("corne_g", CubeListBuilder.create().texOffs(24, 16).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(44, 16).addBox(-0.5F, -5.0F, 0.2F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -9.0F, 2.0F, 0.6F, 0.0F, 0.15F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition jambieres_chevalier() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(1.12F)).texOffs(32, 16).addBox(-1.0F, 10.0F, -3.4F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.62F)).texOffs(38, 16).addBox(-1.5F, 4.0F, -3.1F, 3.0F, 2.0F, 1.0F).texOffs(0, 16).addBox(-2.0F, 1.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.72F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.62F)).texOffs(46, 16).addBox(-1.5F, 4.0F, -3.1F, 3.0F, 2.0F, 1.0F).texOffs(16, 16).addBox(-2.0F, 1.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.72F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition pantalon_pirate() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, 9.5F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(1.15F)).texOffs(56, 0).addBox(2.0F, 11.0F, -3.5F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(0, 14).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(16, 14).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition jambieres_runiques() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(1.12F)).texOffs(24, 16).addBox(-1.0F, 10.0F, -3.4F, 2.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(32, 0).addBox(-0.5F, 1.0F, -3.0F, 1.0F, 8.0F, 1.0F).texOffs(40, 0).addBox(-2.6F, 2.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(36, 0).addBox(-0.5F, 1.0F, -3.0F, 1.0F, 8.0F, 1.0F).texOffs(44, 0).addBox(1.6F, 2.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 32);
    }

    private static LayerDefinition kilt() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 7.0F, 4.0F, new CubeDeformation(1.25F)).texOffs(0, 11).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(1.35F)).texOffs(56, 11).addBox(-1.5F, 11.5F, -4.0F, 3.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(24, 11).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.7F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p2 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F)).texOffs(40, 11).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.7F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition bottes_cowboy() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(32, 0).addBox(-1.5F, 10.2F, -4.2F, 3.0F, 2.0F, 2.0F).texOffs(52, 0).addBox(-1.5F, 12.2F, 1.5F, 3.0F, 1.0F, 2.0F).texOffs(10, 10).addBox(-0.5F, 10.5F, 3.3F, 1.0F, 1.0F, 1.0F).texOffs(14, 10).addBox(-1.0F, 10.5F, 4.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(42, 0).addBox(-1.5F, 10.2F, -4.2F, 3.0F, 2.0F, 2.0F).texOffs(0, 10).addBox(-1.5F, 12.2F, 1.5F, 3.0F, 1.0F, 2.0F).texOffs(20, 10).addBox(-0.5F, 10.5F, 3.3F, 1.0F, 1.0F, 1.0F).texOffs(24, 10).addBox(-1.0F, 10.5F, 4.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition baskets() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(32, 0).addBox(-2.0F, 11.6F, -2.6F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.75F)).texOffs(18, 7).addBox(-1.5F, 9.6F, -3.9F, 3.0F, 2.0F, 1.0F).texOffs(34, 7).addBox(-1.0F, 8.2F, -3.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(0, 7).addBox(-2.0F, 11.6F, -2.6F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.75F)).texOffs(26, 7).addBox(-1.5F, 9.6F, -3.9F, 3.0F, 2.0F, 1.0F).texOffs(40, 7).addBox(-1.0F, 8.2F, -3.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition bottes_ailees() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(0, 9).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(1.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p1 = p0.addOrReplaceChild("aile", CubeListBuilder.create().texOffs(32, 0).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 3.0F, 3.0F).texOffs(40, 0).addBox(-0.5F, -5.0F, 1.5F, 1.0F, 3.0F, 3.0F).texOffs(32, 9).addBox(-0.5F, -6.5F, 3.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-3.1F, 8.5F, 0.5F, 0.0F, 0.45F, 0.35F));
        PartDefinition p2 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(16, 9).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(1.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition p3 = p2.addOrReplaceChild("aile", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 3.0F, 3.0F).texOffs(56, 0).addBox(-0.5F, -5.0F, 1.5F, 1.0F, 3.0F, 3.0F).texOffs(38, 9).addBox(-0.5F, -6.5F, 3.0F, 1.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(3.1F, 8.5F, 0.5F, 0.0F, -0.45F, -0.35F));
        return LayerDefinition.create(m, 64, 16);
    }

    private static LayerDefinition bottes_lave() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition p0 = r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(32, 0).addBox(-2.0F, 11.8F, -2.4F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.6F)).texOffs(18, 10).addBox(-1.0F, 9.0F, -3.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p1 = r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(0, 10).addBox(-2.0F, 11.8F, -2.4F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.6F)).texOffs(24, 10).addBox(-1.0F, 9.0F, -3.1F, 2.0F, 1.0F, 1.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(m, 64, 16);
    }

    private CosmetiqueModeles() {}
}
