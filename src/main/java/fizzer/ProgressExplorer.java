package fizzer;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.nio.file.*;
import java.util.*;
import fizzer.SourceMapping.LineColumn;

public class ProgressExplorer implements MouseListener, ActionListener, ListSelectionListener, ChangeListener {
    final Vector<String> options;

    private SourceMapping sourceMapping;
    private ExecutionTree executionTree;

    private JPanel rootPanel;

    private JTable analysesTable;
    private JTextArea analysesInfo;
    private JSplitPane analysesSplitPane;

    private AnalysisPlainInputsViewer analysisStartupViewer;
    private AnalysisPlainInputsViewer analysisBitshareViewer;
    private AnalysisPlainInputsViewer analysisLocalSearchViewer;
    private AnalysisPlainInputsViewer analysisBitflipViewer;
    private AnalysisPlainInputsViewer analysisTaintRequestViewer;
    private AnalysisPlainInputsViewer analysisTaintResponseViewer;
    private JPanel analysisPanel;

    private JSlider zoomSlider;
    private ExecutionTreeViewer executionTreeViewer;
    private MonteCarloViewer monteCarloViewer;
    private NavigatorViewer navigatorViewer;

    private SourceViewerC sourceC;
    private SourceViewerLL sourceLL;

    private JMenuItem menuFileOpen;
    private JMenuItem menuFileExit;

    private JMenuItem menuSummaryDlg;
    private JMenuItem menuViewAnalysisNode;
    private JMenuItem menuViewAnalysisTab;
    private JMenuItem menuViewTreeTab;
    private JMenuItem menuViewCTab;
    private JMenuItem menuViewLLTab;
    private JMenuItem menuViewMonteCarloTab;
    private JMenuItem menuViewNavigatorTab;
    private JMenuItem menuViewTreeId;
    private JMenuItem menuViewTreeC;
    private JMenuItem menuViewTreeLL;
    private JMenuItem menuViewSensitiveBits;
    private JMenuItem menuViewInputBytes;
    private JMenuItem menuViewBestValue;
    private JMenuItem menuViewTraceIndex;
    private JMenuItem menuViewNodeGuid;

    private JMenuItem menuHelpDocumentation;
    private JMenuItem menuHelpLicense;
    private JMenuItem menuHelpAbout;

    private String openFolderStartDir;

    private JScrollPane listScrollPane;
    private JScrollPane infoScrollPane;
    private JScrollPane treeScrollPane;
    private JScrollPane monteCarloScrollPane;
    private JScrollPane navigatorScrollPane;
    private JSplitPane splitPane;
    private JTabbedPane tabbedPane;
    private JPanel treePanel;

    public static final int listScrollSpeed = 20;
    public static final int infoScrollSpeed = 20;
    public static final int treeScrollSpeed = ExecutionTreeViewer.nodeHeight;
    public static final int textScrollSpeed = 20;
    public static final int zoomScrollMultiplier = 10;
    public static final int textFontSize = 14;

