package kz.bejiihiu.candyriya.forge.mod;

import kz.bejiihiu.candyriya.common.mod.CandyriyaCommon;
import kz.bejiihiu.candyriya.common.mod.CandyriyaMixinPlugin;

public class ForgeMixinPlugin extends CandyriyaMixinPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        CandyriyaCommon.setInstance(new ForgeCommonImpl());
    }
}
