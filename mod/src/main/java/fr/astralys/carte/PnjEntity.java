package fr.astralys.carte;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** PNJ du spawn : immobile, invulnerable ; clic droit = ouvre son ecran (boutique, metiers, faction, hotel des ventes, carte) ou donne sa quete. */
public class PnjEntity extends PathfinderMob {
    static final String[] SKINS = {"marchand", "maitre_metiers", "heraut", "commissaire", "guide", "archimage", "maitre_forge", "enchanteresse"};
    static final String[] NOMS = {"Marchand", "Maître des métiers", "Héraut des factions", "Commissaire-priseur", "Guide d'Astralys",
            "Archimage Elyndor", "Forgeronne Brunhild", "Enchanteresse Sylène"};
    static final int[] COULEURS = {0xF0B429, 0xE8322B, 0xB46CFF, 0x3DD6C5, 0x5AA8FF, 0x8FB8FF, 0xFF8A3D, 0x3DE0A0};
    static final int[] ECRANS = {0, 2, 1, 4, 5, -1, -1, -1};   // codes de Economie.Ouvrir ; -1 = PNJ a quetes
    private static final String[] EN_MAIN = {"minecraft:emerald", "minecraft:golden_pickaxe", "minecraft:white_banner", "minecraft:bell", "minecraft:filled_map",
            "astralyscarte:baton_foudre", "astralyscarte:marteau_forgeron", "astralyscarte:grimoire_astralys"};
    private static final EntityDataAccessor<Integer> SORTE = SynchedEntityData.defineId(PnjEntity.class, EntityDataSerializers.INT);

    public PnjEntity(EntityType<? extends PnjEntity> t, Level l) {
        super(t, l);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SORTE, 0); }

    int sorte() { return Math.floorMod(entityData.get(SORTE), SKINS.length); }

    void sorte(int t) {
        entityData.set(SORTE, t);
        setCustomName(Component.literal(NOMS[t]).withStyle(st -> st.withColor(COULEURS[t]).withBold(true)));
        setCustomNameVisible(true);
        setItemSlot(EquipmentSlot.MAINHAND, Metiers.pile(EN_MAIN[t]));
        setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player j, InteractionHand main) {
        if (main == InteractionHand.MAIN_HAND && j instanceof ServerPlayer sp) { if (ECRANS[sorte()] < 0) QuetesPnj.parler(sp, this); else Economie.ouvrir(sp, ECRANS[sorte()]); }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("Type", sorte()); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); sorte(t.getInt("Type")); }
}
