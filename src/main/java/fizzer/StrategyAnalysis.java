package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.json.*;

public class StrategyAnalysis {

    public static enum Strategy {
        NONE,
        LOOP_HEAD_SENSITIVE,
        LOOP_HEAD_OTHERS,
        SENSITIVE,
        UNTOUCHED,
        IID_TWINS_SENSITIVE,
        IID_TWINS_OTHERS,
        MONTE_CARLO,
        MONTE_CARLO_BACKWARD;

        public static Strategy parse(String strategyName) throws Exception {
            switch (strategyName) {
                case "NONE": return NONE;
                case "LOOP_HEAD_SENSITIVE": return LOOP_HEAD_SENSITIVE;
                case "LOOP_HEAD_OTHERS": return LOOP_HEAD_OTHERS;
                case "SENSITIVE": return SENSITIVE;
                case "UNTOUCHED": return UNTOUCHED;
                case "IID_TWINS_SENSITIVE": return IID_TWINS_SENSITIVE;
                case "IID_TWINS_OTHERS": return IID_TWINS_OTHERS;
                case "MONTE_CARLO": return MONTE_CARLO;
                case "MONTE_CARLO_BACKWARD": return MONTE_CARLO_BACKWARD;
                default: throw new Exception("Unknown strategy name: " + strategyName);
            }
        }
    }

    private Strategy strategy;
    private HashSet<Long> closedNodeGuids;

    public StrategyAnalysis(File analysisDir) throws Exception {
        strategy = Strategy.NONE;
        closedNodeGuids = new HashSet<>();

        File infoFile = new File(analysisDir, "strategy.json");
        if (!infoFile.isFile())
            return;

        JSONObject strategyJson = new JSONObject(
            Files.lines(Paths.get(infoFile.getPath())).collect(Collectors.joining("\n"))
            );

        strategy = Strategy.parse(strategyJson.getString("strategy"));

        JSONArray closedNodeGuidsArray = strategyJson.getJSONArray("closed_node_guids");
        for (int j = 0; j != closedNodeGuidsArray.length(); ++j)
            closedNodeGuids.add(closedNodeGuidsArray.getLong(j));
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public HashSet<Long> getClosedNodeGuids() {
        return closedNodeGuids;
    }
}
