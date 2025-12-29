package fizzer.nav;

import fizzer.ExecutionTree;
import fizzer.Node;
import java.util.*;

public class GenPaths {
    public GenPaths(ExecutionTree tree_) {
        tree = tree_;
        out = null;
        succ = null;
        starts = null;
    }

    public String run() {
        out = new StringBuilder();
        succ = new HashMap<>();
        starts = new HashSet<>();

        buildSuccAndFrontier(tree.getRootNode());
        dumpSucc();
        //dumpStarts();

        return out.toString();
    }

    private void buildSuccAndFrontier(Node node) {
        for (int i = 0; i < 2; ++i) {
            Node child = node.getChildren()[i];
            int dir = 2*i - 1;
            if (child != null && child.getDiscoveryIndex() <= tree.getAnalyses()[tree.getAnalysisIndex()].getMaxDiscoveryIndex()) {
                succ.put(node.getLocationId().id * dir, child.getLocationId().id);
                buildSuccAndFrontier(child);
            } else if (child == null && !node.getChildLabel(tree.getAnalysisIndex(), i).equals(Node.ChildLabel.NOT_VISITED)) {
                succ.putIfAbsent(node.getLocationId().id * dir, 0);
            } else {
                starts.add(node.guid * dir);
            }
        }
    }

    private void dumpSucc() {
        out.append("Succ:\n");
        for (Map.Entry<Integer, Integer> e : succ.entrySet()) {
            if (e.getKey() < 0)
                out.append(-e.getKey()).append('-');
            else
                out.append(e.getKey()).append('+');
            out.append(" -> ").append(e.getValue()).append("\n");
        }
    }

    private void dumpStarts() {
        out.append("Starts:\n");
        for (Long sguid : starts) {
            out.append('g');
            if (sguid < 0)
                out.append(-sguid).append('-');
            else
                out.append(sguid).append('+');
            out.append("\n");
        }
    }

    private final ExecutionTree tree;
    private StringBuilder out;
    private HashMap<Integer, Integer> succ;
    private HashSet<Long> starts;
}
