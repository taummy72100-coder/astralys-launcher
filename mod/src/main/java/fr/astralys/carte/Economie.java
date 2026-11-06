package fr.astralys.carte;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/** Argent, boutique a cours variables, factions, achat de chunks et defis de la semaine. Tout est decide par le serveur. */
final class Economie {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    // ------------------------------------------------------------------ boutique
    static final String[] CATEGORIES = {"Minerais", "Agriculture", "Butin", "Pierres", "Bois", "Couleurs", "Nether & End", "Create", "Alchimie", "Clés"};

    record Article(String item, int categorie, long base) {}

    static final List<Article> ARTICLES = new ArrayList<>();

    private static void article(int cat, Object... paires) {
        for (int i = 0; i < paires.length; i += 2) ARTICLES.add(new Article((String) paires[i], cat, ((Number) paires[i + 1]).longValue()));
    }

    private static void serie(int cat, long prix, String modele, String... mots) {
        for (String m : mots) ARTICLES.add(new Article("minecraft:" + modele.replace("*", m), cat, prix));
    }

    static {
        String[] couleurs = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        // ---- minerais : bruts, lingots, pepites, minerais en bloc (normaux et deepslate), blocs de stockage
        article(0, "minecraft:coal", 4, "minecraft:charcoal", 3, "minecraft:raw_copper", 4, "minecraft:copper_ingot", 6, "minecraft:raw_iron", 8,
                "minecraft:iron_nugget", 2, "minecraft:iron_ingot", 12, "minecraft:raw_gold", 14, "minecraft:gold_nugget", 3, "minecraft:gold_ingot", 20,
                "minecraft:redstone", 4, "minecraft:lapis_lazuli", 6, "minecraft:quartz", 5, "minecraft:amethyst_shard", 8, "minecraft:emerald", 90,
                "minecraft:diamond", 120, "minecraft:netherite_scrap", 600, "minecraft:netherite_ingot", 2600, "minecraft:flint", 3);
        String[][] minerais = {{"coal", "6"}, {"copper", "6"}, {"iron", "11"}, {"gold", "18"}, {"redstone", "10"}, {"lapis", "14"}, {"diamond", "140"}, {"emerald", "110"}};
        for (String[] m : minerais) {
            article(0, "minecraft:" + m[0] + "_ore", Long.parseLong(m[1]), "minecraft:deepslate_" + m[0] + "_ore", Long.parseLong(m[1]) + 2);
        }
        article(0, "minecraft:nether_gold_ore", 10, "minecraft:nether_quartz_ore", 8, "minecraft:ancient_debris", 650,
                "minecraft:coal_block", 36, "minecraft:copper_block", 54, "minecraft:raw_copper_block", 36, "minecraft:iron_block", 108, "minecraft:raw_iron_block", 72,
                "minecraft:gold_block", 180, "minecraft:raw_gold_block", 126, "minecraft:redstone_block", 36, "minecraft:lapis_block", 54,
                "minecraft:diamond_block", 1080, "minecraft:emerald_block", 810, "minecraft:amethyst_block", 32, "minecraft:netherite_block", 23400);
        // ---- agriculture
        article(1, "minecraft:wheat", 3, "minecraft:wheat_seeds", 1, "minecraft:carrot", 3, "minecraft:potato", 3, "minecraft:baked_potato", 5, "minecraft:beetroot", 3,
                "minecraft:beetroot_seeds", 1, "minecraft:sugar_cane", 3, "minecraft:sugar", 3, "minecraft:melon_slice", 2, "minecraft:melon", 16, "minecraft:pumpkin", 6,
                "minecraft:pumpkin_seeds", 1, "minecraft:apple", 5, "minecraft:sweet_berries", 2, "minecraft:glow_berries", 4, "minecraft:cocoa_beans", 4,
                "minecraft:bamboo", 1, "minecraft:cactus", 3, "minecraft:kelp", 1, "minecraft:dried_kelp", 2, "minecraft:egg", 3, "minecraft:milk_bucket", 20,
                "minecraft:honeycomb", 8, "minecraft:honey_bottle", 10, "minecraft:leather", 8, "minecraft:feather", 2, "minecraft:white_wool", 6,
                "minecraft:beef", 6, "minecraft:cooked_beef", 9, "minecraft:porkchop", 6, "minecraft:cooked_porkchop", 9, "minecraft:chicken", 4,
                "minecraft:cooked_chicken", 7, "minecraft:mutton", 5, "minecraft:cooked_mutton", 8, "minecraft:cod", 4, "minecraft:salmon", 5,
                "minecraft:bread", 10, "minecraft:cookie", 3, "minecraft:pumpkin_pie", 14, "minecraft:cake", 40, "minecraft:golden_carrot", 25,
                "minecraft:golden_apple", 300, "minecraft:hay_block", 27, "minecraft:bone_meal", 2);
        // ---- butin de monstres
        article(2, "minecraft:rotten_flesh", 2, "minecraft:bone", 4, "minecraft:string", 4, "minecraft:spider_eye", 5, "minecraft:arrow", 2,
                "minecraft:gunpowder", 8, "minecraft:slime_ball", 10, "minecraft:phantom_membrane", 25, "minecraft:ender_pearl", 40, "minecraft:ink_sac", 4,
                "minecraft:glow_ink_sac", 10, "minecraft:prismarine_shard", 6, "minecraft:prismarine_crystals", 8, "minecraft:rabbit_hide", 4,
                "minecraft:scute", 60, "minecraft:nautilus_shell", 80, "minecraft:totem_of_undying", 900, "minecraft:saddle", 120, "minecraft:name_tag", 80);
        // ---- pierres et blocs de construction
        article(3, "minecraft:cobblestone", 2, "minecraft:stone", 3, "minecraft:smooth_stone", 4, "minecraft:stone_bricks", 4, "minecraft:mossy_stone_bricks", 5,
                "minecraft:cracked_stone_bricks", 5, "minecraft:chiseled_stone_bricks", 6, "minecraft:mossy_cobblestone", 4, "minecraft:cobbled_deepslate", 2,
                "minecraft:deepslate", 3, "minecraft:polished_deepslate", 4, "minecraft:deepslate_bricks", 5, "minecraft:deepslate_tiles", 5, "minecraft:tuff", 3,
                "minecraft:calcite", 4, "minecraft:dripstone_block", 4, "minecraft:andesite", 2, "minecraft:polished_andesite", 3, "minecraft:diorite", 2,
                "minecraft:polished_diorite", 3, "minecraft:granite", 2, "minecraft:polished_granite", 3, "minecraft:dirt", 2, "minecraft:coarse_dirt", 2,
                "minecraft:grass_block", 4, "minecraft:podzol", 4, "minecraft:mud", 2, "minecraft:mud_bricks", 4, "minecraft:clay_ball", 2, "minecraft:clay", 8,
                "minecraft:bricks", 6, "minecraft:sand", 2, "minecraft:red_sand", 2, "minecraft:sandstone", 4, "minecraft:smooth_sandstone", 5,
                "minecraft:cut_sandstone", 5, "minecraft:red_sandstone", 4, "minecraft:gravel", 2, "minecraft:glass", 4, "minecraft:glass_pane", 2,
                "minecraft:obsidian", 25, "minecraft:crying_obsidian", 35, "minecraft:ice", 4, "minecraft:packed_ice", 10, "minecraft:blue_ice", 40,
                "minecraft:snow_block", 3, "minecraft:moss_block", 4, "minecraft:prismarine", 20, "minecraft:prismarine_bricks", 45, "minecraft:dark_prismarine", 50,
                "minecraft:sea_lantern", 30, "minecraft:lantern", 14, "minecraft:torch", 1, "minecraft:ladder", 2, "minecraft:chest", 10, "minecraft:barrel", 10,
                "minecraft:bookshelf", 30, "minecraft:terracotta", 5);
        // ---- bois de toutes les essences
        for (String b : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"}) {
            article(4, "minecraft:" + b + "_log", 6, "minecraft:stripped_" + b + "_log", 6, "minecraft:" + b + "_planks", 2, "minecraft:" + b + "_sapling", 3,
                    "minecraft:" + b + "_leaves", 1, "minecraft:" + b + "_stairs", 3, "minecraft:" + b + "_slab", 1, "minecraft:" + b + "_fence", 3, "minecraft:" + b + "_door", 4);
        }
        article(4, "minecraft:mangrove_roots", 3, "minecraft:bamboo_block", 6, "minecraft:bamboo_planks", 2, "minecraft:bamboo_mosaic", 3, "minecraft:stick", 1);
        // ---- couleurs : laines, beton, poudre, terre cuite, verre, tapis, colorants
        serie(5, 6, "*_wool", couleurs);
        serie(5, 5, "*_concrete", couleurs);
        serie(5, 4, "*_concrete_powder", couleurs);
        serie(5, 6, "*_terracotta", couleurs);
        serie(5, 8, "*_glazed_terracotta", couleurs);
        serie(5, 6, "*_stained_glass", couleurs);
        serie(5, 3, "*_carpet", couleurs);
        serie(5, 3, "*_dye", couleurs);
        // ---- Nether et End
        article(6, "minecraft:netherrack", 1, "minecraft:nether_bricks", 4, "minecraft:red_nether_bricks", 6, "minecraft:soul_sand", 3, "minecraft:soul_soil", 3,
                "minecraft:basalt", 3, "minecraft:polished_basalt", 4, "minecraft:blackstone", 3, "minecraft:polished_blackstone", 4, "minecraft:polished_blackstone_bricks", 5,
                "minecraft:gilded_blackstone", 30, "minecraft:magma_block", 12, "minecraft:glowstone_dust", 4, "minecraft:glowstone", 18, "minecraft:shroomlight", 15,
                "minecraft:crimson_stem", 6, "minecraft:warped_stem", 6, "minecraft:crimson_planks", 2, "minecraft:warped_planks", 2, "minecraft:nether_wart", 5,
                "minecraft:nether_wart_block", 10, "minecraft:warped_wart_block", 10, "minecraft:quartz_block", 24, "minecraft:quartz_bricks", 26,
                "minecraft:blaze_rod", 30, "minecraft:ghast_tear", 60, "minecraft:magma_cream", 18, "minecraft:end_stone", 4, "minecraft:end_stone_bricks", 6,
                "minecraft:purpur_block", 8, "minecraft:chorus_fruit", 6, "minecraft:popped_chorus_fruit", 8, "minecraft:shulker_shell", 250, "minecraft:ender_eye", 70);
        // ---- Create
        article(7, "create:shaft", 6, "create:raw_zinc", 8, "create:zinc_ingot", 12, "create:zinc_nugget", 2, "create:zinc_block", 108, "create:andesite_alloy", 10,
                "create:cogwheel", 14, "create:large_cogwheel", 20, "create:andesite_casing", 22, "create:brass_ingot", 24, "create:brass_nugget", 3,
                "create:brass_sheet", 28, "create:copper_sheet", 10, "create:iron_sheet", 16, "create:golden_sheet", 26, "create:copper_casing", 30,
                "create:brass_casing", 50, "create:belt_connector", 30, "create:gearbox", 40, "create:water_wheel", 90, "create:large_water_wheel", 260,
                "create:windmill_bearing", 120, "create:mechanical_press", 180, "create:mechanical_mixer", 200, "create:mechanical_drill", 150,
                "create:mechanical_saw", 150, "create:encased_fan", 140, "create:millstone", 120, "create:depot", 30, "create:chute", 20,
                "create:fluid_pipe", 12, "create:fluid_tank", 40, "create:mechanical_pump", 90, "create:electron_tube", 30, "create:precision_mechanism", 400,
                "create:wrench", 40, "create:goggles", 60);
        // ---- alchimie
        // ---- minerais d'Alex's Caves
        article(0, "alexscaves:coprolith_coal_ore", 6, "alexscaves:galena_iron_ore", 14, "alexscaves:guanostone_redstone_ore", 8, "alexscaves:radrock_uranium_ore", 120,
                "alexscaves:raw_scarlet_neodymium", 30, "alexscaves:raw_azure_neodymium", 30, "alexscaves:scarlet_neodymium_ingot", 45, "alexscaves:azure_neodymium_ingot", 45,
                "alexscaves:block_of_scarlet_neodymium", 405, "alexscaves:block_of_azure_neodymium", 405, "alexscaves:uranium_shard", 10, "alexscaves:uranium", 90,
                "alexscaves:block_of_uranium", 810, "alexscaves:sulfur_dust", 6, "alexscaves:sulfur", 24, "alexscaves:amber", 20, "alexscaves:pearl", 40,
                "alexscaves:block_of_pearl", 360, "alexscaves:scrap_metal", 8, "alexscaves:occult_gem", 500);
        // ---- cles des coffres (chaque coffre a la sienne)
        article(9, "astralyscarte:cle_commun", 1500, "astralyscarte:cle_rare", 6000, "astralyscarte:cle_epique", 20000,
                "astralyscarte:cle_legendaire", 60000, "astralyscarte:cle_special", 200000);
        article(8, "minecraft:glass_bottle", 3, "minecraft:fermented_spider_eye", 12, "minecraft:blaze_powder", 16, "minecraft:glistering_melon_slice", 30,
                "minecraft:rabbit_foot", 40, "minecraft:pufferfish", 15, "minecraft:phantom_membrane", 25, "minecraft:experience_bottle", 50,
                "minecraft:brewing_stand", 120, "minecraft:cauldron", 60, "minecraft:dragon_breath", 200, "minecraft:enchanting_table", 400, "minecraft:book", 10,
                "minecraft:lapis_lazuli", 6, "minecraft:redstone", 4, "minecraft:glowstone_dust", 4, "minecraft:gunpowder", 8);
        // ---- objets les plus durs a obtenir : en vente, mais tres chers
        article(2, "minecraft:enchanted_golden_apple", 50_000, "minecraft:heart_of_the_sea", 15_000, "minecraft:trident", 20_000,
                "minecraft:wither_skeleton_skull", 8_000, "minecraft:nether_star", 50_000, "minecraft:beacon", 75_000, "minecraft:elytra", 150_000,
                "minecraft:dragon_head", 100_000, "minecraft:netherite_upgrade_smithing_template", 30_000);
        // ---- plus c'est difficile a obtenir, plus c'est cher (remplace les prix de base ci-dessus)
        cher("minecraft:diamond", 600, "minecraft:diamond_ore", 700, "minecraft:deepslate_diamond_ore", 720, "minecraft:diamond_block", 5_400,
                "minecraft:emerald", 300, "minecraft:emerald_ore", 350, "minecraft:deepslate_emerald_ore", 370, "minecraft:emerald_block", 2_700,
                "minecraft:ancient_debris", 6_500, "minecraft:netherite_scrap", 6_000, "minecraft:netherite_ingot", 25_000, "minecraft:netherite_block", 225_000,
                "minecraft:totem_of_undying", 15_000, "minecraft:golden_apple", 1_500, "minecraft:shulker_shell", 4_000, "minecraft:nautilus_shell", 600,
                "minecraft:scute", 300, "minecraft:ender_eye", 250, "minecraft:ender_pearl", 80, "minecraft:ghast_tear", 250, "minecraft:blaze_rod", 120,
                "minecraft:dragon_breath", 2_000, "minecraft:enchanting_table", 3_000, "minecraft:experience_bottle", 150, "minecraft:name_tag", 300,
                "minecraft:saddle", 300, "minecraft:phantom_membrane", 60, "minecraft:crying_obsidian", 80, "minecraft:blue_ice", 90,
                "alexscaves:occult_gem", 5_000, "alexscaves:uranium", 400, "alexscaves:block_of_uranium", 3_600, "alexscaves:radrock_uranium_ore", 450,
                "create:precision_mechanism", 800,
                "astralyscarte:cle_commun", 5_000, "astralyscarte:cle_rare", 25_000, "astralyscarte:cle_epique", 80_000,
                "astralyscarte:cle_legendaire", 250_000, "astralyscarte:cle_special", 1_000_000);
    }

