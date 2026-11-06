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
 * Cosmetiques (chapeaux, casques, jambieres, chaussures en 3D, et tenues), profil et coffres cosmetiques, payes en rubis bleus, la monnaie premium.
 * Regles Minecraft : purement visuels, pas de cape ; les rubis bleus ne s'echangent pas entre joueurs et ne se revendent pas.
 * Ils sont credites par la boutique en ligne avec la commande console « rubis donner <joueur> <n> » (ou l'ancienne « eclats donner »).
 */
final class Cosmetiques {
    static final String[] PARTIES = {"Tête", "Buste", "Jambes", "Pieds"};
    static final String[] CATEGORIES = {"Chapeaux", "Casques", "Jambières", "Chaussures", "Tenues"};
    static final String[] RARETES = {"Commun", "Rare", "Épique", "Légendaire"};
    static final int[] COULEURS_RARETE = {0x6EE07A, 0x4C9BFF, 0xB46CFF, 0xF0B429};

    /** couleur : teinte principale ; base : modele 3D (null = tenue peinte sur le skin) ; categorie : onglet ; rarete : 0 a 3. */
    record Cosm(String id, int partie, String nom, int prix, int couleur, String base, int categorie, int rarete) {}

    private static Cosm tenue(String id, int partie, String nom, int prix, int couleur) { return new Cosm(id, partie, nom, prix, couleur, null, 4, prix >= 400 ? 1 : 0); }

    static final List<Cosm> LISTE;
    static {
        List<Cosm> l = new ArrayList<>(CatalogueCosm.LISTE);
        l.addAll(List.of(
            tenue("couronne_astrale", 0, "Couronne astrale", 300, 0xF0B429), tenue("chapeau_archimage", 0, "Chapeau d'archimage", 250, 0x3C2C6E),
            tenue("cornes_nebuleuse", 0, "Cornes de nébuleuse", 350, 0xB46CFF),
            tenue("robe_etoilee", 1, "Robe étoilée", 400, 0x3C2C6E), tenue("plastron_runique", 1, "Plastron runique", 450, 0x3C78BE),
            tenue("manteau_aurore", 1, "Manteau d'aurore", 400, 0x50E6B4),
            tenue("pantalon_nebuleuse", 2, "Pantalon de nébuleuse", 250, 0x961E78), tenue("jambieres_runiques", 2, "Jambières runiques", 300, 0x3C78BE),
            tenue("pans_astraux", 2, "Pans astraux", 300, 0x9660E6),
            tenue("bottes_comete", 3, "Bottes comète", 250, 0x8CD2FA), tenue("sandales_lune", 3, "Sandales de lune", 200, 0xE6E6F0),
            tenue("bottes_flamme", 3, "Bottes de flamme astrale", 300, 0xF06E1E)));
        LISTE = List.copyOf(l);
    }

    static Cosm get(String id) { for (Cosm c : LISTE) if (c.id().equals(id)) return c; return null; }

    static final java.time.ZoneId PARIS = java.time.ZoneId.of("Europe/Paris");

    /** Prix du pack du mois pour ce joueur : 70 % des pieces qu'il n'a pas encore. */
    static long prixPack(List<Cosm> pack, java.util.Collection<String> possedes) {
        long t = 0;
        for (Cosm c : pack) if (!possedes.contains(c.id())) t += c.prix();
        return t * Profil.REMISE_PACK / 100;
    }

    // ------------------------------------------------------------------ donnees
    static final class Donnees extends SavedData {
        final Map<UUID, Long> eclats = new HashMap<>();
        final Map<UUID, Set<String>> possedes = new HashMap<>();
        final Map<UUID, String[]> portes = new HashMap<>();
        final Map<UUID, CompoundTag> profils = new HashMap<>();

        static Donnees lire(CompoundTag t) {
            Donnees d = new Donnees();
            CompoundTag e = t.getCompound("eclats"), p = t.getCompound("possedes"), o = t.getCompound("portes"), pr = t.getCompound("profils");
            for (String k : pr.getAllKeys()) d.profils.put(UUID.fromString(k), pr.getCompound(k));
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
            CompoundTag pr = new CompoundTag();
            profils.forEach((u, c) -> pr.put(u.toString(), c));
            t.put("eclats", e); t.put("possedes", p); t.put("portes", o); t.put("profils", pr);
            return t;
        }

        String[] portes(UUID u) { return portes.computeIfAbsent(u, k -> new String[4]); }

        CompoundTag profil(UUID u) { return profils.computeIfAbsent(u, k -> new CompoundTag()); }
    }

    static Donnees d(MinecraftServer s) { return s.overworld().getDataStorage().computeIfAbsent(Donnees::lire, Donnees::new, "astralys_cosmetiques"); }

    // ------------------------------------------------------------------ paquets
    /** profil : choix de banniere, grade et emote favorite (cles fond, motif, embleme, grade, emote). */
    record Vue(long eclats, List<String> possedes, List<String> portes, CompoundTag profil) {
        void ecrire(FriendlyByteBuf b) { b.writeVarLong(eclats); b.writeCollection(possedes, FriendlyByteBuf::writeUtf); b.writeCollection(portes, FriendlyByteBuf::writeUtf); b.writeNbt(profil); }
        static Vue lire(FriendlyByteBuf b) { return new Vue(b.readVarLong(), b.readList(FriendlyByteBuf::readUtf), b.readList(FriendlyByteBuf::readUtf), b.readNbt()); }

        String choix(int type) { String s = profil == null ? "" : profil.getString(Profil.TYPES[type]); return s.isEmpty() ? Profil.defaut(type) : s; }
        boolean a(Profil.Option o) { return o.prix() == 0 || possedes.contains(o.cle()); }
    }

    /** Un joueur joue une emote : relaye a tous ceux qui le voient. */
    record Emote(UUID joueur, String id) {
        void ecrire(FriendlyByteBuf b) { b.writeUUID(joueur); b.writeUtf(id); }
        static Emote lire(FriendlyByteBuf b) { return new Emote(b.readUUID(), b.readUtf(32)); }
    }

    /** Ce que porte chaque joueur connecte (pour que tout le monde voie les cosmetiques des autres). */
    record Portes(Map<UUID, List<String>> joueurs) {
        void ecrire(FriendlyByteBuf b) { b.writeMap(joueurs, FriendlyByteBuf::writeUUID, (bb, l) -> bb.writeCollection(l, FriendlyByteBuf::writeUtf)); }
        static Portes lire(FriendlyByteBuf b) { return new Portes(b.readMap(FriendlyByteBuf::readUUID, bb -> bb.readList(FriendlyByteBuf::readUtf))); }
    }

    /** 0 demander la vue, 1 acheter, 2 porter, 3 retirer, 4 acheter une option de profil, 5 la choisir, 6 acheter le pack du mois,
     *  7 ouvrir un coffre cosmetique (id = son numero), 8 jouer une emote. */
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
        Metiers.CANAL.messageBuilder(Emote.class, 16, NetworkDirection.PLAY_TO_CLIENT).encoder(Emote::ecrire).decoder(Emote::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Emotes.jouer(m.joueur(), m.id()))).add();
        // grade : prefixe devant le nom, dans le chat et la liste des joueurs
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.NameFormat e) -> {
            if (e.getEntity() instanceof ServerPlayer p) { Component g = prefixe(p); if (g != null) e.setDisplayname(g.copy().append(e.getDisplayname())); }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.TabListNameFormat e) -> {
            if (e.getEntity() instanceof ServerPlayer p) { Component g = prefixe(p); if (g != null) e.setDisplayName(g.copy().append(p.getGameProfile().getName())); }
        });
        MinecraftForge.EVENT_BUS.addListener(Cosmetiques::commandes);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { envoyerVue(p); diffuser(p.server); } });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) diffuser(p.server); });
    }

    static void envoyerVue(ServerPlayer p) {
        Donnees d = d(p.server);
        List<String> portes = new ArrayList<>();
        for (String x : d.portes(p.getUUID())) portes.add(x == null ? "" : x);
        Economie.envoyer(p, new Vue(d.eclats.getOrDefault(p.getUUID(), 0L), List.copyOf(d.possedes.getOrDefault(p.getUUID(), Set.of())), portes, d.profil(p.getUUID()).copy()));
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

    private static boolean payer(ServerPlayer p, Donnees d, long prix) {
        long solde = d.eclats.getOrDefault(p.getUUID(), 0L);
        if (solde < prix) { p.sendSystemMessage(Component.literal("Pas assez de rubis bleus (" + solde + " / " + prix + ").").withStyle(ChatFormatting.RED)); return false; }
        d.eclats.put(p.getUUID(), solde - prix);
        return true;
    }

    private static void action(ServerPlayer p, Action a) {
        Donnees d = d(p.server);
        Cosm c = get(a.id());
        UUID u = p.getUUID();
        Set<String> possedes = d.possedes.computeIfAbsent(u, k -> new HashSet<>());
        switch (a.action()) {
            case 1 -> {
                if (c != null && !possedes.contains(c.id()) && payer(p, d, c.prix())) {
                    possedes.add(c.id()); d.portes(u)[c.partie()] = c.id();
                    p.sendSystemMessage(Component.literal("✧ Tu as acheté « " + c.nom() + " » !").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
            }
            case 2 -> { if (c != null && possedes.contains(c.id())) d.portes(u)[c.partie()] = c.id(); }
            case 3 -> { if (c != null) { String[] t = d.portes(u); if (c.id().equals(t[c.partie()])) t[c.partie()] = null; } }
            case 4 -> {
                Profil.Option o = Profil.get(a.id());
                if (o != null && o.prix() > 0 && !possedes.contains(o.cle()) && payer(p, d, o.prix())) {
                    possedes.add(o.cle()); d.profil(u).putString(Profil.TYPES[o.type()], o.id());
                    p.sendSystemMessage(Component.literal("✧ Débloqué : " + o.nom() + " !").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    if (o.type() == 3) { p.refreshDisplayName(); p.refreshTabListName(); }
                }
            }
            case 5 -> {
                Profil.Option o = Profil.get(a.id());
                if (o != null && (o.prix() == 0 || possedes.contains(o.cle()))) {
                    d.profil(u).putString(Profil.TYPES[o.type()], o.id());
                    if (o.type() == 3) { p.refreshDisplayName(); p.refreshTabListName(); }
                }
            }
            case 6 -> {
                List<Cosm> pack = Profil.offreDuMois(java.time.LocalDate.now(PARIS));
                long prix = prixPack(pack, possedes);
                if (prix > 0 && payer(p, d, prix)) {
                    for (Cosm x : pack) { possedes.add(x.id()); d.portes(u)[x.partie()] = x.id(); }
                    p.sendSystemMessage(Component.literal("✧ Pack du mois débloqué : tout est équipé !").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
            }
            case 7 -> ouvrirCoffre(p, d, possedes, a.id());
            case 8 -> {
                Profil.Option o = Profil.get(4, a.id());
                if (o != null && (o.prix() == 0 || possedes.contains(o.cle()))) {
                    Emote m = new Emote(u, o.id());
                    for (ServerPlayer x : p.serverLevel().players()) if (x.distanceToSqr(p) < 96 * 96) Economie.envoyer(x, m);
                }
                return;
            }
            default -> {}
        }
        d.setDirty();
        envoyerVue(p);
        if (a.action() != 0) diffuser(p.server);
    }

    /** Tire une rarete selon les chances affichees, puis un cosmetique 3D que le joueur n'a pas encore (sinon rembourse). */
    private static void ouvrirCoffre(ServerPlayer p, Donnees d, Set<String> possedes, String id) {
        int k;
        try { k = Integer.parseInt(id); } catch (NumberFormatException e) { return; }
        if (k < 0 || k >= Profil.COFFRES.size()) return;
        Profil.CoffreCosm co = Profil.COFFRES.get(k);
        List<Cosm> libres = new ArrayList<>();
        for (Cosm c : CatalogueCosm.LISTE) if (!possedes.contains(c.id())) libres.add(c);
        if (libres.isEmpty()) { p.sendSystemMessage(Component.literal("Tu as déjà tous les cosmétiques : rien à gagner dans ce coffre !").withStyle(ChatFormatting.YELLOW)); return; }
        if (!payer(p, d, co.prix())) return;
        var rnd = p.getRandom();
        int rar = rarete(co, rnd);
        Cosm gain = null;
        for (int ecart = 0; gain == null; ecart++)   // rarete tiree, sinon la plus proche ou il en reste
            for (int r : new int[] {rar + ecart, rar - ecart}) {
                List<Cosm> l = libres.stream().filter(c -> c.rarete() == r).toList();
                if (gain == null && !l.isEmpty()) gain = l.get(rnd.nextInt(l.size()));
            }
        possedes.add(gain.id());
        List<net.minecraft.world.item.ItemStack> bande = new ArrayList<>();
        List<Integer> raretes = new ArrayList<>();
        for (int i = 0; i < Coffres.TAILLE; i++) {
            Cosm x = gain;
            if (i != Coffres.GAGNANT) { int r = rarete(co, rnd); List<Cosm> l = CatalogueCosm.LISTE.stream().filter(c -> c.rarete() == r).toList(); x = l.get(rnd.nextInt(l.size())); }
            bande.add(pile(x)); raretes.add(x.rarete());
        }
        Economie.envoyer(p, new Coffres.Roulette(co.modele(), bande, raretes, Coffres.GAGNANT));
        if (gain.rarete() >= 3)
            p.server.getPlayerList().broadcastSystemMessage(Component.literal("✦ " + p.getGameProfile().getName() + " a obtenu le cosmétique légendaire « " + gain.nom() + " » !")
                    .withStyle(st -> st.withColor(COULEURS_RARETE[3]).withBold(true)), false);
    }

    private static int rarete(Profil.CoffreCosm co, net.minecraft.util.RandomSource rnd) {
        int x = rnd.nextInt(100);
        for (int r = 0; r < 4; r++) if ((x -= co.chances()[r]) < 0) return r;
        return 3;
    }

    /** Objet d'affichage d'un cosmetique (roulette) : l'ecran dessine le mannequin qui le porte. */
    static net.minecraft.world.item.ItemStack pile(Cosm c) {
        var s = Metiers.pile("astralyscarte:rubis_bleu");
        s.getOrCreateTag().putString("astralys_cosm", c.id());
        s.setHoverName(Component.literal(c.nom()));
        return s;
    }

    static Component prefixe(ServerPlayer p) {
        String g = d(p.server).profil(p.getUUID()).getString("grade");
        Profil.Option o = g.isEmpty() ? null : Profil.get(3, g);
        if (o == null || o.prix() == 0) return null;
        return Component.literal("[" + o.nom().toUpperCase(java.util.Locale.FRANCE) + "] ").withStyle(st -> st.withColor(o.c1()).withBold(true));
    }

    // ------------------------------------------------------------------ commandes
    private static void commandes(RegisterCommandsEvent e) {
        for (String nom : new String[] {"rubis", "eclats"}) e.getDispatcher().register(Commands.literal(nom)
                .executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException();
                    p.sendSystemMessage(Component.literal("✧ Tes rubis bleus : " + d(p.server).eclats.getOrDefault(p.getUUID(), 0L)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)); return 1; })
                .then(Commands.literal("donner").requires(s -> s.hasPermission(2)).then(Commands.argument("joueurs", GameProfileArgument.gameProfile())
                        .then(Commands.argument("nombre", LongArgumentType.longArg(1, 1_000_000)).executes(c -> modifier(c.getSource(), GameProfileArgument.getGameProfiles(c, "joueurs"), LongArgumentType.getLong(c, "nombre"))))))
                .then(Commands.literal("retirer").requires(s -> s.hasPermission(2)).then(Commands.argument("joueurs", GameProfileArgument.gameProfile())
                        .then(Commands.argument("nombre", LongArgumentType.longArg(1, 1_000_000)).executes(c -> modifier(c.getSource(), GameProfileArgument.getGameProfiles(c, "joueurs"), -LongArgumentType.getLong(c, "nombre")))))));
        e.getDispatcher().register(Commands.literal("emote").then(Commands.argument("nom", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Profil.du(4).stream().map(Profil.Option::id), b))
                .executes(c -> { action(c.getSource().getPlayerOrException(), new Action(8, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "nom"))); return 1; })));
    }

    // Marche aussi pour un joueur hors ligne (achat sur la boutique en ligne pendant qu'il ne joue pas).
    private static int modifier(CommandSourceStack src, Collection<GameProfile> joueurs, long n) {
        Donnees d = d(src.getServer());
        for (GameProfile g : joueurs) {
            long v = Math.max(0, d.eclats.getOrDefault(g.getId(), 0L) + n);
            d.eclats.put(g.getId(), v);
            src.sendSuccess(() -> Component.literal("Rubis bleus de " + g.getName() + " : " + v + " (" + (n > 0 ? "+" : "") + n + ")"), true);
            ServerPlayer p = src.getServer().getPlayerList().getPlayer(g.getId());
            if (p != null) {
                envoyerVue(p);
                if (n > 0) p.sendSystemMessage(Component.literal("✧ Tu as reçu " + n + " rubis bleus. Merci pour ton soutien !").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
        }
        d.setDirty();
        return joueurs.size();
    }

    private Cosmetiques() {}
}
