package fizzer;

import java.util.*;

public class MonteCarlo {

    public static class ExtrapolationLinear {
        public ExtrapolationLinear(final Vector<Vec2> input) {
            float A = 0, B = 0, C = 0, D = 0;
            for (int i = 0; i != input.size(); ++i) {
                Vec2 p = input.get(i);
                A += p.x * p.x;
                B += p.x;
                C += p.x * p.y;
                D += p.y;
            }
            c1 = (input.size() * C - B * D) / (input.size() * A - B * B);
            c0 = (D - c1 * B) / input.size();
        }

        public float apply(final float value) {return c0 + value * c1; }

        private final float c0;
        private final float c1;
    }

    public static class Extrapolations {
        public Extrapolations(final Vector<Vec2> input, final boolean splitBySign) {
            if (splitBySign) {
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

    public MonteCarlo(final ExecutionTree tree) {
        this.tree = tree;
        targetSid = 0;
        traces = new Vector<>();
        locations = new Vector<>();
        samples = new HashMap<>();
        sizes = new HashMap<>();
        frequencies = new Vector<>();
        consumptions = new HashMap<>();
        functionSizes = new HashMap<>();
        frequenciesExtrapolation = new Vector<>();
    }

    public ExecutionTree getTree() { return tree; }
    public int getTargetSIid() { return targetSid; }
    public boolean isEmpty() { return targetSid == 0; }
    public Vector<Vector<Node>> getTraces() { return traces; }
    public int getNumTraces() { return traces.size(); }
    public Node getTraceTargetNode(Vector<Node> trace) { return trace.get(trace.size() - 1); }
    public Node getTraceTargetNode(int traceIndex) { return getTraceTargetNode(traces.get(traceIndex)); }
    public double getNodeValue(Node node) { return node.getBestValue(tree.getAnalysisIndex()); }
    public double getTraceValue(Vector<Node> trace) { return getNodeValue(getTraceTargetNode(trace)); }
    public double getTraceValue(int traceIndex) { return getTraceValue(traces.get(traceIndex)); }
    public HashMap<Integer, Vector<Vector<Float>>> getSamples() { return samples; }
    public Vector<Vector<Float>> getSamples(int sid) { return samples.get(sid); }
    public double getSampleValue(int sampleIndex) { return getTraceValue(traces.get(sampleIndex)); }
    public Vector<Integer> getSignedLocations() { return locations; }
    public HashMap<Integer, Vector<Integer>> getSizes() { return sizes; }
    public Vector<Integer> getSizes(int sid) { return sizes.get(sid); }
    public Vector<Vector<Float>> getFrequencies() { return frequencies; }
    public Vector<Float> getFrequencies(int sample) { return frequencies.get(sample); }
    public HashMap<Integer, Vector<Vector<Float[]>>> getConsumptions() { return consumptions; }
    public Vector<Vector<Float[]>> getConsumptions(int sid) { return consumptions.get(sid); }
    public HashMap<Integer, Vector<Float>> getFunctionSizes() { return functionSizes; }
    public Vector<Float> getFunctionSizes(int sid) { return functionSizes.get(sid); }
    public Vector<Float> getFunctionSizesOverall() { return functionSizes.get(0); }
    public int evalFunctionSizesLinear(int sid, float value) { return Math.round(evalFunctionLinear(functionSizes.get(sid), value)); }
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
        functionSizes.clear();
        frequenciesExtrapolation.clear();
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
        computeFunctionSizes();
        computeFrequenciesExtrapolation();
    }

    private Analysis getAnalysis() {
        return tree.getAnalyses()[tree.getAnalysisIndex()];
    }

    private boolean isNodeValid(final Node node) {
        return node.getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex;
    }

    private void collectTraces() {
        collectTraces(tree.getRootNode());
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

    private void collectTraces(final Node node) {
        if (!isNodeValid(node))
            return;
        if (node.getLocationId().id == Math.abs(targetSid)) {
            if (node.getChildLabel(tree.getAnalysisIndex(), targetSid < 0 ? 0 : 1) != Node.ChildLabel.NOT_VISITED)
                return;
            final Vector<Node> trace = new Vector<>();
            for (Node n = node; n != null; n = n.getParent())
                trace.add(n);
            Collections.reverse(trace);
            for (int i = 0; i + 1 < trace.size(); ++i)
                samples.putIfAbsent((trace.get(i).getChildren()[0] == trace.get(i + 1) ? -1 : 1) * trace.get(i).getLocationId().id, null);
            traces.add(trace);
        }
        for (int i = 0; i != 2; ++i)
            if (node.getChildren()[i] != null)
                collectTraces(node.getChildren()[i]);
    }

    private void computeSamples() {
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
            final Vector<Vector<Float[]>> u = new Vector<>();
            for (Vector<Float> sample : entry.getValue()) {
                final Vector<Float[]> v = new Vector<>();
                int count = 0;
                for (float t : sample) {
                    ++count;
                    v.add(new Float[] { t, count / (float)sample.size() });
                }
                u.add(v);
            }
            consumptions.put(entry.getKey(), u);
        }
    }

    private void computeFunctionSizes() {
        for (int sid : locations)
            functionSizes.put(sid, computeFunctionSizes(sizes.get(sid)));
        Vector<Integer> all = new Vector<>();
        for (int i = 0; i != getNumTraces(); ++i) {
            int sum = 0;
            for (int sid : locations)
                sum += sizes.get(sid).get(i);
            all.add(sum);
        }
        functionSizes.put(0, computeFunctionSizes(all));
    }

    private Vector<Float> computeFunctionSizes(Vector<Integer> data) {
        final Vector<Vec2> input = new Vector<>();
        for (int i = 0; i != data.size(); ++i)
            input.add(new Vec2((float)getTraceValue(i), (float)data.get(i)));
        return computeApproximations(input);
    }

    private void computeFrequenciesExtrapolation() {
        for (int i = 0; i < locations.size(); ++i) {
            final Vector<Vec2> input = new Vector<>();
            for (int j = 0; j < frequencies.size(); ++j)
                input.add(new Vec2((float)getTraceValue(j), frequencies.get(j).get(i)));
            frequenciesExtrapolation.add(new Extrapolations(input, true));
        }
    }

    private Vector<Float> computeApproximations(final Vector<Vec2> input) {
        final Vector<Float> output = new Vector<>();
        computeLinearApproximation(input, output);
        computeQuadraticApproximation(input, output);
        return output;
    }

    private void computeLinearApproximation(final Vector<Vec2> input, final Vector<Float> output) {
        float A = 0, B = 0, C = 0, D = 0;
        for (int i = 0; i != input.size(); ++i) {
            Vec2 p = input.get(i);
            A += p.x * p.x;
            B += p.x;
            C += p.x * p.y;
            D += p.y;
        }
        float a = (input.size() * C - B * D) / (input.size() * A - B * B);
        float b = (D - a * B) / input.size();
        output.add(b);
        output.add(a);
    }

    private void computeQuadraticApproximation(final Vector<Vec2> input, final Vector<Float> output) {
        // TODO!
        output.add(0.0f);
        output.add(0.0f);
        output.add(0.0f);
    }

    private float evalFunctionLinear(final Vector<Float> coefficients, final float value) {
        return coefficients.get(0) + value * coefficients.get(1);
    }

    private final ExecutionTree tree;
    private int targetSid;
    private final Vector<Vector<Node>> traces;
    private final Vector<Integer> locations;
    private final HashMap<Integer, Vector<Vector<Float>>> samples;
    private final HashMap<Integer, Vector<Integer>> sizes;
    private final Vector<Vector<Float>> frequencies;
    private final HashMap<Integer, Vector<Vector<Float[]>>> consumptions;
    private final HashMap<Integer, Vector<Float>> functionSizes;
    private final Vector<Extrapolations> frequenciesExtrapolation;
}
