package cn.kafei.TeamGlowing.tracker;

import cn.kafei.TeamGlowing.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class TrackerBarService
{
    private static final int LOCATOR_BAR_WIDTH = 41;
    private static final int MAX_TRACKED_TEAMMATES = 4;
    private static final double MAX_TRACKING_ANGLE = 90.0D;

    public void updateTrackerBar(EntityPlayerMP player, PartyManager partyManager)
    {
        List<EntityPlayerMP> teammates = partyManager.getOnlineTeammates(player);
        if (teammates.isEmpty())
        {
            return;
        }

        List<TrackerMarker> markers = new ArrayList<>();
        for (EntityPlayerMP teammate : teammates)
        {
            if (markers.size() >= MAX_TRACKED_TEAMMATES)
            {
                break;
            }

            TrackerMarker marker = this.createTrackerMarker(player, teammate);
            if (marker != null)
            {
                markers.add(marker);
            }
        }

        if (markers.isEmpty())
        {
            return;
        }

        ITextComponent text = new TextComponentString(this.buildLocatorBar(markers));
        player.connection.sendPacket(new SPacketChat(text));
    }

    private TrackerMarker createTrackerMarker(EntityPlayerMP viewer, EntityPlayerMP teammate)
    {
        double dx = teammate.posX - viewer.posX;
        double dz = teammate.posZ - viewer.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double relativeAngle = this.getRelativeAngleDegrees(viewer, teammate);
        int slot = this.projectAngleToBarSlot(relativeAngle);
        char symbol = this.chooseMarkerSymbol(teammate.getName(), distance);
        String color = this.chooseMarkerColor(distance);
        return new TrackerMarker(teammate.getName(), distance, slot, symbol, color);
    }

    private String buildLocatorBar(List<TrackerMarker> markers)
    {
        String[] slots = new String[LOCATOR_BAR_WIDTH];
        for (int index = 0; index < slots.length; index++)
        {
            slots[index] = TextFormatting.DARK_GRAY + "-";
        }

        int center = LOCATOR_BAR_WIDTH / 2;
        slots[center] = TextFormatting.WHITE + "^";
        this.applyDirectionTicks(slots, center);

        for (TrackerMarker marker : markers)
        {
            int slot = marker.slot;
            String markerText = marker.color + marker.symbol;
            String existing = slots[slot];
            if (slot == center)
            {
                slots[slot] = markerText;
                continue;
            }
            if (!existing.endsWith("-") && !existing.endsWith("<") && !existing.endsWith(">") && !existing.endsWith("^"))
            {
                slots[slot] = marker.color + "*";
                continue;
            }
            slots[slot] = markerText;
        }

        StringBuilder barBuilder = new StringBuilder();
        barBuilder.append(TextFormatting.GRAY).append("[");
        for (String slot : slots)
        {
            barBuilder.append(slot);
        }
        barBuilder.append(TextFormatting.GRAY).append("] ");

        for (int index = 0; index < markers.size(); index++)
        {
            TrackerMarker marker = markers.get(index);
            if (index > 0)
            {
                barBuilder.append(TextFormatting.DARK_GRAY).append(" ");
            }
            barBuilder.append(marker.color)
                .append(marker.symbol)
                .append(TextFormatting.GRAY)
                .append(":")
                .append(marker.name)
                .append(" ")
                .append(TextFormatting.YELLOW)
                .append((int) marker.distance)
                .append("m");
        }
        return barBuilder.toString();
    }

    private void applyDirectionTicks(String[] slots, int center)
    {
        int leftTick = Math.max(0, center - 10);
        int rightTick = Math.min(slots.length - 1, center + 10);
        slots[leftTick] = TextFormatting.GRAY + "<";
        slots[rightTick] = TextFormatting.GRAY + ">";
    }

    private double getRelativeAngleDegrees(EntityPlayerMP viewer, EntityPlayerMP teammate)
    {
        double dx = teammate.posX - viewer.posX;
        double dz = teammate.posZ - viewer.posZ;
        if (Math.abs(dx) < 0.0001D && Math.abs(dz) < 0.0001D)
        {
            return 0.0D;
        }

        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        return this.wrapDegrees(targetYaw - viewer.rotationYaw);
    }

    private int projectAngleToBarSlot(double relativeAngle)
    {
        double clampedAngle = Math.max(-MAX_TRACKING_ANGLE, Math.min(MAX_TRACKING_ANGLE, relativeAngle));
        double normalized = (clampedAngle + MAX_TRACKING_ANGLE) / (MAX_TRACKING_ANGLE * 2.0D);
        int slot = (int) Math.round(normalized * (LOCATOR_BAR_WIDTH - 1));
        if (slot < 0)
        {
            return 0;
        }
        if (slot >= LOCATOR_BAR_WIDTH)
        {
            return LOCATOR_BAR_WIDTH - 1;
        }
        return slot;
    }

    private char chooseMarkerSymbol(String name, double distance)
    {
        if (distance < 16.0D)
        {
            return '!';
        }
        char first = Character.toUpperCase(name.charAt(0));
        if (first >= 'A' && first <= 'Z')
        {
            return first;
        }
        if (first >= '0' && first <= '9')
        {
            return first;
        }
        return 'O';
    }

    private String chooseMarkerColor(double distance)
    {
        if (distance < 32.0D)
        {
            return TextFormatting.GREEN.toString();
        }
        if (distance < 96.0D)
        {
            return TextFormatting.YELLOW.toString();
        }
        return TextFormatting.RED.toString();
    }

    private double wrapDegrees(double angle)
    {
        double wrapped = angle;
        while (wrapped <= -180.0D)
        {
            wrapped += 360.0D;
        }
        while (wrapped > 180.0D)
        {
            wrapped -= 360.0D;
        }
        return wrapped;
    }
}
