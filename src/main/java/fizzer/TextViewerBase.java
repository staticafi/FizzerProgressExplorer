package fizzer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import javax.swing.text.*;
import java.io.IOException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;

public class TextViewerBase extends RSyntaxTextArea {

    public static boolean setDarkTheme(final RSyntaxTextArea area) {
        try {
            Theme dark = Theme.load(area.getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            dark.apply(area);
            return true;
        } catch (IOException e) { return false; }
    }

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
        setDarkTheme(this);
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
