package uz.yangialifbo.klaviatura;

import android.app.Activity;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Ilova oçilganda körinadigan ekran: yoqiş, tanlaş va sinab köriş. */
public class MainActivity extends Activity {

    private TextView status;
    private float density;
    private boolean night;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int textColor = night ? 0xFFF1F2F4 : 0xFF1A1C20;
        int mutedColor = night ? 0xFF9095A0 : 0xFF5E646E;

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

        TextView tips = text(
                "Maslahatlar\n"
                        + "• ⇧ tugmasini ikki marta bossangiz — doimiy boş harf.\n"
                        + "• Yuqori qatordagi harfni bosib tursangiz — raqam yoziladi.\n"
                        + "• Böş joyni ikki marta bossangiz — nuqta qöyiladi.\n"
                        + "• 🌐 yoki böş joyni bosib tursangiz — boşqa klaviaturaga ötasiz.\n"
                        + "• T9: har harf uçun tugmani bir marta bosing, söz özi topiladi. "
                        + "Luğatda yöq sözni yoziş uçun pastki öng tugma bilan «abc» rejimiga öting.\n"
                        + "• Klaviatura siz yozgan sözlarni faqat şu telefonda eslab qoladi, "
                        + "hech qayerga yubormaydi.",
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
