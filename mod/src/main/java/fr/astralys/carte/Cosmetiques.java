package fr.astralys.carte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;

/**
 * Cosmetiques (tete, buste, jambes, pieds) payes en eclats astraux, la monnaie premium.
 * Regles Minecraft : purement visuels, pas de cape ; les eclats ne s'echangent pas entre joueurs et ne se revendent pas.
 * Ils sont credites par la boutique en ligne avec la commande console « eclats donner <joueur> <n> ».
 */
final class Cosmetiques {
    static final String[] PARTIES = {"Tête", "Buste", "Jambes", "Pieds"};

    /** couleur : teinte de l'icone dans la boutique. */
    record Cosm(String id, int partie, String nom, int prix, int couleur) {}

    static final List<Cosm> LISTE = List.of(
            new Cosm("couronne_astrale", 0, "Couronne astrale", 300, 0xF0B429), new Cosm("chapeau_archimage", 0, "Chapeau d'archimage", 250, 0x3C2C6E),
            new Cosm("cornes_nebuleuse", 0, "Cornes de nébuleuse", 350, 0xB46CFF),
            new Cosm("robe_etoilee", 1, "Robe étoilée", 400, 0x3C2C6E), new Cosm("plastron_runique", 1, "Plastron runique", 450, 0x3C78BE),
            new Cosm("manteau_aurore", 1, "Manteau d'aurore", 400, 0x50E6B4),
            new Cosm("pantalon_nebuleuse", 2, "Pantalon de nébuleuse", 250, 0x961E78), new Cosm("jambieres_runiques", 2, "Jambières runiques", 300, 0x3C78BE),
            new Cosm("pans_astraux", 2, "Pans astraux", 300, 0x9660E6),
            new Cosm("bottes_comete", 3, "Bottes comète", 250, 0x8CD2FA), new Cosm("sandales_lune", 3, "Sandales de lune", 200, 0xE6E6F0),
            new Cosm("bottes_flamme", 3, "Bottes de flamme astrale", 300, 0xF06E1E));

