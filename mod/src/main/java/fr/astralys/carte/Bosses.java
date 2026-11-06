package fr.astralys.carte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Gros boss qui apparaissent sur la carte : annonce dans le tchat avec un lien [COMBATTRE], barre de vie, recompenses aux participants. */
final class Bosses {
    record Type(String id, String nom, EntityType<? extends Mob> entite, double vie, double degats, double armure, BossEvent.BossBarColor couleur,
                EntityType<? extends Mob> sbire, String materiau, ItemStack arme) {}

    static final List<Type> TYPES = List.of(
            new Type("ombre", "Seigneur de l'Ombre", EntityType.WITHER_SKELETON, 700, 14, 12, BossEvent.BossBarColor.PURPLE, EntityType.WITHER_SKELETON, "essence_ombre", new ItemStack(Items.NETHERITE_SWORD)),
            new Type("titan", "Titan de pierre", EntityType.RAVAGER, 1000, 16, 16, BossEvent.BossBarColor.RED, EntityType.VINDICATOR, "coeur_titan", ItemStack.EMPTY),
            new Type("archimage", "Archimage déchu", EntityType.EVOKER, 600, 8, 10, BossEvent.BossBarColor.BLUE, EntityType.WITCH, "plume_celeste", ItemStack.EMPTY));

    private static final String TAG = "astralys_boss";
    private static final String[] MONDES = {"astralys:sylvaria", "astralys:pyrelune", "astralys:azurea"};
    private static final String[] NOMS_MONDES = {"Sylvaria", "Pyrélune", "Azuréa"};
    private static final int INTERVALLE = 2 * 60 * 60, PREMIER = 30 * 60, DUREE = 30 * 60;   // secondes

