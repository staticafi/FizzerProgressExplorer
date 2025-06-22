package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.*;

public class ExecutionTree {
    private Node rootNode;
    private Analysis[] analyses;
    private StrategyAnalysis[] strategyAnalyses;
    private int analysisIndex;
    private HashMap<LocationId,Integer>[] coverage;
    private HashMap<Integer,Integer>[] coveredIds;
    private HashMap<Long,Node> fromGuidsToNodes;
    private boolean loaded;

    @SuppressWarnings("unchecked")
    public void load(String dir) throws Exception {
        clear();
        TreeMap<Integer, Analysis.Type> analysesMap = new TreeMap<Integer, Analysis.Type>();
        for (String dirName : Stream.of(new File(dir).listFiles())
                .filter(file -> file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet())) {
            int idx = dirName.indexOf('_');
            if (idx < 0)
                throw new RuntimeException("Invalid format of the analysis directory: " + dirName);
            int ordinal = Integer.parseInt(dirName.substring(0, idx));
            if (ordinal < 0)
                throw new RuntimeException("Invalid ordinal of the analysis: " + dirName);
            String name = dirName.substring(idx + 1);
            analysesMap.put(ordinal, Analysis.Type.parse(name));
        }
        coverage = (HashMap<LocationId,Integer>[])new HashMap[] { new HashMap<LocationId,Integer>(), new HashMap<LocationId,Integer>() };
        coveredIds = (HashMap<Integer,Integer>[])new HashMap[] { new HashMap<Integer,Integer>(), new HashMap<Integer,Integer>() };
        fromGuidsToNodes = new HashMap<>();
        analyses = new Analysis[analysesMap.size()];
        strategyAnalyses = new StrategyAnalysis[analysesMap.size()];
        analysisIndex = 0;
        int constructionIndex = 0;
        for (Map.Entry<Integer, Analysis.Type> analysisEntry : analysesMap.entrySet()) {
            File analysisDir = new File(dir, analysisEntry.getKey().toString() + '_' + analysisEntry.getValue());

            TreeMap<Integer, String> tracesMap = new TreeMap<Integer, String>();
            for (String fileName : Stream.of(analysisDir.listFiles())
                    .filter(file -> file.isFile() && file.getName().endsWith(".json"))
                    .map(File::getName)
                    .collect(Collectors.toSet()))
                if (!fileName.equals("info.json") && !fileName.equals("strategy.json") && !fileName.equals("post.json"))
                    tracesMap.put(
                        Integer.parseInt(fileName.substring(0, fileName.indexOf(".json"))),
                        analysisDir.getPath() + '/' + fileName
                        );

            analyses[analysisIndex] = new Analysis(
                analysisEntry.getValue(),
                analysisIndex,
                tracesMap.size(),
                fromGuidsToNodes,
                analysisDir
                );

            strategyAnalyses[analysisIndex] = new StrategyAnalysis(analysisDir);

            for (Map.Entry<Integer, String> traceEntry : tracesMap.entrySet()) {
                JSONObject traceInfo = new JSONObject(
                    Files.lines(Paths.get(traceEntry.getValue())).collect(Collectors.joining("\n"))
                    );
                JSONObject executionResults = traceInfo.getJSONObject("execution_results");
    
                executeTrace(
                    executionResults.getJSONArray("trace"),
                    analysisIndex,
                    constructionIndex,
                    executionResults.getString("termination"),
                    traceEntry.getValue()
                    );

                analyses[analysisIndex].readTraceInfo(traceInfo);

                ++constructionIndex;
            }

            Node analysisNode = analyses[analysisIndex].getNode();
            if (analysisNode != null)
                switch (analyses[analysisIndex].getType()) {
                    case BITSHARE:
                        if (!analysisNode.bitshareApplied(analysisIndex))
                            analysisNode.setBitShareIndex(analysisIndex);
                        break;
                    case LOCAL_SEARCH:
                        if (!analysisNode.localSearchApplied(analysisIndex))
                            analysisNode.setLocalSearchIndex(analysisIndex);
                        break;
                    case BITFLIP:
                        if (!analysisNode.bitflipApplied(analysisIndex))
                            analysisNode.setBitFlipIndex(analysisIndex);
                        break;
                    case TAINT_RESPONSE:
                        if (!analysisNode.sensitivityApplied(analysisIndex))
                            for (Node node = analysisNode; node != null; node = node.getParent())
                                if (!node.sensitivityApplied(analysisIndex))
                                    node.setSensitivityIndex(analysisIndex);
                        break;
                    default:
                        // Nothing to do.
                        break;
                }

            for (long guid : strategyAnalyses[analysisIndex].getClosedNodeGuids())
                fromGuidsToNodes.get(guid).setClosedIndex(analysisIndex);

            ++analysisIndex;
        }
        analysisIndex = analyses.length - 1;
        if (analysisIndex < 0)
            throw new RuntimeException("ERROR: no analysis performed on the benchmark => there is nothing to show.");

        loaded = true;
    }

