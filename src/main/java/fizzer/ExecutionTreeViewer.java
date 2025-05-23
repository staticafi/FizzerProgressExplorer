package fizzer;

import java.util.ArrayList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ExecutionTreeViewer extends JPanel {

    public static enum LocationViewType { ID, C, LL, SENSITIVE_BITS, INPUT_BYTES, VALUE, TRACE_INDEX, NODE_GUID }

    public static final int nodeWidth = 100;
    public static final int nodeHeight = 14;

    private static final int separatorHorizontal = 15;
    private static final int separatorVertical = 80;
    private static final int hitCountHeight = nodeHeight / 2;
    private static final int coverageWidth = nodeWidth / 2;
    private static final int coverageHeight = nodeHeight / 2;
    private static final int nodeMarkWidth = separatorHorizontal / 2;
    private static final int terminalSize = nodeHeight + hitCountHeight;
    private static final int closedSize = 2 * terminalSize;
    private static final int textShift = 2;
    private static final float textZoomLimit = 0.5f;
    private static final int borderSize = 25;
    private static final Color[] edgeColors = new Color[] { Color.RED, Color.BLUE };
    private static final Color nodeColorNoAnalysis = Color.BLACK;
    private static final Color nodeColorBitshare = new Color(255,125,125);
    private static final Color nodeColorLocalSearch = new Color(125,255,125);
    private static final Color nodeColorBitflip = new Color(125,125,255);
    private static final Color nodeColorSensitivity = Color.GRAY;
    private static final Color nodeColorBitshareLocalSearch = Color.MAGENTA;
    private static final Color nodeNoSensitiveBitsColor = Color.ORANGE;
    private static final Color nodeMarkColor = Color.BLACK;
    private static final Color hitCountColor = Color.BLACK;
    private static final Color coveredColor = Color.BLACK;
    private static final Color fontColor = Color.BLACK;

    private ExecutionTree executionTree;
    private SourceMapping mapping;
    private float zoom;
    private LocationViewType locationViewType;
    private Rectangle viewRect;
    private List<TrNode> visibleNodes;
    private Font font;
    private NodeAndDirection mark;
    
    private Font makeFont(float zoom) {
        return font = new Font("Monospaced", Font.PLAIN, Math.round((1.0f * nodeHeight) * zoom));
    }

    private class NodeAndDirection {
        public NodeAndDirection(final Node node_, final boolean direction_) { node = node_; direction = direction_; }
        public final Node node;
        public final boolean direction;
    } 

    // Node with Transformed Coordinates
    private class TrNode {
        private Rectangle transformedCoordinates;
        private Node node;

        public TrNode(Rectangle transformedCoordinates, Node node) {
            this.transformedCoordinates = transformedCoordinates;
            this.node = node;
        } 

        public Rectangle getCoordinates() {
            return this.transformedCoordinates;
        }

        public Node getNode() {
            return this.node;
        }
    }

    public ExecutionTreeViewer(ExecutionTree et, SourceMapping sourceMapping) {
        executionTree = et;
        mapping = sourceMapping;

        zoom = 1.0f;
        setBackground(Color.white);

        locationViewType = LocationViewType.C;

        viewRect = getVisibleRect();
        visibleNodes = new ArrayList<>();

        font = makeFont(zoom);

        mark = new NodeAndDirection(null, false);

        setAutoscrolls(true);
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
                else if (e.getButton() == MouseEvent.BUTTON2) {
                    if (e.isControlDown()) makeMarkNodeVisible(); else makeAnalysisNodeVisible();
                }
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

    public ExecutionTree getTree() {
        return executionTree;
    }

    public Analysis getAnalysis(int analysisIndex) {
        return executionTree.getAnalyses()[analysisIndex];
    }

    public Analysis getAnalysis() {
        return getAnalysis(executionTree.getAnalysisIndex());
    }

    public int getNumAnalyses() {
        return executionTree.getAnalyses().length;
    }

    public void onLoad() {
        int maxDiscoveryIndex = 0;
        for (int i = 0; i < executionTree.getAnalyses().length; ++i) {
            maxDiscoveryIndex += getAnalysis(i).getNumTraces();
            getAnalysis(i).getViewProps().maxDiscoveryIndex = maxDiscoveryIndex - 1;
        }

        if (executionTree.getRootNode() != null) {
            computeNodeLocations(executionTree.getRootNode(), borderSize + nodeWidth / 2, 0);
            for (int i = 0; i < executionTree.getAnalyses().length; ++i) {
                computeAreas(executionTree.getRootNode(), i);
                getAnalysis(i).getViewProps().area.width += borderSize;
                getAnalysis(i).getViewProps().area.height += borderSize;
            }
        }

        updateArea();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (executionTree != null && executionTree.getRootNode() != null) {
                    Rectangle rect = new Rectangle(getVisibleRect());
                    rect.x = executionTree.getRootNode().getViewProps().x + nodeWidth/2 - (int)rect.getWidth() / 2;
                    rect.y = executionTree.getRootNode().getViewProps().y + nodeHeight/2 - (int)rect.getHeight() / 2;
                    scrollRectToVisible(rect);    
                }
            }
        });
    }

    public Node getNodeBasedOnMousePosition(int x, int y) {
        for (TrNode node : visibleNodes) {
            if (node.getCoordinates().contains(x, y)) {
                return node.getNode();
            }
        }
        return null;
    }

    private int computeNodeLocations(Node node, int minX, int depth) {
        Node[] children = node.getChildren();
        if (children[0] != null && children[1] != null) {
            minX = Math.max(minX, computeNodeLocations(children[0], minX, depth + 1));
            minX = Math.max(minX, computeNodeLocations(children[1], minX + nodeWidth + separatorHorizontal, depth + 1));
            node.getViewProps().x = (children[0].getViewProps().x + children[1].getViewProps().x) / 2;
            node.getViewProps().subTreeMinX = children[0].getViewProps().subTreeMinX;
            node.getViewProps().subTreeMaxX = children[1].getViewProps().subTreeMaxX;
        } else if (children[0] != null) {
            minX = Math.max(minX, computeNodeLocations(children[0], minX, depth + 1));
            node.getViewProps().x = children[0].getViewProps().x;
            node.getViewProps().subTreeMinX = children[0].getViewProps().subTreeMinX;
            node.getViewProps().subTreeMaxX = children[0].getViewProps().subTreeMaxX;
        } else if (children[1] != null) {
            minX = Math.max(minX, computeNodeLocations(children[1], minX, depth + 1));
            node.getViewProps().x = children[1].getViewProps().x;
            node.getViewProps().subTreeMinX = children[1].getViewProps().subTreeMinX;
            node.getViewProps().subTreeMaxX = children[1].getViewProps().subTreeMaxX;
        } else {
            node.getViewProps().x = minX;
            node.getViewProps().subTreeMinX = minX - nodeWidth / 2 - nodeMarkWidth;
            node.getViewProps().subTreeMaxX = minX + nodeWidth / 2 + nodeMarkWidth;
        }
        node.getViewProps().y = borderSize + depth * (nodeHeight + separatorVertical);
        return minX;
    }

    private void computeAreas(Node node, int analysisIndex) {
        if (node == null || getAnalysis(analysisIndex).getViewProps().maxDiscoveryIndex < node.getDiscoveryIndex())
            return;

        computeAreas(node.getChildren()[0], analysisIndex);
        computeAreas(node.getChildren()[1], analysisIndex);

        int maxX = node.getViewProps().x + nodeWidth/2;
        if (getAnalysis(analysisIndex).getViewProps().area.width < maxX)
            getAnalysis(analysisIndex).getViewProps().area.width = maxX;

        int maxY = node.getViewProps().y + nodeHeight/2;
        if (getAnalysis(analysisIndex).getViewProps().area.height < maxY)
            getAnalysis(analysisIndex).getViewProps().area.height = maxY;
    }

    public void updateArea() {
        if (!executionTree.isLoaded())
            return;
        Dimension analysisArea = getAnalysis().getViewProps().area;
        setPreferredSize(new Dimension(Math.round(zoom * analysisArea.width), Math.round(zoom * analysisArea.height)));
        revalidate();
        repaint();
    }

    public void onAnalysisChanged() {
        updateArea();
    }

    public void clear() {
        revalidate();
        repaint();
    }

    public LocationViewType getLocationViewType() {
        return this.locationViewType;
    }

    public void onZoomChanged(int zoomPercentage) {
        float newZoom = zoomPercentage / 100.0f;
        Point mouse = getMousePosition();
        viewRect = new Rectangle(getVisibleRect());
        if (mouse != null) {
            viewRect.x += Math.round(newZoom / zoom * mouse.x) - mouse.x;
            viewRect.y += Math.round(newZoom / zoom * mouse.y) - mouse.y;
        }
        zoom = newZoom;
        font = makeFont(zoom); 
        updateArea();
    }

    public void setLocationViewType(LocationViewType type) {
        locationViewType = type;
        updateArea();
    }

    public void clearMark() { mark = new NodeAndDirection(null, false); revalidate(); repaint(); }
    public void setMark(Node node, boolean dir) { mark = new NodeAndDirection(node, dir); revalidate(); repaint(); }

    public List<TrNode> getVisibleNodes() {
        return Collections.unmodifiableList(visibleNodes);
    }

    public void makeAnalysisNodeVisible() {
        makeNodeVisible(getAnalysis().getNode());
    }

    public void makeMarkNodeVisible() {
        makeNodeVisible(mark.node);
    }

    public void makeNodeVisible(Node node) {
        if (node == null || getAnalysis().getViewProps().maxDiscoveryIndex < node.getDiscoveryIndex())
            return;
        Node.ViewProps props = node.getViewProps(); 
        Rectangle r = new Rectangle(getVisibleRect());
        r.x = Math.round(zoom * props.x) - r.width / 2;
        r.y = Math.round(zoom * props.y) - r.height / 2;
        scrollRectToVisible(r);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (viewRect != null) {
            scrollRectToVisible(viewRect);
            viewRect = null;
        }
        if (executionTree.isLoaded()) {
            g.setFont(font);
            if (getAnalysis().getNode() != null)
                drawCross(g, getAnalysis().getNode().getViewProps().x, getAnalysis().getNode().getViewProps().y, getAnalysis(getNumAnalyses() - 1).getViewProps().area);
            visibleNodes.clear();
            drawSubTree(g, executionTree.getRootNode(), getVisibleRect());
        }
    }

    private void drawCross(Graphics g, int x, int y, Dimension area) {
        g.setColor(new Color(200, 200, 200));
        g.drawLine(Math.round(zoom * x), 0, Math.round(zoom * x), Math.round((float)area.getHeight()));
        g.drawLine(0, Math.round(zoom * y), Math.round((float)area.getWidth()), Math.round(zoom * y));
    }

    private void drawSubTree(Graphics g, Node node, Rectangle visibleRect) {
        if (zoom * node.getViewProps().subTreeMinX > visibleRect.x + visibleRect.width || zoom * node.getViewProps().subTreeMaxX < visibleRect.x)
            return;
        if (zoom * (node.getViewProps().y - nodeHeight/2 - hitCountHeight - closedSize) > visibleRect.y + visibleRect.height)
            return;
        Node[] children = node.getChildren();
        if (zoom * (node.getViewProps().y + nodeHeight + separatorVertical) < visibleRect.y) {
            for (int i = 0; i != 2; ++i)
                if (children[i] != null && children[i].getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex)
                    drawSubTree(g, children[i], getVisibleRect());
            return;
        }
        for (int i = 0; i != 2; ++i) {
            g.setColor(edgeColors[i]);
            if (children[i] != null && children[i].getDiscoveryIndex() <= getAnalysis().getViewProps().maxDiscoveryIndex) {
                g.drawLine(
                    Math.round(zoom * node.getViewProps().x),
                    Math.round(zoom * (node.getViewProps().y + nodeHeight/2)),
                    Math.round(zoom * children[i].getViewProps().x),
                    Math.round(zoom * (children[i].getViewProps().y - nodeHeight/2 - hitCountHeight))
                    );
                drawSubTree(g, children[i], getVisibleRect());
            } else {
                int dirX = 2 * i - 1;
                switch (node.getChildLabel(executionTree.getAnalysisIndex(), i)) {
                    case NOT_VISITED:
                        g.drawLine(
                            Math.round(zoom * (node.getViewProps().x + dirX * (nodeWidth/2 - terminalSize))),
                            Math.round(zoom * (node.getViewProps().y + nodeHeight/2)),
                            Math.round(zoom * (node.getViewProps().x + dirX * nodeWidth/2)),
                            Math.round(zoom * (node.getViewProps().y + nodeHeight/2 + terminalSize))
                            );
                        break;
                    case END_EXCEPTIONAL:
                        g.fillOval(
                            Math.round(zoom * (node.getViewProps().x + dirX * (nodeWidth/2 - terminalSize/2) - terminalSize/2)),
                            Math.round(zoom * (node.getViewProps().y + nodeHeight/2)),
                            Math.round(zoom * terminalSize),
                            Math.round(zoom * terminalSize)
                            );
                        break;
                    case END_NORMAL:
                        g.drawOval(
                            Math.round(zoom * (node.getViewProps().x + dirX * (nodeWidth/2 - terminalSize/2) - terminalSize/2)),
                            Math.round(zoom * (node.getViewProps().y + nodeHeight/2)),
                            Math.round(zoom * terminalSize),
                            Math.round(zoom * terminalSize)
                            );
                        break;
                    default: /* Cannot happen. */ break;
                }
            }
        }

        float hitRatio = (float)node.getHitCount(executionTree.getAnalysisIndex()) /
                         (float)executionTree.getRootNode().getHitCount(executionTree.getAnalysisIndex());

        // Calculating Rectangle of Node that will be Rendered and Storing the Information
        int trNodeX = Math.round(zoom * (node.getViewProps().x - nodeWidth/2));
        int trNodeY = Math.round(zoom * (node.getViewProps().y - nodeHeight/2 - hitCountHeight));
        int trNodeWidth = Math.round(zoom * nodeWidth);
        int trNodeHeight = Math.round(zoom * (hitCountHeight + coverageHeight + nodeHeight));

        visibleNodes.add(new TrNode(new Rectangle(trNodeX, trNodeY, trNodeWidth, trNodeHeight), node));

        g.setColor(hitCountColor);
        g.drawRect(
            Math.round(zoom * (node.getViewProps().x - nodeWidth/2)),
            Math.round(zoom * (node.getViewProps().y - nodeHeight/2 - hitCountHeight)),
            Math.round(zoom * nodeWidth),
            Math.round(zoom * hitCountHeight)
            );
        g.fillRect(
            Math.round(zoom * (node.getViewProps().x - hitRatio * nodeWidth/2)),
            Math.round(zoom * (node.getViewProps().y - nodeHeight/2 - hitCountHeight)),
            Math.round(zoom * hitRatio * nodeWidth),
            Math.round(zoom * hitCountHeight)
            );

        if (executionTree.isCovered(node.getLocationId(), false) && executionTree.isCovered(node.getLocationId(), true)) {
            g.setColor(coveredColor);
            g.fillRect(
                Math.round(zoom * (node.getViewProps().x - coverageWidth/2)),
                Math.round(zoom * (node.getViewProps().y + nodeHeight/2)),
                Math.round(zoom * coverageWidth),
                Math.round(zoom * coverageHeight)
                );
        }

        int numSensitiveBits = node.getSensitiveBits(executionTree.getAnalysisIndex()).size();

        if (!node.sensitivityApplied(executionTree.getAnalysisIndex()) && !node.bitflipApplied(executionTree.getAnalysisIndex())) {
            g.setColor(nodeColorNoAnalysis);
            g.drawRect(
                Math.round(zoom * (node.getViewProps().x - nodeWidth/2)),
                Math.round(zoom * (node.getViewProps().y - nodeHeight/2)),
                Math.round(zoom * nodeWidth),
                Math.round(zoom * nodeHeight)
                );
        }
        else {
            Color nodeColor = null;
            if (node.sensitivityApplied(executionTree.getAnalysisIndex())) {
                if (numSensitiveBits == 0)
                    nodeColor = nodeNoSensitiveBitsColor;
                else if (!node.localSearchApplied(executionTree.getAnalysisIndex()) && !node.bitshareApplied(executionTree.getAnalysisIndex())) {
                    nodeColor = nodeColorSensitivity;
                } else {
                    if (node.localSearchApplied(executionTree.getAnalysisIndex()) && node.bitshareApplied(executionTree.getAnalysisIndex()))
                        nodeColor = nodeColorBitshareLocalSearch;
                    else if (node.localSearchApplied(executionTree.getAnalysisIndex()))
                        nodeColor = nodeColorLocalSearch;
                    else if (node.bitshareApplied(executionTree.getAnalysisIndex()))
                        nodeColor = nodeColorBitshare;
                }
            }
            if (node.bitflipApplied(executionTree.getAnalysisIndex())) {
                nodeColor = nodeColor == null ? nodeColorBitflip :
                    new Color((nodeColor.getRed() + nodeColorBitflip.getRed()) / 2,
                            (nodeColor.getGreen() + nodeColorBitflip.getGreen()) / 2,
                            (nodeColor.getBlue() + nodeColorBitflip.getBlue()) / 2
                            );
            }
            if (nodeColor != null) {
                g.setColor(nodeColor);
                g.fillRect(
                    Math.round(zoom * (node.getViewProps().x - nodeWidth/2)),
                    Math.round(zoom * (node.getViewProps().y - nodeHeight/2)),
                    Math.round(zoom * nodeWidth),
                    Math.round(zoom * nodeHeight)
                    );
            }
        }

        if (node == getAnalysis().getNode()) {
            g.setColor(nodeMarkColor);
            g.fillPolygon(
                new int[] {
                    Math.round(zoom * (node.getViewProps().x - nodeWidth/2 - nodeMarkWidth)),
                    Math.round(zoom * (node.getViewProps().x - nodeWidth/2)),
                    Math.round(zoom * (node.getViewProps().x - nodeWidth/2 - nodeMarkWidth))
                    },
                new int[] {
                    Math.round(zoom * (node.getViewProps().y - nodeHeight/2)),
                    Math.round(zoom * node.getViewProps().y),
                    Math.round(zoom * (node.getViewProps().y + nodeHeight/2))
                    },
                3
                );
            g.fillPolygon(
                new int[] {
                    Math.round(zoom * (node.getViewProps().x + nodeWidth/2 + nodeMarkWidth)),
                    Math.round(zoom * (node.getViewProps().x + nodeWidth/2)),
                    Math.round(zoom * (node.getViewProps().x + nodeWidth/2 + nodeMarkWidth))
                    },
                new int[] {
                    Math.round(zoom * (node.getViewProps().y - nodeHeight/2)),
                    Math.round(zoom * node.getViewProps().y),
                    Math.round(zoom * (node.getViewProps().y + nodeHeight/2))
                    },
                3
                );
        }

        if (node.isClosed(executionTree.getAnalysisIndex())) {
            g.setColor(coveredColor);
            g.drawArc(
                Math.round(zoom * (node.getViewProps().x - closedSize/2)),
                Math.round(zoom * (node.getViewProps().y - nodeHeight/2 - hitCountHeight - closedSize/2)),
                Math.round(zoom * closedSize),
                Math.round(zoom * closedSize),
                0,180
                );
        }

        if (zoom >= textZoomLimit) {
            g.setColor(fontColor);
            String text;
            switch (locationViewType) {
                case ID:
                    text = 'i' + Integer.toUnsignedString(node.getLocationId().id);
                    break;
                case C: {
                    SourceMapping.LineColumn lineColumn = mapping.getCondMapC(node.getLocationId().id);
                    if (lineColumn == null)
                        text = Integer.toUnsignedString(node.getLocationId().id);
                    else
                        text = 'c' + Integer.toUnsignedString(lineColumn.line) + ':' + Integer.toUnsignedString(lineColumn.column);
                    break;
                    }
                case LL: {
                    Integer line = mapping.getCondMapLL(node.getLocationId().id);
                    if (line == null)
                        text = Integer.toUnsignedString(node.getLocationId().id);
                    else
                        text = 'L' + Integer.toUnsignedString(line);
                    break;
                    }
                case SENSITIVE_BITS:
                    text = 's' + (node.sensitivityApplied(executionTree.getAnalysisIndex()) ? Integer.toUnsignedString(numSensitiveBits) : "???");
                    break;
                case INPUT_BYTES:
                    text = 'b' + Integer.toUnsignedString(node.getNumInputBytes());
                    break;
                case VALUE:
                    text = 'v' + Double.toString(node.getBestValue(executionTree.getAnalysisIndex()));
                    break;
                case TRACE_INDEX:
                    text = 't' + Integer.toUnsignedString(node.getTraceIndex());
                    break;
                case NODE_GUID:
                    text = 'g' + Long.toUnsignedString(node.guid);
                    break;
                default:
                    text = "<UNKNOWN-LOCATION-VIEW-TYPE>";
                    break;
            }
            g.drawString(
                text,
                Math.round(zoom * (node.getViewProps().x - nodeWidth/2 + textShift)),
                Math.round(zoom * (node.getViewProps().y + nodeHeight/2 - textShift))
            );
        }

        if (node == mark.node) {
            g.setColor(edgeColors[mark.direction ? 1 : 0]);
            g.drawRect(
                Math.round(zoom * (node.getViewProps().x - nodeWidth/2 - separatorHorizontal/2)),
                Math.round(zoom * (node.getViewProps().y - nodeHeight/2 - separatorVertical/2)),
                Math.round(zoom * (nodeWidth + separatorHorizontal)),
                Math.round(zoom * (nodeHeight + separatorVertical))
                );
        }
    }
}
