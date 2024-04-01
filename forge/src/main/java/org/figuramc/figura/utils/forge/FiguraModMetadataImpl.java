package org.figuramc.figura.utils.forge;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.figuramc.figura.utils.FiguraModMetadata;
import org.figuramc.figura.utils.Version;

public class FiguraModMetadataImpl implements FiguraModMetadata {
    private final ModContainer modInfo;
    private String modID;
    protected FiguraModMetadataImpl(String modID) {
        this.modID = modID;
        this.modInfo =  Loader.instance().getIndexedModList().get(modID);
    }

    public FiguraModMetadata getMetadataForMod(String modID) {
        return new FiguraModMetadataImpl(modID);
    }

    @Override
    public String getCustomValueAsString(String key) {
        return modInfo.getCustomModProperties().get(key).toString();
    }

    @Override
    public Number getCustomValueAsNumber(String key) {
        return Double.parseDouble(modInfo.getCustomModProperties().get(key));
    }

    @Override
    public Boolean getCustomValueAsBoolean(String key) {
        return Boolean.parseBoolean(modInfo.getCustomModProperties().get(key));
    }

    @Override
    public Object getCustomValueAsObject(String key) {
        return modInfo.getCustomModProperties().get(key);
    }


    @Override
    public Version getModVersion() {
        return new Version(modInfo.getVersion().toString());
    }

    @Override
    public String getModId() {
        return modID;
    }
}
