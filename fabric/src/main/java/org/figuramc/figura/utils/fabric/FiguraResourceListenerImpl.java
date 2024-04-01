package org.figuramc.figura.utils.fabric;

import net.legacyfabric.fabric.api.resource.IdentifiableResourceReloadListener;
import net.legacyfabric.fabric.api.util.Identifier;
import net.minecraft.client.resources.IResourceManager;
import org.figuramc.figura.utils.FiguraResourceListener;

import java.util.function.Consumer;

public class FiguraResourceListenerImpl extends FiguraResourceListener implements IdentifiableResourceReloadListener {
    public FiguraResourceListenerImpl(String id, Consumer<IResourceManager> reloadConsumer) {
        super(id, reloadConsumer);
    }

    @Override
    public FiguraResourceListener createResourceListener(String id, Consumer<IResourceManager> reloadConsumer) {
        return new FiguraResourceListenerImpl(id, reloadConsumer);
    }

    public Identifier getFabricId() {
        return new Identifier(this.id());
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        reloadConsumer().accept(manager);
    }
}
