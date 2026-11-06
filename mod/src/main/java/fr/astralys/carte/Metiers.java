package fr.astralys.carte;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

/** Metiers facon Paladium : XP par action, niveaux, 3 quetes quotidiennes. Tout est calcule par le serveur. */
final class Metiers {
    static final String[] NOMS = {"MINEUR", "FARMEUR", "HUNTER", "ALCHIMISTE"};
    static final int MINEUR = 0, FARMEUR = 1, HUNTER = 2, ALCHIMISTE = 3, NIVEAU_MAX = 100;
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final String VERSION = "3";   // a changer a chaque modification d'un paquet : client et serveur doivent avoir le meme mod
    // Les joueurs sans le mod peuvent toujours se connecter (ils n'ont simplement pas l'ecran).
    static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(new ResourceLocation(AstralysCarte.MODID, "metiers"),
            () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    // ------------------------------------------------------------------ niveaux
    /** XP pour passer du niveau n au niveau n+1. */
    // Tres long a monter (~1,45 million d'XP pour le niveau 100 d'un metier), mais rapide au debut.
    static long cout(int n) { return 100 + 25L * n + 4L * n * n; }

    static int niveau(long xp) {
        int n = 0;
        while (n < NIVEAU_MAX && xp >= cout(n)) { xp -= cout(n); n++; }
        return n;
    }

    /** XP deja gagnee dans le niveau en cours. */
    static long reste(long xp) {
        int n = 0;
        while (n < NIVEAU_MAX && xp >= cout(n)) { xp -= cout(n); n++; }
        return xp;
    }

    /** Bonus en pourcent, 0,1 % par niveau cumule sur les 4 metiers (affiche "BONUS QUETE" et "MULTIPLICATEUR"). */
    static float bonus(long[] xp) {
        int total = 0;
        for (long x : xp) total += niveau(x);
        return total * .1f;
    }

    // ------------------------------------------------------------------ quetes
    enum Type { CASSER, RECOLTER, TUER, VISITER, FABRIQUER, PREPARER }

    /** cible : "#ns:tag" (tag de bloc), "ns:id" (bloc, entite, dimension ou objet), "@monstre", "ns:" (tout un mod). */
    record Quete(String titre, String desc, Type type, String cible, int nombre, int metier, int recompense) {}

    // Dans l'ordre demande : minage (Sylvaria), chasse, puis exploration, construction/Create, ferme, alchimie en rotation.
    static final List<Quete> MINAGE = List.of(
            new Quete("Mineur de fer", "Casser 32 minerais de fer", Type.CASSER, "#forge:ores/iron", 32, MINEUR, 150),
            new Quete("Charbonnier", "Casser 64 minerais de charbon", Type.CASSER, "#forge:ores/coal", 64, MINEUR, 120),
            new Quete("Éclat de diamant", "Casser 3 minerais de diamant", Type.CASSER, "#forge:ores/diamond", 3, MINEUR, 300),
            new Quete("Filon d'or", "Casser 12 minerais d'or", Type.CASSER, "#forge:ores/gold", 12, MINEUR, 200),
            new Quete("Cuivre brut", "Casser 48 minerais de cuivre", Type.CASSER, "#forge:ores/copper", 48, MINEUR, 130),
            new Quete("Les profondeurs", "Casser 256 blocs de deepslate", Type.CASSER, "minecraft:deepslate", 256, MINEUR, 140),
            new Quete("Pierre rouge", "Casser 24 minerais de redstone", Type.CASSER, "#forge:ores/redstone", 24, MINEUR, 150));
    static final List<Quete> CHASSE = List.of(
            new Quete("Chasseur de nuit", "Tuer 20 zombies", Type.TUER, "minecraft:zombie", 20, HUNTER, 150),
            new Quete("Os brisés", "Tuer 15 squelettes", Type.TUER, "minecraft:skeleton", 15, HUNTER, 150),
            new Quete("Toiles et crocs", "Tuer 12 araignées", Type.TUER, "minecraft:spider", 12, HUNTER, 130),
            new Quete("Nettoyeur", "Tuer 30 monstres", Type.TUER, "@monstre", 30, HUNTER, 200),
            new Quete("Faune sauvage", "Tuer 6 créatures d'Alex's Mobs", Type.TUER, "alexsmobs:", 6, HUNTER, 220));
    static final List<Quete> ROTATION = List.of(
            new Quete("Mine de Sylvaria", "Aller miner à Sylvaria", Type.VISITER, "astralys:sylvaria", 1, MINEUR, 80),
            new Quete("Terres de feu", "Visiter Pyrélune", Type.VISITER, "astralys:pyrelune", 1, HUNTER, 80),
            new Quete("Royaume de glace", "Visiter Azuréa", Type.VISITER, "astralys:azurea", 1, HUNTER, 80),
            new Quete("Ingénieur", "Fabriquer 16 roues dentées Create", Type.FABRIQUER, "create:cogwheel", 16, MINEUR, 180),
            new Quete("Mécanicien", "Fabriquer 4 arbres de transmission", Type.FABRIQUER, "create:shaft", 4, MINEUR, 120),
            new Quete("Moisson", "Récolter 64 blés mûrs", Type.RECOLTER, "minecraft:wheat", 64, FARMEUR, 150),
            new Quete("Potager", "Récolter 32 carottes mûres", Type.RECOLTER, "minecraft:carrots", 32, FARMEUR, 130),
            new Quete("Citrouilles", "Récolter 12 citrouilles", Type.RECOLTER, "minecraft:pumpkin", 12, FARMEUR, 140),
            new Quete("Apprenti alchimiste", "Préparer 3 potions", Type.PREPARER, "*", 3, ALCHIMISTE, 200),
            new Quete("Verrier", "Fabriquer 9 fioles", Type.FABRIQUER, "minecraft:glass_bottle", 9, ALCHIMISTE, 120));

    /** Les 3 quetes du jour d'un joueur (memes pour tout le jour, differentes d'un joueur a l'autre). */
    static Quete[] duJour(UUID id, long jour) {
        Random r = new Random(jour * 31 + id.getLeastSignificantBits());
        return new Quete[] {MINAGE.get(r.nextInt(MINAGE.size())), CHASSE.get(r.nextInt(CHASSE.size())),
                ROTATION.get((int) Math.floorMod(jour + r.nextInt(3), ROTATION.size()))};
    }

    // ------------------------------------------------------------------ donnees sauvegardees
    static final class Joueur {
        String nom = "?";
        final long[] xp = new long[4];
        long jour = -1;
        final int[] avance = new int[3];
    }

    static final class Donnees extends SavedData {
        final Map<UUID, Joueur> joueurs = new HashMap<>();

        static Donnees lire(CompoundTag t) {
            Donnees d = new Donnees();
            for (String k : t.getAllKeys()) {
                CompoundTag c = t.getCompound(k);
                Joueur j = new Joueur();
                long[] xp = c.getLongArray("xp");
                System.arraycopy(xp, 0, j.xp, 0, Math.min(4, xp.length));
                j.jour = c.getLong("jour");
                if (c.contains("nom")) j.nom = c.getString("nom");
                int[] av = c.getIntArray("avance");
                System.arraycopy(av, 0, j.avance, 0, Math.min(3, av.length));
                d.joueurs.put(UUID.fromString(k), j);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            joueurs.forEach((id, j) -> {
                CompoundTag c = new CompoundTag();
                c.putLongArray("xp", j.xp);
                c.putLong("jour", j.jour);
                c.putIntArray("avance", j.avance);
                c.putString("nom", j.nom);
                t.put(id.toString(), c);
            });
            return t;
        }
    }

    private static Donnees donnees(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(Donnees::lire, Donnees::new, "astralys_metiers");
    }

    private static long aujourdhui() { return LocalDate.now(PARIS).toEpochDay(); }

    private static Joueur joueur(ServerPlayer p) {
        Donnees d = donnees(p.server);
        Joueur j = d.joueurs.computeIfAbsent(p.getUUID(), k -> new Joueur());
        long jour = aujourdhui();
        if (j.jour != jour) { j.jour = jour; java.util.Arrays.fill(j.avance, 0); d.setDirty(); }
        if (!j.nom.equals(p.getGameProfile().getName())) { j.nom = p.getGameProfile().getName(); d.setDirty(); }
        return j;
    }

    private static final Set<UUID> aEnvoyer = new HashSet<>();

    // ------------------------------------------------------------------ gains
    private static void gagner(ServerPlayer p, int metier, double base) {
        if (p.isCreative() || p.isSpectator() || base <= 0) return;
        Joueur j = joueur(p);
        long avant = j.xp[metier];
        j.xp[metier] += Math.max(1, Math.round(base * (1 + bonus(j.xp) / 100)));
        donnees(p.server).setDirty();
        int nv = niveau(j.xp[metier]);
        if (nv > niveau(avant)) {
            niveauSup(p, metier, nv);
            for (int pal = niveau(avant) / 10 + 1; pal <= nv / 10; pal++) palier(p, metier, pal);
            Economie.crediter(p, 100L * nv, NOMS[metier] + " niveau " + nv);
            Economie.defi(p, Economie.Defi.NIVEAU, nv - niveau(avant));
        }
        aEnvoyer.add(p.getUUID());
    }

    // ------------------------------------------------------------------ paliers (niveaux 10, 20 ... 100)
    // shortcut: objets du jeu en attendant les recompenses custom faites avec Blockbench, a remplacer palier par palier.
    static final String[][] PALIERS = {
            {"minecraft:iron_pickaxe", "minecraft:iron_ingot*16", "minecraft:diamond_pickaxe", "minecraft:diamond*8", "astralyscarte:pioche_cristal",
             "minecraft:diamond_block*2", "minecraft:emerald_block*4", "minecraft:netherite_scrap*4", "minecraft:netherite_ingot", "astralyscarte:pioche_celeste"},
            {"minecraft:iron_hoe", "minecraft:bone_meal*32", "minecraft:golden_carrot*16", "minecraft:diamond_hoe", "astralyscarte:houe_doree",
             "minecraft:hay_block*32", "minecraft:enchanted_golden_apple", "minecraft:golden_apple*16", "minecraft:netherite_ingot", "astralyscarte:faux_moissons"},
            {"minecraft:iron_sword", "minecraft:bow", "minecraft:arrow*64", "minecraft:diamond_sword", "astralyscarte:dague_chasseur",
             "minecraft:golden_apple*4", "minecraft:totem_of_undying", "minecraft:totem_of_undying*2", "minecraft:trident", "astralyscarte:lame_lunaire"},
            {"minecraft:brewing_stand", "minecraft:blaze_powder*16", "minecraft:nether_wart*16", "minecraft:glowstone_dust*32", "astralyscarte:fiole_eternelle",
             "minecraft:experience_bottle*16", "minecraft:experience_bottle*32", "minecraft:dragon_breath*4", "minecraft:enchanting_table", "astralyscarte:grimoire_astralys"}};

    static long argentPalier(int palier) { return 1000L * palier * 10; }

    /** "ns:id*n" -> pile d'objets (ou vide si l'objet n'existe pas). */
    static net.minecraft.world.item.ItemStack pile(String s) {
        String[] p = s.split("\\*");
        var it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(p[0]));
        return it == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(it, p.length > 1 ? Integer.parseInt(p[1]) : 1);
    }

    private static void palier(ServerPlayer p, int metier, int palier) {
        var obj = pile(PALIERS[metier][palier - 1]);
        if (!obj.isEmpty() && !p.getInventory().add(obj.copy())) p.drop(obj.copy(), false);
        Economie.crediter(p, argentPalier(palier), "palier " + palier * 10 + " " + NOMS[metier]);
        p.sendSystemMessage(Component.literal("★ PALIER " + palier * 10 + " " + NOMS[metier] + " : " + obj.getCount() + "× ").withStyle(st -> st.withColor(COULEURS[metier]).withBold(true))
                .append(obj.getHoverName()));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1, 1);
    }

