package fr.astralys.carte;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calendrier et effacement des fichiers de Sylvaria (sans dependance a Minecraft, testable seul). */
final class Calendrier {
    /** Lundi de reference : cycle 0. Un reset tous les 14 jours, au redemarrage de 5 h. */
    static final LocalDate DEPART = LocalDate.of(2026, 11, 2);
    static final LocalTime HEURE = LocalTime.of(5, 0);
    /** Marge : un redemarrage a 4 h 59 compte deja pour le reset de 5 h. */
    static final Duration MARGE = Duration.ofMinutes(30);
    /** Village du spawn : tous les chunks a moins de GARDE blocs de (CX, CZ) sont gardes. */
    static final int CX = -12, CZ = 3, GARDE = 215;
    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    private Calendrier() {}

    /** Numero du dernier cycle commence a l'heure locale donnee. */
    static int cycle(LocalDateTime maintenant) {
        long jours = ChronoUnit.DAYS.between(DEPART, maintenant.plus(MARGE).minusHours(HEURE.getHour()).toLocalDate());
        return (int) Math.floorDiv(jours, 14);
    }

    /** Prochain lundi 5 h strictement apres maintenant (redemarrage hebdomadaire du panel). */
    static LocalDateTime prochainLundi(LocalDateTime maintenant) {
        LocalDateTime l = maintenant.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(HEURE);
        return l.isAfter(maintenant) ? l : l.plusWeeks(1);
    }

    static boolean estJourDeReset(LocalDate jour) {
        return Math.floorMod(ChronoUnit.DAYS.between(DEPART, jour), 14) == 0;
    }

    /** Le carre de blocs [x0,x1]x[z0,z1] touche-t-il le cercle garde ? */
    static boolean garde(int x0, int x1, int z0, int z1) {
        long dx = Math.max(0, Math.max(x0 - CX, CX - x1)), dz = Math.max(0, Math.max(z0 - CZ, CZ - z1));
        return dx * dx + dz * dz <= (long) GARDE * GARDE;
    }

    /**
     * Efface Sylvaria sauf le village : supprime les fichiers de region qui ne le touchent pas,
     * et dans les autres retire du sommaire les chunks hors du cercle (Minecraft les regenere).
     */
    static void effacer(Path dimension, Consumer<String> journal) throws IOException {
        for (String dossier : List.of("region", "entities", "poi")) {
            Path d = dimension.resolve(dossier);
            if (!Files.isDirectory(d)) continue;
            List<Path> fichiers;
            try (var l = Files.list(d)) { fichiers = l.toList(); }
            int supprimes = 0, chunks = 0;
            for (Path f : fichiers) {
                Matcher m = REGION.matcher(f.getFileName().toString());
                if (!m.matches()) continue;
                int rx = Integer.parseInt(m.group(1)), rz = Integer.parseInt(m.group(2));
                if (!garde(rx * 512, rx * 512 + 511, rz * 512, rz * 512 + 511)) { Files.delete(f); supprimes++; }
                else chunks += vider(f, rx, rz);
            }
            journal.accept(dossier + " : " + supprimes + " fichiers supprimes, " + chunks + " chunks retires des fichiers du village");
        }
    }

    /** Met a zero l'entree du sommaire (position + date) des chunks hors du cercle. */
    static int vider(Path f, int rx, int rz) throws IOException {
        int n = 0;
        try (RandomAccessFile raf = new RandomAccessFile(f.toFile(), "rw")) {
            if (raf.length() < 8192) return 0;
            for (int i = 0; i < 1024; i++) {
                int cx = rx * 32 + (i & 31), cz = rz * 32 + (i >> 5);
                if (garde(cx * 16, cx * 16 + 15, cz * 16, cz * 16 + 15)) continue;
                raf.seek(i * 4L);
                if (raf.readInt() == 0) continue;
                raf.seek(i * 4L); raf.writeInt(0);
                raf.seek(4096 + i * 4L); raf.writeInt(0);
                n++;
            }
        }
        return n;
    }
}
