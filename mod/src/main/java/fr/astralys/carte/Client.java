package fr.astralys.carte;

import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

/** Tout ce qui ne tourne que chez le joueur (carte, touche M, liste des serveurs). */
class Client {
    static final String NOM_SERVEUR = "ASTRALYS";
    static final String ADRESSE = "gm57-dc02.ouiheberg.com:3218";

    static final KeyMapping TOUCHE = new KeyMapping("key.astralyscarte.carte",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.astralys");

    /** Ouverture demandee par /carte : faite au tick suivant, une fois le chat ferme. */
    private boolean ouvrir;
    private boolean clavierVerifie;

    Client() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::touches);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Client::packInterface);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::touche);
        MinecraftForge.EVENT_BUS.addListener(this::commande);
        MinecraftForge.EVENT_BUS.addListener(this::listeAvant);
        MinecraftForge.EVENT_BUS.addListener(this::pause);
        ClientMetiers.init();
        BarreMagie.init();
        if (System.getenv("ASTRALYS_CAPTURES") != null) Captures.init();
        MinecraftForge.EVENT_BUS.addListener(this::listeRendu);
    }

    // Inventaire et barre d'action d'Astralys : pack obligatoire, toujours au-dessus de "vanilla" (meme si le joueur a change l'ordre).
    private static void packInterface(net.minecraftforge.event.AddPackFindersEvent e) {
        if (e.getPackType() != net.minecraft.server.packs.PackType.CLIENT_RESOURCES) return;
        var dossier = net.minecraftforge.fml.ModList.get().getModFileById(AstralysCarte.MODID).getFile().findResource("interface_astralys");
        net.minecraft.server.packs.repository.Pack.ResourcesSupplier res = id -> new net.minecraft.server.packs.PathPackResources(id, dossier, true);
        var info = net.minecraft.server.packs.repository.Pack.readPackInfo("astralys_interface", res);
        if (info == null) return;
        var pack = net.minecraft.server.packs.repository.Pack.create("astralys_interface", Component.literal("Astralys - interface"), true, res, info,
                net.minecraft.server.packs.PackType.CLIENT_RESOURCES, net.minecraft.server.packs.repository.Pack.Position.TOP, true,
                net.minecraft.server.packs.repository.PackSource.BUILT_IN);
        e.addRepositorySource(c -> c.accept(pack));
    }

    private void touches(RegisterKeyMappingsEvent event) {
        event.register(TOUCHE);
    }

    private static void carte() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) mc.setScreen(new CarteScreen());
    }

    // Lecture directe de la touche : marche meme si un autre mod utilise deja M.
    private void touche(InputEvent.Key event) {
        InputConstants.Key k = TOUCHE.getKey();
        if (event.getAction() == GLFW.GLFW_PRESS && k.getType() == InputConstants.Type.KEYSYM && k.getValue() == event.getKey()) carte();
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!clavierVerifie) { clavierVerifie = true; toucheMSurAzerty(); }
        while (TOUCHE.consumeClick()) carte();
        if (ouvrir && Minecraft.getInstance().screen == null) {
            ouvrir = false;
            carte();
        }
    }

    // GLFW nomme les touches selon un clavier QWERTY : sur un AZERTY, la touche "M" du QWERTY est la virgule.
    // Si le joueur n'a pas change la touche, on la place sur la touche qui affiche vraiment "m" sur son clavier.
    private static void toucheMSurAzerty() {
        if (!TOUCHE.isDefault()) return;
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_GRAVE_ACCENT; k++) {
            if (GLFW.glfwGetKeyScancode(k) <= 0 || !"m".equalsIgnoreCase(GLFW.glfwGetKeyName(k, 0))) continue;
            if (k != GLFW.GLFW_KEY_M) {
                TOUCHE.setKey(InputConstants.Type.KEYSYM.getOrCreate(k));
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            return;
        }
    }

    private void commande(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("carte").executes(c -> { ouvrir = true; return 1; }));
    }

    static boolean estAstralys(ServerData s) {
        if (s.ip == null) return false;
        String ip = s.ip.toLowerCase(Locale.ROOT).trim();
        // le nom d'hote ou son adresse IP (certains joueurs l'ont enregistre ainsi)
        return ip.startsWith("gm57-dc02.ouiheberg.com") || ip.equals("45.140.164.235:3218");
    }

    // Menu Echap : pas de bouton "Mods" (la liste des mods du serveur reste privee).
    private void pause(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        for (GuiEventListener w : List.copyOf(event.getListenersList())) {
            if (!(w instanceof AbstractWidget b)) continue;
            Component m = b.getMessage();
            boolean mods = m.getContents() instanceof TranslatableContents t ? t.getKey().equals("fml.menu.mods") : "Mods".equalsIgnoreCase(m.getString());
            if (mods) event.removeListener(w);
        }
    }

    // Liste des serveurs : ASTRALYS y est toujours, sous son nom.
    private void listeAvant(ScreenEvent.Init.Pre event) {
        if (!(event.getScreen() instanceof JoinMultiplayerScreen)) return;
        ServerList liste = new ServerList(Minecraft.getInstance());
        liste.load();
        boolean change = false, present = false;
        for (int i = 0; i < liste.size(); i++) {
            ServerData s = liste.get(i);
            if (!estAstralys(s)) continue;
            present = true;
            if (!NOM_SERVEUR.equals(s.name)) { s.name = NOM_SERVEUR; change = true; }
        }
        if (!present) { liste.add(new ServerData(NOM_SERVEUR, ADRESSE, false), false); change = true; }
        if (change) liste.save();
    }

    // Le nombre de joueurs (et la liste au survol) n'est pas affiche pour ASTRALYS.
    private void listeRendu(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof JoinMultiplayerScreen ecran)) return;
        ServerList liste = ecran.getServers();
        if (liste == null) return;
        for (int i = 0; i < liste.size(); i++) {
            ServerData s = liste.get(i);
            if (!estAstralys(s)) continue;
            s.name = NOM_SERVEUR;
            s.status = Component.empty();
            s.playerList = List.of();
        }
    }
}
