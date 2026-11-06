package fr.astralys.carte;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Onglet cosmetiques de la boutique : apercu sur son personnage au survol, achat en eclats astraux, porter / retirer. */
public class CosmetiqueScreen extends EcranAstralys {
    private static final int ROSE = 0xFF4FD8, PRIX = 0xC9A0FF;
    private static final ItemStack[] ICONES = {new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS), new ItemStack(Items.LEATHER_BOOTS)};
    private String choisi;
    private long choisiA;
    private Cosmetiques.Cosm survol;

    public CosmetiqueScreen() {
        super("Cosmétiques", ROSE);
        retour = ShopScreen::new;
        if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Cosmetiques.Action(0, ""));
    }

    private static ItemStack icone(Cosmetiques.Cosm c) {
        ItemStack s = ICONES[c.partie()].copy();
        ((DyeableLeatherItem) s.getItem()).setColor(s, c.couleur());
        return s;
    }

    private static boolean possede(Cosmetiques.Cosm c) { return ClientCosm.vue != null && ClientCosm.vue.possedes().contains(c.id()); }

    private static boolean porte(Cosmetiques.Cosm c) { return ClientCosm.vue != null && c.id().equals(ClientCosm.vue.portes().get(c.partie())); }

    @Override
    void contenu(GuiGraphics g) {
        long eclats = ClientCosm.vue == null ? 0 : ClientCosm.vue.eclats();
        texte(g, "✧ " + Economie.format(eclats) + " éclats", 596, 40, 1.3f, PRIX, 1, true);
        texte(g, "Monnaie premium : s'obtient sur la boutique en ligne du serveur  •  purement visuel", 622, 62, .55f, 0x8A8A9A, 1, false);

        // apercu du personnage
        carte(g, 38, 72, 170, 268, ROSE);
        section(g, "Aperçu", 48, 80, ROSE);
        Arrays.fill(ClientCosm.apercu, null);
        if (survol != null) ClientCosm.apercu[survol.partie()] = survol.id();
        if (minecraft.player != null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, 123, 300, 82, 123 - vx, 170 - vy, minecraft.player);
        texte(g, "Survole un cosmétique pour l'essayer", 123, 318, .55f, 0x9A9AA4, 0, false);

        // les 4 parties, 3 cosmetiques chacune
        survol = null;
        if (choisi != null && System.currentTimeMillis() - choisiA > 4000) choisi = null;
        for (int p = 0; p < 4; p++) {
            float y = 72 + p * 67;
            texte(g, Cosmetiques.PARTIES[p].toUpperCase(java.util.Locale.FRANCE), 222, y + 25, .75f, 0xFFFFFF, -1, true);
            int i = 0;
            for (Cosmetiques.Cosm c : Cosmetiques.LISTE) {
                if (c.partie() != p) continue;
                float x = 280 + i++ * 114;
                boolean s = dans(x, y, x + 108, y + 60), pos = possede(c), por = porte(c), sel = c.id().equals(choisi);
                if (s) survol = c;
                carte(g, x, y, 108, 60, por ? 0x22C55E : s ? ROSE : 0);
                if (s || sel) { int bc = sel ? 0xF0B429 : ROSE; rect(g, x, y, x + 108, y + 1, argb(bc, a)); rect(g, x, y + 59, x + 108, y + 60, argb(bc, a)); }
                float bob = s ? (float) Math.sin(t * 5) * 1.2f : 0;
                objet(g, icone(c), x + 6, y + 8 + bob, 1.6f);
                texte(g, c.nom(), x + 36, y + 10, .62f, 0xFFFFFF, -1, true);
                String etat = por ? "PORTÉ ✓" : pos ? "PORTER" : sel ? "CONFIRMER ?" : c.prix() + " ✧";
                int ce = por ? 0x22C55E : pos ? 0x3DD6C5 : sel ? 0xF0B429 : PRIX;
                texte(g, etat, x + 36, y + 26, .75f, ce, -1, true);
                if (!pos) texte(g, eclats >= c.prix() ? "clic : acheter" : "éclats insuffisants", x + 36, y + 42, .5f, eclats >= c.prix() ? 0x9A9AA4 : 0xF0645A, -1, false);
                else texte(g, por ? "clic : retirer" : "clic : équiper", x + 36, y + 42, .5f, 0x9A9AA4, -1, false);
            }
        }
    }

    @Override
    void infobulle(GuiGraphics g, int mx, int my) {
        if (survol == null) return;
        g.renderComponentTooltip(font, List.of(Component.literal(survol.nom()).withStyle(st -> st.withColor(ROSE).withBold(true)),
                Component.literal("Cosmétique — " + Cosmetiques.PARTIES[survol.partie()]).withStyle(st -> st.withColor(0x9A9AA4)),
                Component.literal("Purement visuel : aucun avantage en jeu").withStyle(st -> st.withColor(0x6E6E78).withItalic(true))), mx, my);
    }

    @Override
    boolean clic(int b, boolean maj) {
        if (survol == null || b != 0) return false;
        Cosmetiques.Cosm c = survol;
        int action;
        if (porte(c)) action = 3;
        else if (possede(c)) action = 2;
        else {
            // deux clics pour acheter : jamais d'achat par erreur
            if (!c.id().equals(choisi)) { choisi = c.id(); choisiA = System.currentTimeMillis(); son(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f); return true; }
            action = 1; choisi = null;
        }
        Metiers.CANAL.sendToServer(new Cosmetiques.Action(action, c.id()));
        son(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
        return true;
    }

    @Override
    public void removed() { Arrays.fill(ClientCosm.apercu, null); super.removed(); }
}
