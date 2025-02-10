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
                throw new Exception("Invalid format of the analysis directory: " + dirName);
            int ordinal = Integer.parseInt(dirName.substring(0, idx));
            if (ordinal < 0)
                throw new Exception("Invalid ordinal of the analysis: " + dirName);
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
                if (!fileName.equals("info.json") && !fileName.equals("strategy.json"))
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

                double analysisNodeValue = executeTrace(
                    traceInfo.getJSONArray("trace_records"),
                    analysisIndex,
                    constructionIndex,
                    traceInfo.getString("trace_termination"),
                    traceEntry.getValue()
                    );

                analyses[analysisIndex].readTraceInfo(traceInfo, analysisNodeValue);

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
                    case TAINT_RES:
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
            throw new Exception("ERROR: no analysis performed on the benchmark => there is nothing to show.");
    }

    public double executeTrace(
            JSONArray trace,
            int analysisIndex,
            int constructionIndex,
            String termination,
            String path
            ) throws Exception {

        double analysisNodeValue = Double.MAX_VALUE;

        if (trace.length() == 0)
            return analysisNodeValue;

        final int NUM_TRACE_RECORD_ITEMS = 6;
        final int TRACE_SHIFT_ID = 0;
        final int TRACE_SHIFT_CONTEXT = 1;
        final int TRACE_SHIFT_DIRECTION = 2;
        final int TRACE_SHIFT_INPUT_BYTES = 3;
        final int TRACE_SHIFT_VALUE = 4;
        final int TRACE_NODE_GUID = 5;

        if (rootNode == null) {
            int id = trace.getInt(TRACE_SHIFT_ID);
            int context = trace.getInt(TRACE_SHIFT_CONTEXT);
            int numInputBytes = trace.getInt(TRACE_SHIFT_INPUT_BYTES);
            double value = trace.getDouble(TRACE_SHIFT_VALUE);
            long nodeGuid = trace.getLong(TRACE_NODE_GUID);
            rootNode = new Node(
                nodeGuid,
                null,
                id,
                context,
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
            int context = trace.getInt(i + TRACE_SHIFT_CONTEXT);
            int direction = trace.getInt(i + TRACE_SHIFT_DIRECTION) == 0 ? 0 : 1;
            double value = trace.getDouble(i + TRACE_SHIFT_VALUE);
            long nodeGuid = trace.getLong(i + TRACE_NODE_GUID);

            if (!node.getLocationId().equals(id, context) || node.guid != nodeGuid)
                throw new Exception("Inconsistency in trace: " + path);

            if (node == analyses[analysisIndex].getNode())
                analysisNodeValue = value; 

            node.updateBestValue(analysisIndex, value);
            node.incrementHitCount(analysisIndex);

            coverage[direction].putIfAbsent(new LocationId(id, context), analysisIndex);
            coveredIds[direction].putIfAbsent(id, analysisIndex);

            int j = i + NUM_TRACE_RECORD_ITEMS;
            if (j >= trace.length()) {
                node.updateChildLabel(
                    analysisIndex, direction,
                    termination.equals("NORMAL") ? Node.ChildLabel.END_NORMAL : Node.ChildLabel.END_EXCEPTIONAL
                    );
                return analysisNodeValue;
            }

            node.setChildLabel(analysisIndex, direction, Node.ChildLabel.VISITED);
            Node[] children = node.getChildren();
            if (children[direction] == null) {
                int sId = trace.getInt(j + TRACE_SHIFT_ID);
                int sContext = trace.getInt(j + TRACE_SHIFT_CONTEXT);
                int sNumInputBytes = trace.getInt(j + TRACE_SHIFT_INPUT_BYTES);
                double sValue = trace.getDouble(j + TRACE_SHIFT_VALUE);
                long sNodeGuid = trace.getLong(j + TRACE_NODE_GUID);
                children[direction] = new Node(
                    sNodeGuid,
                    node,
                    sId,
                    sContext,
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

    public ExecutionTree() {
        clear();
    }

    public boolean isLoaded() {
        return rootNode != null && analyses != null;
    }

    public void clear() {
        rootNode = null;
        analyses = null;
        analysisIndex = 0;
        coverage = null;
    }
}
