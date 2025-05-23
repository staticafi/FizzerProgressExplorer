package fizzer;

import java.util.*;

public class MonteCarlo {

    public static class ExtrapolationLinear {
        public ExtrapolationLinear(final float c0_, final float c1_) { c0 = c0_; c1 = c1_; }
        public ExtrapolationLinear(final Vector<Vec2> input) {
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

        public Vector<Float> getCoefficients() { Vector<Float> v = new Vector<>(); v.add(c0); v.add(c1); return v; }
        public Vector<Vec2> parametric() { return parametric(c0, c1); }

        public static ExtrapolationLinear fromLoHi(final Vector<Float> x) { return fromLoHi(x.get(0), x.get(1)); }
        public static ExtrapolationLinear fromLoHi(float lo, float hi) {
            final float slope = 1.0f / Math.max(1e-5f, hi - lo);
            return new ExtrapolationLinear(-lo * slope, slope);
        }

        public static Vector<Vec2> parametric(final float c0, final float c1) {
            final Vector<Vec2> Au = new Vector<>();
            final Vec2 A = new Vec2(0.0f, c0);
            final Vec2 B = new Vec2(1.0f, c0 + c1);
            final Vec2 u = B.sub(A);
            Au.add(A);
            Au.add(u);
            return Au;
        }

        public static float apply(final Vector<Float> coeffs, final float value) { return apply(coeffs.get(0), coeffs.get(1), value); }
        public static float apply(final float c0, final float c1, final float value) { return c0 + value * c1; }
        public float apply(final float value) { return apply(c0, c1, value); }
        public float applyInverse(final float value) { return Math.abs(c1) <= 1e-6f ? 0.0f : (value - c0) / c1; }
        public float applyAndClip(final float value, final float lo, float hi) {
            return Math.max(0.0f, Math.min(1.0f, apply(c0, c1, value)));
        }

        private final float c0;
        private final float c1;
    }

    public static class Extrapolations {
        public Extrapolations(final float c0, final float c1) {
            linearPositive = new ExtrapolationLinear(c0, c1);
            linearNegative = linearPositive;
        }
        public Extrapolations(final Vector<Vec2> input) {
            final Vector<Vec2> positive = new Vector<>();
            final Vector<Vec2> negative = new Vector<>();
            final Vector<Vec2> zero = new Vector<>();
            for (Vec2 point : input)
                (point.x > 0.0f ? positive : point.x < 0.0f ? negative : zero).add(point);
            if (!positive.isEmpty() && !negative.isEmpty()) {
                positive.addAll(zero);
                negative.addAll(zero);
                linearPositive = new ExtrapolationLinear(positive);
                linearNegative = new ExtrapolationLinear(negative);
                return;
            }
            linearPositive = new ExtrapolationLinear(input);
            linearNegative = linearPositive;
        }

        public float applyLinear(final float value) {
            if (value > 0.0f) return linearPositive.apply(value);
            if (value < 0.0f) return linearNegative.apply(value);
            return 0.5f * (linearPositive.apply(value) + linearNegative.apply(value));
        }

        private final ExtrapolationLinear linearPositive;
        private final ExtrapolationLinear linearNegative;
    }

    public static class Clip {
        public static Vector<Vec2> extrapolationLinear(final Vector<Vec2> Au, final Vec2 lo, final Vec2 hi) {
            final Vec2 A = Au.get(0);
            final Vec2 u = Au.get(1);
            float tx, tX;
            if (u.x == 0.0f) {
                if (A.x < lo.x || A.x > hi.x) return null;
                tx = -Float.MAX_VALUE;
                tX = Float.MAX_VALUE;
            } else {
                final float l = (lo.x - A.x) / u.x;
                final float h = (hi.x - A.x) / u.x;
                tx = Math.min(l, h);
                tX = Math.max(l, h);
            }
            float ty, tY;
            if (u.y == 0.0f) {
                if (A.y < lo.y || A.y > hi.y) return null;
                ty = -Float.MAX_VALUE;
                tY = Float.MAX_VALUE;
            } else {
                final float l = (lo.y - A.y) / u.y;
                final float h = (hi.y - A.y) / u.y;
                ty = Math.min(l, h);
                tY = Math.max(l, h);
            }
            final float t0 = Math.max(tx, ty);
            final float t1 = Math.min(tX, tY);
            if (t1 < t0) return null;
            final Vector<Vec2> result = new Vector<>();
            result.add(A.add(u.mul(t0)));
            result.add(A.add(u.mul(t1)));
            return result;
        }
    }

