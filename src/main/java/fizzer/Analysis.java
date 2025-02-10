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
        NONE,
        BITSHARE,
        LOCAL_SEARCH,
        BITFLIP,
        TAINT_REQ,
        TAINT_RES;

        public static Type parse(String typeName) throws Exception {
            switch (typeName) {
                case "NONE": return NONE;
                case "BITSHARE": return BITSHARE;
                case "LOCAL_SEARCH": return LOCAL_SEARCH;
                case "BITFLIP": return BITFLIP;
                case "TAINT_REQ": return TAINT_REQ;
                case "TAINT_RES": return TAINT_RES;
                default: throw new Exception("Unknown analysis name: " + typeName);
            }
        }
    }

    public static enum StartAttribute {
        NONE,
        REGULAR,
        RESUMED;

        public static StartAttribute parse(String typeName) throws Exception {
            switch (typeName) {
                case "NONE": return NONE;
                case "REGULAR": return REGULAR;
                case "RESUMED": return RESUMED;
                default: throw new Exception("Unknown analysis start attribute name: " + typeName);
            }
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
        INTERRUPTED;

        public static StopAttribute parse(String typeName) throws Exception {
            switch (typeName) {
                case "INSTANT": return INSTANT;
                case "EARLY": return EARLY;
                case "REGULAR": return REGULAR;
                case "INTERRUPTED": return INTERRUPTED;
                default: throw new Exception("Unknown analysis stop attribute name: " + typeName);
            }
        }

        public static String getAbbreviation(StopAttribute attribute) {
            switch (attribute) {
                case INSTANT: return "I";
                case EARLY: return "E";
                case REGULAR: return "R";
                case INTERRUPTED: return "X";
                default: return "???";
            }
        }
    }

    public static enum TypeOfInputBits {
        // Known types:
        BOOLEAN,
        UINT8,
        SINT8,
        UINT16,
        SINT16,
        UINT32,
        SINT32,
        UINT64,
        SINT64,
        FLOAT32,
        FLOAT64,
        // Unknown types:
        UNTYPED8,
        UNTYPED16,
        UNTYPED32,
        UNTYPED64;

        public static TypeOfInputBits parse(String typeName) throws Exception {
            switch (typeName) {
                case "BOOLEAN": return BOOLEAN;
                case "UINT8": return UINT8;
                case "SINT8": return SINT8;
                case "UINT16": return UINT16;
                case "SINT16": return SINT16;
                case "UINT32": return UINT32;
                case "SINT32": return SINT32;
                case "UINT64": return UINT64;
                case "SINT64": return SINT64;
                case "FLOAT32": return FLOAT32;
                case "FLOAT64": return FLOAT64;
                case "UNTYPED8": return UNTYPED8;
                case "UNTYPED16": return UNTYPED16;
                case "UNTYPED32": return UNTYPED32;
                case "UNTYPED64": return UNTYPED64;
                default: throw new Exception("Unknown type name of input bits: " + typeName);
            }
        }
    }

    public abstract class Info {
        private StopAttribute stopAttribute;
        private int numCoverageFailureResets;

        public Info(StartAttribute  startAttribute_, StopAttribute  stopAttribute_) {
            stopAttribute = stopAttribute_;
            numCoverageFailureResets = 0;
        }

        public Info(JSONObject infoJson) throws Exception {
            stopAttribute = StopAttribute.parse(infoJson.getString("stop_attribute"));
            numCoverageFailureResets = infoJson.getInt("num_coverage_failure_resets");
        }

        public abstract void readTraceInfo(JSONObject traceInfo, double analysisNodeValue) throws Exception;
    }

    public class InputsListInfo extends Info {
        public class TraceInputBits {
            private int numGeneratedBits;
            private int numObtainedBits;
            private Vector<Boolean> traceBits;

            public TraceInputBits(JSONObject traceInfo) {
                numGeneratedBits = 8 * traceInfo.getInt("num_generated_input_bytes");
                numObtainedBits = 8 * traceInfo.getInt("num_obtained_input_bytes");
                traceBits = new Vector<>();
                JSONArray bytesJson = traceInfo.optJSONArray("obtained_input_bytes");
                for (int j = 0; j < bytesJson.length(); ++j)
                    for (int k = 0; k != 8; ++k)
                        traceBits.add((((byte)bytesJson.getInt(j)) & (1 << (7 - k))) != 0);    
            }

            public int getNumGeneratedBits() {
                return numGeneratedBits;
            }

            public int getNumObtainedBits() {
                return numObtainedBits;
            }

            public Vector<Boolean> getTraceBits() {
                return traceBits;
            }
        }

        public InputsListInfo() {
            super(StartAttribute.NONE, StopAttribute.REGULAR);
            bits = new Vector<>();
        }

        public InputsListInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
            bits = new Vector<>();
        }

        public Vector<TraceInputBits> bits;

        @Override
        public void readTraceInfo(JSONObject traceInfo, double analysisNodeValue) throws Exception {
            bits.add(new TraceInputBits(traceInfo));
        }
    }

    public class BitshareInfo extends InputsListInfo {
        public BitshareInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class LocalSearchInfo extends InputsListInfo {
        public LocalSearchInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class BitflipInfo extends InputsListInfo {
        public BitflipInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class TaintRequestInfo extends InputsListInfo {
        public TaintRequestInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class TaintResponseInfo extends InputsListInfo {
        public TaintResponseInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class NoneInfo extends InputsListInfo {
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

    private ViewProps viewProps;

    public Analysis(Type type_, int analysisIndex, int numTraces_, HashMap<Long,Node> fromGuidsToNodes, File analysisDir) throws Exception {
        type = type_;
        index = analysisIndex;
        numTraces = numTraces_;
        node = null;
        info = new NoneInfo();
        viewProps = new ViewProps();

        File infoFile = new File(analysisDir, "info.json");
        if (!infoFile.isFile()) {
            if (!type.equals(Type.NONE))
                throw new Exception("Cannot access analysis info JSON file: " + infoFile.getPath());
            return;
        }

        JSONObject infoJson = new JSONObject(
            Files.lines(Paths.get(infoFile.getPath())).collect(Collectors.joining("\n"))
            );

        node = fromGuidsToNodes.get(infoJson.getLong("node_guid"));
        if (node == null)
            throw new Exception("Cannot find analysis node by its guid. File: " + infoFile.getPath());

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

    public Info getInfo() {
        return this.info;
    }

    public ViewProps getViewProps() {
        return this.viewProps;
    }

    public void readTraceInfo(JSONObject traceInfo, double analysisNodeValue) throws Exception {
        info.readTraceInfo(traceInfo, analysisNodeValue);
    }

    public static void setSensitiveBits(int analysisIndex, Node leafNode, JSONArray bitsAlongPath, String filePath) throws Exception {
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
            throw new Exception("Cannot find analysis node in the empty tree. File: " + filePath);
    }
}
