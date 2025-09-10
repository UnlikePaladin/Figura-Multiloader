package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.nio.charset.StandardCharsets;

public class S2CAvatarReadyPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/avatar_ready");
    public final String avatarId;
    public final EHashPair ref;

    public S2CAvatarReadyPacket(String avatarId, EHashPair ref) {
        this.avatarId = avatarId;
        this.ref = ref;
    }

    public S2CAvatarReadyPacket(IFriendlyByteBuf buf) {
        this.avatarId = new String(buf.readByteArray(256), StandardCharsets.UTF_8);
        Hash hash = buf.readHash();
        Hash ehash = buf.readHash();
        this.ref = new EHashPair(hash, ehash);
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeByteArray(avatarId.getBytes(StandardCharsets.UTF_8));
        buf.writeBytes(ref.hash().get());
        buf.writeBytes(ref.ehash().get());
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
