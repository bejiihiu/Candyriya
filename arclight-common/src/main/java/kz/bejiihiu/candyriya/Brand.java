package kz.bejiihiu.candyriya;

/**
 * Candyriya brand info.
 * Fork of Arclight by bejiihiu.
 */
public final class Brand {

    public static final String NAME = "Candyriya";
    public static final String AUTHOR = "bejiihiu";
    public static final String BASED_ON = "Arclight";
    public static final String HOMEPAGE = "https://github.com/bejiihiu/Candyriya";

    private Brand() {
    }

    public static String getServerName() {
        return NAME;
    }

    public static String getBrandString() {
        return NAME + " (based on " + BASED_ON + ")";
    }
}
