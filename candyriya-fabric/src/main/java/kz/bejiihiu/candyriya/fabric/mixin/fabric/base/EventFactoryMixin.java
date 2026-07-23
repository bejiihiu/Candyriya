package kz.bejiihiu.candyriya.fabric.mixin.fabric.base;

import kz.bejiihiu.candyriya.fabric.mod.event.FabricEventAdaptor;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import net.fabricmc.fabric.api.event.EventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(EventFactory.class)
public class EventFactoryMixin {

    @Decorate(method = "createArrayBacked(Ljava/lang/Class;Ljava/util/function/Function;)Lnet/fabricmc/fabric/api/event/Event;", inject = true, at = @At("HEAD"), remap = false)
    private static<T> void Candyriya$fabric$injectMonitor(Class<? super T> type, Function<T[], T> invokerFactory) throws Throwable {
        invokerFactory = FabricEventAdaptor.monitored(type, invokerFactory);
        DecorationOps.blackhole().invoke(invokerFactory);
    }
}
