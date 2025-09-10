package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CAvatarReadyPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CAvatarReadyPacketHandler extends ConnectedPacketHandler<S2CAvatarReadyPacket>  {
    @Override
    protected void handlePacket(S2CAvatarReadyPacket packet) {
        FSB.instance().handleAvatarReady(packet);
    }

    @Override
    public S2CAvatarReadyPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CAvatarReadyPacket(byteBuf);
    }
}
