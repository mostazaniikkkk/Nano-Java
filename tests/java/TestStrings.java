// String, StringBuffer and number formatting/parsing.
public class TestStrings {
    public static void main(String[] args) throws Exception {
        String s = "  Hello, World  ";
        System.out.println("[" + s.trim() + "]");
        System.out.println(s.trim().toUpperCase() + " " + s.trim().toLowerCase());
        System.out.println(s.indexOf('o') + " " + s.lastIndexOf('o') + " " + s.indexOf("World") + " " + s.indexOf("xyz"));
        System.out.println(s.trim().substring(7) + "|" + s.trim().substring(0, 5));
        System.out.println("abc".compareTo("abd") + " " + "b".compareTo("a") + " " + "ab".compareTo("abc"));
        System.out.println("Hello".equalsIgnoreCase("hELLO") + " " + "abc".startsWith("ab") + " " + "abc".endsWith("bc"));
        System.out.println("a-b-c".replace('-', '+') + " " + "x".concat("y") + " " + String.valueOf(3.5f));
        System.out.println("char " + "hey".charAt(1) + " " + "hey".toCharArray().length);
        try {
            "abc".charAt(5);
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println("SIOOBE");
        }

        StringBuffer sb = new StringBuffer();
        sb.append(1).append('a').append(true).append(2.5).append(10L).append((Object) null);
        System.out.println(sb.toString() + " " + sb.length());
        sb.insert(0, "start:").reverse();
        System.out.println(sb);
        sb.setLength(3);
        sb.setCharAt(0, 'Z');
        System.out.println(sb + " " + sb.charAt(1));
        sb.delete(0, 1).deleteCharAt(0);
        System.out.println("[" + sb + "]");

        System.out.println(Integer.parseInt("-12345") + " " + Integer.parseInt("ff", 16) + " " + Integer.toHexString(-1));
        System.out.println(Integer.toString(255, 2) + " " + Integer.toOctalString(8) + " " + Integer.toBinaryString(10));
        System.out.println(Long.parseLong("-9223372036854775808") + " " + Long.toString(Long.MIN_VALUE, 16));
        try {
            Integer.parseInt("2147483648");
        } catch (NumberFormatException e) {
            System.out.println("NFE " + e.getMessage());
        }
        try {
            Integer.parseInt("12a");
        } catch (NumberFormatException e) {
            System.out.println("NFE " + e.getMessage());
        }

        double[] ds = {0.1, 1.0, -2.5, 100, 1234567.0, 12345678.0, 0.001, 0.0001, 1e-10,
                       3.141592653589793, 1.0 / 3, 2.0 / 3, 1e21, -0.0, Double.MAX_VALUE, Double.MIN_VALUE};
        for (int i = 0; i < ds.length; i++) {
            System.out.println(ds[i]);
        }
        float[] fs = {0.1f, 1.0f, 1.0f / 3, 16777216f, 1e-5f, 3.4e38f};
        for (int i = 0; i < fs.length; i++) {
            System.out.println(fs[i]);
        }
        System.out.println(Double.parseDouble("1.5e3") + " " + Float.parseFloat(" 2.25f ") + " " + Double.parseDouble("-.5"));
        System.out.println(Double.isNaN(Double.parseDouble("NaN")) + " " + Double.parseDouble("-Infinity"));
        try {
            Double.parseDouble("1.2.3");
        } catch (NumberFormatException e) {
            System.out.println("NFE double");
        }
        System.out.println(Float.floatToIntBits(1.0f) + " " + Double.doubleToLongBits(-2.0));
        System.out.println(new Integer(5).equals(new Integer(5)) + " " + new Long(7).hashCode() + " " + new Character('x'));
        System.out.println(Character.isDigit('7') + " " + Character.digit('z', 36) + " " + Character.toUpperCase('q'));

        byte[] utf = "héllo €".getBytes("UTF-8");
        System.out.println(utf.length + " " + new String(utf, "UTF-8").length());
        byte[] latin = "hé".getBytes("ISO-8859-1");
        System.out.println(latin.length + " " + (latin[1] & 0xff));
    }
}
