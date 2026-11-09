package uz.yangialifbo.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Ilova sozlamalari. */
final class Prefs {

    private static final String FILE = "sozlamalar";
    private static final String KEY_CURRENT = "joriy_til";
    private static final String KEY_SUGGESTIONS = "takliflar";
    private static final String KEY_T9_PREDICTIVE = "t9_lugat";

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean langEnabled(Context c, String id) {
        return get(c).getBoolean("til_" + id, true);
    }

    static void setLangEnabled(Context c, String id, boolean on) {
        get(c).edit().putBoolean("til_" + id, on).apply();
    }

    static List<String> enabledLangs(Context c) {
        List<String> out = new ArrayList<>();
        for (String id : Lang.ALL) if (langEnabled(c, id)) out.add(id);
        if (out.isEmpty()) out.add(Lang.UZ);
        return out;
    }

    static String currentLang(Context c) {
        return get(c).getString(KEY_CURRENT, Lang.UZ);
    }

    static void setCurrentLang(Context c, String id) {
        get(c).edit().putString(KEY_CURRENT, id).apply();
    }

    static boolean suggestions(Context c) {
        return get(c).getBoolean(KEY_SUGGESTIONS, true);
    }

    static void setSuggestions(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_SUGGESTIONS, on).apply();
    }

    static boolean t9Predictive(Context c) {
        return get(c).getBoolean(KEY_T9_PREDICTIVE, true);
    }

    static void setT9Predictive(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_T9_PREDICTIVE, on).apply();
    }

    // ---- Özgarişlar hisoblagiçi (ranglar va joylaşuv özgarsa, klaviatura qayta quriladi) ----

    static int version(Context c) {
        return get(c).getInt("versiya", 0);
    }

    static void bumpVersion(Context c) {
        SharedPreferences p = get(c);
        p.edit().putInt("versiya", p.getInt("versiya", 0) + 1).apply();
    }

    // ---- Foydalanuvçi joylaşuvi ----

    /** Til uçun harf qatorlari; özgartirilmagan yoki buzilgan bölsa — asl joylaşuv. */
    static String[] layoutRows(Context c, String langId) {
        String[] def = Layouts.defaultRows(langId);
        if (def == null) return null;
        String saved = get(c).getString("joylasuv_" + langId, null);
        if (saved == null) return def;
        String[] rows = saved.split("\\|", -1);
        if (!Layouts.isPermutation(def, rows)) return def;
        return rows;
    }

    static void setLayoutRows(Context c, String langId, String[] rows) {
        get(c).edit().putString("joylasuv_" + langId, String.join("|", rows)).apply();
        bumpVersion(c);
    }

    static void resetLayout(Context c, String langId) {
        get(c).edit().remove("joylasuv_" + langId).apply();
        bumpVersion(c);
    }

    // ---- Ölçamlar ----

    static final String[] HEIGHT_IDS = {"ixcham", "oddiy", "baland"};
    static final String[] HEIGHT_NAMES = {"Ixçam", "Oddiy", "Baland"};
    private static final float[] HEIGHT_SCALES = {0.86f, 1f, 1.14f};
    static final int[] EXTRA_BOTTOM = {0, 8, 16, 24};

    static String height(Context c) {
        return get(c).getString("balandlik", "oddiy");
    }

    static float heightScale(Context c) {
        String h = height(c);
        for (int i = 0; i < HEIGHT_IDS.length; i++) if (HEIGHT_IDS[i].equals(h)) return HEIGHT_SCALES[i];
        return 1f;
    }

    static void setHeight(Context c, String id) {
        get(c).edit().putString("balandlik", id).apply();
        bumpVersion(c);
    }

    /** Pastdan qöşimça böşliq (dp) — tizim tugmalari ustma-ust tuşsa. */
    static int extraBottom(Context c) {
        return get(c).getInt("pastki_bosliq", 0);
    }

    static void setExtraBottom(Context c, int dp) {
        get(c).edit().putInt("pastki_bosliq", dp).apply();
        bumpVersion(c);
    }

    static boolean keyPreview(Context c) {
        return get(c).getBoolean("harf_korinishi", true);
    }

    static void setKeyPreview(Context c, boolean on) {
        get(c).edit().putBoolean("harf_korinishi", on).apply();
        bumpVersion(c);
    }

    /** Harflar ustida alohida raqamlar qatori (Gboard kabi). */
    static boolean numberRow(Context c) {
        return get(c).getBoolean("raqamlar_qatori", true);
    }

    static void setNumberRow(Context c, boolean on) {
        get(c).edit().putBoolean("raqamlar_qatori", on).apply();
        bumpVersion(c);
    }

    // ---- Tugmalar oralığı ----

    static final String[] GAP_NAMES = {"Tor", "Oddiy", "Keng"};
    /** {gorizontal, vertikal} dp. */
    private static final float[][] GAPS = {{3f, 5f}, {4.5f, 7f}, {6f, 9f}};

    static int gap(Context c) {
        return get(c).getInt("oraliq", 1);
    }

    static float[] gapValues(Context c) {
        int g = gap(c);
        return GAPS[Math.max(0, Math.min(GAPS.length - 1, g))];
    }

    static void setGap(Context c, int index) {
        get(c).edit().putInt("oraliq", index).apply();
        bumpVersion(c);
    }
}
