package javax.microedition.lcdui;

public class AlertType {
    public static final AlertType INFO = new AlertType(1);
    public static final AlertType WARNING = new AlertType(2);
    public static final AlertType ERROR = new AlertType(3);
    public static final AlertType ALARM = new AlertType(4);
    public static final AlertType CONFIRMATION = new AlertType(5);

    /* Which icon Alert draws; 0 for application subclasses. */
    final int kind;

    protected AlertType() {
        kind = 0;
    }

    private AlertType(int kind) {
        this.kind = kind;
    }

    public boolean playSound(Display display) {
        if (display == null) {
            throw new NullPointerException();
        }
        return false;
    }
}
