package org.figuramc.figura.ducks;

import java.util.UUID;

public interface ISoundAccessor {

    UUID getFigura$owner();
    void setFigura$owner(UUID uuid);

    String getFigura$name();
    void setFigura$name(String figura$name);
}
