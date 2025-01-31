package fizzer;

import java.util.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.awt.*;

public class AnalysisTypedMinimizationViewer extends JPanel {

    public class WholeInputViewer extends TextViewerBase {
        public void load() {
            clear();

            Analysis.TypedMinimizationInfo info = (Analysis.TypedMinimizationInfo)analysis.getInfo();

            StringBuilder stringBuilder = new StringBuilder();

            HashMap<Integer, Character> usedBitIndices = new HashMap<>();
            for (int i = 0; i != info.fromVariablesToInput.size(); ++i) {
                Analysis.TypedMinimizationInfo.MappingToInputBits mapping = info.fromVariablesToInput.get(i);
                char varCode = (char)('A' + i) > 'Z' ? '~' : (char)('A' + i);
                stringBuilder.append(varCode).append(": ").append(info.typesOfVariables.get(i).toString());
                if (i + 1 < info.fromVariablesToInput.size())
                    stringBuilder.append(", ");
                for (short shift : mapping.valueBitIndices)
                    usedBitIndices.put(mapping.inputStartBitIndex + shift, varCode);
            }
            stringBuilder.append('\n');
            int bitsStartIndex = stringBuilder.length();
            for (int j = 0; j != info.allInputBits.size(); ++j) {
                stringBuilder.append(usedBitIndices.getOrDefault(j, info.allInputBits.get(j) ? '1' : '0'));
                if ((j + 1) % 8 == 0)
                    stringBuilder.append(' ');
            }

            String wholeText = stringBuilder.toString();

            setText(wholeText);
            setCaretPosition(0);

            for (int idx : usedBitIndices.keySet()) {
                int byteIndex = idx / 8;
                int bitIndex = idx - 8 * byteIndex;
                int charIndex = bitsStartIndex + 9 * byteIndex + bitIndex;
                try {
                    getHighlighter().addHighlight(charIndex, charIndex + 1, textHighlightPainter);
                } catch (BadLocationException e) { /* Ignore */ }
            }
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            return LIGHT_RED;
        }
    }

    public class ExecutionsViewer extends TextViewerBase {
        private HashMap<Integer, Analysis.TypedMinimizationStage> highlights;

        public ExecutionsViewer() {
            super();
            highlights = null;
        }

