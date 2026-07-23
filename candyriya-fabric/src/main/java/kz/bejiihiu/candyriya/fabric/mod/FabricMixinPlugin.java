package kz.bejiihiu.candyriya.fabric.mod;

import kz.bejiihiu.candyriya.api.CandyriyaPlatform;
import kz.bejiihiu.candyriya.boot.AbstractBootstrap;
import kz.bejiihiu.candyriya.common.mod.CandyriyaCommon;
import kz.bejiihiu.candyriya.common.mod.CandyriyaMixinPlugin;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import kz.bejiihiu.candyriya.i18n.CandyriyaLocale;
import kz.bejiihiu.candyriya.mixin.MixinTools;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;

public class FabricMixinPlugin extends CandyriyaMixinPlugin implements AbstractBootstrap {

    @Override
    public void onLoad(String mixinPackage) {
        CandyriyaCommon.setInstance(new FabricCommonImpl());
        super.onLoad(mixinPackage);
        MixinTools.setup();
        LoggerFactory.getLogger("Candyriya").info(
            CandyriyaLocale.getInstance().format("i18n.using-language", CandyriyaConfig.spec().getLocale().getCurrent(), CandyriyaConfig.spec().getLocale().getFallback())
        );
        try {
            this.setupMod(CandyriyaPlatform.FABRIC, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(LogManager::shutdown, "log flusher"));
    }
}
