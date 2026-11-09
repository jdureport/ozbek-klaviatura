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
}
