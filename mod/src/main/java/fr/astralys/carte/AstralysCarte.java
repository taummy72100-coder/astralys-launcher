package fr.astralys.carte;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AstralysCarte.MODID)
public class AstralysCarte {
    public static final String MODID = "astralyscarte";
    static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public AstralysCarte() {
        // Chez le joueur : la carte. Sur le serveur : la reinitialisation de Sylvaria.
        Objets.init();
        Metiers.init();
        Economie.init();
        Accueil.init();
        if (FMLEnvironment.dist == Dist.CLIENT) new Client();
        else ResetSylvaria.init();
    }
}
