package fr.astralys.carte;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil du joueur : banniere (fond, motif, embleme), grade affiche dans le chat, badges de quetes, emotes.
 * Tout ce qui se paie se paie en rubis bleus et reste purement visuel (regles Minecraft : aucun avantage en jeu).
 */
final class Profil {
    /** type : 0 fond, 1 motif, 2 embleme, 3 grade, 4 emote. prix 0 = offert a tous. c1, c2 : couleurs ; objet : icone (embleme). */
    record Option(int type, String id, String nom, int prix, int c1, int c2, String objet) {
        String cle() { return TYPES[type] + ":" + id; }
    }

    static final String[] TYPES = {"fond", "motif", "embleme", "grade", "emote"};

    private static Option o(int t, String id, String nom, int prix, int c1, int c2, String objet) { return new Option(t, id, nom, prix, c1, c2, objet); }

    static final List<Option> OPTIONS = List.of(
            // fonds de banniere (degrade)
            o(0, "nuit", "Nuit indigo", 0, 0x2C1F55, 0x120C26, null), o(0, "crepuscule", "Crépuscule", 0, 0x5A2A6E, 0x1E1440, null),
            o(0, "foret", "Forêt profonde", 0, 0x1E4A32, 0x0C1E16, null),
            o(0, "aurore", "Aurore boréale", 500, 0x1BB59A, 0x3A2A8C, null), o(0, "braise", "Braise", 500, 0xC8461E, 0x3A0C10, null),
            o(0, "abysse", "Abysse", 600, 0x123A7A, 0x04081E, null), o(0, "nebuleuse", "Nébuleuse", 800, 0xB43CA0, 0x2A1460, null),
            o(0, "or_royal", "Or royal", 1000, 0xE2B65C, 0x6A4210, null), o(0, "givre", "Givre", 600, 0x9ADCF8, 0x2A5A8C, null),
            o(0, "sakura", "Sakura", 800, 0xF8A8C8, 0x7A3A6A, null),
            // motifs
            o(1, "aucun", "Aucun", 0, 0, 0, null), o(1, "etoiles", "Étoiles", 400, 0xFFFFFF, 0, null), o(1, "vagues", "Vagues", 400, 0xFFFFFF, 0, null),
            o(1, "runes", "Runes", 600, 0xC9A0FF, 0, null), o(1, "flammes", "Flammes", 600, 0xFFB040, 0, null),
            o(1, "cristaux", "Cristaux", 800, 0x9AE8FF, 0, null), o(1, "constellation", "Constellation", 1000, 0xFFECAA, 0, null),
            // emblemes
            o(2, "epee", "Épée", 0, 0, 0, "minecraft:iron_sword"), o(2, "pioche", "Pioche", 0, 0, 0, "minecraft:iron_pickaxe"),
            o(2, "ble", "Blé", 0, 0, 0, "minecraft:wheat"), o(2, "arc", "Arc", 0, 0, 0, "minecraft:bow"),
            o(2, "couronne", "Couronne", 600, 0, 0, "minecraft:golden_helmet"), o(2, "trident", "Trident", 800, 0, 0, "minecraft:trident"),
            o(2, "totem", "Totem", 800, 0, 0, "minecraft:totem_of_undying"), o(2, "coeur_mer", "Cœur de la mer", 800, 0, 0, "minecraft:heart_of_the_sea"),
            o(2, "etoile", "Étoile du Nether", 1000, 0, 0, "minecraft:nether_star"), o(2, "cristal_end", "Cristal de l'End", 1000, 0, 0, "minecraft:end_crystal"),
            o(2, "balise", "Balise", 1200, 0, 0, "minecraft:beacon"), o(2, "oeuf_dragon", "Œuf de dragon", 1500, 0, 0, "minecraft:dragon_egg"),
            // grades : prefixe dans le chat et la liste des joueurs
            o(3, "aucun", "Voyageur", 0, 0xB8B8C4, 0, null), o(3, "etoile", "Étoilé", 800, 0x7FE0FF, 0, null),
            o(3, "astral", "Astral", 1600, 0xB46CFF, 0, null), o(3, "celeste", "Céleste", 3000, 0xF0B429, 0, null),
            o(3, "legende", "Légende", 5000, 0xFF4FD8, 0, null),
            // emotes
            o(4, "salut", "Salut", 0, 0x7FE0FF, 0, null), o(4, "applaudir", "Applaudir", 300, 0xF0B429, 0, null),
            o(4, "reverence", "Révérence", 300, 0xC9A0FF, 0, null), o(4, "reflexion", "Réflexion", 300, 0x9AE8FF, 0, null),
            o(4, "victoire", "Victoire", 400, 0x22C55E, 0, null), o(4, "facepalm", "Facepalm", 400, 0xF0645A, 0, null),
            o(4, "danse", "Danse astrale", 500, 0xFF4FD8, 0, null), o(4, "flex", "Muscles", 500, 0xE8322B, 0, null),
            o(4, "dab", "Dab", 600, 0x3DD6C5, 0, null));

    static Option get(int type, String id) {
        for (Option x : OPTIONS) if (x.type() == type && x.id().equals(id)) return x;
        return null;
    }

