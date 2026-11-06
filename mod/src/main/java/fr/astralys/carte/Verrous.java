package fr.astralys.carte;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

/** Objets reserves a un niveau de metier : on peut les avoir, mais pas s'en servir (ni porter l'armure) avant le niveau. */
final class Verrous {
    record Verrou(int metier, int niveau) {}

    static final Map<String, Verrou> LISTE = new HashMap<>();
    static {
        // Mineur 0, Farmeur 1, Hunter 2, Alchimiste 3
        String[][] l = {{"broyeur_astral", "0", "10"}, {"marteau_forgeron", "0", "40"}, {"foreuse_celeste", "0", "70"},
                {"arrosoir_enchante", "1", "20"}, {"baguette_soins", "1", "30"},
                {"epee_runique", "2", "20"}, {"piege_runique", "2", "30"}, {"hache_titan", "2", "40"},
                {"baton_braise", "3", "10"}, {"sceptre_givre", "3", "20"}, {"baton_foudre", "3", "30"}, {"orbe_transfert", "3", "40"}, {"tome_tempete", "3", "60"}};
        for (String[] v : l) LISTE.put(v[0], new Verrou(Integer.parseInt(v[1]), Integer.parseInt(v[2])));
        // armures d'ensemble : Titan = Mineur 50, Ombre = Hunter 50, Celeste = Alchimiste 50
        String[][] a = {{"titan", "0"}, {"ombre", "2"}, {"celeste", "3"}};
        for (String[] v : a) for (String p : new String[] {"casque_", "plastron_", "jambieres_", "bottes_"}) LISTE.put(p + v[0], new Verrou(Integer.parseInt(v[1]), 50));
    }

    /** XP des metiers recue par le joueur (rempli par ClientMetiers), pour griser les objets dans l'infobulle. */
    static long[] xpClient;

    static Verrou de(ItemStack s) {
        if (s.isEmpty()) return null;
        var id = ForgeRegistries.ITEMS.getKey(s.getItem());
        return id == null || !id.getNamespace().equals(AstralysCarte.MODID) ? null : LISTE.get(id.getPath());
    }

    /** Niveau du joueur dans un metier : cote serveur depuis les donnees, cote joueur depuis ce que le serveur a envoye. */
    static int niveau(Player j, int m) {
        if (j instanceof ServerPlayer sp) return Metiers.niveau(sp, m);
        long[] x = xpClient;
        return x == null ? 0 : Metiers.niveau(x[m]);
    }

    static boolean bloque(Player j, ItemStack s) {
        Verrou v = de(s);
        return v != null && !j.isCreative() && niveau(j, v.metier()) < v.niveau();
    }

    private static void dire(Player j, ItemStack s) {
        Verrou v = de(s);
        if (j instanceof ServerPlayer sp && v != null)
            sp.displayClientMessage(Component.literal("🔒 " + s.getHoverName().getString() + " : il faut " + Metiers.NOMS[v.metier()] + " niveau " + v.niveau()
                    + " (tu es niveau " + niveau(j, v.metier()) + ")").withStyle(ChatFormatting.RED), true);
    }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (bloque(e.getEntity(), e.getItemStack())) { e.setCanceled(true); e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL); dire(e.getEntity(), e.getItemStack()); }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (bloque(e.getEntity(), e.getItemStack())) { e.setUseItem(Event.Result.DENY); dire(e.getEntity(), e.getItemStack()); }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.BreakSpeed e) -> {
            if (bloque(e.getEntity(), e.getEntity().getMainHandItem())) { e.setCanceled(true); dire(e.getEntity(), e.getEntity().getMainHandItem()); }
        });
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (bloque(e.getEntity(), e.getEntity().getMainHandItem())) { e.setCanceled(true); dire(e.getEntity(), e.getEntity().getMainHandItem()); }
        });
        // armure verrouillee : elle retourne dans l'inventaire
        MinecraftForge.EVENT_BUS.addListener((LivingEquipmentChangeEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer j) || e.getSlot().getType() != EquipmentSlot.Type.ARMOR || !bloque(j, e.getTo())) return;
            ItemStack s = e.getTo().copy();
            j.setItemSlot(e.getSlot(), ItemStack.EMPTY);
            if (!j.getInventory().add(s)) j.drop(s, false);
            dire(j, s);
        });
        MinecraftForge.EVENT_BUS.addListener(Verrous::foreuse);
    }

    // ------------------------------------------------------------------ foreuse celeste : creuse en 3x3 (sauf accroupi)
    private static boolean enCours;

    private static void foreuse(BlockEvent.BreakEvent e) {
        if (enCours || !(e.getPlayer() instanceof ServerPlayer j) || j.isShiftKeyDown()) return;
        ItemStack outil = j.getMainHandItem();
        var id = ForgeRegistries.ITEMS.getKey(outil.getItem());
        if (id == null || !id.getPath().equals("foreuse_celeste") || !id.getNamespace().equals(AstralysCarte.MODID) || bloque(j, outil)) return;
        HitResult h = j.pick(6, 1, false);
        Direction.Axis axe = h instanceof BlockHitResult b ? b.getDirection().getAxis() : Direction.Axis.Y;
        BlockPos c = e.getPos();
        enCours = true;
        try {
            for (int u = -1; u <= 1; u++)
                for (int v = -1; v <= 1; v++) {
                    if (u == 0 && v == 0) continue;
                    BlockPos p = switch (axe) { case X -> c.offset(0, u, v); case Y -> c.offset(u, 0, v); default -> c.offset(u, v, 0); };
                    BlockState s = j.level().getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(j.level(), p) < 0 || !outil.isCorrectToolForDrops(s)) continue;
                    j.gameMode.destroyBlock(p);   // passe par les protections de faction comme un coup normal
                    if (outil.isEmpty()) return;
                }
        } finally { enCours = false; }
    }

    // ------------------------------------------------------------------ arrosoir enchante : fait pousser les cultures en 5x5
    static boolean arroser(ServerPlayer j) {
        BlockPos c = j.blockPosition();
        var l = j.serverLevel();
        int n = 0;
        for (BlockPos b : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
            BlockState s = l.getBlockState(b);
            if (s.getBlock() instanceof BonemealableBlock && s.is(BlockTags.CROPS)) {
                for (int k = 0; k < 3; k++) l.getBlockState(b).randomTick(l, b.immutable(), l.random);
                l.sendParticles(ParticleTypes.SPLASH, b.getX() + .5, b.getY() + .8, b.getZ() + .5, 6, .3, .1, .3, 0);
                n++;
            }
        }
        if (n == 0) j.displayClientMessage(Component.literal("Aucune culture autour de toi.").withStyle(ChatFormatting.GRAY), true);
        return n > 0;
    }

    private Verrous() {}
}
