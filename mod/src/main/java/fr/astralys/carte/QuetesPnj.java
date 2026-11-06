package fr.astralys.carte;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Chaines de quetes des PNJ (archimage, forgeronne, enchanteresse) : apporter des objets, recevoir coffres et objets speciaux. */
final class QuetesPnj {
    /** demande : "ns:id*n" a apporter ; recompense : "ns:id*n" ; argent en plus. */
    record Quete(String texte, String demande, String recompense, long argent) {}

    private static Quete q(String t, String d, String r, long a) { return new Quete(t, d, r.contains(":") ? r : "astralyscarte:" + r, a); }

    // index = sorte du PNJ - 5
    static final List<List<Quete>> CHAINES = List.of(
            List.of(q("Mes bâtons ont besoin de feu : rapporte-moi des bâtons de blaze.", "minecraft:blaze_rod*16", "coffre_rare", 2000),
                    q("L'améthyste garde la mémoire des sorts. Il m'en faut beaucoup.", "minecraft:amethyst_shard*64", "orbe_transfert", 4000),
                    q("Les larmes de ghast apaisent les esprits du Nether.", "minecraft:ghast_tear*12", "coffre_epique", 6000),
                    q("On raconte qu'un archimage déchu perd des plumes célestes…", "astralyscarte:plume_celeste*2", "tome_tempete", 10000),
                    q("Dernière épreuve : l'étoile du Wither. Prouve ta valeur.", "minecraft:nether_star*1", "baton_foudre", 20000)),
            List.of(q("Ma forge a faim ! Des blocs de fer, vite.", "minecraft:iron_block*16", "coffre_rare", 2000),
                    q("L'or se travaille mieux que tu ne crois. Apporte-m'en.", "minecraft:gold_block*16", "cle_rare", 4000),
                    q("Pour les grandes armures, il faut du diamant. Beaucoup.", "minecraft:diamond_block*8", "coffre_epique", 6000),
                    q("La netherite… le métal des rois.", "minecraft:netherite_ingot*4", "cle_legendaire", 12000),
                    q("Le cœur d'un titan battra dans ma forge éternellement.", "astralyscarte:coeur_titan*2", "coffre_special", 25000)),
            List.of(q("Le lapis est l'encre des enchantements.", "minecraft:lapis_block*16", "coffre_rare", 2000),
                    q("De l'expérience en bouteille, pour mes rituels.", "minecraft:experience_bottle*32", "elixir_vie*3", 4000),
                    q("Mes grimoires s'usent : il me faut des livres.", "minecraft:book*64", "cle_epique", 6000),
                    q("Les émeraudes chantent quand la lune est pleine.", "minecraft:emerald_block*16", "coffre_legendaire", 12000),
                    q("L'essence d'ombre… ne la touche pas sans gants.", "astralyscarte:essence_ombre*2", "coffre_special", 25000)));

    private static final String CLE = "astralys_quetes";

    private static CompoundTag donnees(Player j) {
        CompoundTag p = j.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        j.getPersistentData().put(Player.PERSISTED_NBT_TAG, p);   // conserve apres la mort
        CompoundTag q = p.getCompound(CLE);
        p.put(CLE, q);
        return q;
    }

    private static int compter(ServerPlayer j, ItemStack modele) {
        int n = 0;
        for (ItemStack s : j.getInventory().items) if (ItemStack.isSameItem(s, modele)) n += s.getCount();
        return n;
    }

    private static void retirer(ServerPlayer j, ItemStack modele, int n) {
        for (ItemStack s : j.getInventory().items) {
            if (n <= 0) return;
            if (!ItemStack.isSameItem(s, modele)) continue;
            int k = Math.min(n, s.getCount()); s.shrink(k); n -= k;
        }
    }

    /** Clic droit sur un PNJ a quetes : rend la quete si le joueur a les objets, sinon la (re)explique. */
    static void parler(ServerPlayer j, PnjEntity pnj) {
        int c = pnj.sorte() - 5;
        if (c < 0 || c >= CHAINES.size()) return;
        List<Quete> chaine = CHAINES.get(c);
        CompoundTag d = donnees(j);
        String cle = PnjEntity.SKINS[pnj.sorte()];
        int etape = d.getInt(cle);
        int couleur = PnjEntity.COULEURS[pnj.sorte()];
        Component nom = Component.literal("[" + PnjEntity.NOMS[pnj.sorte()] + "] ").withStyle(st -> st.withColor(couleur).withBold(true));
        if (etape >= chaine.size()) { j.sendSystemMessage(nom.copy().append(Component.literal("Tu as accompli toutes mes quêtes. Les étoiles se souviendront de toi.").withStyle(ChatFormatting.ITALIC))); return; }
        Quete q = chaine.get(etape);
        ItemStack modele = Metiers.pile(q.demande());
        int a = compter(j, modele), n = modele.getCount();
        if (a >= n) {
            retirer(j, modele, n);
            ItemStack gain = Metiers.pile(q.recompense());
            if (!j.getInventory().add(gain.copy())) j.drop(gain.copy(), false);
            Economie.crediter(j, q.argent(), "quête " + PnjEntity.NOMS[pnj.sorte()]);
            d.putInt(cle, etape + 1);
            j.sendSystemMessage(nom.copy().append(Component.literal("Merci ! Voici ta récompense : ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(gain.getCount() + "× ").withStyle(ChatFormatting.GOLD)).append(gain.getHoverName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal(" + " + Economie.format(q.argent()) + " $").withStyle(ChatFormatting.GREEN)));
            j.serverLevel().playSound(null, pnj.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1, 1.2f);
            return;
        }
        j.sendSystemMessage(nom.copy().append(Component.literal("« " + q.texte() + " »").withStyle(ChatFormatting.ITALIC)));
        j.sendSystemMessage(Component.literal("   Quête " + (etape + 1) + "/" + chaine.size() + " : apporte " + n + "× ").withStyle(ChatFormatting.GRAY)
                .append(modele.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("  (tu en as " + a + ")").withStyle(a > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)));
    }

    private QuetesPnj() {}
}
