package fizzer;

import fizzer.Analysis.InputsListInfo.InputData;
import fizzer.Analysis.DataType;
import java.util.Vector;
import javax.swing.*;
import java.awt.*;


public class AnalysisPlainInputsViewer extends JPanel {
    public class InputsViewer extends TextViewerBase {
        public InputsViewer() {
            super();
        }

        public void load() {
            clear();
            Vector<InputData> inputs = ((Analysis.InputsListInfo)analysis.getInfo()).getInputs();

            int lineColumnSize = 1;
            for (int n = analysis.getNumTraces(); n > 10; n /= 10)
                ++lineColumnSize;

            String lineColumnFormat = "%" + Integer.toString(lineColumnSize) + "s";

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i != inputs.size(); ++i) {
                stringBuilder.append(String.format(lineColumnFormat, i+1));
                stringBuilder.append(": ");

                InputData inputData = inputs.get(i);

                for (int j = 0; j != inputData.getNumTypes(); ++j) {
                    DataType dataType = inputData.getTypes().get(j);
                    Number value = inputData.getValues().get(j);
                    stringBuilder.append(dataType.getAbbreviation());
                    stringBuilder.append(':');
                    stringBuilder.append(dataType.toStringNumber(value));
                    stringBuilder.append(' ');
                }

                stringBuilder.append('\n');
                stringBuilder.append(" ".repeat(lineColumnSize + 2));

                for (int j = 0; j != inputData.getNumMetadata(); ++j) {
                    stringBuilder.append(inputData.getMetadata().get(j).toString());
                    stringBuilder.append(' ');
                }

                stringBuilder.append('\n');
            }

            String wholeText = stringBuilder.toString();

            setText(wholeText);
            setCaretPosition(0);
        }

        @Override
        public Color getCharacterHighlightColor(int charIdx) {
            return LIGHT_RED;
        }
    }

    protected Analysis analysis;
    protected InputsViewer inputsViewer;

    public AnalysisPlainInputsViewer(Analysis.Type type_) {
        super(new BorderLayout(3,3));
        analysis = null;
        inputsViewer = new InputsViewer();
        add(new JScrollPane(inputsViewer), BorderLayout.CENTER);
    }

    public void onAnalysisChanged(Analysis analysis_) {
        if (analysis_ != analysis) {
            analysis = analysis_;
            if (analysis != null)
                inputsViewer.load();
        }
    }

    public void clear() {
        analysis = null;
        inputsViewer.clear();
    }
}
