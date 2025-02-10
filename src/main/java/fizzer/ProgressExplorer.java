package fizzer;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import fizzer.SourceMapping.LineColumn;

import java.nio.file.Paths;

public class ProgressExplorer implements MouseListener, ActionListener, ListSelectionListener, ChangeListener {
    private SourceMapping sourceMapping;
    private ExecutionTree executionTree;

    private JPanel rootPanel;

    private JTable analysesTable;
    private JTextArea analysesInfo;
    private JSplitPane analysesSplitPane;

    private AnalysisPlainInputsViewer analysisNoneViewer;
    private AnalysisPlainInputsViewer analysisBitshareViewer;
    private AnalysisPlainInputsViewer analysisLocalSearchViewer;
    private AnalysisPlainInputsViewer analysisBitflipViewer;
    private AnalysisPlainInputsViewer analysisTaintRequestViewer;
    private AnalysisPlainInputsViewer analysisTaintResponseViewer;
    private JPanel analysisPanel;

    private JSlider zoomSlider;
    private ExecutionTreeViewer executionTreeViewer;

    private SourceViewerC sourceC;
    private SourceViewerLL sourceLL;

    private JMenuItem menuFileOpen;
    private JMenuItem menuFileExit;

    private JMenuItem menuViewAnalysisTab;
    private JMenuItem menuViewTreeTab;
    private JMenuItem menuViewCTab;
    private JMenuItem menuViewLLTab;
    private JMenuItem menuViewTreeId;
    private JMenuItem menuViewTreeIdCtx;
    private JMenuItem menuViewTreeC;
    private JMenuItem menuViewTreeLL;
    private JMenuItem menuViewSensitiveBits;
    private JMenuItem menuViewInputBytes;
    private JMenuItem menuViewBestValue;
    private JMenuItem menuViewTraceIndex;
    private JMenuItem menuViewNodeGuid;

    private String openFolderStartDir;

    private JScrollPane listScrollPane;
    private JScrollPane treeScrollPane;
    private JSplitPane splitPane;
    private JTabbedPane tabbedPane;
    private JPanel treePanel;

    public static final int listScrollSpeed = 20;
    public static final int treeScrollSpeed = ExecutionTreeViewer.nodeHeight;
    public static final int textScrollSpeed = 20;
    public static final int zoomScrollMultiplier = 10;
    public static final int textFontSize = 14;

