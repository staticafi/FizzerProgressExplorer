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
    public static class Signed implements Filter {
        public Signed(float sign_) { sign = sign_; }
        @Override public void run(final Vector<Node> input, final Metric metric, final Vector<Node> output) {
            for (Node node : input)
                if (metric.getValue(node) * sign >= 0.0f)
                    output.add(node);
        }
        private final float sign;
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

        infos = new Vector<>();
        for (final HashMap<Integer, Vector<Float>> conMap : consumptions) {
            final HashMap<Integer, IdInfo> map = new HashMap<>();
            for (int sid : conMap.keySet())
                map.compute(Math.abs(sid), (k, v) -> {
                    if (v == null)
                        v = new IdInfo();
                    v.counts[sid < 0 ? 0 : 1] = conMap.get(sid).size();
                    return v;
                });
            infos.add(map);
        }
        final HashSet<Integer> processed = new HashSet<>();
        for (int sid : sids){
            if (processed.contains(sid))
                continue;
            processed.add(sid);
            processed.add(-sid);
            int avgCount = 0;
            final float[][] avgRatios = new float[2][3]; 
            final HashMap<Integer, Integer> singular = new HashMap<>();
            for (int i = 0; i != consumptions.size(); ++i) {
                final Vector<Vector<Float>> x = new Vector<>();
                x.add(consumptions.get(i).get(-Math.abs(sid)));
                x.add(consumptions.get(i).get(Math.abs(sid)));
                if (x.get(0) != null && x.get(1) != null) {
                    final IdInfo info = infos.get(i).get(Math.abs(sid));
                    for (int k = 0; k != 2; ++k) {
                        final Vector<Float> f = x.get(k);
                        final Vector<Float> g = x.get((k + 1) % 2);
                        for (int j = 0; j < f.size() && f.get(j) < g.firstElement(); ++j)
                            ++info.ratios[k][0];
                        for (int j = f.size() - 1; j >= 0 && f.get(j) > g.lastElement(); --j)
                            ++info.ratios[k][2];
                        info.ratios[k][1] = f.size() - info.ratios[k][0] - info.ratios[k][2];
                        for (int l = 0; l != 3; ++l) {
                            info.ratios[k][l] /= f.size();
                            avgRatios[k][l] += info.ratios[k][l];
                        }
                    }
                    ++avgCount;
                }
                else if (x.get(0) != null)
                    singular.put(i, 0);
                else if (x.get(1) != null)
                    singular.put(i, 1);
            }
            if (avgCount == 0)
                for (int k = 0; k != 2; ++k)
                    avgRatios[k][0] = 1.0f;
            else
                for (int k = 0; k != 2; ++k) {
                    float sum = 0.0f;
                    for (int l = 0; l != 3; ++l) {
                        avgRatios[k][l] /= avgCount;
                        sum += avgRatios[k][l];
                    }
                    for (int l = 0; l != 3; ++l)
                        avgRatios[k][l] /= sum;
                }
            for (Map.Entry<Integer, Integer> entry : singular.entrySet()) {
                final IdInfo info = infos.get(entry.getKey()).get(Math.abs(sid));
                for (int l = 0; l != 3; ++l)
                    info.ratios[entry.getValue()][l] = avgRatios[entry.getValue()][l];
            }
        }
    }

    public Vector<Integer> getSignedLocations() { return sids; }
    public Vector<HashMap<Integer, Vector<Float>>> getConsumptions() { return consumptions; }
    public Vector<Float> getValues() { return values; }
    public Vector<HashMap<Integer, IdInfo>> getInfos() { return infos; }

    public NodeAndDirection run(ExecutionTree tree, float value) {
        // TODO!
        return new NodeAndDirection(tree.getRootNode(), 0);
    }

    public static class IdInfo {
        public final int[] counts = new int[2];
        public final float[][] ratios = new float[2][3];
    }
    
    private final Vector<Integer> sids;
    private final Vector<HashMap<Integer, Vector<Float>>> consumptions;
    private final Vector<Float> values;
    private final Vector<HashMap<Integer, IdInfo>> infos;
}
