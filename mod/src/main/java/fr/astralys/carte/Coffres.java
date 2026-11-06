package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;

/** Coffres de butin a 5 raretes (s'ouvrent d'un clic droit, roulette a l'ecran) et caisse du spawn qui s'ouvre avec une cle. */
final class Coffres {
    static final String[] RARETES = {"commun", "rare", "epique", "legendaire", "special"};
    static final String[] NOMS = {"Commun", "Rare", "Épique", "Légendaire", "Spécial"};
    static final int[] COULEURS = {0xB8B8C4, 0x4C9BFF, 0xB46CFF, 0xF0B429, 0xFF4FD8};
    private static final Rarity[] RARITES = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.EPIC};

    /** Un lot possible : objet "ns:id*n", rarete affichee (couleur de la carte), poids du tirage. */
    record Lot(String objet, int rarete, int poids) {}

    private static Lot l(String o, int r, int p) { return new Lot(o.contains(":") ? o : "astralyscarte:" + o, r, p); }

    static final List<List<Lot>> LOTS = List.of(
            List.of(l("minecraft:iron_ingot*16", 0, 20), l("minecraft:gold_ingot*12", 0, 16), l("minecraft:emerald*8", 0, 14), l("minecraft:experience_bottle*16", 0, 14),
                    l("minecraft:diamond*2", 1, 10), l("minecraft:golden_apple*2", 1, 8), l("elixir_celerite*2", 1, 8), l("elixir_mineur*2", 1, 8), l("coffre_rare", 2, 2)),
            List.of(l("minecraft:diamond*6", 1, 18), l("minecraft:golden_apple*6", 1, 14), l("elixir_force*2", 1, 12), l("elixir_phenix*2", 1, 12), l("elixir_vie*2", 1, 10),
                    l("minecraft:netherite_scrap*2", 2, 8), l("cle_rare", 2, 6), l("orbe_transfert", 2, 4), l("baton_braise", 2, 4), l("coffre_epique", 3, 2)),
            List.of(l("minecraft:netherite_ingot", 2, 16), l("elixir_titan*2", 2, 14), l("elixir_ombre*2", 2, 12), l("minecraft:totem_of_undying", 2, 10),
                    l("cle_epique", 2, 8), l("sceptre_givre", 3, 6), l("baguette_soins", 3, 6), l("epee_runique", 3, 5), l("minecraft:enchanted_golden_apple", 3, 5),
                    l("coffre_legendaire", 3, 2)),
            List.of(l("minecraft:netherite_ingot*3", 3, 16), l("minecraft:elytra", 3, 8), l("cle_legendaire", 3, 8), l("essence_ombre", 3, 10), l("coeur_titan", 3, 10),
                    l("plume_celeste", 3, 10), l("baton_foudre", 3, 6), l("tome_tempete", 3, 6), l("marteau_forgeron", 3, 6), l("hache_titan", 3, 6), l("coffre_special", 4, 2)),
            List.of(l("minecraft:enchanted_golden_apple*3", 3, 14), l("minecraft:netherite_block", 4, 10), l("minecraft:beacon", 4, 8), l("grimoire_astralys", 4, 8),
                    l("lame_lunaire", 4, 7), l("pioche_celeste", 4, 7), l("casque_ombre", 4, 5), l("bottes_ombre", 4, 5), l("casque_titan", 4, 5), l("bottes_titan", 4, 5),
                    l("casque_celeste", 4, 5), l("bottes_celeste", 4, 5)));

    static Lot tirer(int r, RandomSource rnd) {
        List<Lot> l = LOTS.get(r);
        int total = l.stream().mapToInt(Lot::poids).sum(), x = rnd.nextInt(total);
        for (Lot lot : l) if ((x -= lot.poids()) < 0) return lot;
        return l.get(0);
    }

    static final int TAILLE = 44, GAGNANT = 38;   // longueur de la bande de la roulette, case ou elle s'arrete

    /** Tire le lot, le donne tout de suite (fermer l'ecran ne fait rien perdre) et envoie l'animation au joueur. */
    static void ouvrir(ServerPlayer j, int r) {
        Lot g = tirer(r, j.getRandom());
        ItemStack gain = Metiers.pile(g.objet());
        if (!j.getInventory().add(gain.copy())) j.drop(gain.copy(), false);
        List<ItemStack> bande = new ArrayList<>();
        List<Integer> raretes = new ArrayList<>();
        for (int i = 0; i < TAILLE; i++) {
            Lot x = i == GAGNANT ? g : tirer(r, j.getRandom());
            bande.add(Metiers.pile(x.objet())); raretes.add(x.rarete());
        }
        Economie.envoyer(j, new Roulette(r, bande, raretes, GAGNANT));
        j.serverLevel().playSound(null, j.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1, .8f);
        if (g.rarete() >= 3)
            j.server.getPlayerList().broadcastSystemMessage(Component.literal("✦ " + j.getGameProfile().getName() + " a obtenu ").withStyle(st -> st.withColor(COULEURS[g.rarete()]))
                    .append(gain.getHoverName().copy().withStyle(ChatFormatting.BOLD))
                    .append(Component.literal(" dans un coffre " + NOMS[r].toLowerCase(java.util.Locale.FRANCE) + " !").withStyle(st -> st.withColor(COULEURS[g.rarete()]))), false);
    }

    record Roulette(int rarete, List<ItemStack> objets, List<Integer> raretes, int gagnant) {
        void ecrire(FriendlyByteBuf b) {
            b.writeVarInt(rarete); b.writeVarInt(gagnant);
            b.writeCollection(objets, FriendlyByteBuf::writeItem);
            b.writeCollection(raretes, FriendlyByteBuf::writeVarInt);
        }
        static Roulette lire(FriendlyByteBuf b) {
            int r = b.readVarInt(), g = b.readVarInt();
            return new Roulette(r, b.readList(FriendlyByteBuf::readItem), b.readList(FriendlyByteBuf::readVarInt), g);
        }
    }

    static void init() {
        Metiers.CANAL.messageBuilder(Roulette.class, 10, NetworkDirection.PLAY_TO_CLIENT).encoder(Roulette::ecrire).decoder(Roulette::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.roulette(m))).add();
    }

    static final class CoffreItem extends Item {
        final int rarete;

        CoffreItem(int r) { super(new Properties().stacksTo(16).rarity(RARITES[r])); rarete = r; }

        @Override public boolean isFoil(ItemStack s) { return rarete >= 3; }

        @Override
        public InteractionResultHolder<ItemStack> use(Level l, Player j, InteractionHand main) {
            ItemStack s = j.getItemInHand(main);
            if (j instanceof ServerPlayer sp) {
                // chaque coffre s'ouvre avec SA cle (boutique, onglet Cles), consommee a l'ouverture
                ItemStack cle = ItemStack.EMPTY;
                for (ItemStack x : sp.getInventory().items) if (x.getItem() instanceof Cle c && c.rarete == rarete) { cle = x; break; }
                if (cle.isEmpty()) {
                    sp.displayClientMessage(Component.literal("🔒 Il faut une clé " + NOMS[rarete].toLowerCase(java.util.Locale.FRANCE) + " pour ouvrir ce coffre (boutique → Clés)")
                            .withStyle(st -> st.withColor(COULEURS[rarete])), true);
                    sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.CHEST_LOCKED, SoundSource.PLAYERS, 1, 1);
                    return InteractionResultHolder.fail(s);
                }
                cle.shrink(1); s.shrink(1);
                ouvrir(sp, rarete);
            }
            return InteractionResultHolder.sidedSuccess(s, l.isClientSide());
        }
    }

    static final class Cle extends Item {
        final int rarete;

        Cle(int r) { super(new Properties().stacksTo(16).rarity(RARITES[r])); rarete = r; }

        @Override public boolean isFoil(ItemStack s) { return true; }
    }

    /** Caisse astrale du spawn : incassable en survie, s'ouvre avec une cle (la rarete de la cle donne celle du coffre). */
    static final class Caisse extends Block {
        Caisse(Properties p) { super(p); }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult use(BlockState st, Level l, BlockPos pos, Player j, InteractionHand main, BlockHitResult h) {
            if (!(j instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            ItemStack s = j.getItemInHand(main);
            if (!(s.getItem() instanceof Cle c)) {
                sp.sendSystemMessage(Component.literal("✦ Il faut une clé (commune à spéciale) pour ouvrir la caisse astrale.").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.CONSUME;
            }
            s.shrink(1);
            ouvrir(sp, c.rarete);
            return InteractionResult.CONSUME;
        }
    }

    private Coffres() {}
}
