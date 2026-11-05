package fr.astralys.carte;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.lwjgl.glfw.GLFW;

@Mod(AstralysCarte.MODID)
public class AstralysCarte {
    public static final String MODID = "astralyscarte";

    static final KeyMapping TOUCHE = new KeyMapping("key.astralyscarte.carte",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.astralys");

    public AstralysCarte() {
        // Mod cote joueur : rien a faire sur un serveur dedie.
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::touches);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::clicDroit);
    }

    private void touches(RegisterKeyMappingsEvent event) {
        event.register(TOUCHE);
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (TOUCHE.consumeClick()) {
            if (mc.player != null && mc.screen == null) mc.setScreen(new CarteScreen());
        }
    }

    // L'objet "Carte d'Astralys" donne par le serveur porte le tag astralys_carte.
    private void clicDroit(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        var tag = event.getItemStack().getTag();
        if (tag == null || !tag.getBoolean("astralys_carte")) return;
        Minecraft.getInstance().setScreen(new CarteScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
