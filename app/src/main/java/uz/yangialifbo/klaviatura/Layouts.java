package uz.yangialifbo.klaviatura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Barça tillar uçun tugmalar joylaşuvi. */
final class Layouts {

    private Layouts() {}

    /**
     * Özbekça (2026-yilgi lotin alifbosi): 28 harf + ʼ.
     *   q ş e r t y u i o ö p
     *    a s d f g ğ h j k l
     *   ⇧ z x ç v b n m ʼ ⌫
     */
    static List<List<Key>> uzbek() {
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(
                Key.ch("q", "1"), Key.ch("ş", "2"), Key.ch("e", "3"), Key.ch("r", "4"),
                Key.ch("t", "5"), Key.ch("y", "6"), Key.ch("u", "7"), Key.ch("i", "8"),
                Key.ch("o", "9"), Key.ch("ö", "0"), Key.ch("p")));
        rows.add(row(
                Key.ch("a"), Key.ch("s"), Key.ch("d"), Key.ch("f"), Key.ch("g"),
                Key.ch("ğ"), Key.ch("h"), Key.ch("j"), Key.ch("k"), Key.ch("l")));
        rows.add(row(
                Key.special(Key.SHIFT, "", 1.5f),
                Key.ch("z"), Key.ch("x"), Key.ch("ç"), Key.ch("v"),
                Key.ch("b"), Key.ch("n"), Key.ch("m"), Key.ch("ʼ"),
                Key.special(Key.DELETE, "", 1.5f)));
        rows.add(bottomRow("?123", 11));
        return rows;
    }

    /**
     * Qoraqalpoqça (2016-yilgi lotin alifbosi): 32 harf, sh va ch — ikki harf bilan.
     *   q w e r t y u ú i ı o p
     *    a á s d f g ǵ h j k l
     *   ⇧ z x c v b n ń m ó ⌫
     */
    static List<List<Key>> karakalpak() {
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(
                Key.ch("q", "1"), Key.ch("w", "2"), Key.ch("e", "3"), Key.ch("r", "4"),
                Key.ch("t", "5"), Key.ch("y", "6"), Key.ch("u", "7"), Key.ch("ú"),
                Key.ch("i", "8"), Key.ch("ı"), Key.ch("o", "9"), Key.ch("p", "0")));
        rows.add(row(
                Key.ch("a"), Key.ch("á"), Key.ch("s"), Key.ch("d"), Key.ch("f"), Key.ch("g"),
                Key.ch("ǵ"), Key.ch("h"), Key.ch("j"), Key.ch("k"), Key.ch("l")));
        rows.add(row(
                Key.special(Key.SHIFT, "", 1.5f),
                Key.ch("z"), Key.ch("x"), Key.ch("c"), Key.ch("v"), Key.ch("b"),
                Key.ch("n"), Key.ch("ń"), Key.ch("m"), Key.ch("ó"),
                Key.special(Key.DELETE, "", 1.5f)));
        rows.add(bottomRow("?123", 12));
        return rows;
    }

    static List<List<Key>> english() {
        float w = 1.1f;
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(
                Key.ch("q", "1", w), Key.ch("w", "2", w), Key.ch("e", "3", w), Key.ch("r", "4", w),
                Key.ch("t", "5", w), Key.ch("y", "6", w), Key.ch("u", "7", w), Key.ch("i", "8", w),
                Key.ch("o", "9", w), Key.ch("p", "0", w)));
        rows.add(row(
                Key.ch("a", null, w), Key.ch("s", null, w), Key.ch("d", null, w), Key.ch("f", null, w),
                Key.ch("g", null, w), Key.ch("h", null, w), Key.ch("j", null, w), Key.ch("k", null, w),
                Key.ch("l", null, w)));
        rows.add(row(
                Key.special(Key.SHIFT, "", 1.5f),
                Key.ch("z"), Key.ch("x"), Key.ch("c"), Key.ch("v"),
                Key.ch("b"), Key.ch("n"), Key.ch("m"), Key.ch("'"),
                Key.special(Key.DELETE, "", 1.5f)));
        rows.add(bottomRow("?123", 11));
        return rows;
    }

    static List<List<Key>> russian() {
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(
                Key.ch("й", "1"), Key.ch("ц", "2"), Key.ch("у", "3"), Key.ch("к", "4"),
                Key.ch("е", "5"), Key.ch("н", "6"), Key.ch("г", "7"), Key.ch("ш", "8"),
                Key.ch("щ", "9"), Key.ch("з", "0"), Key.ch("х")));
        rows.add(row(
                Key.ch("ф"), Key.ch("ы"), Key.ch("в"), Key.ch("а"), Key.ch("п"), Key.ch("р"),
                Key.ch("о"), Key.ch("л"), Key.ch("д"), Key.ch("ж"), Key.ch("э")));
        rows.add(row(
                Key.special(Key.SHIFT, "", 1f),
                Key.ch("я"), Key.ch("ч"), Key.ch("с"), Key.ch("м"), Key.ch("и"),
                Key.ch("т"), Key.ch("ь", "ъ"), Key.ch("б"), Key.ch("ю", "ё"),
                Key.special(Key.DELETE, "", 1f)));
        rows.add(bottomRow("?123", 11));
        return rows;
    }

    /**
     * Özbekça T9: 12 tugma.
     *   1 ʼ.,  2 abç  3 def   ⌫
     *   4 ghiğ 5 jkl  6 mnoö  ⇧
     *   7 pqrsş 8 tuv 9 xyz   ↵
     *   ?123   🌐    ␣ 0     T9
     */
    static List<List<Key>> uzT9() {
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(Key.t9("1", "ʼ.,?!-"), Key.t9("2", "abç"), Key.t9("3", "def"),
                Key.special(Key.DELETE, "", 1f)));
        rows.add(row(Key.t9("4", "ghiğ"), Key.t9("5", "jkl"), Key.t9("6", "mnoö"),
                Key.special(Key.SHIFT, "", 1f)));
        rows.add(row(Key.t9("7", "pqrsş"), Key.t9("8", "tuv"), Key.t9("9", "xyz"),
                Key.special(Key.ENTER, "", 1f)));
        rows.add(row(Key.special(Key.TO_SYMBOLS, "?123", 1f), Key.special(Key.LANG, "", 1f),
                Key.special(Key.SPACE, "0", 1f), Key.special(Key.T9MODE, "T9", 1f)));
        return rows;
    }

    /** T9 dagi raqam → harflar (T9 luğati uçun). */
    static final String[] T9_LETTERS = {
            "", "ʼ", "abç", "def", "ghiğ", "jkl", "mnoö", "pqrsş", "tuv", "xyz"
    };

    static List<List<Key>> symbols(String abcLabel) {
        List<List<Key>> rows = new ArrayList<>();
        float w = 1.1f;
        rows.add(row(
                Key.ch("1", null, w), Key.ch("2", null, w), Key.ch("3", null, w), Key.ch("4", null, w),
                Key.ch("5", null, w), Key.ch("6", null, w), Key.ch("7", null, w), Key.ch("8", null, w),
                Key.ch("9", null, w), Key.ch("0", null, w)));
        rows.add(row(
                Key.ch("@", "~", w), Key.ch("#", "№", w), Key.ch("$", "€", w), Key.ch("%", "^", w),
                Key.ch("&", "|", w), Key.ch("-", "_", w), Key.ch("+", "=", w), Key.ch("(", "[", w),
                Key.ch(")", "]", w), Key.ch("/", "\\", w)));
        rows.add(row(
                Key.ch("*", "`"), Key.ch("\"", "{"), Key.ch("'", "}"), Key.ch(":", "<"),
                Key.ch(";", ">"), Key.ch("!", "¡"), Key.ch("?", "¿"), Key.ch("«", "“"),
                Key.ch("»", "”"),
                Key.special(Key.DELETE, "", 2f)));
        List<Key> bottom = bottomRow(abcLabel, 11);
        bottom.set(0, Key.special(Key.TO_LETTERS, abcLabel, 1.25f));
        rows.add(bottom);
        return rows;
    }

    /** Pastki qator: ?123  🌐  ,  [böş joy]  .  ↵ */
    private static List<Key> bottomRow(String layerLabel, float total) {
        return row(
                Key.special(Key.TO_SYMBOLS, layerLabel, 1.25f),
                Key.special(Key.LANG, "", 1f),
                Key.ch(",", "-"),
                Key.special(Key.SPACE, " ", total - 6.25f),
                Key.ch(".", "?"),
                Key.special(Key.ENTER, "", 2f));
    }

    private static List<Key> row(Key... keys) {
        return new ArrayList<>(Arrays.asList(keys));
    }
}