    // ------------------------------------------------------------------ classement par metier
    /** Top 10 de chaque metier : lignes "nom|niveau|xp". */
    static List<List<String>> classement(MinecraftServer s) {
        List<List<String>> out = new ArrayList<>();
        for (int m = 0; m < 4; m++) {
            final int mm = m;
            List<String> l = new ArrayList<>();
            donnees(s).joueurs.values().stream().filter(j -> j.xp[mm] > 0).sorted((a, b) -> Long.compare(b.xp[mm], a.xp[mm])).limit(10)
                    .forEach(j -> l.add(j.nom + "|" + niveau(j.xp[mm]) + "|" + j.xp[mm]));
            out.add(l);
        }
        return out;
    }

    private static void niveauSup(ServerPlayer p, int metier, int nv) {
        p.sendSystemMessage(Component.literal("✦ " + NOMS[metier] + " niveau " + nv + " !").withStyle(st -> st.withColor(COULEURS[metier]).withBold(true)));
        ServerLevel l = p.serverLevel();
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 1.2f);
        l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1, 1);
        l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, .5, .8, .5, .3);
        l.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.5, p.getZ(), 60, .6, .6, .6, 1);
    }

    static final int[] COULEURS = {0xE8322B, 0xF5B800, 0x2F6BFF, 0xF060C0};

    /** Ce qui rapporte de l'XP, par metier (objet affiche, nom, XP de base) : affiche dans le detail des niveaux. */
    static final Object[][][] SOURCES = {
            {{"minecraft:coal_ore", "Charbon", 2}, {"minecraft:copper_ore", "Cuivre", 2}, {"minecraft:iron_ore", "Fer", 4}, {"minecraft:gold_ore", "Or", 6},
             {"minecraft:redstone_ore", "Redstone", 3}, {"minecraft:lapis_ore", "Lapis", 4}, {"minecraft:diamond_ore", "Diamant", 15},
             {"minecraft:emerald_ore", "Émeraude", 15}, {"minecraft:nether_quartz_ore", "Quartz", 2}, {"minecraft:ancient_debris", "Débris antique", 30}},
            {{"minecraft:wheat", "Blé mûr", 2}, {"minecraft:carrot", "Carotte mûre", 2}, {"minecraft:potato", "Pomme de terre", 2},
             {"minecraft:beetroot", "Betterave", 2}, {"minecraft:nether_wart", "Verrue du Nether", 2}, {"minecraft:pumpkin", "Citrouille", 3}, {"minecraft:melon", "Pastèque", 3}},
            {{"minecraft:zombie_head", "Zombie", 7}, {"minecraft:skeleton_skull", "Squelette", 7}, {"minecraft:creeper_head", "Creeper", 7},
             {"minecraft:spider_eye", "Araignée", 5}, {"minecraft:ender_pearl", "Enderman", 13}, {"minecraft:blaze_rod", "Blaze", 7},
             {"minecraft:wither_skeleton_skull", "Wither squelette", 7}, {"minecraft:beef", "Animal", 1}},
            {{"minecraft:potion", "Potion préparée", 6}, {"minecraft:splash_potion", "Potion jetable", 6}, {"minecraft:lingering_potion", "Potion persistante", 6}}};

    /** Avance des quetes du jour correspondant a l'action. */
    private static void avancer(ServerPlayer p, Type type, java.util.function.Predicate<String> cible, int n) {
        if (p.isCreative() || p.isSpectator()) return;
        Joueur j = joueur(p);
        Quete[] q = duJour(p.getUUID(), j.jour);
        for (int i = 0; i < 3; i++) {
            if (q[i].type != type || j.avance[i] >= q[i].nombre || !cible.test(q[i].cible)) continue;
            j.avance[i] = Math.min(q[i].nombre, j.avance[i] + n);
            donnees(p.server).setDirty();
            aEnvoyer.add(p.getUUID());
            if (j.avance[i] >= q[i].nombre) {
                p.sendSystemMessage(Component.literal("✔ Quête terminée : " + q[i].titre + "  (+" + q[i].recompense + " XP " + NOMS[q[i].metier] + ")")
                        .withStyle(st -> st.withColor(0x22C55E).withBold(true)));
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .8f, 1);
                gagner(p, q[i].metier, q[i].recompense);
                Economie.crediter(p, 2L * q[i].recompense, "quête " + q[i].titre);
            }
        }
    }

    private static boolean bloc(String cible, BlockState s) {
        if (cible.startsWith("#")) return s.is(TagKey.create(Registries.BLOCK, new ResourceLocation(cible.substring(1))));
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return id != null && id.toString().equals(cible);
    }

    private static final String[][] MINERAIS = {
            {"coal", "2"}, {"copper", "2"}, {"iron", "4"}, {"gold", "6"}, {"redstone", "3"}, {"lapis", "4"},
            {"diamond", "15"}, {"emerald", "15"}, {"quartz", "2"}, {"netherite_scrap", "30"}};

    private static void casse(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p)) return;
        BlockState s = e.getState();
        Block b = s.getBlock();
        // Mineur
        if (s.is(TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores")))) {
            double xp = 3;
            for (String[] m : MINERAIS) if (s.is(TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores/" + m[0])))) xp = Double.parseDouble(m[1]);
            gagner(p, MINEUR, xp);
            Economie.defi(p, Economie.Defi.MINERAI, 1);
        }
        avancer(p, Type.CASSER, c -> bloc(c, s), 1);
        // Farmeur : seulement les recoltes mures
        boolean mur = (b instanceof CropBlock c && c.isMaxAge(s)) || (b instanceof NetherWartBlock && s.getValue(NetherWartBlock.AGE) >= 3)
                || b == Blocks.PUMPKIN || b == Blocks.MELON;
        if (mur) {
            gagner(p, FARMEUR, b == Blocks.PUMPKIN || b == Blocks.MELON ? 3 : 2);
            avancer(p, Type.RECOLTER, c -> bloc(c, s), 1);
            Economie.defi(p, Economie.Defi.CULTURE, 1);
        }
    }

    private static void mort(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        LivingEntity v = e.getEntity();
        if (v instanceof ServerPlayer) return;
        gagner(p, HUNTER, v instanceof Enemy ? Math.max(2, v.getMaxHealth() / 3) : 1);
        if (v instanceof Enemy) Economie.defi(p, Economie.Defi.MONSTRE, 1);
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(v.getType());
        String ids = id == null ? "" : id.toString();
        avancer(p, Type.TUER, c -> c.equals("@monstre") ? v instanceof Enemy : c.endsWith(":") ? ids.startsWith(c) : ids.equals(c), 1);
    }

    private static void fabrique(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.getCrafting().getItem());
        String ids = id == null ? "" : id.toString();
        avancer(p, Type.FABRIQUER, ids::equals, e.getCrafting().getCount());
    }

    private static void potion(PlayerBrewedPotionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        gagner(p, ALCHIMISTE, 6);
        Economie.defi(p, Economie.Defi.POTION, 1);
        avancer(p, Type.PREPARER, c -> true, 1);
    }

    private static void visite(ServerPlayer p) {
        String dim = p.level().dimension().location().toString();
        avancer(p, Type.VISITER, dim::equals, 1);
    }

    // ------------------------------------------------------------------ reseau
    /** Ce que l'ecran affiche. */
    record Vue(long[] xp, float bonus, List<QueteVue> quetes) {
        void ecrire(FriendlyByteBuf b) {
            for (long x : xp) b.writeVarLong(x);
            b.writeFloat(bonus);
            b.writeVarInt(quetes.size());
            for (QueteVue q : quetes) { b.writeUtf(q.titre); b.writeUtf(q.desc); b.writeVarInt(q.avance); b.writeVarInt(q.nombre); b.writeVarInt(q.metier); b.writeVarInt(q.recompense); }
        }

        static Vue lire(FriendlyByteBuf b) {
            long[] xp = new long[4];
            for (int i = 0; i < 4; i++) xp[i] = b.readVarLong();
            float bonus = b.readFloat();
            int n = b.readVarInt();
            List<QueteVue> q = new ArrayList<>();
            for (int i = 0; i < n; i++) q.add(new QueteVue(b.readUtf(), b.readUtf(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
            return new Vue(xp, bonus, q);
        }
    }

    record QueteVue(String titre, String desc, int avance, int nombre, int metier, int recompense) {}

    private static void envoyer(ServerPlayer p) {
        if (!CANAL.isRemotePresent(p.connection.connection)) return;
        Joueur j = joueur(p);
        Quete[] q = duJour(p.getUUID(), j.jour);
        List<QueteVue> v = new ArrayList<>();
        for (int i = 0; i < 3; i++) v.add(new QueteVue(q[i].titre, q[i].desc, j.avance[i], q[i].nombre, q[i].metier, q[i].recompense));
        CANAL.send(PacketDistributor.PLAYER.with(() -> p), new Vue(j.xp.clone(), bonus(j.xp), v));
    }

    static void init() {
        CANAL.messageBuilder(Vue.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Vue::ecrire).decoder(Vue::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMetiers.recevoir(m))).add();
        MinecraftForge.EVENT_BUS.addListener(Metiers::casse);
        MinecraftForge.EVENT_BUS.addListener(Metiers::mort);
        MinecraftForge.EVENT_BUS.addListener(Metiers::fabrique);
        MinecraftForge.EVENT_BUS.addListener(Metiers::potion);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) visite(p); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { visite(p); aEnvoyer.add(p.getUUID()); } });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            // envoi groupe, deux fois par seconde au plus ; et a minuit les nouvelles quetes
            if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 10 != 0) return;
            if (e.getServer().getTickCount() % 1200 == 0) for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) aEnvoyer.add(p.getUUID());
            for (UUID id : aEnvoyer) { ServerPlayer p = e.getServer().getPlayerList().getPlayer(id); if (p != null) envoyer(p); }
            aEnvoyer.clear();
        });
    }

    private Metiers() {}
}
