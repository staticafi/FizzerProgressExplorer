package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.json.*;

public class StrategyAnalysis {

    private String metric;
    private String filter;
    private Vector<Pair<Double, Integer>> valuesAndGuids;
    private String navigatorName;
    private LocationId targetId;
    private LocationId bestNodeId;
    private int bestNodeGuid;
    private boolean sensitive;
    private double target_value;
    private HashSet<Long> closedNodeGuids;

    public StrategyAnalysis(File analysisDir) throws Exception {
        metric = null;
        filter = null;
        valuesAndGuids = new Vector<>();
        navigatorName = null;
        targetId = null;
        bestNodeId = null;
        bestNodeGuid = 0;
        sensitive = false;
        target_value = 0.0;
        closedNodeGuids = new HashSet<>();

        File file = new File(analysisDir, "strategy.json");
        if (file.isFile()) {
            JSONObject strategyJson = new JSONObject(
                Files.lines(Paths.get(file.getPath())).collect(Collectors.joining("\n"))
                );
            metric = strategyJson.getString("metric");
            filter = strategyJson.getString("filter");
            final JSONArray valuesAndGuidsArray = strategyJson.getJSONArray("values_and_node_guids");
            if (valuesAndGuidsArray.length() % 2 != 0)
                throw new RuntimeException("StrategyAnalysis[" + analysisDir + "]: JSON array 'values_and_node_guids' has odd count of elements.");
            for (int j = 0; j != valuesAndGuidsArray.length(); j += 2)
                valuesAndGuids.add(new Pair<Double, Integer>(valuesAndGuidsArray.getDouble(j), (int)valuesAndGuidsArray.getLong(j+1)));
            navigatorName = strategyJson.getString("navigator");
            targetId = new LocationId((int)strategyJson.getLong("target_location_id"));
            bestNodeId = new LocationId((int)strategyJson.getLong("best_node_location_id"));
            bestNodeGuid = (int)strategyJson.getLong("best_node_guid");
            sensitive = strategyJson.getInt("sensitive") != 0;
            target_value = strategyJson.getDouble("target_value");
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
}