    public static class NodeAndDirection {
        public NodeAndDirection(final Node node_, final int direction_) { node = node_; direction = direction_; }
        public final Node node;
        public final int direction;
    } 

    public static interface NodeEvaluator { float getValue(Node node); }
    public static class BestValue implements NodeEvaluator {
        public BestValue(ExecutionTree tree_) { tree = tree_; }
        @Override public float getValue(Node node) { return node.getBestValue(tree.getAnalysisIndex()).floatValue(); }
        private final ExecutionTree tree;
    }
    public static class InputSize implements NodeEvaluator {
        @Override public float getValue(Node node) { return (float)node.getNumInputBytes(); }
    }

    public static interface TracesFilter { void run(Vector<Vector<Node>> input, NodeEvaluator evaluator, Vector<Vector<Node>> output); }
    public static class KeepAll implements TracesFilter {
        @Override public void run(final Vector<Vector<Node>> input, final NodeEvaluator evaluator, final Vector<Vector<Node>> output) {
            output.addAll(input);
        }
    }
    public static class InputUse implements TracesFilter {
        @Override
        public void run(final Vector<Vector<Node>> input, final NodeEvaluator evaluator, final Vector<Vector<Node>> output) {
            final HashMap<Float, Vector<Vector<Node>>> tracesMap = new HashMap<>();
            for (Vector<Node> trace : input)
                tracesMap.compute(evaluator.getValue(trace.lastElement()), (k ,v) -> {
                    final Vector<Vector<Node>> V = v == null ? new Vector<>() : v; 
                    V.add(trace);
                    return V;
                });
            for (Map.Entry<Float, Vector<Vector<Node>>> entry : tracesMap.entrySet()) {
                Vector<Node> winner = null;
                for (Vector<Node> trace : entry.getValue())
                    winner = winner == null ? trace : better(winner, trace);
                output.add(winner);
            }
        }
        private static Vector<Node> better(final Vector<Node> left, final Vector<Node> right) {
            return sizeError(left) <= sizeError(right) ? left : right;
        }
        private static int sizeError(final Vector<Node> trace) { return Math.abs(idealSize(trace) - trace.size()); }
        private static int idealSize(final Vector<Node> trace) { return 2 * (maxReadIndex(trace) + 1); }
        private static int maxReadIndex(final Vector<Node> trace) {
            int index = 0;
            for (int i = 1; i < trace.size(); ++i)
                if (trace.get(index).getNumInputBytes() < trace.get(i).getNumInputBytes())
                    index = i;
            return index;
        }
    }

    public MonteCarlo(final ExecutionTree tree, final NodeEvaluator nodeEvaluator, final TracesFilter tracesFilter) {
        this.tree = tree;
        this.nodeEvaluator = nodeEvaluator;
        this.tracesFilter = tracesFilter;
        targetSid = 0;
        traces = new Vector<>();
        locations = new Vector<>();
        samples = new HashMap<>();
        sizes = new HashMap<>();
        frequencies = new Vector<>();
        consumptions = new HashMap<>();
        sizesExtrapolation = new HashMap<>();
        frequenciesExtrapolation = new Vector<>();
        consumptionsExtrapolation = new HashMap<>();
    }

