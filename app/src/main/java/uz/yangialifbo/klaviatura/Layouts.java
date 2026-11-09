package uz.yangialifbo.klaviatura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tugmalar joylaşuvi.
 *
 * Harflar: özbek tilining 2026-yilgi lotin alifbosi — 28 harf va ʼ belgisi.
 *   q ş e r t y u i o ö p
 *    a s d f g ğ h j k l
 *   ⇧ z x ç v b n m ʼ ⌫
 *   ?123  ,  [ böş joy ]  .  ↵
 */
final class Layouts {

    private Layouts() {}

    static List<List<Key>> letters() {
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
        rows.add(bottomRow(Key.special(Key.TO_SYMBOLS, "?123", 1.5f)));
        return rows;
    }

    static List<List<Key>> symbols() {
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
        rows.add(bottomRow(Key.special(Key.TO_LETTERS, "ABD", 1.5f)));
        return rows;
    }

    private static List<Key> bottomRow(Key layerKey) {
        return row(
                layerKey,
                Key.ch(",", "-"),
                Key.special(Key.SPACE, " ", 5.5f),
                Key.ch(".", "?"),
                Key.special(Key.ENTER, "", 2f));
    }

    private static List<Key> row(Key... keys) {
        return new ArrayList<>(Arrays.asList(keys));
    }
}
