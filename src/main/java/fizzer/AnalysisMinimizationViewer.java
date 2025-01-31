package fizzer;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.awt.*;

public class AnalysisMinimizationViewer extends AnalysisPlainInputsViewer {

    public class WholeInputViewer extends TextViewerBase {
        public void load() {
            clear();

            Analysis.MinimizationInfo info = (Analysis.MinimizationInfo)analysis.getInfo();

            StringBuilder stringBuilder = new StringBuilder();
            for (int j = 0; j != info.allInputBits.size(); ++j) {
                if (info.bitTranslation.contains(j))
                    stringBuilder.append('X');
                else
                    stringBuilder.append(info.allInputBits.get(j) ? '1' : '0');
                if ((j + 1) % 8 == 0)
                    stringBuilder.append(' ');
            }

            String wholeText = stringBuilder.toString();

            setText(wholeText);
            setCaretPosition(0);

            for (int idx : info.bitTranslation) {
                int byteIndex = idx / 8;
                int bitIndex = idx - 8 * byteIndex;
                int charIndex = 9 * byteIndex + bitIndex;
                try {
                    getHighlighter().addHighlight(charIndex, charIndex + 1, textHighlightPainter);
                } catch (BadLocationException e) { /* Ignore */ }
            }
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            return LIGHT_RED;
        }

        public WholeInputViewer() {
            addMouseMotionListener(new MouseMotionListener() {
                private int lastIdx = -1;
                @Override
                public void mouseDragged(MouseEvent e) {}
                @Override
                @SuppressWarnings("deprecation")
                public void mouseMoved(MouseEvent e) {
                    int idx = viewToModel(e.getPoint());
                    if (idx != -1 && idx != lastIdx) {
                        int sectionIndex = idx / 9;
                        int sectionShift = idx % 9;
                        int bitIndex = Math.max(0, 8 * sectionIndex + sectionShift - 1);
                        lineColumnLabel.setText("Ln 1, Bit " + Integer.toString(bitIndex));

                        bitsNumericPreviewLine = -1;
                        bitsNumericPreviewSection = 0;
                        String bitsString = null;
                        try { bitsString = getText(9 * sectionIndex, Math.min(9 * 8, getText().length() - 9 * sectionIndex)); }
                        catch (BadLocationException ex) {}
                        if (bitsString != null)
                            try { updateBitsNumericPreviewLabel(bitsStringToBytes(bitsString)); } catch (Exception ex) {}

                        lastIdx = idx;
                    }
                }
            });
        }
    }

    public class BitsViewer extends AnalysisPlainInputsViewer.BitsViewer {
        private String lineColumnFormat;
        private HashMap<Integer, Analysis.MinimizationStage> highlights;

        private class HighlightsBuilder {
            private class BestTrial {
                double value = Double.MAX_VALUE;
                int line = -1;
                int lineStart = -1;

                private void update(double value_, int line_, int lineStart_) {
                    if (Math.abs(value) > Math.abs(value_)) {
                        value = value_;
                        line = line_;
                        lineStart = lineStart_;
                    }
                }
            }

            private Vector<Analysis.MinimizationInfo.StageChange> stageChanges;
            private Vector<Integer> lineStarts;
            private int stageIndex;
            private BestTrial bestTrial;

            private HighlightsBuilder(Vector<Analysis.MinimizationInfo.StageChange> stageChanges_) {
                stageChanges = stageChanges_;
                lineStarts = new Vector<>();
                stageIndex = 0;
                bestTrial = new BestTrial();
            }

            private void update(int traceIndex, int cacheHitIndex, int line, int lineStart, double value) {
                if (stageIndex == stageChanges.size())
                    return;
                Analysis.MinimizationInfo.StageChange stageChange = stageChanges.get(stageIndex);
                if (stageChange.index == traceIndex || -stageChange.index == cacheHitIndex) {
                    if (stageChange.stage == Analysis.MinimizationStage.STEP) {
                        if (bestTrial.line != -1) {
                            highlights.put(bestTrial.line - 1, Analysis.MinimizationStage.STEP);
                            lineStarts.add(bestTrial.lineStart);
                            bestTrial = new BestTrial();
                        }
                        ++stageIndex;
                        update(traceIndex, cacheHitIndex, line, lineStart, value);
                        return;
                    }
                    highlights.put(line - 1, stageChange.stage);
                    lineStarts.add(lineStart);
                    ++stageIndex;

                    switch (stageChange.stage) {
                        case TAKE_NEXT_SEED:
                        case EXECUTE_SEED:
                            bestTrial = new BestTrial();
                            break;
                        default: break;
                    }
                }
                bestTrial.update(value, line, lineStart);
            }
        }

