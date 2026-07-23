package kz.bejiihiu.candyriya.common.mixin.core.util;

import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringUtil.class)
public class StringUtilMixin {

    @Unique
    private static boolean Candyriya$validUsernameCheck(String name) {
        var regex = CandyriyaConfig.spec().getCompat().getValidUsernameRegex();
        return !regex.isBlank() && name.matches(regex);
    }

    @Inject(method = "isValidPlayerName", cancellable = true, at = @At("HEAD"))
    private static void Candyriya$checkUsername(String name, CallbackInfoReturnable<Boolean> cir) {
        if (Candyriya$validUsernameCheck(name)) {
            cir.setReturnValue(true);
        }
    }
}
