package fr.astralys.carte;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/** Emotes (cote joueur) : le serveur relaie qui joue quoi, le modele du joueur prend la pose image par image. */
final class Emotes {
    private record Jeu(String id, long debut) {}

    private static final Map<UUID, Jeu> ACTIFS = new HashMap<>();
    static final float DUREE = 3.5f;

    static void jouer(UUID joueur, String id) { ACTIFS.put(joueur, new Jeu(id, System.currentTimeMillis())); }

    /** Modele du joueur qui applique l'emote en cours par-dessus l'animation normale. */
    static final class Modele extends PlayerModel<AbstractClientPlayer> {
        Modele(ModelPart racine, boolean fin) { super(racine, fin); }

        @Override
        public void setupAnim(AbstractClientPlayer j, float marche, float vitesse, float age, float lacet, float tangage) {
            super.setupAnim(j, marche, vitesse, age, lacet, tangage);
            if (!appliquer(this, j, vitesse)) return;
            leftPants.copyFrom(leftLeg); rightPants.copyFrom(rightLeg);
            leftSleeve.copyFrom(leftArm); rightSleeve.copyFrom(rightArm);
            jacket.copyFrom(body); hat.copyFrom(head);
        }
    }

    private static boolean appliquer(PlayerModel<?> m, AbstractClientPlayer j, float vitesse) {
        String id;
        float t;
        if (j instanceof ClientCosm.Mannequin mq) {
            if (mq.emote == null) return false;
            id = mq.emote; t = ((System.currentTimeMillis() - mq.emoteDebut) / 1000f) % DUREE;
        } else {
            Jeu e = ACTIFS.get(j.getUUID());
            if (e == null) return false;
            t = (System.currentTimeMillis() - e.debut) / 1000f;
            if (t > DUREE || vitesse > .15f || j.isSwimming() || j.isFallFlying()) { ACTIFS.remove(j.getUUID()); return false; }   // on bouge : l'emote s'arrete
            id = e.id;
        }
        float k = Math.min(1, t / .25f) * Math.min(1, (DUREE - t) / .3f);   // entree et sortie en douceur
        k = Math.max(0, k);
        float s = Mth.sin(t * 12), s6 = Mth.sin(t * 6);
        switch (id) {
            case "salut" -> { bras(m.rightArm, -2.9f, 0, .25f + s * .35f, k); m.head.zRot = .1f * k; }
            case "applaudir" -> {
                float v = .55f + Mth.sin(t * 18) * .3f;
                bras(m.rightArm, -1.25f, -v, 0, k); bras(m.leftArm, -1.25f, v, 0, k);
            }
            case "reverence" -> {
                float b = Math.min(1, t / .6f) * Math.min(1, (DUREE - t) / .6f);
                accroupi(m, b * k);
                m.head.xRot += .6f * b * k;
                bras(m.rightArm, -.9f, .5f, 0, b * k); bras(m.leftArm, .6f, 0, 0, b * k);
            }
            case "reflexion" -> {
                bras(m.rightArm, -2.1f, -.65f, 0, k); bras(m.leftArm, -.75f, .85f, 0, k);
                m.head.xRot = Mth.lerp(k, m.head.xRot, .25f); m.head.zRot = .15f * k * Mth.sin(t * 2);
            }
            case "victoire" -> {
                float p = Math.abs(Mth.sin(t * 7)) * .25f;
                bras(m.rightArm, -2.9f + p, 0, .35f, k); bras(m.leftArm, -2.9f + p, 0, -.35f, k);
                m.head.xRot = Mth.lerp(k, m.head.xRot, -.35f);
            }
            case "facepalm" -> {
                bras(m.rightArm, -2.45f, -.5f, 0, k);
                m.head.xRot = Mth.lerp(k, m.head.xRot, .45f); m.head.yRot += Mth.sin(t * 9) * .15f * k;
            }
            case "danse" -> {
                m.body.yRot = s6 * .3f * k;
                bras(m.rightArm, -1.6f + s6 * .9f, 0, .3f, k); bras(m.leftArm, -1.6f - s6 * .9f, 0, -.3f, k);
                m.rightLeg.xRot = Mth.lerp(k, m.rightLeg.xRot, s6 * .45f); m.leftLeg.xRot = Mth.lerp(k, m.leftLeg.xRot, -s6 * .45f);
                m.head.yRot += s6 * .35f * k; m.head.zRot = Mth.sin(t * 3) * .15f * k;
            }
            case "flex" -> {
                float p = Math.abs(Mth.sin(t * 5)) * .25f;
                bras(m.rightArm, -.2f, 0, 1.55f - p, k); bras(m.leftArm, -.2f, 0, -1.55f + p, k);
                m.head.xRot = Mth.lerp(k, m.head.xRot, -.15f);
            }
            case "dab" -> {
                bras(m.rightArm, -2.35f, 0, .9f, k); bras(m.leftArm, -1.75f, 1.05f, 0, k);
                m.head.xRot = Mth.lerp(k, m.head.xRot, .55f); m.head.yRot = Mth.lerp(k, m.head.yRot, .55f);
            }
            default -> { return false; }
        }
        return true;
    }

    private static void bras(ModelPart b, float x, float y, float z, float k) {
        b.xRot = Mth.lerp(k, b.xRot, x); b.yRot = Mth.lerp(k, b.yRot, y); b.zRot = Mth.lerp(k, b.zRot, z);
    }

    /** Meme pose que s'accroupir (corps penche, jambes reculees), dosee par k. */
    private static void accroupi(PlayerModel<?> m, float k) {
        m.body.xRot = .5f * k; m.body.y = 3.2f * k;
        m.rightArm.y = m.leftArm.y = 2 + 3.2f * k;
        m.rightLeg.z = m.leftLeg.z = 4 * k; m.rightLeg.y = m.leftLeg.y = 12 + .2f * k;
        m.head.y = 4.2f * k;
    }

    /** Joue son emote favorite (touche B). */
    static void favorite() {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        String id = ClientCosm.vue == null ? Profil.defaut(4) : ClientCosm.vue.choix(4);
        Metiers.CANAL.sendToServer(new Cosmetiques.Action(8, id));
    }

    private Emotes() {}
}
