package fizzer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class NavigatorViewer extends JPanel {

    public NavigatorViewer(final ExecutionTreeViewer treeViewer_) {
        navigator = null;

        treeViewer = treeViewer_;

        final Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        targetSid = new Label(targetLabelPrefix + "0");
        targetSid.setFont(font);

        metricSelector = new JComboBox<>(MetricType.values());
        metricSelector.setFont(font);
        metricSelector.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ae) { compute(getTargetSid()); }
        });
        metricSelector.setSelectedItem(MetricType.BestValue);

        filterSelector = new JComboBox<FilterType>(FilterType.values());
        filterSelector.setFont(font);
        filterSelector.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ae) { compute(getTargetSid()); }
        });
        filterSelector.setSelectedItem(FilterType.All);

        targetMetric = new JTextField(8);
        targetMetric.setFont(font);
        targetMetric.setText("0.0");
        targetMetric.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (navigator == null) return;
                try {
                    final Navigator.NodeAndDirection nd = navigator.run(treeViewer.getTree(), Float.parseFloat(targetMetric.getText()));
                    treeViewer.setMark(nd.node, nd.direction);
                    treeViewer.makeMarkNodeVisible();
                    ((JTabbedPane)getParent()).setSelectedIndex(1); // Tree view tab.
                } catch (NumberFormatException e) {}
                redraw();
            }
        });

        locations = new JList<>(new DefaultListModel<Integer>());
        locations.setFont(font);
        locations.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        locations.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                locationsSelection.clear();
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
                for (int i = minIndex; i <= maxIndex; i++)
                    if (lsm.isSelectedIndex(i))
                    locationsSelection.add(locations.getModel().getElementAt(i));
                resize();
                redraw();
            }
        });
        locations.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("rawtypes") 
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Component self = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                self.setForeground(locationColors.get(locations.getModel().getElementAt(index)));
                return self;
            }
        });
        locationsSelection = new HashSet<>();
        locationColors = new HashMap<>();


        final JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(true);
        controlPanel.add(targetSid);
        controlPanel.add(targetMetric);
        controlPanel.add(metricSelector);
        controlPanel.add(filterSelector);

        final JScrollPane locationsScrollPane = new JScrollPane(locations);
        locationsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        locationsScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(true);
        leftPanel.add(controlPanel, BorderLayout.NORTH);
        leftPanel.add(locationsScrollPane, BorderLayout.CENTER);

        consumptionsPainter = new ConsumptionsPainter(font);
        final JScrollPane consumptionsScrollPane = new JScrollPane(consumptionsPainter);
        consumptionsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        consumptionsScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        countsPainter = new CountsPainter(font);
        final JScrollPane countsPainterScrollPane = new JScrollPane(countsPainter);
        countsPainterScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        countsPainterScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        ratiosPainters = new RatiosPainter[3];

        painterTabs = new JTabbedPane();
        painterTabs.addTab("Consumptions", consumptionsScrollPane);
        painterTabs.addTab("Counts", countsPainterScrollPane);
        for (int i = 0; i != 3; ++i) {
            ratiosPainters[i] = new RatiosPainter(font, i);
            final JScrollPane scrollPane = new JScrollPane(ratiosPainters[i]);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            painterTabs.addTab("Ratios" + Integer.toString(i), scrollPane);
        }
        painterTabs.setSelectedIndex(0);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, painterTabs);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(100);

        setLayout(new BorderLayout());
        add(splitPane);
    }

    public void clear() {
        navigator = null;

        locationsSelection.clear();
        locationColors.clear();

        targetSid.setText(targetLabelPrefix + Integer.toString(0));
        ((DefaultListModel<Integer>)locations.getModel()).clear();
        redraw();
    }

    public void onTargetChanged(final int sid) {
        compute(sid);
        locations.setSelectionInterval(0, locations.getModel().getSize() - 1);
        redraw();
    }

    private static enum MetricType {
        BestValue,
        InputSize,
        HitCount
    }

    private static enum FilterType {
        All,
        Warm,
        Cold,
        InputUse,
        InputWarm,
        InputCold
    }

    private int getTargetSid() { return Integer.parseInt(targetSid.getText().substring(targetLabelPrefix.length())); }
    private void setTargetSid(final int sid) { targetSid.setText(targetLabelPrefix + Integer.toString(sid)); }

    private void compute(final int tgtSid) {
        if (tgtSid == 0)
            return;

        final Vector<Integer> locationIndices = new Vector<>();
        for (int i = 0; i < locations.getModel().getSize(); ++i)
            if (locationsSelection.contains(locations.getModel().getElementAt(i)))
                locationIndices.add(i);

        clear();
        setTargetSid(tgtSid);

        Navigator.Metric metric;
        switch ((MetricType)metricSelector.getSelectedItem()) {
            case BestValue: metric = new Navigator.BestValue(treeViewer.getTree().getAnalysisIndex()); break;
            case InputSize: metric = new Navigator.InputSize(); break;
            case HitCount: metric = new Navigator.HitCount(); break;
            default: throw new RuntimeException();
        }

        Navigator.Filter filter;
        switch ((FilterType)filterSelector.getSelectedItem()) {
            case All: filter = new Navigator.KeepAll(); break;
            case Warm: filter = new Navigator.Signed(1.0f); break;
            case Cold: filter = new Navigator.Signed(-1.0f); break;
            case InputUse: filter = new Navigator.InputUse(); break;
            case InputWarm: filter = new Navigator.Signed(1.0f).then(new Navigator.InputUse()); break;
            case InputCold: filter = new Navigator.Signed(-1.0f).then(new Navigator.InputUse()); break;
            default: throw new RuntimeException();
        }

        navigator = new Navigator(treeViewer.getTree(), metric, filter, tgtSid);

        final Vector<Integer> sids = navigator.getSignedLocations();
        for (int i = 0; i < sids.size(); ++i)
            locationColors.put(sids.get(i), Color.getHSBColor(i/(float)(sids.size() + 1), 1.0f, 0.8f));
        for (int sid : sids)
            ((DefaultListModel<Integer>)(locations.getModel())).addElement(sid);
        locations.setSelectedIndices(locationIndices.stream().mapToInt(Integer::intValue).toArray());

        resize();
    }

    private void resize() {
        consumptionsPainter.resize();
        countsPainter.resize();
        for (int i = 0; i != 3; ++i)
            ratiosPainters[i].resize();
    }

    private void redraw() {
        switch (painterTabs.getSelectedIndex()) {
            case 0: consumptionsPainter.redraw(); break;
            case 1: countsPainter.redraw(); break;
            case 2: ratiosPainters[0].redraw(); break;
            case 3: ratiosPainters[1].redraw(); break;
            case 4: ratiosPainters[2].redraw(); break;
        }
    }

    private class Painter extends JPanel {
        Painter(final Font font) {
            setFont(font);
            setBackground(Color.white);
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
            if (navigator != null)
                setPreferredSize(new Dimension(paperWidth, (navigator.getConsumptions().size()) * samplesStride() + panelBottomMargin));
        }

        void redraw() {
            revalidate();
            repaint();
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (navigator != null)
                render(g);
        }

        protected void render(Graphics g) {}

        protected void renderLinesAndValues(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0, n = navigator.getValues().size(); i != n; ++i) {
                final int y = samplesStride() * (i+1);
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(sampleMarginLeft, y, sampleMarginRight, y);
                g.setColor(Color.BLACK);
                g.drawString(Float.toString(navigator.getValues().get(i)), sampleMarginRight + sampleValueShiftX, y + sampleValueShiftY);
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

    private class ConsumptionsPainter extends Painter {
        ConsumptionsPainter(final Font font) {
            super(font);
        }

        @Override
        protected void render(Graphics g) {
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
            for (int sid : locationsSelection) {
                g.setColor(locationColors.get(sid));
                for (int i = 0; i != navigator.getConsumptions().size(); ++i) {
                    final Vector<Float> sidX = navigator.getConsumptions().get(i).get(sid);
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

        private static final int curveHeight = 100;
        private static final int lineWidth = 2;
    }

    private abstract class InfosPainter extends Painter {
        InfosPainter(final Font font) { super(font); }

        float getMaxValue(Vector<Integer> sids) { return 1.0f; }
        abstract float getValue(Navigator.IdInfo info, int sid);
        abstract Navigator.Extrapolation getExtra(Navigator.IdExtra extra, int sid);

        @Override
        protected void render(Graphics g) {
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
            final Vector<Integer> sids = new Vector<>(locationsSelection.stream().sorted().toList());
            final float maxValue = Math.max(1.0f, getMaxValue(sids));
            for (int i = 0; i != navigator.getInfos().size(); ++i) {
                final int y = sampleLineY(i);
                for (int j = 0; j != sids.size(); ++j) {
                    final Navigator.IdInfo info = navigator.getInfos().get(i).get(Math.abs(sids.get(j)));
                    float value = info == null ? -1.0f : getValue(info, sids.get(j));
                    if (value > 0.0f) {
                        value /= maxValue;
                        g.setColor(locationColors.get(sids.get(j)));
                        int x0 = sampleLineX(0.0f);
                        int x1 = sampleLineX(value);
                        int y0 = y - (j + 1) * infoStride;
                        g.drawLine(x0, y0, x1, y0);
                    }
                }
            }
            ((Graphics2D)g).setStroke(new BasicStroke(2 * lineWidth));
            for (int i = 0; i != navigator.getValues().size(); ++i) {
                final int y = sampleLineY(i);
                for (int j = 0; j != sids.size(); ++j) {
                    final Navigator.IdExtra extra = navigator.getExtrapolations().get(Math.abs(sids.get(j)));
                    if (extra == null)
                        continue;
                    float value = getExtra(extra, sids.get(j)).apply(navigator.getValues().get(i));
                    value /= maxValue;
                    value = Math.max(0.0f, Math.min(1.0f, value));
                    g.setColor(locationColors.get(sids.get(j)));
                    int x0 = sampleLineX(value);
                    int y0 = y - (j + 1) * infoStride;
                    g.drawLine(x0, y0 - lineHeight/2, x0, y0 + lineHeight/2);
                }
            }
        }
        @Override
        protected int samplesStride() { return locationsSelection.size() * (infoStride + 1 + lineWidth) + 30; }

        private static final int infoStride = 15;
        private static final int lineWidth = 4;
        private static final int lineHeight = 6;
    }

    private class CountsPainter extends InfosPainter {
        CountsPainter(final Font font) { super(font); }
        @Override float getMaxValue(final Vector<Integer> sids) {
            int maxValue = 0;
            for (int i = 0; i != navigator.getInfos().size(); ++i)
                for (int sid : sids) {
                    final Navigator.IdInfo info = navigator.getInfos().get(i).get(Math.abs(sid));
                    if (info != null)
                        maxValue = Math.max(maxValue, info.counts[sid < 0 ? 0 : 1]);
                }
            return maxValue;
        }
        @Override float getValue(Navigator.IdInfo info, int sid) { return sid < 0 ? info.counts[0] : info.counts[1]; }
        @Override Navigator.Extrapolation getExtra(Navigator.IdExtra extra, int sid) { return sid < 0 ? extra.counts[0] : extra.counts[1]; }
    };

    private class RatiosPainter extends InfosPainter {
        RatiosPainter(final Font font, final int index_) { super(font); index = index_; }
        @Override float getMaxValue(Vector<Integer> sids) { return 1.0f; }
        @Override float getValue(Navigator.IdInfo info, int sid) { return sid < 0 ? info.ratios[0][index] : info.ratios[1][index]; }
        @Override Navigator.Extrapolation getExtra(Navigator.IdExtra extra, int sid) {
            return sid < 0 ? extra.ratios[0][index] : extra.ratios[1][index];
        }
        private final int index;
    }

    private Navigator navigator;

    private final ExecutionTreeViewer treeViewer;
    private final Label targetSid;
    private final JComboBox<MetricType> metricSelector;
    private final JComboBox<FilterType> filterSelector;
    private final JTextField targetMetric;
    private final JList<Integer> locations;
    private final HashSet<Integer> locationsSelection;
    private final HashMap<Integer, Color> locationColors;
    private final JTabbedPane painterTabs;
    private final ConsumptionsPainter consumptionsPainter;
    private final InfosPainter countsPainter;
    private final RatiosPainter[] ratiosPainters;

    private static final String targetLabelPrefix = "Tgt: ";
}
