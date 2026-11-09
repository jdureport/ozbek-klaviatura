package uz.yangialifbo.klaviatura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Barça tillar uçun tugmalar joylaşuvi. */
final class Layouts {

    private Layouts() {}

    // ---- Harf qatorlari (foydalanuvçi joylarini özgartira oladi) ----

    /** Özbekça (2026-yilgi lotin alifbosi): 28 harf + ʼ. */
    static final String[] UZ_ROWS = {"qşertyuioöp", "asdfgğhjkl", "zxçvbnmʼ"};
    /** Qoraqalpoqça (2016-yilgi lotin alifbosi): 32 harf; sh, ch — ikki harf bilan. */
    static final String[] KAA_ROWS = {"qwertyuúiıop", "aásdfgǵhjkl", "zxcvbnńmó"};
    static final String[] EN_ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm'"};
    static final String[] RU_ROWS = {"йцукенгшщзх", "фывапролджэ", "ячсмитьбю"};

    private static final String DIGITS = "1234567890";
    /** Gboard kabi: harflarni bosib turganda çiqadigan belgilar (qator va örin böyiça). */
    private static final String SYM0 = "%^~|[]<>{}";
    private static final String SYM1 = "@#$_&-+()";
    private static final String SYM2 = "*\"':;!?";

    private static String symAt(String sym, int i) {
        return i < sym.length() ? String.valueOf(sym.charAt(i)) : null;
    }

    static String[] defaultRows(String langId) {
        switch (langId) {
            case Lang.UZ: return UZ_ROWS.clone();
            case Lang.KAA: return KAA_ROWS.clone();
            case Lang.EN: return EN_ROWS.clone();
            case Lang.RU: return RU_ROWS.clone();
            default: return null;
        }
    }

    /** Joylaşuvini özgartiriş mumkin bölgan tillar. */
    static final String[] EDITABLE = {Lang.UZ, Lang.KAA, Lang.EN, Lang.RU};

