package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.network.TeamLocatorEntry;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientBannerVisibilityState {
    // 客户端本地隐藏状态，不进入服务端存档，避免影响其他玩家。
    private static final Set<String> HIDDEN_ENTRY_IDS = ConcurrentHashMap.newKeySet();

    private ClientBannerVisibilityState() {
    }

    public static boolean togglePartyBanner(String entryId) {
        return toggleEntry(entryId);
    }

    public static boolean toggleLocatorBanner(String entryId) {
        return toggleEntry(entryId);
    }

    public static boolean toggleLocatorBanners(Collection<String> entryIds) {
        List<String> normalizedIds = entryIds.stream()
            .map(ClientBannerVisibilityState::normalizeEntryId)
            .filter(id -> !id.isBlank())
            .toList();
        if (normalizedIds.isEmpty()) {
            return false;
        }

        boolean shouldHide = normalizedIds.stream().anyMatch(id -> !HIDDEN_ENTRY_IDS.contains(id));
        if (shouldHide) {
            HIDDEN_ENTRY_IDS.addAll(normalizedIds);
        } else {
            normalizedIds.forEach(HIDDEN_ENTRY_IDS::remove);
        }
        return shouldHide;
    }

    public static boolean isHidden(TeamLocatorEntry entry) {
        if (entry == null || !entry.banner()) {
            return false;
        }
        return HIDDEN_ENTRY_IDS.contains(normalizeEntryId(entry.entryId()));
    }

    public static boolean hasHiddenEntries() {
        return !HIDDEN_ENTRY_IDS.isEmpty();
    }

    public static void clear() {
        HIDDEN_ENTRY_IDS.clear();
    }

    private static boolean toggleEntry(String entryId) {
        String normalizedId = normalizeEntryId(entryId);
        if (normalizedId.isBlank()) {
            return false;
        }
        if (HIDDEN_ENTRY_IDS.remove(normalizedId)) {
            return false;
        }
        HIDDEN_ENTRY_IDS.add(normalizedId);
        return true;
    }

    private static String normalizeEntryId(String entryId) {
        return entryId == null ? "" : entryId.toLowerCase(Locale.ROOT);
    }
}
