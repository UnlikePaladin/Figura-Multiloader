package org.figuramc.figura.utils;

public interface FiguraModMetadata {
    String getCustomValueAsString(String key);
    Number getCustomValueAsNumber(String key);
    Boolean getCustomValueAsBoolean(String key);
    Object getCustomValueAsObject(String key);

    Version getModVersion();

    String getModId();

    FiguraModMetadata getMetadataForMod(String modID);
}