    private static void cher(Object... paires) {
        for (int i = 0; i < paires.length; i += 2) {
            String id = (String) paires[i]; long prix = ((Number) paires[i + 1]).longValue();
            for (int k = 0; k < ARTICLES.size(); k++)
                if (ARTICLES.get(k).item().equals(id)) ARTICLES.set(k, new Article(id, ARTICLES.get(k).categorie(), prix));
        }
    }

    static long achat(long base, double f) { return Math.max(1, Math.round(base * f)); }
    static long vente(long base, double f) { return Math.max(1, Math.round(base * f * .5)); }

    // ------------------------------------------------------------------ factions et defis
    private static final Pattern NOM = Pattern.compile("[A-Za-z0-9_]{3,16}");
    static final Set<String> MONDES_BASES = Set.of("astralys:pyrelune", "astralys:azurea");

    enum Defi { MINERAI, MONSTRE, CULTURE, POTION, VENTE, NIVEAU }

    record DefiModele(String titre, String desc, Defi type, long nombre, long argent, long points) {}

    static final List<DefiModele> DEFIS = List.of(
            new DefiModele("Mine collective", "Casser 1500 minerais", Defi.MINERAI, 1500, 20000, 100),
            new DefiModele("Chasse royale", "Tuer 600 monstres", Defi.MONSTRE, 600, 18000, 100),
            new DefiModele("Grande moisson", "Récolter 2000 cultures", Defi.CULTURE, 2000, 15000, 80),
            new DefiModele("Cercle des alchimistes", "Préparer 150 potions", Defi.POTION, 150, 15000, 80),
            new DefiModele("Marchands", "Vendre pour 50 000 $ au shop", Defi.VENTE, 50000, 25000, 120),
            new DefiModele("Maîtres artisans", "Gagner 25 niveaux de métier", Defi.NIVEAU, 25, 20000, 120));

