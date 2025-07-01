package fizzer;

import javax.swing.*;
import java.awt.BorderLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class StrategyViewer extends JPanel {
    private class JSONViewer extends RSyntaxTextArea {
        public JSONViewer() {
            TextViewerBase.setDarkTheme(this);
            setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        }
    }

    private JSONViewer viewer;

    public StrategyViewer() {
        super(new BorderLayout());

        viewer = new JSONViewer();

        JScrollPane sourceScrollPane = new JScrollPane(viewer);
        sourceScrollPane.getHorizontalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);
        sourceScrollPane.getVerticalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);

        add(sourceScrollPane, BorderLayout.CENTER);
    }

    public void onAnalysisChanged(StrategyAnalysis strategy) {
        viewer.setText(strategy.getJsonText());
        viewer.setCaretPosition(0);
    }

    public void clear() {
        viewer.setText("");
    }
}