    public ExecutionTree getTree() { return tree; }
    public NodeEvaluator getNodeEvaluator() { return nodeEvaluator; }
    public TracesFilter getTracesFilter() { return tracesFilter; }
    public int getTargetSIid() { return targetSid; }
    public boolean isEmpty() { return targetSid == 0; }
    public Vector<Vector<Node>> getTraces() { return traces; }
    public int getNumTraces() { return traces.size(); }
    public Node getTraceTargetNode(Vector<Node> trace) { return trace.lastElement(); }
    public Node getTraceTargetNode(int traceIndex) { return getTraceTargetNode(traces.get(traceIndex)); }
    public double getNodeValue(Node node) { return nodeEvaluator.getValue(node); }
    public double getTraceValue(Vector<Node> trace) { return getNodeValue(getTraceTargetNode(trace)); }
    public double getTraceValue(int traceIndex) { return getTraceValue(traces.get(traceIndex)); }
    public HashMap<Integer, Vector<Vector<Float>>> getSamples() { return samples; }
    public Vector<Vector<Float>> getSamples(int sid) { return samples.get(sid); }
    public Vector<Integer> getSignedLocations() { return locations; }
    public HashMap<Integer, Vector<Integer>> getSizes() { return sizes; }
    public Vector<Integer> getSizes(int sid) { return sizes.get(sid); }
    public Vector<Vector<Float>> getFrequencies() { return frequencies; }
    public Vector<Float> getFrequencies(int sample) { return frequencies.get(sample); }
    public HashMap<Integer, Vector<Vector<Vec2>>> getConsumptions() { return consumptions; }
    public Vector<Vector<Vec2>> getConsumptions(int sid) { return consumptions.get(sid); }
    public int extrapolateSizesLinear(int sid, float value) { return Math.round(sizesExtrapolation.get(sid).applyLinear(value)); }
    public float extrapolateFrequenciesLinear(int locationIndex, float value) {
        return Math.max(0.0f, frequenciesExtrapolation.get(locationIndex).applyLinear(value));
    }
    public Vector<Float> extrapolateFrequenciesLinear(float value) {
        Vector<Float> result = new Vector<>();
        float sum = 0.0f;
        for (int i = 0; i != locations.size(); ++i) {
            final float r = extrapolateFrequenciesLinear(i, value);
            result.add(r);
            sum += r;
        }
        if (sum > 0.0f)
            for (int i = 0; i != result.size(); ++i)
                result.set(i, result.get(i) / sum);
        return result;
    }
    public Vector<Vec2> extrapolateConsumptionsLinear(int sid, int traceIndex) {
        final Vector<Float> values = computeConsumptionsExtrapolationLinear(sid, (float)getTraceValue(traceIndex));
        final Vector<Vec2> Au = new Vector<>();
        Au.add(new Vec2(values.get(0), 0.0f));
        Au.add(new Vec2(values.get(1) - values.get(0), 1.0f));
        return Clip.extrapolationLinear(Au, Vec2.zero, new Vec2(1.0f, 1.0f));
    }
    public Vector<Float> computeConsumptionsExtrapolationLinear(int sid, float value) {
        final Vector<Float> values = new Vector<>();
        final Vector<Extrapolations> e = consumptionsExtrapolation.get(sid);
        float v0 = e == null ? 0.0f : e.get(0).applyLinear(value);
        float v1 = e == null ? 0.0f : e.get(1).applyLinear(value);
        if (v0 >= v1) {
            v1 = (v0 + v1) / 2;
            v0 = v1 - 1.0f / Math.max(1.0f, sizesExtrapolation.get(0).applyLinear(value));
        }
        values.add(v0);
        values.add(v1);
        return values;
    }
    public NodeAndDirection selectNodeForValue(final float value) { return selectNode(value); }

    public void setTargetSid(final int sid) { targetSid = sid; }
    public boolean setTargetSid(final Node node) {
        if (!isNodeValid(node))
            return false;
        final boolean leftNotVisited = node.getChildLabel(tree.getAnalysisIndex(), 0) == Node.ChildLabel.NOT_VISITED;
        final boolean rightNotVisited = node.getChildLabel(tree.getAnalysisIndex(), 1) == Node.ChildLabel.NOT_VISITED;
        if (leftNotVisited == rightNotVisited)
            return false;
        targetSid = (leftNotVisited ? -1 : 1) * node.getLocationId().id;
        return true;
    }

