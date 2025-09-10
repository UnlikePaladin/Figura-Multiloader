package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.nio.charset.StandardCharsets;

public class S2CAvatarDeletedPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/avatar_deleted");
    public final String avatarId;

    public S2CAvatarDeletedPacket(String avatarId) {
        this.avatarId = avatarId;
    }

    public S2CAvatarDeletedPacket(IFriendlyByteBuf buf) {
        this.avatarId = new String(buf.readByteArray(256), StandardCharsets.UTF_8);
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeByteArray(avatarId.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