    public ProgressExplorer(final Vector<String> options_) {
        options = options_;

        sourceMapping = new SourceMapping();
        executionTree = new ExecutionTree();

        analysesTable = new JTable(new DefaultTableModel(null, new Object[]{"Index", "Type", "Start", "Stop", "Traces", "Strategy"}));
        analysesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        {
            final TableColumnModel columnModel = analysesTable.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(40);
            columnModel.getColumn(1).setPreferredWidth(100);
            columnModel.getColumn(2).setPreferredWidth(60);
            columnModel.getColumn(3).setPreferredWidth(70);
            columnModel.getColumn(4).setPreferredWidth(45);
            columnModel.getColumn(5).setPreferredWidth(140);
        }
        analysesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        analysesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        analysesTable.getSelectionModel().addListSelectionListener(this);
        analysesTable.setDefaultEditor(Object.class, null);
        analysesTable.setDefaultRenderer(Object.class, new AnalyzesTableRenderer(executionTree));

        analysesInfo = new JTextArea();
        analysesInfo.setEditable(false);

        analysisStartupViewer = new AnalysisPlainInputsViewer(Analysis.Type.STARTUP);
        analysisBitshareViewer = new AnalysisPlainInputsViewer(Analysis.Type.BITSHARE);
        analysisLocalSearchViewer = new AnalysisPlainInputsViewer(Analysis.Type.LOCAL_SEARCH);
        analysisBitflipViewer = new AnalysisPlainInputsViewer(Analysis.Type.BITFLIP);
        analysisTaintRequestViewer = new AnalysisPlainInputsViewer(Analysis.Type.TAINT_REQ);
        analysisTaintResponseViewer = new AnalysisPlainInputsViewer(Analysis.Type.TAINT_RES);
        analysisPanel = new JPanel(new CardLayout());
        analysisPanel.add(analysisStartupViewer, Analysis.Type.STARTUP.toString());
        analysisPanel.add(analysisBitshareViewer, Analysis.Type.BITSHARE.toString());
        analysisPanel.add(analysisLocalSearchViewer, Analysis.Type.LOCAL_SEARCH.toString());
        analysisPanel.add(analysisBitflipViewer, Analysis.Type.BITFLIP.toString());
        analysisPanel.add(analysisTaintRequestViewer, Analysis.Type.TAINT_REQ.toString());
        analysisPanel.add(analysisTaintResponseViewer, Analysis.Type.TAINT_RES.toString());

        zoomSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, 100);
        zoomSlider.addChangeListener(this);
        zoomSlider.setMajorTickSpacing(10);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomSlider.setValue(zoomSlider.getValue() + zoomScrollMultiplier * e.getWheelRotation());
            }
        });

        executionTreeViewer = new ExecutionTreeViewer(executionTree, sourceMapping);
        monteCarloViewer = options.contains("--showMonteCarloTab") ? new MonteCarloViewer(executionTreeViewer) : null;
        navigatorViewer = options.contains("--showNavigatorTab") ? new NavigatorViewer(executionTreeViewer) : null;

        sourceC = new SourceViewerC(sourceMapping, executionTree);
        sourceLL = new SourceViewerLL(sourceMapping, executionTree);

        menuFileOpen = new JMenuItem("Open directory");
        menuFileOpen.setMnemonic(KeyEvent.VK_O);
        menuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        menuFileOpen.addActionListener(this);
        menuFileExit = new JMenuItem("Exit");
        menuFileExit.setMnemonic(KeyEvent.VK_X);
        menuFileExit.addActionListener(this);


        menuSummaryDlg = new JMenuItem("Summary dialog");
        menuSummaryDlg.setMnemonic(KeyEvent.VK_0);
        menuSummaryDlg.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.ALT_DOWN_MASK));
        menuSummaryDlg.addActionListener(this);

        menuViewAnalysisNode = new JMenuItem("Analisis node");
        menuViewAnalysisNode.setMnemonic(KeyEvent.VK_9);
        menuViewAnalysisNode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, KeyEvent.ALT_DOWN_MASK));
        menuViewAnalysisNode.addActionListener(this);

        menuViewAnalysisTab = new JMenuItem("Analysis tab");
        menuViewAnalysisTab.setMnemonic(KeyEvent.VK_1);
        menuViewAnalysisTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeTab = new JMenuItem("Tree tab");
        menuViewTreeTab.setMnemonic(KeyEvent.VK_2);
        menuViewTreeTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK));
        menuViewCTab = new JMenuItem("C tab");
        menuViewCTab.setMnemonic(KeyEvent.VK_3);
        menuViewCTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK));
        menuViewLLTab = new JMenuItem("LL tab");
        menuViewLLTab.setMnemonic(KeyEvent.VK_4);
        menuViewLLTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.ALT_DOWN_MASK));
        if (monteCarloViewer != null) {
            menuViewMonteCarloTab = new JMenuItem("MonteCarlo tab");
            menuViewMonteCarloTab.setMnemonic(KeyEvent.VK_5);
            menuViewMonteCarloTab.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, KeyEvent.ALT_DOWN_MASK));
        }
        if (navigatorViewer != null) {
            final int key = monteCarloViewer == null ? KeyEvent.VK_5 : KeyEvent.VK_6;
            menuViewNavigatorTab = new JMenuItem("Navigator tab");
            menuViewNavigatorTab.setMnemonic(key);
            menuViewNavigatorTab.setAccelerator(KeyStroke.getKeyStroke(key, KeyEvent.ALT_DOWN_MASK));
        }

        menuViewTreeId = new JMenuItem("Tree node id");
        menuViewTreeId.setMnemonic(KeyEvent.VK_I);
        menuViewTreeId.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeId.addActionListener(this);
        menuViewTreeC = new JMenuItem("Tree node C line:column");
        menuViewTreeC.setMnemonic(KeyEvent.VK_C);
        menuViewTreeC.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeC.addActionListener(this);
        menuViewTreeLL = new JMenuItem("Tree node LL line");
        menuViewTreeLL.setMnemonic(KeyEvent.VK_L);
        menuViewTreeLL.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeLL.addActionListener(this);

        menuViewSensitiveBits = new JMenuItem("Tree node sensitive bits");
        menuViewSensitiveBits.setMnemonic(KeyEvent.VK_S);
        menuViewSensitiveBits.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK));
        menuViewSensitiveBits.addActionListener(this);
        menuViewInputBytes = new JMenuItem("Tree node input bytes");
        menuViewInputBytes.setMnemonic(KeyEvent.VK_B);
        menuViewInputBytes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_DOWN_MASK));
        menuViewInputBytes.addActionListener(this);
        menuViewBestValue = new JMenuItem("Tree node best value");
        menuViewBestValue.setMnemonic(KeyEvent.VK_V);
        menuViewBestValue.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_DOWN_MASK));
        menuViewBestValue.addActionListener(this);
        menuViewTraceIndex = new JMenuItem("Tree node trace index");
        menuViewTraceIndex.setMnemonic(KeyEvent.VK_T);
        menuViewTraceIndex.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_DOWN_MASK));
        menuViewTraceIndex.addActionListener(this);
        menuViewNodeGuid = new JMenuItem("Tree node guid");
        menuViewNodeGuid.setMnemonic(KeyEvent.VK_G);
        menuViewNodeGuid.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.ALT_DOWN_MASK));
        menuViewNodeGuid.addActionListener(this);

        
        menuHelpDocumentation = new JMenuItem("Documentation");
        menuHelpDocumentation.addActionListener(this);
        menuHelpLicense = new JMenuItem("License");
        menuHelpLicense.addActionListener(this);
        menuHelpAbout = new JMenuItem("About");
        menuHelpAbout.addActionListener(this);


        rootPanel = new JPanel(new BorderLayout());

        openFolderStartDir = Paths.get("").toAbsolutePath().toString();

        listScrollPane = new JScrollPane(analysesTable);
        listScrollPane.getHorizontalScrollBar().setUnitIncrement(listScrollSpeed);
        listScrollPane.getVerticalScrollBar().setUnitIncrement(listScrollSpeed);

        infoScrollPane = new JScrollPane(analysesInfo);
        infoScrollPane.getHorizontalScrollBar().setUnitIncrement(infoScrollSpeed);
        infoScrollPane.getVerticalScrollBar().setUnitIncrement(infoScrollSpeed);

        analysesSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScrollPane, infoScrollPane);
        analysesSplitPane.setResizeWeight(0.9);

        treeScrollPane = new JScrollPane(executionTreeViewer);
        treeScrollPane.getHorizontalScrollBar().setUnitIncrement(treeScrollSpeed);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(treeScrollSpeed);
        treeScrollPane.setWheelScrollingEnabled(false);
        treeScrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomSlider.setValue(zoomSlider.getValue() + zoomScrollMultiplier * e.getWheelRotation());
            }
        });
        treePanel = new JPanel(new BorderLayout());
        treePanel.add(zoomSlider, BorderLayout.NORTH);
        treePanel.add(treeScrollPane, BorderLayout.CENTER);

        if (monteCarloViewer != null) {
            monteCarloScrollPane = new JScrollPane(monteCarloViewer);
            monteCarloScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
            monteCarloScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        }
        if (navigatorViewer != null) {
            navigatorScrollPane = new JScrollPane(navigatorViewer);
            navigatorScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
            navigatorScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        }

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Analysis", analysisPanel);
        tabbedPane.addTab("Tree", treePanel);
        tabbedPane.addTab("C", sourceC);
        tabbedPane.addTab("LL", sourceLL);
        if (monteCarloViewer != null)
            tabbedPane.addTab("MonteCarlo", monteCarloViewer);
        if (navigatorViewer != null)
            tabbedPane.addTab("Navigator", navigatorViewer);
        menuViewAnalysisTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(0);
            }
        });
        menuViewTreeTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(1);
            }
        });
        menuViewCTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(2);
            }
        });
        menuViewLLTab.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(3);
            }
        });
        if (monteCarloViewer != null)
            menuViewMonteCarloTab.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tabbedPane.setSelectedIndex(4);
                }
            });
        if (navigatorViewer != null)
            menuViewNavigatorTab.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tabbedPane.setSelectedIndex(monteCarloViewer == null ? 4 : 5);
                }
            });
        tabbedPane.setSelectedIndex(1);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, analysesSplitPane, tabbedPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(580);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(splitPane, BorderLayout.CENTER);
        rootPanel.setOpaque(true);

        executionTreeViewer.addMouseListener(this);
        sourceC.getSourceViewer().addMouseListener(this);
        sourceLL.getSourceViewer().addMouseListener(this);
        if (monteCarloViewer != null)
            monteCarloViewer.addMouseListener(this);
        if (navigatorViewer != null)
            navigatorViewer.addMouseListener(this);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    @Override
    public void mouseClicked(MouseEvent e){
        switch (tabbedPane.getSelectedIndex()) {
            case 0: { // Analyses
                break;
            }
            case 1: { // Tree
                Node node = executionTreeViewer.getNodeBasedOnMousePosition(e.getX(), e.getY());
                if (node != null) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.isControlDown()) {
                            final int sid = executionTree.getUncoveredSignedLocationId(node.getLocationId());
                            if (sid != 0) {
                                if (monteCarloViewer != null)
                                    monteCarloViewer.onTargetChanged(sid);
                                if (navigatorViewer != null)
                                    navigatorViewer.onTargetChanged(sid);
                                tabbedPane.setSelectedIndex(4);
                            }
                        } else
                            navigateFromTree(node);
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showWindowWithNodeInformation(node);
                    }
                }
                break;
            }
            case 2: { // C
                int id = sourceC.getSourceViewer().getIdOfCurrentLine(e);
                if (id != -1) {
                    if (e.isControlDown()) {
                        final int sid = executionTree.getUncoveredSignedLocationId(id);
                        if (sid != 0) {
                            if (monteCarloViewer != null)
                                monteCarloViewer.onTargetChanged(sid);
                            if (navigatorViewer != null)
                                navigatorViewer.onTargetChanged(sid);
                            tabbedPane.setSelectedIndex(4);
                        }
                    } else {
                        tabbedPane.setSelectedIndex(3);
                        sourceLL.getSourceViewer().setLine(sourceMapping.getCondMapLL(id));
                    }
                }
                break;
            }
            case 3: { // LL
                int id = sourceLL.getSourceViewer().getIdOfCurrentLine(e);
                if (id != -1) {
                    tabbedPane.setSelectedIndex(2);
                    sourceC.getSourceViewer().setLine(sourceMapping.getCondMapC(id).line);
                }
                break;
            }
            case 4:
            case 5: { // MonteCarlo, Navigator
                break;
            }
        }
    }

    private void navigateFromTree(Node node) {
        if (executionTreeViewer.getLocationViewType() == ExecutionTreeViewer.LocationViewType.C) {
            LineColumn lineColumn = sourceMapping.getCLineAndColumnWithId((Integer)node.getLocationId().id);
            tabbedPane.setSelectedIndex(2);
            sourceC.getSourceViewer().setLine(lineColumn.line);
        }
        if (executionTreeViewer.getLocationViewType() == ExecutionTreeViewer.LocationViewType.LL) {
            Integer line = sourceMapping.getLlvmLineWithId((Integer)node.getLocationId().id);
            tabbedPane.setSelectedIndex(3);
            sourceLL.getSourceViewer().setLine(line);
        }
    }

    private void showWindowWithNodeInformation(Node node) {
        LineColumn lineColumn = sourceMapping.getCLineAndColumnWithId((Integer)node.getLocationId().id);
        Integer llLine = sourceMapping.getLlvmLineWithId(node.getLocationId().id);
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.append("GUID: " + Long.toUnsignedString(node.guid) + "\n");
        textArea.append("Location ID: " + Integer.toUnsignedString(node.getLocationId().id) + "\n");
        textArea.append("C line and column: " + lineColumn.line + ", " + lineColumn.column + "\n");
        textArea.append("LL line: " + llLine + "\n");
        textArea.append("Trace index: " + Integer.toUnsignedString(node.getTraceIndex()) + "\n");
        textArea.append("Best value: " + Double.toString(node.getBestValue(executionTree.getAnalysisIndex())) + "\n");
        textArea.append("Input bytes: " + Integer.toUnsignedString(node.getNumInputBytes()) + "\n");
        textArea.append("Sensitive Bits count: " + Integer.toUnsignedString(node.getSensitiveBits(executionTree.getAnalysisIndex()).size()) + "\n");
        textArea.append("Sensitive Bits: " + node.getSensitiveBits(executionTree.getAnalysisIndex()) + "\n");
        textArea.append("Sensitivity applied: " + Boolean.toString(node.sensitivityApplied(executionTree.getAnalysisIndex())) + "\n");
        textArea.append("Bitshare applied: " + Boolean.toString(node.bitshareApplied(executionTree.getAnalysisIndex())) + "\n");
        textArea.append("Local search applied: " + Boolean.toString(node.localSearchApplied(executionTree.getAnalysisIndex())) + "\n");
        textArea.append("Bitflip applied: " + Boolean.toString(node.bitflipApplied(executionTree.getAnalysisIndex())));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.setPreferredSize(new Dimension(250,250));
        JOptionPane.showMessageDialog(rootPanel, scrollPane, "Node Information", JOptionPane.PLAIN_MESSAGE);
    }

    private void showSummary() {
        StringBuilder information = new StringBuilder();

        SourceViewerC.CoverageInfo coverageInfo = sourceC.computeCoverageInfo(executionTree.getAnalyses().length - 1);
        information.append(
            "Coverage: " + String.format(Locale.US, "%.2f", 100 * coverageInfo.coverage) + '%' +
            " (both: " + Integer.toString(coverageInfo.numBothCovered) +
            ", left: " + Integer.toString(coverageInfo.numLeftCovered) +
            ", right: " + Integer.toString(coverageInfo.numRightCovered) +
            ", none: " + Integer.toString(coverageInfo.numNoneCovered) +
            ", all: " + Integer.toString(coverageInfo.numAllLocations) + ')'
            );
        information.append(System.lineSeparator());

        information.append("Analyses:\n");
        final Analysis.Type[] types = {
            Analysis.Type.STARTUP,
            Analysis.Type.BITSHARE,
            Analysis.Type.LOCAL_SEARCH,
            Analysis.Type.BITFLIP,
            Analysis.Type.TAINT_REQ,
            Analysis.Type.TAINT_RES,
        };
        class AnalysisSummary {
            int numCalls = 0;
            int numTraces = 0;
            int numCovered = 0;
            float coveredPercentage = 0.0f;
            float effectivity = 0.0f;
        }
        HashMap<Analysis.Type,AnalysisSummary> summaries = new HashMap<>();
        for (Analysis.Type type : types)
            summaries.put(type, new AnalysisSummary());
        for (int i = 0; i < executionTree.getAnalyses().length; ++i) {
            Analysis analysis = executionTree.getAnalyses()[i];
            AnalysisSummary summary = summaries.get(analysis.getType());
            if (analysis.getStartAttribute() == Analysis.StartAttribute.REGULAR)
                ++summary.numCalls;
            summary.numTraces += analysis.getNumTraces();
            summary.numCovered += analysis.getCoveredLocationIds().size();
        }
        if (coverageInfo.numBothCovered > 0)
            for (AnalysisSummary summary : summaries.values()) {
                summary.coveredPercentage = (float)summary.numCovered / (float)coverageInfo.numBothCovered;
                if (summary.numTraces > 0)
                    summary.effectivity = (float)summary.numCovered / (float)summary.numTraces;
            }
        for (Analysis.Type type : types) {
            AnalysisSummary summary = summaries.get(type);
            information.append("    " + type.toString() + ": calls: " + Integer.toString(summary.numCalls));
            if (type != Analysis.Type.TAINT_REQ && type != Analysis.Type.TAINT_RES) {
                information.append(
                    ", covered: " + Integer.toString(summary.numCovered) +
                        " (" + String.format(Locale.US, "%.2f", 100 * summary.coveredPercentage) + '%' + ')' +
                    ", traces: " + Integer.toString(summary.numTraces) +
                    ", effectivity: " + String.format(Locale.US, "%.2f", summary.effectivity)
                    );
            }
            information.append(System.lineSeparator());
        }

        information.append("Strategies:\n");
        HashMap<String,Integer> strategyCounters = new HashMap<>();
        for (int i = 0; i < executionTree.getAnalyses().length; ++i)
            strategyCounters.compute(executionTree.getStrategyAnalyses()[i].getStrategy(), (k, v) -> { return v == null ? 1 : v + 1; });
        for (String strategy : strategyCounters.keySet().stream().sorted().toList())
            if (!strategy.isEmpty()) {
                information.append("    " + strategy.toString() + ": " + Integer.toString(strategyCounters.get(strategy)));
                information.append(System.lineSeparator());
            }

        information.append("Nodes:\n");
        class NodesInfoCollector {
            int numIDs = 0;
            int numIIDs = 0;
            int numUnknown = 0;
            int numEndsExceptional = 0;
            int numEndsNormal = 0;

            void updateInfoForSubtree(Node node) {
                if (node == null)
                    return;
                if (node.sensitivityApplied(executionTree.getAnalyses().length)) {
                    int numBits = node.getSensitiveBits(executionTree.getAnalyses().length).size();
                    if (numBits == 0)
                        ++numIIDs;
                    else
                        ++numIDs;
                } else
                    ++numUnknown;
                for (int i = 0; i != 2; ++i)
                    switch (node.getChildLabel(executionTree.getAnalyses().length, i)) {
                        case END_EXCEPTIONAL: ++numEndsExceptional; break;
                        case END_NORMAL: ++numEndsNormal; break;
                        default: break;
                    }
                for (Node child : node.getChildren())
                    updateInfoForSubtree(child);
            }
        }
        NodesInfoCollector nodesInfoCollector = new NodesInfoCollector();
        nodesInfoCollector.updateInfoForSubtree(executionTree.getRootNode());
        information.append(
            "    All: " + Integer.toString(nodesInfoCollector.numIDs + nodesInfoCollector.numIIDs + nodesInfoCollector.numUnknown) + "\n" +
            "    ID: " + Integer.toString(nodesInfoCollector.numIDs) + "\n" +
            "    IID: " + Integer.toString(nodesInfoCollector.numIIDs) + "\n" +
            "    Others: " + Integer.toString(nodesInfoCollector.numUnknown) + "\n" +
            "    Ends exceptional: " + Integer.toString(nodesInfoCollector.numEndsExceptional) + "\n" +
            "    Ends normal: " + Integer.toString(nodesInfoCollector.numEndsNormal)
            );
        JTextArea ta = new JTextArea(information.toString());
        ta.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(ta);
        scrollPane.setPreferredSize(new Dimension(450,420));
        JOptionPane.showMessageDialog(null, scrollPane, "Summary", JOptionPane.PLAIN_MESSAGE);

    }

    private void updateStrategyAnalysisInfo(int analysisIndex) {
        StrategyAnalysis strategyAnalysis = executionTree.getStrategyAnalyses()[analysisIndex];
        Analysis analysis = executionTree.getAnalyses()[analysisIndex];
        analysesInfo.setText(""); 
        analysesInfo.append("Covered: " + analysis.getCoveredLocationIds() + "\n");
        analysesInfo.append("Closed Guids: " + strategyAnalysis.getClosedNodeGuids() + "\n");
        analysesInfo.append("Coverage Failure Resets: " + analysis.getNumCoverageFailureResets());
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() != analysesTable.getSelectionModel())
            return;
        if (analysesTable.getSelectionModel().getValueIsAdjusting())
            return;
        if (analysesTable.getSelectedRow() < 0)
            return;
        executionTree.setAnalysisIndex(analysesTable.getSelectedRow());
        activeAnalysisCard();
        analysisStartupViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitshareViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisLocalSearchViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitflipViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintRequestViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintResponseViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        executionTreeViewer.onAnalysisChanged();
        sourceC.onAnalysisChanged();
        sourceLL.onAnalysisChanged();

        if (monteCarloViewer != null || navigatorViewer != null) {
            final StrategyAnalysis strategyAnalysis = executionTree.getStrategyAnalyses()[executionTree.getAnalysisIndex()];
            final int sid = executionTree.getUncoveredSignedLocationId(strategyAnalysis.getStrategyLocationID());
            if (sid != 0) {
                if (monteCarloViewer != null) {
                    monteCarloViewer.clear();
                    monteCarloViewer.onTargetChanged(sid);
                }
                if (navigatorViewer != null) {
                    navigatorViewer.clear();
                    navigatorViewer.onTargetChanged(sid);
                }
            }
        }

        updateStrategyAnalysisInfo(executionTree.getAnalysisIndex());
    }

    public void activeAnalysisCard(int analysisIndex) {
        if (executionTree.getAnalyses() != null)
            ((CardLayout)(analysisPanel.getLayout())).show(analysisPanel, executionTree.getAnalyses()[analysisIndex].getType().toString());
    }

    public void activeAnalysisCard() {
        activeAnalysisCard(executionTree.getAnalysisIndex());
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() != zoomSlider)
            return;
        if (zoomSlider.getValueIsAdjusting())
            return;
        executionTreeViewer.onZoomChanged((int)zoomSlider.getValue());
    }

    public static final class Resource {
        public static void show(String resource, String title, int icon, int width, int height) {
            String text = null;
            try (Scanner scanner = new Scanner(ProgressExplorer.class.getResourceAsStream(resource), "UTF-8")) {
                text = scanner.useDelimiter("\\A").next();
            }
            JTextPane textPane = new JTextPane(); 
            textPane.setContentType("text/html");
            textPane.setFont(new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize));
            textPane.setText(text);
            JScrollPane scrollPane = new JScrollPane(textPane);
            if (width > 0 && height > 0)
                scrollPane.setPreferredSize(new Dimension(width, height));
            JOptionPane.showMessageDialog(null, scrollPane, title, icon);            
        }
        public static void show(String resource, String title) {
            show(resource, title, JOptionPane.INFORMATION_MESSAGE, -1, -1);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuFileOpen) {
            JFileChooser fileChooser = new JFileChooser(openFolderStartDir);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION)
                load(fileChooser.getSelectedFile().getAbsolutePath());
        } else if (e.getSource() == menuFileExit) {
            System.exit(0);
        } else if (e.getSource() == menuSummaryDlg) {
            showSummary();
        } else if (e.getSource() == menuViewAnalysisNode) {
            executionTreeViewer.makeAnalysisNodeVisible();
        } else if (e.getSource() == menuViewTreeId) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.ID);
        } else if (e.getSource() == menuViewTreeC) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.C);
        } else if (e.getSource() == menuViewTreeLL) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.LL);
        } else if (e.getSource() == menuViewSensitiveBits) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.SENSITIVE_BITS);
        } else if (e.getSource() == menuViewInputBytes) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.INPUT_BYTES);
        } else if (e.getSource() == menuViewBestValue) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.VALUE);
        } else if (e.getSource() == menuViewTraceIndex) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.TRACE_INDEX);
        } else if (e.getSource() == menuViewNodeGuid) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.NODE_GUID);
        } else if (e.getSource() == menuHelpDocumentation) {
            Resource.show("/documentation.html", "Documentation", JOptionPane.PLAIN_MESSAGE, 800, 600);
        } else if (e.getSource() == menuHelpLicense) {
            Resource.show("/license.html", "License");
        } else if (e.getSource() == menuHelpAbout) {
            Resource.show("/about.html", "About");
        }
    }

    public void load(String dir) {
        ((JFrame)SwingUtilities.getWindowAncestor(rootPanel)).setTitle("Fizzer's ProgressExplorer [Loading...]");
        SwingUtilities.getWindowAncestor(rootPanel).setEnabled(false);

        openFolderStartDir = Paths.get(dir).normalize().getParent().toString();

        try {
            clear();
            sourceMapping.load(dir);
            executionTree.load(dir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPanel, "Load has FAILED: " + e.toString());
            clear();
            ((JFrame)SwingUtilities.getWindowAncestor(rootPanel)).setTitle("Fizzer's ProgressExplorer [Load has FAILED]");
            SwingUtilities.getWindowAncestor(rootPanel).setEnabled(true);
            return;
        }

        for (int i = 0; i < executionTree.getAnalyses().length; ++i) {
            StrategyAnalysis strategyAnalysis = executionTree.getStrategyAnalyses()[i];
            Analysis analysis = executionTree.getAnalyses()[i];
            ((DefaultTableModel)analysesTable.getModel()).addRow(new Object[]{
                analysis.getIndex() + 1,
                analysis.getType(),
                analysis.getStartAttribute().toString().toLowerCase(),
                analysis.getStopAttribute().toString().toLowerCase(),
                analysis.getNumTraces(),
                strategyAnalysis.getStrategy()
            });
        }
        analysesTable.scrollRectToVisible(analysesTable.getCellRect(executionTree.getAnalysisIndex(), 0, true));
        analysesTable.setRowSelectionInterval(executionTree.getAnalysisIndex(), executionTree.getAnalysisIndex());
        analysesTable.setColumnSelectionInterval(1, 1);

        sourceC.load();
        sourceLL.load();

        // not working as expected
        JScrollBar verticalBar = listScrollPane.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());

        analysisStartupViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitshareViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisLocalSearchViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitflipViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintRequestViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintResponseViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        executionTreeViewer.onLoad();
        sourceC.onAnalysisChanged();
        sourceLL.onAnalysisChanged();
        if (monteCarloViewer != null)
            monteCarloViewer.clear();
        if (navigatorViewer != null)
            navigatorViewer.clear();
        updateStrategyAnalysisInfo(executionTree.getAnalysisIndex());

        String rawName = Paths.get("").toAbsolutePath().relativize(Paths.get(dir)).toString();
        ((JFrame)SwingUtilities.getWindowAncestor(rootPanel)).setTitle("Fizzer's ProgressExplorer [" + (rawName.isEmpty() ? "." : rawName) + "]");
        SwingUtilities.getWindowAncestor(rootPanel).setEnabled(true);
    }

    public void clear() {
        sourceMapping.clear();
        executionTree.clear();
        DefaultTableModel dm = (DefaultTableModel)analysesTable.getModel();
        for (int i = dm.getRowCount() - 1; i >= 0; i--)
            dm.removeRow(i);
        analysisStartupViewer.clear();
        analysisBitshareViewer.clear();
        analysisLocalSearchViewer.clear();
        analysisBitflipViewer.clear();
        analysisTaintRequestViewer.clear();
        analysisTaintResponseViewer.clear();
        executionTreeViewer.clear();
        sourceC.clear();
        sourceLL.clear();
        if (monteCarloViewer != null)
            monteCarloViewer.clear();
        if (navigatorViewer != null)
            navigatorViewer.clear();
    }

    public static void main( String[] args ) {
        final Vector<String> options = new Vector<>();
        final String loadPath = args.length > 0 && Files.isDirectory(Paths.get(args[0]))? args[0] : null;
        for (int i = loadPath == null ? 0 : 1; i < args.length; ++i)
            if (args[i].startsWith("--"))
                options.add(args[i]);

        // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        JFrame frame = new JFrame("Fizzer's ProgressExplorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1024, 768));

        ProgressExplorer explorer = new ProgressExplorer(options);

        explorer.activeAnalysisCard();

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic(KeyEvent.VK_F);
        menuFile.add(explorer.menuFileOpen);
        menuFile.add(explorer.menuFileExit);

        JMenu menuView = new JMenu("View");
        menuView.setMnemonic(KeyEvent.VK_W);
        menuView.add(explorer.menuSummaryDlg);
        menuView.addSeparator();
        menuView.add(explorer.menuViewAnalysisNode);
        menuView.addSeparator();
        menuView.add(explorer.menuViewAnalysisTab);
        menuView.add(explorer.menuViewTreeTab);
        menuView.add(explorer.menuViewCTab);
        menuView.add(explorer.menuViewLLTab);
        if (explorer.monteCarloViewer != null)
            menuView.add(explorer.menuViewMonteCarloTab);
        if (explorer.navigatorViewer != null)
            menuView.add(explorer.menuViewNavigatorTab);
        menuView.addSeparator();
        menuView.add(explorer.menuViewTreeId);
        menuView.add(explorer.menuViewTreeC);
        menuView.add(explorer.menuViewTreeLL);
        menuView.add(explorer.menuViewSensitiveBits);
        menuView.add(explorer.menuViewInputBytes);
        menuView.add(explorer.menuViewBestValue);
        menuView.add(explorer.menuViewTraceIndex);
        menuView.add(explorer.menuViewNodeGuid);

        JMenu menuHelp = new JMenu("Help");
        menuHelp.setMnemonic(KeyEvent.VK_H);
        menuHelp.add(explorer.menuHelpDocumentation);
        menuHelp.add(explorer.menuHelpLicense);
        menuHelp.add(explorer.menuHelpAbout);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuFile);
        menuBar.add(menuView);
        menuBar.add(menuHelp);
        frame.setJMenuBar(menuBar);
        
        frame.setContentPane(explorer.rootPanel);

        try {
            URL url = ProgressExplorer.class.getResource("/icon.png");
            frame.setIconImage(Toolkit.getDefaultToolkit().getImage(url)); 
        } catch (Exception e) {} 

        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.pack();
        frame.setVisible(true);

        try { Thread.sleep(250); } catch (InterruptedException e) {}
        if (loadPath != null)
            explorer.load(Paths.get(loadPath).toAbsolutePath().toString());
        try { Thread.sleep(250); } catch (InterruptedException e) {}
    }
}
