package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CAvatarDeletedPacket;
import org.figuramc.figura.server.packets.s2c.S2CAvatarReadyPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CAvatarDeletedPacketHandler extends ConnectedPacketHandler<S2CAvatarDeletedPacket>  {
    @Override
    protected void handlePacket(S2CAvatarDeletedPacket packet) {
        FSB.instance().handleAvatarDeleted();
    }

    @Override
    public S2CAvatarDeletedPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CAvatarDeletedPacket(byteBuf);
    }
}
