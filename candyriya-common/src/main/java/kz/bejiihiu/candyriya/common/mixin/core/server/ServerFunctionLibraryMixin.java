package kz.bejiihiu.candyriya.common.mixin.core.server;

import net.minecraft.server.ServerFunctionLibrary;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerFunctionLibrary.class)
public class ServerFunctionLibraryMixin {

    @Redirect(
        method = "reload",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            remap = false
        ),
        require = 0
    )
    private static void Candyriya$downgradeFunctionParseError(Logger logger, String message, Object functionName, Object throwable) {
        if (throwable instanceof Throwable t) {
            Throwable cause = t;
            if (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) {
                cause = t.getCause();
            }
            
            if (cause instanceof IllegalArgumentException) {
                String errorMsg = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
                logger.warn("Failed to load function {}: {}", functionName, errorMsg);
                return;
            }
        }
        logger.error(message, functionName, throwable);
    }
}
