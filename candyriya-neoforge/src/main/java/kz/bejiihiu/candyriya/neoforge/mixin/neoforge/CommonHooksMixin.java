package kz.bejiihiu.candyriya.neoforge.mixin.neoforge;

import kz.bejiihiu.candyriya.common.mod.server.event.EntityEventHandler;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CommonHooks.class)
public abstract class CommonHooksMixin {

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("HEAD"))
    private static void Candyriya$captureHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        CandyriyaCaptures.capturePlaceEventHand(context.getHand());
    }

    @Inject(method = "onPlaceItemIntoWorld", remap = false, at = @At("RETURN"))
    private static void Candyriya$removeHand(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        CandyriyaCaptures.getPlaceEventHand(InteractionHand.MAIN_HAND);
    }

    @Decorate(method = "onLivingDrops", remap = false, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/entity/living/LivingDropsEvent;isCanceled()Z"))
    private static boolean Candyriya$monitorLivingDrops(LivingDropsEvent instance) throws Throwable {
        instance.setCanceled(EntityEventHandler.monitorLivingDrops(instance.getEntity(), instance.getSource(), (List<ItemEntity>) instance.getDrops(), instance.isCanceled()));
        return (boolean) DecorationOps.callsite().invoke(instance);
    }
}
