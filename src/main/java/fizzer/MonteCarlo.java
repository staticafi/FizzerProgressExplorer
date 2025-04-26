package fizzer;

import java.util.*;

public class MonteCarlo {

    private final ExecutionTree tree;
    private LocationId targetId;
    private final Vector<Vector<Node>> traces;
    private final HashMap<Integer, Vector<Vector<Float>>> samples;

    public MonteCarlo(final ExecutionTree tree) {
        this.tree = tree;
        targetId = null;
        traces = new Vector<>();
        samples = new HashMap<>();
    }

    public ExecutionTree getTree() { return tree; }
    public LocationId getTargetId() { return targetId; }
    public boolean isEmpty() { return targetId == null; }
    public Vector<Vector<Node>> getTraces() { return traces; }
    public Node getTraceTargetNode(Vector<Node> trace) { return trace.get(trace.size() - 1); }
    public Node getTraceTargetNode(int traceIndex) { return getTraceTargetNode(traces.get(traceIndex)); }
    public double getNodeValue(Node node) { return node.getBestValue(tree.getAnalysisIndex()); }
    public double getTraceValue(Vector<Node> trace) { return getNodeValue(getTraceTargetNode(trace)); }
    public double getTraceValue(int traceIndex) { return getTraceValue(traces.get(traceIndex)); }
    public HashMap<Integer, Vector<Vector<Float>>> getSamples() { return samples; }
    public Vector<Vector<Float>> getSamples(int sid) { return samples.get(sid); }
    public double getSampleValue(int sampleIndex) { return getTraceValue(traces.get(sampleIndex)); }
    public Set<Integer> getSignedLocations() { return samples.keySet(); }

    public void clear() {
        targetId = null;
        traces.clear();
        samples.clear();
    }

    public void compute(final LocationId targetId) {
        this.targetId = targetId;
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
        for (Map.Entry<Integer, Vector<Vector<Float>>> entry : samples.entrySet()) {
            entry.setValue(new Vector<>());
            for (Vector<Node> trace : traces)
                entry.getValue().add(computeSample(entry.getKey(), trace));
        }
    }

    private Analysis getAnalysis() {
        return tree.getAnalyses()[tree.getAnalysisIndex()];
    }

    private boolean isNodeValid(final Node node) {
        return node.getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex;
    }

    private void collectTraces(final Node node) {
        if (!isNodeValid(node))
            return;
        if (node.getLocationId().equals(targetId)) {
            Vector<Node> trace = new Vector<>();
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

    private static Vector<Float> computeSample(final int sid, final Vector<Node> trace) {
        final int id = Math.abs(sid);
        final int dir = sid < 0 ? 0 : 1;
        Vector<Float> sample = new Vector<>();
        for (int i = 0, n = trace.size() - 1; i < n; ++i)
            if (trace.get(i).getLocationId().id == id && trace.get(i).getChildren()[dir] == trace.get(i + 1))
                sample.add((float)i / (float)(Math.max(n - 1, 1)));
        return sample;
    }

}
