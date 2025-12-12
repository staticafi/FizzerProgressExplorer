package fizzer;

public enum DataType {
    // Known types:
    BOOLEAN,
    UINT8,
    SINT8,
    UINT16,
    SINT16,
    UINT32,
    SINT32,
    UINT64,
    SINT64,
    FLOAT32,
    FLOAT64,
    // Unknown types:
    UNTYPED8,
    UNTYPED16,
    UNTYPED32,
    UNTYPED64;

    public static DataType parse(String typeName) {
        return DataType.valueOf(typeName);
    }

    public static DataType fromOrdinal(int ordinal) {
        return DataType.values()[ordinal];
    }

    public int getNumBytes() {
        switch (this)
        {
            case BOOLEAN: return 1;
            case UINT8: return 1;
            case SINT8: return 1;
            case UINT16: return 2;
            case SINT16: return 2;
            case UINT32: return 4;
            case SINT32: return 4;
            case UINT64: return 8;
            case SINT64: return 8;
            case FLOAT32: return 4;
            case FLOAT64: return 8;
            case UNTYPED8: return 1;
            case UNTYPED16: return 2;
            case UNTYPED32: return 4;
            case UNTYPED64: return 8;
            default: throw new RuntimeException("DataType.getNumBytes(): Unknown type of 'this'.");
        }
    }

    public String getAbbreviation() {
        switch (this)
        {
            case BOOLEAN: return "bO";
            case UINT8: return "u1";
            case SINT8: return "s1";
            case UINT16: return "u2";
            case SINT16: return "s2";
            case UINT32: return "u4";
            case SINT32: return "s4";
            case UINT64: return "u8";
            case SINT64: return "s8";
            case FLOAT32: return "f4";
            case FLOAT64: return "f8";
            case UNTYPED8: return "x1";
            case UNTYPED16: return "x2";
            case UNTYPED32: return "x4";
            case UNTYPED64: return "x8";
            default: throw new RuntimeException("DataType.getAbbreviation(): Unknown type of 'this'.");
        }
    }

    public Number parseNumber(String hexBytes) {
        int numBytes = getNumBytes();
        if (hexBytes.length() != 2 * numBytes)
            throw new RuntimeException("The passed string does not have even number of characters");
        switch (this)
        {
            case BOOLEAN: return Integer.parseUnsignedInt(hexBytes, 16) != 0 ? Integer.valueOf(1) : Integer.valueOf(0);
            case UINT8: return Integer.parseUnsignedInt(hexBytes, 16);
            case SINT8: return Integer.valueOf((byte)Integer.parseInt(hexBytes, 16));
            case UINT16: return Integer.parseUnsignedInt(hexBytes, 16);
            case SINT16: return Integer.valueOf((short)Integer.parseInt(hexBytes, 16));
            case UINT32: return Integer.parseUnsignedInt(hexBytes, 16);
            case SINT32: return Integer.valueOf(Integer.parseUnsignedInt(hexBytes, 16));
            case UINT64: return Long.parseUnsignedLong(hexBytes, 16);
            case SINT64: return Long.valueOf(Long.parseUnsignedLong(hexBytes, 16));
            case FLOAT32: return Float.intBitsToFloat(Integer.parseUnsignedInt(hexBytes, 16));
            case FLOAT64: return Double.longBitsToDouble(Long.parseUnsignedLong(hexBytes, 16));
            case UNTYPED8: return Integer.parseUnsignedInt(hexBytes, 16);
            case UNTYPED16: return Integer.parseUnsignedInt(hexBytes, 16);
            case UNTYPED32: return Integer.parseUnsignedInt(hexBytes, 16);
            case UNTYPED64: return Long.parseUnsignedLong(hexBytes, 16);
            default: throw new RuntimeException("DataType.parseNumber(String): Unknown type of 'this'.");
        }
    }

    public String toStringNumber(Number number) {
        switch (this)
        {
            case BOOLEAN: return number.intValue() != 0 ? "true" : "false";
            case UINT8: return Integer.toUnsignedString(number.intValue());
            case SINT8: return Integer.toString(number.intValue());
            case UINT16: return Integer.toUnsignedString(number.intValue());
            case SINT16: return Integer.toString(number.intValue());
            case UINT32: return Integer.toUnsignedString(number.intValue());
            case SINT32: return Integer.toString(number.intValue());
            case UINT64: return Long.toUnsignedString(number.longValue());
            case SINT64: return Long.toString(number.longValue());
            case FLOAT32: return Float.toString(number.floatValue());
            case FLOAT64: return Double.toString(number.doubleValue());
            case UNTYPED8: return Integer.toUnsignedString(number.intValue());
            case UNTYPED16: return Integer.toUnsignedString(number.intValue());
            case UNTYPED32: return Integer.toUnsignedString(number.intValue());
            case UNTYPED64: return Long.toUnsignedString(number.longValue());
            default: throw new RuntimeException("DataType.toString(Number): Unknown type of 'this'.");
        }
    }
}