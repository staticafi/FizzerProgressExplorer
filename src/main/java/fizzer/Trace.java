package fizzer;

import java.util.Vector;

import org.json.JSONArray;

public class Trace {
    private Vector<Record> records;

    public class Record {
        private int id;
        private int direction;
        private int numInputBytes;
        private double value;
        private long nodeGuid;

        public Record(int id_, int direction_, int numInputBytes_, double value_, long nodeGuid_) {
            id = id_;
            direction = direction_;
            numInputBytes = numInputBytes_;
            value = value_;
            nodeGuid = nodeGuid_;
        }

        public int getId() {
            return id;
        }

        public int getDirection() {
            return direction;
        }

        public int getSid() {
            return id * direction;
        }

        public int getNumInputBytes() {
            return numInputBytes;
        }

        public double getValue() {
            return value;
        }

        public long getNodeGuid() {
            return nodeGuid;
        }
    }

    public Trace(JSONArray trace) {
        records = new Vector<>();

        final int NUM_TRACE_RECORD_ITEMS = 5;
        final int TRACE_SHIFT_ID = 0;
        final int TRACE_SHIFT_DIRECTION = 1;
        final int TRACE_SHIFT_INPUT_BYTES = 2;
        final int TRACE_SHIFT_VALUE = 3;
        final int TRACE_NODE_GUID = 4;
        for (int i = 0; i < trace.length(); i += NUM_TRACE_RECORD_ITEMS) {
            records.add(new Record(
                trace.getInt(i + TRACE_SHIFT_ID),
                trace.getInt(i + TRACE_SHIFT_DIRECTION) == 0 ? -1 : 1,
                trace.getInt(i + TRACE_SHIFT_INPUT_BYTES),
                trace.getDouble(i + TRACE_SHIFT_VALUE),
                trace.getLong(i + TRACE_NODE_GUID)
                ));
        }
    }

    public Vector<Record> getRecords() {
        return records;
    }
}