    public void clear() {
        targetSid = 0;
        traces.clear();
        locations.clear();
        samples.clear();
        sizes.clear();
        frequencies.clear();
        consumptions.clear();
        sizesExtrapolation.clear();
        frequenciesExtrapolation.clear();
        consumptionsExtrapolation.clear();
    }

    public void compute() {
        if (isEmpty())
            return;
        collectTraces();
        computeSamples();
        computeLocations();
        computeSizes();
        computeFrequencies();
        computeConsumptions();
        computeSizesExtrapolation();
        computeFrequenciesExtrapolation();
        computeConsumptionsExtrapolation();
    }

    private Analysis getAnalysis() {
        return tree.getAnalyses()[tree.getAnalysisIndex()];
    }

    private boolean isNodeValid(final Node node) {
        return node.getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex;
    }

    private void collectTraces() {
        final Vector<Vector<Node>> allTraces = new Vector<>();
        collectTraces(tree.getRootNode(), allTraces);
        tracesFilter.run(allTraces, nodeEvaluator, traces);
        traces.sort(new Comparator<Vector<Node>>() {
            @Override
            public int compare(Vector<Node> left, Vector<Node> right) {
                Node ln = getTraceTargetNode(left);
                Node rn = getTraceTargetNode(right);
                double l = getNodeValue(ln);
                double r = getNodeValue(rn);
                if (l < r)
                    return -1;
                if (l > r)
                    return 1;
                return left.size() - right.size();
            }
        });
    }

    private void collectTraces(final Node node, final Vector<Vector<Node>> result) {
        if (!isNodeValid(node))
            return;
        if (node.getLocationId().id == Math.abs(targetSid)) {
            if (node.getChildLabel(tree.getAnalysisIndex(), targetSid < 0 ? 0 : 1) != Node.ChildLabel.NOT_VISITED)
                return;
            final Vector<Node> trace = new Vector<>();
            for (Node n = node; n != null; n = n.getParent())
                trace.add(n);
            Collections.reverse(trace);
            result.add(trace);
        }
        for (int i = 0; i != 2; ++i)
            if (node.getChildren()[i] != null)
                collectTraces(node.getChildren()[i], result);
    }

    private void computeSamples() {
        for (Vector<Node> trace : traces)
            for (int i = 0; i + 1 < trace.size(); ++i) {
                final int sid = (trace.get(i).getChildren()[0] == trace.get(i + 1) ? -1 : 1) * trace.get(i).getLocationId().id;
                samples.compute(sid, (k, v) -> { return v == null ? new Vector<>() : v; });
            }
        for (Map.Entry<Integer, Vector<Vector<Float>>> entry : samples.entrySet()) {
            entry.setValue(new Vector<>());
            for (Vector<Node> trace : traces)
                entry.getValue().add(computeSample(entry.getKey(), trace));
        }
    }

    private static Vector<Float> computeSample(final int sid, final Vector<Node> trace) {
        final int id = Math.abs(sid);
        final int dir = sid < 0 ? 0 : 1;
        final Vector<Float> sample = new Vector<>();
        for (int i = 0, n = trace.size() - 1; i < n; ++i)
            if (trace.get(i).getLocationId().id == id && trace.get(i).getChildren()[dir] == trace.get(i + 1))
                sample.add((float)i / (float)(Math.max(n - 1, 1)));
        return sample;
    }

    private void computeLocations() {
        for (int sid : samples.keySet().stream().sorted().toList())
            locations.add(sid);
    }

    private void computeSizes() {
        for (Map.Entry<Integer, Vector<Vector<Float>>> entry : samples.entrySet()) {
            final Vector<Integer> v = new Vector<>();
            for (Vector<Float> sample : entry.getValue())
                v.add(sample.size());
            sizes.put(entry.getKey(), v);
        }
    }

    private void computeFrequencies() {
        for (int i = 0; i < traces.size(); ++i) {
            final Vector<Float> f = new Vector<>();
            int sum = 0;
            for (int sid : locations) {
                int s = sizes.get(sid).get(i);
                f.add((float)s);
                sum += s;
            }
            for (int j = 0; j < f.size(); ++j)
                f.set(j, f.get(j) / sum);
            frequencies.add(f);
        }
    }