    static DefiModele[] defisDeLaSemaine(long semaine) {
        int n = DEFIS.size(), a = (int) Math.floorMod(semaine, n);
        return new DefiModele[] {DEFIS.get(a), DEFIS.get((a + 2) % n), DEFIS.get((a + 4) % n)};
    }

    static long semaine() { return Math.floorDiv(LocalDate.now(PARIS).toEpochDay() - 4, 7); } // semaines du lundi au dimanche

    static final class Faction {
        String nom;
        UUID chef;
        final Map<UUID, String> membres = new LinkedHashMap<>();
        final Set<UUID> invites = new HashSet<>();
        long banque, points, semaine = -1;
        final long[] defis = new long[3];

        CompoundTag ecrire() {
            CompoundTag t = new CompoundTag();
            t.putString("nom", nom); t.putUUID("chef", chef);
            CompoundTag m = new CompoundTag(); membres.forEach((u, n) -> m.putString(u.toString(), n)); t.put("membres", m);
            t.putLong("banque", banque); t.putLong("points", points); t.putLong("semaine", semaine); t.putLongArray("defis", defis);
            return t;
        }

        static Faction lire(CompoundTag t) {
            Faction f = new Faction();
            f.nom = t.getString("nom"); f.chef = t.getUUID("chef");
            CompoundTag m = t.getCompound("membres"); for (String k : m.getAllKeys()) f.membres.put(UUID.fromString(k), m.getString(k));
            f.banque = t.getLong("banque"); f.points = t.getLong("points"); f.semaine = t.getLong("semaine");
            long[] d = t.getLongArray("defis"); System.arraycopy(d, 0, f.defis, 0, Math.min(3, d.length));
            return f;
        }