    public void executeTrace(
            JSONArray trace,
            int analysisIndex,
            int constructionIndex,
            String termination,
            String path
            ) {

        if (trace.length() == 0)
            return;

        final int NUM_TRACE_RECORD_ITEMS = 5;
        final int TRACE_SHIFT_ID = 0;
        final int TRACE_SHIFT_DIRECTION = 1;
        final int TRACE_SHIFT_INPUT_BYTES = 2;
        final int TRACE_SHIFT_VALUE = 3;
        final int TRACE_NODE_GUID = 4;

        if (rootNode == null) {
            int id = trace.getInt(TRACE_SHIFT_ID);
            int numInputBytes = trace.getInt(TRACE_SHIFT_INPUT_BYTES);
            double value = trace.getDouble(TRACE_SHIFT_VALUE);
            long nodeGuid = trace.getLong(TRACE_NODE_GUID);
            rootNode = new Node(
                nodeGuid,
                null,
                id,
                value,
                0,
                numInputBytes,
                analysisIndex,
                constructionIndex
                );
            fromGuidsToNodes.put(nodeGuid, rootNode);
        }

        Node node = rootNode;
        int traceIndex = 0;
        for (int i = 0; true; i += NUM_TRACE_RECORD_ITEMS, ++traceIndex) {
            int id = trace.getInt(i + TRACE_SHIFT_ID);
            int direction = trace.getInt(i + TRACE_SHIFT_DIRECTION) == 0 ? 0 : 1;
            double value = trace.getDouble(i + TRACE_SHIFT_VALUE);
            long nodeGuid = trace.getLong(i + TRACE_NODE_GUID);

            if (!node.getLocationId().equals(id) || node.guid != nodeGuid)
                throw new RuntimeException("Inconsistency in trace: " + path);

            node.updateBestValue(analysisIndex, value);
            node.incrementHitCount(analysisIndex);

            LocationId locationId = new LocationId(id);
            if (coverage[direction].putIfAbsent(locationId, analysisIndex) == null)
                if (isCovered(analysisIndex, locationId, direction == 0 ? true : false))
                    analyses[analysisIndex].getCoveredLocationIds().add(locationId);
            coveredIds[direction].putIfAbsent(id, analysisIndex);

            int j = i + NUM_TRACE_RECORD_ITEMS;
            if (j >= trace.length()) {
                node.updateChildLabel(
                    analysisIndex, direction,
                    termination.equals("NORMAL") ? Node.ChildLabel.END_NORMAL : Node.ChildLabel.END_EXCEPTIONAL
                    );
                return;
            }

            node.setChildLabel(analysisIndex, direction, Node.ChildLabel.VISITED);
            Node[] children = node.getChildren();
            if (children[direction] == null) {
                int sId = trace.getInt(j + TRACE_SHIFT_ID);
                int sNumInputBytes = trace.getInt(j + TRACE_SHIFT_INPUT_BYTES);
                double sValue = trace.getDouble(j + TRACE_SHIFT_VALUE);
                long sNodeGuid = trace.getLong(j + TRACE_NODE_GUID);
                children[direction] = new Node(
                    sNodeGuid,
                    node,
                    sId,
                    sValue,
                    traceIndex + 1,
                    sNumInputBytes,
                    analysisIndex,
                    constructionIndex
                    );
                node.setChildLabel(analysisIndex, direction, Node.ChildLabel.VISITED);
                fromGuidsToNodes.put(sNodeGuid, children[direction]);
            }

            node = children[direction];
        }
    }

    public Node getRootNode() {
        return this.rootNode;
    }

    public Analysis[] getAnalyses() {
        return this.analyses;
    }

    public StrategyAnalysis[] getStrategyAnalyses() {
        return this.strategyAnalyses;
    }

    public StrategyAnalysis getStrategyAnalysisSelectingNode() { return getStrategyAnalysisSelectingNode(getAnalysisIndex()); }

    public StrategyAnalysis getStrategyAnalysisSelectingNode(int index) {
        final Analysis.Type endType;
        switch (analyses[index].getType())
        {
            case BITSHARE: endType = Analysis.Type.BITSHARE; break;
            case LOCAL_SEARCH: endType = Analysis.Type.BITSHARE; break;
            case TAINT_RESPONSE: endType = Analysis.Type.TAINT_REQUEST; break;
            default: return this.strategyAnalyses[index];
        }
        while (index > 0 && !(analyses[index].getType().equals(endType) &&
                              analyses[index].getStartAttribute().equals(Analysis.StartAttribute.REGULAR)))
            --index;
        return this.strategyAnalyses[index];
    }

    public int getAnalysisIndex() {
        return this.analysisIndex;
    }

    public void setAnalysisIndex(int index) {
        this.analysisIndex = index;
    }

    public HashMap<Integer,Integer>[] getCoveredIds() {
        return this.coveredIds;
    }

    public boolean isCovered(LocationId locationId, boolean direction) {
        return isCovered(analysisIndex, locationId, direction);
    }

    public boolean isCovered(int analysisIndex_, LocationId locationId, boolean direction) {
        return coverage[direction ? 1 : 0].getOrDefault(locationId, Integer.MAX_VALUE) <= analysisIndex_;
    }

    public boolean isCovered(int id, boolean direction) {
        return isCovered(analysisIndex, id, direction);
    }

    public boolean isCovered(int analysisIndex_, int id, boolean direction) {
        return coveredIds[direction ? 1 : 0].getOrDefault(id, Integer.MAX_VALUE) <= analysisIndex_;
    }

    public int getUncoveredSignedLocationId(final LocationId id) { return id == null ? 0 : getUncoveredSignedLocationId(id.id); }

    public int getUncoveredSignedLocationId(final int id) {
        final boolean leftCovered = isCovered(id, false);
        final boolean rightCovered = isCovered(id, true);
        return leftCovered != rightCovered ?  (leftCovered ? 1 : -1) * id : 0;
    }

    public ExecutionTree() {
        clear();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void clear() {
        rootNode = null;
        analyses = null;
        analysisIndex = 0;
        coverage = null;
        loaded = false;
    }
}
