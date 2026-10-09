package javax.microedition.lcdui;

import java.util.Vector;

/* Pop-up list of commands shown when a soft key has more than one. */
final class CommandMenu {
    private final Vector commands;
    private int selected;
    boolean closed;

    CommandMenu(Vector commands) {
        this.commands = commands;
    }

    /* Handles a key press; returns the chosen command, if any. */
    Command key(int code) {
        int action = Canvas.gameAction(code);
        if (action == Canvas.UP) {
            selected = (selected + commands.size() - 1) % commands.size();
        } else if (action == Canvas.DOWN) {
            selected = (selected + 1) % commands.size();
        } else if (action == Canvas.FIRE || code == Canvas.KEY_SOFT_LEFT) {
            closed = true;
            return (Command) commands.elementAt(selected);
        } else if (code == Canvas.KEY_SOFT_RIGHT) {
            closed = true;
        }
        return null;
    }

    void paint(Graphics g, int w, int h) {
        Font f = Theme.FONT;
        int rowH = f.getHeight() + 4;
        int menuW = 0;
        for (int i = 0; i < commands.size(); i++) {
            String label = ((Command) commands.elementAt(i)).getLabel();
            menuW = Math.max(menuW, f.stringWidth(label));
        }
        menuW = Math.min(w, menuW + 12);
        int menuH = rowH * commands.size() + 4;
        int x = 0;
        int y = Math.max(0, h - Theme.barHeight() - menuH);
        g.setColor(Theme.BACKGROUND);
        g.fillRect(x, y, menuW, menuH);
        g.setColor(Theme.BORDER);
        g.drawRect(x, y, menuW - 1, menuH - 1);
        g.setFont(f);
        for (int i = 0; i < commands.size(); i++) {
            int ry = y + 2 + i * rowH;
            if (i == selected) {
                g.setColor(Theme.HIGHLIGHT);
                g.fillRect(x + 2, ry, menuW - 4, rowH);
                g.setColor(Theme.HIGHLIGHT_TEXT);
            } else {
                g.setColor(Theme.FOREGROUND);
            }
            g.drawString(((Command) commands.elementAt(i)).getLabel(), x + 6, ry + 2,
                    Graphics.TOP | Graphics.LEFT);
        }
        Theme.drawSoftKeys(g, "Select", "Cancel", w, h);
    }
}
