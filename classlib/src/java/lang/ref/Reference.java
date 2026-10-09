package java.lang.ref;

public abstract class Reference {
    /* The collector does not trace this field for weak references. */
    private Object referent;

    Reference(Object referent) {
        this.referent = referent;
    }

    public Object get() {
        return referent;
    }

    public void clear() {
        referent = null;
    }
}
