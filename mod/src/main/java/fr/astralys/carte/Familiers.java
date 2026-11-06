package fr.astralys.carte;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Pets d'Astralys : un par metier, oeuf achete au niveau 50 du metier, eclos dans l'incubateur astral pose sur sa construction.
 * Le pet vole a cote de son maitre et lui donne ses avantages... et ses inconvenients.
 */
final class Familiers {
    static final String[] IDS = {"golem_cristal", "bourdon_dore", "faucon_spectral", "fee_ether"};
    static final String[] NOMS = {"Golem de cristal", "Bourdon doré", "Faucon spectral", "Fée d'éther"};
    static final String[] OEUFS = {"oeuf_golem", "oeuf_bourdon", "oeuf_faucon", "oeuf_fee"};
    static final int NIVEAU = 50;
    static final long PRIX_OEUF = 250_000;
    static final String[][] AVANTAGES = {
            {"Célérité II en permanence", "Vision nocturne sous la couche 50", "+25 % d'XP de Mineur"},
            {"Fait pousser les cultures autour de toi", "+25 % d'XP de Farmeur"},
            {"Force I", "Frappe ta cible : 3 dégâts / 2 s", "+25 % d'XP de Hunter"},
            {"Régénération I", "Chute lente quand tu tombes", "+25 % d'XP d'Alchimiste"}};
    static final String[][] INCONVENIENTS = {
            {"−2 cœurs de vie max", "Lenteur dans l'eau (cristal lourd)"},
            {"Tu as faim plus vite", "Inactif dans le Nether et l'End"},
            {"Surbrillance : visible à travers les murs", "−1 cœur de vie max"},
            {"Faiblesse I", "Tu as faim plus vite"}};
    /** Construction d'eclosion : 3x3 de SOCLE sous l'incubateur, et un COIN aux quatre coins, au niveau de l'incubateur. */
    static final String[] SOCLE = {"minecraft:iron_block", "minecraft:hay_block", "minecraft:bone_block", "minecraft:amethyst_block"};
    static final String[] COIN = {"astralyscarte:cristal_astral", "minecraft:composter", "minecraft:soul_lantern", "minecraft:brewing_stand"};
    static final String[] CONSTRUCTION = {"3×3 blocs de fer sous l'incubateur + 4 cristaux astraux aux coins",
            "3×3 bottes de foin sous l'incubateur + 4 composteurs aux coins", "3×3 blocs d'os sous l'incubateur + 4 lanternes des âmes aux coins",
            "3×3 blocs d'améthyste sous l'incubateur + 4 alambics aux coins"};
    /** Niveau du metier pour monter sur son pet, puis 4 passifs debloques aux niveaux 70, 80, 90 et 100. */
    static final int NIVEAU_MONTURE = 60;
    static final int[] NIVEAUX_PASSIFS = {70, 80, 90, 100};
    static final String[][] PASSIFS = {
            {"Résistance I", "10 % de minerais en double", "Plus de malus de vie", "Célérité III"},
            {"Fait pousser dans un rayon de 5", "Saturation toutes les 30 s", "15 % de récoltes en double", "Actif dans le Nether et l'End"},
            {"Vision nocturne", "Frappe ta cible : 6 dégâts", "Plus de surbrillance", "Force II"},
            {"Résistance au feu", "Soigne les joueurs à 6 blocs", "Plus de faiblesse", "Régénération II + Absorption"}};
    /** Vitesse en vol quand on le monte : golem lourd, faucon le plus rapide. */
    static final float[] VITESSE = {.25f, .35f, .5f, .4f};
    /** Taille du modele quand on le monte, et hauteur de la selle (en blocs) : le dos du corps agrandi. */
    static final float[] TAILLE_MONTURE = {2.2f, 3.2f, 3.4f, 2.6f};
    static final double[] SELLE = {2.0, 1.7, 1.9, 2.2};
    private static final UUID SANTE = UUID.fromString("6a7c9e1d-5b0f-4c3a-9a51-2f1e7d4b8c60");

