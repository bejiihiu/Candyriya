package kz.bejiihiu.candyriya.neoforge.mixin.core.world.item.crafting;

import com.google.gson.Gson;
import kz.bejiihiu.candyriya.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_NeoForge extends SimpleJsonResourceReloadListener implements RecipeManagerBridge {

    public RecipeManagerMixin_NeoForge(Gson gson, String string) {
        super(gson, string);
    }
}
