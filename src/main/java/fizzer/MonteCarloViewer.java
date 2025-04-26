package fizzer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class MonteCarloViewer extends JPanel {

    public MonteCarloViewer(MonteCarlo method, SourceMapping sourceMapping) {
        this.sourceMapping = sourceMapping;
        this.method = method;
        this.activeLocations = new Vector<>();
        this.locationColors = new HashMap<>();

        Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        targetLabel = new Label("Tgt: 0");
        targetLabel.setFont(font);

        locations = new JList<>(new DefaultListModel<Integer>());
        locations.setFont(font);
        locations.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        locations.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                activeLocations.clear();
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
                for (int i = minIndex; i <= maxIndex; i++)
                    if (lsm.isSelectedIndex(i))
                        activeLocations.add(locations.getModel().getElementAt(i));
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

        samplesPainter = new SamplesPainter(font);
        sizesPainter = new SizesPainter(font);
        frequenciesPainter = new FrequenciesPainter(font);

        JScrollPane locationsScrollPane = new JScrollPane(locations);
        locationsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        locationsScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JScrollPane samplesScrollPane = new JScrollPane(samplesPainter);
        samplesScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        samplesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JScrollPane sizesScrollPane = new JScrollPane(sizesPainter);
        sizesScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        sizesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JScrollPane frequenciesScrollPane = new JScrollPane(frequenciesPainter);
        frequenciesScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        frequenciesScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(true);
        leftPanel.add(targetLabel, BorderLayout.NORTH);
        leftPanel.add(locationsScrollPane, BorderLayout.CENTER);

        stages = new JTabbedPane();
        stages.addTab("Samples", samplesScrollPane);
        stages.addTab("Sizes", sizesScrollPane);
        stages.addTab("Frequencies", frequenciesScrollPane);
        stages.setSelectedIndex(0);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, stages);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(100);

        setLayout(new BorderLayout());
        add(splitPane);
    }

    public void clear() {
        method.clear();

        activeLocations.clear();
        locationColors.clear();

        ((DefaultListModel<Integer>)locations.getModel()).clear();
        redraw();
    }

    public void onAnalysisChanged() {
        clear();
    }

    public boolean onTargetChanged(final Node node) {
        clear();
        if (!method.setTargetSid(node)) {
            targetLabel.setText("Tgt: 0");
            return false;
        }
        compute();
        return true;
    }

    public void onTargetChanged(final int sid) {
        clear();
        method.setTargetSid(sid);
        compute();
    }

    private void compute() {
        method.compute();

        targetLabel.setText("Tgt: " + Integer.toString(method.getTargetSIid()));
        computeLocationColors();
        for (int sid : method.getSignedLocations())
            ((DefaultListModel<Integer>)(locations.getModel())).addElement(sid);
        locations.setSelectedIndex(0);
        resize();
        redraw();
    }

    private void computeLocationColors() {
        java.util.List<Integer> allLocations = method.getSignedLocations();
        locationColors.clear();
        for (int i = 0; i < allLocations.size(); ++i)
            locationColors.put(allLocations.get(i), Color.getHSBColor(i/(float)(allLocations.size() + 1), 1.0f, 0.8f));
    }

    private void resize() {
        samplesPainter.resize();
        sizesPainter.resize();
        frequenciesPainter.resize();
    }

    private void redraw() {
        switch (stages.getSelectedIndex()) {
            case 0: samplesPainter.redraw(); break;
            case 1: sizesPainter.redraw(); break;
            case 2: frequenciesPainter.redraw(); break;
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
            setPreferredSize(new Dimension(paperWidth, (method.getTraces().size() + 1) * samplesStride));
        }

        void redraw() {
            revalidate();
            repaint();
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!method.isEmpty())
                render(g);
        }

        protected void render(Graphics g) {}

        protected void renderLinesAndValues(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            for (int sid : activeLocations) {
                final Vector<Vector<Float>> samples = method.getSamples(sid);
                for (int i = 0; i != samples.size(); ++i) {
                    final int y = samplesStride * (i+1);
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawLine(sampleMarginLeft, y, sampleMarginRight, y);
                    g.setColor(Color.BLACK);
                    g.drawString(Double.toString(method.getSampleValue(i)), sampleMarginRight + sampleValueShiftX, y + sampleValueShiftY);
                }
            }
        }

        protected int sampleLineX(float t) {
            return Math.round(sampleMarginLeft + t * (sampleMarginRight - sampleMarginLeft));
        }

        protected static final int paperWidth = 1250;
        protected static final int sampleSize = 6;
        protected static final int samplesStride = 20;
        protected static final int sampleMarginLeft = 10;
        protected static final int sampleMarginRight = paperWidth - 100;
        protected static final int sampleValueShiftX = 10;
        protected static final int sampleValueShiftY = 5;
    }

    private class SamplesPainter extends Painter {
        SamplesPainter(final Font font) {
            super(font);
        }

        @Override
        protected void render(Graphics g) {
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(3));
            for (int sid : activeLocations) {
                g.setColor(locationColors.get(sid));
                final Vector<Vector<Float>> samples = method.getSamples(sid);
                for (int i = 0; i != samples.size(); ++i) {
                    final int y = samplesStride * (i+1);
                    for (float t : samples.get(i)) {
                        final int x = sampleLineX(t);
                        g.drawLine(x, y-sampleSize/2, x, y+sampleSize/2);
                    }
                }
            }
        }
    }

    private class SizesPainter extends Painter {
        SizesPainter(final Font font) {
            super(font);
        }

        @Override
        protected void render(Graphics g) {
            renderLinesAndValues(g);
            int maxSize = 0;
            for (int sid : activeLocations)
                for (int size : method.getSizes(sid))
                    maxSize = Math.max(maxSize, size);
            ((Graphics2D)g).setStroke(new BasicStroke(3));
            for (int sid : activeLocations) {
                g.setColor(locationColors.get(sid));
                final Vector<Integer> sizes = method.getSizes(sid);
                for (int i = 0; i != sizes.size(); ++i) {
                    final int y = samplesStride * (i+1);
                    final int x = sampleLineX(sizes.get(i) / (float)maxSize);
                    g.drawLine(x, y-sampleSize/2, x, y+sampleSize/2);
                }
            }
        }
    }

    private class FrequenciesPainter extends Painter {
        FrequenciesPainter(final Font font) {
            super(font);
        }

        @Override
        protected void render(Graphics g) {
            renderLinesAndValues(g);
            ((Graphics2D)g).setStroke(new BasicStroke(sampleSize));
            final Vector<Vector<Float>> frequencies = method.getFrequencies();
            for (int i = 0; i != frequencies.size(); ++i) {
                final int y = samplesStride * (i+1);
                final Vector<Float> f = frequencies.get(i);
                float accumulator = 0.0f;
                for (int j = 0; j != f.size(); ++j) {
                    g.setColor(locationColors.get(method.getSignedLocations().get(j)));
                    g.drawLine(sampleLineX(accumulator), y, sampleLineX(accumulator + f.get(j)), y);
                    accumulator += f.get(j);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private SourceMapping sourceMapping;

    private MonteCarlo method;
    private Vector<Integer> activeLocations;
    private HashMap<Integer, Color> locationColors;

    private Label targetLabel;
    private JList<Integer> locations;
    private JTabbedPane stages;
    private SamplesPainter samplesPainter;
    private SizesPainter sizesPainter;
    private FrequenciesPainter frequenciesPainter;
}
