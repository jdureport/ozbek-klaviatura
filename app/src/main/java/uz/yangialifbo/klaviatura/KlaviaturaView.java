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
import android.view.View;

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

    /** Ranglar — yoruğ va qorönği mavzu. */
    private static final class Palette {
        int background, key, specialKey, pressed, specialPressed, shadow,
                text, hint, accent, accentPressed, onAccent;
    }

    private final Palette c = new Palette();
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
    private Listener listener;
    private int shiftState = SHIFT_OFF;
    private boolean symbols;
    private String enterLabel;
    private String spaceLabel = "Özbek";

    private final float rowHeight, sidePad, topPad, bottomPad, gapH, gapV, radius, shadowDy,
            letterSize, specialSize, spaceSize, hintSize, iconSize, hintInset;

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
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        rowHeight = dp(dm, landscape ? 44 : 56);
        sidePad = dp(dm, 3);
        topPad = dp(dm, 6);
        bottomPad = dp(dm, 6);
        gapH = dp(dm, 5);
        gapV = dp(dm, 8);
        radius = dp(dm, 7);
        shadowDy = dp(dm, 1.2f);
        letterSize = sp(dm, 23);
        specialSize = sp(dm, 15);
        spaceSize = sp(dm, 13);
        hintSize = sp(dm, 10.5f);
        iconSize = dp(dm, 22);
        hintInset = dp(dm, 5);

        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            c.background = 0xFF17181B;
            c.key = 0xFF33353A;
            c.specialKey = 0xFF24262A;
            c.pressed = 0xFF4A4D54;
            c.specialPressed = 0xFF3A3D43;
            c.shadow = 0xFF0C0D0F;
            c.text = 0xFFF1F2F4;
            c.hint = 0xFF9095A0;
            c.accent = 0xFF23A3AE;
            c.accentPressed = 0xFF1B858E;
            c.onAccent = 0xFF04282B;
        } else {
            c.background = 0xFFE6E8EC;
            c.key = 0xFFFFFFFF;
            c.specialKey = 0xFFC8CDD5;
            c.pressed = 0xFFD9DDE3;
            c.specialPressed = 0xFFAEB4BE;
            c.shadow = 0xFFA9AFB8;
            c.text = 0xFF1A1C20;
            c.hint = 0xFF7C828C;
            c.accent = 0xFF0E7C86;
            c.accentPressed = 0xFF0A626A;
            c.onAccent = 0xFFFFFFFF;
        }

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

        setHapticFeedbackEnabled(true);
    }

    private static float dp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm);
    }

    private static float sp(DisplayMetrics dm, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, dm);
    }

    // ---- Holatni özgartiriş ----

    public void setListener(Listener l) {
        listener = l;
    }

    public void setRows(List<List<Key>> newRows, boolean isSymbols) {
        cancelAllPresses();
        rows = newRows;
        symbols = isSymbols;
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

    // ---- Ölçov va joylaşuv ----

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = (int) Math.ceil(topPad + bottomPad + rowHeight * Math.max(1, rows.size()));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutKeys(w);
    }

    private void layoutKeys(int width) {
        if (width <= 0 || rows.isEmpty()) return;
        float maxUnits = 0;
        for (List<Key> row : rows) maxUnits = Math.max(maxUnits, units(row));
        float unit = (width - 2 * sidePad) / maxUnits;
        for (int i = 0; i < rows.size(); i++) {
            List<Key> row = rows.get(i);
            float x = sidePad + (maxUnits - units(row)) * unit / 2f;
            float top = topPad + i * rowHeight;
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
        int ri = (int) Math.floor((y - topPad) / rowHeight);
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
        for (List<Key> row : rows) {
            for (Key k : row) drawKey(canvas, k, isPressed(k));
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
        boolean special = k.type != Key.CHAR && k.type != Key.SPACE;
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
                        ? k.text.toUpperCase(Locale.ROOT) : k.text;
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
                break;
            default:
                drawText(canvas, k.text, specialSize, fg);
                break;
        }
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
                            || o.key.type == Key.SHIFT)) {
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
