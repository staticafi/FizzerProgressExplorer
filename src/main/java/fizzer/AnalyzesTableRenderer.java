package fizzer;

import javax.swing.JTable;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.Color;

public class AnalyzesTableRenderer extends DefaultTableCellRenderer {
    private final ExecutionTree executionTree;

    public AnalyzesTableRenderer(ExecutionTree tree) {
        executionTree = tree;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        if (table == null)
            return this;
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); 
        Color foregroundColor = table.getForeground();
        Color backgroundColor = table.getBackground();
        if (column == 1 && row >= 0 && executionTree.getAnalyses() != null && row < executionTree.getAnalyses().length) {
            Analysis analysis = executionTree.getAnalyses()[row];

            if (analysis.getType() == Analysis.Type.TAINT_REQ)
                foregroundColor = Color.GREEN;
            else if (analysis.getType() == Analysis.Type.TAINT_RES)
                foregroundColor = Color.GREEN;
            else {
                if (analysis.getNode() != null
                        && analysis.getNode().getChildLabel(row, 0) != Node.ChildLabel.NOT_VISITED
                        && analysis.getNode().getChildLabel(row, 1) != Node.ChildLabel.NOT_VISITED)
                    foregroundColor = Color.MAGENTA;
                else if (!analysis.getCoveredLocationIds().isEmpty()) {
                    if (analysis.getNode() != null && analysis.getCoveredLocationIds().contains(analysis.getNode().getLocationId()))
                        foregroundColor = Color.CYAN;
                    else
                        foregroundColor = Color.ORANGE;
                }
                else if (analysis.getStopAttribute() != Analysis.StopAttribute.INTERRUPTED)
                    foregroundColor = Color.RED;
                else if (analysis.getStartAttribute() == Analysis.StartAttribute.RESUMED)
                    foregroundColor = Color.LIGHT_GRAY;
            }
        }

        if (table.getSelectedRow() == row)
            backgroundColor = Color.YELLOW;

        component.setForeground(foregroundColor);
        component.setBackground(backgroundColor);
        return component;
    }

}
