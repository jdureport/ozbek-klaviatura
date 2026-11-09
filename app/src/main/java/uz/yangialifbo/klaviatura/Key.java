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
    /** Tilni almaştiriş (🌐). */
    public static final int LANG = 7;
    /** T9 raqam tugmasi: text — raqam, alt — harflar. */
    public static final int T9 = 8;
    /** T9 rejimini almaştiriş (T9 ↔ abc). */
    public static final int T9MODE = 9;
    /** Takliflar qatoridagi katak. */
    public static final int SUGGESTION = 10;
    /** Kod rejimi: Tab va kursorni çapga / öngga suriş. */
    public static final int TAB = 11;
    public static final int LEFT = 12;
    public static final int RIGHT = 13;

    public final int type;
    /** Tugmada körinadigan va yoziladigan matn. */
    public final String text;
    /** Bosib turilganda yoziladigan belgi (T9 da — harflar), bölmasa null. */
    public final String alt;
    /** Nisbiy kenglik (1 = oddiy harf tugmasi). */
    public final float width;
    /** Tugma egallagan katak (bosiş uçun), KlaviaturaView hisoblaydi. */
    public final RectF rect = new RectF();
    /** Takliflar katagining tartib raqami. */
    public int index;

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

    static Key t9(String digit, String letters) {
        return new Key(T9, digit, letters, 1f);
    }
}
