package fizzer;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

public class InputData {
    private Vector<Number> values;
    private Vector<DataType> types;
    private Vector<Byte> metadata;
    private int numBytes;
    private int traceLength;
    private int traceEndNodeGuid;
    private int traceNumCovered;
    private String progressMessage;

    public InputData(JSONObject traceInfo) {
        JSONObject executionResults = traceInfo.getJSONObject("execution_results");

        String bytesString = executionResults.getString("bytes");
        if (bytesString.length() % 2 != 0)
            throw new RuntimeException("In trace JSON: execution_results/bytes: odd number of characters in the string.");
        numBytes = bytesString.length();
        values = new Vector<>();

        String typesString = executionResults.getString("types");
        if (typesString.length() % 2 != 0)
            throw new RuntimeException("In trace JSON: execution_results/types: odd number of characters in the string.");
        types = new Vector<>();

        int i = 0;
        for (int j = 0; j < typesString.length(); j += 2) {
            DataType dataType = DataType.fromOrdinal(Integer.parseInt(typesString.substring(j, j+2), 16));
            types.add(dataType);
            int k = i + 2 * dataType.getNumBytes();
            if (k > bytesString.length())
                throw new RuntimeException("In trace JSON: execution_results/bytes: too few bytes w.r.t. types.");
            StringBuilder stringBuilder = new StringBuilder();
            for (int u = i; u < k; u += 2) {
                int idx = k - (u - i) - 2;
                stringBuilder.append(bytesString.charAt(idx));
                stringBuilder.append(bytesString.charAt(idx + 1));
            }
            values.add(dataType.parseNumber(stringBuilder.toString()));
            i = k;
        }
        if (i != bytesString.length())
            throw new RuntimeException("In trace JSON: execution_results/bytes: too many bytes w.r.t. types.");

        String metadataString = executionResults.getString("metadata");
        if (metadataString.length() % 2 != 0)
            throw new RuntimeException("In trace JSON: execution_results/metadata: odd number of characters in the string.");
        metadata = new Vector<>();
        for (int j = 0; j < metadataString.length(); j += 2)
            metadata.add((byte)Integer.parseInt(metadataString.substring(j, j+2), 16));

        JSONArray traceJSON = executionResults.getJSONArray("trace");
        if (traceJSON.length() % 5 != 0)
            throw new RuntimeException("In trace JSON: execution_results/trace: unexpected array size.");
        traceLength = traceJSON.length() / 5;
        traceEndNodeGuid = traceJSON.isEmpty() ? 0 : traceJSON.getInt(traceJSON.length() - 1);
        traceNumCovered = traceInfo.getJSONArray("covered_locations").length();

        progressMessage = traceInfo.has("progress_message") ? traceInfo.getString("progress_message") : "";
    }

    public int getNumBytes() {
        return numBytes;
    }

    public int getNumTypes() {
        return types.size();
    }

    public int getNumMetadata() {
        return metadata.size();
    }

    public Vector<Number> getValues() {
        return values;
    }

    public Vector<DataType> getTypes() {
        return types;
    }

    public Vector<Byte> getMetadata() {
        return metadata;
    }

    public int getTraceLength() {
        return traceLength;
    }

    public int getTraceEndNodeGuid() {
        return traceEndNodeGuid;
    }

    public int getTraceNumCovered() {
        return traceNumCovered;
    }

    public String getProgressMessage() {
        return progressMessage;
    }
}