    private static Type type;
    private static UUID id;
    private static ResourceKey<Level> monde;
    private static BlockPos lieu;
    private static ServerBossEvent barre;
    private static int secondes, restant;
    private static final Map<UUID, Float> degats = new HashMap<>();

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(Bosses::tick);
        MinecraftForge.EVENT_BUS.addListener(Bosses::blesse);
        MinecraftForge.EVENT_BUS.addListener(Bosses::mort);
        MinecraftForge.EVENT_BUS.addListener(Bosses::commandes);
        // un boss d'avant un redemarrage n'est plus suivi : on le retire quand son chunk se recharge
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide && e.getEntity().getTags().contains(TAG) && !e.getEntity().getUUID().equals(id)) e.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppingEvent e) -> fin());
    }

    private static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer s = e.getServer();
        secondes++;
        if (id == null) {
            if (secondes >= PREMIER && (secondes - PREMIER) % INTERVALLE == 0 && !s.getPlayerList().getPlayers().isEmpty())
                lancer(s, TYPES.get(s.overworld().getRandom().nextInt(TYPES.size())));
            return;
        }
        ServerLevel l = s.getLevel(monde);
        Entity b = l == null ? null : l.getEntity(id);
        if (--restant <= 0) {
            if (b != null) b.discard();
            s.getPlayerList().broadcastSystemMessage(Component.literal("☠ " + type.nom() + " a disparu sans être vaincu…").withStyle(ChatFormatting.GRAY), false);
            fin(); return;
        }
        if (b instanceof Mob m) {
            barre.setProgress(m.getHealth() / m.getMaxHealth());
            lieu = m.blockPosition();
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                boolean pres = p.level() == l && p.distanceToSqr(m) < 90 * 90;
                if (pres) barre.addPlayer(p); else barre.removePlayer(p);
            }
            if (m.getTarget() != null && restant % 20 == 0) sbires(l, m);
        }
    }

    static boolean lancer(MinecraftServer s, Type t) {
        if (id != null) return false;
        var rnd = s.overworld().getRandom();
        int k = rnd.nextInt(MONDES.length);
        ServerLevel l = s.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MONDES[k])));
        String nomMonde = NOMS_MONDES[k];
        if (l == null) { l = s.overworld(); nomMonde = "le monde"; }
        BlockPos pos = null;
        for (int essai = 0; essai < 12 && pos == null; essai++) {
            int x = (rnd.nextBoolean() ? 1 : -1) * (350 + rnd.nextInt(900)), z = (rnd.nextBoolean() ? 1 : -1) * (350 + rnd.nextInt(900));
            pos = sol(l, x, z);
        }
        if (pos == null) return false;
        Mob m = t.entite().create(l);
        if (m == null) return false;
        m.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, rnd.nextFloat() * 360, 0);
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        attr(m, Attributes.MAX_HEALTH, t.vie()); attr(m, Attributes.ATTACK_DAMAGE, t.degats()); attr(m, Attributes.ARMOR, t.armure());
        attr(m, Attributes.KNOCKBACK_RESISTANCE, 1); attr(m, Attributes.FOLLOW_RANGE, 48);
        m.setHealth(m.getMaxHealth());
        if (!t.arme().isEmpty()) { m.setItemSlot(EquipmentSlot.MAINHAND, t.arme().copy()); m.setDropChance(EquipmentSlot.MAINHAND, 0); }
        m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false));
        m.setCustomName(Component.literal("☠ " + t.nom()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        m.setCustomNameVisible(true);
        m.setPersistenceRequired();
        m.setGlowingTag(true);
        m.addTag(TAG);
        type = t; id = m.getUUID(); monde = l.dimension(); lieu = pos; restant = DUREE; degats.clear();
        l.addFreshEntity(m);
        barre = new ServerBossEvent(Component.literal("☠ " + t.nom()).withStyle(ChatFormatting.BOLD), t.couleur(), BossEvent.BossBarOverlay.NOTCHED_10);
        barre.setDarkenScreen(true);
        String ou = nomMonde;
        Component lien = Component.literal(" [⚔ COMBATTRE]").withStyle(st -> st.withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/boss rejoindre"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Clique pour être téléporté près du boss"))));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_PURPLE));
            p.sendSystemMessage(Component.literal("☠ BOSS : " + t.nom() + " est apparu à " + ou + " (" + pos.getX() + ", " + pos.getZ() + ") !").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(lien));
            p.sendSystemMessage(Component.literal("   Récompenses spéciales pour tous ceux qui le blessent. Il repart dans 30 min.").withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_PURPLE));
            p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, .6f, 1);
        }
        return true;
    }

    private static void attr(Mob m, Attribute a, double v) { var i = m.getAttribute(a); if (i != null) i.setBaseValue(v); }

    /** Sol sur lequel poser le boss (ou null si eau, lave ou rien de solide). */
    private static BlockPos sol(ServerLevel l, int x, int z) {
        int y;
        if (!l.dimensionType().hasCeiling()) y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        else {
            y = -1;
            for (int yy = l.getMinBuildHeight() + l.dimensionType().logicalHeight() - 8; yy > l.getMinBuildHeight() + 4; yy--)
                if (l.getBlockState(new BlockPos(x, yy, z)).isAir() && l.getBlockState(new BlockPos(x, yy + 1, z)).isAir() && l.getBlockState(new BlockPos(x, yy - 1, z)).isSolid()) { y = yy; break; }
            if (y < 0) return null;
        }
        BlockPos p = new BlockPos(x, y, z);
        if (y <= l.getMinBuildHeight() + 1 || !l.getFluidState(p.below()).isEmpty() || !l.getBlockState(p.below()).isSolid()) return null;
        return p;
    }

    private static void sbires(ServerLevel l, Mob boss) {
        for (int i = 0; i < 2; i++) {
            Mob s = type.sbire().create(l);
            if (s == null) return;
            s.moveTo(boss.getX() + l.random.nextInt(7) - 3, boss.getY(), boss.getZ() + l.random.nextInt(7) - 3, 0, 0);
            s.finalizeSpawn(l, l.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
            s.setTarget(boss.getTarget());
            l.addFreshEntity(s);
        }
    }

    private static void blesse(LivingHurtEvent e) {
        if (id != null && e.getEntity().getUUID().equals(id) && e.getSource().getEntity() instanceof ServerPlayer p)
            degats.merge(p.getUUID(), Math.min(e.getAmount(), e.getEntity().getHealth()), Float::sum);
    }

    private static void mort(LivingDeathEvent e) {
        if (id == null || !e.getEntity().getUUID().equals(id)) return;
        MinecraftServer s = e.getEntity().getServer();
        if (s == null) return;
        float total = (float) type.vie();
        var rnd = e.getEntity().getRandom();
        UUID meilleur = degats.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        s.getPlayerList().broadcastSystemMessage(Component.literal("⚔ " + type.nom() + " est vaincu ! " + degats.size() + " héros ont participé.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        degats.forEach((u, d) -> {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            if (p == null || d < total * .02f) return;   // 2 % des PV minimum pour etre recompense
            donner(p, Metiers.pile("astralyscarte:" + type.materiau() + "*" + (1 + rnd.nextInt(3))));
            donner(p, Metiers.pile(u.equals(meilleur) ? "astralyscarte:coffre_legendaire" : "astralyscarte:coffre_epique"));
            if (rnd.nextFloat() < .1f) donner(p, Metiers.pile("astralyscarte:coffre_special"));
            long argent = 2000 + (long) (8000 * d / total);
            Economie.crediter(p, argent, "boss " + type.nom());
            p.sendSystemMessage(Component.literal("★ Récompense du boss : matériaux, coffre" + (u.equals(meilleur) ? " légendaire (meilleur combattant !)" : " épique") + " et " + Economie.format(argent) + " $").withStyle(ChatFormatting.LIGHT_PURPLE));
        });
        e.getEntity().level().playSound(null, e.getEntity().blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2, 1);
        fin();
    }

    private static void donner(ServerPlayer p, ItemStack s) { if (!s.isEmpty() && !p.getInventory().add(s)) p.drop(s, false); }

    private static void fin() {
        if (barre != null) barre.removeAllPlayers();
        barre = null; id = null; type = null; degats.clear();
    }

    private static void commandes(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("boss")
                .then(Commands.literal("rejoindre").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (id == null) { p.sendSystemMessage(Component.literal("Aucun boss en ce moment.").withStyle(ChatFormatting.GRAY)); return 0; }
                    ServerLevel l = p.server.getLevel(monde);
                    if (l == null) return 0;
                    BlockPos cible = null;
                    for (int r = 14; r <= 30 && cible == null; r += 4) cible = sol(l, lieu.getX() + r, lieu.getZ());
                    if (cible == null) cible = lieu.above(3);
                    p.teleportTo(l, cible.getX() + .5, cible.getY(), cible.getZ() + .5, 90, 0);
                    p.sendSystemMessage(Component.literal("⚔ Bonne chance contre " + type.nom() + " !").withStyle(ChatFormatting.RED));
                    return 1;
                }))
                .then(Commands.literal("lancer").requires(s -> s.hasPermission(2)).then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(TYPES.stream().map(Type::id), b))
                        .executes(c -> {
                            String t = StringArgumentType.getString(c, "type");
                            Type ty = TYPES.stream().filter(x -> x.id().equals(t)).findFirst().orElse(null);
                            if (ty == null || !lancer(c.getSource().getServer(), ty)) { c.getSource().sendFailure(Component.literal("Impossible (type inconnu ou boss déjà en cours).")); return 0; }
                            return 1;
                        }))));
    }

    private Bosses() {}
}
