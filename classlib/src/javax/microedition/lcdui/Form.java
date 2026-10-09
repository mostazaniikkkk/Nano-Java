package javax.microedition.lcdui;

import java.util.Vector;

/*
 * Items are stacked vertically, one per row. Up/down move the focus
 * between focusable items (or inside one, e.g. a ChoiceGroup), scrolling
 * when needed; other keys go to the focused item. App threads may change
 * the item list while the UI thread paints: the Vector calls are
 * synchronized and itemAt() tolerates a shrinking list.
 */
public class Form extends Screen {
    private final Vector items = new Vector();
    private ItemStateListener stateListener;
    private Item focused;
    private Item pressed;
    private int scroll;
    private int total;
    private int layoutWidth = -1;
    private boolean layoutDirty = true;

    public Form(String title) {
        this.title = title;
    }

    public Form(String title, Item[] items) {
        this(title);
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                append(items[i]);
            }
        }
    }

    public int append(Item item) {
        adopt(item);
        items.addElement(item);
        itemInvalidated();
        return items.size() - 1;
    }

    public int append(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        return append(new StringItem(null, str));
    }

    public int append(Image img) {
        if (img == null) {
            throw new NullPointerException();
        }
        return append(new ImageItem(null, img, Item.LAYOUT_DEFAULT, null));
    }

    public void insert(int itemNum, Item item) {
        if (itemNum < 0 || itemNum > items.size()) {
            throw new IndexOutOfBoundsException();
        }
        adopt(item);
        items.insertElementAt(item, itemNum);
        itemInvalidated();
    }

    public void delete(int itemNum) {
        Item it = get(itemNum);
        items.removeElementAt(itemNum);
        release(it, itemNum);
        itemInvalidated();
    }

    public void deleteAll() {
        while (items.size() > 0) {
            Item it = (Item) items.elementAt(items.size() - 1);
            items.removeElementAt(items.size() - 1);
            it.owner = null;
        }
        focused = null;
        scroll = 0;
        itemInvalidated();
    }

    public void set(int itemNum, Item item) {
        Item old = get(itemNum);
        adopt(item);
        items.setElementAt(item, itemNum);
        release(old, itemNum);
        itemInvalidated();
    }

    public Item get(int itemNum) {
        if (itemNum < 0 || itemNum >= items.size()) {
            throw new IndexOutOfBoundsException();
        }
        return (Item) items.elementAt(itemNum);
    }

    public int size() {
        return items.size();
    }

    public void setItemStateListener(ItemStateListener l) {
        stateListener = l;
    }

    private void adopt(Item item) {
        if (item == null) {
            throw new NullPointerException();
        }
        if (item.owner != null) {
            throw new IllegalStateException();
        }
        item.owner = this;
    }

    private void release(Item it, int index) {
        it.owner = null;
        if (it == focused) {
            focused = null;
            /* Move the focus to the nearest focusable item. */
            for (int i = index; i < items.size() && focused == null; i++) {
                Item c = itemAt(i);
                if (c != null && c.isFocusable()) {
                    focused = c;
                }
            }
            for (int i = index - 1; i >= 0 && focused == null; i--) {
                Item c = itemAt(i);
                if (c != null && c.isFocusable()) {
                    focused = c;
                }
            }
        }
    }

    private Item itemAt(int i) {
        try {
            return (Item) items.elementAt(i);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    void itemStateChanged(Item item) {
        ItemStateListener l = stateListener;
        if (l != null) {
            l.itemStateChanged(item);
        }
    }

    /* ---- Layout ---- */

    void itemInvalidated() {
        layoutDirty = true;
        repaintDisplayable();
    }

    private void ensureLayout() {
        int w = contentWidth();
        if (!layoutDirty && w == layoutWidth) {
            return;
        }
        layoutDirty = false;
        layoutWidth = w;
        int y = 0;
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item it = itemAt(i);
            if (it == null) {
                break;
            }
            it.y = y;
            y += it.layout(w);
        }
        total = y;
        clampScroll();
        if (focused != null && focused.owner != this) {
            focused = null;
        }
    }

    private void clampScroll() {
        int max = Math.max(0, total - contentHeight());
        if (scroll > max) {
            scroll = max;
        }
        if (scroll < 0) {
            scroll = 0;
        }
    }

    void paintContent(Graphics g, int w, int h) {
        ensureLayout();
        Item f = focused;
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item it = itemAt(i);
            if (it == null) {
                break;
            }
            int top = it.y - scroll;
            if (top >= h) {
                break;
            }
            if (top + it.height <= 0) {
                continue;
            }
            g.translate(0, top);
            it.paintItem(g, w, it == f);
            g.translate(0, -top);
        }
        if (total > h) {
            int bh = Math.max(10, h * h / total);
            int by = scroll * (h - bh) / (total - h);
            g.setColor(Theme.BORDER);
            g.fillRect(w - 3, by, 3, bh);
        }
    }

    /* ---- Focus and scrolling ---- */

    private void ensureVisible(Item it) {
        int vh = contentHeight();
        int top = it.y + it.focusTop();
        int bottom = it.y + it.focusBottom();
        if (bottom - top > vh || top < scroll) {
            scroll = top;
        } else if (bottom > scroll + vh) {
            scroll = bottom - vh;
        }
        clampScroll();
    }

    private void setFocus(Item it, int dir) {
        Item old = focused;
        if (old != null && old != it) {
            old.traverseOutItem();
        }
        focused = it;
        if (it != null) {
            it.traverseItem(dir, true);
            ensureVisible(it);
        }
        repaintDisplayable();
    }

    private Item findFocusable(Item from, int dir) {
        int n = items.size();
        int i = from == null ? (dir == Canvas.DOWN ? -1 : n) : items.indexOf(from);
        int step = dir == Canvas.DOWN ? 1 : -1;
        for (i += step; i >= 0 && i < n; i += step) {
            Item it = itemAt(i);
            if (it != null && it.isFocusable()) {
                return it;
            }
        }
        return null;
    }

    private void navigate(int dir) {
        Item f = focused;
        if (f != null && f.traverseItem(dir, false)) {
            ensureVisible(f);
            repaintDisplayable();
            return;
        }
        int vh = contentHeight();
        Item next = findFocusable(f, dir);
        boolean down = dir == Canvas.DOWN;
        /* Scroll instead of jumping when the next focusable item is well
         * beyond the visible area (long text in between). */
        boolean far = next == null
                || (down ? next.y > scroll + vh + vh / 3 : next.y + next.height < scroll - vh / 3);
        boolean canScroll = down ? scroll < total - vh : scroll > 0;
        if (far && canScroll) {
            scroll += down ? vh * 2 / 3 : -vh * 2 / 3;
            clampScroll();
            repaintDisplayable();
        } else if (next != null) {
            setFocus(next, dir);
        }
    }

    void focusItem(Item item) {
        ensureLayout();
        if (item.owner == this && item.isFocusable()) {
            setFocus(item, 0);
        } else if (item.owner == this) {
            ensureVisible(item);
            repaintDisplayable();
        }
    }

    void showNotify0() {
        ensureLayout();
        if (focused == null || focused.owner != this) {
            Item first = findFocusable(null, Canvas.DOWN);
            if (first != null) {
                focused = first;
                first.traverseItem(0, true);
            }
        }
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item it = itemAt(i);
            if (it != null) {
                it.showItem();
            }
        }
    }

    void hideNotify0() {
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item it = itemAt(i);
            if (it != null) {
                it.hideItem();
            }
        }
    }

    /* ---- Input ---- */

    boolean keyEvent(int type, int code) {
        ensureLayout();
        int action = Canvas.gameAction(code);
        boolean nav = action == Canvas.UP || action == Canvas.DOWN;
        if (nav && code < 0) {
            if (type != nanojava.Events.KEY_UP) {
                navigate(action);
            }
            return true;
        }
        Item f = focused;
        if (f != null && f.owner == this && f.itemKey(type, code)) {
            return true;
        }
        if (type == nanojava.Events.KEY_UP) {
            return false;
        }
        if (nav) {
            navigate(action);
            return true;
        }
        if (action == Canvas.FIRE && f != null && type == nanojava.Events.KEY_DOWN) {
            Command c = f.activationCommand();
            if (c != null) {
                f.fireItemCommand(c);
                return true;
            }
        }
        return false;
    }

    void pointerEvent(int type, int x, int y) {
        ensureLayout();
        int cy = y - Theme.titleHeight();
        if (type == nanojava.Events.POINTER_DOWN && (cy < 0 || cy >= contentHeight())) {
            pressed = null;
            return;
        }
        cy += scroll;
        Item hit = null;
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item it = itemAt(i);
            if (it != null && cy >= it.y && cy < it.y + it.height) {
                hit = it;
                break;
            }
        }
        if (type == nanojava.Events.POINTER_DOWN) {
            pressed = hit;
            if (hit != null && hit.isFocusable() && hit != focused) {
                setFocus(hit, 0);
            }
        }
        Item f = focused;
        if (f != null && (hit == f || (pressed == f && type != nanojava.Events.POINTER_DOWN))) {
            int bx = x - Item.PAD;
            int by = cy - f.y - Item.VPAD - f.labelHeight;
            boolean used = f.itemPointer(type, bx, by);
            if (!used && type == nanojava.Events.POINTER_UP && pressed == f && hit == f) {
                Command c = f.activationCommand();
                if (c != null) {
                    f.fireItemCommand(c);
                }
            }
        }
        if (type == nanojava.Events.POINTER_UP) {
            pressed = null;
        }
        repaintDisplayable();
    }

    /* ---- Commands of the focused item ---- */

    Vector itemCommands() {
        Item f = focused;
        return f != null && f.commands.size() > 0 ? f.commands : null;
    }

    void fireCommand(Command c) {
        Item f = focused;
        if (f != null && f.commands.contains(c)) {
            f.fireItemCommand(c);
        } else {
            super.fireCommand(c);
        }
    }
}