        int maxClaims() { return Math.min(60, 5 + 3 * membres.size()); }
    }

    // ------------------------------------------------------------------ sauvegarde
    static final class Donnees extends SavedData {
        final Map<UUID, Long> argent = new HashMap<>();
        final Map<String, double[]> marche = new HashMap<>();     // item -> {facteur, facteur il y a une heure}
        final Map<String, Faction> factions = new HashMap<>();    // cle = nom en minuscules
        final Map<String, String> claims = new HashMap<>();       // "dimension|cx|cz" -> faction

        static Donnees lire(CompoundTag t) {
            Donnees d = new Donnees();
            CompoundTag a = t.getCompound("argent"); for (String k : a.getAllKeys()) d.argent.put(UUID.fromString(k), a.getLong(k));
            CompoundTag m = t.getCompound("marche"); for (String k : m.getAllKeys()) { CompoundTag c = m.getCompound(k); d.marche.put(k, new double[] {c.getDouble("f"), c.getDouble("h")}); }
            for (Tag x : t.getList("factions", Tag.TAG_COMPOUND)) { Faction f = Faction.lire((CompoundTag) x); d.factions.put(f.nom.toLowerCase(), f); }
            CompoundTag c = t.getCompound("claims"); for (String k : c.getAllKeys()) d.claims.put(k, c.getString(k));
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            CompoundTag a = new CompoundTag(); argent.forEach((u, v) -> a.putLong(u.toString(), v)); t.put("argent", a);
            CompoundTag m = new CompoundTag(); marche.forEach((k, v) -> { CompoundTag c = new CompoundTag(); c.putDouble("f", v[0]); c.putDouble("h", v[1]); m.put(k, c); }); t.put("marche", m);
            ListTag l = new ListTag(); factions.values().forEach(f -> l.add(f.ecrire())); t.put("factions", l);
            CompoundTag c = new CompoundTag(); claims.forEach(c::putString); t.put("claims", c);
            return t;
        }
    }

    static MinecraftServer serveur;

    static Donnees d() { return serveur.overworld().getDataStorage().computeIfAbsent(Donnees::lire, Donnees::new, "astralys_economie"); }

    private static double[] cours(String item) { return d().marche.computeIfAbsent(item, k -> new double[] {1, 1}); }

    // ------------------------------------------------------------------ argent
    static long solde(UUID u) { return d().argent.getOrDefault(u, 0L); }

