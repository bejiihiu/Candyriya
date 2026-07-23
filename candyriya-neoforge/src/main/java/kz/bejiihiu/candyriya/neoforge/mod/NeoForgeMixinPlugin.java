package kz.bejiihiu.candyriya.neoforge.mod;

import kz.bejiihiu.candyriya.common.mod.CandyriyaCommon;
import kz.bejiihiu.candyriya.common.mod.CandyriyaMixinPlugin;

public class NeoForgeMixinPlugin extends CandyriyaMixinPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        CandyriyaCommon.setInstance(new NeoForgeCommonImpl());
    }
}
