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
        density = getResources().getDisplayMetrics().density;
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        textColor = night ? 0xFFF1F2F4 : 0xFF1A1C20;
        mutedColor = night ? 0xFF9095A0 : 0xFF5E646E;
        keyColor = night ? 0xFF33353A : 0xFFFFFFFF;
        bgColor = night ? 0xFF17181B : 0xFFE6E8EC;
        accent = night ? 0xFF23A3AE : 0xFF0E7C86;

        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(20), px(24), px(20), px(24));
        scroll.addView(col);

        TextView title = new TextView(this);
        title.setText("Harf joylari");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(textColor);
        col.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Ikki harfni ketma-ket bosing — ularning joyi almaşadi. "
                + "Özgarişlar darhol saqlanadi va klaviaturada keyingi safar körinadi.");
        hint.setTextSize(15);
        hint.setTextColor(mutedColor);
        hint.setPadding(0, px(6), 0, px(16));
        col.addView(hint);

        tabs = new LinearLayout(this);
        col.addView(tabs);
        for (String id : Layouts.EDITABLE) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(Lang.settingsName(id));
            b.setTag(id);
            b.setOnClickListener(v -> {
                langId = id;
                load();
            });
            tabs.addView(b, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(px(6), px(10), px(6), px(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(px(14));
        grid.setBackground(bg);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.topMargin = px(14);
        glp.bottomMargin = px(14);
        col.addView(grid, glp);

        Button reset = new Button(this);
        reset.setAllCaps(false);
        reset.setText("Asl holiga qaytariş");
        reset.setOnClickListener(v -> {
            Prefs.resetLayout(this, langId);
            Toast.makeText(this, "Asl joylaşuv tiklandi", Toast.LENGTH_SHORT).show();
            load();
        });
        col.addView(reset);

        setContentView(scroll);
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
