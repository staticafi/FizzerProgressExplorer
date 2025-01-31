package fizzer;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.text.*;

public abstract class SourceViewerBase extends TextViewerBase {

    @Override
    public Color getCharacterHighlightColor(int charIdx) {
        int id = indexToId.get(charIdx);
        boolean isLeftCovered = isCovered(id, false);
        boolean isRightCovered = isCovered(id, true);
        if (isLeftCovered && isRightCovered)
            return LIGHT_MAGENTA;
        else if (isLeftCovered)
            return LIGHT_RED;
        else if (isRightCovered)
            return LIGHT_BLUE;
        else
            return LIGHT_ORANGE;
    }

    protected int numLineColumnChars;
    private HashMap<Integer, Integer> indexToId;
    private TextHighlightPainter textHighlightPainter;

    @SuppressWarnings("unchecked")
    public void load() {
        clear();
        indexToId = new HashMap<>();
        textHighlightPainter = new TextHighlightPainter();

        int lineColumnSize = 1;
        for (int n = getSourceCodeLines().size(); n > 10; n /= 10)
            ++lineColumnSize;

        numLineColumnChars = lineColumnSize + 2;

        String lineColumnFormat = "%" + Integer.toString(lineColumnSize) + "s";
        
        StringBuilder stringBuilder = new StringBuilder();
        
        int line = 1;
        for (String text : getSourceCodeLines()) {
            Object value = getInvertedMapping(line);
            stringBuilder.append(String.format(lineColumnFormat, line));
            stringBuilder.append(": ");
            if (value != null) {
                if (value instanceof Integer)
                    indexToId.put(stringBuilder.length(), (Integer)value);
                else
                    for (Map.Entry<Integer, Integer> entry : ((Map<Integer, Integer>)value).entrySet())
                        indexToId.put(stringBuilder.length() + entry.getKey() - 1, entry.getValue());
            }
            stringBuilder.append(text);
            stringBuilder.append('\n');
            ++line;
        }

        String wholeText = stringBuilder.toString();

        setText(wholeText);
        setCaretPosition(0);

        for (Integer start : indexToId.keySet()) {
            int end = start + 1;
            while(end < wholeText.length() && belongsToMark(wholeText.charAt(end)))
                ++end;
            try {
                getHighlighter().addHighlight(start, end, textHighlightPainter);
            } catch (BadLocationException e) { /* Ignore */ }
        }
    }

    public void onAnalysisChanged() {
        repaint();
    }

    public SourceViewerBase() {
        super();
        clear();
    }

    public void clear() {
        super.clear();
        indexToId = null;
        textHighlightPainter = null;
    }

    public void setLine(int line) {
        try {
            int y = (int) modelToView2D(getLineStartOffset(line - 1)).getY();
            Rectangle viewRect = new Rectangle(getVisibleRect());
            viewRect.y = Math.max(0, y - viewRect.height / 2);
            scrollRectToVisible(viewRect);
        } catch (BadLocationException e){
            // Nothing to do.
        }
    }

    @SuppressWarnings("unchecked")
    public int getIdOfCurrentLine(MouseEvent e) {
        try {
            int idx = viewToModel2D(e.getPoint());
            int line = getLineOfOffset(idx);
            Object idObj = getInvertedMapping(line + 1);
            if (idObj != null) {
                if (idObj instanceof TreeMap) {
                    TreeMap<Integer, Integer> idTreeMap = (TreeMap<Integer, Integer>) idObj;
                    int column = (idx - getLineStartOffset(line)) - numLineColumnChars;
                    return idTreeMap.getOrDefault(column, -1);
                }
                return (int) idObj;
            }
        } catch (BadLocationException e1) {
            // Nothing to do.
        }
        return -1;
    }

    public abstract List<String> getSourceCodeLines();
    public abstract Object getInvertedMapping(int line);
    public abstract boolean belongsToMark(char c);
    public abstract boolean isCovered(int id, boolean direction);
}
