package ui;

import java.io.IOException;
import java.util.Date;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

/*
 * Exercises the high-level UI: a Form with most item types, a List, an
 * Alert and a TextBox. Every observable change is printed so scripted key
 * presses can be checked from the output.
 */
public class UIMIDlet extends MIDlet implements CommandListener, ItemStateListener, ItemCommandListener {
    private Display display;
    private Form form;
    private List list;
    private TextBox box;
    private TextField name, age;
    private ChoiceGroup color, extras, size;
    private Gauge volume;
    private DateField date;
    private StringItem listButton, alertButton, boxButton;
    private final Command exit = new Command("Exit", Command.EXIT, 1);
    private final Command back = new Command("Back", Command.BACK, 1);
    private final Command ok = new Command("OK", Command.OK, 1);
    private final Command open = new Command("Open", Command.ITEM, 1);
    private final Command dump = new Command("Dump", Command.SCREEN, 2);

    static class Swatch extends CustomItem {
        int presses;

        Swatch() {
            super("Custom");
        }

        protected int getMinContentWidth() {
            return 40;
        }

        protected int getMinContentHeight() {
            return 20;
        }

        protected int getPrefContentWidth(int h) {
            return 120;
        }

        protected int getPrefContentHeight(int w) {
            return 24;
        }

        protected void paint(Graphics g, int w, int h) {
            g.setColor(0x40A060);
            g.fillRect(0, 0, w, h);
            g.setColor(0xFFFFFF);
            g.drawString("presses " + presses, 4, 4, Graphics.TOP | Graphics.LEFT);
        }

        protected void keyPressed(int keyCode) {
            if (getGameAction(keyCode) == Canvas.FIRE) {
                presses++;
                repaint();
                notifyStateChanged();
            }
        }
    }

    private Swatch swatch;

    protected void startApp() {
        if (display != null) {
            return;
        }
        display = Display.getDisplay(this);
        Image icon = null;
        try {
            icon = Image.createImage("/icon.png");
        } catch (IOException e) {
            System.out.println("no icon");
        }

        form = new Form("UI Test");
        form.append(new StringItem("Intro", "This form exercises the MIDP high-level UI on Nano Java."));
        name = new TextField("Name", "", 20, TextField.ANY);
        age = new TextField("Age", "", 3, TextField.NUMERIC);
        color = new ChoiceGroup("Color", Choice.EXCLUSIVE, new String[] {"Red", "Green", "Blue"}, null);
        extras = new ChoiceGroup("Extras", Choice.MULTIPLE, new String[] {"Cheese", "Bacon"}, null);
        size = new ChoiceGroup("Size", Choice.POPUP, new String[] {"Small", "Medium", "Large"}, null);
        volume = new Gauge("Volume", true, 10, 5);
        date = new DateField("Date", DateField.DATE_TIME);
        date.setDate(new Date(0));
        listButton = new StringItem(null, "Open list", Item.BUTTON);
        listButton.setDefaultCommand(open);
        listButton.setItemCommandListener(this);
        alertButton = new StringItem(null, "Show alert", Item.BUTTON);
        alertButton.setDefaultCommand(open);
        alertButton.setItemCommandListener(this);
        boxButton = new StringItem(null, "Text box", Item.HYPERLINK);
        boxButton.setDefaultCommand(open);
        boxButton.setItemCommandListener(this);
        swatch = new Swatch();

        form.append(name);
        form.append(age);
        form.append(color);
        form.append(extras);
        form.append(size);
        form.append(volume);
        form.append(new Gauge("Progress", false, 100, 40));
        form.append(date);
        form.append(listButton);
        form.append(alertButton);
        form.append(boxButton);
        form.append(new Spacer(10, 6));
        if (icon != null) {
            form.append(new ImageItem("Image", icon, Item.LAYOUT_CENTER, "icon"));
        }
        form.append(swatch);
        form.addCommand(exit);
        form.addCommand(dump);
        form.setCommandListener(this);
        form.setItemStateListener(this);

        list = new List("Pick one", List.IMPLICIT);
        list.append("Apples", icon);
        list.append("Bananas", icon);
        list.append("Cherries", null);
        for (int i = 4; i <= 12; i++) {
            list.append("Item " + i, null);
        }
        list.addCommand(back);
        list.setCommandListener(this);

        box = new TextBox("Notes", "hi", 100, TextField.ANY);
        box.addCommand(ok);
        box.setCommandListener(this);

        selfTest();
        display.setCurrent(form);
        System.out.println("form shown");
    }

