package fr.astralys.carte;

import net.minecraft.client.Minecraft;

/** Cote joueur : solde, cours de la boutique et faction recus du serveur. */
final class ClientEco {
    static long solde;
    static Economie.Marche marche;
    static Economie.FactionVue faction;

    static void solde(long s) { solde = s; }
    static void marche(Economie.Marche m) { marche = m; }
    static void faction(Economie.FactionVue f) { faction = f; }

    static void ouvrir(int ecran) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(switch (ecran) { case 0 -> new ShopScreen(); case 1 -> new FactionScreen(); default -> new MetiersScreen(); });
    }

    static void demander(int quoi) { Metiers.CANAL.sendToServer(new Economie.Demande(quoi)); }

    private ClientEco() {}
}