    public ProgressExplorer() {
        sourceMapping = new SourceMapping();
        executionTree = new ExecutionTree();

        analysesTable = new JTable(new DefaultTableModel(null, new Object[]{"Index", "Type", "Traces", "Strategy"}));
        analysesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        analysesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        analysesTable.getSelectionModel().addListSelectionListener(this);
        analysesTable.setDefaultEditor(Object.class, null);

        analysesInfo = new JTextArea();
        analysesInfo.setEditable(false);

        analysisNoneViewer = new AnalysisPlainInputsViewer(Analysis.Type.NONE);
        analysisBitshareViewer = new AnalysisPlainInputsViewer(Analysis.Type.BITSHARE);
        analysisLocalSearchViewer = new AnalysisPlainInputsViewer(Analysis.Type.LOCAL_SEARCH);
        analysisBitflipViewer = new AnalysisPlainInputsViewer(Analysis.Type.BITFLIP);
        analysisTaintRequestViewer = new AnalysisPlainInputsViewer(Analysis.Type.TAINT_REQ);
        analysisTaintResponseViewer = new AnalysisPlainInputsViewer(Analysis.Type.TAINT_RES);
        analysisPanel = new JPanel(new CardLayout());
        analysisPanel.add(analysisNoneViewer, Analysis.Type.NONE.toString());
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

        sourceC = new SourceViewerC(sourceMapping, executionTree);
        sourceLL = new SourceViewerLL(sourceMapping, executionTree);

        menuFileOpen = new JMenuItem("Open directory");
        menuFileOpen.setMnemonic(KeyEvent.VK_O);
        menuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        menuFileOpen.addActionListener(this);
        menuFileExit = new JMenuItem("Exit");
        menuFileExit.setMnemonic(KeyEvent.VK_X);
        menuFileExit.addActionListener(this);

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

        menuViewTreeId = new JMenuItem("Tree node id");
        menuViewTreeId.setMnemonic(KeyEvent.VK_I);
        menuViewTreeId.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeId.addActionListener(this);
        menuViewTreeIdCtx = new JMenuItem("Tree node id:context");
        menuViewTreeIdCtx.setMnemonic(KeyEvent.VK_X);
        menuViewTreeIdCtx.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_DOWN_MASK));
        menuViewTreeIdCtx.addActionListener(this);
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
    
        rootPanel = new JPanel(new BorderLayout());

        openFolderStartDir = Paths.get("").toAbsolutePath().toString();

        listScrollPane = new JScrollPane(analysesTable);
        listScrollPane.getHorizontalScrollBar().setUnitIncrement(listScrollSpeed);
        listScrollPane.getVerticalScrollBar().setUnitIncrement(listScrollSpeed);

        analysesSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScrollPane, analysesInfo);
        analysesSplitPane.setResizeWeight(1.0);

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

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Analysis", analysisPanel);
        tabbedPane.addTab("Tree", treePanel);
        tabbedPane.addTab("C", sourceC);
        tabbedPane.addTab("LL", sourceLL);
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
        tabbedPane.setSelectedIndex(1);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, analysesSplitPane, tabbedPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(450);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(splitPane, BorderLayout.CENTER);
        rootPanel.setOpaque(true);

        executionTreeViewer.addMouseListener(this);
        sourceC.getSourceViewer().addMouseListener(this);
        sourceLL.getSourceViewer().addMouseListener(this);
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e){}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    @Override
    public void mouseClicked(MouseEvent e){
        switch (tabbedPane.getSelectedIndex()) {
            case 0: {
                break;
            }
            case 1: {
                Node node = executionTreeViewer.getNodeBasedOnMousePosition(e.getX(), e.getY());
                if (node != null) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        navigateFromTree(node);
                    }
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showWindowWithNodeInformation(node);
                    }
                }
                break;
            }
            case 2: { // C to LL
                int id = sourceC.getSourceViewer().getIdOfCurrentLine(e);
                if (id != -1) {
                    tabbedPane.setSelectedIndex(3);
                    sourceLL.getSourceViewer().setLine(sourceMapping.getCondMapLL(id));
                }
                break;
            }
            case 3: { // LL to C
                int id = sourceLL.getSourceViewer().getIdOfCurrentLine(e);
                if (id != -1) {
                    tabbedPane.setSelectedIndex(2);
                    sourceC.getSourceViewer().setLine(sourceMapping.getCondMapC(id).line);
                }
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
        StringBuilder information = new StringBuilder();
        information.append("GUID: " + Long.toUnsignedString(node.guid));
        information.append(System.lineSeparator());
        information.append("Location ID: " + Integer.toUnsignedString(node.getLocationId().id));
        information.append(System.lineSeparator());
        information.append("Context: " + Integer.toUnsignedString(node.getLocationId().context));
        information.append(System.lineSeparator());
        information.append(String.format("C line and column: %d, %d", lineColumn.line , lineColumn.column));
        information.append(System.lineSeparator());
        information.append(String.format("LL line: %d", llLine));
        information.append(System.lineSeparator());
        information.append("Trace Index: " + Integer.toUnsignedString(node.getTraceIndex()));
        information.append(System.lineSeparator());
        information.append("Best Value: " + Double.toString(node.getBestValue(executionTree.getAnalysisIndex())));
        information.append(System.lineSeparator());
        information.append("Input bytes: " + Integer.toUnsignedString(node.getNumInputBytes()));
        information.append(System.lineSeparator());
        information.append("Sensitive Bits: " + Integer.toUnsignedString(node.getSensitiveBits(executionTree.getAnalysisIndex()).size()));
        information.append(System.lineSeparator());
        information.append("Sensitivity applied: " + Boolean.toString(node.sensitivityApplied(executionTree.getAnalysisIndex())));
        information.append(System.lineSeparator());
        information.append("Bitshare applied: " + Boolean.toString(node.bitshareApplied(executionTree.getAnalysisIndex())));
        information.append(System.lineSeparator());
        information.append("Local search applied: " + Boolean.toString(node.localSearchApplied(executionTree.getAnalysisIndex())));
        information.append(System.lineSeparator());
        information.append("Bitflip applied: " + Boolean.toString(node.bitflipApplied(executionTree.getAnalysisIndex())));
        JOptionPane.showMessageDialog(null, information.toString(), "Node Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStrategyAnalysisInfo(int analysisIndex) {
        StrategyAnalysis strategyAnalysis = executionTree.getStrategyAnalyses()[analysisIndex];
        Analysis analysis = executionTree.getAnalyses()[analysisIndex];
        analysesInfo.setText(""); 
        analysesInfo.append("Analysis index: " + (analysisIndex + 1) + "\n");
        analysesInfo.append("Strategy: " + strategyAnalysis.getStrategy() + "\n");
        analysesInfo.append("Type: " + analysis.getType() + "\n");
        analysesInfo.append("Start Attribute: " + analysis.getStartAttribute() + "\n");
        analysesInfo.append("Stop Attribute: " + analysis.getStopAttribute() + "\n");
        analysesInfo.append("Number of Traces: " + analysis.getNumTraces() + "\n");
        analysesInfo.append("Coverage Failure Resets: " + analysis.getNumCoverageFailureResets() + "\n");
        analysesInfo.append("Closed Node Guids: " + strategyAnalysis.getClosedNodeGuids() + "\n");
    }

    public void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width +1 , width);
            }
            width = Math.max(width, table.getColumnModel().getColumn(column).getPreferredWidth());
            // if(width > 300)
            //     width=300;
            columnModel.getColumn(column).setPreferredWidth(width);
        }
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
        analysisNoneViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitshareViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisLocalSearchViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitflipViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintRequestViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintResponseViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        executionTreeViewer.onAnalysisChanged();
        sourceC.onAnalysisChanged();
        sourceLL.onAnalysisChanged();
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

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuFileOpen) {
            JFileChooser fileChooser = new JFileChooser(openFolderStartDir);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION)
                load(fileChooser.getSelectedFile().getAbsolutePath());
        } else if (e.getSource() == menuFileExit) {
            System.exit(0);
        } else if (e.getSource() == menuViewTreeId) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.ID);
        } else if (e.getSource() == menuViewTreeIdCtx) {
            executionTreeViewer.setLocationViewType(ExecutionTreeViewer.LocationViewType.ID_CTX);
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
        }
    }

    public void load(String dir) {
        rootPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        openFolderStartDir = Paths.get(dir).normalize().getParent().toString();

        try {
            sourceMapping.load(dir);
            executionTree.load(dir);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPanel, "Load has FAILED: " + e.toString());
            clear();
            rootPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }

        for (int i = 0; i < executionTree.getAnalyses().length; ++i) {
            StrategyAnalysis strategyAnalysis = executionTree.getStrategyAnalyses()[i];
            Analysis analysis = executionTree.getAnalyses()[i];
            ((DefaultTableModel)analysesTable.getModel()).addRow(new Object[]{
                analysis.getIndex() + 1,
                analysis.getType(),
                analysis.getNumTraces(),
                strategyAnalysis.getStrategy()
            });
        }
        resizeColumnWidth(analysesTable);

        sourceC.load();
        sourceLL.load();

        analysesTable.setRowSelectionInterval(executionTree.getAnalysisIndex(), executionTree.getAnalysisIndex());

        // not working as expected
        JScrollBar verticalBar = listScrollPane.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());

        analysisNoneViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitshareViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisLocalSearchViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisBitflipViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintRequestViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        analysisTaintResponseViewer.onAnalysisChanged(executionTree.getAnalyses()[executionTree.getAnalysisIndex()]);
        executionTreeViewer.onLoad();
        sourceC.onAnalysisChanged();
        sourceLL.onAnalysisChanged();
        updateStrategyAnalysisInfo(executionTree.getAnalysisIndex());

        String rawName = Paths.get("").toAbsolutePath().relativize(Paths.get(dir)).toString();
        Frame.getFrames()[0].setTitle("Fizzer: ProgressExplorer [" + (rawName.isEmpty() ? "." : rawName) + "]");

        rootPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        sourceMapping.clear();
        executionTree.clear();
        ((DefaultListModel<StrategyAnalysis>)analysesTable.getModel()).clear();
        analysisNoneViewer.clear();
        analysisBitshareViewer.clear();
        analysisLocalSearchViewer.clear();
        analysisBitflipViewer.clear();
        analysisTaintRequestViewer.clear();
        analysisTaintResponseViewer.clear();
        executionTreeViewer.clear();
        sourceC.clear();
        sourceLL.clear();
    }

    public static void main( String[] args ) {
        // try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Fizzer: ProgressExplorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setPreferredSize(new Dimension(1024, 768));

                ProgressExplorer explorer = new ProgressExplorer();
                if (args.length == 1)
                    explorer.load(Paths.get(args[0]).toAbsolutePath().toString());

                explorer.activeAnalysisCard();

                JMenu menuFile = new JMenu("File");
                menuFile.setMnemonic(KeyEvent.VK_F);
                menuFile.add(explorer.menuFileOpen);
                menuFile.add(explorer.menuFileExit);

                JMenu menuView = new JMenu("View");
                menuView.setMnemonic(KeyEvent.VK_W);
                menuView.add(explorer.menuViewAnalysisTab);
                menuView.add(explorer.menuViewTreeTab);
                menuView.add(explorer.menuViewCTab);
                menuView.add(explorer.menuViewLLTab);
                menuView.addSeparator();
                menuView.add(explorer.menuViewTreeId);
                menuView.add(explorer.menuViewTreeIdCtx);
                menuView.add(explorer.menuViewTreeC);
                menuView.add(explorer.menuViewTreeLL);
                menuView.add(explorer.menuViewSensitiveBits);
                menuView.add(explorer.menuViewInputBytes);
                menuView.add(explorer.menuViewBestValue);
                menuView.add(explorer.menuViewTraceIndex);
                menuView.add(explorer.menuViewNodeGuid);

                JMenuBar menuBar = new JMenuBar();
                menuBar.add(menuFile);
                menuBar.add(menuView);
                frame.setJMenuBar(menuBar);

                
                frame.setContentPane(explorer.rootPanel);

                try {
                    URL url = getClass().getResource("/icon.png");
                    frame.setIconImage(Toolkit.getDefaultToolkit().getImage(url)); 
                } catch (Exception e) {} 

                frame.pack();
                frame.setVisible(true);
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });
    }
}
