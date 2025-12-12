package fizzer;

import java.awt.Dimension;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.*;
import org.json.*;

public class Analysis {

    public static enum Type {
        STARTUP,
        BITSHARE,
        LOCAL_SEARCH,
        BITFLIP,
        TAINT_REQ,
        TAINT_RES;

        public static Type parse(String typeName) {
            return Type.valueOf(typeName);
        }
    }

    public static enum StartAttribute {
        NONE,
        REGULAR,
        RESUMED;

        public static StartAttribute parse(String typeName) {
            return StartAttribute.valueOf(typeName);
        }

        public static String getAbbreviation(StartAttribute attribute) {
            switch (attribute) {
                case NONE: return "N";
                case REGULAR: return "R";
                case RESUMED: return "X";
                default: return "???";
            }
        }
    }

    public static enum StopAttribute {
        INSTANT,
        EARLY,
        REGULAR,
        INTERRUPTED,
        FAILED;

        public static StopAttribute parse(String typeName) {
            return StopAttribute.valueOf(typeName);
        }

        public static String getAbbreviation(StopAttribute attribute) {
            switch (attribute) {
                case INSTANT: return "I";
                case EARLY: return "E";
                case REGULAR: return "R";
                case INTERRUPTED: return "X";
                case FAILED: return "F";
                default: return "???";
            }
        }
    }

    public abstract class Info {
        private StartAttribute startAttribute;
        private StopAttribute stopAttribute;
        private int numCoverageFailureResets;

        public Info(StartAttribute  startAttribute_, StopAttribute  stopAttribute_) {
            startAttribute = startAttribute_;
            stopAttribute = stopAttribute_;
            numCoverageFailureResets = 0;
        }

        public Info(JSONObject infoJson) {
            startAttribute = StartAttribute.parse(infoJson.getString("start_attribute"));
            stopAttribute = StopAttribute.parse(infoJson.getString("stop_attribute"));
            numCoverageFailureResets = infoJson.getInt("num_coverage_failure_resets");
        }

        public abstract void readTraceInfo(JSONObject traceInfo) throws Exception;
    }

    public class InputsListInfo extends Info {
        private Vector<InputData> inputs;

        public InputsListInfo() {
            super(StartAttribute.NONE, StopAttribute.REGULAR);
            inputs = new Vector<>();
        }

        public InputsListInfo(JSONObject infoJson) {
            super(infoJson);
            inputs = new Vector<>();
        }

        public Vector<InputData> getInputs() {
            return inputs;
        }

        @Override
        public void readTraceInfo(JSONObject executionResults) {
            inputs.add(new InputData(executionResults));
        }
    }

    public class BitshareInfo extends InputsListInfo {
        public BitshareInfo(JSONObject infoJson) {
            super(infoJson);
        }
    }

    public class LocalSearchInfo extends InputsListInfo {
        public LocalSearchInfo(JSONObject infoJson) {
            super(infoJson);
        }
    }

    public class BitflipInfo extends InputsListInfo {
        public BitflipInfo(JSONObject infoJson) {
            super(infoJson);
        }
    }

    public class TaintRequestInfo extends InputsListInfo {
        public TaintRequestInfo(JSONObject infoJson) {
            super(infoJson);
        }
    }

    public class TaintResponseInfo extends InputsListInfo {
        public TaintResponseInfo(JSONObject infoJson) {
            super(infoJson);
        }
    }

    public class StartupInfo extends InputsListInfo {
    }

    public class ViewProps {
        Dimension area = new Dimension(0, 0);
        int maxDiscoveryIndex = 0;
    }

    private Type type;
    private int index;
    private int numTraces;
    private Node node;
    private Info info;
    private HashSet<LocationId> coveredLocationIds;

    private ViewProps viewProps;

    public Analysis(Type type_, int analysisIndex, int numTraces_, HashMap<Long,Node> fromGuidsToNodes, File analysisDir) throws Exception {
        type = type_;
        index = analysisIndex;
        numTraces = numTraces_;
        node = null;
        info = new StartupInfo();
        coveredLocationIds = new HashSet<>();
        viewProps = new ViewProps();

        File infoFile = new File(analysisDir, "info.json");
        if (!infoFile.isFile()) {
            if (!type.equals(Type.STARTUP))
                throw new RuntimeException("Cannot access analysis info JSON file: " + infoFile.getPath());
            return;
        }

        JSONObject infoJson = new JSONObject(
            Files.lines(Paths.get(infoFile.getPath())).collect(Collectors.joining("\n"))
            );

        node = fromGuidsToNodes.get(infoJson.getLong("node_guid"));

        switch (type) {
            case BITSHARE:
                info = new BitshareInfo(infoJson);
                break;
            case LOCAL_SEARCH:
                info = new LocalSearchInfo(infoJson);
                break;
            case BITFLIP:
                info = new BitflipInfo(infoJson);
                break;
            case TAINT_REQ:
                info = new TaintRequestInfo(infoJson);
                break;
            case TAINT_RES:
                setSensitiveBits(index, node, infoJson.getJSONArray("sensitive_bits"), infoFile.getPath());
                info = new TaintResponseInfo(infoJson);
                break;
            default:
                break;
        }
    }

    public StartAttribute getStartAttribute() {
        return info.startAttribute;
    }

    public StopAttribute getStopAttribute() {
        return info.stopAttribute;
    }

    public int getNumCoverageFailureResets() {
        return info.numCoverageFailureResets;
    }

    public int getNumTraces() {
        return numTraces;
    }

    public Type getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public Node getNode() {
        return this.node;
    }

    public HashSet<LocationId> getCoveredLocationIds() {
        return this.coveredLocationIds;
    }

    public Info getInfo() {
        return this.info;
    }

    public ViewProps getViewProps() {
        return this.viewProps;
    }

    public void readTraceInfo(JSONObject traceInfo) throws Exception {
        info.readTraceInfo(traceInfo);
    }

    public static void setSensitiveBits(int analysisIndex, Node leafNode, JSONArray bitsAlongPath, String filePath) {
        Node node = leafNode;
        int i = bitsAlongPath.length() - 1;
        for ( ; node != null && i >= 0 ; --i, node = node.getParent()) {
            HashSet<Integer> sensitiveBits = new HashSet<>();
            JSONArray bitsArray = bitsAlongPath.getJSONArray(i);
            for (int j = 0; j != bitsArray.length(); ++j)
                sensitiveBits.add(bitsArray.getInt(j));
            node.setSensitiveBits(analysisIndex, sensitiveBits);
        }
        if (node != null || i != -1)
            throw new RuntimeException("Cannot find analysis node in the empty tree. File: " + filePath);
    }
}
