package fizzer;

import java.util.*;

public class Node {

    public class ViewProps {
        int x = 0;
        int y = 0;
        int subTreeMinX = 0;
        int subTreeMaxX = 0;
    }

    public static enum ChildLabel {
        NOT_VISITED(0),         // children[?] == null
        END_EXCEPTIONAL(1),     // children[?] == null
        END_NORMAL(2),          // children[?] == null
        VISITED(3);             // children[?] != null

        public int value;

        private ChildLabel(int value_) {
            value = value_;
        }

        public static ChildLabel min(ChildLabel left, ChildLabel right) {
            return left.value <= right.value ? left : right;
        }

        public static ChildLabel max(ChildLabel left, ChildLabel right) {
            return left.value >= right.value ? left : right;
        }
    }

    public final long guid;

    private Node parent;
    private Node[] children;
    private TreeMap<Integer,ChildLabel>[] childLabels;

    private LocationId locationId;
    private TreeMap<Integer,Double> bestValue;
    private int traceIndex;
    private int numInputBytes;

    private int discoveryIndex;

    private TreeMap<Integer,Integer> hitCount;

    private TreeMap<Integer,HashSet<Integer>> sensitiveBits;

    private int bitshareIndex;
    private int localSearchIndex;
    private int bitflipIndex;
    private int sensitivityIndex;

    private int closedIndex;

    private ViewProps viewProps;

    @SuppressWarnings("unchecked")
    public Node(
            long guid_,
            Node parent_,
            int id_,
            double bestValue_,
            int traceIndex_,
            int numInputBytes_,
            int analysisIndex_,
            int discoveryIndex_
            ) {
        guid = guid_;

        parent = parent_;
        children = new Node[] { null, null };
        childLabels = (TreeMap<Integer,ChildLabel>[]) new TreeMap[] { new TreeMap<Integer,ChildLabel>(), new TreeMap<Integer,ChildLabel>() };
        childLabels[0].put(analysisIndex_, ChildLabel.NOT_VISITED);
        childLabels[1].put(analysisIndex_, ChildLabel.NOT_VISITED);

        locationId = new LocationId(id_);
  
        bestValue = new TreeMap<Integer, Double>();
        bestValue.put(analysisIndex_, bestValue_);
  
        traceIndex = traceIndex_;

        numInputBytes = numInputBytes_;

        discoveryIndex = discoveryIndex_;

        hitCount = new TreeMap<Integer,Integer>();
        hitCount.put(analysisIndex_, 0);

        sensitiveBits = new TreeMap<Integer,HashSet<Integer>>();
        sensitiveBits.put(analysisIndex_, new HashSet<>());

        bitshareIndex = Integer.MAX_VALUE;
        localSearchIndex = Integer.MAX_VALUE;
        bitflipIndex = Integer.MAX_VALUE;
        sensitivityIndex = Integer.MAX_VALUE;

        closedIndex = Integer.MAX_VALUE;

        viewProps = new ViewProps();
    }

    public LocationId getLocationId() {
        return this.locationId;
    }

    public int getNumInputBytes() {
        return this.numInputBytes;
    }

    public Node[] getChildren() {
        return this.children;
    }

    public int getDiscoveryIndex() {
        return this.discoveryIndex;
    }

    public int getTraceIndex() {
        return this.traceIndex;
    }

    public Node getParent() {
        return this.parent;
    }

    public void setBitShareIndex(int index) {
        this.bitshareIndex = index;
    }

    public void setLocalSearchIndex(int index) {
        this.localSearchIndex = index;
    }

    public void setBitFlipIndex(int index) {
        this.bitflipIndex = index;
    }

    public void setSensitivityIndex(int index) {
        this.sensitivityIndex = index;
    }

    public void setClosedIndex(int index) {
        this.closedIndex = index;
    }

    public ChildLabel getChildLabel(int analysisIndex, int direction) {
        return childLabels[direction].floorEntry(analysisIndex).getValue();
    }

    public void setChildLabel(int analysisIndex, int direction, ChildLabel label) {
        childLabels[direction].put(analysisIndex, label);
    }

    public void updateChildLabel(int analysisIndex, int direction, ChildLabel label) {
        Map.Entry<Integer, ChildLabel> prevEntry = childLabels[direction].floorEntry(analysisIndex - 1);
        setChildLabel(analysisIndex, direction, ChildLabel.max(label, prevEntry == null ? label : prevEntry.getValue()));
    }

    public Double getBestValue(int analysisIndex) {
        return bestValue.floorEntry(analysisIndex).getValue();
    }

    public void updateBestValue(int analysisIndex, double value) {
        Map.Entry<Integer, Double> prevEntry = bestValue.floorEntry(analysisIndex - 1);
        bestValue.put(analysisIndex, Math.min(getBestValue(analysisIndex), prevEntry == null ? value : prevEntry.getValue() ));
    }

    public int getHitCount(int analysisIndex) {
        return hitCount.floorEntry(analysisIndex).getValue();
    }

    public void incrementHitCount(int analysisIndex) {
        Integer oldValue = hitCount.get(analysisIndex);
        if (oldValue == null)
            hitCount.put(analysisIndex, hitCount.floorEntry(analysisIndex - 1).getValue() + 1);
        else
            hitCount.put(analysisIndex, oldValue + 1);
    }

    public HashSet<Integer> getSensitiveBits(int analysisIndex) {
        return sensitiveBits.floorEntry(analysisIndex).getValue();
    }

    public void setSensitiveBits(int analysisIndex, HashSet<Integer> bitIndices) {
        sensitiveBits.put(analysisIndex, bitIndices);
    }

    public boolean bitshareApplied(int analysisIndex) {
        return bitshareIndex <= analysisIndex;
    }

    public boolean localSearchApplied(int analysisIndex) {
        return localSearchIndex <= analysisIndex;
    }

    public boolean bitflipApplied(int analysisIndex) {
        return bitflipIndex <= analysisIndex;
    }

    public boolean sensitivityApplied(int analysisIndex) {
        return sensitivityIndex <= analysisIndex;
    }

    public boolean isClosed(int analysisIndex) {
        return closedIndex <= analysisIndex;
    }

    public ViewProps getViewProps() {
        return this.viewProps;

    }
}
