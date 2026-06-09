package nl.multicode.model;

import java.util.Collections;
import java.util.Map;

public final class ClusterResult {

    private final Map<String, Map<Integer, String>> groups;
    private final Map<Integer, String> ungrouped;

    public ClusterResult(
            Map<String, Map<Integer, String>> groups,
            Map<Integer, String> ungrouped
    ) {
        this.groups = Collections.unmodifiableMap(groups);
        this.ungrouped = Collections.unmodifiableMap(ungrouped);
    }

    public Map<String, Map<Integer, String>> getGroups() {
        return groups;
    }

    public Map<Integer, String> getUngrouped() {
        return ungrouped;
    }

    public boolean hasGroups() {
        return !groups.isEmpty();
    }
}
