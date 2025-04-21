package fizzer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import javax.swing.text.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class TextViewerBase extends RSyntaxTextArea {

    public static final Color LIGHT_RED = new Color(255, 128, 128);
    public static final Color LIGHT_GREEN = new Color(128, 255, 128);
    public static final Color LIGHT_BLUE = new Color(150, 150, 255);
    public static final Color LIGHT_MAGENTA = new Color(255, 128, 255);
    public static final Color LIGHT_ORANGE = new Color(255, 200, 100);

    public class TextHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        private Color background;

        public TextHighlightPainter() {
            super(Color.LIGHT_GRAY);
            background = Color.LIGHT_GRAY;
        }

        @Override
        public Color getColor() {
            return background;
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            background = getCharacterHighlightColor(offs0);
            return super.paintLayer(g, offs0, offs1, bounds, c, view);
        }
    }

    protected TextHighlightPainter textHighlightPainter;

    public Color getCharacterHighlightColor(int charIdx) {
        return Color.LIGHT_GRAY;
    }

    public TextViewerBase() {
        textHighlightPainter = new TextHighlightPainter();
        clear();
        setFont(new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize));
        setEditable(false);
        setFocusable(false);
    }

    public void clear() {
        getHighlighter().removeAllHighlights();
        setText("");
    }
}
