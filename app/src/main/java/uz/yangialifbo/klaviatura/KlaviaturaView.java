package uz.yangialifbo.klaviatura;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tugmalarni çizadigan va bosişlarni qabul qiladigan view. */
public class KlaviaturaView extends View {

    public interface Listener {
        void onKeyDown(Key key);
        void onKeyUp(Key key);
        void onKeyTap(Key key);
        boolean onKeyLongPress(Key key);
    }

    public static final int SHIFT_OFF = 0;
    public static final int SHIFT_ONCE = 1;
    public static final int SHIFT_LOCK = 2;

    private static final long LONG_PRESS_MS = 380;

    private Theme c;
    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shiftPath = new Path();
    private final Path lockBarPath = new Path();
    private final Path deletePath = new Path();
    private final Path deleteXPath = new Path();
    private final Path enterPath = new Path();
    private final Path tmpPath = new Path();
    private final Matrix matrix = new Matrix();
    private final RectF drawRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<Press> presses = new SparseArray<>();

    private List<List<Key>> rows = new ArrayList<>();
    private final Key[] suggestionKeys = new Key[3];
    private List<String> suggestions = new ArrayList<>();
    private boolean stripVisible = true;
    private final Key[] toolKeys = {
            new Key(Key.TOOL_CLIP, "", null, 1f), new Key(Key.TOOL_SETTINGS, "", null, 1f)};
    private final Path clipPath = new Path();
    private final Path gearPath = new Path();
    /** Tizim navigatsiya paneli balandligi (Android 15+ da klaviatura uning ostigaça çizadi). */
    private int navInset;
    private boolean previewOn = true;
    private float previewSize;
    private final RectF bubble = new RectF();
    private Lang lang;
    private String modeLabel = "T9";
    private final Path globePath = new Path();
    private final Paint stripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private int shiftState = SHIFT_OFF;
    private boolean symbols;
    private String enterLabel;
    private String spaceLabel = "Özbek";

    private float rowHeight, sidePad, topPad, bottomPad, gapH, gapV, radius, shadowDy,
            letterSize, specialSize, spaceSize, hintSize, iconSize, hintInset, stripHeight,
            suggestionSize, t9DigitSize, t9LettersSize;

    private final class Press implements Runnable {
        Key key;
        boolean consumed;

        Press(Key key) {
            this.key = key;
        }

        @Override
        public void run() {
            if (consumed || listener == null) return;
            if (listener.onKeyLongPress(key)) {
                consumed = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                invalidate();
            }
        }
    }

    public KlaviaturaView(Context context) {
        super(context);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        loadSizes();
        for (int i = 0; i < suggestionKeys.length; i++) {
            suggestionKeys[i] = new Key(Key.SUGGESTION, "", null, 1f);
            suggestionKeys[i].index = i;
        }
        stripPaint.setStrokeWidth(dp(dm, 1));

        c = Theme.load(context);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        hintPaint.setTextAlign(Paint.Align.RIGHT);
        hintPaint.setTextSize(hintSize);
        hintPaint.setColor(c.hint);
        iconPaint.setStrokeWidth(dp(dm, 1.8f));
        iconPaint.setStrokeJoin(Paint.Join.ROUND);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);

        // Ikonkalar 24x24 katakda çizilgan.
        shiftPath.moveTo(12, 3.5f);
        shiftPath.lineTo(20.5f, 12.5f);
        shiftPath.lineTo(15.5f, 12.5f);
        shiftPath.lineTo(15.5f, 19);
        shiftPath.lineTo(8.5f, 19);
        shiftPath.lineTo(8.5f, 12.5f);
        shiftPath.lineTo(3.5f, 12.5f);
        shiftPath.close();
        lockBarPath.moveTo(8.5f, 22);
        lockBarPath.lineTo(15.5f, 22);

        deletePath.moveTo(8, 5);
        deletePath.lineTo(21, 5);
        deletePath.lineTo(21, 19);
        deletePath.lineTo(8, 19);
        deletePath.lineTo(2, 12);
        deletePath.close();
        deleteXPath.moveTo(11.5f, 9);
        deleteXPath.lineTo(17.5f, 15);
        deleteXPath.moveTo(17.5f, 9);
        deleteXPath.lineTo(11.5f, 15);

        enterPath.moveTo(19, 5.5f);
        enterPath.lineTo(19, 13);
        enterPath.lineTo(5, 13);
        enterPath.moveTo(9.5f, 8.5f);
        enterPath.lineTo(5, 13);
        enterPath.lineTo(9.5f, 17.5f);

