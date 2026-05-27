package cn.kafei.TeamGlowing.network;

public record PartyTabEntry(
    String playerId,
    String playerName,
    String partyName,
    int partyColor,
    int sortOrder
) {
}