    static Option get(String cle) {
        int i = cle.indexOf(':');
        if (i < 0) return null;
        for (int t = 0; t < TYPES.length; t++) if (TYPES[t].equals(cle.substring(0, i))) return get(t, cle.substring(i + 1));
        return null;
    }

    static List<Option> du(int type) { List<Option> l = new ArrayList<>(); for (Option x : OPTIONS) if (x.type() == type) l.add(x); return l; }

    /** Choix par defaut de chaque type (le premier, offert). */
    static String defaut(int type) { return du(type).get(0).id(); }

    // ------------------------------------------------------------------ badges : gagnes en jouant, jamais vendus
    /** genre : 0 quetes terminees, 1 niveau du joueur, 2 meilleur metier, 3 les 4 metiers au moins a. */
    record Badge(String id, String nom, String desc, int genre, int seuil, int couleur, String symbole) {}

    static final List<Badge> BADGES = List.of(
            new Badge("quete_1", "Premiers pas", "Terminer 1 quête", 0, 1, 0x9AE8FF, "✦"),
            new Badge("quete_10", "Aventurier", "Terminer 10 quêtes", 0, 10, 0x22C55E, "✧"),
            new Badge("quete_50", "Vétéran", "Terminer 50 quêtes", 0, 50, 0x4C9BFF, "❖"),
            new Badge("quete_150", "Héros d'Astralys", "Terminer 150 quêtes", 0, 150, 0xB46CFF, "✪"),
            new Badge("quete_500", "Légende vivante", "Terminer 500 quêtes", 0, 500, 0xF0B429, "♛"),
            new Badge("niveau_10", "Initié", "Atteindre le niveau 10", 1, 10, 0x7FE0FF, "Ⅹ"),
            new Badge("niveau_30", "Conquérant", "Atteindre le niveau 30", 1, 30, 0xB46CFF, "⚑"),
            new Badge("niveau_60", "Astral", "Atteindre le niveau 60", 1, 60, 0xFF4FD8, "☄"),
            new Badge("niveau_100", "Maître d'Astralys", "Atteindre le niveau 100", 1, 100, 0xF0B429, "☀"),
            new Badge("metier_50", "Expert", "Un métier au niveau 50", 2, 50, 0xE8322B, "⚒"),
            new Badge("metier_100", "Grand maître", "Un métier au niveau 100", 2, 100, 0xF0B429, "⚜"),
            new Badge("metiers_50", "Les quatre voies", "Les 4 métiers au niveau 50", 3, 50, 0x3DD6C5, "✤"));

    static boolean obtenu(Badge b, int quetes, int niveauJoueur, int[] metiers) {
        int max = 0, min = Integer.MAX_VALUE;
        for (int n : metiers) { max = Math.max(max, n); min = Math.min(min, n); }
        return switch (b.genre()) { case 0 -> quetes >= b.seuil(); case 1 -> niveauJoueur >= b.seuil(); case 2 -> max >= b.seuil(); default -> min >= b.seuil(); };
    }

    // ------------------------------------------------------------------ boutique de cosmetiques : offre du mois et selection du jour
    static final long REMISE_PACK = 70;   // le pack coute 70 % de ses pieces encore a acheter

    /** Offre du mois : un ensemble complet (chapeau ou casque, jambieres, chaussures) de meme style, change chaque mois. */
    static List<Cosmetiques.Cosm> offreDuMois(LocalDate d) {
        java.util.Random r = new java.util.Random(d.getYear() * 12L + d.getMonthValue() * 7919L);
        List<Cosmetiques.Cosm> l = new ArrayList<>();
        for (int cat : new int[] {r.nextBoolean() ? 0 : 1, 2, 3}) {
            List<Cosmetiques.Cosm> c = new ArrayList<>();
            for (Cosmetiques.Cosm x : CatalogueCosm.LISTE) if (x.categorie() == cat && x.rarete() >= 1) c.add(x);
            l.add(c.get(r.nextInt(c.size())));
        }
        return l;
    }

    /** Selection du jour : 6 cosmetiques tires au hasard (memes pour tout le monde), renouveles a minuit. */
    static List<Cosmetiques.Cosm> selectionDuJour(LocalDate d) {
        List<Cosmetiques.Cosm> l = new ArrayList<>(CatalogueCosm.LISTE);
        java.util.Collections.shuffle(l, new java.util.Random(d.toEpochDay() * 31 + 7));
        return l.subList(0, 6);
    }

    /** Coffres cosmetiques payes en rubis bleus : ne contiennent QUE des cosmetiques, chances affichees (commun, rare, epique, legendaire). */
    record CoffreCosm(String nom, int prix, int modele, int[] chances) {}

    static final List<CoffreCosm> COFFRES = List.of(
            new CoffreCosm("Coffre étoilé", 400, 0, new int[] {70, 24, 5, 1}),
            new CoffreCosm("Coffre astral", 900, 1, new int[] {35, 45, 16, 4}),
            new CoffreCosm("Coffre céleste", 2000, 3, new int[] {0, 40, 42, 18}));

    private Profil() {}
}
