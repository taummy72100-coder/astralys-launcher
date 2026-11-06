package fr.astralys.carte;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Objets speciaux d'Astralys : outils legendaires, objets de mage, armes, elixirs, armures d'ensemble, blocs, coffres ; et les PNJ. */
final class Objets {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AstralysCarte.MODID);
    static final DeferredRegister<Block> BLOCS = DeferredRegister.create(ForgeRegistries.BLOCKS, AstralysCarte.MODID);
    static final DeferredRegister<EntityType<?>> ENTITES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AstralysCarte.MODID);
    static final DeferredRegister<CreativeModeTab> ONGLETS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralysCarte.MODID);

    /** Matiere des outils : jamais reparable a l'enclume (recompense unique). */
    record Rang(int getUses, float getSpeed, float getAttackDamageBonus, int getLevel, int getEnchantmentValue) implements Tier {
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }

    private static Item.Properties p(Rarity r) { return new Item.Properties().stacksTo(1).rarity(r).fireResistant(); }

    private static RegistryObject<Item> objet(String id, Supplier<Item> f) { return ITEMS.register(id, f); }

    // ------------------------------------------------------------------ outils legendaires (paliers 50 et 100)
    static {
        objet("pioche_cristal", () -> new PickaxeItem(new Rang(2000, 10, 3, 3, 18), 1, -2.8f, p(Rarity.RARE)));
        objet("pioche_celeste", () -> new PickaxeItem(new Rang(4000, 14, 4, 4, 22), 1, -2.8f, p(Rarity.EPIC)) { @Override public boolean isFoil(ItemStack s) { return true; } });
        objet("houe_doree", () -> new HoeItem(new Rang(1800, 10, 0, 3, 22), -3, 0f, p(Rarity.RARE)));
        objet("faux_moissons", () -> new HoeItem(new Rang(3500, 14, 2, 4, 25), -2, -1f, p(Rarity.EPIC)) { @Override public boolean isFoil(ItemStack s) { return true; } });
        objet("dague_chasseur", () -> new SwordItem(new Rang(1900, 8, 3, 3, 18), 3, -2.0f, p(Rarity.RARE)));
        objet("lame_lunaire", () -> new SwordItem(new Rang(3600, 9, 5, 4, 22), 4, -2.2f, p(Rarity.EPIC)) { @Override public boolean isFoil(ItemStack s) { return true; } });
        objet("fiole_eternelle", () -> new Sort(p(Rarity.RARE), 60, false, SoundEvents.BREWING_STAND_BREW, j -> {
            j.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 1)); return true; }));
        objet("grimoire_astralys", () -> new Sort(p(Rarity.EPIC), 300, true, SoundEvents.AMETHYST_BLOCK_CHIME, j -> {
            j.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1)); j.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            j.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0)); return true; }));

        // ---------------------------------------------------------------- objets de mage (sorts avec temps de recharge)
        objet("baton_foudre", () -> new Sort(p(Rarity.EPIC), 15, true, SoundEvents.TRIDENT_THUNDER, Objets::foudre));
        objet("sceptre_givre", () -> new Sort(p(Rarity.EPIC), 20, false, SoundEvents.GLASS_BREAK, Objets::givre));
        objet("orbe_transfert", () -> new Sort(p(Rarity.RARE), 10, false, SoundEvents.ENDERMAN_TELEPORT, Objets::transfert));
        objet("baguette_soins", () -> new Sort(p(Rarity.RARE), 30, false, SoundEvents.AMETHYST_BLOCK_RESONATE, Objets::soins));
        objet("tome_tempete", () -> new Sort(p(Rarity.EPIC), 25, true, SoundEvents.ELYTRA_FLYING, Objets::tempete));
        objet("baton_braise", () -> new Sort(p(Rarity.RARE), 2, false, SoundEvents.BLAZE_SHOOT, Objets::braise));

        // ---------------------------------------------------------------- armes
        objet("marteau_forgeron", () -> new SwordItem(new Rang(3000, 6, 6, 4, 15), 5, -3.0f, p(Rarity.EPIC)));
        objet("epee_runique", () -> new SwordItem(new Rang(2600, 9, 5, 4, 22), 3, -2.2f, p(Rarity.EPIC)) { @Override public boolean isFoil(ItemStack s) { return true; } });
        objet("hache_titan", () -> new AxeItem(new Rang(3200, 10, 6, 4, 15), 7, -3.1f, p(Rarity.EPIC)));

        // ---------------------------------------------------------------- elixirs
        elixir("elixir_force", Rarity.UNCOMMON, new Object[] {MobEffects.DAMAGE_BOOST, 180, 1});
        elixir("elixir_titan", Rarity.RARE, new Object[] {MobEffects.DAMAGE_RESISTANCE, 120, 1}, new Object[] {MobEffects.ABSORPTION, 120, 2});
        elixir("elixir_ombre", Rarity.RARE, new Object[] {MobEffects.INVISIBILITY, 300, 0}, new Object[] {MobEffects.MOVEMENT_SPEED, 300, 0});
        elixir("elixir_celerite", Rarity.UNCOMMON, new Object[] {MobEffects.MOVEMENT_SPEED, 180, 1}, new Object[] {MobEffects.DIG_SPEED, 180, 1});
        elixir("elixir_mineur", Rarity.UNCOMMON, new Object[] {MobEffects.DIG_SPEED, 300, 2}, new Object[] {MobEffects.NIGHT_VISION, 300, 0});
        elixir("elixir_phenix", Rarity.RARE, new Object[] {MobEffects.FIRE_RESISTANCE, 480, 0}, new Object[] {MobEffects.REGENERATION, 60, 0});
        elixir("elixir_vie", Rarity.RARE, new Object[] {MobEffects.HEAL, 1, 2}, new Object[] {MobEffects.REGENERATION, 20, 1});

        // ---------------------------------------------------------------- materiaux de boss (pour les armures)
        for (String m : new String[] {"essence_ombre", "coeur_titan", "plume_celeste"})
            objet(m, () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()) { @Override public boolean isFoil(ItemStack s) { return true; } });

        // ---------------------------------------------------------------- armures d'ensemble
        for (Matiere m : Matiere.values()) {
            objet("casque_" + m.id, () -> new Armure(m, ArmorItem.Type.HELMET));
            objet("plastron_" + m.id, () -> new Armure(m, ArmorItem.Type.CHESTPLATE));
            objet("jambieres_" + m.id, () -> new Armure(m, ArmorItem.Type.LEGGINGS));
            objet("bottes_" + m.id, () -> new Armure(m, ArmorItem.Type.BOOTS));
        }

        // ---------------------------------------------------------------- coffres et cles
        for (int r = 0; r < Coffres.RARETES.length; r++) { int rr = r; objet("coffre_" + Coffres.RARETES[r], () -> new Coffres.CoffreItem(rr)); }
        for (int r = 1; r <= 3; r++) { int rr = r; objet("cle_" + Coffres.RARETES[r], () -> new Coffres.Cle(rr)); }
    }

    // ------------------------------------------------------------------ blocs
    static final RegistryObject<Block> BLOC_FANTOME = bloc("bloc_fantome", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
            .strength(1.5f, 6f).sound(SoundType.STONE).noCollission().noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)), Rarity.RARE);
    static final RegistryObject<Block> CRISTAL = bloc("cristal_astral", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(1f).sound(SoundType.AMETHYST).lightLevel(s -> 15)), Rarity.UNCOMMON);
    static final RegistryObject<Block> CAISSE = bloc("caisse_astrale", () -> new Coffres.Caisse(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(-1, 3600000).sound(SoundType.WOOD).noLootTable().lightLevel(s -> 7)), Rarity.EPIC);

    private static RegistryObject<Block> bloc(String id, Supplier<Block> f, Rarity r) {
        RegistryObject<Block> b = BLOCS.register(id, f);
        ITEMS.register(id, () -> new BlockItem(b.get(), new Item.Properties().rarity(r)));
        return b;
    }

    static final RegistryObject<CreativeModeTab> ONGLET = ONGLETS.register("astralys", () -> CreativeModeTab.builder()
            .title(Component.literal("Astralys")).icon(() -> new ItemStack(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(AstralysCarte.MODID, "grimoire_astralys"))))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(o -> out.accept(o.get()))).build());

    static final RegistryObject<EntityType<PnjEntity>> PNJ = ENTITES.register("pnj", () -> EntityType.Builder.<PnjEntity>of(PnjEntity::new, MobCategory.MISC)
            .sized(.6f, 1.8f).clientTrackingRange(10).build("pnj"));

    static void init() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCS.register(mod); ITEMS.register(mod); ENTITES.register(mod); ONGLETS.register(mod);
        mod.addListener((EntityAttributeCreationEvent e) -> e.put(PNJ.get(), Mob.createMobAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(Objets::commandes);
        MinecraftForge.EVENT_BUS.addListener(Objets::ensembles);
        Coffres.init();
        Bosses.init();
        Cosmetiques.init();
    }

    // ------------------------------------------------------------------ sorts
    interface Effet { boolean lancer(ServerPlayer j); }

    /** Objet active d'un clic droit : effet, puis temps de recharge ; jamais consomme. */
    static final class Sort extends Item {
        private final int recharge;
        private final boolean brille;
        private final SoundEvent son;
        private final Effet effet;

        Sort(Properties p, int secondes, boolean brille, SoundEvent son, Effet effet) {
            super(p); this.recharge = secondes * 20; this.brille = brille; this.son = son; this.effet = effet;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level l, Player j, InteractionHand main) {
            ItemStack s = j.getItemInHand(main);
            if (j instanceof ServerPlayer sp && effet.lancer(sp)) {
                j.getCooldowns().addCooldown(this, recharge);
                l.playSound(null, j.blockPosition(), son, SoundSource.PLAYERS, 1, 1.2f);
            }
            return InteractionResultHolder.sidedSuccess(s, l.isClientSide());
        }

        @Override public boolean isFoil(ItemStack s) { return brille || super.isFoil(s); }
    }

    private static List<LivingEntity> autour(ServerPlayer j, double r, boolean joueurs) {
        return j.serverLevel().getEntitiesOfClass(LivingEntity.class, j.getBoundingBox().inflate(r),
                e -> e != j && e.isAlive() && !(e instanceof PnjEntity) && (e instanceof Player) == joueurs);
    }

    private static boolean foudre(ServerPlayer j) {
        HitResult h = j.pick(40, 0, false);
        if (!(h instanceof BlockHitResult b) || h.getType() == HitResult.Type.MISS) return false;
        var eclair = EntityType.LIGHTNING_BOLT.create(j.serverLevel());
        if (eclair == null) return false;
        eclair.moveTo(Vec3.atBottomCenterOf(b.getBlockPos().above()));
        eclair.setCause(j);
        j.serverLevel().addFreshEntity(eclair);
        return true;
    }

    private static boolean givre(ServerPlayer j) {
        for (LivingEntity e : autour(j, 7, false)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            e.setTicksFrozen(220);
            e.hurt(j.damageSources().indirectMagic(j, j), 4);
        }
        j.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, j.getX(), j.getY() + 1, j.getZ(), 120, 3.5, 1, 3.5, .02);
        return true;
    }

    private static boolean transfert(ServerPlayer j) {
        Vec3 v = j.getLookAngle();
        for (int d = 10; d >= 2; d--) {
            Vec3 delta = new Vec3(v.x * d, Math.max(0, v.y * d), v.z * d);
            if (j.level().noCollision(j, j.getBoundingBox().move(delta))) {
                j.serverLevel().sendParticles(ParticleTypes.PORTAL, j.getX(), j.getY() + 1, j.getZ(), 40, .3, .6, .3, .4);
                j.teleportTo(j.getX() + delta.x, j.getY() + delta.y, j.getZ() + delta.z);
                j.fallDistance = 0;
                j.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, j.getX(), j.getY() + 1, j.getZ(), 40, .3, .6, .3, .1);
                return true;
            }
        }
        return false;
    }

    private static boolean soins(ServerPlayer j) {
        j.heal(8);
        for (LivingEntity e : autour(j, 8, true)) { e.heal(8); j.serverLevel().sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 2, e.getZ(), 5, .4, .3, .4, 0); }
        j.serverLevel().sendParticles(ParticleTypes.HEART, j.getX(), j.getY() + 2, j.getZ(), 8, .5, .3, .5, 0);
        return true;
    }

    private static boolean tempete(ServerPlayer j) {
        for (LivingEntity e : autour(j, 8, false)) {
            Vec3 d = e.position().subtract(j.position()).normalize();
            e.push(d.x * 1.8, .7, d.z * 1.8); e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
        }
        j.serverLevel().sendParticles(ParticleTypes.CLOUD, j.getX(), j.getY() + .5, j.getZ(), 80, 3, .4, 3, .15);
        return true;
    }

    private static boolean braise(ServerPlayer j) {
        Vec3 v = j.getLookAngle();
        SmallFireball f = new SmallFireball(j.level(), j, v.x, v.y, v.z);
        f.setPos(j.getX() + v.x, j.getEyeY() - .1 + v.y, j.getZ() + v.z);
        j.level().addFreshEntity(f);
        return true;
    }

    // ------------------------------------------------------------------ elixirs
    private static void elixir(String id, Rarity r, Object[]... effets) {
        objet(id, () -> new Elixir(new Item.Properties().stacksTo(16).rarity(r), effets));
    }

    /** Potion speciale : se boit, donne ses effets (duree en secondes) et rend une fiole vide. */
    static final class Elixir extends Item {
        private final Object[][] effets;

        Elixir(Properties p, Object[][] effets) { super(p); this.effets = effets; }

        @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.DRINK; }
        @Override public int getUseDuration(ItemStack s) { return 32; }
        @Override public boolean isFoil(ItemStack s) { return true; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level l, Player j, InteractionHand main) {
            j.startUsingItem(main);
            return InteractionResultHolder.consume(j.getItemInHand(main));
        }

        @Override
        public ItemStack finishUsingItem(ItemStack s, Level l, LivingEntity e) {
            if (!l.isClientSide) for (Object[] ef : effets) {
                MobEffect m = (MobEffect) ef[0];
                e.addEffect(new MobEffectInstance(m, m.isInstantenous() ? 1 : (int) ef[1] * 20, (int) ef[2]));
            }
            if (e instanceof Player j && !j.getAbilities().instabuild) {
                s.shrink(1);
                if (s.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                if (!j.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) j.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
            return s;
        }
    }

    // ------------------------------------------------------------------ armures d'ensemble
    /** Les 3 ensembles : protection proche de la netherite, bonus quand les 4 pieces sont portees. */
    enum Matiere implements ArmorMaterial {
        OMBRE("ombre", new int[] {3, 6, 8, 3}, 3, 0, "essence_ombre"),
        TITAN("titan", new int[] {4, 7, 9, 4}, 4, .2f, "coeur_titan"),
        CELESTE("celeste", new int[] {3, 6, 8, 3}, 3, .1f, "plume_celeste");

        final String id, materiau;
        private final int[] defense;   // bottes, jambieres, plastron, casque
        private final float durete, recul;

        Matiere(String id, int[] defense, float durete, float recul, String materiau) {
            this.id = id; this.defense = defense; this.durete = durete; this.recul = recul; this.materiau = materiau;
        }

        private static int idx(ArmorItem.Type t) { return switch (t) { case BOOTS -> 0; case LEGGINGS -> 1; case CHESTPLATE -> 2; case HELMET -> 3; }; }
        public int getDurabilityForType(ArmorItem.Type t) { return new int[] {13, 15, 16, 11}[idx(t)] * 45; }
        public int getDefenseForType(ArmorItem.Type t) { return defense[idx(t)]; }
        public int getEnchantmentValue() { return 18; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(AstralysCarte.MODID, materiau)));
        }
        public String getName() { return AstralysCarte.MODID + ":" + id; }
        public float getToughness() { return durete; }
        public float getKnockbackResistance() { return recul; }
    }

    static final class Armure extends ArmorItem {
        Armure(Matiere m, Type t) { super(m, t, new Properties().rarity(Rarity.EPIC).fireResistant()); }

        // Ensemble de l'Ombre complet + invisible : l'armure ne se voit plus non plus.
        @Override
        public String getArmorTexture(ItemStack s, Entity e, EquipmentSlot slot, String type) {
            if (getMaterial() == Matiere.OMBRE && e instanceof LivingEntity le && le.isInvisible() && ensemble(le) == Matiere.OMBRE)
                return AstralysCarte.MODID + ":textures/models/armor/vide.png";
            return null;
        }
    }

    private static final EquipmentSlot[] PIECES = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    static Matiere ensemble(LivingEntity e) {
        Matiere m = null;
        for (EquipmentSlot s : PIECES) {
            if (!(e.getItemBySlot(s).getItem() instanceof Armure a)) return null;
            if (m == null) m = (Matiere) a.getMaterial(); else if (a.getMaterial() != m) return null;
        }
        return m;
    }

    private static final EnumMap<Matiere, Object[][]> BONUS = new EnumMap<>(Matiere.class);
    static {
        BONUS.put(Matiere.OMBRE, new Object[][] {{MobEffects.INVISIBILITY, 0}, {MobEffects.MOVEMENT_SPEED, 0}});
        BONUS.put(Matiere.TITAN, new Object[][] {{MobEffects.DAMAGE_RESISTANCE, 0}, {MobEffects.DAMAGE_BOOST, 0}, {MobEffects.HEALTH_BOOST, 1}});
        BONUS.put(Matiere.CELESTE, new Object[][] {{MobEffects.SLOW_FALLING, 0}, {MobEffects.JUMP, 1}, {MobEffects.FIRE_RESISTANCE, 0}});
    }

    private static void ensembles(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide || e.player.tickCount % 40 != 0) return;
        Matiere m = ensemble(e.player);
        if (m == null) return;
        // sans particules : sinon l'ensemble de l'Ombre trahirait le joueur
        for (Object[] b : BONUS.get(m)) e.player.addEffect(new MobEffectInstance((MobEffect) b[0], 100, (int) b[1], true, false, true));
    }

    // ------------------------------------------------------------------ /pnj creer <type> | /pnj supprimer (op)
    private static void commandes(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("pnj").requires(s -> s.hasPermission(2))
                .then(Commands.literal("creer").then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(PnjEntity.SKINS, b))
                        .executes(c -> creer(c.getSource(), StringArgumentType.getString(c, "type")))))
                .then(Commands.literal("supprimer").executes(c -> supprimer(c.getSource()))));
    }

    private static int creer(CommandSourceStack src, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer j = src.getPlayerOrException();
        int t = List.of(PnjEntity.SKINS).indexOf(type);
        if (t < 0) { src.sendFailure(Component.literal("Types : " + String.join(", ", PnjEntity.SKINS))); return 0; }
        PnjEntity pnj = PNJ.get().create(j.serverLevel());
        if (pnj == null) return 0;
        pnj.moveTo(j.getX(), j.getY(), j.getZ(), j.getYRot(), 0);
        pnj.setYHeadRot(j.getYRot()); pnj.setYBodyRot(j.getYRot());
        pnj.sorte(t);
        j.serverLevel().addFreshEntity(pnj);
        src.sendSuccess(() -> Component.literal("PNJ « " + PnjEntity.NOMS[t] + " » placé.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int supprimer(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer j = src.getPlayerOrException();
        var proche = j.serverLevel().getEntitiesOfClass(PnjEntity.class, j.getBoundingBox().inflate(6)).stream()
                .min(Comparator.comparingDouble(j::distanceToSqr));
        if (proche.isEmpty()) { src.sendFailure(Component.literal("Aucun PNJ à moins de 6 blocs.")); return 0; }
        proche.get().discard();
        src.sendSuccess(() -> Component.literal("PNJ supprimé.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private Objets() {}
}
