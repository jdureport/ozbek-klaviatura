package uz.yangialifbo.klaviatura;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Ilova ekranlari uçun kiçik dizayn tizimi: kartoçkalar, qatorlar, switchlar, segmentlar. */
final class Ui {

    final Activity a;
    final float d;
    final boolean night;
    final int bg, card, text, muted, accent, onAccent, divider, tonal;

    Ui(Activity activity) {
        a = activity;
        d = activity.getResources().getDisplayMetrics().density;
        night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        bg = night ? 0xFF0F1012 : 0xFFF3F5F7;
        card = night ? 0xFF1A1C1F : 0xFFFFFFFF;
        text = night ? 0xFFF1F2F4 : 0xFF15171A;
        muted = night ? 0xFF9095A0 : 0xFF5E646E;
        accent = night ? 0xFF23A3AE : 0xFF0E7C86;
        onAccent = night ? 0xFF04282B : 0xFFFFFFFF;
        divider = night ? 0xFF2A2D31 : 0xFFE9ECEF;
        tonal = night ? 0xFF16363A : 0xFFDDF0F1;
    }

    int px(float dp) {
        return Math.round(dp * d);
    }

    // ---- Ekran ----

    /** Butun ekran: fon, tizim panellari uçun çetdan joy, aylantiriladigan ustun. */
    LinearLayout screen() {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(bg);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(night ? 0 : light, light);
            }
        } else if (!night) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        ScrollView scroll = new ScrollView(a);
        scroll.setBackgroundColor(bg);
        scroll.setClipToPadding(false);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(18), px(12), px(18), px(32));
        scroll.addView(col);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                top = i.top;
                bottom = i.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            // Android 15+ da ekran tizim panellari ostigaça çiziladi — faqat şunda joy qoldiramiz.
            if (Build.VERSION.SDK_INT >= 35) v.setPadding(0, top, 0, bottom);
            return insets;
        });
        a.setContentView(scroll);
        return col;
    }

    // ---- Matn ----

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setLineSpacing(px(2), 1f);
        return t;
    }

    View title(String s, Runnable back) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, px(8), 0, px(18));
        if (back != null) {
            TextView arrow = text("←", 24, text, false);
            arrow.setGravity(Gravity.CENTER);
            arrow.setBackground(ripple(0x00000000, px(22)));
            arrow.setOnClickListener(v -> back.run());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(44), px(44));
            lp.rightMargin = px(6);
            row.addView(arrow, lp);
        }
        TextView t = text(s, 30, text, true);
        row.addView(t);
        return row;
    }

    TextView section(String s) {
        TextView t = text(s.toUpperCase(java.util.Locale.ROOT), 13, accent, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(px(6), px(22), 0, px(8));
        return t;
    }

    // ---- Kartoçka va qatorlar ----

    LinearLayout card() {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(card);
        g.setCornerRadius(px(22));
        c.setBackground(g);
        c.setClipToOutline(true);
        c.setPadding(0, px(4), 0, px(4));
        return c;
    }

    View divider() {
        View v = new View(a);
        v.setBackgroundColor(divider);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, px(1)));
        lp.leftMargin = px(18);
        lp.rightMargin = px(18);
        v.setLayoutParams(lp);
        return v;
    }

    /** Sarlavha, izoh va öng tomondagi element bilan qator. */
    LinearLayout row(String title, String subtitle, View right, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(px(60));
        row.setPadding(px(18), px(12), px(14), px(12));
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 16, text, false));
        if (subtitle != null) {
            TextView s = text(subtitle, 13.5f, muted, false);
            s.setPadding(0, px(2), 0, 0);
            texts.addView(s);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (right != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = px(12);
            row.addView(right, lp);
        }
        if (click != null) {
            row.setBackground(ripple(0x00000000, 0));
            row.setOnClickListener(click);
        }
        return row;
    }

    TextView chevron() {
        return text("›", 24, muted, false);
    }

    TextView value(String s) {
        return text(s, 15, muted, false);
    }

    Switch toggle(boolean checked, Consumer<Boolean> onChange) {
        Switch s = new Switch(a);
        s.setChecked(checked);
        int[][] states = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(states, new int[]{accent, night ? 0xFF9095A0 : 0xFFFFFFFF}));
        s.setTrackTintList(new ColorStateList(states,
                new int[]{Theme.mix(accent, card, 0.45f), night ? 0xFF3A3D42 : 0xFFC9CED6}));
        s.setOnCheckedChangeListener((b, on) -> onChange.accept(on));
        return s;
    }

    /** Switch'li qator: butun qatorni bosganda ham özgaradi. */
    LinearLayout switchRow(String title, String subtitle, boolean checked, Consumer<Boolean> onChange) {
        Switch s = toggle(checked, onChange);
        return row(title, subtitle, s, v -> s.toggle());
    }

    /** Segmentli tanlov (masalan: Ixçam | Oddiy | Baland). */
    LinearLayout segmented(String[] labels, int selected, IntConsumer onPick) {
        LinearLayout box = new LinearLayout(a);
        GradientDrawable g = new GradientDrawable();
        g.setColor(night ? 0xFF24272B : 0xFFEEF0F3);
        g.setCornerRadius(px(14));
        box.setBackground(g);
        box.setPadding(px(4), px(4), px(4), px(4));
        TextView[] items = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = text(labels[i], 14.5f, text, true);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(px(6), px(9), px(6), px(9));
            items[i] = t;
            t.setOnClickListener(v -> {
                styleSegments(items, idx);
                onPick.accept(idx);
            });
            box.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        styleSegments(items, selected);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(px(18), px(2), px(18), px(12));
        box.setLayoutParams(lp);
        return box;
    }

    private void styleSegments(TextView[] items, int selected) {
        for (int i = 0; i < items.length; i++) {
            boolean on = i == selected;
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(px(11));
            g.setColor(on ? card : 0x00000000);
            items[i].setBackground(g);
            items[i].setTextColor(on ? accent : muted);
            items[i].setElevation(on ? px(1) : 0);
        }
    }

    /** Asosiy (töldirilgan) tugma. */
    TextView primary(String label, View.OnClickListener click) {
        TextView b = text(label, 16, onAccent, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(22), px(14), px(22), px(14));
        b.setBackground(ripple(accent, px(26)));
        b.setOnClickListener(click);
        return b;
    }

    /** Ikkilamçi (yumşoq rangli) tugma. */
    TextView tonalButton(String label, View.OnClickListener click) {
        TextView b = text(label, 15, night ? 0xFF8FE3EA : 0xFF0A5A61, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(18), px(12), px(18), px(12));
        b.setBackground(ripple(tonal, px(24)));
        b.setOnClickListener(click);
        return b;
    }

    /** Rang doiraçasi. */
    View swatch(int color, int sizeDp) {
        View v = new View(a);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setStroke(px(1), night ? 0xFF4A4D52 : 0xFFD5D9DF);
        v.setBackground(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(px(sizeDp), px(sizeDp)));
        return v;
    }

    RippleDrawable ripple(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(radius);
        return new RippleDrawable(ColorStateList.valueOf(night ? 0x33FFFFFF : 0x1F000000), shape, mask);
    }

    LinearLayout.LayoutParams matchWrap(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(topDp);
        return lp;
    }
}