    /* Model checks that need no input (for tests/run.sh). */
    private void selfTest() {
        TextField t = new TextField("n", "12", 4, TextField.NUMERIC);
        try {
            t.setString("abc");
            System.out.println("numeric accepted letters");
        } catch (IllegalArgumentException e) {
            System.out.println("numeric rejects letters");
        }
        try {
            t.setString("12345");
            System.out.println("maxSize not enforced");
        } catch (IllegalArgumentException e) {
            System.out.println("maxSize enforced");
        }
        t.insert("9", 1);
        t.delete(0, 1);
        char[] chars = new char[4];
        int n = t.getChars(chars);
        System.out.println("textfield " + t.getString() + " size=" + t.size() + " chars=" + new String(chars, 0, n)
                + " max=" + t.setMaxSize(2) + " now=" + t.getString());

        ChoiceGroup cg = new ChoiceGroup("c", Choice.EXCLUSIVE);
        cg.append("a", null);
        cg.append("b", null);
        cg.append("c", null);
        cg.setSelectedIndex(2, true);
        cg.delete(2);
        System.out.println("exclusive after delete selected=" + cg.getSelectedIndex() + " size=" + cg.size());

        List l = new List("l", Choice.MULTIPLE, new String[] {"x", "y", "z"}, null);
        l.setSelectedFlags(new boolean[] {true, false, true});
        boolean[] f = new boolean[3];
        System.out.println("multiple count=" + l.getSelectedFlags(f) + " idx=" + l.getSelectedIndex());

        Gauge g = new Gauge(null, false, 10, 99);
        System.out.println("gauge clamped=" + g.getValue());

        Alert a = new Alert("a");
        System.out.println("alert timeout=" + a.getTimeout() + " forever=" + Alert.FOREVER);

        Form fm = new Form("f");
        StringItem si = new StringItem("s", "t");
        fm.append(si);
        try {
            new Form("g").append(si);
            System.out.println("item shared");
        } catch (IllegalStateException e) {
            System.out.println("item owned once");
        }
        fm.delete(0);
        System.out.println("form size=" + fm.size());
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }

    private void dumpState() {
        boolean[] flags = new boolean[2];
        extras.getSelectedFlags(flags);
        System.out.println("state name=" + name.getString() + " age=" + age.getString()
                + " color=" + color.getSelectedIndex() + " extras=" + flags[0] + "," + flags[1]
                + " size=" + size.getString(size.getSelectedIndex()) + " volume=" + volume.getValue()
                + " custom=" + swatch.presses + " box=" + box.getString());
    }

    public void itemStateChanged(Item item) {
        String v;
        if (item instanceof TextField) {
            v = ((TextField) item).getString();
        } else if (item instanceof ChoiceGroup) {
            ChoiceGroup c = (ChoiceGroup) item;
            v = String.valueOf(c.getSelectedIndex());
            if (c == extras) {
                v = c.isSelected(0) + "," + c.isSelected(1);
            }
        } else if (item instanceof Gauge) {
            v = String.valueOf(((Gauge) item).getValue());
        } else if (item instanceof DateField) {
            v = String.valueOf(((DateField) item).getDate().getTime());
        } else {
            v = "?";
        }
        System.out.println("changed " + item.getLabel() + " = " + v);
    }

    public void commandAction(Command c, Item item) {
        if (item == listButton) {
            System.out.println("open list");
            display.setCurrent(list);
        } else if (item == alertButton) {
            System.out.println("open alert");
            Alert a = new Alert("Hello", "This alert dismisses itself after one second.", null, AlertType.INFO);
            a.setTimeout(1000);
            display.setCurrent(a, form);
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1300);
                    } catch (InterruptedException e) {
                    }
                    System.out.println("after alert form shown=" + form.isShown());
                }
            }.start();
        } else if (item == boxButton) {
            System.out.println("open textbox");
            display.setCurrent(box);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == List.SELECT_COMMAND) {
            System.out.println("list select " + list.getSelectedIndex() + " " + list.getString(list.getSelectedIndex()));
            display.setCurrent(form);
        } else if (c == back || c == ok) {
            System.out.println("back from " + d.getTitle());
            if (d == box) {
                System.out.println("box=" + box.getString());
            }
            display.setCurrent(form);
        } else if (c == dump) {
            dumpState();
        } else if (c == exit) {
            dumpState();
            notifyDestroyed();
        }
    }
}
