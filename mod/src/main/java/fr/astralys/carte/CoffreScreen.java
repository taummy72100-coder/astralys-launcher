package fr.astralys.carte;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/** Ouverture d'un coffre : bande d'objets qui defile et ralentit, puis le lot gagne se revele dans la couleur de sa rarete. */
public class CoffreScreen extends EcranAstralys {
    private static final float DUREE = 5.5f, PAS = 66, CX = 320, Y = 132;
    private final Coffres.Roulette r;
    private final float decalage;
    private float avance;   // secondes gagnees quand on clique pour accelerer
    private int dernier = -1;
    private boolean revele;

    public CoffreScreen(Coffres.Roulette r) {
        super("Coffre " + Coffres.NOMS[r.rarete()], Coffres.COULEURS[r.rarete()]);
        this.r = r;
        decalage = (float) (Math.random() * 36 - 18);   // ne s'arrete jamais pile au milieu de la carte
    }

    private float progres() { return Math.min(1, (t + avance - .4f) / DUREE); }

    @Override
    void contenu(GuiGraphics g) {
        int cr = Coffres.COULEURS[r.rarete()];
        float p = Math.max(0, progres()), e = 1 - (float) Math.pow(1 - p, 3.2);
        float pos = e * (r.gagnant() * PAS + decalage);
        texte(g, "Le coffre s'ouvre…", 320, 88, 1, 0xC8C8D0, 0, false);

        // bande des objets
        rect(g, 36, Y - 8, 604, Y + 88, argb(0x0A0714, .6f * a));
        for (int i = 0; i < r.objets().size(); i++) {
            float x = CX + i * PAS - pos - 30;
            if (x < 30 || x > 550) continue;
            float bord = Math.min(1, Math.min(x - 30, 550 - x) / 50);
            int rc = Coffres.COULEURS[r.raretes().get(i)];
            boolean gagnant = revele && i == r.gagnant();
            carte(g, x, Y, 60, 80, 0);
            g.fillGradient(Math.round(x), Math.round(Y + 40), Math.round(x + 60), Math.round(Y + 80), argb(rc, 0), argb(rc, .45f * bord * a));
            rect(g, x, Y + 77, x + 60, Y + 80, argb(rc, bord * a));
            if (gagnant) { rect(g, x - 2, Y - 2, x + 62, Y, argb(rc, a)); rect(g, x - 2, Y + 80, x + 62, Y + 82, argb(rc, a)); }
            objet(g, r.objets().get(i), x + 10, Y + 14, 2.5f);
            int n = r.objets().get(i).getCount();
            if (n > 1) texte(g, "×" + n, x + 56, Y + 60, .8f, 0xFFFFFF, 1, true);
        }
        // aiguille doree au centre
        rect(g, CX - 1, Y - 14, CX + 1, Y + 94, argb(0xF0D28A, a));
        texte(g, "▼", CX, Y - 22, 1.2f, 0xF0D28A, 0, true);
        texte(g, "▲", CX, Y + 92, 1.2f, 0xF0D28A, 0, true);

        // tic a chaque carte qui passe sous l'aiguille
        int sous = Math.round((pos + 0) / PAS);
        if (sous != dernier && p < 1) { dernier = sous; son(SoundEvents.NOTE_BLOCK_HAT.value(), 1.2f + .8f * e); }

        if (p >= 1) {
            if (!revele) { revele = true; son(r.raretes().get(r.gagnant()) >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1); }
            ItemStack gain = r.objets().get(r.gagnant());
            int gc = Coffres.COULEURS[r.raretes().get(r.gagnant())];
            float puls = .5f + .5f * (float) Math.sin(t * 4);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            halo(g, 320, 272, 70 + 8 * puls, gc, .45f * a);
            halo(g, 320, 272, 36, 0xFFFFFF, .25f * a);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            objet(g, gain, 304, 256, 2);
            texte(g, Coffres.NOMS[r.raretes().get(r.gagnant())].toUpperCase(java.util.Locale.FRANCE), 320, 234, .8f, gc, 0, true);
            texte(g, (gain.getCount() > 1 ? gain.getCount() + "× " : "") + gain.getHoverName().getString(), 320, 296, 1.2f, 0xFFFFFF, 0, true);
            texte(g, "Ajouté à ton inventaire  •  clique pour fermer", 320, 316, .7f, 0x9A9AA4, 0, false);
        } else texte(g, "Clique pour accélérer", 320, 316, .7f, 0x6E6E78, 0, false);
        texte(g, "✦ " + Coffres.NOMS[r.rarete()].toUpperCase(java.util.Locale.FRANCE) + " ✦", 600, 40, 1, cr, 1, true);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (progres() >= 1) { onClose(); return true; }
        avance += 1.5f;
        return true;
    }
}
