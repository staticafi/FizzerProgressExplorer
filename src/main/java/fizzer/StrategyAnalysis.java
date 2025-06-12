package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.json.*;

public class StrategyAnalysis {

    private String strategy;
    private HashSet<Long> closedNodeGuids;

    public StrategyAnalysis(File analysisDir) throws Exception {
        strategy = "";
        closedNodeGuids = new HashSet<>();

        File file = new File(analysisDir, "strategy.json");
        if (file.isFile()) {
            JSONObject strategyJson = new JSONObject(
                Files.lines(Paths.get(file.getPath())).collect(Collectors.joining("\n"))
                );
            strategy = strategyJson.getString("strategy");
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

    public String getStrategy() {
        return strategy;
    }

    public LocationId getStrategyLocationID() {
        return strategy.isEmpty() ? null : new LocationId(Integer.parseUnsignedInt(strategy.substring(0, strategy.indexOf('_'))));
    }

    public HashSet<Long> getClosedNodeGuids() {
        return closedNodeGuids;
    }
}
