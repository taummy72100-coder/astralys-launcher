package fr.astralys.carte;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/** Sylvaria est le monde minage : il est remis a neuf tous les 14 jours (lundi 5 h), sauf le village du spawn. */
final class ResetSylvaria {
    private static final Logger LOG = LogUtils.getLogger();
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ResourceKey<Level> SYLVARIA = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("astralys", "sylvaria"));
    private static final String CLE = "astralys_reset";
    private static final int[] AVERTIR = {60, 30, 15, 10, 5, 2, 1};

    private static int cycle;
    private static boolean fait;
    private static int dejaAverti = -1;

    private ResetSylvaria() {}

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(ResetSylvaria::avantDemarrage);
        MinecraftForge.EVENT_BUS.addListener(ResetSylvaria::demarre);
        MinecraftForge.EVENT_BUS.addListener(ResetSylvaria::connexion);
        MinecraftForge.EVENT_BUS.addListener(ResetSylvaria::tick);
    }

    private static LocalDateTime maintenant() { return LocalDateTime.now(PARIS); }

    // Avant le chargement des mondes : aucun fichier de Sylvaria n'est ouvert.
    private static void avantDemarrage(ServerAboutToStartEvent e) {
        Path monde = e.getServer().getWorldPath(LevelResource.ROOT);
        Path etat = monde.resolve("astralys_reset.properties");
        Properties p = new Properties();
        int attendu = Calendrier.cycle(maintenant());
        try {
            if (Files.exists(etat)) try (Reader r = Files.newBufferedReader(etat)) { p.load(r); }
            // Premier lancement : on note le cycle en cours sans rien effacer.
            cycle = Integer.parseInt(p.getProperty("cycle", String.valueOf(attendu)));
            if (attendu > cycle) {
                Path dim = monde.resolve("dimensions/astralys/sylvaria");
                LOG.info("[Astralys] Reinitialisation de Sylvaria (cycle {} -> {})", cycle, attendu);
                if (Files.isDirectory(dim)) Calendrier.effacer(dim, s -> LOG.info("[Astralys] {}", s));
                cycle = attendu;
                fait = true;
            }
            p.setProperty("cycle", String.valueOf(cycle));
            try (Writer w = Files.newBufferedWriter(etat)) { p.store(w, "Reset de Sylvaria : numero du dernier cycle fait (ne pas modifier)"); }
        } catch (IOException | NumberFormatException ex) {
            // Sylvaria reste tel quel : mieux vaut un reset manque qu'un monde abime.
            LOG.error("[Astralys] Reinitialisation de Sylvaria impossible", ex);
        }
    }

    private static void demarre(ServerStartedEvent e) {
        if (!fait) return;
        // Sylvaria est en tete de la pre-generation (etapes 0 a 1023) : on la relance depuis le debut.
        MinecraftServer s = e.getServer();
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
                "execute if score #etat astralys_pregen matches 0 run scoreboard players set #p astralys_pregen 0");
    }

    private static void connexion(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer j)) return;
        CompoundTag d = j.getPersistentData();
        // Deconnecte dans Sylvaria avant le reset : il serait coince dans la roche regeneree.
        if (d.getInt(CLE) < cycle && j.level().dimension() == SYLVARIA) {
            ServerLevel sy = j.server.getLevel(SYLVARIA);
            if (sy != null) j.teleportTo(sy, -11.5, 65, -8.5, 0, 0);
            j.sendSystemMessage(Component.literal("✦ Sylvaria a été réinitialisé : tu as été ramené au spawn du village.").withStyle(st -> st.withColor(0x7CE07C)));
        }
        d.putInt(CLE, cycle);
        LocalDateTime prochain = Calendrier.DEPART.plusDays(14L * (cycle + 1)).atTime(Calendrier.HEURE);
        j.sendSystemMessage(Component.literal("⛏ Prochain reset de Sylvaria (monde minage) : "
                + prochain.format(DateTimeFormatter.ofPattern("EEEE d MMMM 'à' H'h'", Locale.FRENCH))
                + ". Construis ta base à Pyrélune ou Azuréa !").withStyle(st -> st.withColor(0xE8DFFF)));
    }

    // Avertissements avant le redemarrage du lundi 5 h (fait par le panel).
    private static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 200 != 0) return;
        LocalDateTime n = maintenant(), l = Calendrier.prochainLundi(n);
        long min = (java.time.Duration.between(n, l).getSeconds() + 59) / 60;
        for (int a : AVERTIR) {
            if (min != a || dejaAverti == a) continue;
            dejaAverti = a;
            String texte = Calendrier.estJourDeReset(l.toLocalDate())
                    ? "⚠ Sylvaria (monde minage) sera réinitialisé dans " + a + " min ! Ramène tes affaires à ta base."
                    : "⚠ Redémarrage du serveur dans " + a + " min.";
            e.getServer().getPlayerList().broadcastSystemMessage(Component.literal(texte).withStyle(st -> st.withColor(0xFFB347)), false);
        }
    }
}
