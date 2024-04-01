package org.figuramc.figura.utils.forge;

import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import org.figuramc.figura.utils.FiguraResourceListener;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class FiguraResourceListenerImpl extends FiguraResourceListener implements ISelectiveResourceReloadListener {
    public FiguraResourceListenerImpl(String id, Consumer<IResourceManager> reloadConsumer) {
        super(id, reloadConsumer);
    }

    @Override
    public FiguraResourceListener createResourceListener(String id, Consumer<IResourceManager> reloadConsumer) {
        return new FiguraResourceListenerImpl(id, reloadConsumer);
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager, Predicate<IResourceType> predicate) {
        reloadConsumer().accept(manager);
    }
}
