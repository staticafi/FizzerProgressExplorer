package fizzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.json.*;

public class SourceMapping {

    public static class LineColumn {
        int line;
        int column;

        public LineColumn(int l, int c) {
            line = l;
            column = c;
        }
    }

    private List<String> sourceC; 
    private HashMap<Integer, LineColumn> condMapC;
    private HashMap<Integer, TreeMap<Integer, Integer>> invCondMapC;

    private List<String> sourceLL; 
    private HashMap<Integer, Integer> condMapLL;
    private HashMap<Integer, Integer> basicBlockLinesLL;
    private HashMap<Integer, Integer> invCondMapLL;

    public void load(String dir) throws Exception {
        clear();

        sourceC = null;
        condMapC = new HashMap<>();
        invCondMapC = new HashMap<>();

        sourceLL = null;
        condMapLL = new HashMap<>();
        basicBlockLinesLL = new HashMap<>();
        invCondMapLL = new HashMap<>();

        File sourceCFile = new File(dir + "/source.c");
        if (!sourceCFile.isFile())
            throw new RuntimeException("Cannot access file: " + sourceCFile.getAbsolutePath());
        sourceC = Files.lines(Paths.get(sourceCFile.getPath())).collect(Collectors.toList());

        File sourceLLFile = new File(dir + "/source.ll");
        if (!sourceLLFile.isFile())
            throw new RuntimeException("Cannot access file: " + sourceLLFile.getAbsolutePath());
        sourceLL = Files.lines(Paths.get(sourceLLFile.getPath())).collect(Collectors.toList());
        boolean isInFunction = false;
        for (int i = 0; i != sourceLL.size(); ++i) {
            String line = sourceLL.get(i);
            if (isInFunction) {
                if (line.startsWith("}"))
                    isInFunction = false;
                else if (line.startsWith("bb")) {
                    int end = line.indexOf(':');
                    if (end != -1)
                        try { basicBlockLinesLL.put(Integer.parseInt(line.substring(2, end)), i + 1); }
                        catch (NumberFormatException e) {}
                }
            } else if (line.startsWith("define "))
                isInFunction = true;
        }

        File condMapFile = new File(dir + "/cond_map.json");
        if (!condMapFile.isFile())
            throw new RuntimeException("Cannot access file: " + condMapFile.getAbsolutePath());
        JSONObject condMapObj = new JSONObject(
            Files.lines(Paths.get(condMapFile.getPath())).collect(Collectors.joining("\n"))
            );
        for (String keyName : condMapObj.keySet()) {
            JSONArray valueArray = condMapObj.getJSONArray(keyName);
            int id = Integer.parseInt(keyName);
            condMapC.put(id, new LineColumn(valueArray.getInt(0), valueArray.getInt(1)));
            condMapLL.put(id, basicBlockLinesLL.get(valueArray.getInt(2)) + valueArray.getInt(3));

            TreeMap<Integer, Integer> fromColumnMap = invCondMapC.get(valueArray.getInt(0));
            if (fromColumnMap == null) {
                fromColumnMap = new TreeMap<>();
                invCondMapC.put(valueArray.getInt(0), fromColumnMap);
            }
            fromColumnMap.put(valueArray.getInt(1), id);

            invCondMapLL.put(condMapLL.get(id), id);
        }
    }
    
    public LineColumn getCLineAndColumnWithId(Integer id) {
        return condMapC.get(id);
    }

    public Integer getLlvmLineWithId(Integer id) {
        return condMapLL.get(id);
    }

    public SourceMapping() {
        clear();
    }

    public LineColumn getCondMapC(int id) {
        return this.condMapC.get(id);
    }

    public int getCondMapCSize() {
        return this.condMapC.size();
    }

    public Integer getCondMapLL(int id) {
        return this.condMapLL.get(id);
    }

    public TreeMap<Integer, Integer> getInvCondMapC(int id) {
        return this.invCondMapC.get(id);
    }

    public Integer getInvCondMapLL(int id) {
        return this.invCondMapLL.get(id);
    }

    public List<String> getSourceC() {
        return this.sourceC;
    }

    public List<String> getSourceLL() {
        return this.sourceLL;
    }

    public void clear() {
        sourceC = null;
        condMapC = null;
        invCondMapC = null;

        sourceLL = null;
        condMapLL = null;
        basicBlockLinesLL = null;
        invCondMapLL = null;
    }
}
