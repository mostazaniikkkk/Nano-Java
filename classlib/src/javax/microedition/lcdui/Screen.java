package javax.microedition.lcdui;

/*
 * Base of the built-in screens (Form, List, TextBox, Alert). A screen is
 * drawn as a title bar, a content area and a soft key bar; subclasses
 * paint the content and handle navigation keys.
 */
public abstract class Screen extends Displayable {
    Screen() {
    }

    void paintDisplayable(Graphics g) {
        int w = Display.screenWidth();
        int h = Display.screenHeight();
        int top = Theme.titleHeight();
        int bottom = h - Theme.barHeight();
        g.setColor(Theme.BACKGROUND);
        g.fillRect(0, 0, w, h);
        Theme.drawTitle(g, title, w);
        g.setClip(0, top, w, bottom - top);
        g.translate(0, top);
        paintContent(g, w, bottom - top);
        g.translate(0, -top);
        g.setClip(0, 0, w, h);
        String[] labels = softLabels();
        Theme.drawSoftKeys(g, labels[0], labels[1], w, h);
    }

    /* Soft key labels {left, right}; screens that handle the soft keys
     * themselves (wantsSoftKeys) override this. */
    String[] softLabels() {
        return Display.softLabels(this);
    }

    /* An item's size or contents changed; Form re-lays itself out. */
    void itemInvalidated() {
        repaintDisplayable();
    }

    /* Paints the content area; (0, 0) is its top-left corner. */
    abstract void paintContent(Graphics g, int w, int h);

    /* Content area size, for layout. */
    int contentWidth() {
        return Display.screenWidth();
    }

    int contentHeight() {
        return Display.screenHeight() - Theme.titleHeight() - Theme.barHeight();
    }

    /* Screens report the content area, as MIDP specifies. */
    public int getWidth() {
        return contentWidth();
    }

    public int getHeight() {
        return contentHeight();
    }

    /* Moves the focus to item (Display.setCurrentItem). */
    void focusItem(Item item) {
    }
}
