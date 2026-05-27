package cn.kafei.TeamGlowing.network;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.marker.SharedMarkerKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class SetSharedMarkerRequest implements CustomPacketPayload {
    public static final Type<SetSharedMarkerRequest> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(TeamGlowingConstants.MODID, "set_shared_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetSharedMarkerRequest> CODEC = StreamCodec.ofMember(SetSharedMarkerRequest::write, SetSharedMarkerRequest::new);

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

    public SetSharedMarkerRequest(RegistryFriendlyByteBuf buf) {
        this.kind = SharedMarkerKind.fromOrdinal(buf.readVarInt());
        this.dimensionId = buf.readUtf();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.targetEntityId = buf.readUtf();
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.kind.ordinal());
        buf.writeUtf(this.dimensionId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeUtf(this.targetEntityId);
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
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