        public void load() {
            clear();

            Analysis.TypedMinimizationInfo info = (Analysis.TypedMinimizationInfo)analysis.getInfo();

            int lineColumnSize = 1;
            for (int n = analysis.getNumTraces(); n > 10; n /= 10)
                ++lineColumnSize;

            String lineColumnFormat = "%" + Integer.toString(lineColumnSize) + "s";

            highlights = new HashMap<>();

            Analysis.TypedMinimizationStage activeStage = null;
            Vector<Integer> lineStarts = new Vector<>();
            HashMap<Long, Integer> hashesToIndices = new HashMap<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0, j = 0, line = 1; i != info.traces.size(); ++i, ++line) {
                Analysis.TypedMinimizationInfo.TraceInfo traceInfo = info.traces.get(i);
                int lineStart = toTextLine(traceInfo.variableValues, info.typesOfVariables, traceInfo.functionValue,
                                           lineColumnFormat, line, -1, stringBuilder);
                hashesToIndices.put(traceInfo.variablesHash, i);
                if (activeStage == null || activeStage != traceInfo.progressStage) {
                    activeStage = traceInfo.progressStage;
                    highlights.put(line - 1, activeStage);
                    lineStarts.add(lineStart);
                }
                for ( ; j != info.executionCacheHits.size() && info.executionCacheHits.get(j).traceIndex == i + 1; ++j) {
                    ++line;
                    Analysis.TypedMinimizationInfo.ExceptionCacheHit cacheHit = info.executionCacheHits.get(j);
                    int idx = hashesToIndices.get(cacheHit.variablesHash);
                    Analysis.TypedMinimizationInfo.TraceInfo refTraceInfo = info.traces.get(idx);
                    lineStart = toTextLine(refTraceInfo.variableValues, info.typesOfVariables, refTraceInfo.functionValue,
                                           lineColumnFormat, line, idx, stringBuilder);
                    if (activeStage != cacheHit.progressStage) {
                        activeStage = cacheHit.progressStage;
                        highlights.put(line - 1, activeStage);
                        lineStarts.add(lineStart);
                    }
                }
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

        private int toTextLine(
                Vector<Object> variableValues,
                Vector<Analysis.TypeOfInputBits> typesOfVariables,
                double functionValue,
                String lineColumnFormat,
                int line,
                int refIdx,
                StringBuilder stringBuilder
                ) {
            stringBuilder.append(String.format(lineColumnFormat, line));
            stringBuilder.append(": ");
            int lineStart = stringBuilder.length();
            for (int k = 0; k != variableValues.size(); ++k) {
                switch (typesOfVariables.get(k)) {
                    case BOOLEAN: stringBuilder.append((boolean)variableValues.get(k)); break;
                    case UINT8: stringBuilder.append((int)variableValues.get(k)); break;
                    case SINT8: stringBuilder.append((int)variableValues.get(k)); break;
                    case UINT16: stringBuilder.append((int)variableValues.get(k)); break;
                    case SINT16: stringBuilder.append((int)variableValues.get(k)); break;
                    case UINT32: stringBuilder.append((int)variableValues.get(k)); break;
                    case SINT32: stringBuilder.append((int)variableValues.get(k)); break;
                    case UINT64: stringBuilder.append((long)variableValues.get(k)); break;
                    case SINT64: stringBuilder.append((long)variableValues.get(k)); break;
                    case FLOAT32: stringBuilder.append((Float)variableValues.get(k)); break;
                    case FLOAT64: stringBuilder.append((Double)variableValues.get(k)); break;
                    default: stringBuilder.append("???"); break;
                }
                if (k + 1 < variableValues.size())
                    stringBuilder.append(',');
                stringBuilder.append(' ');
            }
            stringBuilder.append(" ->  | " + Double.toString(functionValue) + " |");
            if (refIdx != -1)
                stringBuilder.append("  @" + Integer.toString(refIdx + 1));
            stringBuilder.append('\n');
            return lineStart;
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            int line;
            try { line = getLineOfOffset(charIdx); } catch (BadLocationException e) { return Color.LIGHT_GRAY; }
            Analysis.TypedMinimizationStage stage = highlights.get(line);
            if (stage == null)
                return Color.LIGHT_GRAY;
            switch (stage) {
                case SEED: return LIGHT_RED;
                case PARTIALS: return LIGHT_ORANGE;
                case STEP: return LIGHT_GREEN;
                default: return Color.LIGHT_GRAY;
            }
        }
    }

    private Analysis.Type type;
    private Analysis analysis;
    private WholeInputViewer wholeInputViewer;
    private ExecutionsViewer executionsViewer;

    public AnalysisTypedMinimizationViewer() {
        super(new BorderLayout(3,3));

        type = Analysis.Type.TYPED_MINIMIZATION;
        analysis = null;
        wholeInputViewer = new WholeInputViewer();
        executionsViewer = new ExecutionsViewer();

        JPanel wholeInputPanel = new JPanel(new GridLayout(0,1));
        JScrollPane wholeInputScrollPane = new JScrollPane(wholeInputViewer);
        wholeInputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        wholeInputPanel.add(wholeInputScrollPane);

        add(wholeInputPanel, BorderLayout.NORTH);
        add(new JScrollPane(executionsViewer), BorderLayout.CENTER);
    }

    public void onAnalysisChanged(Analysis analysis_) {
        if (analysis_.getType()!= type || analysis_ == null)
            return;
        analysis = analysis_;
        wholeInputViewer.load();
        executionsViewer.load();
    }

    public void clear() {
        wholeInputViewer.clear();
        executionsViewer.clear();
    }
}
