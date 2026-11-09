package uz.yangialifbo.klaviatura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/** Klaviatura sozlamalari: köriniş, ölçam, tillar, yoziş, harf joylari, ilova haqida. */
public class SettingsActivity extends Activity {

    private static final String PRIVACY_URL = "https://klaviatura.volontyor.uz/maxfiylik/";

    private Ui ui;
    private KlaviaturaView preview;
    private LinearLayout chips;
    private final View[] colorSwatches = new View[4];
    private final LinearLayout[] colorSlots = new LinearLayout[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            build();
        } catch (Throwable e) {
            App.save(this, e);
            showError(e);
        }
    }

    /** Ekranni qura olmasak — xatoni körsatamiz (nusxalab yuboriş uçun). */
    private void showError(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        TextView t = new TextView(this);
        t.setTextIsSelectable(true);
        t.setPadding(40, 120, 40, 40);
        t.setText("Sozlamalarni oçişda xato yuz berdi. Bu matnni nusxalab dasturçiga yuboring:\n\n" + sw);
        sv.addView(t);
        setContentView(sv);
    }

    private void build() {
        ui = new Ui(this);
        LinearLayout col = ui.screen();
        col.addView(ui.title("Sozlamalar", this::finish));

        // ---- Jonli köriniş ----
        LinearLayout pcard = ui.card();
        pcard.setPadding(0, 0, 0, 0);
        preview = new KlaviaturaView(this);
        pcard.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(pcard);
        refreshPreviewLayout();

        // ---- Köriniş ----
        col.addView(ui.section("Köriniş"));
        LinearLayout look = ui.card();
        TextView themeTitle = ui.text("Mavzu", 16, ui.text, false);
        themeTitle.setPadding(ui.px(18), ui.px(14), ui.px(18), ui.px(10));
        look.addView(themeTitle);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chips = new LinearLayout(this);
        chips.setPadding(ui.px(14), 0, ui.px(14), ui.px(12));
        hs.addView(chips);
        look.addView(hs);
        for (int i = 0; i < Theme.PRESET_IDS.length; i++) chips.addView(themeChip(i));
        String[] colorNames = {"Fon", "Tugmalar", "Harflar", "Urğu rangi"};
        String[] colorHints = {"Klaviaturaning orqa foni", "Harf tugmalarining rangi",
                "Tugmalardagi yozuvlar", "↵, ⇧ va tanlangan elementlar"};
        for (int i = 0; i < 4; i++) {
            final int which = i;
            look.addView(ui.divider());
            LinearLayout slot = new LinearLayout(this);
            colorSlots[i] = slot;
            look.addView(ui.row(colorNames[i], colorHints[i], slot,
                    v -> pickColor(which, colorNames[which])));
        }
        col.addView(look);

        // ---- Ölçam ----
        col.addView(ui.section("Ölçam"));
        LinearLayout size = ui.card();
        size.addView(ui.row("Balandlik", null, null, null));
        size.addView(ui.segmented(Prefs.HEIGHT_NAMES, indexOf(Prefs.HEIGHT_IDS, Prefs.height(this)), i -> {
            Prefs.setHeight(this, Prefs.HEIGHT_IDS[i]);
            preview.loadSizes();
        }));
        size.addView(ui.divider());
        size.addView(ui.row("Tugmalar oralığı", null, null, null));
        size.addView(ui.segmented(Prefs.GAP_NAMES, Prefs.gap(this), i -> {
            Prefs.setGap(this, i);
            preview.loadSizes();
        }));
        size.addView(ui.divider());
        size.addView(ui.row("Pastdan qöşimça joy",
                "Telefonning ∨ va 🌐 tugmalari klaviaturaga tegib tursa, oşiring", null, null));
        String[] gaps = new String[Prefs.EXTRA_BOTTOM.length];
        int gapSel = 0;
        for (int i = 0; i < gaps.length; i++) {
            gaps[i] = Prefs.EXTRA_BOTTOM[i] == 0 ? "Yöq" : Prefs.EXTRA_BOTTOM[i] + " dp";
            if (Prefs.EXTRA_BOTTOM[i] == Prefs.extraBottom(this)) gapSel = i;
        }
        size.addView(ui.segmented(gaps, gapSel, i -> {
            Prefs.setExtraBottom(this, Prefs.EXTRA_BOTTOM[i]);
            preview.loadSizes();
        }));
        size.addView(ui.divider());
        size.addView(ui.switchRow("Raqamlar qatori", "Harflar ustida 1–0 tugmalari",
                Prefs.numberRow(this), on -> {
                    Prefs.setNumberRow(this, on);
                    refreshPreviewLayout();
                }));
        size.addView(ui.divider());
        size.addView(ui.switchRow("Bosilgan harfni kattalaştiriş", "Harf tugma ustida kattaroq körinadi",
                Prefs.keyPreview(this), on -> {
                    Prefs.setKeyPreview(this, on);
                    preview.loadSizes();
                }));
        col.addView(size);

        // ---- Tillar ----
        col.addView(ui.section("Tillar"));
        LinearLayout langs = ui.card();
        TextView langHint = ui.text("Til böş joy tugmasini çapga yoki öngga surib almaştiriladi.",
                13.5f, ui.muted, false);
        langHint.setPadding(ui.px(18), ui.px(12), ui.px(18), ui.px(4));
        langs.addView(langHint);
        for (String id : Lang.ALL) {
            langs.addView(ui.divider());
            langs.addView(ui.switchRow(Lang.settingsName(id), langSubtitle(id),
                    Prefs.langEnabled(this, id), on -> {
                        Prefs.setLangEnabled(this, id, on);
                        Prefs.bumpVersion(this);
                    }));
        }
        col.addView(langs);

        // ---- Yoziş ----
        col.addView(ui.section("Yoziş"));
        LinearLayout typing = ui.card();
        typing.addView(ui.switchRow("Söz takliflari", "Yozayotganda klaviatura ustida sözlar çiqadi",
                Prefs.suggestions(this), on -> Prefs.setSuggestions(this, on)));
        typing.addView(ui.divider());
        typing.addView(ui.row("Harf joylari", "Harflarni özingizga qulay qilib almaştiring",
                ui.chevron(), v -> startActivity(new Intent(this, LayoutEditorActivity.class))));
        typing.addView(ui.divider());
        typing.addView(ui.row("Örganilgan sözlarni öçiriş", "Klaviatura eslab qolgan sözlar tozalanadi",
                null, v -> new AlertDialog.Builder(this)
                        .setTitle("Örganilgan sözlar öçirilsinmi?")
                        .setMessage("Klaviatura siz yozgan sözlarni qaytadan örganadi.")
                        .setPositiveButton("Öçiriş", (d, w) -> {
                            KlaviaturaService.clearLearned(this);
                            Toast.makeText(this, "Örganilgan sözlar öçirildi", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Bekor qiliş", null)
                        .show()));
        col.addView(typing);

        // ---- Haqida ----
        col.addView(ui.section("Ilova haqida"));
        LinearLayout about = ui.card();
        about.addView(ui.row("Dasturçi", null, ui.value("J.Imomnazarov"), null));
        about.addView(ui.divider());
        about.addView(ui.row("Versiya", null, ui.value(version()), null));
        about.addView(ui.divider());
        about.addView(ui.row("Maxfiylik siyosati", "Klaviatura matningizni heç qayerga yubormaydi",
                ui.chevron(), v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)));
                    } catch (Exception e) {
                        Toast.makeText(this, PRIVACY_URL, Toast.LENGTH_LONG).show();
                    }
                }));
        about.addView(ui.divider());
        about.addView(ui.row("Luğatlar va litsenziyalar",
                "Özbekça — öz röyxatimiz, CC0 korpus, MIT va Apache lemmalari; qoraqalpoqça — MIT; "
                        + "inglizça va rusça — CC BY-SA 4.0", ui.chevron(), v -> showLicenses()));
        col.addView(about);

        refreshTheme();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        refreshPreviewLayout();
    }

    private void refreshPreviewLayout() {
        Lang uz = Lang.create(this, Lang.UZ);
        preview.setRows(uz.letters, false, uz);
        preview.setSpaceLabel("◂  " + uz.label + "  ▸");
        preview.setSuggestions(Arrays.asList("salom", "salomat", "salomlar"));
    }

    private View themeChip(int i) {
        String id = Theme.PRESET_IDS[i];
        LinearLayout chip = new LinearLayout(this);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(ui.px(10), ui.px(8), ui.px(14), ui.px(8));
        chip.setTag(id);
        Theme t = Theme.preset(id, this);
        View dot = ui.swatch(t.accent, 18);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(t.accent);
        dg.setStroke(ui.px(4), t.background);
        dot.setBackground(dg);
        chip.addView(dot);
        TextView name = ui.text(Theme.PRESET_NAMES[i], 14.5f, ui.text, true);
        name.setPadding(ui.px(8), 0, 0, 0);
        chip.addView(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = ui.px(8);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            Theme.setPreset(this, id);
            refreshTheme();
        });
        return chip;
    }

    private void refreshTheme() {
        Theme t = Theme.load(this);
        preview.applyTheme(t);
        String mode = Theme.mode(this);
        for (int i = 0; i < chips.getChildCount(); i++) {
            View chip = chips.getChildAt(i);
            boolean on = mode.equals(chip.getTag());
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(ui.px(20));
            g.setColor(on ? ui.tonal : 0x00000000);
            g.setStroke(ui.px(1), on ? ui.accent : ui.divider);
            chip.setBackground(g);
        }
        for (int i = 0; i < 4; i++) {
            colorSlots[i].removeAllViews();
            colorSwatches[i] = ui.swatch(t.base(i), 30);
            colorSlots[i].addView(colorSwatches[i]);
        }
    }

    /** Rang tanlaş oynasi: tayyor ranglar va HEX kod. */
    private void pickColor(int which, String title) {
        Theme t = Theme.load(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(ui.px(20), ui.px(12), ui.px(20), 0);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        EditText hex = new EditText(this);
        hex.setText(Theme.hex(t.base(which)));
        hex.setSingleLine(true);
        for (int color : Theme.SWATCHES) {
            View sw = ui.swatch(color, 40);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = ui.px(40);
            lp.height = ui.px(40);
            lp.setMargins(ui.px(6), ui.px(6), ui.px(6), ui.px(6));
            sw.setOnClickListener(v -> hex.setText(Theme.hex(color)));
            grid.addView(sw, lp);
        }
        box.addView(grid);
        TextView hint = ui.text("HEX kod (masalan, #0E7C86):", 14, 0xFF8A8F99, false);
        hint.setPadding(0, ui.px(12), 0, 0);
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

    private void showLicenses() {
        String text;
        try (java.io.InputStream in = getAssets().open("LICENSES.txt")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            text = out.toString("UTF-8");
        } catch (Exception e) {
            text = "Litsenziyalar fayli topilmadi.";
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        TextView t = ui.text(text, 13, ui.text, false);
        t.setTextIsSelectable(true);
        t.setPadding(ui.px(22), ui.px(12), ui.px(22), ui.px(12));
        sv.addView(t);
        new AlertDialog.Builder(this)
                .setTitle("Luğatlar va litsenziyalar")
                .setView(sv)
                .setPositiveButton("Yopiş", null)
                .show();
    }

    private static String langSubtitle(String id) {
        switch (id) {
            case Lang.UZ: return "2026-yilgi lotin alifbosi, 28 harf va ʼ";
            case Lang.UZ_T9: return "12 tugma, söz luğatdan topiladi";
            case Lang.KAA: return "2016-yilgi lotin alifbosi";
            case Lang.EN: return "QWERTY";
            case Lang.RU: return "Standart rus joylaşuvi";
            default: return null;
        }
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return 0;
    }
}
