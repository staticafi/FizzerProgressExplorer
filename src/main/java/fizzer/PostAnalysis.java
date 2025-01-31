package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.json.*;

public class PostAnalysis {

    public static enum Strategy {
        NONE,
        PRIMARY_LOOP_HEAD,
        PRIMARY_SENSITIVE,
        PRIMARY_UNTOUCHED,
        PRIMARY_IID_TWINS,
        MONTE_CARLO,
        MONTE_CARLO_BACKWARD;

        public static Strategy parse(String strategyName) throws Exception {
            switch (strategyName) {
                case "NONE": return NONE;
                case "PRIMARY_LOOP_HEAD": return PRIMARY_LOOP_HEAD;
                case "PRIMARY_SENSITIVE": return PRIMARY_SENSITIVE;
                case "PRIMARY_UNTOUCHED": return PRIMARY_UNTOUCHED;
                case "PRIMARY_IID_TWINS": return PRIMARY_IID_TWINS;
                case "MONTE_CARLO": return MONTE_CARLO;
                case "MONTE_CARLO_BACKWARD": return MONTE_CARLO_BACKWARD;
                default: throw new Exception("Unknown strategy name: " + strategyName);
            }
        }
    }

    private Strategy strategy;
    private HashSet<Long> closedNodeGuids;

    public PostAnalysis(File analysisDir) throws Exception {
        strategy = Strategy.NONE;
        closedNodeGuids = new HashSet<>();

        File infoFile = new File(analysisDir, "post.json");
        if (!infoFile.isFile())
            return;

        JSONObject postJson = new JSONObject(
            Files.lines(Paths.get(infoFile.getPath())).collect(Collectors.joining("\n"))
            );

        strategy = Strategy.parse(postJson.getString("strategy"));

        JSONArray closedNodeGuidsArray = postJson.getJSONArray("closed_node_guids");
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
