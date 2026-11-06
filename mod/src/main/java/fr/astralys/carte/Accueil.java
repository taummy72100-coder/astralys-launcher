package fr.astralys.carte;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Premiere connexion : kit de depart (armure en fer, pioche, 32 steaks), titre et message de bienvenue, annonce a tout le serveur. */
final class Accueil {
    private static final String CLE = "astralys_kit";

    static void init() { MinecraftForge.EVENT_BUS.addListener(Accueil::connexion); }

    private static void connexion(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer j) || !j.server.isDedicatedServer()) return;   // monde solo (captures, tests) : rien
        CompoundTag d = j.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (d.getBoolean(CLE)) return;
        d.putBoolean(CLE, true);
        j.getPersistentData().put(Player.PERSISTED_NBT_TAG, d);
        // deja venu avant cette mise a jour : pas de kit, juste la marque
        if (j.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) > 0) return;

        equiper(j, EquipmentSlot.HEAD, Items.IRON_HELMET);
        equiper(j, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
        equiper(j, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
        equiper(j, EquipmentSlot.FEET, Items.IRON_BOOTS);
        donner(j, new ItemStack(Items.IRON_PICKAXE));
        donner(j, new ItemStack(Items.COOKED_BEEF, 32));

        String nom = j.getGameProfile().getName();
        j.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        j.connection.send(new ClientboundSetTitleTextPacket(Component.literal("✦ ASTRALYS ✦").withStyle(st -> st.withColor(0xF0B429).withBold(true))));
        j.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Bienvenue, " + nom + " !").withStyle(st -> st.withColor(0xC9A0FF))));
        j.serverLevel().playSound(null, j.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .8f, 1);
        String[] lignes = {
                "✦ Bienvenue sur Astralys, " + nom + " ! ✦",
                "Ton kit de départ t'attend : armure en fer, pioche et 32 steaks.",
                "• Ouvre ton inventaire (E) : Niveau, Métiers, Boutique, Faction, Marché, Pets, Profil.",
                "• Touche M : la carte des mondes. Mine à Sylvaria, construis ta base à Pyrélune ou Azuréa.",
                "• Monte tes 4 métiers pour gagner de l'argent, des objets spéciaux et ton pet.",
                "• Niveau 30 : rejoins ou fonde une faction.  Bon jeu !"};
        for (int i = 0; i < lignes.length; i++) {
            int c = i == 0 ? 0xF0B429 : i == 1 ? 0x22C55E : 0xE8DFFF;
            boolean gras = i == 0;
            j.sendSystemMessage(Component.literal(lignes[i]).withStyle(st -> st.withColor(c).withBold(gras)));
        }
        j.server.getPlayerList().broadcastSystemMessage(Component.literal("✦ " + nom + " arrive sur Astralys pour la première fois ! Souhaitez-lui la bienvenue !")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static void equiper(ServerPlayer j, EquipmentSlot s, net.minecraft.world.item.Item i) {
        if (j.getItemBySlot(s).isEmpty()) j.setItemSlot(s, new ItemStack(i)); else donner(j, new ItemStack(i));
    }

    private static void donner(ServerPlayer j, ItemStack s) { if (!j.getInventory().add(s)) j.drop(s, false); }

    private Accueil() {}
}
