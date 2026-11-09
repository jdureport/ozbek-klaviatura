package uz.yangialifbo.klaviatura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ilova oçilganda körinadigan ekran: yoqiş, tanlaş va sinab köriş. */
public class MainActivity extends Activity {

    private TextView status;
    private float density;
    private boolean night;
    private int textColor, mutedColor;
    private KlaviaturaView preview;
    private final View[] colorChips = new View[4];
    private LinearLayout presetRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        textColor = night ? 0xFFF1F2F4 : 0xFF1A1C20;
        mutedColor = night ? 0xFF9095A0 : 0xFF5E646E;

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = px(24);
        col.setPadding(pad, pad, pad, pad);
        scroll.addView(col);

        TextView title = text("Yangi alifbo klaviaturasi", 26, textColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);

        TextView sub = text("Özbek tilining 2026-yilgi lotin alifbosi: 28 harf va ʼ belgisi.",
                16, mutedColor);
        sub.setPadding(0, px(6), 0, px(20));
        col.addView(sub);

        status = text("", 15, textColor);
        status.setPadding(px(14), px(12), px(14), px(12));
        GradientDrawable box = new GradientDrawable();
        box.setCornerRadius(px(10));
        box.setColor(night ? 0xFF24262A : 0xFFEEF6F7);
        status.setBackground(box);
        col.addView(status);

        col.addView(step("1. Klaviaturani yoqing", textColor));
        col.addView(button("Sozlamalarni oçiş", v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));

