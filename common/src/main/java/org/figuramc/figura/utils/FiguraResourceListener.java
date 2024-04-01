package org.figuramc.figura.utils;

import net.minecraft.client.resources.IResourceManager;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class FiguraResourceListener {
    private final String id;
    private final Consumer<IResourceManager> reloadConsumer;
    protected FiguraResourceListener(String id, Consumer<IResourceManager> reloadConsumer) {
        this.id = id;
        this.reloadConsumer = reloadConsumer;
    }

    public abstract FiguraResourceListener createResourceListener(String id, Consumer<IResourceManager> reloadConsumer);

    public String id() {
        return id;
    }

    public Consumer<IResourceManager> reloadConsumer() {
        return reloadConsumer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        FiguraResourceListener that = (FiguraResourceListener) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.reloadConsumer, that.reloadConsumer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reloadConsumer);
    }

    @Override
    public String toString() {
        return "FiguraResourceListener[" +
                "id=" + id + ", " +
                "reloadConsumer=" + reloadConsumer + ']';
    }

}
