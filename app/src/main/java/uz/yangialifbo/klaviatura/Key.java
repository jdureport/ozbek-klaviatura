package uz.yangialifbo.klaviatura;

import android.graphics.RectF;

/** Klaviaturadagi bitta tugma. */
public final class Key {
    public static final int CHAR = 0;
    public static final int SHIFT = 1;
    public static final int DELETE = 2;
    public static final int TO_SYMBOLS = 3;
    public static final int TO_LETTERS = 4;
    public static final int SPACE = 5;
    public static final int ENTER = 6;

    public final int type;
    /** Tugmada körinadigan va yoziladigan matn. */
    public final String text;
    /** Bosib turilganda yoziladigan belgi (bölmasa null). */
    public final String alt;
    /** Nisbiy kenglik (1 = oddiy harf tugmasi). */
    public final float width;
    /** Tugma egallagan katak (bosiş uçun), KlaviaturaView hisoblaydi. */
    public final RectF rect = new RectF();

    public Key(int type, String text, String alt, float width) {
        this.type = type;
        this.text = text;
        this.alt = alt;
        this.width = width;
    }

    static Key ch(String text) {
        return new Key(CHAR, text, null, 1f);
    }

    static Key ch(String text, String alt) {
        return new Key(CHAR, text, alt, 1f);
    }

    static Key ch(String text, String alt, float width) {
        return new Key(CHAR, text, alt, width);
    }

    static Key special(int type, String text, float width) {
        return new Key(type, text, null, width);
    }
}
