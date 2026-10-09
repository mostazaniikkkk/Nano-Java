package javax.microedition.lcdui;

/*
 * Rows of elements with a focus highlight. Up/down move the focus
 * (wrapping around), left/right page. Fire: IMPLICIT selects and runs the
 * select command, EXCLUSIVE selects, MULTIPLE toggles.
 */
public class List extends Screen implements Choice {
    public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);

    private final ChoiceModel model;
    private Command selectCommand = SELECT_COMMAND;
    private int focus;
    private int first;
    private int pressedRow = -1;

    public List(String title, int listType) {
        this(title, listType, new String[0], null);
    }

    public List(String title, int listType, String[] stringElements, Image[] imageElements) {
        if (listType != IMPLICIT && listType != EXCLUSIVE && listType != MULTIPLE) {
            throw new IllegalArgumentException();
        }
        if (stringElements == null) {
            throw new NullPointerException();
        }
        if (imageElements != null && imageElements.length != stringElements.length) {
            throw new IllegalArgumentException();
        }
        this.title = title;
        model = new ChoiceModel(listType);
        for (int i = 0; i < stringElements.length; i++) {
            model.append(stringElements[i], imageElements != null ? imageElements[i] : null);
        }
    }

    private void changed() {
        if (focus >= model.size()) {
            focus = Math.max(0, model.size() - 1);
        }
        repaintDisplayable();
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
        if (elementNum <= focus && model.size() > 1) {
            focus++;
        }
        changed();
    }

    public void delete(int elementNum) {
        model.delete(elementNum);
        if (elementNum < focus) {
            focus--;
        }
        changed();
    }

    public void deleteAll() {
        model.deleteAll();
        focus = 0;
        first = 0;
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
        if (selected && model.type != MULTIPLE) {
            focus = elementNum;
        }
        changed();
    }

    public void setSelectedFlags(boolean[] selectedArray) {
        model.setSelectedFlags(selectedArray);
        changed();
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

    public void setSelectCommand(Command command) {
        if (model.type != IMPLICIT) {
            return;
        }
        if (command != null && command != SELECT_COMMAND) {
            addCommand(command);
        }
        selectCommand = command;
    }

    public void removeCommand(Command cmd) {
        if (cmd == selectCommand) {
            selectCommand = null;
        }
        super.removeCommand(cmd);
    }

    /* ---- Screen hooks ---- */

    void paintContent(Graphics g, int w, int h) {
        int n = model.size();
        if (n == 0) {
            return;
        }
        int rh = model.rowHeight();
        int visible = Math.max(1, h / rh);
        int f = Math.min(focus, n - 1);
        if (f < first) {
            first = f;
        } else if (f >= first + visible) {
            first = f - visible + 1;
        }
        if (first > Math.max(0, n - visible)) {
            first = Math.max(0, n - visible);
        }
        boolean bar = n > visible;
        int rw = bar ? w - 4 : w;
        for (int i = first; i < n && (i - first) * rh < h; i++) {
            model.paintRow(g, i, 0, (i - first) * rh, rw, i == f, true);
        }
        if (bar) {
            int bh = Math.max(10, h * visible / n);
            int by = first * (h - bh) / Math.max(1, n - visible);
            g.setColor(Theme.BORDER);
            g.fillRect(w - 3, by, 3, bh);
        }
    }

    private void activate(int i) {
        if (i < 0 || i >= model.size()) {
            return;
        }
        if (model.type == MULTIPLE) {
            model.setSelectedIndex(i, !model.isSelected(i));
        } else {
            model.setSelectedIndex(i, true);
        }
        repaintDisplayable();
        Command c = selectCommand;
        if (model.type == IMPLICIT && c != null) {
            fireCommand(c);
        }
    }

    boolean keyEvent(int type, int code) {
        if (type == nanojava.Events.KEY_UP) {
            return true;
        }
        int n = model.size();
        int visible = Math.max(1, contentHeight() / model.rowHeight());
        boolean press = type == nanojava.Events.KEY_DOWN;
        switch (Canvas.gameAction(code)) {
        case Canvas.UP:
            if (focus > 0) {
                focus--;
            } else if (press && n > 0) {
                focus = n - 1;
            }
            break;
        case Canvas.DOWN:
            if (focus < n - 1) {
                focus++;
            } else if (press) {
                focus = 0;
            }
            break;
        case Canvas.LEFT:
            focus = Math.max(0, focus - visible);
            break;
        case Canvas.RIGHT:
            focus = Math.max(0, Math.min(n - 1, focus + visible));
            break;
        case Canvas.FIRE:
            if (press) {
                activate(focus);
            }
            return true;
        default:
            return false;
        }
        repaintDisplayable();
        return true;
    }

    void pointerEvent(int type, int x, int y) {
        int cy = y - Theme.titleHeight();
        int row = cy < 0 || cy >= contentHeight() ? -1 : first + cy / model.rowHeight();
        if (row >= model.size()) {
            row = -1;
        }
        if (type == nanojava.Events.POINTER_DOWN) {
            pressedRow = row;
            if (row >= 0) {
                focus = row;
                repaintDisplayable();
            }
        } else if (type == nanojava.Events.POINTER_UP) {
            if (row >= 0 && row == pressedRow) {
                activate(row);
            }
            pressedRow = -1;
        }
    }
}
