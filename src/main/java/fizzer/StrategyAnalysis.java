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

        File infoFile = new File(analysisDir, "strategy.json");
        if (!infoFile.isFile())
            return;

        JSONObject strategyJson = new JSONObject(
            Files.lines(Paths.get(infoFile.getPath())).collect(Collectors.joining("\n"))
            );

        strategy = strategyJson.getString("strategy");

        JSONArray closedNodeGuidsArray = strategyJson.getJSONArray("closed_node_guids");
        for (int j = 0; j != closedNodeGuidsArray.length(); ++j)
            closedNodeGuids.add(closedNodeGuidsArray.getLong(j));
    }

    public String getStrategy() {
        return strategy;
    }

    public HashSet<Long> getClosedNodeGuids() {
        return closedNodeGuids;
    }
}
