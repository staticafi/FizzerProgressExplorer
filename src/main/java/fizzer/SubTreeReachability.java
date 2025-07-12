package fizzer;

import java.util.HashMap;
import java.util.Map;

public final class SubTreeReachability {

    public static class SidInfo {
        int num_not_visited_pending = 0;
        int num_not_visited_rest = 0;
        int num_end_exceptional = 0;
        int num_end_normal = 0;
        int num_visited = 0;
    }

    public static SubTreeReachability create(final Node node, final int analysisIndex) {
        final SubTreeReachability result = new SubTreeReachability(node);
        result.reachability.put(-node.getLocationId().id, new SidInfo());
        result.reachability.put(+node.getLocationId().id, new SidInfo());
        for (int direction = 0; direction != 2; ++direction) {
            final int sid = (direction == 0 ? -1 : 1) * node.getLocationId().id;
            switch (node.getChildLabel(analysisIndex, direction)) {
                case NOT_VISITED:
                    if (node.hasPendingAnalysis(analysisIndex))
                        ++result.reachability.get(sid).num_not_visited_pending;
                    else
                        ++result.reachability.get(sid).num_not_visited_rest;
                    break;
                case END_EXCEPTIONAL:
                    ++result.reachability.get(sid).num_end_exceptional;
                    break;
                case END_NORMAL:
                    ++result.reachability.get(sid).num_end_normal;
                    break;
                case VISITED:
                    ++result.reachability.get(sid).num_visited;
                    for (Map.Entry<Integer, SidInfo> entry : create(node.getChildren()[direction], analysisIndex).reachability.entrySet())
                        result.reachability.compute(entry.getKey(), (key, info) -> {
                            if (info == null)
                                return entry.getValue();
                            info.num_not_visited_pending += entry.getValue().num_not_visited_pending;
                            info.num_not_visited_rest += entry.getValue().num_not_visited_rest;
                            info.num_end_exceptional += entry.getValue().num_end_exceptional;
                            info.num_end_normal += entry.getValue().num_end_normal;
                            info.num_visited += entry.getValue().num_visited;
                            return info;
                        });
                    break;
            }
        }
        return result;
    }

    public HashMap<Integer, SidInfo> getReachability() { return reachability; }

    public String getReachabilityJSON() {
        final StringBuilder bld = new StringBuilder();
        bld.append("{");
        boolean first = true;
        for (final int sid : reachability.keySet().stream().sorted().toList()) {
            if (first) first = false; else bld.append(',');
            bld.append("\n    \"");
            bld.append(sid);
            bld.append("\": {");
            bld.append(" \"not_visited_pending\": "); bld.append(reachability.get(sid).num_not_visited_pending);
            bld.append(", \"not_visited_rest\": "); bld.append(reachability.get(sid).num_not_visited_rest);
            bld.append(", \"end_exceptional\": "); bld.append(reachability.get(sid).num_end_exceptional);
            bld.append(", \"end_normal\": "); bld.append(reachability.get(sid).num_end_normal);
            bld.append(", \"visited\": "); bld.append(reachability.get(sid).num_visited);
            bld.append(" }");
        }
        bld.append("\n}");
        return bld.toString();
    }

    private SubTreeReachability(final Node startNode_) {
        reachability = new HashMap<>();
    }

    private final HashMap<Integer, SidInfo> reachability;
}
