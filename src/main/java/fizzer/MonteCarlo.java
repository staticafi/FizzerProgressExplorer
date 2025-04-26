package fizzer;

import java.util.*;

public class MonteCarlo {

    private ExecutionTree tree;
    private LocationId targetId;
    private Vector<Vector<Node>> traces;
    private HashMap<LocationId, Vector<Vector<Float>>> samples;

    public MonteCarlo(ExecutionTree tree) {
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
    public HashMap<LocationId, Vector<Vector<Float>>> getSamples() { return samples; }
    public Vector<Vector<Float>> getSamples(LocationId id) { return samples.get(id); }
    public double getSampleValue(int sampleIndex) { return getTraceValue(traces.get(sampleIndex)); }
    public Set<LocationId> getLocations() { return samples.keySet(); }

    public void clear() {
        targetId = null;
        traces.clear();
        samples.clear();
    }

    public void compute(LocationId targetId) {
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
        for (Map.Entry<LocationId, Vector<Vector<Float>>> entry : samples.entrySet()) {
            entry.setValue(new Vector<>());
            for (Vector<Node> trace : traces)
                entry.getValue().add(computeSample(entry.getKey(), trace));
        }
    }

    private Analysis getAnalysis() {
        return tree.getAnalyses()[tree.getAnalysisIndex()];
    }

    private boolean isNodeValid(Node node) {
        return node.getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex;
    }

    private void collectTraces(Node node) {
        if (!isNodeValid(node))
            return;
        if (node.getLocationId().equals(targetId)) {
            Vector<Node> trace = new Vector<>();
            for (Node n = node; n != null; n = n.getParent())
                trace.add(n);
            for (Node n = node.getParent(); n != null; n = n.getParent())
                samples.putIfAbsent(n.getLocationId(), null);
            Collections.reverse(trace);
            traces.add(trace);
        }
        for (int i = 0; i != 2; ++i)
            if (node.getChildren()[i] != null)
                collectTraces(node.getChildren()[i]);
    }

    private static Vector<Float> computeSample(LocationId id, Vector<Node> trace) {
        Vector<Float> sample = new Vector<>();
        for (int i = 0, n = trace.size() - 1; i < n; ++i)
            if (trace.get(i).getLocationId().equals(id))
                sample.add((float)i / (float)(Math.max(n - 1, 1)));
        return sample;
    }

}