    static void crediter(ServerPlayer p, long n, String raison) {
        if (n <= 0 || serveur == null) return;
        d().argent.merge(p.getUUID(), n, Long::sum); d().setDirty();
        if (raison != null) p.sendSystemMessage(Component.literal("+ " + format(n) + " $ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(raison).withStyle(ChatFormatting.GRAY)));
        envoyerSolde(p);
    }

    static boolean debiter(UUID u, long n) {
        long s = solde(u);
        if (n < 0 || s < n) return false;
        d().argent.put(u, s - n); d().setDirty();
        return true;
    }

    static String format(long n) { return String.format(java.util.Locale.FRANCE, "%,d", n).replace(' ', ' ').replace(' ', ' '); }

    // ------------------------------------------------------------------ factions : outils
    static Faction factionDe(UUID u) {
        for (Faction f : d().factions.values()) if (f.membres.containsKey(u)) return f;
        return null;
    }

    private static String cle(Level l, ChunkPos c) { return l.dimension().location() + "|" + c.x + "|" + c.z; }

    static Faction proprietaire(Level l, BlockPos pos) {
        String f = d().claims.get(cle(l, new ChunkPos(pos)));
        return f == null ? null : d().factions.get(f);
    }

    private static int nbClaims(Faction f) {
        String k = f.nom.toLowerCase(); int n = 0;
        for (String v : d().claims.values()) if (v.equals(k)) n++;
        return n;
    }

    static long prixClaim(Faction f) { return 500 + 250L * nbClaims(f); }

    private static void majSemaine(Faction f) {
        long s = semaine();
        if (f.semaine != s) { f.semaine = s; java.util.Arrays.fill(f.defis, 0); d().setDirty(); }
    }

    /** Une action d'un membre fait avancer les defis de sa faction. */
    static void defi(ServerPlayer p, Defi type, long n) {
        if (serveur == null || p.isCreative()) return;
        Faction f = factionDe(p.getUUID());
        if (f == null) return;
        majSemaine(f);
        DefiModele[] dm = defisDeLaSemaine(f.semaine);
        for (int i = 0; i < 3; i++) {
            if (dm[i].type != type || f.defis[i] >= dm[i].nombre) continue;
            f.defis[i] = Math.min(dm[i].nombre, f.defis[i] + n); d().setDirty();
            if (f.defis[i] >= dm[i].nombre) {
                f.banque += dm[i].argent; f.points += dm[i].points;
                annoncerFaction(f, Component.literal("★ Défi de la semaine réussi : " + dm[i].titre + " ! +" + format(dm[i].argent) + " $ en banque, +" + dm[i].points + " points")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
        }
    }

    private static void annoncerFaction(Faction f, Component c) {
        for (UUID u : f.membres.keySet()) {
            ServerPlayer p = serveur.getPlayerList().getPlayer(u);
            if (p != null) { p.sendSystemMessage(c); p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .7f, 1.2f); }
        }
    }

    // ------------------------------------------------------------------ protection des claims
    private static final Map<UUID, Long> dernierAvis = new HashMap<>();

    private static boolean interdit(ServerPlayer p, Level l, BlockPos pos) {
        if (p.hasPermissions(2)) return false;
        Faction f = proprietaire(l, pos);
        if (f == null || f.membres.containsKey(p.getUUID())) return false;
        long now = System.currentTimeMillis();
        if (now - dernierAvis.getOrDefault(p.getUUID(), 0L) > 2000) {
            dernierAvis.put(p.getUUID(), now);
            p.displayClientMessage(Component.literal("⛔ Territoire de la faction " + f.nom).withStyle(ChatFormatting.RED), true);
        }
        return true;
    }

    // ------------------------------------------------------------------ reseau
    record Solde(long argent) {
        void ecrire(FriendlyByteBuf b) { b.writeVarLong(argent); }
        static Solde lire(FriendlyByteBuf b) { return new Solde(b.readVarLong()); }
    }

    record Ligne(String item, int categorie, long achat, long vente, float tendance) {}

    /** Le catalogue vient du serveur, categories comprises : le client n'affiche que ce qui existe vraiment. */
    record Marche(List<String> categories, List<Ligne> lignes) {
        void ecrire(FriendlyByteBuf b) {
            b.writeVarInt(categories.size()); categories.forEach(b::writeUtf);
            b.writeVarInt(lignes.size());
            for (Ligne l : lignes) { b.writeUtf(l.item); b.writeVarInt(l.categorie); b.writeVarLong(l.achat); b.writeVarLong(l.vente); b.writeFloat(l.tendance); }
        }
        static Marche lire(FriendlyByteBuf b) {
            int nc = Math.min(b.readVarInt(), 32); List<String> c = new ArrayList<>();
            for (int i = 0; i < nc; i++) c.add(b.readUtf());
            int n = Math.min(b.readVarInt(), 4000); List<Ligne> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(new Ligne(b.readUtf(), b.readVarInt(), b.readVarLong(), b.readVarLong(), b.readFloat()));
            return new Marche(c, l);
        }
    }

    record DefiVue(String titre, String desc, long avance, long nombre, long argent, long points) {}

    record FactionVue(String nom, String chef, List<String> membres, List<Boolean> enLigne, long banque, long points, int claims, int max,
                      long prixClaim, List<DefiVue> defis, List<String> top) {
        void ecrire(FriendlyByteBuf b) {
            b.writeUtf(nom); b.writeUtf(chef);
            b.writeVarInt(membres.size()); for (int i = 0; i < membres.size(); i++) { b.writeUtf(membres.get(i)); b.writeBoolean(enLigne.get(i)); }
            b.writeVarLong(banque); b.writeVarLong(points); b.writeVarInt(claims); b.writeVarInt(max); b.writeVarLong(prixClaim);
            b.writeVarInt(defis.size()); for (DefiVue v : defis) { b.writeUtf(v.titre); b.writeUtf(v.desc); b.writeVarLong(v.avance); b.writeVarLong(v.nombre); b.writeVarLong(v.argent); b.writeVarLong(v.points); }
            b.writeVarInt(top.size()); top.forEach(b::writeUtf);
        }
        static FactionVue lire(FriendlyByteBuf b) {
            String nom = b.readUtf(), chef = b.readUtf();
            int n = Math.min(b.readVarInt(), 200); List<String> m = new ArrayList<>(); List<Boolean> e = new ArrayList<>();
            for (int i = 0; i < n; i++) { m.add(b.readUtf()); e.add(b.readBoolean()); }
            long banque = b.readVarLong(), points = b.readVarLong(); int claims = b.readVarInt(), max = b.readVarInt(); long prix = b.readVarLong();
            int nd = Math.min(b.readVarInt(), 10); List<DefiVue> d = new ArrayList<>();
            for (int i = 0; i < nd; i++) d.add(new DefiVue(b.readUtf(), b.readUtf(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong()));
            int nt = Math.min(b.readVarInt(), 20); List<String> t = new ArrayList<>(); for (int i = 0; i < nt; i++) t.add(b.readUtf());
            return new FactionVue(nom, chef, m, e, banque, points, claims, max, prix, d, t);
        }
    }

    /** Client -> serveur : 0 = donnees de la boutique, 1 = donnees de la faction. */
    record Demande(int quoi) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(quoi); }
        static Demande lire(FriendlyByteBuf b) { return new Demande(b.readVarInt()); }
    }

    /** Client -> serveur : acheter ou vendre n exemplaires d'un article (n = -1 : vendre tout). */
    record Transaction(String item, int n, boolean achat) {
        void ecrire(FriendlyByteBuf b) { b.writeUtf(item); b.writeVarInt(n); b.writeBoolean(achat); }
        static Transaction lire(FriendlyByteBuf b) { return new Transaction(b.readUtf(256), b.readVarInt(), b.readBoolean()); }
    }

    /** Serveur -> client : top 10 de chaque metier ("nom|niveau|xp"). */
    record Classement(List<List<String>> metiers) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(metiers.size()); for (List<String> l : metiers) { b.writeVarInt(l.size()); l.forEach(b::writeUtf); } }
        static Classement lire(FriendlyByteBuf b) {
            int n = Math.min(b.readVarInt(), 8); List<List<String>> out = new ArrayList<>();
            for (int i = 0; i < n; i++) { int k = Math.min(b.readVarInt(), 20); List<String> l = new ArrayList<>(); for (int j = 0; j < k; j++) l.add(b.readUtf()); out.add(l); }
            return new Classement(out);
        }
    }