    static Block bloc(String id) { return ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(id)); }

    static int oeuf(ItemStack s) {
        for (int i = 0; i < OEUFS.length; i++) if (s.getItem() == ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(AstralysCarte.MODID, OEUFS[i]))) return i;
        return -1;
    }

    static boolean construction(Level l, BlockPos inc, int t) {
        Block socle = bloc(SOCLE[t]), coin = bloc(COIN[t]);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) if (!l.getBlockState(inc.offset(x, -1, z)).is(socle)) return false;
        for (int x = -1; x <= 1; x += 2)
            for (int z = -1; z <= 1; z += 2) if (!l.getBlockState(inc.offset(x, 0, z)).is(coin)) return false;
        return true;
    }

    // ------------------------------------------------------------------ donnees du joueur (gardees apres la mort)
    private static CompoundTag donnees(Player j) {
        CompoundTag p = j.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        j.getPersistentData().put(Player.PERSISTED_NBT_TAG, p);
        CompoundTag q = p.getCompound("astralys_pets");
        if (!q.contains("a")) q.putInt("a", -1);
        p.put("astralys_pets", q);
        return q;
    }

    static int debloques(Player j) { return donnees(j).getInt("d"); }
    static int actif(Player j) { return donnees(j).getInt("a"); }
    static boolean possede(Player j, int t) { return (debloques(j) & (1 << t)) != 0; }

    static void debloquer(ServerPlayer j, int t) {
        CompoundTag d = donnees(j);
        d.putInt("d", d.getInt("d") | (1 << t)); d.putInt("a", t);
        j.sendSystemMessage(Component.literal("🥚 Ton œuf a éclos : " + NOMS[t] + " est maintenant ton pet ! (inventaire → PETS)")
                .withStyle(st -> st.withColor(Metiers.COULEURS[t]).withBold(true)));
        j.serverLevel().playSound(null, j.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1, 1.2f);
        invoquer(j);
        envoyer(j);
    }

    /** Bonus d'XP de metier quand le pet du meme metier est sorti. */
    static double bonusXp(ServerPlayer p, int metier) { return actif(p) == metier ? 1.25 : 1; }

    // ------------------------------------------------------------------ paquets
    record Vue(int debloques, int actif) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(debloques); b.writeVarInt(actif); }
        static Vue lire(FriendlyByteBuf b) { return new Vue(b.readVarInt(), b.readVarInt()); }
    }

    /** Nombre de passifs debloques par le niveau du metier du pet (0 a 4). */
    static int passifs(int niveau) { int n = 0; for (int v : NIVEAUX_PASSIFS) if (niveau >= v) n++; return n; }

    /** 0 demander, 1 acheter l'oeuf, 2 sortir le pet, 3 le ranger, 4 monter / descendre. */
    record Action(int action, int type) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(action); b.writeVarInt(type); }
        static Action lire(FriendlyByteBuf b) { return new Action(b.readVarInt(), b.readVarInt()); }
    }

    static void init() {
        Metiers.CANAL.messageBuilder(Vue.class, 14, NetworkDirection.PLAY_TO_CLIENT).encoder(Vue::ecrire).decoder(Vue::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PetsScreen.vue = m)).add();
        Metiers.CANAL.messageBuilder(Action.class, 15, NetworkDirection.PLAY_TO_SERVER).encoder(Action::ecrire).decoder(Action::lire)
                .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p != null) action(p, m); }).add();
        MinecraftForge.EVENT_BUS.addListener(Familiers::tick);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) envoyer(p); });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ranger(e.getEntity().getUUID()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, Familiers::casse);
    }

    static void envoyer(ServerPlayer p) { Economie.envoyer(p, new Vue(debloques(p), actif(p))); }

    private static void action(ServerPlayer p, Action a) {
        int t = a.type();
        if (a.action() != 0 && (t < 0 || t >= IDS.length)) return;
        if (a.action() == 1) {
            if (Metiers.niveau(p, t) < NIVEAU) dire(p, "Il faut le niveau " + NIVEAU + " en " + Metiers.NOMS[t] + " pour cet œuf.", ChatFormatting.RED);
            else if (possede(p, t)) dire(p, "Tu as déjà ce pet.", ChatFormatting.YELLOW);
            else if (!Economie.debiter(p.getUUID(), PRIX_OEUF)) dire(p, "Il te faut " + Economie.format(PRIX_OEUF) + " $ pour l'œuf.", ChatFormatting.RED);
            else {
                ItemStack o = Metiers.pile(AstralysCarte.MODID + ":" + OEUFS[t]);
                if (!p.getInventory().add(o.copy())) p.drop(o.copy(), false);
                Economie.envoyerSolde(p);
                dire(p, "🥚 Œuf obtenu ! Pose l'incubateur astral sur sa construction : " + CONSTRUCTION[t], ChatFormatting.GREEN);
            }
        } else if (a.action() == 2 && possede(p, t)) { donnees(p).putInt("a", t); invoquer(p); }
        else if (a.action() == 3) { donnees(p).putInt("a", -1); ranger(p.getUUID()); appliquer(p, -1); }
        else if (a.action() == 4) monter(p, t);
        envoyer(p);
    }

    private static void monter(ServerPlayer p, int t) {
        Pet pet = VIVANTS.get(p.getUUID());
        if (actif(p) != t || pet == null || pet.isRemoved()) { dire(p, "Fais d'abord venir ton pet (inventaire → PETS).", ChatFormatting.YELLOW); return; }
        if (p.getVehicle() == pet) { p.stopRiding(); return; }
        if (Metiers.niveau(p, t) < NIVEAU_MONTURE) { dire(p, "Il faut le niveau " + NIVEAU_MONTURE + " en " + Metiers.NOMS[t] + " pour monter sur ton pet.", ChatFormatting.RED); return; }
        if (p.isPassenger()) p.stopRiding();
        pet.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        p.startRiding(pet, true);
        dire(p, "✦ En selle ! Regarde où tu veux aller et avance • accroupis-toi pour descendre.", ChatFormatting.AQUA);
    }

    /** Passifs de double recolte : minerais pour le golem (niv. 80), cultures mures pour le bourdon (niv. 90). */
    private static void casse(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getPlayer() instanceof ServerPlayer j) || j.isCreative() || !(e.getLevel() instanceof ServerLevel l)) return;
        int t = actif(j);
        BlockState s = e.getState();
        ItemStack outil = j.getMainHandItem();
        boolean minerai = t == 0 && s.is(Tags.Blocks.ORES) && passifs(Metiers.niveau(j, 0)) >= 2 && l.random.nextFloat() < .10f
                && j.hasCorrectToolForDrops(s) && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, outil) == 0;
        boolean culture = t == 1 && s.getBlock() instanceof CropBlock c && c.isMaxAge(s) && passifs(Metiers.niveau(j, 1)) >= 3 && l.random.nextFloat() < .15f;
        if (!minerai && !culture) return;
        Block.dropResources(s, l, e.getPos(), l.getBlockEntity(e.getPos()), j, outil);
        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getPos().getX() + .5, e.getPos().getY() + .5, e.getPos().getZ() + .5, 6, .3, .3, .3, 0);
    }

    private static void dire(ServerPlayer p, String m, ChatFormatting c) { p.sendSystemMessage(Component.literal(m).withStyle(c)); }

    // ------------------------------------------------------------------ pets presents dans le monde
    private static final Map<UUID, Pet> VIVANTS = new HashMap<>();

    static void ranger(UUID maitre) { Pet p = VIVANTS.remove(maitre); if (p != null) p.discard(); }

    static void invoquer(ServerPlayer j) {
        ranger(j.getUUID());
        int t = actif(j);
        if (t < 0) return;
        Pet p = Objets.PET.get().create(j.serverLevel());
        if (p == null) return;
        p.maitre = j.getUUID(); p.sorte(t);
        p.moveTo(j.getX(), j.getY() + 1.6, j.getZ(), j.getYRot(), 0);
        j.serverLevel().addFreshEntity(p);
        VIVANTS.put(j.getUUID(), p);
        j.serverLevel().sendParticles(ParticleTypes.END_ROD, j.getX(), j.getY() + 1.6, j.getZ(), 20, .4, .4, .4, .02);
    }

    private static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer j) || j.tickCount % 40 != 0) return;
        int t = actif(j);
        Pet p = VIVANTS.get(j.getUUID());
        if (t >= 0 && j.isAlive() && (p == null || p.isRemoved() || p.level() != j.level())) invoquer(j);   // connexion, mort, changement de monde
        appliquer(j, t);
    }

    private static MobEffectInstance ef(MobEffect m, int amp, int duree) { return new MobEffectInstance(m, duree, amp, true, false, true); }

    /** Avantages et inconvenients du pet sorti (toutes les 2 s). */
    private static void appliquer(ServerPlayer j, int t) {
        int n = t < 0 ? 0 : passifs(Metiers.niveau(j, t));
        var vie = j.getAttribute(Attributes.MAX_HEALTH);
        double malus = t == 0 ? (n >= 3 ? 0 : -4) : t == 2 ? -2 : 0;
        var actuel = vie == null ? null : vie.getModifier(SANTE);
        if (vie != null && (actuel == null ? malus != 0 : actuel.getAmount() != malus)) {
            vie.removeModifier(SANTE);
            if (malus != 0) vie.addTransientModifier(new AttributeModifier(SANTE, "pet astralys", malus, AttributeModifier.Operation.ADDITION));
            if (j.getHealth() > j.getMaxHealth()) j.setHealth(j.getMaxHealth());
        }
        if (t < 0) return;
        ServerLevel l = j.serverLevel();
        switch (t) {
            case 0 -> {
                j.addEffect(ef(MobEffects.DIG_SPEED, n >= 4 ? 2 : 1, 80));
                if (n >= 1) j.addEffect(ef(MobEffects.DAMAGE_RESISTANCE, 0, 80));
                if (j.getY() < 50) j.addEffect(ef(MobEffects.NIGHT_VISION, 0, 400));
                if (j.isInWater()) j.addEffect(ef(MobEffects.MOVEMENT_SLOWDOWN, 0, 60));
            }
            case 1 -> {
                if (n < 4 && l.dimension() != Level.OVERWORLD && !l.dimension().location().getNamespace().equals("astralys")) return;
                if (n >= 2 && j.tickCount % 600 < 40) j.addEffect(ef(MobEffects.SATURATION, 0, 1));
                int r = n >= 1 ? 5 : 3;
                BlockPos c = j.blockPosition();
                for (BlockPos b : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, 1, r))) {
                    BlockState s = l.getBlockState(b);
                    if (s.getBlock() instanceof BonemealableBlock && s.is(net.minecraft.tags.BlockTags.CROPS) && l.random.nextFloat() < .35f) {
                        s.randomTick(l, b.immutable(), l.random);
                        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.getX() + .5, b.getY() + .6, b.getZ() + .5, 1, .2, .1, .2, 0);
                    }
                }
                j.causeFoodExhaustion(.5f);
            }
            case 2 -> {
                j.addEffect(ef(MobEffects.DAMAGE_BOOST, n >= 4 ? 1 : 0, 80));
                if (n >= 1) j.addEffect(ef(MobEffects.NIGHT_VISION, 0, 400));
                if (n < 3) j.addEffect(ef(MobEffects.GLOWING, 0, 80));
                LivingEntity cible = j.getLastHurtMob();
                if (cible != null && cible.isAlive() && cible.distanceTo(j) < 16 && j.tickCount - j.getLastHurtMobTimestamp() < 200) {
                    cible.hurt(j.damageSources().playerAttack(j), n >= 2 ? 6 : 3);
                    l.sendParticles(ParticleTypes.SWEEP_ATTACK, cible.getX(), cible.getY() + cible.getBbHeight() * .6, cible.getZ(), 1, 0, 0, 0, 0);
                }
            }
            default -> {
                j.addEffect(ef(MobEffects.REGENERATION, n >= 4 ? 1 : 0, 80));
                if (n >= 4) j.addEffect(ef(MobEffects.ABSORPTION, 0, 400));
                if (n >= 1) j.addEffect(ef(MobEffects.FIRE_RESISTANCE, 0, 80));
                if (n >= 2) for (Player o : l.getEntitiesOfClass(Player.class, j.getBoundingBox().inflate(6), x -> x != j))
                    o.addEffect(ef(MobEffects.REGENERATION, 0, 80));
                if (n < 3) j.addEffect(ef(MobEffects.WEAKNESS, 0, 80));
                if (j.fallDistance > 3) j.addEffect(ef(MobEffects.SLOW_FALLING, 0, 60));
                j.causeFoodExhaustion(.4f);
            }
        }
    }

    // ------------------------------------------------------------------ l'entite : vole pres de l'epaule de son maitre
    public static final class Pet extends Mob {
        private static final EntityDataAccessor<Integer> SORTE = SynchedEntityData.defineId(Pet.class, EntityDataSerializers.INT);
        UUID maitre;

        public Pet(EntityType<? extends Pet> t, Level l) {
            super(t, l);
            setNoGravity(true); noPhysics = true; setInvulnerable(true); setSilent(true);
        }

        @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SORTE, 0); }
        int sorte() { return Math.floorMod(entityData.get(SORTE), IDS.length); }
        void sorte(int t) { entityData.set(SORTE, t); setCustomName(Component.literal(NOMS[t]).withStyle(st -> st.withColor(Metiers.COULEURS[t]))); }

        @Override
        public void tick() {
            noPhysics = !isVehicle();   // monte : il heurte les blocs ; sinon il traverse tout pour suivre son maitre
            super.tick();
            if (level().isClientSide || maitre == null) return;
            ServerPlayer j = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(maitre);
            if (j == null || j.level() != level() || actif(j) != sorte()) { discard(); return; }
            if (isVehicle()) return;   // le client du cavalier le pilote (comme un cheval)
            double a = Math.toRadians(j.getYRot());
            Vec3 avant = new Vec3(-Math.sin(a), 0, Math.cos(a)), droite = new Vec3(-Math.cos(a), 0, -Math.sin(a));
            Vec3 cible = j.position().add(droite.scale(1.1)).add(avant.scale(-.8)).add(0, 1.45 + Math.sin(tickCount * .12) * .12, 0);
            Vec3 ici = position();
            Vec3 pos = ici.distanceToSqr(cible) > 100 ? cible : ici.add(cible.subtract(ici).scale(.3));
            setPos(pos.x, pos.y, pos.z);
            setYRot(j.getYRot()); yBodyRot = j.getYRot(); yHeadRot = j.getYHeadRot();
            if (tickCount % 8 == 0) ((ServerLevel) level()).sendParticles(sorte() == 0 ? ParticleTypes.END_ROD : sorte() == 1 ? ParticleTypes.WAX_ON
                    : sorte() == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ENCHANT, getX(), getY() + .2, getZ(), 1, .1, .1, .1, 0);
        }

        // ---- monture : vole la ou regarde le cavalier
        @Override public LivingEntity getControllingPassenger() { return getFirstPassenger() instanceof Player p ? p : null; }
        @Override protected void tickRidden(Player p, Vec3 v) {
            super.tickRidden(p, v);
            setRot(p.getYRot(), p.getXRot() * .5f);
            Vec3 d = getDeltaMovement(); setDeltaMovement(d.x, d.y * .9, d.z);   // sans gravite, la montee/descente s'emballerait
            yRotO = yBodyRot = yHeadRot = getYRot();
        }
        @Override protected Vec3 getRiddenInput(Player p, Vec3 v) {
            double monte = -Math.sin(Math.toRadians(p.getXRot())) * p.zza;
            return new Vec3(p.xxa * .5, monte, p.zza);
        }
        @Override protected float getRiddenSpeed(Player p) { return VITESSE[sorte()]; }
        @Override public double getPassengersRidingOffset() { return SELLE[sorte()]; }
        @Override public EntityDimensions getDimensions(Pose pose) { return isVehicle() ? EntityDimensions.scalable(1.3f, (float) SELLE[sorte()] + .4f) : super.getDimensions(pose); }
        @Override protected void addPassenger(net.minecraft.world.entity.Entity e) { super.addPassenger(e); refreshDimensions(); }
        @Override protected void removePassenger(net.minecraft.world.entity.Entity e) {
            super.removePassenger(e);
            refreshDimensions();
            if (!level().isClientSide && e instanceof ServerPlayer j) j.addEffect(ef(MobEffects.SLOW_FALLING, 0, 120));   // descendre en plein vol
        }

        @Override public boolean isPickable() { return false; }
        @Override public boolean isPushable() { return false; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean removeWhenFarAway(double d) { return false; }
        @Override public boolean isAttackable() { return false; }
        @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    }

    private Familiers() {}
}
