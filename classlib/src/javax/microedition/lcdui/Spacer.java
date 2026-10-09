package javax.microedition.lcdui;

public class Spacer extends Item {
    private int minWidth;
    private int minHeight;

    public Spacer(int minWidth, int minHeight) {
        super(null);
        setMinimumSize(minWidth, minHeight);
    }

    public void setMinimumSize(int minWidth, int minHeight) {
        if (minWidth < 0 || minHeight < 0) {
            throw new IllegalArgumentException();
        }
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        invalidate0();
    }

    public void addCommand(Command cmd) {
        throw new IllegalStateException();
    }

    public void setDefaultCommand(Command cmd) {
        throw new IllegalStateException();
    }

    public void setLabel(String label) {
        throw new IllegalStateException();
    }

    public int getMinimumWidth() {
        return minWidth;
    }

    public int getMinimumHeight() {
        return minHeight;
    }

    int layout(int w) {
        labelHeight = 0;
        bodyHeight = Math.max(minHeight, lockedHeight);
        height = bodyHeight;
        return height;
    }

    int measureBody(int w) {
        return minHeight;
    }

    void paintItem(Graphics g, int w, boolean focused) {
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
    }
}
