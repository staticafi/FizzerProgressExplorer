package fizzer;

import java.util.*;

public class Navigator {

    public static interface Metric { float getValue(Node node); }
    public static class BestValue implements Metric {
        public BestValue(final int analysisIndex_) { analysisIndex = analysisIndex_; }
        @Override public float getValue(final Node node) { return node.getBestValue(analysisIndex).floatValue(); }
        private final int analysisIndex;
    }
    public static class InputSize implements Metric {
        @Override public float getValue(final Node node) { return (float)node.getNumInputBytes(); }
    }
    public static class HitCount implements Metric {
        @Override public float getValue(final Node node) {
            int count = 0;
            for (Node n = node; n != null; n = n.getParent())
                if (n.getLocationId().equals(node.getLocationId()))
                    ++count;
            return (float)count;
        }
    }

    public static interface Filter { void run(Vector<Node> input, Metric metric, Vector<Node> output); }
    public static class KeepAll implements Filter {
        @Override public void run(final Vector<Node> input, final Metric metric, final Vector<Node> output) { output.addAll(input); }
    }
    public static class InputUse implements Filter {
        @Override
        public void run(Vector<Node> input, Metric metric, Vector<Node> output) {
            final HashMap<Float, Vector<Node>> tracesMap = new HashMap<>();
            for (Node node : input)
                tracesMap.compute(metric.getValue(node), (k ,v) -> {
                    final Vector<Node> V = v == null ? new Vector<>() : v; 
                    V.add(node);
                    return V;
                });
            for (Map.Entry<Float, Vector<Node>> entry : tracesMap.entrySet()) {
                Node winner = null;
                for (Node node : entry.getValue())
                    winner = winner == null ? node : better(winner, node);
                output.add(winner);
            }
        }
        private static Node better(final Node left, final Node right) {
            return sizeError(left) <= sizeError(right) ? left : right;
        }
        private static int sizeError(final Node node) { return Math.abs(idealSize(node) - node.getTraceIndex()); }
        private static int idealSize(final Node node) { return 2 * (maxReadIndex(node) + 1); }
        private static int maxReadIndex(Node node) {
            while (node.getParent() != null && node.getParent().getNumInputBytes() == node.getNumInputBytes())
                node = node.getParent();
            return node.getTraceIndex();
        }
    }

    public static class NodeAndDirection {
        public NodeAndDirection(final Node node_, final int direction_) { node = node_; direction = direction_ == 0 ? false : true; }
        public final Node node;
        public final boolean direction;
    } 

    public Navigator(final ExecutionTree tree, final Metric metric, final Filter filter, final int targetSid) {
        sids = new Vector<>();
        consumptions = new Vector<>();
        values = new Vector<>();

        final class TracesCollector {
            TracesCollector() {
                maxDiscoveryIndex= tree.getAnalyses()[tree.getAnalysisIndex()].getViewProps().maxDiscoveryIndex;
                result = new Vector<>();
                run(tree.getRootNode());
            }
            private void run(final Node node) {
                if (node.getDiscoveryIndex() > maxDiscoveryIndex)
                    return;
                if (node.getLocationId().id == Math.abs(targetSid))
                    result.add(node);
                for (int i = 0; i != 2; ++i)
                    if (node.getChildren()[i] != null)
                        run(node.getChildren()[i]);
            }
            private final int maxDiscoveryIndex;
            final Vector<Node> result;
        }
        final Vector<Node> traces = new Vector<>();
        filter.run(new TracesCollector().result, metric, traces);
        traces.sort(new Comparator<Node>() {
            @Override
            public int compare(Node left, Node right) {
                final float l = metric.getValue(left);
                final float r = metric.getValue(right);
                if (l < r)
                    return -1;
                if (l > r)
                    return 1;
                return left.getTraceIndex() - right.getTraceIndex();
            }
        });
        final HashSet<Integer> sidSet = new HashSet<>();
        for (Node node : traces) {
            final HashMap<Integer, Vector<Float>> map = new HashMap<>();
            map.put(-targetSid, new Vector<>());
            map.get(-targetSid).add(1.0f);
            for (Node n = node.getParent(), m = node; n != null; m = n, n = n.getParent()) {
                final int sid = (n.getChildren()[0] == m ? -1 : 1) * n.getLocationId().id;
                final float x = n.getTraceIndex() / (float)Math.max(1, node.getTraceIndex());
                map.compute(sid, (k, v) -> {
                    if (v == null)
                        v = new Vector<>();
                    v.add(0, x);
                    return v;
                });
            }
            sidSet.addAll(map.keySet());
            consumptions.add(map);
            values.add(metric.getValue(node));
        }
        sids.addAll(sidSet.stream().sorted().toList());
    }

    public Vector<Integer> getSignedLocations() { return sids; }
    public Vector<HashMap<Integer, Vector<Float>>> getConsumptions() { return consumptions; }
    public Vector<Float> getValues() { return values; }

    public NodeAndDirection run(ExecutionTree tree, float value) {
        // TODO!
        return new NodeAndDirection(tree.getRootNode(), 0);
    }

    private final Vector<Integer> sids;
    private final Vector<HashMap<Integer, Vector<Float>>> consumptions;
    private final Vector<Float> values;
}
