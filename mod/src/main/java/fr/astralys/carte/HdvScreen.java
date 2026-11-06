package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Hotel des ventes : offres des joueurs, ses propres ventes, et mise en vente de l'objet en main (frais + taxe). */
public class HdvScreen extends EcranAstralys {
    private static final int OR = 0xF0B429, COLS = 8, LIGNES = 3;
    private boolean miennes;
    private int defile;
    private String prix = "";
    private UUID choisie;
    private long choisieA;
    private HotelVentes.OffreVue survol;

    public HdvScreen() { super("Hôtel des ventes", 0x3DD6C5); ClientEco.demander(3); }

    private List<HotelVentes.OffreVue> offres() {
        List<HotelVentes.OffreVue> l = new ArrayList<>();
        if (ClientEco.hdv != null) for (var o : ClientEco.hdv.offres()) if (o.mienne() == miennes) l.add(o);
        return l;
    }

    private long prixSaisi() { try { return prix.isEmpty() ? 0 : Long.parseLong(prix); } catch (NumberFormatException e) { return 0; } }

    private void onglet(GuiGraphics g, float x, float w, String t, boolean sel) {
        boolean s = dans(x, 66, x + w, 88);
        rect(g, x - 1, 65, x + w + 1, 89, argb(0x0A0714, a));
        g.fillGradient(Math.round(x), 66, Math.round(x + w), 88, argb(sel ? 0x3D2E1E : s ? 0x3A2C66 : 0x2A1F4D, a), argb(sel ? 0x2A2014 : 0x211838, a));
        if (sel) rect(g, x, 87, x + w, 88, argb(OR, a));
        texte(g, t, x + w / 2, 73, .75f, sel ? OR : 0xE8E8EE, 0, true);
    }