    static Cosm get(String id) { return LISTE.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null); }

    // ------------------------------------------------------------------ donnees
    static final class Donnees extends SavedData {
        final Map<UUID, Long> eclats = new HashMap<>();
        final Map<UUID, Set<String>> possedes = new HashMap<>();
        final Map<UUID, String[]> portes = new HashMap<>();

        static Donnees lire(CompoundTag t) {
            Donnees d = new Donnees();
            CompoundTag e = t.getCompound("eclats"), p = t.getCompound("possedes"), o = t.getCompound("portes");
            for (String k : e.getAllKeys()) d.eclats.put(UUID.fromString(k), e.getLong(k));
            for (String k : p.getAllKeys()) { Set<String> s = new HashSet<>(); for (Tag x : p.getList(k, Tag.TAG_STRING)) s.add(x.getAsString()); d.possedes.put(UUID.fromString(k), s); }
            for (String k : o.getAllKeys()) { String[] a = new String[4]; ListTag l = o.getList(k, Tag.TAG_STRING); for (int i = 0; i < 4 && i < l.size(); i++) a[i] = l.getString(i); d.portes.put(UUID.fromString(k), a); }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            CompoundTag e = new CompoundTag(), p = new CompoundTag(), o = new CompoundTag();
            eclats.forEach((u, n) -> e.putLong(u.toString(), n));
            possedes.forEach((u, s) -> { ListTag l = new ListTag(); s.forEach(x -> l.add(StringTag.valueOf(x))); p.put(u.toString(), l); });
            portes.forEach((u, a) -> { ListTag l = new ListTag(); for (String x : a) l.add(StringTag.valueOf(x == null ? "" : x)); o.put(u.toString(), l); });
            t.put("eclats", e); t.put("possedes", p); t.put("portes", o);
            return t;
        }

        String[] portes(UUID u) { return portes.computeIfAbsent(u, k -> new String[4]); }
    }

    private static Donnees d(MinecraftServer s) { return s.overworld().getDataStorage().computeIfAbsent(Donnees::lire, Donnees::new, "astralys_cosmetiques"); }

    // ------------------------------------------------------------------ paquets
    record Vue(long eclats, List<String> possedes, List<String> portes) {
        void ecrire(FriendlyByteBuf b) { b.writeVarLong(eclats); b.writeCollection(possedes, FriendlyByteBuf::writeUtf); b.writeCollection(portes, FriendlyByteBuf::writeUtf); }
        static Vue lire(FriendlyByteBuf b) { return new Vue(b.readVarLong(), b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readUtf)); }
    }

    /** Ce que porte chaque joueur connecte (pour que tout le monde voie les cosmetiques des autres). */
    record Portes(Map<UUID, List<String>> joueurs) {
        void ecrire(FriendlyByteBuf b) { b.writeMap(joueurs, FriendlyByteBuf::writeUUID, (bb, l) -> bb.writeCollection(l, FriendlyByteBuf::writeUtf)); }
        static Portes lire(FriendlyByteBuf b) { return new Portes(b.readMap(FriendlyByteBuf::readUUID, bb -> bb.readList(FriendlyByteBuf::readUtf))); }
    }

    /** 0 demander la vue, 1 acheter, 2 porter, 3 retirer. */
    record Action(int action, String id) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(action); b.writeUtf(id); }
        static Action lire(FriendlyByteBuf b) { return new Action(b.readVarInt(), b.readUtf(64)); }
    }

    static void init() {
        Metiers.CANAL.messageBuilder(Vue.class, 11, NetworkDirection.PLAY_TO_CLIENT).encoder(Vue::ecrire).decoder(Vue::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCosm.vue = m)).add();
        Metiers.CANAL.messageBuilder(Portes.class, 12, NetworkDirection.PLAY_TO_CLIENT).encoder(Portes::ecrire).decoder(Portes::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCosm.portes = m.joueurs())).add();
        Metiers.CANAL.messageBuilder(Action.class, 13, NetworkDirection.PLAY_TO_SERVER).encoder(Action::ecrire).decoder(Action::lire)
                .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p != null) action(p, m); }).add();
        MinecraftForge.EVENT_BUS.addListener(Cosmetiques::commandes);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { envoyerVue(p); diffuser(p.server); } });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) diffuser(p.server); });
    }

    static void envoyerVue(ServerPlayer p) {
        Donnees d = d(p.server);
        List<String> portes = new ArrayList<>();
        for (String x : d.portes(p.getUUID())) portes.add(x == null ? "" : x);
        Economie.envoyer(p, new Vue(d.eclats.getOrDefault(p.getUUID(), 0L), List.copyOf(d.possedes.getOrDefault(p.getUUID(), Set.of())), portes));
    }

    private static void diffuser(MinecraftServer s) {
        Donnees d = d(s);
        Map<UUID, List<String>> m = new HashMap<>();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            String[] a = d.portes.get(p.getUUID());
            if (a != null) m.put(p.getUUID(), Arrays.stream(a).map(x -> x == null ? "" : x).toList());
        }
        Portes paquet = new Portes(m);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) Economie.envoyer(p, paquet);
    }

    private static void action(ServerPlayer p, Action a) {
        Donnees d = d(p.server);
        Cosm c = get(a.id());
        UUID u = p.getUUID();
        Set<String> possedes = d.possedes.computeIfAbsent(u, k -> new HashSet<>());
        if (a.action() == 1 && c != null && !possedes.contains(c.id())) {
            long solde = d.eclats.getOrDefault(u, 0L);
            if (solde < c.prix()) { p.sendSystemMessage(Component.literal("Pas assez d'éclats astraux (" + solde + " / " + c.prix() + " ✧).").withStyle(ChatFormatting.RED)); }
            else {
                d.eclats.put(u, solde - c.prix()); possedes.add(c.id()); d.portes(u)[c.partie()] = c.id();
                p.sendSystemMessage(Component.literal("✧ Tu as acheté « " + c.nom() + " » !").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        } else if (a.action() == 2 && c != null && possedes.contains(c.id())) d.portes(u)[c.partie()] = c.id();
        else if (a.action() == 3 && c != null) { String[] t = d.portes(u); if (c.id().equals(t[c.partie()])) t[c.partie()] = null; }
        d.setDirty();
        envoyerVue(p);
        if (a.action() != 0) diffuser(p.server);
    }

    // ------------------------------------------------------------------ commandes
    private static void commandes(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("eclats")
                .executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException();
                    p.sendSystemMessage(Component.literal("✧ Tes éclats astraux : " + d(p.server).eclats.getOrDefault(p.getUUID(), 0L)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)); return 1; })
                .then(Commands.literal("donner").requires(s -> s.hasPermission(2)).then(Commands.argument("joueurs", GameProfileArgument.gameProfile())
                        .then(Commands.argument("nombre", LongArgumentType.longArg(1, 1_000_000)).executes(c -> modifier(c.getSource(), GameProfileArgument.getGameProfiles(c, "joueurs"), LongArgumentType.getLong(c, "nombre"))))))
                .then(Commands.literal("retirer").requires(s -> s.hasPermission(2)).then(Commands.argument("joueurs", GameProfileArgument.gameProfile())
                        .then(Commands.argument("nombre", LongArgumentType.longArg(1, 1_000_000)).executes(c -> modifier(c.getSource(), GameProfileArgument.getGameProfiles(c, "joueurs"), -LongArgumentType.getLong(c, "nombre")))))));
    }

    // Marche aussi pour un joueur hors ligne (achat sur la boutique en ligne pendant qu'il ne joue pas).
    private static int modifier(CommandSourceStack src, Collection<GameProfile> joueurs, long n) {
        Donnees d = d(src.getServer());
        for (GameProfile g : joueurs) {
            long v = Math.max(0, d.eclats.getOrDefault(g.getId(), 0L) + n);
            d.eclats.put(g.getId(), v);
            src.sendSuccess(() -> Component.literal("Éclats de " + g.getName() + " : " + v + " (" + (n > 0 ? "+" : "") + n + ")"), true);
            ServerPlayer p = src.getServer().getPlayerList().getPlayer(g.getId());
            if (p != null) {
                envoyerVue(p);
                if (n > 0) p.sendSystemMessage(Component.literal("✧ Tu as reçu " + n + " éclats astraux. Merci pour ton soutien !").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        }
        d.setDirty();
        return joueurs.size();
    }

    private Cosmetiques() {}
}
