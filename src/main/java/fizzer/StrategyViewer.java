package fizzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class StrategyViewer extends JPanel {
    public StrategyViewer(ExecutionTree tree) {
        super(new BorderLayout());

        strategy = null;
        executionTree = tree;

        jsonViewer = new JSONViewer();

        locationsModel = new DefaultListModel<Integer>();
        locationsSelectionModel = new DefaultListSelectionModel();
        locationsSelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        locationsSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                locationsSelection.clear();
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
                for (int i = minIndex; i <= maxIndex; i++)
                    if (lsm.isSelectedIndex(i))
                        locationsSelection.add(locationsModel.getElementAt(i));
                tracesPainter.redraw();
                countsPainter.redraw();
            }
        });
        locationsSelection = new HashSet<>();
        locationColors = new HashMap<>();

        tracesPainter = new TracesPainter();
        countsPainter = new CountsPainter();

        final JScrollPane jsonScrollPane = new JScrollPane(jsonViewer);
        jsonScrollPane.getHorizontalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);
        jsonScrollPane.getVerticalScrollBar().setUnitIncrement(ProgressExplorer.textScrollSpeed);

        final DefaultListCellRenderer locationsRenderer = new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("rawtypes") 
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Component self = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                self.setForeground(locationColors.get(locationsModel.getElementAt(index)));
                return self;
            }
        };

        final JList<Integer> tracesLocations = new JList<>(locationsModel);
        tracesLocations.setFont(fontNormal);
        tracesLocations.setBackground(ExecutionTreeViewer.DARK_BACKGROUND);
        tracesLocations.setSelectionBackground(Color.DARK_GRAY);
        tracesLocations.setSelectionModel(locationsSelectionModel);
        tracesLocations.setCellRenderer(locationsRenderer);
        final JScrollPane tracesLocationsScrollPane = new JScrollPane(tracesLocations);
        tracesLocationsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        tracesLocationsScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        final JScrollPane tracesPainterScrollPane = new JScrollPane(tracesPainter);
        tracesPainterScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        tracesPainterScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        final JSplitPane tracesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tracesLocationsScrollPane, tracesPainterScrollPane);
        tracesSplitPane.setOneTouchExpandable(true);
        tracesSplitPane.setDividerLocation(110);

        final JList<Integer> countsLocations = new JList<>(locationsModel);
        countsLocations.setFont(fontNormal);
        countsLocations.setBackground(ExecutionTreeViewer.DARK_BACKGROUND);
        countsLocations.setSelectionBackground(Color.DARK_GRAY);
        countsLocations.setSelectionModel(locationsSelectionModel);
        countsLocations.setCellRenderer(locationsRenderer);
        final JScrollPane countsLocationsScrollPane = new JScrollPane(countsLocations);
        countsLocationsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        countsLocationsScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        final JScrollPane countsPainterScrollPane = new JScrollPane(countsPainter);
        countsPainterScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        countsPainterScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        final JSplitPane countsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, countsLocationsScrollPane, countsPainterScrollPane);
        countsSplitPane.setOneTouchExpandable(true);
        countsSplitPane.setDividerLocation(110);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("JSON", jsonScrollPane);
        tabbedPane.addTab("Traces", tracesSplitPane);
        tabbedPane.addTab("Counts", countsSplitPane);
        tabbedPane.setSelectedIndex(0);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void onAnalysisChanged() {
        clear();

        strategy = executionTree.getStrategyAnalysisSelectingNode();
        if (strategy == null)
            return;

        jsonViewer.onStrategyChanged();

        final Vector<Integer> sids = new Vector<>();
        if (strategy.getExtrapolations() != null)
            sids.addAll(strategy.getExtrapolations().keySet().stream().sorted().toList());
        for (int i = 0; i < sids.size(); ++i)
            locationColors.put(sids.get(i), Color.getHSBColor(i/(float)(sids.size() + 1), 0.6f, 0.9f));
        for (int sid : sids)
            locationsModel.addElement(sid);
        locationsSelectionModel.setSelectionInterval(0, locationsModel.getSize() - 1);

        tracesPainter.resize();
        tracesPainter.redraw();

        countsPainter.resize();
        countsPainter.redraw();
    }

    public void clear() {
        strategy = null;

        jsonViewer.clear();

        locationsModel.clear();
        locationsSelectionModel.clearSelection();
        locationsSelection.clear();
        locationColors.clear();

        tracesPainter.clear();
        tracesPainter.resize();
        tracesPainter.redraw();

        countsPainter.clear();
        countsPainter.resize();
        countsPainter.redraw();
    }

    private class JSONViewer extends RSyntaxTextArea {
        JSONViewer() {
            TextViewerBase.setDarkTheme(this);
            setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        }
        void onStrategyChanged() { setText(strategy.getJsonText()); setCaretPosition(0); }
        void clear() { setText(""); }
    }

    private class Painter extends JPanel {
        Painter() {
            setFont(fontNormal);
            setBackground(ExecutionTreeViewer.DARK_BACKGROUND);
            setForeground(ExecutionTreeViewer.DARK_FOREGROUND);
            setAutoscrolls(true);
            setOpaque(true);
            MouseAdapter ma = new MouseAdapter() {
                private Point origin = null;
    
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1)
                        origin = new Point(e.getPoint());
                }
    
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1)
                        origin = null;
                }
    
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (origin != null) {
                        Rectangle rect = new Rectangle(getVisibleRect());
                        rect.x += origin.x - e.getX();
                        rect.y += origin.y - e.getY();
                        scrollRectToVisible(rect);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void resize() {
            setPreferredSize(new Dimension(paperWidth, (strategy == null ? 0 : strategy.getValuesAndNodeGuids().size()) * samplesStride() + panelBottomMargin));
        }

        void redraw() {
            revalidate();
            repaint();
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (strategy != null)
                render(g);
        }

        protected void render(Graphics g) {}

        protected void renderLinesAndValues(Graphics g) {
            g.setColor(ExecutionTreeViewer.DARK_FOREGROUND);
            g.setFont(fontNormal);
            for (int i = 0, n = strategy.getValuesAndNodeGuids().size(); i != n; ++i) {
                final int y = samplesStride() * (i+1);
                g.drawLine(sampleMarginLeft, y, sampleMarginRight, y);
                g.drawString(
                    Double.toString(strategy.getValuesAndNodeGuids().get(i).getKey())
                        + " (" +  Integer.toString(strategy.getValuesAndNodeGuids().get(i).getValue())+ ')',
                    sampleMarginRight + sampleValueShiftX,
                    y + sampleValueShiftY
                    );
            }
        }

        protected int sampleLineX(final float t) { return Math.round(sampleMarginLeft + t * (sampleMarginRight - sampleMarginLeft)); }
        protected int sampleLineY(final int index) { return samplesStride() * (index + 1); }
        protected int samplesStride() { return 20; }

        protected static final int paperWidth = 1111;
        protected static final int sampleMarginLeft = 10;
        protected static final int sampleMarginRight = paperWidth - 100;
        protected static final int sampleValueShiftX = 10;
        protected static final int sampleValueShiftY = 5;
        protected static final int panelBottomMargin = 20;
    }

    private class TracesPainter extends Painter {
        @Override
        protected void render(Graphics g) {
            if (consumptions == null)
                buildConsumptions();
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
            for (int sid : locationsSelection) {
                g.setColor(locationColors.get(sid));
                for (int i = 0; i != strategy.getValuesAndNodeGuids().size(); ++i) {
                    final Vector<Float> sidX = consumptions.get(i).get(sid);
                    if (sidX != null) {
                        final int y = sampleLineY(i);
                        int x0 = sampleLineX(0.0f);
                        int y0 = 0;
                        int count = 0;
                        for (float x : sidX) {
                            ++count;
                            final float h = count / (float)sidX.size();
                            final int x1 = sampleLineX(x);
                            g.drawLine(x0, y - y0, x1, y - y0);
                            g.drawLine(x1, y - y0, x1, y - Math.round(h * curveHeight));
                            x0 = x1;
                            y0 = Math.round(h * curveHeight);
                        }
                        g.drawLine(x0, y - y0, sampleLineX(1.0f), y - curveHeight);
                    }
                }
            }
        }

        @Override
        protected int samplesStride() { return curveHeight + 20; }

        void clear() { consumptions = null; }
        void buildConsumptions() {
            consumptions = new Vector<>();
            for (Pair<Double, Integer> valueAndGuid : strategy.getValuesAndNodeGuids()) {
                final Node node = executionTree.getNodeByGuid(valueAndGuid.getValue());
                final HashMap<Integer, Vector<Float>> map = new HashMap<>();
                for (Node n = node.getParent(), m = node; n != null; m = n, n = n.getParent()) {
                    final int sid = (n.getChildren()[0] == m ? -1 : 1) * n.getLocationId().id;
                    final float x = n.getTraceIndex() / (float)Math.max(1, node.getTraceIndex());
                    map.compute(sid, (k, v) -> {
                        if (v == null)
                            v = new Vector<>();
                        v.add(0, x);
                        return v;
                    });
                }
                consumptions.add(map);
            }
        }

        private Vector<HashMap<Integer, Vector<Float>>> consumptions;

        private static final int curveHeight = 100;
        private static final int lineWidth = 2;
    }

    private class CountsPainter extends Painter {
        @Override
        protected void render(Graphics g) {
            if (counts == null)
                buildCounts();
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
            final Vector<Integer> sids = new Vector<>(locationsSelection.stream().sorted().toList());
            for (int i = 0; i != counts.size(); ++i) {
                final int y = sampleLineY(i);
                for (int j = 0; j != sids.size(); ++j) {
                    final Integer count = counts.get(i).get(sids.get(j));
                    if (count != null) {
                        final float value = (float)count / (float)maxCount;
                        g.setColor(locationColors.get(sids.get(j)));
                        int x0 = sampleLineX(0.0f);
                        int x1 = sampleLineX(value);
                        int y0 = y - (j + 1) * infoStride;
                        g.drawLine(x0, y0, x1, y0);
                    }
                }
            }
            ((Graphics2D)g).setStroke(new BasicStroke(2 * lineWidth));
            g.setFont(fontSmall);
            for (int i = 0; i != strategy.getValuesAndNodeGuids().size(); ++i) {
                final int y = sampleLineY(i);
                for (int j = 0; j != sids.size(); ++j) {
                    final StrategyAnalysis.Extrapolation extrapolation = strategy.getExtrapolations().get(sids.get(j));
                    final float value = extrapolation == null ? 0.0f : extrapolation.apply(strategy.getValuesAndNodeGuids().get(i).getKey().floatValue());
                    float valueScaled = value / maxCount;
                    valueScaled = Math.max(0.0f, Math.min(1.0f, valueScaled));
                    g.setColor(locationColors.get(sids.get(j)));
                    int x0 = sampleLineX(valueScaled);
                    int y0 = y - (j + 1) * infoStride;
                    g.drawLine(x0, y0 - lineHeight/2, x0, y0 + lineHeight/2);

                    final Integer count = counts.get(i).get(sids.get(j));
                    float countScaled = count == null ? 0.0f : (float)count / (float)maxCount;
                    countScaled = Math.max(0.0f, Math.min(1.0f, countScaled));
                    x0 = sampleLineX(Math.max(valueScaled, countScaled)) + 10;
                    g.drawString(
                        Integer.toString(count == null ? 0 : count) + " (" +  Integer.toString(Math.round(value))+ ')',
                        x0, y0 + fontSmall.getSize() / 2 - 1
                        );
                }
            }
        }
        @Override
        protected int samplesStride() { return locationsSelection.size() * (infoStride + 1 + lineWidth) + 30; }

        void clear() { counts = null; }
        void buildCounts() {
            counts = new Vector<>();
            maxCount = 1;
            for (Pair<Double, Integer> valueAndGuid : strategy.getValuesAndNodeGuids()) {
                final Node node = executionTree.getNodeByGuid(valueAndGuid.getValue());
                final HashMap<Integer, Integer> map = new HashMap<>();
                for (Node n = node.getParent(), m = node; n != null; m = n, n = n.getParent()) {
                    final int sid = (n.getChildren()[0] == m ? -1 : 1) * n.getLocationId().id;
                    map.compute(sid, (k, v) -> { return v == null ? 1 : v + 1; });
                }
                counts.add(map);
                for (int cnt : map.values())
                    maxCount = Math.max(maxCount, cnt);
            }
        }

        private Vector<HashMap<Integer, Integer>> counts;
        private int maxCount;

        private static final int infoStride = 15;
        private static final int lineWidth = 4;
        private static final int lineHeight = 6;
    }

    private static final Font fontNormal = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);
    private static final Font fontSmall = new Font("Monospaced", Font.PLAIN, Math.round(0.75f * ProgressExplorer.textFontSize));

    private StrategyAnalysis strategy;
    private final ExecutionTree executionTree;
    private final JSONViewer jsonViewer;
    private final DefaultListModel<Integer> locationsModel;
    private final DefaultListSelectionModel locationsSelectionModel;
    private final HashSet<Integer> locationsSelection;
    private final HashMap<Integer, Color> locationColors;
    private final TracesPainter tracesPainter;
    private final CountsPainter countsPainter;
    private final JTabbedPane tabbedPane;
}
