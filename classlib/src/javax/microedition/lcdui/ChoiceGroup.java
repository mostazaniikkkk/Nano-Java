package javax.microedition.lcdui;

/*
 * EXCLUSIVE and MULTIPLE groups show every element; up/down move between
 * them and fire selects/toggles. POPUP shows only the selected element;
 * left/right or fire cycle through the elements.
 */
public class ChoiceGroup extends Item implements Choice {
    private static final int ARROWS = 22;

    private final ChoiceModel model;
    private int focusEl;

    public ChoiceGroup(String label, int choiceType) {
        this(label, choiceType, new String[0], null);
    }

    public ChoiceGroup(String label, int choiceType, String[] stringElements, Image[] imageElements) {
        super(label);
        if (choiceType != EXCLUSIVE && choiceType != MULTIPLE && choiceType != POPUP) {
            throw new IllegalArgumentException();
        }
        if (stringElements == null) {
            throw new NullPointerException();
        }
        if (imageElements != null && imageElements.length != stringElements.length) {
            throw new IllegalArgumentException();
        }
        model = new ChoiceModel(choiceType);
        for (int i = 0; i < stringElements.length; i++) {
            model.append(stringElements[i], imageElements != null ? imageElements[i] : null);
        }
    }

    private void changed() {
        if (focusEl >= model.size()) {
            focusEl = Math.max(0, model.size() - 1);
        }
        invalidate0();
    }

    public int size() {
        return model.size();
    }

    public String getString(int elementNum) {
        return model.getString(elementNum);
    }

    public Image getImage(int elementNum) {
        return model.getImage(elementNum);
    }

    public int append(String stringPart, Image imagePart) {
        int i = model.append(stringPart, imagePart);
        changed();
        return i;
    }

    public void insert(int elementNum, String stringPart, Image imagePart) {
        model.insert(elementNum, stringPart, imagePart);
        changed();
    }

    public void delete(int elementNum) {
        model.delete(elementNum);
        changed();
    }

    public void deleteAll() {
        model.deleteAll();
        changed();
    }

    public void set(int elementNum, String stringPart, Image imagePart) {
        model.set(elementNum, stringPart, imagePart);
        changed();
    }

    public boolean isSelected(int elementNum) {
        return model.isSelected(elementNum);
    }

    public int getSelectedIndex() {
        return model.getSelectedIndex();
    }

    public int getSelectedFlags(boolean[] selectedArray_return) {
        return model.getSelectedFlags(selectedArray_return);
    }

    public void setSelectedIndex(int elementNum, boolean selected) {
        model.setSelectedIndex(elementNum, selected);
        repaint0();
    }

    public void setSelectedFlags(boolean[] selectedArray) {
        model.setSelectedFlags(selectedArray);
        repaint0();
    }

    public void setFitPolicy(int fitPolicy) {
        model.setFitPolicy(fitPolicy);
    }

    public int getFitPolicy() {
        return model.getFitPolicy();
    }

    public void setFont(int elementNum, Font font) {
        model.setFont(elementNum, font);
        changed();
    }

    public Font getFont(int elementNum) {
        return model.getFont(elementNum);
    }

    /* ---- Item protocol ---- */

    private boolean popup() {
        return model.type == POPUP;
    }

    boolean isFocusable() {
        return model.size() > 0 || super.isFocusable();
    }

    int measureBody(int w) {
        int n = model.size();
        if (popup()) {
            return model.rowHeight() + 2;
        }
        return n * model.rowHeight();
    }

    void paintBody(Graphics g, int w, int h, boolean focused) {
        int rh = model.rowHeight();
        if (popup()) {
            g.setColor(Theme.FIELD_BG);
            g.fillRect(0, 0, w, rh + 2);
            int sel = model.getSelectedIndex();
            int x1 = g.cx1, y1 = g.cy1, x2 = g.cx2, y2 = g.cy2;
            g.clipRect(0, 0, w - ARROWS, rh + 2);
            model.paintRow(g, sel, 1, 1, w - ARROWS, false, false);
            g.cx1 = x1;
            g.cy1 = y1;
            g.cx2 = x2;
            g.cy2 = y2;
            g.setColor(focused ? Theme.HIGHLIGHT : Theme.BORDER);
            g.drawRect(0, 0, w - 1, rh + 1);
            int ax = w - ARROWS / 2 - 1;
            int cy = rh / 2 + 1;
            g.fillTriangle(ax - 8, cy, ax - 3, cy - 5, ax - 3, cy + 5);
            g.fillTriangle(ax + 8, cy, ax + 3, cy - 5, ax + 3, cy + 5);
            return;
        }
        int n = model.size();
        for (int i = 0; i < n; i++) {
            model.paintRow(g, i, 0, i * rh, w, focused && i == focusEl, true);
        }
    }

    boolean traverseItem(int dir, boolean entering) {
        int n = model.size();
        if (popup() || n == 0) {
            return false;
        }
        if (entering) {
            if (dir == Canvas.UP) {
                focusEl = n - 1;
            } else if (dir == Canvas.DOWN) {
                focusEl = 0;
            } else if (focusEl >= n) {
                focusEl = 0;
            }
            return true;
        }
        if (dir == Canvas.UP && focusEl > 0) {
            focusEl--;
            return true;
        }
        if (dir == Canvas.DOWN && focusEl < n - 1) {
            focusEl++;
            return true;
        }
        return false;
    }

    int focusTop() {
        if (popup() || focusEl == 0) {
            return 0;
        }
        return VPAD + labelHeight + focusEl * model.rowHeight();
    }

    int focusBottom() {
        if (popup()) {
            return height;
        }
        return Math.min(height, VPAD + labelHeight + (focusEl + 1) * model.rowHeight() + VPAD);
    }

    /* User action on element i (or the popup, when i < 0). */
    private void activate(int i, int step) {
        int n = model.size();
        if (n == 0) {
            return;
        }
        if (popup()) {
            int sel = model.getSelectedIndex();
            model.setSelectedIndex(((sel < 0 ? 0 : sel) + step + n) % n, true);
        } else if (model.type == MULTIPLE) {
            model.setSelectedIndex(i, !model.isSelected(i));
        } else {
            if (model.isSelected(i)) {
                return;
            }
            model.setSelectedIndex(i, true);
        }
        repaint0();
        notifyStateChanged();
    }

    boolean itemKey(int type, int code) {
        if (type != nanojava.Events.KEY_DOWN && type != Display.KEY_REPEAT) {
            return false;
        }
        int action = Canvas.gameAction(code);
        if (action == Canvas.FIRE && type == nanojava.Events.KEY_DOWN && model.size() > 0) {
            if (popup()) {
                activate(-1, 1);
                return true;
            }
            if (activationCommand() == null || model.type == MULTIPLE || !model.isSelected(focusEl)) {
                activate(focusEl, 0);
                return true;
            }
            return false;
        }
        if (popup() && (action == Canvas.LEFT || action == Canvas.RIGHT)) {
            activate(-1, action == Canvas.LEFT ? -1 : 1);
            return true;
        }
        return false;
    }

    boolean itemPointer(int type, int x, int y) {
        if (type != nanojava.Events.POINTER_UP || model.size() == 0) {
            return true;
        }
        if (popup()) {
            activate(-1, x < bodyWidth() / 3 ? -1 : 1);
        } else if (y >= 0) {
            int i = y / model.rowHeight();
            if (i < model.size()) {
                focusEl = i;
                activate(i, 0);
            }
        }
        return true;
    }

    private int bodyWidth() {
        return availWidth() - 2 * PAD;
    }
}
