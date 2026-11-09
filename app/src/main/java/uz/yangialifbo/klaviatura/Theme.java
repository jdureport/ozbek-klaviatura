package uz.yangialifbo.klaviatura;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/**
 * Klaviatura ranglari. Foydalanuvçi 4 ta asosiy rangni tanlaydi
 * (fon, tugma, harf, urğu), qolganlari ulardan hisoblanadi.
 */
final class Theme {

    /** Tayyor mavzular: id, nom, fon, tugma, harf, urğu. */
    static final String[] PRESET_IDS = {"auto", "yorug", "qorongi", "tun", "anor", "osmon", "lola", "qum"};
    static final String[] PRESET_NAMES = {"Avtomatik", "Yoruğ", "Qorönği", "Tun", "Anor", "Osmon", "Lola", "Qum"};
    private static final int[][] PRESET_COLORS = {
            null,
            {0xFFE6E8EC, 0xFFFFFFFF, 0xFF1A1C20, 0xFF0E7C86},
            {0xFF17181B, 0xFF33353A, 0xFFF1F2F4, 0xFF23A3AE},
            {0xFF000000, 0xFF1C1C1E, 0xFFFFFFFF, 0xFF23A3AE},
            {0xFFF6E9EA, 0xFFFFFFFF, 0xFF2A1215, 0xFFB3263E},
            {0xFFE3ECF7, 0xFFFFFFFF, 0xFF13233A, 0xFF1F6FEB},
            {0xFFF8E8F0, 0xFFFFFFFF, 0xFF2E1424, 0xFFD63384},
            {0xFFEFE6D8, 0xFFFFFBF4, 0xFF2B2117, 0xFFB7791F},
    };
    static final String CUSTOM = "maxsus";

    /** Rang tanlaş oynasidagi tayyor ranglar. */
    static final int[] SWATCHES = {
            0xFFFFFFFF, 0xFFF1F2F4, 0xFFE6E8EC, 0xFFC8CDD5, 0xFF8A8F99,
            0xFF33353A, 0xFF1C1C1E, 0xFF000000, 0xFF0E7C86, 0xFF23A3AE,
            0xFF1F6FEB, 0xFF5B4BDB, 0xFFD63384, 0xFFB3263E, 0xFFE8590C,
            0xFFB7791F, 0xFF2F9E44, 0xFF0B7285, 0xFFF6E9EA, 0xFFEFE6D8,
    };

    final int background, key, text, accent;
    final int specialKey, pressed, specialPressed, shadow, hint, accentPressed, onAccent;

    Theme(int background, int key, int text, int accent) {
        this.background = background;
        this.key = key;
        this.text = text;
        this.accent = accent;
        boolean light = luminance(background) > 0.4;
        specialKey = mix(background, text, light ? 0.13f : 0.10f);
        pressed = mix(key, text, 0.12f);
        specialPressed = mix(specialKey, text, 0.14f);
        shadow = mix(background, 0xFF000000, light ? 0.25f : 0.55f);
        hint = mix(text, key, 0.45f);
        accentPressed = mix(accent, 0xFF000000, 0.18f);
        onAccent = luminance(accent) > 0.55 ? 0xFF111214 : 0xFFFFFFFF;
    }

    // ---- Yuklaş va saqlaş ----

    static Theme load(Context c) {
        SharedPreferences p = Prefs.get(c);
        String mode = p.getString("mavzu", "auto");
        if (CUSTOM.equals(mode)) {
            int[] d = PRESET_COLORS[1];
            return new Theme(p.getInt("rang_fon", d[0]), p.getInt("rang_tugma", d[1]),
                    p.getInt("rang_harf", d[2]), p.getInt("rang_urgu", d[3]));
        }
        return preset(mode, c);
    }

    static Theme preset(String id, Context c) {
        int i = indexOf(id);
        if (i <= 0) {
            boolean night = (c.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            i = night ? 2 : 1;
        }
        int[] v = PRESET_COLORS[i];
        return new Theme(v[0], v[1], v[2], v[3]);
    }

    static String mode(Context c) {
        return Prefs.get(c).getString("mavzu", "auto");
    }

    static void setPreset(Context c, String id) {
        Prefs.get(c).edit().putString("mavzu", id).apply();
        Prefs.bumpVersion(c);
    }

    /** Bitta rangni özgartiriş; mavzu "maxsus"ga ötadi. which: 0 fon, 1 tugma, 2 harf, 3 urğu. */
    static void setColor(Context c, int which, int color) {
        Theme t = load(c);
        int[] v = {t.background, t.key, t.text, t.accent};
        v[which] = color;
        Prefs.get(c).edit()
                .putString("mavzu", CUSTOM)
                .putInt("rang_fon", v[0]).putInt("rang_tugma", v[1])
                .putInt("rang_harf", v[2]).putInt("rang_urgu", v[3])
                .apply();
        Prefs.bumpVersion(c);
    }

    int base(int which) {
        switch (which) {
            case 0: return background;
            case 1: return key;
            case 2: return text;
            default: return accent;
        }
    }

    private static int indexOf(String id) {
        for (int i = 0; i < PRESET_IDS.length; i++) if (PRESET_IDS[i].equals(id)) return i;
        return 0;
    }

    // ---- Rang hisob-kitobi ----

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static double luminance(int c) {
        double r = ((c >> 16) & 0xFF) / 255.0, g = ((c >> 8) & 0xFF) / 255.0, b = (c & 0xFF) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    static String hex(int c) {
        return String.format("#%06X", c & 0xFFFFFF);
    }

    /** "#0E7C86" yoki "0E7C86" → rang; nötöğri bölsa null. */
    static Integer parseHex(String s) {
        s = s.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() != 6) return null;
        try {
            return 0xFF000000 | Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
