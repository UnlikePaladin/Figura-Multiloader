package org.figuramc.figura.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.ICommand;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;


public interface FiguraClientCommandSource extends ICommand {
    /**
     * Sends a feedback message to the player.
     *
     * @param message the feedback message
     */
    void figura$sendFeedback(ITextComponent message);

    /**
     * Sends an error message to the player.
     *
     * @param message the error message
     */
    void figura$sendError(ITextComponent message);

    /**
     * Gets the client instance used to run the command.
     *
     * @return the client
     */
    Minecraft figura$getClient();

    /**
     * Gets the player that used the command.
     *
     * @return the player
     */
    EntityPlayerSP figura$getPlayer();

    /**
     * Gets the entity that used the command.
     *
     * @return the entity
     */
    default Entity figura$getEntity() {
        return figura$getPlayer();
    }

    /**
     * Gets the position from where the command has been executed.
     *
     * @return the position
     */
    default Vec3d figura$getPosition() {
        return figura$getPlayer().getPositionVector();
    }

    /**
     * Gets the world where the player used the command.
     *
     * @return the world
     */
    WorldClient figura$getWorld();

    /**
     * Gets the meta property under {@code key} that was assigned to this source.
     *
     * <p>This method should return the same result for every call with the same {@code key}.
     *
     * @param key the meta key
     * @return the meta
     */
    default Object figura$getMeta(String key) {
        return null;
    }
}