package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class SetSharedMarkerRequest implements CustomPayload {
    public static final Id<SetSharedMarkerRequest> ID = new Id<>(Identifier.of(TeamGlowingConstants.MODID, "set_shared_marker"));
    public static final PacketCodec<RegistryByteBuf, SetSharedMarkerRequest> CODEC = PacketCodec.of(SetSharedMarkerRequest::write, SetSharedMarkerRequest::new);

    private final SharedMarkerKind kind;
    private final String dimensionId;
    private final double x;
    private final double y;
    private final double z;
    private final String targetEntityId;

    public SetSharedMarkerRequest(SharedMarkerKind kind, String dimensionId, double x, double y, double z, String targetEntityId) {
        this.kind = kind;
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.targetEntityId = targetEntityId == null ? "" : targetEntityId;
    }

    public SetSharedMarkerRequest(RegistryByteBuf buf) {
        this.kind = SharedMarkerKind.fromOrdinal(buf.readVarInt());
        this.dimensionId = buf.readString();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.targetEntityId = buf.readString();
    }

    private void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.kind.ordinal());
        buf.writeString(this.dimensionId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeString(this.targetEntityId);
    }

    public SharedMarkerKind kind() {
        return this.kind;
    }

    public String dimensionId() {
        return this.dimensionId;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public String targetEntityId() {
        return this.targetEntityId;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
