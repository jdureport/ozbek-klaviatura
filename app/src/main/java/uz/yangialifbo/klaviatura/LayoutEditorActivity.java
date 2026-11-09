package uz.yangialifbo.klaviatura;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Harf joylarini tahrirlaş: ikki harfni ketma-ket bosing — ularning joyi almaşadi.
 * Raqamlar (bosib turilganda) birinçi qatordagi örniga qarab avtomatik beriladi.
 */
public class LayoutEditorActivity extends Activity {

    private String langId = Lang.UZ;
    private String[] rows;
    private int selRow = -1, selCol = -1;
    private LinearLayout grid;
    private LinearLayout tabs;
    private float density;
    private int textColor, mutedColor, keyColor, accent, bgColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui ui = new Ui(this);
        density = ui.d;
        textColor = ui.text;
        mutedColor = ui.muted;
        keyColor = ui.night ? 0xFF33353A : 0xFFFFFFFF;
        bgColor = ui.night ? 0xFF24272B : 0xFFE6E8EC;
        accent = ui.accent;

        LinearLayout col = ui.screen();
        col.addView(ui.title("Harf joylari", this::finish));
        TextView hint = ui.text("Ikki harfni ketma-ket bosing — ularning joyi almaşadi. "
                + "Özgarişlar darhol saqlanadi.", 15, ui.muted, false);
        hint.setPadding(ui.px(4), 0, ui.px(4), ui.px(14));
        col.addView(hint);

        String[] names = new String[Layouts.EDITABLE.length];
        for (int i = 0; i < names.length; i++) names[i] = Lang.settingsName(Layouts.EDITABLE[i]);
        LinearLayout seg = ui.segmented(names, 0, i -> {
            langId = Layouts.EDITABLE[i];
            load();
        });
        ((LinearLayout.LayoutParams) seg.getLayoutParams()).setMargins(0, 0, 0, ui.px(14));
        col.addView(seg);
        tabs = new LinearLayout(this);

        LinearLayout card = ui.card();
        card.setPadding(ui.px(8), ui.px(12), ui.px(8), ui.px(12));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(ui.px(4), ui.px(8), ui.px(4), ui.px(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(ui.px(16));
        grid.setBackground(bg);
        card.addView(grid);
        col.addView(card);

        col.addView(ui.tonalButton("Asl holiga qaytariş", v -> {
            Prefs.resetLayout(this, langId);
            Toast.makeText(this, "Asl joylaşuv tiklandi", Toast.LENGTH_SHORT).show();
            load();
        }), ui.matchWrap(16));

        load();
    }

    private void load() {
        rows = Prefs.layoutRows(this, langId);
        selRow = selCol = -1;
        for (int i = 0; i < tabs.getChildCount(); i++) {
            tabs.getChildAt(i).setAlpha(langId.equals(tabs.getChildAt(i).getTag()) ? 1f : 0.5f);
        }
        render();
    }

    private void render() {
        grid.removeAllViews();
        for (int r = 0; r < rows.length; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            int maxLen = 0;
            for (String s : rows) maxLen = Math.max(maxLen, s.length());
            for (int c = 0; c < rows[r].length(); c++) {
                final int rr = r, cc = c;
                TextView k = new TextView(this);
                k.setText(String.valueOf(rows[r].charAt(c)));
                k.setTextSize(20);
                k.setGravity(Gravity.CENTER);
                boolean sel = r == selRow && c == selCol;
                k.setTextColor(sel ? 0xFFFFFFFF : textColor);
                GradientDrawable d = new GradientDrawable();
                d.setCornerRadius(px(7));
                d.setColor(sel ? accent : keyColor);
                k.setBackground(d);
                k.setOnClickListener(v -> tap(rr, cc));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(50), 1f);
                lp.setMargins(px(2), px(4), px(2), px(4));
                row.addView(k, lp);
            }
            // qisqa qatorlarni markazga tekislaş uçun ikki çetdan böş joy
            float pad = (maxLen - rows[r].length()) / 2f;
            row.setWeightSum(maxLen);
            if (pad > 0) {
                row.addView(new android.view.View(this), 0, new LinearLayout.LayoutParams(0, 1, pad));
                row.addView(new android.view.View(this), new LinearLayout.LayoutParams(0, 1, pad));
            }
            grid.addView(row);
        }
    }

    private void tap(int r, int c) {
        if (selRow < 0) {
            selRow = r;
            selCol = c;
        } else if (selRow == r && selCol == c) {
            selRow = selCol = -1;
        } else {
            char a = rows[selRow].charAt(selCol);
            char b = rows[r].charAt(c);
            rows[selRow] = replaceAt(rows[selRow], selCol, b);
            rows[r] = replaceAt(rows[r], c, a);
            Prefs.setLayoutRows(this, langId, rows);
            selRow = selCol = -1;
        }
        render();
    }

    private static String replaceAt(String s, int i, char ch) {
        return s.substring(0, i) + ch + s.substring(i + 1);
    }

    private int px(float dp) {
        return Math.round(dp * density);
    }
}