    private void computeConsumptions() {
        for (Map.Entry<Integer, Vector<Vector<Float>>> entry : samples.entrySet()) {
            final Vector<Vector<Vec2>> u = new Vector<>();
            for (Vector<Float> sample : entry.getValue()) {
                final Vector<Vec2> v = new Vector<>();
                int count = 0;
                for (float t : sample) {
                    ++count;
                    v.add(new Vec2(t, count / (float)sample.size()));
                }
                u.add(v);
            }
            consumptions.put(entry.getKey(), u);
        }
    }

    private void computeSizesExtrapolation() {
        for (int sid : locations)
            sizesExtrapolation.put(sid, computeSizesExtrapolation(sizes.get(sid)));
        Vector<Integer> all = new Vector<>();
        for (int i = 0; i != getNumTraces(); ++i) {
            int sum = 0;
            for (int sid : locations)
                sum += sizes.get(sid).get(i);
            all.add(sum);
        }
        sizesExtrapolation.put(0, computeSizesExtrapolation(all));
    }

    private Extrapolations computeSizesExtrapolation(Vector<Integer> data) {
        final Vector<Vec2> input = new Vector<>();
        for (int i = 0; i != data.size(); ++i)
            input.add(new Vec2((float)getTraceValue(i), (float)data.get(i)));
        return new Extrapolations(input);
    }

    private void computeFrequenciesExtrapolation() {
        for (int i = 0; i < locations.size(); ++i) {
            final Vector<Vec2> input = new Vector<>();
            for (int j = 0; j < frequencies.size(); ++j)
                input.add(new Vec2((float)getTraceValue(j), frequencies.get(j).get(i)));
            frequenciesExtrapolation.add(new Extrapolations(input));
        }
    }

    private void computeConsumptionsExtrapolation() {
        final Vector<Integer> failedSids = new Vector<>();
        for (int sid : locations) {
            final Vector<Vector<Float>> coefficients = new Vector<>();
            boolean hasValid = false;
            for (Vector<Vec2> input : consumptions.get(sid))
                if (input.size() > 1) {
                    final Vector<Float> values = new Vector<>();
                    final ExtrapolationLinear extrapolation = new ExtrapolationLinear(input);
                    values.add(extrapolation.applyInverse(0.0f));
                    values.add(extrapolation.applyInverse(1.0f));
                    coefficients.add(values);
                    hasValid = true;
                }
                else
                    coefficients.add(null);
            if (hasValid) {
                final Vector<Extrapolations> v = new Vector<>();
                for (int j = 0; j != 2; ++j) {
                    final Vector<Vec2> input = new Vector<>();
                    for (int i = 0; i != coefficients.size(); ++i)
                        if (coefficients.get(i) != null)
                            input.add(new Vec2((float)getTraceValue(i), coefficients.get(i).get(j)));
                    v.add(new Extrapolations(input));
                }
                consumptionsExtrapolation.put(sid, v);
            }
            else
                failedSids.add(sid);
        }
        for (int sid : failedSids) {
            final Vector<Vec2> input = new Vector<>();
            final Vector<Vector<Vec2>> C = consumptions.get(sid);
            for (int i = 0; i != C.size(); ++i)
                if (!C.get(i).isEmpty())
                    input.add(new Vec2((float)getTraceValue(i), C.get(i).get(0).x));
            if (input.isEmpty())
                input.add(new Vec2(0.0f, 2.0f));
            final Vector<Extrapolations> v = new Vector<>();
            v.add(new Extrapolations(input));
            v.add(v.lastElement());
            consumptionsExtrapolation.put(sid, v);
        }
    }