    /** Qatorlar asl joylaşuvdagi harflarning aynan özi (faqat joyi boşqa) ekanini tekşiradi. */
    static boolean isPermutation(String[] def, String[] rows) {
        if (rows == null || rows.length != def.length) return false;
        for (int i = 0; i < def.length; i++) if (rows[i].length() != def[i].length()) return false;
        char[] a = String.join("", def).toCharArray();
        char[] b = String.join("", rows).toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    private static String letterAlt(String langId, char c) {
        if (Lang.RU.equals(langId)) {
            if (c == 'ь') return "ъ";
            if (c == 'е') return "ё";
        }
        return null;
    }

    /**
     * Harf qatorlaridan töliq joylaşuv quriş:
     * 1-qatorda raqamlar (bosib turilsa), 3-qatorda ⇧ va ⌫, pastda umumiy qator.
     */
    static List<List<Key>> letters(String langId, String[] rows, boolean numberRow) {
        float w = Lang.EN.equals(langId) ? 1.1f : 1f;
        float total = Math.max(rows[0].length(), rows[1].length()) * w;
        List<List<Key>> out = new ArrayList<>();
        if (numberRow) {
            List<Key> nr = new ArrayList<>();
            for (int i = 0; i < DIGITS.length(); i++) {
                Key k = Key.ch(String.valueOf(DIGITS.charAt(i)), null, total / DIGITS.length());
                k.small = true;
                nr.add(k);
            }
            out.add(nr);
        }
        List<Key> r0 = new ArrayList<>();
        for (int i = 0; i < rows[0].length(); i++) {
            char ch = rows[0].charAt(i);
            String alt = letterAlt(langId, ch);
            if (alt == null) {
                alt = numberRow ? symAt(SYM0, i)
                        : (i < DIGITS.length() ? String.valueOf(DIGITS.charAt(i)) : null);
            }
            r0.add(Key.ch(String.valueOf(ch), alt, w));
        }
        out.add(r0);
        List<Key> r1 = new ArrayList<>();
        for (int i = 0; i < rows[1].length(); i++) {
            char ch = rows[1].charAt(i);
            String alt = letterAlt(langId, ch);
            r1.add(Key.ch(String.valueOf(ch), alt != null ? alt : symAt(SYM1, i), w));
        }
        out.add(r1);
        int n2 = rows[2].length();
        float side = (total - n2) / 2f;
        float lw = 1f;
        if (side < 1f) {
            side = 1f;
            lw = (total - 2f) / n2;
        }
        List<Key> r2 = new ArrayList<>();
        r2.add(Key.special(Key.SHIFT, "", side));
        for (int i = 0; i < n2; i++) {
            char ch = rows[2].charAt(i);
            String alt = letterAlt(langId, ch);
            r2.add(Key.ch(String.valueOf(ch), alt != null ? alt : symAt(SYM2, i), lw));
        }
        r2.add(Key.special(Key.DELETE, "", side));
        out.add(r2);
        out.add(bottomRow("?123", total));
        return out;
    }

    /**
     * Raqam maydonlari (son, sana, PIN, SMS-kod):
     *   +  1 2 3  ⌫
     *   -  4 5 6  ␣
     *   /  7 8 9  ↵
     *   ABD , 0 .  *
     */
    static List<List<Key>> numberPad(String abcLabel) {
        float d = 1.3f;
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(Key.ch("+", "±"), Key.ch("1", null, d), Key.ch("2", null, d), Key.ch("3", null, d),
                Key.special(Key.DELETE, "", 1.2f)));
        rows.add(row(Key.ch("-", "−"), Key.ch("4", null, d), Key.ch("5", null, d), Key.ch("6", null, d),
                Key.special(Key.SPACE, "␣", 1.2f)));
        rows.add(row(Key.ch("/", ":"), Key.ch("7", null, d), Key.ch("8", null, d), Key.ch("9", null, d),
                Key.special(Key.ENTER, "", 1.2f)));
        rows.add(row(Key.special(Key.TO_LETTERS, abcLabel, 1f), Key.ch(",", null, d), Key.ch("0", null, d),
                Key.ch(".", null, d), Key.ch("*", "#", 1.2f)));
        return rows;
    }

    /**
     * Telefon raqami maydonlari:
     *   (  1 2 3  ⌫
     *   )  4 5 6  ␣
     *   -  7 8 9  ↵
     *   ABD * 0 #  +
     */
    static List<List<Key>> phonePad(String abcLabel) {
        float d = 1.3f;
        List<List<Key>> rows = new ArrayList<>();
        rows.add(row(Key.ch("("), Key.ch("1", null, d), Key.ch("2", null, d), Key.ch("3", null, d),
                Key.special(Key.DELETE, "", 1.2f)));
        rows.add(row(Key.ch(")"), Key.ch("4", null, d), Key.ch("5", null, d), Key.ch("6", null, d),
                Key.special(Key.SPACE, "␣", 1.2f)));
        rows.add(row(Key.ch("-"), Key.ch("7", null, d), Key.ch("8", null, d), Key.ch("9", null, d),
                Key.special(Key.ENTER, "", 1.2f)));
        rows.add(row(Key.special(Key.TO_LETTERS, abcLabel, 1f), Key.ch("*", null, d), Key.ch("0", "+", d),
                Key.ch("#", null, d), Key.ch("+", null, 1.2f)));
        return rows;
    }

    private static List<Key> charRow(String chars, String alts, float w) {
        List<Key> r = new ArrayList<>();
        for (int i = 0; i < chars.length(); i++) {
            char a = i < alts.length() ? alts.charAt(i) : ' ';
            r.add(Key.ch(String.valueOf(chars.charAt(i)), a == ' ' ? null : String.valueOf(a), w));
        }
        return r;
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
        rows.add(charRow("1234567890", "", w));
        rows.add(charRow("@#$%&-+()/", "~№€^|_=[]\\", w));
        List<Key> r2 = charRow("*\"':;!?«»", "`{}<>¡¿“”", 1f);
        r2.add(Key.special(Key.DELETE, "", 2f));
        rows.add(r2);
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
