package fr.astralys.carte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;

/**
 * Hotel des ventes entre joueurs : on met en vente l'objet en main a un prix,
 * frais de mise en vente payes tout de suite, taxe prelevee sur la vente, offres qui expirent et reviennent au vendeur.
 */
final class HotelVentes {
    static final long DUREE_MS = 72L * 3600 * 1000;
    static final int MAX_OFFRES = 8;
    static final double FRAIS = .02, TAXE = .05;

    static long frais(long prix) { return Math.max(5, Math.round(prix * FRAIS)); }
    static long taxe(long prix) { return Math.round(prix * TAXE); }

    static final class Offre {
        UUID id, vendeur;
        String nomVendeur;
        ItemStack objet;
        long prix, expire;

        CompoundTag ecrire() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id); t.putUUID("v", vendeur); t.putString("n", nomVendeur); t.put("o", objet.save(new CompoundTag()));
            t.putLong("p", prix); t.putLong("e", expire);
            return t;
        }

        static Offre lire(CompoundTag t) {
            Offre o = new Offre();
            o.id = t.getUUID("id"); o.vendeur = t.getUUID("v"); o.nomVendeur = t.getString("n"); o.objet = ItemStack.of(t.getCompound("o"));
            o.prix = t.getLong("p"); o.expire = t.getLong("e");
            return o;
        }
    }

    static final class Donnees extends SavedData {
        final List<Offre> offres = new ArrayList<>();
        final Map<UUID, List<ItemStack>> aRendre = new HashMap<>();   // offres expirees de joueurs hors ligne

        static Donnees lire(CompoundTag t) {
            Donnees d = new Donnees();
            for (Tag x : t.getList("offres", Tag.TAG_COMPOUND)) d.offres.add(Offre.lire((CompoundTag) x));
            CompoundTag r = t.getCompound("rendre");
            for (String k : r.getAllKeys()) {
                List<ItemStack> l = new ArrayList<>();
                for (Tag x : r.getList(k, Tag.TAG_COMPOUND)) l.add(ItemStack.of((CompoundTag) x));
                d.aRendre.put(UUID.fromString(k), l);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag l = new ListTag(); offres.forEach(o -> l.add(o.ecrire())); t.put("offres", l);
            CompoundTag r = new CompoundTag();
            aRendre.forEach((u, items) -> { ListTag li = new ListTag(); items.forEach(i -> li.add(i.save(new CompoundTag()))); r.put(u.toString(), li); });
            t.put("rendre", r);
            return t;
        }
    }

    private static Donnees d(MinecraftServer s) { return s.overworld().getDataStorage().computeIfAbsent(Donnees::lire, Donnees::new, "astralys_hdv"); }

    private static void donner(ServerPlayer p, ItemStack s) { if (!p.getInventory().add(s)) p.drop(s, false); }

    private static void rendre(MinecraftServer s, UUID u, ItemStack obj) {
        ServerPlayer p = s.getPlayerList().getPlayer(u);
        if (p != null) { donner(p, obj); p.sendSystemMessage(Component.literal("↩ Ton offre a expiré : l'objet t'est rendu.").withStyle(ChatFormatting.GRAY)); }
        else d(s).aRendre.computeIfAbsent(u, k -> new ArrayList<>()).add(obj);
        d(s).setDirty();
    }

    private static void expirer(MinecraftServer s) {
        long now = System.currentTimeMillis();
        for (Offre o : List.copyOf(d(s).offres)) if (o.expire <= now) { d(s).offres.remove(o); rendre(s, o.vendeur, o.objet); }
    }

    // ------------------------------------------------------------------ reseau
    record OffreVue(UUID id, ItemStack objet, long prix, String vendeur, int minutes, boolean mienne) {}

    record Vue(List<OffreVue> offres, int miennes) {
        void ecrire(FriendlyByteBuf b) {
            b.writeVarInt(offres.size());
            for (OffreVue o : offres) { b.writeUUID(o.id); b.writeItem(o.objet); b.writeVarLong(o.prix); b.writeUtf(o.vendeur); b.writeVarInt(o.minutes); b.writeBoolean(o.mienne); }
            b.writeVarInt(miennes);
        }
        static Vue lire(FriendlyByteBuf b) {
            int n = Math.min(b.readVarInt(), 500); List<OffreVue> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(new OffreVue(b.readUUID(), b.readItem(), b.readVarLong(), b.readUtf(), b.readVarInt(), b.readBoolean()));
            return new Vue(l, b.readVarInt());
        }
    }

    /** Client -> serveur : 1 acheter l'offre, 2 retirer son offre, 3 mettre en vente l'objet en main au prix donne. */
    record Action(int type, UUID offre, long prix) {
        void ecrire(FriendlyByteBuf b) { b.writeVarInt(type); b.writeUUID(offre); b.writeVarLong(prix); }
        static Action lire(FriendlyByteBuf b) { return new Action(b.readVarInt(), b.readUUID(), b.readVarLong()); }
    }

    static void envoyerVue(ServerPlayer p) {
        expirer(p.server);
        long now = System.currentTimeMillis();
        List<OffreVue> l = new ArrayList<>();
        int miennes = 0;
        List<Offre> tri = new ArrayList<>(d(p.server).offres);
        tri.sort((a, b) -> Long.compare(b.expire, a.expire));   // les plus recentes d'abord
        for (Offre o : tri) {
            boolean m = o.vendeur.equals(p.getUUID());
            if (m) miennes++;
            if (l.size() < 400) l.add(new OffreVue(o.id, o.objet, o.prix, o.nomVendeur, (int) Math.max(0, (o.expire - now) / 60000), m));
        }
        Economie.envoyer(p, new Vue(l, miennes));
        Economie.envoyerSolde(p);
    }

    private static void dire(ServerPlayer p, String t, ChatFormatting c) { p.displayClientMessage(Component.literal(t).withStyle(c), true); }

    private static void action(ServerPlayer p, Action a) {
        MinecraftServer s = p.server;
        expirer(s);
        Donnees d = d(s);
        if (a.type == 3) {
            ItemStack main = p.getMainHandItem();
            if (main.isEmpty()) { dire(p, "Prends en main l'objet à vendre", ChatFormatting.RED); return; }
            if (a.prix < 1 || a.prix > 1_000_000_000L) { dire(p, "Prix invalide", ChatFormatting.RED); return; }
            if (d.offres.stream().filter(o -> o.vendeur.equals(p.getUUID())).count() >= MAX_OFFRES) { dire(p, "Maximum " + MAX_OFFRES + " offres en même temps", ChatFormatting.RED); return; }
            long f = frais(a.prix);
            if (!Economie.debiter(p.getUUID(), f)) { dire(p, "Il faut " + Economie.format(f) + " $ de frais de mise en vente", ChatFormatting.RED); return; }
            Offre o = new Offre();
            o.id = UUID.randomUUID(); o.vendeur = p.getUUID(); o.nomVendeur = p.getGameProfile().getName();
            o.objet = main.copy(); o.prix = a.prix; o.expire = System.currentTimeMillis() + DUREE_MS;
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            d.offres.add(o);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .8f, 1.3f);
            dire(p, "✔ Mis en vente pour " + Economie.format(a.prix) + " $ (frais : " + Economie.format(f) + " $)", ChatFormatting.GREEN);
        } else {
            Offre o = d.offres.stream().filter(x -> x.id.equals(a.offre)).findFirst().orElse(null);
            if (o == null) { dire(p, "Cette offre n'existe plus", ChatFormatting.RED); envoyerVue(p); return; }
            boolean mienne = o.vendeur.equals(p.getUUID());
            if (a.type == 2 && mienne) {
                d.offres.remove(o); donner(p, o.objet);
                dire(p, "Offre retirée, objet rendu (les frais ne sont pas remboursés)", ChatFormatting.GRAY);
            } else if (a.type == 1 && !mienne) {
                if (!Economie.debiter(p.getUUID(), o.prix)) { dire(p, "Pas assez d'argent (" + Economie.format(o.prix) + " $)", ChatFormatting.RED); Economie.envoyerSolde(p); return; }
                d.offres.remove(o); donner(p, o.objet);
                long gain = o.prix - taxe(o.prix);
                Economie.d().argent.merge(o.vendeur, gain, Long::sum); Economie.d().setDirty();   // marche aussi si le vendeur est hors ligne
                ServerPlayer v = s.getPlayerList().getPlayer(o.vendeur);
                if (v != null) {
                    v.sendSystemMessage(Component.literal("💰 " + p.getGameProfile().getName() + " a acheté ton offre : +" + Economie.format(gain) + " $ (taxe 5 % déduite)").withStyle(ChatFormatting.GOLD));
                    Economie.envoyerSolde(v);
                }
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .7f, 1.2f);
                dire(p, "✔ Acheté pour " + Economie.format(o.prix) + " $", ChatFormatting.GREEN);
            } else return;
        }
        d.setDirty();
        envoyerVue(p);
    }

    static void init() {
        Metiers.CANAL.messageBuilder(Vue.class, 8, NetworkDirection.PLAY_TO_CLIENT).encoder(Vue::ecrire).decoder(Vue::lire)
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEco.hdv(m))).add();
        Metiers.CANAL.messageBuilder(Action.class, 9, NetworkDirection.PLAY_TO_SERVER).encoder(Action::ecrire).decoder(Action::lire)
                .consumerMainThread((m, ctx) -> { ServerPlayer p = ctx.get().getSender(); if (p != null) action(p, m); }).add();
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            List<ItemStack> l = d(p.server).aRendre.remove(p.getUUID());
            if (l == null) return;
            l.forEach(i -> donner(p, i));
            d(p.server).setDirty();
            p.sendSystemMessage(Component.literal("↩ " + l.size() + " offre(s) de l'hôtel des ventes ont expiré : objets rendus.").withStyle(ChatFormatting.GRAY));
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> { if (e.phase == TickEvent.Phase.END && e.getServer().getTickCount() % 1200 == 0) expirer(e.getServer()); });
    }

    private HotelVentes() {}
}
