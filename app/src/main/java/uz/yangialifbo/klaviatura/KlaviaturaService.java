package uz.yangialifbo.klaviatura;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.List;
import java.util.Locale;

/** Klaviatura servisi: tugma bosişlarini matnga aylantiradi. */
public class KlaviaturaService extends InputMethodService implements KlaviaturaView.Listener {

    private static final long DOUBLE_TAP_MS = 400;
    private static final long REPEAT_START_MS = 400;
    private static final long REPEAT_MS = 50;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable repeatDelete = new Runnable() {
        @Override
        public void run() {
            deleteOne();
            handler.postDelayed(this, REPEAT_MS);
        }
    };

    private KlaviaturaView view;
    private List<List<Key>> letterRows;
    private List<List<Key>> symbolRows;
    private boolean symbols;
    private int shift = KlaviaturaView.SHIFT_OFF;
    private long lastShiftTap;
    private long lastSpaceTap;

    @Override
    public View onCreateInputView() {
        view = new KlaviaturaView(this);
        view.setListener(this);
        letterRows = Layouts.letters();
        symbolRows = Layouts.symbols();
        applyLayer();
        return view;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        symbols = cls == InputType.TYPE_CLASS_NUMBER
                || cls == InputType.TYPE_CLASS_PHONE
                || cls == InputType.TYPE_CLASS_DATETIME;
        shift = KlaviaturaView.SHIFT_OFF;
        if (view != null) {
            view.setEnterLabel(enterLabel(info));
            applyLayer();
        }
        updateAutoCaps();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        handler.removeCallbacks(repeatDelete);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        updateAutoCaps();
    }

    // ---- KlaviaturaView.Listener ----

    @Override
    public void onKeyDown(Key key) {
        if (key.type == Key.DELETE) {
            deleteOne();
            handler.removeCallbacks(repeatDelete);
            handler.postDelayed(repeatDelete, REPEAT_START_MS);
        }
    }

    @Override
    public void onKeyUp(Key key) {
        if (key.type == Key.DELETE) handler.removeCallbacks(repeatDelete);
    }

    @Override
    public void onKeyTap(Key key) {
        switch (key.type) {
            case Key.CHAR: {
                String t = key.text;
                if (!symbols && shift != KlaviaturaView.SHIFT_OFF) t = t.toUpperCase(Locale.ROOT);
                commit(t);
                if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
                break;
            }
            case Key.SHIFT: {
                long now = SystemClock.uptimeMillis();
                if (shift == KlaviaturaView.SHIFT_OFF) {
                    setShift(KlaviaturaView.SHIFT_ONCE);
                } else if (shift == KlaviaturaView.SHIFT_ONCE) {
                    setShift(now - lastShiftTap < DOUBLE_TAP_MS
                            ? KlaviaturaView.SHIFT_LOCK : KlaviaturaView.SHIFT_OFF);
                } else {
                    setShift(KlaviaturaView.SHIFT_OFF);
                }
                lastShiftTap = now;
                break;
            }
            case Key.TO_SYMBOLS:
                symbols = true;
                applyLayer();
                break;
            case Key.TO_LETTERS:
                symbols = false;
                applyLayer();
                updateAutoCaps();
                break;
            case Key.SPACE:
                handleSpace();
                break;
            case Key.ENTER:
                handleEnter();
                break;
            default:
                // DELETE bosilgan zahoti bajariladi (onKeyDown).
                break;
        }
    }

    @Override
    public boolean onKeyLongPress(Key key) {
        switch (key.type) {
            case Key.CHAR:
                if (key.alt == null) return false;
                commit(key.alt);
                if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
                return true;
            case Key.SPACE: {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
                return true;
            }
            case Key.SHIFT:
                setShift(KlaviaturaView.SHIFT_LOCK);
                return true;
            default:
                return false;
        }
    }

    // ---- Yordamçi metodlar ----

    private void applyLayer() {
        if (view == null) return;
        view.setRows(symbols ? symbolRows : letterRows, symbols);
        view.setShiftState(shift);
    }

    private void setShift(int s) {
        shift = s;
        if (view != null) view.setShiftState(s);
    }

    private void updateAutoCaps() {
        if (symbols || shift == KlaviaturaView.SHIFT_LOCK) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei == null || ic == null || ei.inputType == InputType.TYPE_NULL) return;
        int caps = ic.getCursorCapsMode(ei.inputType);
        setShift(caps != 0 ? KlaviaturaView.SHIFT_ONCE : KlaviaturaView.SHIFT_OFF);
    }

    private void commit(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    private void deleteOne() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    /** Ikki marta böş joy bosilsa — nuqta qöyiladi. */
    private void handleSpace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = SystemClock.uptimeMillis();
        boolean done = false;
        if (!symbols && now - lastSpaceTap < 700) {
            CharSequence before = ic.getTextBeforeCursor(2, 0);
            if (before != null && before.length() == 2 && before.charAt(1) == ' '
                    && Character.isLetterOrDigit(before.charAt(0))) {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(1, 0);
                ic.commitText(". ", 1);
                ic.endBatchEdit();
                done = true;
                lastSpaceTap = 0;
            }
        }
        if (!done) {
            ic.commitText(" ", 1);
            lastSpaceTap = now;
        }
    }

    private void handleEnter() {
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int action = ei == null ? EditorInfo.IME_ACTION_NONE : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean noAction = ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        if (!noAction && action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            sendKeyChar('\n');
        }
    }

    /** Enter tugmasidagi yozuv; null bölsa — ↵ ikonkasi. */
    private static String enterLabel(EditorInfo ei) {
        if (ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return null;
        switch (ei.imeOptions & EditorInfo.IME_MASK_ACTION) {
            case EditorInfo.IME_ACTION_GO:
                return "Ötiş";
            case EditorInfo.IME_ACTION_SEARCH:
                return "Qidir";
            case EditorInfo.IME_ACTION_SEND:
                return "Yubor";
            case EditorInfo.IME_ACTION_NEXT:
                return "Keyingi";
            case EditorInfo.IME_ACTION_DONE:
                return "Tayyor";
            default:
                return null;
        }
    }
}
