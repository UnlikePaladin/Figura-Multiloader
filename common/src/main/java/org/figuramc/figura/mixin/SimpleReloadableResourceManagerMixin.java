package org.figuramc.figura.mixin;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.figuramc.figura.resources.FiguraRuntimeResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SimpleReloadableResourceManager.class)
public class SimpleReloadableResourceManagerMixin {

    @ModifyVariable(at = @At(value = "HEAD"), method = "reloadResources", argsOnly = true)
    private List<IResourcePack> createReload(List<IResourcePack> packs) {
        List<IResourcePack> list = new ArrayList<>(packs);

        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            String id = list.get(i).getPackName();
            if ("Fabric Mods".equals(id) || "vanilla".equals(id))
                index = i + 1;
        }

        FiguraRuntimeResources.joinFuture();
        list.add(index, FiguraRuntimeResources.PACK);

        return list;
    }
}
