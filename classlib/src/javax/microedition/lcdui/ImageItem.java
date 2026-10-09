package javax.microedition.lcdui;

public class ImageItem extends Item {
    private Image image;
    private String altText;
    private final int appearance;

    public ImageItem(String label, Image img, int layout, String altText) {
        this(label, img, layout, altText, PLAIN);
    }

    public ImageItem(String label, Image image, int layout, String altText, int appearanceMode) {
        super(label);
        if (appearanceMode < PLAIN || appearanceMode > BUTTON) {
            throw new IllegalArgumentException();
        }
        setLayout(layout);
        this.image = image;
        this.altText = altText;
        appearance = appearanceMode;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image img) {
        image = img;
        invalidate0();
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String text) {
        altText = text;
        invalidate0();
    }

    public int getAppearanceMode() {
        return appearance;
    }

    private int border() {
        return appearance == PLAIN ? 0 : 2;
    }

    int measureBody(int w) {
        Image img = image;
        if (img != null) {
            return img.getHeight() + 2 * border();
        }
        return altText != null ? Theme.FONT.getHeight() : 0;
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        Image img = image;
        if (img == null) {
            if (altText != null) {
                g.setFont(Theme.FONT);
                g.setColor(Theme.BORDER);
                g.drawString(altText, alignX(w, Theme.FONT.stringWidth(altText)), 0,
                        Graphics.TOP | Graphics.LEFT);
            }
            return;
        }
        int b = border();
        int iw = img.getWidth() + 2 * b;
        int x = alignX(w, iw);
        if (b > 0) {
            g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
            g.drawRect(x, 0, iw - 1, h - 1);
        }
        g.drawImage(img, x + b, b, Graphics.TOP | Graphics.LEFT);
    }
}
