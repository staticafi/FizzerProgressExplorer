package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.json.*;

public class StrategyAnalysis {

    public static class Extrapolation {
        public Extrapolation(final float c0_, final float c1_) { c0 = c0_; c1 = c1_; }
        public Extrapolation(final Vector<Vec2> input) {
            float A = 0, B = 0, C = 0, D = 0;
            for (int i = 0; i != input.size(); ++i) {
                Vec2 p = input.get(i);
                A += p.x * p.x;
                B += p.x;
                C += p.x * p.y;
                D += p.y;
            }
            c1 = input.isEmpty() || input.size() * A - B * B == 0.0f ? 0.0f : (input.size() * C - B * D) / (input.size() * A - B * B);
            c0 = input.isEmpty() ? 0.0f : (D - c1 * B) / input.size();
        }
        public float apply(final float value) { return c0 + value * c1; }
        private final float c0;
        private final float c1;
    }

    private String metric;
    private String filter;
    private Vector<Pair<Double, Integer>> valuesAndGuids;
    private HashMap<Integer, Extrapolation> extrapolations;
    private String navigatorName;
    private LocationId targetId;
    private boolean sensitive;
    private HashSet<Long> closedNodeGuids;
    private String strategyJsonText;

    public StrategyAnalysis(File analysisDir) throws Exception {
        metric = null;
        filter = null;
        valuesAndGuids = new Vector<>();
        navigatorName = null;
        targetId = null;
        sensitive = false;
        closedNodeGuids = new HashSet<>();
        strategyJsonText = "";

        File file = new File(analysisDir, "strategy.json");
        if (file.isFile()) {
            strategyJsonText = Files.readString(Paths.get(file.getPath()));
            JSONObject strategyJson = new JSONObject(strategyJsonText);
            metric = strategyJson.getString("metric");
            filter = strategyJson.getString("filter");
            final JSONArray valuesAndGuidsArray = strategyJson.getJSONArray("values_and_node_guids");
            if (valuesAndGuidsArray.length() % 2 != 0)
                throw new RuntimeException("StrategyAnalysis[" + analysisDir + "]: JSON array 'values_and_node_guids' has odd count of elements.");
            for (int j = 0; j < valuesAndGuidsArray.length(); j += 2)
                valuesAndGuids.add(new Pair<Double, Integer>(valuesAndGuidsArray.getDouble(j), (int)valuesAndGuidsArray.getLong(j+1)));
            navigatorName = strategyJson.getString("navigator");
            targetId = new LocationId((int)strategyJson.getLong("target_location_id"));
            sensitive = strategyJson.getInt("sensitive") != 0;
            extrapolations = new HashMap<>();
            final JSONArray extrapolationsArray = strategyJson.getJSONArray("extrapolations");
            if (extrapolationsArray.length() % 3 != 0)
                throw new RuntimeException("StrategyAnalysis[" + analysisDir + "]: JSON array 'extrapolations' has count of elements divisible by 3.");
            for (int j = 0; j < extrapolationsArray.length(); j += 3)
                extrapolations.put(extrapolationsArray.getInt(j), new Extrapolation(extrapolationsArray.getFloat(j+1), extrapolationsArray.getFloat(j+2)));
        }
        file = new File(analysisDir, "post.json");
        if (file.isFile()) {
            JSONObject postJson = new JSONObject(
                Files.lines(Paths.get(file.getPath())).collect(Collectors.joining("\n"))
                );
            JSONArray closedNodeGuidsArray = postJson.getJSONArray("closed_node_guids");
            for (int j = 0; j != closedNodeGuidsArray.length(); ++j)
                closedNodeGuids.add(closedNodeGuidsArray.getLong(j));
        }
    }

    public String getBasicInfoString() {
        return targetId == null ? "" : targetId.id + "_" + metric + "_" + filter + "_" + navigatorName.charAt(0) + "_" + (sensitive ? 1 : 0);
    }

    public LocationId getStrategyLocationID() {
        return targetId;
    }

    public HashSet<Long> getClosedNodeGuids() {
        return closedNodeGuids;
    }

    public String getJsonText() {
        return strategyJsonText;
    }

    public Vector<Pair<Double, Integer>> getValuesAndNodeGuids() {
        return valuesAndGuids;
    }

    public HashMap<Integer, Extrapolation> getExtrapolations() {
        return extrapolations;
    }
}
