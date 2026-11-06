package fr.astralys.carte;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Test visuel (seulement avec ASTRALYS_CAPTURES) : ouvre chaque ecran avec des donnees d'exemple et en fait une capture. */
final class Captures {
    private record Etape(String nom, Runnable donnees, Supplier<Screen> ecran) {}

    private static final List<Etape> ETAPES = new ArrayList<>();
    private static int i = -1, attente;

    static void init() {
        ETAPES.add(new Etape("metiers", Captures::exemple, MetiersScreen::new));
        ETAPES.add(new Etape("metier_mineur", Captures::exemple, () -> new MetierScreen(0)));
        ETAPES.add(new Etape("metier_hunter", Captures::exemple, () -> new MetierScreen(2)));
        ETAPES.add(new Etape("boutique", Captures::exemple, ShopScreen::new));
        ETAPES.add(new Etape("faction", Captures::exemple, FactionScreen::new));
        ETAPES.add(new Etape("faction_vide", () -> { exemple(); ClientEco.faction = new Economie.FactionVue("", "", List.of(), List.of(), 0, 0, 0, 0, 0, List.of(),
                List.of("Dragons|1840", "Lumina|1320", "Givre|960", "Braises|410", "Sylves|120")); }, FactionScreen::new));
        ETAPES.add(new Etape("carte", Captures::exemple, CarteScreen::new));
        ETAPES.add(new Etape("classement", () -> { exemple(); ClientEco.classement = new Economie.Classement(List.of(
                List.of("Lyra|63|512000", "Dev|47|48000", "Kaelen|31|120400", "Nox|12|9100", "Mira|4|820"),
                List.of("Mira|22|60100", "Tobias|9|6000"), List.of("Nox|38|199000", "Lyra|30|116000", "Dev|8|2300", "Sora|2|300"), List.of())); }, ClassementScreen::new));
        ETAPES.add(new Etape("hdv", () -> { exemple(); var o = new java.util.ArrayList<HotelVentes.OffreVue>();
                String[] it = {"diamond_sword", "netherite_ingot", "elytra", "golden_apple", "oak_log", "beacon", "enchanted_book", "totem_of_undying", "iron_block", "ender_pearl", "shulker_box"};
                for (int k = 0; k < it.length; k++) o.add(new HotelVentes.OffreVue(java.util.UUID.randomUUID(), new net.minecraft.world.item.ItemStack(
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(it[k])), k == 4 ? 64 : 1), 150L * (k + 1) * (k + 3), k % 3 == 0 ? "Lyra" : "Nox", 3000 - k * 200, false));
                ClientEco.hdv = new HotelVentes.Vue(o, 2); }, HdvScreen::new));
        MinecraftForge.EVENT_BUS.addListener(Captures::tick);
    }

    private static void exemple() {
        ClientMetiers.vue = new Metiers.Vue(new long[] {48000, 9100, 2300, 450}, 4.6f, List.of(
                new Metiers.QueteVue("Mineur de fer", "Casser 32 minerais de fer", 21, 32, 0, 150),
                new Metiers.QueteVue("Nettoyeur", "Tuer 30 monstres", 30, 30, 2, 200),
                new Metiers.QueteVue("Apprenti alchimiste", "Préparer 3 potions", 1, 3, 3, 200)));
        ClientEco.solde = 125430;
        List<Economie.Ligne> l = new ArrayList<>();
        int n = 0;
        for (Economie.Article a : Economie.ARTICLES) {
            double f = 1 + Math.sin(n++ * 1.7) * .12;
            l.add(new Economie.Ligne(a.item(), a.categorie(), Economie.achat(a.base(), f), Economie.vente(a.base(), f), (float) (Math.sin(n * 2.3) * 6)));
        }
        ClientEco.marche = new Economie.Marche(List.of(Economie.CATEGORIES), l);
        ClientEco.faction = new Economie.FactionVue("Dragons", "Dev", List.of("Dev", "Lyra", "Kaelen", "Nox", "Mira", "Tobias", "Sora"),
                List.of(true, true, false, true, false, false, true), 84250, 1840, 14, 26, 4000,
                List.of(new Economie.DefiVue("Mine collective", "Casser 1500 minerais", 1500, 1500, 20000, 100),
                        new Economie.DefiVue("Grande moisson", "Récolter 2000 cultures", 1240, 2000, 15000, 80),
                        new Economie.DefiVue("Marchands", "Vendre pour 50 000 $ au shop", 8700, 50000, 25000, 120)),
                List.of("Dragons|1840", "Lumina|1320", "Givre|960", "Braises|410", "Sylves|120"));
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (i < 0) { if (mc.screen instanceof TitleScreen && ++attente > 100) { i = 0; attente = 0; } return; }
        if (i >= ETAPES.size()) { monde(mc); return; }
        Etape et = ETAPES.get(i);
        if (attente == 0) { et.donnees.run(); mc.setScreen(et.ecran.get()); }
        // la souris sur un element pour voir les survols
        if (++attente == 60) Screenshot.grab(mc.gameDirectory, "astralys_" + et.nom + ".png", mc.getMainRenderTarget(), c -> {});
        if (attente > 70) { i++; attente = 0; }
    }

    // Ensuite : un monde plat de test pour capturer l'inventaire (normal, puis souris sur le bouton Metiers).
    private static int phase, attenteMonde;

    private static void monde(Minecraft mc) {
        if (phase == 0) {
            phase = 1;
            var reglages = new net.minecraft.world.level.LevelSettings("captures", net.minecraft.world.level.GameType.SURVIVAL, false,
                    net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("captures", reglages, new net.minecraft.world.level.levelgen.WorldOptions(0L, false, false),
                    ra -> ra.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null) { if (++attenteMonde > 6000) mc.stop(); return; }
        attenteMonde++;
        if (phase == 1 && attenteMonde > 200) { phase = 2; attenteMonde = 0; mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
        if (phase == 2 && attenteMonde == 60) Screenshot.grab(mc.gameDirectory, "astralys_inventaire.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 70 && mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen s) {
            double f = mc.getWindow().getGuiScale();
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), (s.getGuiLeft() + 176 + 40) * f, (s.getGuiTop() + 20) * f);
        }
        if (phase == 2 && attenteMonde == 110) Screenshot.grab(mc.gameDirectory, "astralys_inventaire_survol.png", mc.getMainRenderTarget(), c -> {});
        // barre d'action seule, puis inventaire creatif
        if (phase == 2 && attenteMonde == 140) { mc.setScreen(null); var objets = Objets.ITEMS.getEntries().stream().toList();
            for (int k = 0; k < objets.size(); k++) mc.player.getInventory().items.set(k, new net.minecraft.world.item.ItemStack(objets.get(k).get())); }
        if (phase == 2 && attenteMonde == 190) Screenshot.grab(mc.gameDirectory, "astralys_barre.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 200) mc.player.connection.sendCommand("gamemode creative");
        if (phase == 2 && attenteMonde == 240) mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
        if (phase == 2 && attenteMonde == 300) Screenshot.grab(mc.gameDirectory, "astralys_creatif.png", mc.getMainRenderTarget(), c -> {});
        // les 5 PNJ alignes devant le joueur
        if (phase == 2 && attenteMonde == 320) {
            mc.setScreen(null);
            var srv = mc.getSingleplayerServer(); var pos = mc.player.position();
            srv.execute(() -> {
                var lvl = srv.overworld();
                srv.getPlayerList().getPlayers().get(0).connection.teleport(pos.x, pos.y, pos.z, 0, 8);
                for (int k = 0; k < PnjEntity.SKINS.length; k++) {
                    var e = Objets.PNJ.get().create(lvl);
                    e.moveTo(pos.x + (k - 2) * 1.5, pos.y, pos.z + 5, 180, 0); e.setYHeadRot(180); e.sorte(k);
                    lvl.addFreshEntity(e);
                }
            });
        }
        if (phase == 2 && attenteMonde == 420) Screenshot.grab(mc.gameDirectory, "astralys_pnj.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde > 440) mc.stop();
    }

    private Captures() {}
}
