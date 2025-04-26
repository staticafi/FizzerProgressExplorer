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
        this.stage = Stage.SAMPLES;
        this.activeLocations = new Vector<>();
        this.locationColors = new HashMap<>();

        Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        stageSelector = new JComboBox<>(new String[] { Stage.SAMPLES.toString() });
        stageSelector.setFont(font);
        stageSelector.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent ae) {
                stage = Stage.valueOf((String)((JComboBox<String>)ae.getSource()).getSelectedItem());
                redraw();
            }
        });

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

        paper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                render(g);
            }
        };
        paper.setFont(font);
        paper.setBackground(Color.white);
        paper.setAutoscrolls(true);
        paper.setOpaque(true);
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
                    Rectangle rect = new Rectangle(paper.getVisibleRect());
                    rect.x += origin.x - e.getX();
                    rect.y += origin.y - e.getY();
                    paper.scrollRectToVisible(rect);
                }
            }
        };
        paper.addMouseListener(ma);
        paper.addMouseMotionListener(ma);

        JScrollPane locationsScrollPane = new JScrollPane(locations);
        locationsScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        locationsScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JScrollPane paperScrollPane = new JScrollPane(paper);
        paperScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        paperScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(true);
        leftPanel.add(stageSelector, BorderLayout.NORTH);
        leftPanel.add(locationsScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, paperScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(100);

        setLayout(new BorderLayout());
        add(splitPane);
    }

    public void clear() {
        method.clear();

        stage = Stage.SAMPLES;
        activeLocations.clear();
        locationColors.clear();

        stageSelector.setSelectedItem(stage.toString());
        ((DefaultListModel<Integer>)locations.getModel()).clear();
        redraw();
    }

    public void onAnalysisChanged() {
        clear();
    }

    public void onTargetChanged(LocationId targetId) {
        clear();
        method.compute(targetId);
        computeLocationColors();
        int maxSamples = 0;
        for (int sid : method.getSignedLocations().stream().sorted().toList()) {
            ((DefaultListModel<Integer>)(locations.getModel())).addElement(sid);
            maxSamples = Math.max(maxSamples, method.getSamples(sid).size());
        }
        locations.setSelectedIndex(0);
        paper.setPreferredSize(new Dimension(paperWidth, (maxSamples + 1) * samplesStride));
        redraw();
    }

    public void redraw() {
        paper.revalidate();
        paper.repaint();
    }

    private void computeLocationColors() {
        java.util.List<Integer> allLocations = method.getSignedLocations().stream().sorted().toList();
        locationColors.clear();
        for (int i = 0; i < allLocations.size(); ++i)
            locationColors.put(allLocations.get(i), Color.getHSBColor(i/(float)(allLocations.size() + 1), 1.0f, 0.8f));
    }

    private void render(Graphics g) {
        if (method.isEmpty())
            return;
        switch (stage) {
            case SAMPLES: paintSamples(g); break;
        }
    }

    private void paintSamples(Graphics g) {
        int start = sampleMarginLeft;
        int end = paperWidth - sampleMarginRight;
        g.setColor(Color.LIGHT_GRAY);
        for (int sid : activeLocations) {
            Vector<Vector<Float>> samples = method.getSamples(sid);
            for (int i = 0; i != samples.size(); ++i) {
                int y = samplesStride * (i+1);
                g.setColor(Color.LIGHT_GRAY);
                g.drawLine(start, y, end, y);
                g.setColor(Color.BLACK);
                g.drawString(Double.toString(method.getSampleValue(i)), end + 10, y + 5);
            }
        }
        ((Graphics2D)g).setStroke(new BasicStroke(3));
        for (int sid : activeLocations) {
            g.setColor(locationColors.get(sid));
            Vector<Vector<Float>> samples = method.getSamples(sid);
            for (int i = 0; i != samples.size(); ++i) {
                int y = samplesStride * (i+1);
                for (float t : samples.get(i)) {
                    int x = Math.round(start + t * (end - start));
                    g.drawLine(x, y-sampleSize/2, x, y+sampleSize/2);
                }
            }
        }
    }

    private static enum Stage {
        SAMPLES
    }

    @SuppressWarnings("unused")
    private SourceMapping sourceMapping;

    private MonteCarlo method;
    private Stage stage;
    private Vector<Integer> activeLocations;
    private HashMap<Integer, Color> locationColors;

    private JComboBox<String> stageSelector;
    private JList<Integer> locations;
    private JPanel paper;

    private static final int sampleSize = 6;
    private static final int samplesStride = 20;
    private static final int sampleMarginLeft = 10;
    private static final int sampleMarginRight = 100;
    private static final int paperWidth = 1250;
}