        @Override
        public void load() {
            clear();

            Analysis.MinimizationInfo info = (Analysis.MinimizationInfo)analysis.getInfo();

            int lineColumnSize = 1;
            for (int n = analysis.getNumTraces(); n > 10; n /= 10)
                ++lineColumnSize;

            numLineColumnChars = lineColumnSize + 2;
            numBitColumnChars = info.bitTranslation.size() + info.bitTranslation.size()/9;

            lineColumnFormat = "%" + Integer.toString(lineColumnSize) + "s";

            highlights = new HashMap<>();

            HighlightsBuilder highlightsBuilder = new HighlightsBuilder(info.stageChanges);
            HashMap<Long, Integer> hashesToIndices = new HashMap<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0, j = 0, line = 1; i != info.bits.size(); ++i, ++line) {
                int lineStart = toTextLine(info.bits.get(i), info.values.get(i), line, -1, stringBuilder);
                hashesToIndices.put(info.bitsHashes.get(i), i);
                highlightsBuilder.update(i + 1, Integer.MAX_VALUE, line, lineStart, info.values.get(i));
                for ( ; j != info.executionCacheHits.size() && info.executionCacheHits.get(j).traceIndex == i + 1; ++j) {
                    ++line;
                    int idx = hashesToIndices.get(info.executionCacheHits.get(j).bitsHash);
                    lineStart = toTextLine(info.bits.get(idx), info.values.get(idx), line, idx, stringBuilder);
                    highlightsBuilder.update(i + 1, j, line, lineStart, info.values.get(idx));
                }
            }

            String wholeText = stringBuilder.toString();

            setText(wholeText);
            setCaretPosition(0);

            for (int start : highlightsBuilder.lineStarts) {
                int end = start + 1;
                while(end < wholeText.length() && wholeText.charAt(end) != '\n')
                    ++end;
                try {
                    getHighlighter().addHighlight(start, end, textHighlightPainter);
                } catch (BadLocationException e) { /* Ignore */ }
            }
        }

        private int toTextLine(Vector<Boolean> bits, double value, int line, int refIdx, StringBuilder stringBuilder) {
            stringBuilder.append(String.format(lineColumnFormat, line));
            stringBuilder.append(": ");
            int lineStart = stringBuilder.length();
            for (int k = 0; k != bits.size(); ++k) {
                stringBuilder.append(bits.get(k) ? '1' : '0');
                if ((k + 1) % 8 == 0)
                    stringBuilder.append(' ');
            }
            stringBuilder.append(" ->  | " + Double.toString(value) + " |");
            if (refIdx != -1)
                stringBuilder.append("  @" + Integer.toString(refIdx + 1));
            stringBuilder.append('\n');
            return lineStart;
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            int line;
            try { line = getLineOfOffset(charIdx); } catch (BadLocationException e) { return Color.LIGHT_GRAY; }
            Analysis.MinimizationStage stage = highlights.get(line);
            if (stage == null)
                return Color.LIGHT_GRAY;
            switch (stage) {
                case TAKE_NEXT_SEED: return LIGHT_RED;
                case EXECUTE_SEED: return LIGHT_RED;
                case STEP: return LIGHT_GREEN;
                case PARTIALS: return LIGHT_ORANGE;
                case PARTIALS_EXTENDED: return LIGHT_MAGENTA;
                default: return Color.LIGHT_GRAY;
            }
        }

        public BitsViewer() {
            super();
        }
    }

    private WholeInputViewer wholeInputViewer;

    public AnalysisMinimizationViewer(Analysis.Type type_) {
        super(type_);
    }

    @Override
    public void loadWidgets() {
        wholeInputViewer.load();
        bitsViewer.load();
    }

    @Override
    public BitsViewer getBitsViewer() {
        return new BitsViewer();
    }

    @Override
    public JPanel getNorthPanel(Font font) {
        wholeInputViewer = new WholeInputViewer();

        JPanel wholeInputPanel = new JPanel(new GridLayout(0,1));
        JScrollPane wholeInputScrollPane = new JScrollPane(wholeInputViewer);
        wholeInputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        wholeInputPanel.add(wholeInputScrollPane);
        return wholeInputPanel;
    }

    @Override
    public void clear() {
        super.clear();
        wholeInputViewer.clear();
    }
}
