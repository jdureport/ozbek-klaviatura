package uz.yangialifbo.klaviatura;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Bosh ekran: klaviaturani yoqiş, tanlaş va sinab köriş. Sozlamalar alohida ekranda. */
public class MainActivity extends Activity {

    private Ui ui;
    private LinearLayout setupCard;
    private TextView statusTitle, statusText;
    private View statusDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new Ui(this);
        LinearLayout col = ui.screen();

        // ---- Sarlavha ----
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(0, ui.px(20), 0, ui.px(22));
        TextView logo = new TextView(this);
        logo.setText("ö");
        logo.setTextSize(30);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(0xFFFFFFFF);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable lg = new GradientDrawable();
        lg.setColor(0xFF0E7C86);
        lg.setCornerRadius(ui.px(18));
        logo.setBackground(lg);
        hero.addView(logo, new LinearLayout.LayoutParams(ui.px(60), ui.px(60)));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(ui.px(16), 0, 0, 0);
        ht.addView(ui.text("Özbek klaviatura", 26, ui.text, true));
        ht.addView(ui.text("Yangi alifboda bemalol yozing", 15, ui.muted, false));
        hero.addView(ht);
        col.addView(hero);

        // ---- Oxirgi xato (bölsa) ----
        String crash = App.read(this);
        if (crash != null) {
            LinearLayout err = ui.card();
            err.setPadding(ui.px(18), ui.px(14), ui.px(18), ui.px(16));
            err.addView(ui.text("Oxirgi xato haqida maʼlumot", 16, 0xFFD9480F, true));
            TextView et = ui.text(crash.length() > 1500 ? crash.substring(0, 1500) + "…" : crash,
                    12, ui.muted, false);
            et.setTextIsSelectable(true);
            et.setPadding(0, ui.px(6), 0, ui.px(10));
            err.addView(et);
            LinearLayout btns = new LinearLayout(this);
            btns.addView(ui.tonalButton("Nusxalaş", v -> {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("xato", crash));
                android.widget.Toast.makeText(this, "Nusxalandi", android.widget.Toast.LENGTH_SHORT).show();
            }));
            TextView dismiss = ui.tonalButton("Yopiş", v -> {
                App.clear(this);
                err.setVisibility(View.GONE);
            });
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dl.leftMargin = ui.px(10);
            btns.addView(dismiss, dl);
            err.addView(btns);
            col.addView(err, ui.matchWrap(0));
            col.addView(new View(this), new LinearLayout.LayoutParams(1, ui.px(12)));
        }

        // ---- Holat ----
        LinearLayout status = ui.card();
        LinearLayout srow = new LinearLayout(this);
        srow.setGravity(Gravity.CENTER_VERTICAL);
        srow.setPadding(ui.px(18), ui.px(16), ui.px(18), ui.px(16));
        statusDot = new View(this);
        srow.addView(statusDot, new LinearLayout.LayoutParams(ui.px(12), ui.px(12)));
        LinearLayout stexts = new LinearLayout(this);
        stexts.setOrientation(LinearLayout.VERTICAL);
        stexts.setPadding(ui.px(14), 0, 0, 0);
        statusTitle = ui.text("", 17, ui.text, true);
        statusText = ui.text("", 14, ui.muted, false);
        stexts.addView(statusTitle);
        stexts.addView(statusText);
        srow.addView(stexts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        status.addView(srow);
        col.addView(status);

        // ---- Yoqiş va tanlaş ----
        setupCard = ui.card();
        setupCard.setPadding(ui.px(18), ui.px(16), ui.px(18), ui.px(18));
        setupCard.addView(ui.text("1. Klaviaturani yoqing", 16, ui.text, true));
        TextView s1 = ui.text("Android sozlamalarida «Özbek (yangi alifbo)»ni yoqing.", 14, ui.muted, false);
        s1.setPadding(0, ui.px(2), 0, ui.px(10));
        setupCard.addView(s1);
        setupCard.addView(ui.primary("Sozlamalarni oçiş", v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))), ui.matchWrap(0));
        TextView h2 = ui.text("2. Uni asosiy klaviatura qilib tanlang", 16, ui.text, true);
        h2.setPadding(0, ui.px(20), 0, 0);
        setupCard.addView(h2);
        TextView s2 = ui.text("Röyxatdan «Özbek (yangi alifbo)»ni belgilang.", 14, ui.muted, false);
        s2.setPadding(0, ui.px(2), 0, ui.px(10));
        setupCard.addView(s2);
        setupCard.addView(ui.primary("Klaviaturani tanlaş", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }), ui.matchWrap(0));
        col.addView(setupCard, ui.matchWrap(12));

        // ---- Sinov ----
        col.addView(ui.section("Sinab köring"));
        LinearLayout test = ui.card();
        test.setPadding(ui.px(14), ui.px(14), ui.px(14), ui.px(14));
        EditText field = new EditText(this);
        field.setHint("Ötgan kuni çiroyli ğunça oçildi…");
        field.setHintTextColor(ui.muted);
        field.setTextColor(ui.text);
        field.setTextSize(17);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP | Gravity.START);
        field.setPadding(ui.px(14), ui.px(12), ui.px(14), ui.px(12));
        GradientDrawable fg = new GradientDrawable();
        fg.setColor(ui.night ? 0xFF24272B : 0xFFF3F5F7);
        fg.setCornerRadius(ui.px(14));
        field.setBackground(fg);
        test.addView(field);
        col.addView(test);

        // ---- Sozlamalar ----
        col.addView(ui.section("Klaviatura"));
        LinearLayout nav = ui.card();
        nav.addView(ui.row("Sozlamalar", "Mavzu, ranglar, ölçam, tillar va takliflar",
                ui.chevron(), v -> startActivity(new Intent(this, SettingsActivity.class))));
        nav.addView(ui.divider());
        nav.addView(ui.row("Harf joylari", "Harflarni özingizga qulay qilib almaştiring",
                ui.chevron(), v -> startActivity(new Intent(this, LayoutEditorActivity.class))));
        col.addView(nav);

        // ---- Qisqa yöriqnoma ----
        col.addView(ui.section("Qisqa yöriqnoma"));
        LinearLayout tips = ui.card();
        String[][] items = {
                {"◂ ▸", "Böş joyni çapga yoki öngga surib tilni almaştiring"},
                {"😊", "Emoji tugmasi — emojilar; bosib tursangiz, boşqa klaviaturaga ötasiz"},
                {"123", "Bossangiz — belgilar, bosib tursangiz — raqamli klaviatura"},
                {"⇧", "Ikki marta bossangiz — doimiy boş harf"},
                {"ç v", "Bosib tursangiz: ç → c, v → w"},
                {"→", "Söz va böş joydan keyin — keyingi söz takliflari; klaviatura sizdan örganadi"},
                {"📋", "Takliflar qatorida — oxirgi nusxalangan matnlar"},
                {"␣␣", "Böş joyni ikki marta bossangiz — nuqta qöyiladi"},
        };
        for (int i = 0; i < items.length; i++) {
            if (i > 0) tips.addView(ui.divider());
            TextView icon = ui.text(items[i][0], 16, ui.accent, true);
            icon.setGravity(Gravity.CENTER);
            icon.setMinWidth(ui.px(40));
            LinearLayout r = ui.row(items[i][1], null, null, null);
            r.addView(icon, 0);
            ((LinearLayout.LayoutParams) icon.getLayoutParams()).rightMargin = ui.px(12);
            tips.addView(r);
        }
        col.addView(tips);

        TextView foot = ui.text("Dasturçi: J.Imomnazarov", 13, ui.muted, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, ui.px(22), 0, 0);
        col.addView(foot);
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
        String current = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = current != null && current.startsWith(getPackageName() + "/");
        int dot;
        if (selected) {
            statusTitle.setText("Hammasi tayyor");
            statusText.setText("Klaviatura yoqilgan va tanlangan. Yozişingiz mumkin!");
            dot = 0xFF2F9E44;
        } else if (enabled) {
            statusTitle.setText("Bir qadam qoldi");
            statusText.setText("Klaviatura yoqilgan. Endi uni asosiy qilib tanlang.");
            dot = 0xFFE8A30C;
        } else {
            statusTitle.setText("Klaviatura hali yoqilmagan");
            statusText.setText("Quyidagi ikki qadamni bajaring.");
            dot = 0xFFD9480F;
        }
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(dot);
        statusDot.setBackground(g);
        setupCard.setVisibility(selected ? View.GONE : View.VISIBLE);
    }
}
