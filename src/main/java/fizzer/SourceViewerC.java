package fizzer;

import java.util.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.*;

public class SourceViewerC extends JPanel {

    public class SourceViewer extends SourceViewerBase {

        public SourceViewer() {
            super();
            addMouseMotionListener(new MouseMotionListener() {
                private int lastIdx = -1;
                @Override
                public void mouseDragged(MouseEvent e) {}
                @Override
                @SuppressWarnings("deprecation")
                public void mouseMoved(MouseEvent e) {
                    int idx = viewToModel(e.getPoint());
                    if (idx != -1 && idx != lastIdx) {
                        int line, column;
                        try {
                            line = getLineOfOffset(idx);
                            column = idx - getLineStartOffset(line);
                        } catch (BadLocationException ex) {
                            return;
                        }
                        if (column > numLineColumnChars)
                            lineColumnLabel.setText("Ln " + Integer.toString(line + 1) + ", Col " + Integer.toString(column - numLineColumnChars));
                        lastIdx = idx;
                    }
                }
            });
        }

        @Override
        public List<String> getSourceCodeLines() {
            return mapping.getSourceC();
        }

        @Override
        public Object getInvertedMapping(int line) {
            return mapping.getInvCondMapC(line);
        }

        @Override
        public boolean belongsToMark(char c) {
            switch (c) {
                case '!':
                case '=':
                case '<':
                case '>':
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean isCovered(int id, boolean direction) {
            return executionTree.isCovered(id, direction);
        }
    }

    private SourceMapping mapping;
    private ExecutionTree executionTree;
    private SourceViewer sourceViewer;
    private JLabel coverageInfoLabel;
    private JLabel lineColumnLabel;

    public SourceViewerC(SourceMapping sourceMapping_, ExecutionTree executionTree_) {
        super(new BorderLayout());

        mapping = sourceMapping_;
        executionTree = executionTree_;

        sourceViewer = new SourceViewer();

        Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        coverageInfoLabel = new JLabel("");
        coverageInfoLabel.setOpaque(true);
        coverageInfoLabel.setFont(font);

        lineColumnLabel = new JLabel("Ln 1, Col 1");
        lineColumnLabel.setOpaque(true);
        lineColumnLabel.setFont(font);

        JScrollPane sourceScrollPaneC = new JScrollPane(sourceViewer);
        sourceScrollPaneC.getHorizontalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);
        sourceScrollPaneC.getVerticalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(coverageInfoLabel, BorderLayout.CENTER);
        statusPanel.add(lineColumnLabel, BorderLayout.EAST);

        add(sourceScrollPaneC, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    public void updateCoverageInfo() {
        HashSet<Integer> leftCovered = new HashSet<>();
        for (Map.Entry<Integer,Integer> entry : executionTree.getCoveredIds()[0].entrySet())
            if (entry.getValue() <= executionTree.getAnalysisIndex())
                leftCovered.add(entry.getKey());
        int numLeftCovered = leftCovered.size();
        int numRightCovered = 0;
        int numBothCovered = 0;
        int numNoneCovered = 0;
        for (Map.Entry<Integer,Integer> entry : executionTree.getCoveredIds()[1].entrySet())
            if (entry.getValue() <= executionTree.getAnalysisIndex()) {
                ++numRightCovered;
                if (leftCovered.contains(entry.getKey()))
                    ++numBothCovered;
            } else if (!leftCovered.contains(entry.getKey()))
                ++numNoneCovered;
        int numAllLocations = mapping.getCondMapCSize();
        float coverage = numAllLocations == 0 ? 1.0f : (float)numBothCovered / (float)numAllLocations;

        coverageInfoLabel.setText(
            "Coverage: " + String.format(Locale.US, "%.2f", 100 * coverage) + '%' +
            ", left: " + Integer.toString(numLeftCovered) +
            ", right: " + Integer.toString(numRightCovered) +
            ", both: " + Integer.toString(numBothCovered) +
            ", none: " + Integer.toString(numNoneCovered) +
            ", all: " + Integer.toString(numAllLocations)
            );
    }
    
    public SourceViewer getSourceViewer() {
        return this.sourceViewer;
    }

    public void onAnalysisChanged() {
        sourceViewer.onAnalysisChanged();
        updateCoverageInfo();
    }

    public void load() {
        sourceViewer.load();
    }

    public void clear() {
        sourceViewer.clear();
        coverageInfoLabel.setText("Ln 1, Col 1");
        lineColumnLabel.setText("");
    }
}