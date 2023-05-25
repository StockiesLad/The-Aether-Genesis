package com.aetherteam.aether_genesis.mixin.mixins.common;

import com.aetherteam.aether.block.portal.AetherPortalBlock;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AetherPortalBlock.class)
public class AetherPortalBlockMixin {
    @Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt" +
            "(I)I", ordinal = 0))
    private int bumpBound(RandomSource random, int i) {
        return random.nextInt(150);
    }
}
