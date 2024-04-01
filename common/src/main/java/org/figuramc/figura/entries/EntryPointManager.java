package org.figuramc.figura.entries;

import org.figuramc.figura.gui.widgets.PanelSelectorWidget;
import org.figuramc.figura.lua.FiguraAPIManager;
import org.figuramc.figura.lua.api.vanilla_model.VanillaModelAPI;
import org.figuramc.figura.lua.docs.FiguraDocsManager;
import org.figuramc.figura.permissions.PermissionManager;

import java.util.Set;

public abstract class EntryPointManager {

    public void init() {
        // APIs
        Set<FiguraAPI> apis = load("figura_api", FiguraAPI.class);
        FiguraAPIManager.initEntryPoints(apis);
        FiguraDocsManager.initEntryPoints(apis);

        // other
        PermissionManager.initEntryPoints(load("figura_permissions", FiguraPermissions.class));
        PanelSelectorWidget.initEntryPoints(load("figura_screen", FiguraScreen.class));
        VanillaModelAPI.initEntryPoints(load("figura_vanilla_part", FiguraVanillaPart.class));
    }

    // TODO :create a service for entry points
    protected abstract <T> Set<T> load(String name, Class<T> clazz);
}
