package fizzer;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.awt.*;

import java.nio.ByteOrder;
import java.util.Vector;
import java.nio.ByteBuffer;

public class AnalysisPlainInputsViewer extends JPanel {
    public class BitsViewer extends TextViewerBase {
        protected int numLineColumnChars;
        protected int numBitColumnChars;

        public BitsViewer() {
            numLineColumnChars = 0;
            numBitColumnChars = 0;

            addMouseMotionListener(new MouseMotionListener() {
                private int lastIdx = -1;
                @Override
                public void mouseDragged(MouseEvent e) {}
                @Override
                @SuppressWarnings("deprecation")
                public void mouseMoved(MouseEvent e) {
                    int idx = viewToModel(e.getPoint());
                    if (idx != -1 && idx != lastIdx) {
                        int line, column, lineStartIdx, lineEndIdx;
                        try {
                            line = getLineOfOffset(idx);
                            lineStartIdx = getLineStartOffset(line);
                            lineEndIdx = getLineStartOffset(line + 1);
                            column = idx - lineStartIdx;
                        } catch (BadLocationException ex) {
                            return;
                        }
                        if (column > numLineColumnChars && column <= numLineColumnChars + numBitColumnChars) {
                            int charIndex = column - numLineColumnChars;
                            int sectionIndex = charIndex / 9;
                            int sectionShift = charIndex % 9;

                            int bitIndex = 8 * sectionIndex + sectionShift - 1;
                            lineColumnLabel.setText("Ln " + Integer.toString(line + 1) + ", Bit " + Integer.toString(bitIndex));

                            if (line != bitsNumericPreviewLine || sectionIndex != bitsNumericPreviewSection) {
                                bitsNumericPreviewLine = line;
                                bitsNumericPreviewSection = sectionIndex;
                                int numChars = Math.min(numBitColumnChars + 1, lineEndIdx - (lineStartIdx + numLineColumnChars));
                                String bitsString = null;
                                try { bitsString = getText(lineStartIdx + numLineColumnChars, numChars); }
                                catch (BadLocationException ex) {}
                                if (bitsString != null)
                                    try { updateBitsNumericPreviewLabel(bitsStringToBytes(bitsString)); } catch (Exception ex) {}
                            }
                        }
                        lastIdx = idx;
                    }
                }
            });
        }

        public void load() {
            clear();

            Analysis.InputsListInfo info = (Analysis.InputsListInfo)analysis.getInfo();

            int lineColumnSize = 1;
            for (int n = analysis.getNumTraces(); n > 10; n /= 10)
                ++lineColumnSize;

            numLineColumnChars = lineColumnSize + 2;
            numBitColumnChars = 0;

            String lineColumnFormat = "%" + Integer.toString(lineColumnSize) + "s";

            Vector<Integer> lineStarts = new Vector<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i != info.bits.size(); ++i) {
                stringBuilder.append(String.format(lineColumnFormat, i+1));
                stringBuilder.append(": ");

                int lineStart = stringBuilder.length();

                Analysis.InputsListInfo.TraceInputBits traceInputBits = info.bits.get(i);
                for (int j = 0, n = traceInputBits.getTraceBits().size(); j != n; ++j) {
                    stringBuilder.append(traceInputBits.getTraceBits().get(j) ? '1' : '0');
                    if (j + 1 < n && (j + 1) % 8 == 0)
                        stringBuilder.append(' ');
                }
                if (traceInputBits.getTraceBits().size() < traceInputBits.getNumGeneratedBits()) {
                    stringBuilder.append(' ');
                    for (int j = traceInputBits.getTraceBits().size(); j != traceInputBits.getNumGeneratedBits(); ++j) {
                        stringBuilder.append('X');
                        if (j + 1 < traceInputBits.getNumGeneratedBits() && (j + 1) % 8 == 0)
                            stringBuilder.append(' ');
                    }
                }

                int lineLength = stringBuilder.length() - lineStart;
                if (numBitColumnChars < lineLength)
                    numBitColumnChars = lineLength;

                if (traceInputBits.getNumGeneratedBits() < traceInputBits.getNumObtainedBits())
                    lineStarts.add(lineStart + traceInputBits.getNumGeneratedBits() + traceInputBits.getNumGeneratedBits() / 8);
                else if (traceInputBits.getNumGeneratedBits() > traceInputBits.getNumObtainedBits())
                    lineStarts.add(lineStart + traceInputBits.getNumObtainedBits() + traceInputBits.getNumObtainedBits() / 8);

                stringBuilder.append('\n');
            }

