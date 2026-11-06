package fr.astralys.carte;

import net.minecraft.client.Minecraft;

/** Cote joueur : solde, cours de la boutique et faction recus du serveur. */
final class ClientEco {
    static long solde;
    static Economie.Marche marche;
    static Economie.FactionVue faction;
    static Economie.Classement classement;
    static HotelVentes.Vue hdv;

    static void solde(long s) { solde = s; }
    static void marche(Economie.Marche m) { marche = m; }
    static void faction(Economie.FactionVue f) { faction = f; }
    static void classement(Economie.Classement c) { classement = c; }
    static void hdv(HotelVentes.Vue v) { hdv = v; }

    static void ouvrir(int ecran) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(switch (ecran) { case 0 -> new ShopScreen(); case 1 -> new FactionScreen(); case 3 -> new ClassementScreen(); case 4 -> new HdvScreen(); case 5 -> new CarteScreen(); default -> new MetiersScreen(); });
    }

    static void roulette(Coffres.Roulette r) { Minecraft.getInstance().setScreen(new CoffreScreen(r)); }

    static void demander(int quoi) { if (Minecraft.getInstance().getConnection() != null) Metiers.CANAL.sendToServer(new Economie.Demande(quoi)); }

    private ClientEco() {}
}
