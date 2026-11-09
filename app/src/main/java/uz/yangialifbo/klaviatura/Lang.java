package uz.yangialifbo.klaviatura;

import android.content.Context;

import java.util.List;
import java.util.Locale;

/** Klaviatura tili: joylaşuv, luğat va harflarning katta-kiçikligi. */
final class Lang {

    static final String UZ = "uz";
    static final String UZ_T9 = "uz_t9";
    static final String KAA = "kaa";
    static final String EN = "en";
    static final String RU = "ru";

    /** Tillar tartibi (🌐 tugmasi şu tartibda almaştiradi). */
    static final String[] ALL = {UZ, UZ_T9, KAA, EN, RU};

    final String id;
    /** Böş joy tugmasidagi nom. */
    final String label;
    /** Sozlamalardagi nom. */
    final String settingsName;
    /** Belgilardan harflarga qaytiş tugmasi. */
    final String abc;
    /** Luğat fayli (assets/dict/...); null — luğatsiz. */
    final String dict;
    final boolean t9;
    final List<List<Key>> letters;
    final List<List<Key>> symbols;
    final List<List<Key>> numberPad;
    final List<List<Key>> phonePad;

    private Lang(String id, String label, String settingsName, String abc, String dict,
                 boolean t9, List<List<Key>> letters) {
        this.id = id;
        this.label = label;
        this.settingsName = settingsName;
        this.abc = abc;
        this.dict = dict;
        this.t9 = t9;
        this.letters = letters;
        this.symbols = Layouts.symbols(abc);
        String letterLabel = t9 ? "T9" : abc;
        this.numberPad = Layouts.numberPad(letterLabel);
        this.phonePad = Layouts.phonePad(letterLabel);
    }

    static Lang create(Context c, String id) {
        switch (id) {
            case UZ_T9:
                return new Lang(UZ_T9, "Özbek T9", "Özbekça T9 (12 tugma)", "T9", "uz", true, Layouts.uzT9());
            case KAA:
                return new Lang(KAA, "Qaraqalpaqsha", "Qoraqalpoqça", "ABD", "kaa", false,
                        Layouts.letters(KAA, Prefs.layoutRows(c, KAA)));
            case EN:
                return new Lang(EN, "English", "Inglizça", "ABC", "en", false,
                        Layouts.letters(EN, Prefs.layoutRows(c, EN)));
            case RU:
                return new Lang(RU, "Русский", "Rusça", "АБВ", "ru", false,
                        Layouts.letters(RU, Prefs.layoutRows(c, RU)));
            case UZ:
            default:
                return new Lang(UZ, "Özbek", "Özbekça", "ABD", "uz", false,
                        Layouts.letters(UZ, Prefs.layoutRows(c, UZ)));
        }
    }

    static String settingsName(String id) {
        switch (id) {
            case UZ_T9: return "Özbekça T9 (12 tugma)";
            case KAA: return "Qoraqalpoqça";
            case EN: return "Inglizça";
            case RU: return "Rusça";
            default: return "Özbekça";
        }
    }

    // ---- Katta / kiçik harflar ----
    // Qoraqalpoq alifbosida: i ↔ I, ı ↔ Í.

    String upper(String s) {
        if (!KAA.equals(id)) return s.toUpperCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c == 'ı' ? 'Í' : Character.toUpperCase(c));
        }
        return b.toString();
    }

    String lower(String s) {
        if (!KAA.equals(id)) return s.toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c == 'Í' ? 'ı' : Character.toLowerCase(c));
        }
        return b.toString();
    }

    String capitalize(String s) {
        if (s.isEmpty()) return s;
        return upper(s.substring(0, 1)) + s.substring(1);
    }

    /** Yozilgan söz şakliga moslaş: "Sal" → "Salom", "SAL" → "SALOM". */
    String matchCase(String typed, String word) {
        if (typed.isEmpty()) return word;
        boolean firstUpper = !typed.substring(0, 1).equals(lower(typed.substring(0, 1)));
        boolean allUpper = typed.length() > 1 && typed.equals(upper(typed));
        if (allUpper) return upper(word);
        if (firstUpper) return capitalize(word);
        return word;
    }

    boolean isWordChar(char c) {
        if (Character.isLetter(c)) return true;
        if (c == 'ʼ') return UZ.equals(id) || UZ_T9.equals(id);
        if (c == '\'') return EN.equals(id);
        return false;
    }
}
