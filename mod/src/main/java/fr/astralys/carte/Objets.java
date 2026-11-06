package fr.astralys.carte;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Objets legendaires d'Astralys (recompenses des paliers 50 et 100), leur onglet creatif, et les PNJ. */
final class Objets {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AstralysCarte.MODID);
    static final DeferredRegister<EntityType<?>> ENTITES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AstralysCarte.MODID);
    static final DeferredRegister<CreativeModeTab> ONGLETS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralysCarte.MODID);

    /** Matiere des outils : jamais reparable a l'enclume (recompense unique). */
    record Rang(int getUses, float getSpeed, float getAttackDamageBonus, int getLevel, int getEnchantmentValue) implements Tier {
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }

    private static Item.Properties p(Rarity r) { return new Item.Properties().stacksTo(1).rarity(r).fireResistant(); }

    static final RegistryObject<Item> PIOCHE_CRISTAL = ITEMS.register("pioche_cristal", () -> new PickaxeItem(new Rang(2000, 10, 3, 3, 18), 1, -2.8f, p(Rarity.RARE)));
    static final RegistryObject<Item> PIOCHE_CELESTE = ITEMS.register("pioche_celeste", () -> new PickaxeItem(new Rang(4000, 14, 4, 4, 22), 1, -2.8f, p(Rarity.EPIC)) {
        @Override public boolean isFoil(ItemStack s) { return true; } });
    static final RegistryObject<Item> HOUE_DOREE = ITEMS.register("houe_doree", () -> new HoeItem(new Rang(1800, 10, 0, 3, 22), -3, 0f, p(Rarity.RARE)));
    static final RegistryObject<Item> FAUX_MOISSONS = ITEMS.register("faux_moissons", () -> new HoeItem(new Rang(3500, 14, 2, 4, 25), -2, -1f, p(Rarity.EPIC)) {
        @Override public boolean isFoil(ItemStack s) { return true; } });
    static final RegistryObject<Item> DAGUE_CHASSEUR = ITEMS.register("dague_chasseur", () -> new SwordItem(new Rang(1900, 8, 3, 3, 18), 3, -2.0f, p(Rarity.RARE)));
    static final RegistryObject<Item> LAME_LUNAIRE = ITEMS.register("lame_lunaire", () -> new SwordItem(new Rang(3600, 9, 5, 4, 22), 4, -2.2f, p(Rarity.EPIC)) {
        @Override public boolean isFoil(ItemStack s) { return true; } });
    static final RegistryObject<Item> FIOLE = ITEMS.register("fiole_eternelle", () -> new Magique(p(Rarity.RARE), 60 * 20, false,
            () -> new MobEffectInstance[] {new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 1)}));
    static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire_astralys", () -> new Magique(p(Rarity.EPIC), 5 * 60 * 20, true,
            () -> new MobEffectInstance[] {new MobEffectInstance(MobEffects.ABSORPTION, 30 * 20, 1), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30 * 20, 0),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 30 * 20, 0)}));

    static final RegistryObject<CreativeModeTab> ONGLET = ONGLETS.register("astralys", () -> CreativeModeTab.builder()
            .title(Component.literal("Astralys")).icon(() -> new ItemStack(GRIMOIRE.get()))
            .displayItems((params, out) -> ITEMS.getEntries().forEach(o -> out.accept(o.get()))).build());

    static final RegistryObject<EntityType<PnjEntity>> PNJ = ENTITES.register("pnj", () -> EntityType.Builder.<PnjEntity>of(PnjEntity::new, MobCategory.MISC)
            .sized(.6f, 1.8f).clientTrackingRange(10).build("pnj"));

    static void init() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(mod); ENTITES.register(mod); ONGLETS.register(mod);
        mod.addListener((EntityAttributeCreationEvent e) -> e.put(PNJ.get(), Mob.createMobAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(Objets::commandes);
    }

    /** Objet qu'on active d'un clic droit : effets puis temps de recharge, sans etre consomme. */
    static final class Magique extends Item {
        private final int recharge;
        private final boolean brille;
        private final Supplier<MobEffectInstance[]> effets;

        Magique(Properties p, int recharge, boolean brille, Supplier<MobEffectInstance[]> effets) {
            super(p); this.recharge = recharge; this.brille = brille; this.effets = effets;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level l, Player j, InteractionHand main) {
            ItemStack s = j.getItemInHand(main);
            if (!l.isClientSide) {
                for (MobEffectInstance e : effets.get()) j.addEffect(e);
                j.getCooldowns().addCooldown(this, recharge);
                l.playSound(null, j.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);
            }
            return InteractionResultHolder.sidedSuccess(s, l.isClientSide());
        }

        @Override public boolean isFoil(ItemStack s) { return brille || super.isFoil(s); }
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