    private NodeAndDirection selectNode(final float value) {
        final class Counts {
            Counts(int dir) { total[dir] = 1; }
            Counts(int total0, int total1) { total[0] = total0; total[1] = total1; }
            boolean depleted() { return current[0] >= total[0] && current[1] >= total[1]; }
            void increment(int dir) { ++current[dir]; }
            int computeDir() { return ratio(0) <= ratio(1) ? 0 : 1; }
            float ratio(int dir) { return (current[dir] + 1) / (float)(total[dir] + 1); }
            private final int[] total = { 0, 0 };
            private final int[] current = { 0, 0 };
        }
        final HashMap<Integer, Vector<Counts>> counts = new HashMap<>();
        for (int sid : locations) {
            final Extrapolations s0 = sizesExtrapolation.get(-Math.abs(sid));
            final Extrapolations s1 = sizesExtrapolation.get(Math.abs(sid));
            final int size0 = s0 == null ? 0 : Math.max(0, Math.round(s0.applyLinear(value)));
            final int size1 = s1 == null ? 0 : Math.max(0, Math.round(s1.applyLinear(value)));
            if (size0 > 0 || size1 > 0) {
                final Vector<Counts> cnt = new Vector<>();
                if (size0 > 0 && size1 > 0) {
                    final Vector<Float> x0 = computeConsumptionsExtrapolationLinear(-Math.abs(sid), value);
                    final Vector<Float> x1 = computeConsumptionsExtrapolationLinear(Math.abs(sid), value);
                    final ExtrapolationLinear e0 = ExtrapolationLinear.fromLoHi(x0);
                    final ExtrapolationLinear e1 = ExtrapolationLinear.fromLoHi(x1);
                    final Vector<Float> x = new Vector<>(x0);
                    x.addAll(x1);
                    x.sort(new Comparator<Float>() {
                        @Override public int compare(Float left, Float right) { return left < right ? -1 : left > right ? 1 : 0; }
                    });
                    for (int i = 1; i < x.size(); ++i) {
                        final float fraction0 = e0.applyAndClip(x.get(i), 0.0f, 1.0f) - e0.applyAndClip(x.get(i-1), 0.0f, 1.0f);
                        final float fraction1 = e1.applyAndClip(x.get(i), 0.0f, 1.0f) - e1.applyAndClip(x.get(i-1), 0.0f, 1.0f);
                        final int count0 = Math.round(size0 * fraction0);
                        final int count1 = Math.round(size1 * fraction1);
                        if (count0 > 0 || count1 > 0)
                            cnt.add(new Counts(count0, count1));
                    }
                    Collections.reverse(cnt);
                } else
                    cnt.add(new Counts(size0, size1));
                counts.put(Math.abs(sid), cnt);
            }
        }
        counts.compute(Math.abs(targetSid), (k, v) -> {
            Vector<Counts> c = v == null ? new Vector<>() : v;
            c.add(0, new Counts(targetSid < 0 ? 0 : 1));
            return c;
        });
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
                dir = cnt.lastElement().computeDir();
            else if (dirOpen[0])
                dir = 0;
            else if (dirOpen[1])
                dir = 1;
            else
                throw new RuntimeException("MonteCarlo.selectNode: Cannot advance in the execution tree.");
            cnt.lastElement().increment(dir);
            if (cnt.lastElement().depleted() && cnt.size() > 1)
                cnt.remove(cnt.size() - 1);

            final Node n = node.getChildren()[dir];
            if (n == null || !isNodeValid(n))
                return new NodeAndDirection(node, dir);
            node = n;
        }
    }

    private final ExecutionTree tree;
    final NodeEvaluator nodeEvaluator;
    final TracesFilter tracesFilter;
    private int targetSid;
    private final Vector<Vector<Node>> traces;
    private final Vector<Integer> locations;
    private final HashMap<Integer, Vector<Vector<Float>>> samples;
    private final HashMap<Integer, Vector<Integer>> sizes;
    private final Vector<Vector<Float>> frequencies;
    private final HashMap<Integer, Vector<Vector<Vec2>>> consumptions;
    private final HashMap<Integer, Extrapolations> sizesExtrapolation;
    private final Vector<Extrapolations> frequenciesExtrapolation;
    private final HashMap<Integer, Vector<Extrapolations>> consumptionsExtrapolation;
}
