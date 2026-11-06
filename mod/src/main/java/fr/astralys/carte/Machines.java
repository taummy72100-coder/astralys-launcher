package fr.astralys.carte;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Machines d'Astralys : broyeur astral (minerai -> double lingot), incubateur astral (eclosion des pets), piege runique. */
final class Machines {
    static final DeferredRegister<BlockEntityType<?>> BE = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AstralysCarte.MODID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AstralysCarte.MODID);

    static final RegistryObject<BlockEntityType<BroyeurBE>> BROYEUR_BE = BE.register("broyeur_astral",
            () -> BlockEntityType.Builder.of(BroyeurBE::new, Objets.BROYEUR.get()).build(null));
    static final RegistryObject<BlockEntityType<IncubateurBE>> INCUBATEUR_BE = BE.register("incubateur_astral",
            () -> BlockEntityType.Builder.of(IncubateurBE::new, Objets.INCUBATEUR.get()).build(null));
    static final RegistryObject<MenuType<BroyeurMenu>> MENU_BROYEUR = MENUS.register("broyeur_astral",
            () -> new MenuType<>(BroyeurMenu::new, FeatureFlags.DEFAULT_FLAGS));

    static void init(IEventBus mod) { BE.register(mod); MENUS.register(mod); }

    // ================================================================== broyeur astral
    /** Recettes du broyeur : minerai ou brut -> resultat (doubles, voire plus). */
    private static Map<Item, ItemStack> recettes;

    static ItemStack resultat(ItemStack s) {
        if (recettes == null) {
            recettes = new HashMap<>();
            String[][] r = {
                    {"raw_iron,iron_ore,deepslate_iron_ore,alexscaves:galena_iron_ore", "iron_ingot", "2"},
                    {"raw_gold,gold_ore,deepslate_gold_ore,nether_gold_ore", "gold_ingot", "2"},
                    {"raw_copper,copper_ore,deepslate_copper_ore", "copper_ingot", "3"},
                    {"coal_ore,deepslate_coal_ore,alexscaves:coprolith_coal_ore", "coal", "3"},
                    {"diamond_ore,deepslate_diamond_ore", "diamond", "2"}, {"emerald_ore,deepslate_emerald_ore", "emerald", "2"},
                    {"lapis_ore,deepslate_lapis_ore", "lapis_lazuli", "10"}, {"redstone_ore,deepslate_redstone_ore,alexscaves:guanostone_redstone_ore", "redstone", "8"},
                    {"nether_quartz_ore", "quartz", "3"}, {"ancient_debris", "netherite_scrap", "2"},
                    {"create:raw_zinc,create:zinc_ore,create:deepslate_zinc_ore", "create:zinc_ingot", "2"},
                    {"alexscaves:raw_scarlet_neodymium", "alexscaves:scarlet_neodymium_ingot", "2"},
                    {"alexscaves:raw_azure_neodymium", "alexscaves:azure_neodymium_ingot", "2"},
                    {"alexscaves:radrock_uranium_ore", "alexscaves:uranium_shard", "4"}};
            for (String[] l : r) {
                Item sortie = item(l[1]);
                if (sortie == null) continue;
                for (String e : l[0].split(",")) { Item in = item(e); if (in != null) recettes.put(in, new ItemStack(sortie, Integer.parseInt(l[2]))); }
            }
        }
        ItemStack r = recettes.get(s.getItem());
        return r == null ? ItemStack.EMPTY : r;
    }

    private static Item item(String id) {
        Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id.contains(":") ? id : "minecraft:" + id));
        return i == null || i == net.minecraft.world.item.Items.AIR ? null : i;
    }

    static final class Broyeur extends BaseEntityBlock {
        static final BooleanProperty LIT = BlockStateProperties.LIT;

        Broyeur(BlockBehaviour.Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(LIT, false)); }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }
        @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new BroyeurBE(p, s); }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
            return l.isClientSide ? null : createTickerHelper(t, BROYEUR_BE.get(), BroyeurBE::tick);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player j, InteractionHand main, BlockHitResult h) {
            if (!l.isClientSide && l.getBlockEntity(p) instanceof BroyeurBE be) j.openMenu(be);
            return InteractionResult.sidedSuccess(l.isClientSide);
        }

        @Override
        public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean deplace) {
            if (!s.is(n.getBlock()) && l.getBlockEntity(p) instanceof BroyeurBE be) Containers.dropContents(l, p, be);
            super.onRemove(s, l, p, n, deplace);
        }

        @Override
        public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
            if (!s.getValue(LIT)) return;
            l.addParticle(ParticleTypes.SMOKE, p.getX() + .5, p.getY() + 1.05, p.getZ() + .5, 0, .03, 0);
            l.addParticle(ParticleTypes.CRIT, p.getX() + r.nextDouble(), p.getY() + .45, p.getZ() + r.nextDouble(), 0, .05, 0);
            if (r.nextInt(6) == 0) l.playLocalSound(p.getX() + .5, p.getY() + .5, p.getZ() + .5, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, .25f, 1.3f, false);
        }
    }

    static final class BroyeurBE extends BaseContainerBlockEntity implements WorldlyContainer {
        static final int DUREE = 160;   // 8 secondes par objet
        private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
        private int progres;
        final ContainerData data = new ContainerData() {
            public int get(int i) { return i == 0 ? progres : DUREE; }
            public void set(int i, int v) { if (i == 0) progres = v; }
            public int getCount() { return 2; }
        };

        BroyeurBE(BlockPos p, BlockState s) { super(BROYEUR_BE.get(), p, s); }

        static void tick(Level l, BlockPos p, BlockState s, BroyeurBE be) {
            ItemStack res = resultat(be.items.get(0));
            boolean peut = !res.isEmpty() && rentre(be.items.get(1), res);
            if (peut) {
                if (++be.progres >= DUREE) {
                    be.progres = 0;
                    be.items.get(0).shrink(1);
                    if (be.items.get(1).isEmpty()) be.items.set(1, res.copy()); else be.items.get(1).grow(res.getCount());
                    ItemStack bonus = Metiers.pile("astralyscarte:poussiere_etoile");
                    if (l.random.nextFloat() < .15f && rentre(be.items.get(2), bonus)) {
                        if (be.items.get(2).isEmpty()) be.items.set(2, bonus); else be.items.get(2).grow(1);
                    }
                    l.playSound(null, p, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, .6f, 1.4f);
                }
                be.setChanged();
            } else if (be.progres > 0) { be.progres = 0; be.setChanged(); }
            if (s.getValue(Broyeur.LIT) != peut) l.setBlock(p, s.setValue(Broyeur.LIT, peut), 3);
        }

        private static boolean rentre(ItemStack dans, ItemStack ajout) {
            return dans.isEmpty() || (ItemStack.isSameItemSameTags(dans, ajout) && dans.getCount() + ajout.getCount() <= dans.getMaxStackSize());
        }

        @Override protected Component getDefaultName() { return Component.literal("Broyeur astral"); }
        @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new BroyeurMenu(id, inv, this, data); }
        @Override public int getContainerSize() { return 3; }
        @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int i) { return items.get(i); }
        @Override public ItemStack removeItem(int i, int n) { setChanged(); return ContainerHelper.removeItem(items, i, n); }
        @Override public ItemStack removeItemNoUpdate(int i) { return ContainerHelper.takeItem(items, i); }
        @Override public void setItem(int i, ItemStack s) { items.set(i, s); setChanged(); }
        @Override public boolean stillValid(Player j) { return Container.stillValidBlockEntity(this, j); }
        @Override public void clearContent() { items.clear(); }
        @Override public boolean canPlaceItem(int i, ItemStack s) { return i == 0 && !resultat(s).isEmpty(); }
        // tremies et entonnoirs : on remplit par le haut ou les cotes, on vide par le dessous
        @Override public int[] getSlotsForFace(Direction d) { return d == Direction.DOWN ? new int[] {1, 2} : new int[] {0}; }
        @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction d) { return canPlaceItem(i, s); }
        @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction d) { return i != 0; }

        @Override public void load(CompoundTag t) { super.load(t); ContainerHelper.loadAllItems(t, items); progres = t.getInt("Progres"); }
        @Override protected void saveAdditional(CompoundTag t) { super.saveAdditional(t); ContainerHelper.saveAllItems(t, items); t.putInt("Progres", progres); }
    }

    static final class BroyeurMenu extends AbstractContainerMenu {
        final Container c;
        final ContainerData d;

        BroyeurMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(3), new SimpleContainerData(2)); }

        BroyeurMenu(int id, Inventory inv, Container c, ContainerData d) {
            super(MENU_BROYEUR.get(), id);
            checkContainerSize(c, 3);
            this.c = c; this.d = d;
            addSlot(new Slot(c, 0, 44, 35));
            addSlot(new Slot(c, 1, 116, 35) { @Override public boolean mayPlace(ItemStack s) { return false; } });
            addSlot(new Slot(c, 2, 142, 35) { @Override public boolean mayPlace(ItemStack s) { return false; } });
            for (int r = 0; r < 3; r++) for (int k = 0; k < 9; k++) addSlot(new Slot(inv, k + r * 9 + 9, 8 + k * 18, 84 + r * 18));
            for (int k = 0; k < 9; k++) addSlot(new Slot(inv, k, 8 + k * 18, 142));
            addDataSlots(d);
        }

        float progres() { return d.get(1) == 0 ? 0 : d.get(0) / (float) d.get(1); }

        @Override
        public ItemStack quickMoveStack(Player j, int i) {
            Slot s = slots.get(i);
            if (!s.hasItem()) return ItemStack.EMPTY;
            ItemStack x = s.getItem(), copie = x.copy();
            if (i < 3) { if (!moveItemStackTo(x, 3, 39, true)) return ItemStack.EMPTY; }
            else if (resultat(x).isEmpty() || !moveItemStackTo(x, 0, 1, false)) return ItemStack.EMPTY;
            if (x.isEmpty()) s.set(ItemStack.EMPTY); else s.setChanged();
            return copie;
        }

        @Override public boolean stillValid(Player j) { return c.stillValid(j); }
    }

    // ================================================================== incubateur astral
    static final class Incubateur extends BaseEntityBlock {
        private static final VoxelShape FORME = Block.box(2, 0, 2, 14, 15, 14);

        Incubateur(BlockBehaviour.Properties p) { super(p); }

        @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
        @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return FORME; }
        @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new IncubateurBE(p, s); }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
            return l.isClientSide ? null : createTickerHelper(t, INCUBATEUR_BE.get(), IncubateurBE::tick);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pj, InteractionHand main, BlockHitResult h) {
            if (l.isClientSide || !(pj instanceof ServerPlayer j) || !(l.getBlockEntity(p) instanceof IncubateurBE be)) return InteractionResult.sidedSuccess(l.isClientSide);
            if (be.sorte >= 0) {
                if (be.reste <= 0 && j.getUUID().equals(be.maitre)) be.eclore(j);
                else j.displayClientMessage(Component.literal("🥚 " + Familiers.NOMS[be.sorte] + " : éclosion dans " + (be.reste / 20 + 1) + " s").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResult.SUCCESS;
            }
            ItemStack o = j.getItemInHand(main);
            int t = Familiers.oeuf(o);
            if (t < 0) { dire(j, "Fais un clic droit avec un œuf de pet (boutique des pets : inventaire → PETS).", ChatFormatting.GRAY); return InteractionResult.SUCCESS; }
            if (Metiers.niveau(j, t) < Familiers.NIVEAU) { dire(j, "🔒 Il faut le niveau " + Familiers.NIVEAU + " en " + Metiers.NOMS[t] + " pour faire éclore cet œuf.", ChatFormatting.RED); return InteractionResult.SUCCESS; }
            if (Familiers.possede(j, t)) { dire(j, "Tu as déjà " + Familiers.NOMS[t] + ".", ChatFormatting.YELLOW); return InteractionResult.SUCCESS; }
            if (!Familiers.construction(l, p, t)) { dire(j, "✦ Construction incorrecte : " + Familiers.CONSTRUCTION[t] + ".", ChatFormatting.GOLD); return InteractionResult.SUCCESS; }
            o.shrink(1);
            be.maitre = j.getUUID(); be.sorte = t; be.reste = IncubateurBE.DUREE; be.setChanged();
            l.playSound(null, p, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1, 1.4f);
            dire(j, "🥚 L'œuf est au chaud… éclosion dans 30 secondes.", ChatFormatting.LIGHT_PURPLE);
            return InteractionResult.SUCCESS;
        }

        private static void dire(ServerPlayer j, String m, ChatFormatting c) { j.sendSystemMessage(Component.literal(m).withStyle(c)); }
    }

    static final class IncubateurBE extends BlockEntity {
        static final int DUREE = 600;
        UUID maitre;
        int sorte = -1, reste;

        IncubateurBE(BlockPos p, BlockState s) { super(INCUBATEUR_BE.get(), p, s); }

        static void tick(Level l, BlockPos p, BlockState s, IncubateurBE be) {
            if (be.sorte < 0 || !(l instanceof ServerLevel sl)) return;
            if (be.reste > 0) {
                be.reste--;
                if (be.reste % 5 == 0) sl.sendParticles(ParticleTypes.ENCHANT, p.getX() + .5, p.getY() + .7, p.getZ() + .5, 6, .3, .3, .3, .4);
                if (be.reste % 40 == 0) l.playSound(null, p, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, .8f, .8f + (DUREE - be.reste) / (float) DUREE);
                if (be.reste == 0) {
                    ServerPlayer j = be.maitre == null ? null : sl.getServer().getPlayerList().getPlayer(be.maitre);
                    if (j != null) be.eclore(j);   // sinon il eclot quand le maitre revient cliquer dessus
                }
                be.setChanged();
            }
        }

        void eclore(ServerPlayer j) {
            int t = sorte;
            sorte = -1; maitre = null; setChanged();
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, 60, .4, .5, .4, .3);
                sl.playSound(null, worldPosition, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1, 1);
            }
            Familiers.debloquer(j, t);
        }

        @Override public void load(CompoundTag t) { super.load(t); sorte = t.contains("Sorte") ? t.getInt("Sorte") : -1; reste = t.getInt("Reste"); if (t.hasUUID("Maitre")) maitre = t.getUUID("Maitre"); }
        @Override protected void saveAdditional(CompoundTag t) { super.saveAdditional(t); t.putInt("Sorte", sorte); t.putInt("Reste", reste); if (maitre != null) t.putUUID("Maitre", maitre); }
    }

    // ================================================================== piege runique (Hunter niveau 30)
    static final class Piege extends Block {
        private static final VoxelShape FORME = Block.box(0, 0, 0, 16, 2, 16);

        Piege(BlockBehaviour.Properties p) { super(p); }

        @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return FORME; }

        @Override
        public void entityInside(BlockState s, Level l, BlockPos p, Entity e) {
            if (l.isClientSide || !(e instanceof Enemy) || !(e instanceof LivingEntity m) || l.getGameTime() % 10 != 0) return;
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            m.hurt(l.damageSources().magic(), 2);
            ((ServerLevel) l).sendParticles(ParticleTypes.CRIMSON_SPORE, m.getX(), m.getY() + .3, m.getZ(), 6, .3, .1, .3, 0);
        }
    }

    private Machines() {}
}
