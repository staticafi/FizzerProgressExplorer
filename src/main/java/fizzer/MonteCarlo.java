package fizzer;

import java.util.*;

public class MonteCarlo {

    public MonteCarlo(final ExecutionTree tree) {
        this.tree = tree;
        targetSid = 0;
        traces = new Vector<>();
        locations = new Vector<>();
        samples = new HashMap<>();
        sizes = new HashMap<>();
        frequencies = new Vector<>();
        consumptions = new HashMap<>();
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
        consumptions.clear();
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

    private final ExecutionTree tree;
    private int targetSid;
    private final Vector<Vector<Node>> traces;
    private final Vector<Integer> locations;
    private final HashMap<Integer, Vector<Vector<Float>>> samples;
    private final HashMap<Integer, Vector<Integer>> sizes;
    private final Vector<Vector<Float>> frequencies;
    private final HashMap<Integer, Vector<Vector<Float[]>>> consumptions;
}