    /** Serveur -> client : ouvrir un ecran (0 boutique, 1 faction, 2 metiers, 3 classement, 4 hotel des ventes). */
    record Ouvrir(int ecran) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(ecran); }
        static Ouvrir lire(FriendlyByteBuf b) { return new Ouvrir(b.readVarInt()); }
    }

    private static boolean aLeMod(ServerPlayer p) { return Metiers.CANAL.isRemotePresent(p.connection.connection); }

    static void envoyer(ServerPlayer p, Object m) { if (aLeMod(p)) Metiers.CANAL.send(PacketDistributor.PLAYER.with(() -> p), m); }

    static void envoyerSolde(ServerPlayer p) { envoyer(p, new Solde(solde(p.getUUID()))); }

    /** La boutique ne vend plus que les cles (les autres articles restent ici, prets a etre remis). */
    static final int CLES = 9;

    private static void envoyerMarche(ServerPlayer p) {
        List<Ligne> l = new ArrayList<>();
        for (Article a : ARTICLES) {
            if (a.categorie != CLES || !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(a.item))) continue;
            double[] c = cours(a.item);
            l.add(new Ligne(a.item, 0, achat(a.base, c[0]), vente(a.base, c[0]), (float) ((c[0] / c[1] - 1) * 100)));
        }
        envoyer(p, new Marche(List.of(CATEGORIES[CLES]), l));
        envoyerSolde(p);
    }

    private static void envoyerFaction(ServerPlayer p) {
        Faction f = factionDe(p.getUUID());
        List<String> top = new ArrayList<>();
        d().factions.values().stream().sorted(Comparator.comparingLong((Faction x) -> -x.points)).limit(5)
                .forEach(x -> top.add(x.nom + "|" + x.points));
        if (f == null) { envoyer(p, new FactionVue("", "", List.of(), List.of(), 0, 0, 0, 0, 0, List.of(), top)); return; }
        majSemaine(f);
        List<String> m = new ArrayList<>(); List<Boolean> e = new ArrayList<>();
        f.membres.forEach((u, n) -> { m.add(n); e.add(serveur.getPlayerList().getPlayer(u) != null); });
        DefiModele[] dm = defisDeLaSemaine(f.semaine);
        List<DefiVue> dv = new ArrayList<>();
        for (int i = 0; i < 3; i++) dv.add(new DefiVue(dm[i].titre, dm[i].desc, f.defis[i], dm[i].nombre, dm[i].argent, dm[i].points));
        envoyer(p, new FactionVue(f.nom, f.membres.getOrDefault(f.chef, "?"), m, e, f.banque, f.points, nbClaims(f), f.maxClaims(), prixClaim(f), dv, top));
        envoyerSolde(p);
    }

    private static void transaction(ServerPlayer p, Transaction t) {
        Article a = null;
        for (Article x : ARTICLES) if (x.item.equals(t.item) && x.categorie == CLES) a = x;
        Item item = a == null ? null : ForgeRegistries.ITEMS.getValue(new ResourceLocation(a.item));
        if (item == null || (t.n != -1 && (t.n < 1 || t.n > 2304))) return;    // paquet invalide : ignore
        double[] c = cours(a.item);
        Inventory inv = p.getInventory();
        if (t.achat) {
            long prix = achat(a.base, c[0]) * t.n;
            if (!debiter(p.getUUID(), prix)) { p.displayClientMessage(Component.literal("Pas assez d'argent (" + format(prix) + " $)").withStyle(ChatFormatting.RED), true); envoyerSolde(p); return; }
            int reste = t.n;
            while (reste > 0) {
                int k = Math.min(reste, item.getMaxStackSize());
                ItemStack s = new ItemStack(item, k);
                if (!inv.add(s)) p.drop(s, false);
                reste -= k;
            }
            c[0] = Math.min(3, c[0] * (1 + .003 * t.n));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .6f, 1.4f);
        } else {
            int voulu = t.n == -1 ? Integer.MAX_VALUE : t.n, vendu = 0;
            for (int i = 0; i < inv.getContainerSize() && vendu < voulu; i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() != item || s.hasTag()) continue;   // objets nommes / enchantes / abimes : jamais vendus
                int k = Math.min(s.getCount(), voulu - vendu);
                s.shrink(k); vendu += k;
            }
            if (vendu == 0) { p.displayClientMessage(Component.literal("Tu n'en as pas à vendre").withStyle(ChatFormatting.RED), true); return; }
            long gain = vente(a.base, c[0]) * vendu;
            c[0] = Math.max(.3, c[0] * (1 - .003 * vendu));
            d().argent.merge(p.getUUID(), gain, Long::sum);
            defi(p, Defi.VENTE, gain);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .8f, 1.2f);
            p.displayClientMessage(Component.literal("+" + format(gain) + " $  (" + vendu + " vendus)").withStyle(ChatFormatting.GOLD), true);
        }
        d().setDirty();
        envoyerMarche(p);
    }

    // ------------------------------------------------------------------ commandes
    private static ServerPlayer joueur(CommandContext<CommandSourceStack> c) throws CommandSyntaxException { return c.getSource().getPlayerOrException(); }

    private static int dire(ServerPlayer p, String texte, ChatFormatting... style) { p.sendSystemMessage(Component.literal(texte).withStyle(style)); return 1; }

    private static void commandes(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> dsp = e.getDispatcher();
        for (String n : new String[] {"argent", "money", "balance"})
            dsp.register(Commands.literal(n).executes(c -> dire(joueur(c), "✦ Ton solde : " + format(solde(joueur(c).getUUID())) + " $", ChatFormatting.GOLD, ChatFormatting.BOLD)));
        dsp.register(Commands.literal("pay").then(Commands.argument("joueur", EntityArgument.player()).then(Commands.argument("montant", LongArgumentType.longArg(1, 1_000_000_000L)).executes(c -> {
            ServerPlayer p = joueur(c), cible = EntityArgument.getPlayer(c, "joueur");
            long m = LongArgumentType.getLong(c, "montant");
            if (cible == p) return dire(p, "Tu ne peux pas te payer toi-même", ChatFormatting.RED);
            if (!debiter(p.getUUID(), m)) return dire(p, "Pas assez d'argent", ChatFormatting.RED);
            crediter(cible, m, "de " + p.getGameProfile().getName());
            envoyerSolde(p);
            return dire(p, "Tu as envoyé " + format(m) + " $ à " + cible.getGameProfile().getName(), ChatFormatting.GREEN);
        }))));
        dsp.register(Commands.literal("baltop").executes(c -> {
            ServerPlayer p = joueur(c);
            dire(p, "✦ Les plus riches d'Astralys", ChatFormatting.GOLD, ChatFormatting.BOLD);
            int[] i = {1};
            d().argent.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(10).forEach(en -> {
                String nom = serveur.getProfileCache().get(en.getKey()).map(g -> g.getName()).orElse("?");
                dire(p, " " + i[0]++ + ". " + nom + " — " + format(en.getValue()) + " $", ChatFormatting.YELLOW);
            });
            return 1;
        }));
        dsp.register(Commands.literal("eco").requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("joueur", EntityArgument.player()).then(Commands.argument("montant", LongArgumentType.longArg(1)).executes(c -> {
                    crediter(EntityArgument.getPlayer(c, "joueur"), LongArgumentType.getLong(c, "montant"), "(admin)"); return 1; }))))
                .then(Commands.literal("set").then(Commands.argument("joueur", EntityArgument.player()).then(Commands.argument("montant", LongArgumentType.longArg(0)).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "joueur"); d().argent.put(p.getUUID(), LongArgumentType.getLong(c, "montant")); d().setDirty(); envoyerSolde(p); return 1; })))));
        for (String n : new String[] {"shop", "boutique"})
            dsp.register(Commands.literal(n).executes(c -> ouvrir(joueur(c), 0)));
        dsp.register(Commands.literal("metiers").executes(c -> ouvrir(joueur(c), 2)));
        dsp.register(Commands.literal("classement").executes(c -> ouvrir(joueur(c), 3)));
        for (String n : new String[] {"hdv", "marche", "ventes"}) dsp.register(Commands.literal(n).executes(c -> ouvrir(joueur(c), 4)));
        for (String n : new String[] {"f", "faction"}) dsp.register(Commands.literal(n)
                .executes(c -> ouvrir(joueur(c), 1))
                .then(Commands.literal("create").then(Commands.argument("nom", StringArgumentType.word()).executes(c -> creer(joueur(c), StringArgumentType.getString(c, "nom")))))
                .then(Commands.literal("invite").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> inviter(joueur(c), EntityArgument.getPlayer(c, "joueur")))))
                .then(Commands.literal("join").then(Commands.argument("nom", StringArgumentType.word()).executes(c -> rejoindre(joueur(c), StringArgumentType.getString(c, "nom")))))
                .then(Commands.literal("leave").executes(c -> quitter(joueur(c))))
                .then(Commands.literal("kick").then(Commands.argument("nom", StringArgumentType.word()).executes(c -> exclure(joueur(c), StringArgumentType.getString(c, "nom")))))
                .then(Commands.literal("disband").executes(c -> dissoudre(joueur(c))))
                .then(Commands.literal("claim").executes(c -> claim(joueur(c))))
                .then(Commands.literal("unclaim").executes(c -> unclaim(joueur(c))))
                .then(Commands.literal("deposit").then(Commands.argument("montant", LongArgumentType.longArg(1)).executes(c -> deposer(joueur(c), LongArgumentType.getLong(c, "montant")))))
                .then(Commands.literal("top").executes(c -> { ServerPlayer p = joueur(c);
                    dire(p, "★ Classement des factions", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD); int[] i = {1};
                    d().factions.values().stream().sorted(Comparator.comparingLong((Faction x) -> -x.points)).limit(10)
                            .forEach(x -> dire(p, " " + i[0]++ + ". " + x.nom + " — " + format(x.points) + " pts", ChatFormatting.LIGHT_PURPLE));
                    return 1; })));
    }

    static int ouvrir(ServerPlayer p, int ecran) {
        if (!aLeMod(p)) return dire(p, "Il faut le mod Astralys (launcher) pour ouvrir cet écran.", ChatFormatting.RED);
        envoyer(p, new Ouvrir(ecran));
        if (ecran == 0) envoyerMarche(p); else if (ecran == 1) envoyerFaction(p);
        return 1;
    }

    private static int creer(ServerPlayer p, String nom) {
        if (factionDe(p.getUUID()) != null) return dire(p, "Tu es déjà dans une faction", ChatFormatting.RED);
        if (Metiers.niveauJoueur(p) < Metiers.NIVEAU_FACTION) return dire(p, verrouFaction(p), ChatFormatting.RED);
        if (!NOM.matcher(nom).matches()) return dire(p, "Nom : 3 à 16 lettres, chiffres ou _", ChatFormatting.RED);
        if (d().factions.containsKey(nom.toLowerCase())) return dire(p, "Ce nom est déjà pris", ChatFormatting.RED);
        Faction f = new Faction(); f.nom = nom; f.chef = p.getUUID(); f.membres.put(p.getUUID(), p.getGameProfile().getName()); f.semaine = semaine();
        d().factions.put(nom.toLowerCase(), f); d().setDirty();
        serveur.getPlayerList().broadcastSystemMessage(Component.literal("★ " + p.getGameProfile().getName() + " a fondé la faction " + nom + " !").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static String verrouFaction(ServerPlayer p) {
        return "🔒 Les factions se débloquent au niveau " + Metiers.NIVEAU_FACTION + " du joueur (tu es niveau " + Metiers.niveauJoueur(p) + "). Inventaire → NIVEAU";
    }

    private static Faction chefDe(ServerPlayer p) {
        Faction f = factionDe(p.getUUID());
        if (f == null) { dire(p, "Tu n'as pas de faction : /f create <nom>", ChatFormatting.RED); return null; }
        if (!f.chef.equals(p.getUUID())) { dire(p, "Seul le chef de la faction peut faire ça", ChatFormatting.RED); return null; }
        return f;
    }

    private static int inviter(ServerPlayer p, ServerPlayer cible) {
        Faction f = chefDe(p); if (f == null) return 0;
        if (factionDe(cible.getUUID()) != null) return dire(p, cible.getGameProfile().getName() + " a déjà une faction", ChatFormatting.RED);
        f.invites.add(cible.getUUID());
        MutableComponent lien = Component.literal("[Rejoindre " + f.nom + "]").withStyle(st -> st.withColor(0xC9A0FF).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f join " + f.nom)));
        cible.sendSystemMessage(Component.literal("★ " + p.getGameProfile().getName() + " t'invite dans la faction " + f.nom + "  ").withStyle(ChatFormatting.LIGHT_PURPLE).append(lien));
        return dire(p, "Invitation envoyée à " + cible.getGameProfile().getName(), ChatFormatting.GREEN);
    }

    private static int rejoindre(ServerPlayer p, String nom) {
        if (factionDe(p.getUUID()) != null) return dire(p, "Quitte d'abord ta faction : /f leave", ChatFormatting.RED);
        if (Metiers.niveauJoueur(p) < Metiers.NIVEAU_FACTION) return dire(p, verrouFaction(p), ChatFormatting.RED);
        Faction f = d().factions.get(nom.toLowerCase());
        if (f == null || !f.invites.remove(p.getUUID())) return dire(p, "Tu n'as pas d'invitation de cette faction", ChatFormatting.RED);
        f.membres.put(p.getUUID(), p.getGameProfile().getName()); d().setDirty();
        annoncerFaction(f, Component.literal("★ " + p.getGameProfile().getName() + " a rejoint la faction !").withStyle(ChatFormatting.LIGHT_PURPLE));
        return 1;
    }

    private static int quitter(ServerPlayer p) {
        Faction f = factionDe(p.getUUID());
        if (f == null) return dire(p, "Tu n'as pas de faction", ChatFormatting.RED);
        if (f.chef.equals(p.getUUID())) return dire(p, "Tu es le chef : /f disband pour dissoudre la faction", ChatFormatting.RED);
        f.membres.remove(p.getUUID()); d().setDirty();
        annoncerFaction(f, Component.literal(p.getGameProfile().getName() + " a quitté la faction").withStyle(ChatFormatting.GRAY));
        return dire(p, "Tu as quitté " + f.nom, ChatFormatting.GRAY);
    }

    private static int exclure(ServerPlayer p, String nom) {
        Faction f = chefDe(p); if (f == null) return 0;
        UUID u = null;
        for (Map.Entry<UUID, String> m : f.membres.entrySet()) if (m.getValue().equalsIgnoreCase(nom)) u = m.getKey();
        if (u == null || u.equals(f.chef)) return dire(p, "Ce joueur n'est pas un membre", ChatFormatting.RED);
        f.membres.remove(u); d().setDirty();
        ServerPlayer c = serveur.getPlayerList().getPlayer(u);
        if (c != null) dire(c, "Tu as été exclu de la faction " + f.nom, ChatFormatting.RED);
        return dire(p, nom + " a été exclu", ChatFormatting.GRAY);
    }

    private static int dissoudre(ServerPlayer p) {
        Faction f = chefDe(p); if (f == null) return 0;
        String k = f.nom.toLowerCase();
        d().claims.values().removeIf(k::equals);
        d().factions.remove(k);
        if (f.banque > 0) d().argent.merge(p.getUUID(), f.banque, Long::sum);   // la banque revient au chef
        d().setDirty();
        annoncerFaction(f, Component.literal("La faction " + f.nom + " a été dissoute").withStyle(ChatFormatting.RED));
        dire(p, "La faction " + f.nom + " a été dissoute", ChatFormatting.RED);
        envoyerSolde(p);
        return 1;
    }

    private static int claim(ServerPlayer p) {
        Faction f = factionDe(p.getUUID());
        if (f == null) return dire(p, "Tu n'as pas de faction : /f create <nom>", ChatFormatting.RED);
        String dim = p.level().dimension().location().toString();
        if (!MONDES_BASES.contains(dim)) return dire(p, "On ne peut acheter des chunks qu'à Pyrélune et Azuréa (mondes des bases)", ChatFormatting.RED);
        ChunkPos c = p.chunkPosition();
        if (Math.abs(c.x) <= 3 && Math.abs(c.z) <= 3) return dire(p, "Trop près du spawn de ce monde", ChatFormatting.RED);
        String k = cle(p.level(), c);
        if (d().claims.containsKey(k)) return dire(p, "Ce chunk appartient déjà à " + d().claims.get(k), ChatFormatting.RED);
        if (nbClaims(f) >= f.maxClaims()) return dire(p, "Limite atteinte (" + f.maxClaims() + " chunks) : recrute des membres !", ChatFormatting.RED);
        long prix = prixClaim(f);
        if (f.banque >= prix) f.banque -= prix;
        else if (!debiter(p.getUUID(), prix)) return dire(p, "Il faut " + format(prix) + " $ (banque de faction ou ton argent)", ChatFormatting.RED);
        d().claims.put(k, f.nom.toLowerCase()); d().setDirty();
        envoyerSolde(p);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, .5f, 1.3f);
        return dire(p, "✔ Chunk acheté pour " + format(prix) + " $ (" + nbClaims(f) + "/" + f.maxClaims() + ")", ChatFormatting.GREEN);
    }

    private static int unclaim(ServerPlayer p) {
        Faction f = factionDe(p.getUUID());
        if (f == null) return dire(p, "Tu n'as pas de faction", ChatFormatting.RED);
        String k = cle(p.level(), p.chunkPosition());
        if (!f.nom.toLowerCase().equals(d().claims.get(k))) return dire(p, "Ce chunk n'est pas à ta faction", ChatFormatting.RED);
        d().claims.remove(k); d().setDirty();
        return dire(p, "Chunk rendu", ChatFormatting.GRAY);
    }

    private static int deposer(ServerPlayer p, long m) {
        Faction f = factionDe(p.getUUID());
        if (f == null) return dire(p, "Tu n'as pas de faction", ChatFormatting.RED);
        if (!debiter(p.getUUID(), m)) return dire(p, "Pas assez d'argent", ChatFormatting.RED);
        f.banque += m; d().setDirty(); envoyerSolde(p);
        return dire(p, "✔ " + format(m) + " $ déposés dans la banque de " + f.nom, ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ demarrage
    static void init() {
        Metiers.CANAL.messageBuilder(Solde.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(Solde::ecrire).decoder(Solde::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.solde(m.argent()))).add();
        Metiers.CANAL.messageBuilder(Marche.class, 2, NetworkDirection.PLAY_TO_CLIENT).encoder(Marche::ecrire).decoder(Marche::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.marche(m))).add();
        Metiers.CANAL.messageBuilder(FactionVue.class, 3, NetworkDirection.PLAY_TO_CLIENT).encoder(FactionVue::ecrire).decoder(FactionVue::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.faction(m))).add();
        Metiers.CANAL.messageBuilder(Ouvrir.class, 4, NetworkDirection.PLAY_TO_CLIENT).encoder(Ouvrir::ecrire).decoder(Ouvrir::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.ouvrir(m.ecran()))).add();
        Metiers.CANAL.messageBuilder(Demande.class, 5, NetworkDirection.PLAY_TO_SERVER).encoder(Demande::ecrire).decoder(Demande::lire)
                .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p == null) return;
                    if (m.quoi() == 0) envoyerMarche(p); else if (m.quoi() == 1) envoyerFaction(p);
                    else if (m.quoi() == 2) envoyer(p, new Classement(Metiers.classement(p.server)));
                    else if (m.quoi() == 3) HotelVentes.envoyerVue(p); }).add();
        Metiers.CANAL.messageBuilder(Classement.class, 7, NetworkDirection.PLAY_TO_CLIENT).encoder(Classement::ecrire).decoder(Classement::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.classement(m))).add();
        HotelVentes.init();
        Metiers.CANAL.messageBuilder(Transaction.class, 6, NetworkDirection.PLAY_TO_SERVER).encoder(Transaction::ecrire).decoder(Transaction::lire)
                .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p != null) transaction(p, m); }).add();

        MinecraftForge.EVENT_BUS.addListener(Economie::commandes);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStartedEvent e) -> serveur = e.getServer());
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent e) -> serveur = null);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (serveur != null && e.getEntity() instanceof ServerPlayer p) envoyerSolde(p); });
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> { if (serveur != null && e.getPlayer() instanceof ServerPlayer p && e.getLevel() instanceof Level l && interdit(p, l, e.getPos())) e.setCanceled(true); });
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> { if (serveur != null && e.getEntity() instanceof ServerPlayer p && e.getLevel() instanceof Level l && interdit(p, l, e.getPos())) e.setCanceled(true); });
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> { if (serveur != null && e.getEntity() instanceof ServerPlayer p && interdit(p, e.getLevel(), e.getPos())) e.setCanceled(true); });
        MinecraftForge.EVENT_BUS.addListener((ExplosionEvent.Detonate e) -> { if (serveur != null && !e.getLevel().isClientSide) e.getAffectedBlocks().removeIf(pos -> proprietaire(e.getLevel(), pos) != null); });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END || serveur == null) return;
            int t = e.getServer().getTickCount();
            // les cours reviennent doucement vers leur prix de base (toutes les 5 min) ; reference de tendance chaque heure
            if (t % 6000 == 0) { for (double[] c : d().marche.values()) c[0] += (1 - c[0]) * .05; d().setDirty(); }
            if (t % 72000 == 0) { for (double[] c : d().marche.values()) c[1] = c[0]; d().setDirty(); }
        });
    }

    private Economie() {}
}
