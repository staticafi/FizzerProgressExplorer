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
            for (Node n = node.getParent(); n != null; n = n.getParent())
                if (n.getLocationId().equals(node.getLocationId()))
                    ++count;
            return (float)count;
        }
    }

    public static abstract class Filter {
        public final void run(Vector<Node> input, Metric metric, Vector<Node> output) {
            if (next == null)
                filter(input, metric, output);
            else {
                final Vector<Node> temp = new Vector<>();
                filter(input, metric, temp);
                next.run(temp, metric, output);
            }
        }
        protected abstract void filter(Vector<Node> input, Metric metric, Vector<Node> output);
        public Filter then(Filter filter) { next = filter; return this; }
        protected Filter next = null;
    }
    public static class KeepAll extends Filter {
        @Override protected void filter(final Vector<Node> input, final Metric metric, final Vector<Node> output) { output.addAll(input); }
    }
    public static class Signed extends Filter {
        public Signed(float sign_) { sign = sign_; }
        @Override protected void filter(final Vector<Node> input, final Metric metric, final Vector<Node> output) {
            for (Node node : input)
                if (metric.getValue(node) * sign >= 0.0f)
                    output.add(node);
        }
        private final float sign;
    }
    public static class InputUse extends Filter {
        @Override protected void filter(Vector<Node> input, Metric metric, Vector<Node> output) {
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

        extrapolations = new HashMap<>();
        for (int sid : sids) {
            if (extrapolations.containsKey(Math.abs(sid)))
                continue;
            final IdExtra extra = new IdExtra();

            final class Input { final Vector<Vec2> points = new Vector<>(); }
            final class Inputs {
                final Input[] counts = new Input[] { new Input(), new Input() };
                final Input[][] ratios = new Input[][] { { new Input(), new Input(), new Input() }, { new Input(), new Input(), new Input() } };
            }
            final Inputs inputs = new Inputs();
            for (int i = 0; i != infos.size(); ++i) {
                final IdInfo info = infos.get(i).get(Math.abs(sid));
                if (info == null)
                    continue;
                for (int j = 0; j != 2; ++j) {
                    inputs.counts[j].points.add(new Vec2(values.get(i), info.counts[j]));
                    for (int k = 0; k != 3; ++k)
                        inputs.ratios[j][k].points.add(new Vec2(values.get(i), info.ratios[j][k]));
                }
            }
            for (int j = 0; j != 2; ++j) {
                extra.counts[j] = new Extrapolation(inputs.counts[j].points);
                for (int k = 0; k != 3; ++k)
                    extra.ratios[j][k] = new Extrapolation(inputs.ratios[j][k].points);
            }
            extrapolations.put(Math.abs(sid), extra);
        }
    }

    public Vector<Integer> getSignedLocations() { return sids; }
    public Vector<HashMap<Integer, Vector<Float>>> getConsumptions() { return consumptions; }
    public Vector<Float> getValues() { return values; }
    public Vector<HashMap<Integer, IdInfo>> getInfos() { return infos; }
    public HashMap<Integer, IdExtra> getExtrapolations() { return extrapolations; }

    public NodeAndDirection run(ExecutionTree tree, float value) {
        final int maxDiscoveryIndex = tree.getAnalyses()[tree.getAnalysisIndex()].getViewProps().maxDiscoveryIndex;

        final class Counts {
            Counts(int total0, int total1) { total[0] = total0; total[1] = total1; }
            boolean depleted() { return current[0] >= total[0] && current[1] >= total[1]; }
            void increment(int dir) { ++current[dir]; }
            int chooseDir() { return ratio(0) <= ratio(1) ? 0 : 1; }
            private float ratio(int dir) { return (current[dir] + 1) / (float)(total[dir] + 1); }
            private final int[] total = { 0, 0 };
            private final int[] current = { 0, 0 };
        }
        final HashMap<Integer, Vector<Counts>> counts = new HashMap<>();
        for (int sid : sids)
            if (!counts.containsKey(Math.abs(sid))) {
                final IdExtra extra = extrapolations.get(Math.abs(sid));
                final IdInfo info = new IdInfo();
                for (int j = 0; j != 2; ++j) {
                    info.counts[j] = Math.round(extra.counts[j].apply(value));
                    float sum = 0.0f;
                    for (int k = 0; k != 3; ++k) {
                        info.ratios[j][k] = extra.ratios[j][k].apply(value);
                        sum += info.ratios[j][k];
                    }
                    if (Math.abs(sum) > 1e-10f)
                        for (int k = 0; k != 3; ++k)
                            info.ratios[j][k] /= sum;
                }
                final Vector<Counts> cnt = new Vector<>();
                for (int k = 0; k != 3; ++k)
                    cnt.add(new Counts(Math.round(info.counts[0] * info.ratios[0][k]), Math.round(info.counts[1] * info.ratios[1][k])));
                Collections.reverse(cnt);
                counts.put(Math.abs(sid), cnt);
            }

        final boolean[] dirOpen = { false, false };
        Node node = tree.getRootNode();
        while (true) {
            final int id = node.getLocationId().id;
            final Vector<Counts> cnt = counts.computeIfAbsent(id, k -> {
                final Vector<Counts> c = new Vector<>();
                c.add(new Counts(0, 0));
                return c;
            });

            int dir;
            for (int i = 0; i != 2; ++i)
                switch (node.getChildLabel(tree.getAnalysisIndex(), i)) {
                    case END_EXCEPTIONAL: case END_NORMAL: dirOpen[i] = false; break;
                    case VISITED: dirOpen[i] = !node.getChildren()[i].isClosed(tree.getAnalysisIndex()); break;
                    default: dirOpen[i] = true; break;
                }
            if (dirOpen[0] && dirOpen[1])
                dir = cnt.lastElement().chooseDir();
            else if (dirOpen[0])
                dir = 0;
            else if (dirOpen[1])
                dir = 1;
            else
                throw new RuntimeException("Navigator.run: Cannot advance in the execution tree.");

            cnt.lastElement().increment(dir);
            if (cnt.lastElement().depleted() && cnt.size() > 1)
                cnt.remove(cnt.size() - 1);

            final Node n = node.getChildren()[dir];
            if (n == null || n.getDiscoveryIndex() > maxDiscoveryIndex)
                return new NodeAndDirection(node, dir);

            node = n;
        }
    }

    public static class IdInfo {
        public final int[] counts = new int[2];
        public final float[][] ratios = new float[2][3];
    }

    public static class Extrapolation {
        public Extrapolation(final Vector<Vec2> input) {
            float A = 0, B = 0, C = 0, D = 0;
            for (int i = 0; i != input.size(); ++i) {
                Vec2 p = input.get(i);
                A += p.x * p.x;
                B += p.x;
                C += p.x * p.y;
                D += p.y;
            }
            c1 = input.isEmpty() || input.size() * A - B * B == 0.0f ? 0.0f : (input.size() * C - B * D) / (input.size() * A - B * B);
            c0 = input.isEmpty() ? 0.0f : (D - c1 * B) / input.size();
        }
        public static float apply(final float c0, final float c1, final float value) { return c0 + value * c1; }
        public float apply(final float value) { return apply(c0, c1, value); }
        private final float c0;
        private final float c1;
    }

    public static class IdExtra {
        public final Extrapolation[] counts = new Extrapolation[2];
        public final Extrapolation[][] ratios = new Extrapolation[2][3];
    }

    private final Vector<Integer> sids;
    private final Vector<HashMap<Integer, Vector<Float>>> consumptions;
    private final Vector<Float> values;
    private final Vector<HashMap<Integer, IdInfo>> infos;
    private final HashMap<Integer, IdExtra> extrapolations;
}