        globePath.addCircle(12, 12, 9, Path.Direction.CW);
        globePath.addOval(8, 3, 16, 21, Path.Direction.CW);
        globePath.moveTo(3, 12);
        globePath.lineTo(21, 12);

        clipPath.addRoundRect(new RectF(5, 4.5f, 19, 21.5f), 2, 2, Path.Direction.CW);
        clipPath.addRoundRect(new RectF(9, 2.5f, 15, 6.5f), 1.2f, 1.2f, Path.Direction.CW);
        clipPath.moveTo(8.5f, 11);
        clipPath.lineTo(15.5f, 11);
        clipPath.moveTo(8.5f, 15);
        clipPath.lineTo(13.5f, 15);

        gearPath.addCircle(12, 12, 6.5f, Path.Direction.CW);
        gearPath.addCircle(12, 12, 2.5f, Path.Direction.CW);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            gearPath.moveTo((float) (12 + 6.5 * Math.cos(a)), (float) (12 + 6.5 * Math.sin(a)));
            gearPath.lineTo((float) (12 + 9.5 * Math.cos(a)), (float) (12 + 9.5 * Math.sin(a)));
        }

        setHapticFeedbackEnabled(true);
    }

    /** Ölçamlarni sozlamalardan yuklaş (balandlik, pastki böşliq). */
    public void loadSizes() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        float scale = Prefs.heightScale(getContext());
        rowHeight = dp(dm, (landscape ? 44 : 54) * scale);
        sidePad = dp(dm, 4);
        topPad = dp(dm, 4);
        bottomPad = dp(dm, 6 + Prefs.extraBottom(getContext()));
        gapH = dp(dm, 6);
        gapV = dp(dm, 10 * Math.min(1.1f, scale));
        radius = dp(dm, 8);
        shadowDy = dp(dm, 1f);
        letterSize = sp(dm, 22);
        specialSize = sp(dm, 15);
        spaceSize = sp(dm, 13);
        hintSize = sp(dm, 10);
        iconSize = dp(dm, 22);
        hintInset = dp(dm, 5);
        stripHeight = dp(dm, 44);
        suggestionSize = sp(dm, 16);
        t9DigitSize = sp(dm, 22);
        t9LettersSize = sp(dm, 13);
        previewOn = Prefs.keyPreview(getContext());
        previewSize = sp(dm, 30);
        if (getWidth() > 0) layoutKeys(getWidth());
        requestLayout();
        invalidate();
    }

    private static float dp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm);
    }

    private static float sp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, dm);
    }

    // ---- Holatni özgartiriş ----

    /** Ranglarni yangilaş (sozlamalar özgarganda yoki oldindan körişda). */
    public void applyTheme(Theme theme) {
        c = theme;
        hintPaint.setColor(c.hint);
        invalidate();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setRows(List<List<Key>> newRows, boolean isSymbols, Lang language) {
        cancelAllPresses();
        rows = newRows;
        symbols = isSymbols;
        lang = language;
        layoutKeys(getWidth());
        requestLayout();
        invalidate();
    }

    public void setShiftState(int state) {
        if (shiftState != state) {
            shiftState = state;
            invalidate();
        }
    }

    public void setEnterLabel(String label) {
        enterLabel = label;
        invalidate();
    }

    public void setSpaceLabel(String label) {
        spaceLabel = label;
        invalidate();
    }

    public void setModeLabel(String label) {
        modeLabel = label;
        invalidate();
    }

    public void setSuggestions(List<String> list) {
        suggestions = list == null ? new ArrayList<>() : new ArrayList<>(list);
        invalidate();
    }

    public void setStripVisible(boolean visible) {
        if (stripVisible != visible) {
            stripVisible = visible;
            layoutKeys(getWidth());
            requestLayout();
            invalidate();
        }
    }

    private float keysTop() {
        return (stripVisible ? stripHeight : 0) + topPad;
    }

    // ---- Ölçov va joylaşuv ----

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = (int) Math.ceil(keysTop() + bottomPad + rowHeight * Math.max(1, rows.size())) + navInset;
        setMeasuredDimension(w, h);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Android 15 (API 35) dan boşlab klaviatura oynasi navigatsiya paneli ostigaça çiziladi.
        int bottom = 0;
        if (Build.VERSION.SDK_INT >= 35) {
            // navigationBars — tizim paneli; tappableElement — klaviatura oçiqligida
            // çiqadigan ∨ (yopiş) va 🌐 (klaviaturani almaştiriş) tugmalari.
            bottom = Math.max(insets.getInsets(WindowInsets.Type.navigationBars()).bottom,
                    insets.getInsets(WindowInsets.Type.tappableElement()).bottom);
        }
        if (bottom != navInset) {
            navInset = bottom;
            requestLayout();
        }
        return insets;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutKeys(w);
    }

    private void layoutKeys(int width) {
        if (width <= 0 || rows.isEmpty()) return;
        float slot = width / (float) suggestionKeys.length;
        for (int i = 0; i < suggestionKeys.length; i++) {
            suggestionKeys[i].rect.set(i * slot, 0, (i + 1) * slot, stripVisible ? stripHeight : 0);
        }
        float tool = stripHeight * 1.25f;
        for (int i = 0; i < toolKeys.length; i++) {
            toolKeys[i].rect.set(sidePad + i * tool, 0, sidePad + (i + 1) * tool, stripHeight);
        }
        float maxUnits = 0;
        for (List<Key> row : rows) maxUnits = Math.max(maxUnits, units(row));
        float unit = (width - 2 * sidePad) / maxUnits;
        for (int i = 0; i < rows.size(); i++) {
            List<Key> row = rows.get(i);
            float x = sidePad + (maxUnits - units(row)) * unit / 2f;
            float top = keysTop() + i * rowHeight;
            for (Key k : row) {
                k.rect.set(x, top, x + k.width * unit, top + rowHeight);
                x = k.rect.right;
            }
        }
    }

    private static float units(List<Key> row) {
        float u = 0;
        for (Key k : row) u += k.width;
        return u;
    }

    private Key findKey(float x, float y) {
        if (rows.isEmpty()) return null;
        if (stripVisible && y < stripHeight) {
            int si = (int) (x / (getWidth() / (float) suggestionKeys.length));
            si = Math.max(0, Math.min(suggestionKeys.length - 1, si));
            if (suggestions.isEmpty()) {
                for (Key t : toolKeys) if (x >= t.rect.left && x < t.rect.right) return t;
                return null;
            }
            return si < suggestions.size() ? suggestionKeys[si] : null;
        }
        int ri = (int) Math.floor((y - keysTop()) / rowHeight);
        ri = Math.max(0, Math.min(rows.size() - 1, ri));
        Key best = null;
        float bestD = Float.MAX_VALUE;
        for (Key k : rows.get(ri)) {
            if (x >= k.rect.left && x < k.rect.right) return k;
            float d = Math.min(Math.abs(x - k.rect.left), Math.abs(x - k.rect.right));
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    // ---- Çiziş ----

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(c.background);
        if (stripVisible) drawStrip(canvas);
        for (List<Key> row : rows) {
            for (Key k : row) drawKey(canvas, k, isPressed(k));
        }
        if (previewOn) drawPreview(canvas);
    }

    /** Bosilgan harfni tugma ustida kattalaştirib körsatiş (Gboard kabi). */
    private void drawPreview(Canvas canvas) {
        Press p = null;
        for (int i = 0; i < presses.size(); i++) {
            Press q = presses.valueAt(i);
            if (!q.consumed && q.key.type == Key.CHAR) p = q;
        }
        if (p == null) return;
        Key k = p.key;
        float kw = k.rect.width() - gapH;
        float w = Math.max(kw * 1.3f, previewSize * 1.6f);
        float h = (k.rect.height() - gapV) * 1.45f;
        float cx = k.rect.centerX();
        float left = Math.max(0, Math.min(getWidth() - w, cx - w / 2f));
        float bottom = k.rect.top + gapV / 2f + (k.rect.height() - gapV) * 0.35f;
        float top = Math.max(0, bottom - h);
        bubble.set(left, top, left + w, bottom);
        keyPaint.setColor(c.shadow);
        canvas.drawRoundRect(bubble.left, bubble.top + shadowDy * 2, bubble.right,
                bubble.bottom + shadowDy * 2, radius * 1.5f, radius * 1.5f, keyPaint);
        keyPaint.setColor(c.key);
        canvas.drawRoundRect(bubble, radius * 1.5f, radius * 1.5f, keyPaint);
        String label = (!symbols && shiftState != SHIFT_OFF) ? upper(k.text) : k.text;
        textPaint.setTextSize(previewSize);
        textPaint.setColor(c.text);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        canvas.drawText(label, bubble.centerX(),
                bubble.top + (bubble.height() * 0.62f) - (fm.ascent + fm.descent) / 2f - bubble.height() * 0.12f,
                textPaint);
    }

    private void drawStrip(Canvas canvas) {
        stripPaint.setColor(c.hint);
        if (suggestions.isEmpty()) {
            // Asboblar: 📋 bufer, ⚙ sozlamalar
            Path[] icons = {clipPath, gearPath};
            for (int i = 0; i < toolKeys.length; i++) {
                Key t = toolKeys[i];
                if (isPressed(t)) {
                    keyPaint.setColor(c.pressed);
                    canvas.drawRoundRect(t.rect.left + 4, t.rect.top + 4, t.rect.right - 4,
                            t.rect.bottom - 4, radius, radius, keyPaint);
                }
                drawRect.set(t.rect);
                iconPaint.setStyle(Paint.Style.STROKE);
                iconPaint.setColor(c.hint);
                drawIcon(canvas, icons[i]);
            }
            return;
        }
        for (int i = 0; i < suggestionKeys.length; i++) {
            Key k = suggestionKeys[i];
            if (i > 0 && i < suggestions.size()) {
                float x = k.rect.left;
                canvas.drawLine(x, stripHeight * 0.28f, x, stripHeight * 0.72f, stripPaint);
            }
            if (i >= suggestions.size()) continue;
            if (isPressed(k)) {
                keyPaint.setColor(c.pressed);
                canvas.drawRect(k.rect, keyPaint);
            }
            drawRect.set(k.rect);
            String s = suggestions.get(i);
            textPaint.setTypeface(i == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            float size = suggestionSize;
            textPaint.setTextSize(size);
            float max = k.rect.width() - hintInset * 2;
            while (size > suggestionSize * 0.6f && textPaint.measureText(s) > max) {
                size -= 1f;
                textPaint.setTextSize(size);
            }
            drawText(canvas, s, size, c.text);
            textPaint.setTypeface(Typeface.DEFAULT);
        }
    }

    private boolean isPressed(Key k) {
        for (int i = 0; i < presses.size(); i++) {
            if (presses.valueAt(i).key == k) return true;
        }
        return false;
    }

    private void drawKey(Canvas canvas, Key k, boolean pressed) {
        drawRect.set(k.rect.left + gapH / 2f, k.rect.top + gapV / 2f,
                k.rect.right - gapH / 2f, k.rect.bottom - gapV / 2f);

        boolean accent = k.type == Key.ENTER || (k.type == Key.SHIFT && shiftState == SHIFT_LOCK);
        boolean special = k.type != Key.CHAR && k.type != Key.SPACE && k.type != Key.T9
                && k.type != Key.CLIP;
        int bg;
        if (accent) bg = pressed ? c.accentPressed : c.accent;
        else if (special) bg = pressed ? c.specialPressed : c.specialKey;
        else bg = pressed ? c.pressed : c.key;
        int fg = accent ? c.onAccent : c.text;

        keyPaint.setColor(c.shadow);
        canvas.drawRoundRect(drawRect.left, drawRect.top + shadowDy, drawRect.right,
                drawRect.bottom + shadowDy, radius, radius, keyPaint);
        keyPaint.setColor(bg);
        canvas.drawRoundRect(drawRect, radius, radius, keyPaint);

        switch (k.type) {
            case Key.CHAR: {
                String label = (!symbols && shiftState != SHIFT_OFF)
                        ? upper(k.text) : k.text;
                drawText(canvas, label, letterSize, fg);
                if (k.alt != null) {
                    canvas.drawText(k.alt, drawRect.right - hintInset,
                            drawRect.top + hintInset + hintSize * 0.8f, hintPaint);
                }
                break;
            }
            case Key.SHIFT: {
                if (shiftState == SHIFT_OFF) {
                    iconPaint.setStyle(Paint.Style.STROKE);
                    iconPaint.setColor(fg);
                } else {
                    iconPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    iconPaint.setColor(shiftState == SHIFT_ONCE ? c.accent : fg);
                }
                drawIcon(canvas, shiftPath);
                if (shiftState == SHIFT_LOCK) {
                    iconPaint.setStyle(Paint.Style.STROKE);
                    drawIcon(canvas, lockBarPath);
                }
                break;
            }
            case Key.DELETE:
                iconPaint.setStyle(Paint.Style.STROKE);
                iconPaint.setColor(fg);
                drawIcon(canvas, deletePath);
                drawIcon(canvas, deleteXPath);
                break;
            case Key.ENTER:
                if (enterLabel == null) {
                    iconPaint.setStyle(Paint.Style.STROKE);
                    iconPaint.setColor(fg);
                    drawIcon(canvas, enterPath);
                } else {
                    drawText(canvas, enterLabel, specialSize, fg);
                }
                break;
            case Key.SPACE:
                if (!symbols) drawText(canvas, spaceLabel, spaceSize, c.hint);
                else if (!k.text.trim().isEmpty()) drawText(canvas, k.text, specialSize, c.hint);
                break;
            case Key.LANG:
                iconPaint.setStyle(Paint.Style.STROKE);
                iconPaint.setColor(fg);
                drawIcon(canvas, globePath);
                break;
            case Key.T9MODE:
                drawText(canvas, modeLabel, specialSize, fg);
                break;
            case Key.CLIP: {
                textPaint.setTextSize(spaceSize + 1);
                textPaint.setColor(k.index < 0 ? c.hint : fg);
                textPaint.setTextAlign(Paint.Align.LEFT);
                float max = drawRect.width() - hintInset * 4;
                String s = k.text.replace('\n', ' ');
                if (textPaint.measureText(s) > max) {
                    int n = textPaint.breakText(s, true, max - textPaint.measureText("…"), null);
                    s = s.substring(0, Math.max(0, n)) + "…";
                }
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                canvas.drawText(s, drawRect.left + hintInset * 2,
                        drawRect.centerY() - (fm.ascent + fm.descent) / 2f, textPaint);
                textPaint.setTextAlign(Paint.Align.CENTER);
                break;
            }
            case Key.T9: {
                float cx = drawRect.centerX();
                textPaint.setTextSize(t9DigitSize);
                textPaint.setColor(fg);
                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float digitBase = drawRect.centerY() - dp(getResources().getDisplayMetrics(), 1);
                canvas.drawText(k.text, cx, digitBase, textPaint);
                textPaint.setTextSize(t9LettersSize);
                textPaint.setColor(c.hint);
                String letters = shiftState != SHIFT_OFF ? upper(k.alt) : k.alt;
                canvas.drawText(letters, cx, digitBase - fm.ascent * 0.75f, textPaint);
                break;
            }
            default:
                drawText(canvas, k.text, specialSize, fg);
                break;
        }
    }

    private String upper(String s) {
        return lang != null ? lang.upper(s) : s.toUpperCase(Locale.ROOT);
    }

    private void drawText(Canvas canvas, String s, float size, int color) {
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float y = drawRect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(s, drawRect.centerX(), y, textPaint);
    }

    private void drawIcon(Canvas canvas, Path p) {
        float s = iconSize / 24f;
        matrix.setScale(s, s);
        matrix.postTranslate(drawRect.centerX() - iconSize / 2f, drawRect.centerY() - iconSize / 2f);
        tmpPath.reset();
        p.transform(matrix, tmpPath);
        canvas.drawPath(tmpPath, iconPaint);
    }

    // ---- Bosişlar ----

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = e.getActionIndex();
                Key k = findKey(e.getX(idx), e.getY(idx));
                if (k == null) return true;
                // Tez yozişda: oldingi barmoq hali turgan bölsa, uning harfini darhol yozamiz.
                for (int i = 0; i < presses.size(); i++) {
                    Press o = presses.valueAt(i);
                    if (!o.consumed && (o.key.type == Key.CHAR || o.key.type == Key.SPACE
                            || o.key.type == Key.SHIFT || o.key.type == Key.T9)) {
                        handler.removeCallbacks(o);
                        o.consumed = true;
                        if (listener != null) listener.onKeyTap(o.key);
                    }
                }
                Press p = new Press(k);
                presses.put(e.getPointerId(idx), p);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (listener != null) listener.onKeyDown(k);
                handler.postDelayed(p, LONG_PRESS_MS);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    Press p = presses.get(e.getPointerId(i));
                    if (p == null || p.consumed || p.key.type != Key.CHAR) continue;
                    Key k = findKey(e.getX(i), e.getY(i));
                    if (k != null && k != p.key && k.type == Key.CHAR) {
                        handler.removeCallbacks(p);
                        p.key = k;
                        handler.postDelayed(p, LONG_PRESS_MS);
                        invalidate();
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int id = e.getPointerId(e.getActionIndex());
                Press p = presses.get(id);
                presses.remove(id);
                if (p != null) {
                    handler.removeCallbacks(p);
                    if (listener != null) {
                        if (!p.consumed) listener.onKeyTap(p.key);
                        listener.onKeyUp(p.key);
                    }
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                cancelAllPresses();
                return true;
            default:
                return true;
        }
    }

    private void cancelAllPresses() {
        for (int i = 0; i < presses.size(); i++) {
            Press p = presses.valueAt(i);
            handler.removeCallbacks(p);
            if (listener != null) listener.onKeyUp(p.key);
        }
        presses.clear();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllPresses();
        handler.removeCallbacksAndMessages(null);
    }
}
