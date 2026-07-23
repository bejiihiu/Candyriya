package kz.bejiihiu.candyriya.common.mixin.core;

import kz.bejiihiu.candyriya.api.CandyriyaVersion;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import org.bukkit.craftbukkit.v.CraftCrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public class CrashReportMixin {

    @Shadow @Final private SystemReport systemReport;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Candyriya$additional(String string, Throwable throwable, CallbackInfo ci) {
        this.systemReport.setDetail("Candyriya Release", CandyriyaVersion.current()::getReleaseName);
        if (CandyriyaServer.isInitialized()) {
            this.systemReport.setDetail("Candyriya", new CraftCrashReport());
        } else {
            this.systemReport.setDetail("Candyriya", "The crash happens before the server initialization.");
        }
    }
}