            String wholeText = stringBuilder.toString();

            setText(wholeText);
            setCaretPosition(0);

            for (int start : lineStarts) {
                int end = start + 1;
                while(end < wholeText.length() && wholeText.charAt(end) != '\n')
                    ++end;
                try {
                    getHighlighter().addHighlight(start, end, textHighlightPainter);
                } catch (BadLocationException e) { /* Ignore */ }
            }
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            int line; try { line = getLineOfOffset(charIdx); } catch (BadLocationException e) { return Color.LIGHT_GRAY; }
            Analysis.InputsListInfo.TraceInputBits traceInputBits = ((Analysis.InputsListInfo)analysis.getInfo()).bits.get(line);
            return traceInputBits.getNumObtainedBits() < traceInputBits.getNumGeneratedBits() ? LIGHT_RED : LIGHT_ORANGE;
        }
    }

    protected Analysis.Type type;
    protected Analysis analysis;
    protected BitsViewer bitsViewer;
    protected JLabel lineColumnLabel;
    protected String bitsNumericPreviewType;
    protected JComboBox<String> bitsNumericPreviewTypeCombo;
    protected String bitsNumericPreviewEndian;
    protected JComboBox<String> bitsNumericPreviewEndianCombo;
    protected JLabel bitsNumericPreviewLabel;
    protected int bitsNumericPreviewLine;
    protected int bitsNumericPreviewSection;

    public AnalysisPlainInputsViewer(Analysis.Type type_) {
        super(new BorderLayout(3,3));

        type = type_;
        analysis = null;
        bitsViewer = getBitsViewer();

        Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        lineColumnLabel = new JLabel("Ln 1, Bit 0");
        lineColumnLabel.setOpaque(true);
        lineColumnLabel.setFont(font);

        bitsNumericPreviewType = "int64";

        bitsNumericPreviewTypeCombo = new JComboBox<>(new String[] {
            "int8", "uint8", "int16", "uint16", "int32", "uint32", "int64", "uint64", "float", "double"
        });
        bitsNumericPreviewTypeCombo.setFont(font);
        bitsNumericPreviewTypeCombo.setSelectedItem(bitsNumericPreviewType);

        bitsNumericPreviewEndian = "little endian";

        bitsNumericPreviewEndianCombo = new JComboBox<>(new String [] {
            "little endian", "big endian"
        });
        bitsNumericPreviewEndianCombo.setFont(font);
        bitsNumericPreviewEndianCombo.setSelectedItem(bitsNumericPreviewEndian);

        bitsNumericPreviewLabel = new JLabel("0");
        bitsNumericPreviewLabel.setOpaque(true);
        bitsNumericPreviewLabel.setFont(font);

        bitsNumericPreviewLine = -1;
        bitsNumericPreviewSection = 0;

        JPanel statusComboPanel = new JPanel();
        statusComboPanel.setLayout(new BoxLayout(statusComboPanel, BoxLayout.X_AXIS));
        statusComboPanel.add(bitsNumericPreviewEndianCombo);
        statusComboPanel.add(bitsNumericPreviewTypeCombo);

        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.add(statusComboPanel, BorderLayout.WEST);
        statusPanel.add(bitsNumericPreviewLabel, BorderLayout.CENTER);
        statusPanel.add(lineColumnLabel, BorderLayout.EAST);

        JPanel northPanel = getNorthPanel(font);
        if (northPanel != null)
            add(northPanel, BorderLayout.NORTH);
        add(new JScrollPane(bitsViewer), BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    public void onAnalysisChanged(Analysis analysis_) {
        if (analysis_.getType()== type) {
            analysis = analysis_;

            bitsNumericPreviewLine = -1;
            bitsNumericPreviewSection = 0;

            loadWidgets();
        }
    }

    public void loadWidgets() {
        bitsViewer.load();
    }

    public BitsViewer getBitsViewer() {
        return new BitsViewer();
    }

    public JPanel getNorthPanel(Font font) {
        return null;
    }

    public void clear() {
        analysis = null;

        bitsNumericPreviewLine = -1;
        bitsNumericPreviewSection = 0;

        bitsViewer.clear();
        lineColumnLabel.setText("Ln 1, Bit 0");
        bitsNumericPreviewLabel.setText("0");
    }

    public void updateBitsNumericPreviewLabel(byte[] allBytes) throws Exception {
        int numBytes;
        boolean asUnsigned;
        boolean asInt;
        switch ((String)bitsNumericPreviewTypeCombo.getSelectedItem()) {
            case "int8":
                numBytes = 1;
                asUnsigned = false;
                asInt = true;
                break;
            case "uint8":
                numBytes = 1;
                asUnsigned = true;
                asInt = true;
                break;

            case "int16":
                numBytes = Math.min(2, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = false;
                asInt = true;
                break;
            case "uint16":
                numBytes = Math.min(2, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = true;
                asInt = true;
                break;

            case "int32":
                numBytes = Math.min(4, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = false;
                asInt = true;
                break;
            case "uint32":
                numBytes = Math.min(4, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = true;
                asInt = true;
                break;
            case "float":
                numBytes = Math.min(4, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = false;
                asInt = false;
                break;

            case "int64":
                numBytes = Math.min(8, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = false;
                asInt = true;
                break;
            case "uint64":
                numBytes = Math.min(8, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = true;
                asInt = true;
                break;
            case "double":
                numBytes = Math.min(8, allBytes.length - bitsNumericPreviewSection);
                asUnsigned = false;
                asInt = false;
                break;

            default: return;
        }

        byte[] bytes = new byte[] { 0,0,0,0, 0,0,0,0 };
        for (int i = 0; i != numBytes; ++i)
            bytes[i] = allBytes[bitsNumericPreviewSection + i];

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        switch ((String)bitsNumericPreviewEndianCombo.getSelectedItem()) {
            case "little endian": byteBuffer.order(ByteOrder.LITTLE_ENDIAN); break;
            case "big endian": byteBuffer.order(ByteOrder.BIG_ENDIAN); break;
            default: return;
        }

        String text;
        if (asInt) {
            if (numBytes == 1) {
                if (asUnsigned)
                    text = Integer.toString(Byte.toUnsignedInt(byteBuffer.get(0)));
                else
                    text = Integer.toString(byteBuffer.get(0));
            } else if (numBytes == 2) {
                short value = byteBuffer.getShort();
                if (asUnsigned)
                    text = Integer.toString(Short.toUnsignedInt(value));
                else
                    text = Short.toString(value);
            } else if (numBytes <= 4) {
                int value = byteBuffer.getInt();
                if (asUnsigned)
                    text = Integer.toUnsignedString(value);
                else
                    text = Integer.toString(value);
            } else {
                long value = byteBuffer.getLong();
                if (asUnsigned)
                    text = Long.toUnsignedString(value);
                else
                    text = Long.toString(value);
            }
        } else {
            if (numBytes <= 4)
                text = Float.toString(byteBuffer.getFloat());
            else
                text = Double.toString(byteBuffer.getDouble());
        }

        bitsNumericPreviewLabel.setText(text);
    }

    public byte[] bitsStringToBytes(String bits) {
        byte[] bytes = new byte[bits.length() / 9];
        for (int i = 0, k = 0; i < bits.length(); ++i, ++k) {
            bytes[k] = 0;
            for (int j = 0; !Character.isWhitespace(bits.charAt(i)); ++i, ++j)
                if (bits.charAt(i) == '1')
                    bytes[k] += 1 << (7 - j);
        }
        return bytes;
    }
}
