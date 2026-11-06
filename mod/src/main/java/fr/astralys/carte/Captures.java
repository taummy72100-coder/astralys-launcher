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
        ETAPES.add(new Etape("niveau", Captures::exemple, NiveauScreen::new));
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
                new Metiers.QueteVue("Apprenti alchimiste", "Préparer 3 potions", 1, 3, 3, 200)), 150_000, 37);
        ClientEco.solde = 125430;
        List<Economie.Ligne> l = new ArrayList<>();
        int n = 0;
        for (Economie.Article a : Economie.ARTICLES) {
            if (a.categorie() != Economie.CLES) continue;
            double f = 1 + Math.sin(n++ * 1.7) * .12;
            l.add(new Economie.Ligne(a.item(), 0, Economie.achat(a.base(), f), Economie.vente(a.base(), f), (float) (Math.sin(n * 2.3) * 6)));
        }
        ClientEco.marche = new Economie.Marche(List.of(Economie.CATEGORIES[Economie.CLES]), l);
        var pr = new net.minecraft.nbt.CompoundTag();
        pr.putString("fond", "nebuleuse"); pr.putString("motif", "constellation"); pr.putString("embleme", "oeuf_dragon"); pr.putString("grade", "astral"); pr.putString("emote", "danse");
        ClientCosm.vue = new Cosmetiques.Vue(4850, List.of("chapeau_sorcier_violet", "jambieres_runiques_violet", "bottes_ailees_or", "casque_chevalier_royal", "couronne_or",
                "fond:nebuleuse", "motif:constellation", "embleme:oeuf_dragon", "grade:astral", "emote:danse", "emote:victoire"),
                java.util.Arrays.asList("chapeau_sorcier_violet", "", "jambieres_runiques_violet", "bottes_ailees_or"), pr);
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
    private static net.minecraft.world.phys.Vec3 origine = net.minecraft.world.phys.Vec3.ZERO;

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
        if (phase == 2 && attenteMonde == 140) { mc.setScreen(null);
            // barre : les objets en 3D d'abord, puis coffres, elixirs et le reste dans l'inventaire
            String[] vitrine = {"baton_foudre", "sceptre_givre", "baguette_soins", "baton_braise", "orbe_transfert", "marteau_forgeron", "hache_titan", "epee_runique", "elixir_vie",
                    "coffre_commun", "coffre_rare", "coffre_epique", "coffre_legendaire", "coffre_special", "caisse_astrale", "elixir_force", "elixir_titan", "elixir_ombre",
                    "elixir_celerite", "elixir_mineur", "elixir_phenix", "cle_rare", "cle_epique", "cle_legendaire", "bloc_fantome", "cristal_astral", "grimoire_astralys"};
            for (int k = 0; k < vitrine.length; k++) mc.player.getInventory().items.set(k, Metiers.pile("astralyscarte:" + vitrine[k])); }
        if (phase == 2 && attenteMonde == 190) Screenshot.grab(mc.gameDirectory, "astralys_barre.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 200) mc.player.connection.sendCommand("gamemode creative");
        if (phase == 2 && attenteMonde == 240) mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
        if (phase == 2 && attenteMonde == 300) Screenshot.grab(mc.gameDirectory, "astralys_creatif.png", mc.getMainRenderTarget(), c -> {});
        // les 5 PNJ alignes devant le joueur
        if (phase == 2 && attenteMonde == 320) {
            mc.setScreen(null);
            var srv = mc.getSingleplayerServer(); var pos = mc.player.position(); origine = pos;
            srv.execute(() -> {
                var lvl = srv.overworld();
                srv.getPlayerList().getPlayers().get(0).connection.teleport(pos.x, pos.y, pos.z, 0, 8);
                for (int k = 0; k < PnjEntity.SKINS.length; k++) {
                    var e = Objets.PNJ.get().create(lvl);
                    e.moveTo(pos.x + (k - 3.5) * 1.6, pos.y, pos.z + 8, 180, 0); e.setYHeadRot(180); e.sorte(k);
                    lvl.addFreshEntity(e);
                }
            });
        }
        if (phase == 2 && attenteMonde == 420) Screenshot.grab(mc.gameDirectory, "astralys_pnj.png", mc.getMainRenderTarget(), c -> {});
        // garde-robe (cosmetiques) avec un exemple, puis ouverture d'un coffre legendaire, puis le joueur vu de face
        if (phase == 2 && attenteMonde == 440) mc.setScreen(new CosmetiqueScreen());
        if (phase == 2 && attenteMonde == 460) exemple();
        if (phase == 2 && attenteMonde == 520) Screenshot.grab(mc.gameDirectory, "astralys_cosmetiques.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 540) {
            var rnd = net.minecraft.util.RandomSource.create(3);
            List<net.minecraft.world.item.ItemStack> bande = new ArrayList<>(); List<Integer> rar = new ArrayList<>();
            for (int k = 0; k < Coffres.TAILLE; k++) { var lot = Coffres.tirer(3, rnd); bande.add(Metiers.pile(lot.objet())); rar.add(lot.rarete()); }
            mc.setScreen(new CoffreScreen(new Coffres.Roulette(3, bande, rar, Coffres.GAGNANT)));
        }
        if (phase == 2 && attenteMonde == 552) Screenshot.grab(mc.gameDirectory, "astralys_coffre_cle.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 582) Screenshot.grab(mc.gameDirectory, "astralys_coffre_tremble.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 594) Screenshot.grab(mc.gameDirectory, "astralys_coffre_ouvert.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 660) Screenshot.grab(mc.gameDirectory, "astralys_coffre_roulette.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 760) Screenshot.grab(mc.gameDirectory, "astralys_coffre_gain.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 780) {
            mc.setScreen(null);
            ClientCosm.portes = java.util.Map.of(mc.player.getUUID(), ClientCosm.vue.portes());
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
        }
        if (phase == 2 && attenteMonde == 830) Screenshot.grab(mc.gameDirectory, "astralys_cosmetiques_porte.png", mc.getMainRenderTarget(), c -> {});
        // gros plans des PNJ (3 par 3), a la premiere personne
        if (phase == 2 && attenteMonde == 860) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        for (int g = 0; g < 3; g++) {
            if (phase == 2 && attenteMonde == 870 + g * 50) {
                double x = origine.x + (g * 3 - 3.5) * 1.6;
                var srv = mc.getSingleplayerServer();
                srv.execute(() -> srv.getPlayerList().getPlayers().get(0).connection.teleport(x, origine.y, origine.z + 5.6, 0, 12));
            }
            if (phase == 2 && attenteMonde == 905 + g * 50) Screenshot.grab(mc.gameDirectory, "astralys_pnj_proche" + g + ".png", mc.getMainRenderTarget(), c -> {});
        }
        // ecran des pets, ecran du broyeur, puis machines et pets poses dans le monde
        if (phase == 2 && attenteMonde == 1040) {
            ClientMetiers.vue = new Metiers.Vue(new long[] {250000, 9100, 2300, 450}, 4.6f, List.of(), 150_000, 37); Verrous.xpClient = ClientMetiers.vue.xp();
            mc.setScreen(new PetsScreen());
        }
        if (phase == 2 && attenteMonde == 1050) PetsScreen.vue = new Familiers.Vue(0b0001, 0);
        if (phase == 2 && attenteMonde == 1090) Screenshot.grab(mc.gameDirectory, "astralys_pets.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 1100) {
            var cont = new net.minecraft.world.SimpleContainer(3);
            cont.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 12));
            cont.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 20));
            cont.setItem(2, Metiers.pile("astralyscarte:poussiere_etoile"));
            var data = new net.minecraft.world.inventory.SimpleContainerData(2); data.set(0, 95); data.set(1, 160);
            mc.setScreen(new BroyeurScreen(new Machines.BroyeurMenu(0, mc.player.getInventory(), cont, data), mc.player.getInventory(), net.minecraft.network.chat.Component.literal("Broyeur astral")));
        }
        if (phase == 2 && attenteMonde == 1130) Screenshot.grab(mc.gameDirectory, "astralys_broyeur.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 1140) {
            mc.setScreen(null);
            var srv = mc.getSingleplayerServer();
            srv.execute(() -> {
                var l = srv.overworld();
                var o = net.minecraft.core.BlockPos.containing(origine.x, origine.y, origine.z - 6);
                var inc = o.offset(0, 0, 3);
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) l.setBlockAndUpdate(inc.offset(x, -1, z), net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState());
                for (int x = -1; x <= 1; x += 2) for (int z = -1; z <= 1; z += 2) l.setBlockAndUpdate(inc.offset(x, 0, z), net.minecraft.world.level.block.Blocks.COMPOSTER.defaultBlockState());
                l.setBlockAndUpdate(inc, Objets.INCUBATEUR.get().defaultBlockState());
                l.setBlockAndUpdate(inc.offset(-3, 0, 0), Objets.BROYEUR.get().defaultBlockState().setValue(Machines.Broyeur.LIT, true));
                l.setBlockAndUpdate(inc.offset(3, 0, 0), Objets.PIEGE.get().defaultBlockState());
                for (int k = 0; k < Familiers.IDS.length; k++) {
                    var p = Objets.PET.get().create(l);
                    p.sorte(k); p.moveTo(o.getX() + .5 + (k - 1.5) * 1.6, o.getY() + 1.15, o.getZ() + 2.2, 180, 0);
                    l.addFreshEntity(p); MONTURES[k] = p;
                }
                srv.getPlayerList().getPlayers().get(0).connection.teleport(o.getX() + .5, o.getY(), o.getZ() - .3, 0, 12);
            });
        }
        if (phase == 2 && attenteMonde == 1200) Screenshot.grab(mc.gameDirectory, "astralys_machines_pets.png", mc.getMainRenderTarget(), c -> {});
        // chaque pet monte, vu de face
        for (int k = 0; k < Familiers.IDS.length; k++) {
            final int kk = k;
            if (phase == 2 && attenteMonde == 1210 + k * 50) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(-30); mc.player.setXRot(28);
                var srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    var j = srv.getPlayerList().getPlayers().get(0);
                    j.stopRiding();
                    if (kk > 0) MONTURES[kk - 1].discard();
                    var p = MONTURES[kk];
                    p.moveTo(origine.x - 9, origine.y + .3, origine.z, 0, 0);
                    j.setYRot(-30); j.setXRot(28);
                    j.startRiding(p, true);
                });
            }
            if (phase == 2 && attenteMonde == 1250 + k * 50) Screenshot.grab(mc.gameDirectory, "astralys_monture" + k + ".png", mc.getMainRenderTarget(), c -> {});
        }
        // boutique (cles seules), coffres cosmetiques, vitrine des cosmetiques par onglet, roulette cosmetique, profil, emote en jeu
        if (phase == 2 && attenteMonde == 1430) {
            var srv = mc.getSingleplayerServer();
            srv.execute(() -> srv.getPlayerList().getPlayers().get(0).stopRiding());
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            exemple(); mc.setScreen(new ShopScreen());
        }
        if (phase == 2 && attenteMonde == 1435) exemple();
        if (phase == 2 && attenteMonde == 1470) Screenshot.grab(mc.gameDirectory, "astralys_boutique_cles.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 1480) mc.setScreen(new CoffresCosmScreen());
        if (phase == 2 && attenteMonde == 1485) exemple();
        if (phase == 2 && attenteMonde == 1520) Screenshot.grab(mc.gameDirectory, "astralys_coffres_cosm.png", mc.getMainRenderTarget(), c -> {});
        String[] onglets = {"selection", "chapeaux", "casques", "jambieres", "chaussures"};
        for (int k = 0; k < onglets.length; k++) {
            if (phase == 2 && attenteMonde == 1530 + k * 50) { var s = new CosmetiqueScreen(); s.onglet = k - 1; mc.setScreen(s); }
            if (phase == 2 && attenteMonde == 1535 + k * 50) exemple();
            if (phase == 2 && attenteMonde == 1575 + k * 50) Screenshot.grab(mc.gameDirectory, "astralys_cosm_" + onglets[k] + ".png", mc.getMainRenderTarget(), c -> {});
        }
        if (phase == 2 && attenteMonde == 1790) {
            var rnd = new java.util.Random(5);
            List<net.minecraft.world.item.ItemStack> bande = new ArrayList<>(); List<Integer> rar = new ArrayList<>();
            for (int k = 0; k < Coffres.TAILLE; k++) { var c = CatalogueCosm.LISTE.get(k == Coffres.GAGNANT ? 12 : rnd.nextInt(CatalogueCosm.LISTE.size())); bande.add(Cosmetiques.pile(c)); rar.add(c.rarete()); }
            mc.setScreen(new CoffreScreen(new Coffres.Roulette(3, bande, rar, Coffres.GAGNANT)));
        }
        if (phase == 2 && attenteMonde == 1910) Screenshot.grab(mc.gameDirectory, "astralys_coffre_cosm_roulette.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 2010) Screenshot.grab(mc.gameDirectory, "astralys_coffre_cosm_gain.png", mc.getMainRenderTarget(), c -> {});
        for (int k = 0; k < 4; k++) {
            if (phase == 2 && attenteMonde == 2020 + k * 50) { var s = new ProfilScreen(); s.onglet = k; mc.setScreen(s); }
            if (phase == 2 && attenteMonde == 2025 + k * 50) exemple();
            if (phase == 2 && attenteMonde == 2065 + k * 50) Screenshot.grab(mc.gameDirectory, "astralys_profil" + k + ".png", mc.getMainRenderTarget(), c -> {});
        }
        if (phase == 2 && attenteMonde == 2230) {
            mc.setScreen(null); exemple();
            ClientCosm.portes = java.util.Map.of(mc.player.getUUID(), ClientCosm.vue.portes());
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(10);
        }
        if (phase == 2 && attenteMonde == 2240) Emotes.jouer(mc.player.getUUID(), "victoire");
        if (phase == 2 && attenteMonde == 2270) Screenshot.grab(mc.gameDirectory, "astralys_emote_victoire.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde == 2300) Emotes.jouer(mc.player.getUUID(), "dab");
        if (phase == 2 && attenteMonde == 2335) Screenshot.grab(mc.gameDirectory, "astralys_emote_dab.png", mc.getMainRenderTarget(), c -> {});
        if (phase == 2 && attenteMonde > 2350) mc.stop();
    }

    private static final Familiers.Pet[] MONTURES = new Familiers.Pet[Familiers.IDS.length];

    private Captures() {}
}
