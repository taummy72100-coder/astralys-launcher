package fr.astralys.carte;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ecran de faction : membres, banque, points, chunks, defis de la semaine et classement. */
public class FactionScreen extends EcranAstralys {
    private static final int VIOLET = 0xB46CFF;
    private static final ItemStack[] DEFI = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.WHEAT),
            new ItemStack(Items.BREWING_STAND), new ItemStack(Items.EMERALD), new ItemStack(Items.EXPERIENCE_BOTTLE)};

    public FactionScreen() { super("Faction", VIOLET); ClientEco.demander(1); }

    private ItemStack icone(String titre) {
        for (int i = 0; i < Economie.DEFIS.size(); i++) if (Economie.DEFIS.get(i).titre().equals(titre)) return DEFI[i];
        return DEFI[0];
    }

    private void carte(GuiGraphics g, float x, float y, float w, float h) { rect(g, x, y, x + w, y + h, argb(0x222228, a)); }

    @Override
    void contenu(GuiGraphics g) {
        texte(g, "✦ " + Economie.format(ClientEco.solde) + " $", 596, 40, 1.4f, 0xF0B429, 1, true);
        Economie.FactionVue f = ClientEco.faction;
        if (f == null) { texte(g, "Chargement…", 320, 180, 1, 0x8A8A92, 0, false); return; }
        if (f.nom().isEmpty()) {
            carte(g, 38, 80, 360, 250);
            texte(g, "TU N'AS PAS ENCORE DE FACTION", 218, 100, 1.2f, 0xFFFFFF, 0, true);
            String[] aide = {"/f create <nom>   fonder ta faction", "/f join <nom>   rejoindre (sur invitation)", "/f invite <joueur>   inviter un joueur",
                    "/f claim   acheter le chunk où tu es", "/f deposit <montant>   remplir la banque", "/f top   classement des factions"};
            for (int i = 0; i < aide.length; i++) texte(g, aide[i], 58, 132 + i * 18, .9f, i % 2 == 0 ? 0xD8C8FF : 0xE8E8EE, -1, false);
            texte(g, "Les chunks s'achètent à Pyrélune et Azuréa (mondes des bases).", 58, 250, .75f, 0x9A9AA4, -1, false);
            texte(g, "Défis chaque semaine : banque + points pour le classement.", 58, 264, .75f, 0x9A9AA4, -1, false);
        } else {
            // en-tete
            texte(g, f.nom().toUpperCase(Locale.FRANCE), 38, 72, 2.2f, 0xFFFFFF, -1, true);
            texte(g, "Chef : " + f.chef(), 38, 94, .9f, 0xD8C8FF, -1, false);
            String[][] stats = {{"BANQUE", Economie.format(f.banque()) + " $"}, {"POINTS", Economie.format(f.points())},
                    {"CHUNKS", f.claims() + " / " + f.max()}, {"PROCHAIN CHUNK", Economie.format(f.prixClaim()) + " $"}};
            for (int i = 0; i < 4; i++) {
                float x = 38 + i * 92;
                carte(g, x, 110, 86, 40);
                texte(g, stats[i][0], x + 43, 116, .65f, 0x9A9AA4, 0, true);
                texte(g, stats[i][1], x + 43, 130, 1.1f, i == 0 ? 0xF0B429 : i == 1 ? VIOLET : 0xFFFFFF, 0, true);
            }
            // membres
            carte(g, 418, 72, 204, 128);
            texte(g, "MEMBRES (" + f.membres().size() + ")", 428, 80, .8f, 0xFFFFFF, -1, true);
            for (int i = 0; i < Math.min(10, f.membres().size()); i++) {
                float x = 428 + (i / 5) * 98, y = 96 + (i % 5) * 20;
                boolean on = f.enLigne().get(i);
                rect(g, x, y + 2, x + 5, y + 7, argb(on ? 0x22C55E : 0x55555E, a));
                texte(g, f.membres().get(i), x + 9, y, .8f, on ? 0xFFFFFF : 0x8A8A92, -1, false);
            }
            // defis
            texte(g, "D É F I S   D E   L A   S E M A I N E", 38, 166, 1, 0xFFFFFF, -1, true);
            for (int i = 0; i < f.defis().size(); i++) {
                Economie.DefiVue d = f.defis().get(i);
                float x = 38 + i * 126, y = 182;
                carte(g, x, y, 120, 100);
                boolean fini = d.avance() >= d.nombre();
                MetiersScreen.hexa(g, x + 6, y + 8, 30, 0x0E0E10, a);
                MetiersScreen.hexa(g, x + 9, y + 11, 24, fini ? 0x22C55E : VIOLET, a);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, a);
                objet(g, icone(d.titre()), x + 13, y + 15, .75f);
                texte(g, d.titre().toUpperCase(Locale.FRANCE), x + 40, y + 12, .62f, 0xFFFFFF, -1, true);
                texte(g, d.desc(), x + 40, y + 24, .58f, 0xC8C8D0, -1, false);
                float p = Math.min(1, d.avance() / (float) Math.max(1, d.nombre()));
                rect(g, x + 8, y + 52, x + 112, y + 56, argb(0x3A3A42, a));
                rect(g, x + 8, y + 52, x + 8 + 104 * p, y + 56, argb(fini ? 0x22C55E : VIOLET, a));
                texte(g, fini ? "✔ RÉUSSI" : Economie.format(d.avance()) + " / " + Economie.format(d.nombre()), x + 60, y + 62, .65f, fini ? 0x22C55E : 0xFFFFFF, 0, true);
                texte(g, "+" + Economie.format(d.argent()) + " $", x + 60, y + 76, .7f, 0xF0B429, 0, true);
                texte(g, "+" + d.points() + " points", x + 60, y + 87, .65f, VIOLET, 0, true);
            }
        }
        // classement
        carte(g, 418, 210, 204, 120);
        texte(g, "★ CLASSEMENT", 428, 218, .9f, VIOLET, -1, true);
        for (int i = 0; i < f.top().size(); i++) {
            String[] e = f.top().get(i).split("\\|");
            float y = 236 + i * 18;
            int c = i == 0 ? 0xF0B429 : i == 1 ? 0xC8CDD4 : i == 2 ? 0xCD7F32 : 0xE8E8EE;
            texte(g, (i + 1) + ".", 430, y, .85f, c, -1, true);
            texte(g, e[0], 446, y, .85f, c, -1, i < 3);
            texte(g, e.length > 1 ? Economie.format(Long.parseLong(e[1])) + " pts" : "", 612, y, .75f, 0x9A9AA4, 1, false);
        }
        if (f.top().isEmpty()) texte(g, "Aucune faction pour l'instant", 520, 260, .75f, 0x8A8A92, 0, false);
    }
}