    @Override
    void contenu(GuiGraphics g) {
        texte(g, "✦ " + Economie.format(ClientEco.solde) + " $", 596, 40, 1.4f, OR, 1, true);
        int nb = ClientEco.hdv == null ? 0 : ClientEco.hdv.miennes();
        onglet(g, 38, 150, "TOUTES LES OFFRES", !miennes);
        onglet(g, 194, 150, "MES VENTES (" + nb + "/" + HotelVentes.MAX_OFFRES + ")", miennes);
        texte(g, "Offres valables 72 h  •  objet rendu s'il n'est pas vendu", 622, 75, .6f, 0x9A9AA4, 1, false);

        // ---- offres
        List<HotelVentes.OffreVue> l = offres();
        survol = null;
        if (ClientEco.hdv == null) texte(g, "Chargement…", 320, 190, 1, 0x8A8A92, 0, false);
        else if (l.isEmpty()) texte(g, miennes ? "Tu n'as rien en vente : prends un objet en main et fixe un prix en bas." : "Aucune offre pour l'instant.", 320, 190, .8f, 0x9A9AA4, 0, false);
        defile = Math.max(0, Math.min(defile, (l.size() - 1) / COLS - LIGNES + 1));
        if (choisie != null && System.currentTimeMillis() - choisieA > 4000) choisie = null;
        for (int i = defile * COLS; i < Math.min(l.size(), (defile + LIGNES) * COLS); i++) {
            var o = l.get(i);
            int c = i % COLS, r = i / COLS - defile;
            float x = 38 + c * 73, y = 96 + r * 68;
            boolean s = dans(x, y, x + 69, y + 64), sel = o.id().equals(choisie);
            if (s) survol = o;
            carte(g, x, y, 69, 64, 0);
            if (s || sel) { int bc = sel ? OR : 0x3DD6C5; rect(g, x, y, x + 69, y + 1, argb(bc, a)); rect(g, x, y + 63, x + 69, y + 64, argb(bc, a)); }
            objet(g, o.objet(), x + 19, y + 5, 2);
            if (o.objet().getCount() > 1) texte(g, "×" + o.objet().getCount(), x + 64, y + 26, .7f, 0xFFFFFF, 1, true);
            texte(g, Economie.format(o.prix()) + " $", x + 35, y + 40, .75f, OR, 0, true);
            texte(g, sel ? (miennes ? "RETIRER ?" : "ACHETER ?") : o.vendeur(), x + 35, y + 51, .6f, sel ? 0x22C55E : 0x9A9AA4, 0, sel);
        }

        // ---- mise en vente de l'objet en main
        carte(g, 38, 304, 584, 44, 0x3DD6C5);
        var j = Minecraft.getInstance().player;
        ItemStack main = j == null ? ItemStack.EMPTY : j.getMainHandItem();
        texte(g, "VENDRE L'OBJET EN MAIN", 48, 310, .75f, 0xFFFFFF, -1, true);
        objet(g, main, 50, 322, 1);
        texte(g, main.isEmpty() ? "Prends un objet en main" : main.getCount() + "× " + main.getHoverName().getString(), 70, 327, .65f, main.isEmpty() ? 0x8A8A92 : 0xE8E8EE, -1, false);
        // champ du prix
        rect(g, 230, 316, 330, 340, argb(0x0A0714, a));
        rect(g, 231, 317, 329, 339, argb(0x150F2C, a));
        boolean curseur = (System.currentTimeMillis() / 500) % 2 == 0;
        texte(g, (prix.isEmpty() ? "" : Economie.format(prixSaisi())) + (curseur ? "_" : ""), 236, 324, .9f, OR, -1, true);
        if (prix.isEmpty()) texte(g, "prix…", 244, 324, .8f, 0x6E6E78, -1, false);
        texte(g, "$", 322, 324, .9f, OR, 0, true);
        long p = prixSaisi();
        texte(g, "Frais : " + Economie.format(p > 0 ? HotelVentes.frais(p) : 0) + " $  •  Taxe à la vente : 5 %", 340, 316, .62f, 0xC8C8D0, -1, false);
        texte(g, "Tu recevras : " + Economie.format(Math.max(0, p - HotelVentes.taxe(p))) + " $", 340, 328, .62f, 0x22C55E, -1, true);
        boolean ok = p > 0 && !main.isEmpty(), sb = dans(500, 314, 612, 340);
        rect(g, 499, 313, 613, 341, argb(0x0A0714, a));
        g.fillGradient(500, 314, 612, 340, argb(ok ? (sb ? 0x4FE8D5 : 0x3DD6C5) : 0x3B2F5E, a), argb(ok ? 0x1F8F84 : 0x2A1F4D, a));
        texte(g, "METTRE EN VENTE", 556, 323, .75f, ok ? 0x0A1F1D : 0x8A8A92, 0, true);
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survol == null) return;
        List<Component> l = new ArrayList<>(getTooltipFromItem(minecraft, survol.objet()));
        l.add(Component.literal("Prix : " + Economie.format(survol.prix()) + " $").withStyle(st -> st.withColor(OR).withBold(true)));
        l.add(Component.literal("Vendeur : " + survol.vendeur() + "  •  encore " + (survol.minutes() / 60) + " h " + (survol.minutes() % 60) + " min").withStyle(st -> st.withColor(0x9A9AA4)));
        l.add(Component.literal(survol.mienne() ? "Clique deux fois pour retirer l'offre" : "Clique deux fois pour acheter").withStyle(st -> st.withColor(0x3DD6C5)));
        g.renderComponentTooltip(font, l, mx, my);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (dans(38, 66, 188, 88)) { miennes = false; defile = 0; son(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f); return true; }
        if (dans(194, 66, 344, 88)) { miennes = true; defile = 0; son(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f); return true; }
        if (dans(500, 314, 612, 340) && prixSaisi() > 0) {
            Metiers.CANAL.sendToServer(new HotelVentes.Action(3, new UUID(0, 0), prixSaisi()));
            prix = ""; miennes = true;
            return true;
        }
        if (survol != null && b == 0) {
            // deux clics pour confirmer : on n'achete (ni ne retire) jamais par erreur
            if (survol.id().equals(choisie)) { Metiers.CANAL.sendToServer(new HotelVentes.Action(survol.mienne() ? 2 : 1, survol.id(), 0)); choisie = null; }
            else { choisie = survol.id(); choisieA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (Character.isDigit(c) && prix.length() < 10) { prix += c; return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int k, int s, int mods) {
        if (k == GLFW.GLFW_KEY_BACKSPACE && !prix.isEmpty()) { prix = prix.substring(0, prix.length() - 1); return true; }
        return super.keyPressed(k, s, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) { defile -= (int) Math.signum(d); return true; }
}
