package com.minelittlepony.unicopia.mixin;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minelittlepony.unicopia.server.world.BlockDestructionManager;
import com.minelittlepony.unicopia.server.world.NocturnalSleepManager;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;

@Mixin(ServerWorld.class)
abstract class MixinServerWorld extends World implements StructureWorldAccess, NocturnalSleepManager.Source {

    private NocturnalSleepManager nocturnalSleepManager;

    MixinServerWorld() { super(null, null, null, null, null, false, false, 0, 0); }

    @Inject(method = "onBlockChanged", at = @At("HEAD"))
    private void onOnBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo info) {
        ((BlockDestructionManager.Source)this).getDestructionManager().onBlockChanged(pos, oldState, newState);
    }

    @ModifyConstant(method = "sendSleepingStatus()V", constant = @Constant(
            stringValue = "sleep.skipping_night"
    ))
    private String modifySleepingMessage(String initial) {
        return getNocturnalSleepManager().getTimeSkippingMessage(initial);
    }

    @Override
    public NocturnalSleepManager getNocturnalSleepManager() {
        if (nocturnalSleepManager == null) {
            nocturnalSleepManager = new NocturnalSleepManager((ServerWorld)(Object)this);
        }
        return nocturnalSleepManager;
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(
        value = "INVOKE",
        target = "net/minecraft/server/world/ServerWorld.wakeSleepingPlayers()V"
    ))
    public void beforeWakeup(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        getNocturnalSleepManager().skipTime();
    }
}

@Mixin(SleepManager.class)
abstract class MixinSleepManager {
    @ModifyVariable(method = "update(Ljava/util/List;)Z", at = @At("HEAD"))
    public List<ServerPlayerEntity> modifyPlayers(List<ServerPlayerEntity> players) {
        return players.size() <= 0 ? players : ((NocturnalSleepManager.Source)players.get(0).getWorld()).getNocturnalSleepManager().filterPlayers(players);
    }
}