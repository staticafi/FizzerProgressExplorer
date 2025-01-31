package fizzer;

import java.util.*;
import javax.swing.*;
import java.awt.BorderLayout;

public class SourceViewerLL extends JPanel {
    public class SourceViewer extends SourceViewerBase {

        public SourceViewer() {
            super();
        }

        @Override
        public List<String> getSourceCodeLines() {
            return mapping.getSourceLL();
        }

        @Override
        public Object getInvertedMapping(int line) {
            return mapping.getInvCondMapLL(line);
        }

        @Override
        public boolean belongsToMark(char c) {
            return c != '\n';
        }

        @Override
        public boolean isCovered(int id, boolean direction) {
            return executionTree.isCovered(id, direction);
        }
    }

    private SourceMapping mapping;
    private ExecutionTree executionTree;
    private SourceViewer sourceViewer;

    public SourceViewerLL(SourceMapping sourceMapping_, ExecutionTree executionTree_) {
        super(new BorderLayout());

        mapping = sourceMapping_;
        executionTree = executionTree_;

        sourceViewer = new SourceViewer();

        JScrollPane sourceScrollPane = new JScrollPane(sourceViewer);
        sourceScrollPane.getHorizontalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);
        sourceScrollPane.getVerticalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);

        add(sourceScrollPane, BorderLayout.CENTER);
    }

    public SourceViewer getSourceViewer() {
        return sourceViewer;
    }

    public void onAnalysisChanged() {
        sourceViewer.onAnalysisChanged();
    }

    public void load() {
        sourceViewer.load();
    }

    public void clear() {
        sourceViewer.clear();
    }
}
