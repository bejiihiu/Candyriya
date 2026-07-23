package kz.bejiihiu.candyriya.common.mixin.core.server;

import net.minecraft.server.ServerFunctionLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerFunctionLibrary.class)
public class ServerFunctionLibraryMixin {

    @Inject(
        method = "method_29452",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void Candyriya$wrapFunctionLoading(
        com.google.common.collect.ImmutableMap.Builder builder,
        net.minecraft.resources.ResourceLocation functionId,
        java.util.concurrent.CompletableFuture<?> future,
        CallbackInfo ci
    ) {
        future.handle((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable;
                if (throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null) {
                    cause = throwable.getCause();
                }
                
                if (cause instanceof IllegalArgumentException) {
                    String errorMsg = cause.getMessage() != null ? cause.getMessage() : "Unknown error";
                    org.slf4j.LoggerFactory.getLogger(ServerFunctionLibrary.class)
                        .warn("Failed to load function {}: {}", functionId, errorMsg);
                    return null;
                }
                
                org.slf4j.LoggerFactory.getLogger(ServerFunctionLibrary.class)
                    .error("Failed to load function {}", functionId, throwable);
            } else if (result != null) {
                builder.put(functionId, result);
            }
            return null;
        }).join();
        ci.cancel();
    }
}
