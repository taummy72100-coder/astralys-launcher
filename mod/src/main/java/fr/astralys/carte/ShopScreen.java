package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** Boutique : categories, prix d'achat / de vente qui suivent l'offre et la demande, tendance sur l'heure. */
public class ShopScreen extends EcranAstralys {
    private static final int OR = 0xF0B429, COLS = 6, LIGNES = 4;
    private static final ItemStack[] ICONES = {new ItemStack(Items.DIAMOND), new ItemStack(Items.WHEAT), new ItemStack(Items.BONE),
            new ItemStack(Items.BRICKS), new ItemStack(Items.CLOCK), new ItemStack(Items.BREWING_STAND)};
    private int categorie, defile;
    private Economie.Ligne survol;

    public ShopScreen() { super("Boutique", OR); ClientEco.demander(0); }

    private List<Economie.Ligne> lignes() {
        List<Economie.Ligne> l = new ArrayList<>();
        if (ClientEco.marche != null) for (Economie.Ligne x : ClientEco.marche.lignes()) if (x.categorie() == categorie) l.add(x);
        return l;
    }

    private static ItemStack pile(String id) {
        var it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

    @Override
    void contenu(GuiGraphics g) {
        texte(g, "✦ " + Economie.format(ClientEco.solde) + " $", 596, 40, 1.4f, OR, 1, true);
        // categories
        for (int i = 0; i < Economie.CATEGORIES.length; i++) {
            float y = 76 + i * 34;
            boolean sel = i == categorie, s = dans(30, y, 152, y + 28);
            rect(g, 30, y, 152, y + 28, argb(sel ? 0x2E2A1E : s ? 0x2A2A31 : 0x222228, a));
            if (sel) rect(g, 30, y, 33, y + 28, argb(OR, a));
            objet(g, ICONES[i], 40, y + 6, 1);
            texte(g, Economie.CATEGORIES[i].toUpperCase(Locale.FRANCE), 62, y + 10, .9f, sel ? OR : 0xE8E8EE, -1, true);
        }
        texte(g, "Clic gauche : acheter 1", 30, 286, .7f, 0x9A9AA4, -1, false);
        texte(g, "Maj + clic : acheter 16", 30, 296, .7f, 0x9A9AA4, -1, false);
        texte(g, "Clic droit : vendre 1", 30, 310, .7f, 0x9A9AA4, -1, false);
        texte(g, "Maj + clic droit : tout vendre", 30, 320, .7f, 0x9A9AA4, -1, false);
        texte(g, "Les prix bougent avec les achats et les ventes.", 30, 336, .6f, 0x6E6E78, -1, false);
        // articles
        List<Economie.Ligne> l = lignes();
        survol = null;
        if (ClientEco.marche == null) { texte(g, "Chargement des cours…", 392, 190, 1, 0x8A8A92, 0, false); return; }
        defile = Math.max(0, Math.min(defile, (l.size() - 1) / COLS - LIGNES + 1));
        for (int i = defile * COLS; i < Math.min(l.size(), (defile + LIGNES) * COLS); i++) {
            int c = i % COLS, r = i / COLS - defile;
            float x = 166 + c * 76, y = 72 + r * 66;
            Economie.Ligne e = l.get(i);
            boolean s = dans(x, y, x + 72, y + 62);
            if (s) survol = e;
            rect(g, x, y, x + 72, y + 62, argb(s ? 0x2E2E36 : 0x222228, a));
            if (s) { rect(g, x, y, x + 72, y + 1, argb(OR, a)); rect(g, x, y + 61, x + 72, y + 62, argb(OR, a)); }
            float bob = s ? (float) Math.sin(t * 5) * 1.2f : 0;
            objet(g, pile(e.item()), x + 20, y + 6 + bob, 2);
            texte(g, "A " + Economie.format(e.achat()) + "$", x + 4, y + 42, .7f, 0x22C55E, -1, true);
            texte(g, "V " + Economie.format(e.vente()) + "$", x + 4, y + 51, .7f, 0xF0645A, -1, true);
            float td = e.tendance();
            if (Math.abs(td) >= .1f)
                texte(g, (td > 0 ? "▲" : "▼") + String.format(Locale.FRANCE, "%.1f%%", Math.abs(td)), x + 70, y + 4, .6f, td > 0 ? 0x22C55E : 0xF0645A, 1, true);
        }
        int pages = (l.size() + COLS - 1) / COLS;
        if (pages > LIGNES) {
            float h = 262f * LIGNES / pages, y = 72 + 262f * defile / pages;
            rect(g, 624, 72, 627, 334, argb(0x2A2A31, a));
            rect(g, 624, y, 627, y + h, argb(OR, a));
        }
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survol == null) return;
        ItemStack s = pile(survol.item());
        g.renderComponentTooltip(font, List.of(s.getHoverName().copy().withStyle(st -> st.withColor(OR).withBold(true)),
                Component.literal("Achat : " + Economie.format(survol.achat()) + " $").withStyle(st -> st.withColor(0x22C55E)),
                Component.literal("Vente : " + Economie.format(survol.vente()) + " $").withStyle(st -> st.withColor(0xF0645A)),
                Component.literal(String.format(Locale.FRANCE, "Tendance sur l'heure : %+.1f %%", survol.tendance())).withStyle(st -> st.withColor(0x9A9AA4))), mx, my);
    }

    @Override
    boolean clic(int b, boolean maj) {
        for (int i = 0; i < Economie.CATEGORIES.length; i++)
            if (dans(30, 76 + i * 34, 152, 104 + i * 34)) { categorie = i; defile = 0; son(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f); return true; }
        if (survol == null || (b != 0 && b != 1)) return false;
        boolean achat = b == 0;
        Metiers.CANAL.sendToServer(new Economie.Transaction(survol.item(), achat ? (maj ? 16 : 1) : (maj ? -1 : 1), achat));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) { defile -= (int) Math.signum(d); return true; }
}