        col.addView(step("2. Uni asosiy klaviatura qilib tanlang", textColor));
        col.addView(button("Klaviaturani tanlaş", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }));

        col.addView(step("3. Şu yerda sinab köring", textColor));
        EditText test = new EditText(this);
        test.setHint("Masalan: Ötgan kuni çiroyli ğunça oçildi");
        test.setMinLines(3);
        test.setGravity(Gravity.TOP | Gravity.START);
        col.addView(test, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        col.addView(step("Tillar", textColor));
        TextView langHint = text("🌐 tugmasi yoqilgan tillar orasida almaştiradi.", 14, mutedColor);
        col.addView(langHint);
        for (String id : Lang.ALL) {
            CheckBox cb = new CheckBox(this);
            cb.setText(Lang.settingsName(id));
            cb.setTextColor(textColor);
            cb.setTextSize(16);
            cb.setChecked(Prefs.langEnabled(this, id));
            cb.setOnCheckedChangeListener((b, on) -> {
                Prefs.setLangEnabled(this, id, on);
                if (!on && Prefs.enabledLangs(this).size() == 1
                        && !Prefs.langEnabled(this, Prefs.enabledLangs(this).get(0))) {
                    Prefs.setLangEnabled(this, Lang.UZ, true);
                }
            });
            col.addView(cb);
        }

        col.addView(step("Takliflar", textColor));
        CheckBox sug = new CheckBox(this);
        sug.setText("Söz yozayotganda takliflarni körsatiş");
        sug.setTextColor(textColor);
        sug.setTextSize(16);
        sug.setChecked(Prefs.suggestions(this));
        sug.setOnCheckedChangeListener((b, on) -> Prefs.setSuggestions(this, on));
        col.addView(sug);
        col.addView(button("Örganilgan sözlarni öçiriş", v -> {
            KlaviaturaService.clearLearned(this);
            Toast.makeText(this, "Örganilgan sözlar öçirildi", Toast.LENGTH_SHORT).show();
        }));

        // ---- Ranglar ----
        col.addView(step("Ranglar", textColor));
        preview = new KlaviaturaView(this);
        preview.setRows(Lang.create(this, Lang.UZ).letters, false, Lang.create(this, Lang.UZ));
        preview.setSuggestions(java.util.Arrays.asList("salom", "salomat", "salomlar"));
        preview.setEnabled(false);
        preview.setClickable(false);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.bottomMargin = px(12);
        col.addView(preview, plp);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        presetRow = new LinearLayout(this);
        hs.addView(presetRow);
        col.addView(hs);
        for (int i = 0; i < Theme.PRESET_IDS.length; i++) {
            final String id = Theme.PRESET_IDS[i];
            Button b = button(Theme.PRESET_NAMES[i], v -> {
                Theme.setPreset(this, id);
                refreshTheme();
            });
            b.setTag(id);
            presetRow.addView(b);
        }

        String[] colorNames = {"Fon", "Tugmalar", "Harflar", "Urğu rangi"};
        for (int i = 0; i < 4; i++) {
            final int which = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, px(10), 0, px(10));
            View chip = new View(this);
            colorChips[i] = chip;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(px(36), px(36));
            clp.rightMargin = px(14);
            row.addView(chip, clp);
            TextView name = text(colorNames[i], 16, textColor);
            row.addView(name);
            row.setOnClickListener(v -> pickColor(which, colorNames[which]));
            col.addView(row);
        }
        refreshTheme();

        // ---- Harf joylari ----
        col.addView(step("Harf joylari", textColor));
        col.addView(text("Har bir til uçun harflarning joyini özingizga qulay qilib almaştiring.",
                14, mutedColor));
        col.addView(button("Joylaşuvni tahrirlaş", v ->
                startActivity(new Intent(this, LayoutEditorActivity.class))));

        TextView tips = text(
                "Maslahatlar\n"
                        + "• ⇧ tugmasini ikki marta bossangiz — doimiy boş harf.\n"
                        + "• Yuqori qatordagi harfni bosib tursangiz — raqam yoziladi.\n"
                        + "• Böş joyni ikki marta bossangiz — nuqta qöyiladi.\n"
                        + "• Raqam, telefon va SMS-kod maydonlarida katta raqamli klaviatura özi oçiladi.\n"
                        + "• 🌐 yoki böş joyni bosib tursangiz — boşqa klaviaturaga ötasiz.\n"
                        + "• T9: har harf uçun tugmani bir marta bosing, söz özi topiladi. "
                        + "Luğatda yöq sözni yoziş uçun pastki öng tugma bilan «abc» rejimiga öting.\n"
                        + "• Klaviatura siz yozgan sözlarni faqat şu telefonda eslab qoladi, "
                        + "heç qayerga yubormaydi.",
                15, mutedColor);
        tips.setPadding(0, px(24), 0, 0);
        tips.setLineSpacing(px(4), 1f);
        col.addView(tips);

        TextView credits = text(
                "Luğatlar: özbekça — kaharjan/uzb-frequent-words (GPL-3.0); "
                        + "qoraqalpoqça — shagalalab/sozlik (MIT); "
                        + "inglizça va rusça — hermitdave/FrequencyWords (CC BY-SA 4.0).",
                12, mutedColor);
        credits.setPadding(0, px(20), 0, 0);
        col.addView(credits);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) refreshStatus();
    }

    private void refreshStatus() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        boolean enabled = false;
        if (imm != null) {
            for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
                if (getPackageName().equals(info.getPackageName())) {
                    enabled = true;
                    break;
                }
            }
        }
        String current = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = current != null && current.startsWith(getPackageName() + "/");
        String s;
        if (selected) s = "✓ Klaviatura yoqilgan va tanlangan. Yozişingiz mumkin!";
        else if (enabled) s = "✓ Klaviatura yoqilgan. Endi uni tanlang (2-qadam).";
        else s = "Klaviatura hali yoqilmagan (1-qadam).";
        status.setText(s);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (preview != null) {
            Lang uz = Lang.create(this, Lang.UZ);
            preview.setRows(uz.letters, false, uz);
        }
    }

    private void refreshTheme() {
        Theme t = Theme.load(this);
        preview.applyTheme(t);
        for (int i = 0; i < 4; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(t.base(i));
            d.setStroke(px(1), mutedColor);
            colorChips[i].setBackground(d);
        }
        String mode = Theme.mode(this);
        for (int i = 0; i < presetRow.getChildCount(); i++) {
            View b = presetRow.getChildAt(i);
            b.setAlpha(mode.equals(b.getTag()) ? 1f : 0.55f);
        }
    }

    /** Rang tanlaş oynasi: tayyor ranglar va HEX kod. */
    private void pickColor(int which, String title) {
        Theme t = Theme.load(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(20), px(12), px(20), 0);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        EditText hex = new EditText(this);
        hex.setText(Theme.hex(t.base(which)));
        hex.setSingleLine(true);
        for (int color : Theme.SWATCHES) {
            View sw = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(color);
            d.setStroke(px(1), 0xFF8A8F99);
            sw.setBackground(d);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = px(40);
            lp.height = px(40);
            lp.setMargins(px(6), px(6), px(6), px(6));
            sw.setOnClickListener(v -> hex.setText(Theme.hex(color)));
            grid.addView(sw, lp);
        }
        box.addView(grid);
        TextView hint = text("HEX kod (masalan, #0E7C86):", 14, 0xFF8A8F99);
        hint.setPadding(0, px(12), 0, 0);
        box.addView(hint);
        box.addView(hex);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Tayyor", (d, w) -> {
                    Integer c = Theme.parseHex(hex.getText().toString());
                    if (c == null) {
                        Toast.makeText(this, "HEX kod nötöğri", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Theme.setColor(this, which, c);
                    refreshTheme();
                })
                .setNegativeButton("Bekor qiliş", null)
                .show();
    }

    private TextView text(String s, float sizeSp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        return t;
    }

    private TextView step(String s, int color) {
        TextView t = text(s, 17, color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, px(24), 0, px(8));
        return t;
    }

    private Button button(String label, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(onClick);
        return b;
    }

    private int px(float dp) {
        return Math.round(dp * density);
    }
}
