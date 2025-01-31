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
        SENSITIVITY,
        TYPED_MINIMIZATION,
        MINIMIZATION,
        BITSHARE;

        public static Type parse(String typeName) throws Exception {
            switch (typeName) {
                case "NONE": return NONE;
                case "SENSITIVITY": return SENSITIVITY;
                case "TYPED_MINIMIZATION": return TYPED_MINIMIZATION;
                case "MINIMIZATION": return MINIMIZATION;
                case "BITSHARE": return BITSHARE;
                default: throw new Exception("Unknown analysis name: " + typeName);
            }
        }
    }

    public static enum StopAttribute {
        INSTANT,
        EARLY,
        REGULAR;

        public static StopAttribute parse(String typeName) throws Exception {
            switch (typeName) {
                case "INSTANT": return INSTANT;
                case "EARLY": return EARLY;
                case "REGULAR": return REGULAR;
                default: throw new Exception("Unknown analysis stop attribute name: " + typeName);
            }
        }

        public static String getAbbreviation(StopAttribute attribute) {
            switch (attribute) {
                case INSTANT: return "I";
                case EARLY: return "E";
                case REGULAR: return "R";
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

    public static enum TypedMinimizationStage {
        SEED,
        PARTIALS,
        STEP;

        public static TypedMinimizationStage parse(String typeName) throws Exception {
            switch (typeName) {
                case "SEED": return SEED;
                case "PARTIALS": return PARTIALS;
                case "STEP": return STEP;
                default: throw new Exception("Unknown typed minimization stage name: " + typeName);
            }
        }
    }

    public static enum MinimizationStage {
        TAKE_NEXT_SEED,
        EXECUTE_SEED,
        STEP,
        PARTIALS,
        PARTIALS_EXTENDED;

        public static MinimizationStage parse(String typeName) throws Exception {
            switch (typeName) {
                case "TAKE_NEXT_SEED": return TAKE_NEXT_SEED;
                case "EXECUTE_SEED": return EXECUTE_SEED;
                case "STEP": return STEP;
                case "PARTIALS": return PARTIALS;
                case "PARTIALS_EXTENDED": return PARTIALS_EXTENDED;
                default: throw new Exception("Unknown minimization stage name: " + typeName);
            }
        }
    }

    public abstract class Info {
        private StopAttribute stopAttribute;
        private int numCoverageFailureResets;

        public Info(StopAttribute  stopAttribute_) {
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
            super(StopAttribute.REGULAR);
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

    public class SensitivityInfo extends InputsListInfo {
        public SensitivityInfo(JSONObject infoJson) throws Exception {
            super(infoJson);
        }
    }

    public class TypedMinimizationInfo extends Info {

        public class TraceInfo {
            public TypedMinimizationStage progressStage;
            public long variablesHash;
            public Vector<Object> variableValues;
            public double functionValue;

            public TraceInfo(JSONObject traceInfo) throws Exception {
                progressStage = TypedMinimizationStage.parse(traceInfo.getString("progress_stage"));
                variablesHash = traceInfo.getLong("variables_hash");
                variableValues = new Vector<>();
                JSONArray variableValuesJson = traceInfo.getJSONArray("variable_values");
                if (variableValuesJson.length() != typesOfVariables.size())
                    throw new Exception("The count of values of variables does not match the count of types of the variables.");
                for (int j = 0; j != variableValuesJson.length(); ++j)
                    switch (typesOfVariables.get(j)) {
                        case BOOLEAN: variableValues.add(variableValuesJson.getBoolean(j)); break;
                        case UINT8: variableValues.add(variableValuesJson.getInt(j)); break;
                        case SINT8: variableValues.add(variableValuesJson.getInt(j)); break;
                        case UINT16: variableValues.add(variableValuesJson.getInt(j)); break;
                        case SINT16: variableValues.add(variableValuesJson.getInt(j)); break;
                        case UINT32: variableValues.add(variableValuesJson.getInt(j)); break;
                        case SINT32: variableValues.add(variableValuesJson.getInt(j)); break;
                        case UINT64: variableValues.add(variableValuesJson.getLong(j)); break;
                        case SINT64: variableValues.add(variableValuesJson.getLong(j)); break;
                        case FLOAT32: variableValues.add(variableValuesJson.getFloat(j)); break;
                        case FLOAT64: variableValues.add(variableValuesJson.getDouble(j)); break;
                        default: throw new Exception("Unknown or unspecified type of input bits for variable's value.");
                    }
                try { functionValue = traceInfo.getDouble("function_value"); }
                catch (JSONException e) { functionValue = Double.POSITIVE_INFINITY; }
            }
        }

        public class MappingToInputBits {
            public int inputStartBitIndex;
            public Vector<Short> valueBitIndices;

            public MappingToInputBits(int inputStartBitIndex_) {
                inputStartBitIndex = inputStartBitIndex_;
                valueBitIndices = new Vector<>();
            }
        }

        public class ExceptionCacheHit {
            public int traceIndex;
            public long variablesHash;
            public TypedMinimizationStage progressStage;

            public ExceptionCacheHit(int traceIndex_, long variablesHash_, TypedMinimizationStage progressStage_) {
                traceIndex = traceIndex_;
                variablesHash = variablesHash_;
                progressStage = progressStage_;
            }
        }

        public Vector<TraceInfo> traces;
        public Vector<Boolean> allInputBits;
        public Vector<TypeOfInputBits> allInputTypes;
        public Vector<MappingToInputBits> fromVariablesToInput;
        public Vector<TypeOfInputBits> typesOfVariables;
        public Vector<ExceptionCacheHit> executionCacheHits;
            
        public TypedMinimizationInfo(JSONObject infoJson) throws Exception {
            super(infoJson);

            traces = new Vector<>();
            allInputBits = new Vector<>();
            allInputTypes = new Vector<>();
            fromVariablesToInput = new Vector<>();
            typesOfVariables = new Vector<>();
            executionCacheHits = new Vector<>();

            JSONArray allInputBitsJson = infoJson.getJSONArray("all_input_bits");
            for (int j = 0; j != allInputBitsJson.length(); ++j)
                allInputBits.add(allInputBitsJson.getInt(j) == 1);

            JSONArray allInputTypesJson = infoJson.getJSONArray("all_input_types");
            for (int j = 0; j != allInputTypesJson.length(); ++j)
                allInputTypes.add(TypeOfInputBits.parse(allInputTypesJson.getString(j)));

            JSONArray fromVariablesToInputJson = infoJson.getJSONArray("from_variables_to_input");
            for (int j = 0; j < fromVariablesToInputJson.length(); ++j) {
                MappingToInputBits mapping = new MappingToInputBits(fromVariablesToInputJson.getInt(j));
                int numBitIndices = fromVariablesToInputJson.getInt(++j);
                for (int k = 0; k < numBitIndices; ++k)
                    mapping.valueBitIndices.add((short)fromVariablesToInputJson.getInt(++j));
                fromVariablesToInput.add(mapping);
            }

            JSONArray typesOfVariablesJson = infoJson.getJSONArray("types_of_variables");
            for (int j = 0; j != typesOfVariablesJson.length(); ++j)
                typesOfVariables.add(TypeOfInputBits.parse(typesOfVariablesJson.getString(j)));

            JSONArray executionCacheHitsJson = infoJson.getJSONArray("execution_cache_hits");
            for (int j = 0; j != executionCacheHitsJson.length(); j += 3)
                executionCacheHits.add(new ExceptionCacheHit(
                    executionCacheHitsJson.getInt(j + 0),
                    executionCacheHitsJson.getLong(j + 1),
                    TypedMinimizationStage.parse(executionCacheHitsJson.getString(j + 2))
                    ));
        }

        @Override
        public void readTraceInfo(JSONObject traceInfo, double analysisNodeValue) throws Exception {
            traces.add(new TraceInfo(traceInfo));
        }
    }

    public class MinimizationInfo extends Info {

        public class ExceptionCacheHit {
            public int traceIndex;
            public long bitsHash;

            public ExceptionCacheHit(int traceIndex_, long bitsHash_) {
                traceIndex = traceIndex_;
                bitsHash = bitsHash_;
            }
        }

        public class StageChange {
            public int index;
            public MinimizationStage stage;

            public StageChange(int index_, MinimizationStage stage_) {
                index = index_;
                stage = stage_;
            }
        }

        public Vector<Vector<Boolean>> bits;
        public Vector<Long> bitsHashes;
        public Vector<Double> values;

        public Vector<Integer> bitTranslation;
        public Vector<Boolean> allInputBits;
        public Vector<ExceptionCacheHit> executionCacheHits;
        public Vector<StageChange> stageChanges;

        public MinimizationInfo(JSONObject infoJson) throws Exception {
            super(infoJson);

            bits = new Vector<>();
            bitsHashes = new Vector<>();
            values = new Vector<>();

            bitTranslation = new Vector<>();
            JSONArray bitTranslationJson = infoJson.getJSONArray("bit_translation");
            for (int j = 0; j != bitTranslationJson.length(); ++j)
                bitTranslation.add(bitTranslationJson.getInt(j));

            allInputBits = new Vector<>();
            JSONArray allInputBitsJson = infoJson.getJSONArray("all_input_bits");
            for (int j = 0; j != allInputBitsJson.length(); ++j)
                allInputBits.add(allInputBitsJson.getInt(j) == 1);

            executionCacheHits = new Vector<>();
            JSONArray executionCacheHitsJson = infoJson.getJSONArray("execution_cache_hits");
            for (int j = 0; j != executionCacheHitsJson.length(); j += 2)
                executionCacheHits.add(new ExceptionCacheHit(
                    executionCacheHitsJson.getInt(j),
                    executionCacheHitsJson.getLong(j + 1)
                    ));

            stageChanges = new Vector<>();
            JSONArray stageChangesJson = infoJson.getJSONArray("stage_changes");
            for (int j = 0; j != stageChangesJson.length(); j += 2)
                stageChanges.add(new StageChange(
                    stageChangesJson.getInt(j),
                    MinimizationStage.parse(stageChangesJson.getString(j + 1))
                    ));
        }

        @Override
        public void readTraceInfo(JSONObject traceInfo, double analysisNodeValue) throws Exception {
            Vector<Boolean> traceBits = new Vector<>();
            JSONArray bitsJson = traceInfo.getJSONArray("bits");
            for (int j = 0; j != bitsJson.length(); ++j)
                traceBits.add(bitsJson.getInt(j) == 1);
            bits.add(traceBits);

            bitsHashes.add(traceInfo.getLong("bits_hash"));
            
            values.add(analysisNodeValue);
        }
    }

    public class BitshareInfo extends InputsListInfo {
        public BitshareInfo(JSONObject infoJson) throws Exception {
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
            case SENSITIVITY:
                setSensitiveBits(index, node, infoJson.getJSONArray("sensitive_bits"), infoFile.getPath());
                info = new SensitivityInfo(infoJson);
                break;
            case TYPED_MINIMIZATION:
                info = new TypedMinimizationInfo(infoJson);
                break;
            case MINIMIZATION:
                info = new MinimizationInfo(infoJson);
                break;
            case BITSHARE:
                info = new BitshareInfo(infoJson);
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
