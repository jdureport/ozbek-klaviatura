package uz.yangialifbo.klaviatura;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Klaviatura servisi: tugma bosişlarini matnga aylantiradi. */
public class KlaviaturaService extends InputMethodService implements KlaviaturaView.Listener {

    private static final long DOUBLE_TAP_MS = 400;
    private static final long REPEAT_START_MS = 400;
    private static final long REPEAT_MS = 50;
    private static final long MULTITAP_MS = 900;
    private static final String PUNCT = ".,?!-:;";
    private static final int SHOWN = 3;

    // ---- Luğatlar (barça oynalar uçun umumiy) ----
    private static final Map<String, Dictionary> DICTS = new HashMap<>();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Luğatsiz tillar (kod rejimi) uçun böş luğat. */
    private static final Dictionary NONE = new Dictionary("none", false);

    static synchronized Dictionary dictionary(Context context, String name) {
        if (name == null) return NONE;
        Dictionary d = DICTS.get(name);
        if (d == null) {
            d = new Dictionary(name, "uz".equals(name));
            DICTS.put(name, d);
            final Dictionary fd = d;
            final Context app = context.getApplicationContext();
            IO.execute(() -> fd.load(app));
        }
        return d;
    }

    static void clearLearned(Context context) {
        final Context app = context.getApplicationContext();
        for (String name : new String[]{"uz", "kaa", "en", "ru"}) {
            dictionary(app, name);
        }
        IO.execute(() -> {
            synchronized (KlaviaturaService.class) {
                for (Dictionary d : DICTS.values()) d.clearLearned(app);
            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Bosib turilganda takrorlanadigan tugma (⌫, ←, →). */
    private Key repeatKey;
    private final Runnable repeatDelete = new Runnable() {
        @Override
        public void run() {
            if (repeatKey != null) repeatAction(repeatKey);
            handler.postDelayed(this, REPEAT_MS);
        }
    };

    private KlaviaturaView view;
    private final Map<String, Lang> langs = new HashMap<>();
    private Lang lang;
    private Dictionary dict;
    private boolean symbols;
    private int shift = KlaviaturaView.SHIFT_OFF;
    private long lastShiftTap;
    private long lastSpaceTap;

    // Joriy maydon
    private boolean suggestOn;      // takliflar körsatiladimi
    private boolean learnOn;        // sözlar eslab qolinadimi
    private boolean forceMultitap;  // parol maydonida T9 luğatsiz
    /** Raqamli klaviatura: 0 — yöq, 1 — son/sana/kod, 2 — telefon. */
    private int pad;
    private boolean padOff;         // foydalanuvçi harflarga ötdi
    private int builtVersion = -1;  // ranglar va joylaşuv versiyasi

    // T9 holati
    private boolean t9Predictive = true;
    private final StringBuilder t9Seq = new StringBuilder();
    private List<String> t9Cands = new ArrayList<>();
    private String t9Raw = "";
    private boolean capFirst, capAll;
    private final StringBuilder mtWord = new StringBuilder();
    private char mtLastDigit;
    private int mtIndex;
    private long mtTime;
    private long lastPunctTime;
    private int punctIndex;
    private boolean punctActive;

    /** Takliflar qatoridagi sözlar (kiçik harfda). */
    private List<String> shownRaw = new ArrayList<>();

    // ---- Bufer (nusxalangan matnlar faqat xotirada, diskka yozilmaydi) ----
    private static final int CLIP_MAX = 20;
    private final java.util.LinkedList<String> clips = new java.util.LinkedList<>();
    private ClipboardManager clipboard;
    private boolean clipMode;
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = this::captureClip;

    // ---- Hayot sikli ----

    @Override
    public void onCreate() {
        super.onCreate();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.addPrimaryClipChangedListener(clipListener);
    }

    @Override
    public void onDestroy() {
        if (clipboard != null) clipboard.removePrimaryClipChangedListener(clipListener);
        super.onDestroy();
    }

    /** Joriy buferdagi matnni röyxatga qöşiş (parol kabi maxfiy belgilangan matnlardan taşqari). */
    private void captureClip() {
        if (clipboard == null) return;
        try {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            ClipDescription desc = clip.getDescription();
            if (desc != null && Build.VERSION.SDK_INT >= 24) {
                PersistableBundle extras = desc.getExtras();
                if (extras != null && extras.getBoolean("android.content.extra.IS_SENSITIVE", false)) return;
            }
            CharSequence t = clip.getItemAt(0).coerceToText(this);
            if (t == null) return;
            String s = t.toString();
            if (s.trim().isEmpty()) return;
            clips.remove(s);
            clips.addFirst(s);
            while (clips.size() > CLIP_MAX) clips.removeLast();
        } catch (RuntimeException ignored) {
            // bufer öqib bölmasa — eʼtiborsiz
        }
    }

    private List<List<Key>> clipRows() {
        captureClip();
        List<List<Key>> rows = new ArrayList<>();
        int shown = Math.min(6, clips.size());
        if (shown == 0) {
            Key empty = new Key(Key.CLIP, "Bufer böş — biror matnni nusxalang", null, 2f);
            empty.index = -1;
            List<Key> r = new ArrayList<>();
            r.add(empty);
            rows.add(r);
            rows.add(new ArrayList<>(java.util.Collections.singletonList(blankClip())));
            rows.add(new ArrayList<>(java.util.Collections.singletonList(blankClip())));
        } else {
            for (int r = 0; r < 3; r++) {
                List<Key> row = new ArrayList<>();
                for (int c = 0; c < 2; c++) {
                    int i = r * 2 + c;
                    if (i < shown) {
                        Key k = new Key(Key.CLIP, clips.get(i), null, 1f);
                        k.index = i;
                        row.add(k);
                    } else {
                        Key k = new Key(Key.CLIP, "", null, 1f);
                        k.index = -1;
                        row.add(k);
                    }
                }
                rows.add(row);
            }
        }
        List<Key> bottom = new ArrayList<>();
        bottom.add(Key.special(Key.TO_LETTERS, lang.t9 ? "T9" : lang.abc, 1f));
        bottom.add(Key.special(Key.CLIP_CLEAR, "Tozalaş", 1f));
        rows.add(bottom);
        return rows;
    }

    private static Key blankClip() {
        Key k = new Key(Key.CLIP, "", null, 2f);
        k.index = -1;
        return k;
    }

    private void openSettings() {
        finishComposing();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        requestHideSelf(0);
    }

    @Override
    public View onCreateInputView() {
        view = new KlaviaturaView(this);
        view.setListener(this);
        builtVersion = Prefs.version(this);
        selectLang(Prefs.currentLang(this), false);
        return view;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        resetComposingState();
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        boolean password = cls == InputType.TYPE_CLASS_TEXT
                && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
        boolean noSuggest = (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0;
        boolean textClass = cls == InputType.TYPE_CLASS_TEXT;
        symbols = false;
        padOff = false;
        clipMode = false;
        pad = cls == InputType.TYPE_CLASS_PHONE ? 2
                : (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_DATETIME) ? 1 : 0;
        suggestOn = Prefs.suggestions(this) && textClass && !password && !noSuggest;
        learnOn = suggestOn
                && variation != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                && variation != InputType.TYPE_TEXT_VARIATION_URI;
        forceMultitap = password;
        t9Predictive = Prefs.t9Predictive(this);
        shift = KlaviaturaView.SHIFT_OFF;

        // Sozlamalarda ranglar yoki joylaşuv özgargan bölsa — qayta quramiz.
        int v = Prefs.version(this);
        if (v != builtVersion) {
            builtVersion = v;
            langs.clear();
            if (view != null) {
                view.applyTheme(Theme.load(this));
                view.loadSizes();
            }
        }

        String current = Prefs.currentLang(this);
        if (!Prefs.enabledLangs(this).contains(current)) current = Prefs.enabledLangs(this).get(0);
        selectLang(current, false);
        if (view != null) view.setEnterLabel(enterLabel(info));
        updateAutoCaps();
        refreshSuggestions();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        handler.removeCallbacks(repeatDelete);
        finishComposing();
        saveDictionaries();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        if (isComposing()) {
            // Foydalanuvçi kursorni boşqa joyga surdi — sözni tugatamiz.
            // (candidatesEnd < 0 — eski, keçikib kelgan xabar, uni eʼtiborsiz qoldiramiz.)
            if (candidatesEnd >= 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.finishComposingText();
                resetComposingState();
                updateAutoCaps();
                refreshSuggestions();
            }
            return;
        }
        updateAutoCaps();
        refreshSuggestions();
    }

    // ---- Til ----

    private void selectLang(String id, boolean remember) {
        Lang l = langs.get(id);
        if (l == null) {
            l = Lang.create(this, id);
            langs.put(id, l);
        }
        lang = l;
        dict = dictionary(this, l.dict);
        if (remember) Prefs.setCurrentLang(this, l.id);
        applyLayer();
    }

    private boolean inPad() {
        return pad != 0 && !padOff;
    }

    private void nextLang() {
        finishComposing();
        List<String> enabled = Prefs.enabledLangs(this);
        if (enabled.size() <= 1) {
            switchToNextInputMethod(false);
            return;
        }
        int i = enabled.indexOf(lang.id);
        String next = enabled.get((i + 1) % enabled.size());
        selectLang(next, true);
        EditorInfo ei = getCurrentInputEditorInfo();
        boolean textField = ei != null && (ei.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT;
        suggestOn = Prefs.suggestions(this) && textField && !forceMultitap
                && (ei.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0;
        learnOn = suggestOn;
        updateAutoCaps();
        refreshSuggestions();
    }

    private boolean t9Layout() {
        return lang.t9 && !symbols && !inPad() && !clipMode;
    }

    private boolean predictive() {
        return t9Predictive && !forceMultitap;
    }

    // ---- KlaviaturaView.Listener ----

    @Override
    public void onKeyDown(Key key) {
        if (key.type == Key.DELETE || key.type == Key.LEFT || key.type == Key.RIGHT) {
            repeatAction(key);
            repeatKey = key;
            handler.removeCallbacks(repeatDelete);
            handler.postDelayed(repeatDelete, REPEAT_START_MS);
        }
    }

    @Override
    public void onKeyUp(Key key) {
        if (key == repeatKey) {
            handler.removeCallbacks(repeatDelete);
            repeatKey = null;
        }
    }

    private void repeatAction(Key key) {
        if (key.type == Key.DELETE) {
            deleteOne();
        } else {
            finishComposing();
            sendDownUpKeyEvents(key.type == Key.LEFT ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
        }
    }

    @Override
    public void onKeyTap(Key key) {
        if (key.type != Key.T9) punctActive = false;
        switch (key.type) {
            case Key.CHAR:
                typeChar(key.text);
                break;
            case Key.T9:
                t9Digit(key.text.charAt(0));
                break;
            case Key.SHIFT:
                tapShift();
                break;
            case Key.TO_SYMBOLS:
                finishComposing();
                symbols = true;
                applyLayer();
                refreshSuggestions();
                break;
            case Key.TO_LETTERS:
                symbols = false;
                if (!clipMode) padOff = true;
                clipMode = false;
                applyLayer();
                updateAutoCaps();
                refreshSuggestions();
                break;
            case Key.LANG:
                nextLang();
                break;
            case Key.T9MODE:
                finishComposing();
                t9Predictive = !t9Predictive;
                Prefs.setT9Predictive(this, t9Predictive);
                applyLayer();
                refreshSuggestions();
                break;
            case Key.SPACE:
                handleSpace();
                break;
            case Key.ENTER:
                finishComposing();
                handleEnter();
                break;
            case Key.SUGGESTION:
                pickSuggestion(key.index);
                break;
            case Key.TOOL_CLIP:
                finishComposing();
                clipMode = true;
                applyLayer();
                break;
            case Key.TOOL_SETTINGS:
                openSettings();
                break;
            case Key.CLIP:
                if (key.index >= 0 && key.index < clips.size()) {
                    commit(clips.get(key.index));
                    clipMode = false;
                    applyLayer();
                    updateAutoCaps();
                    refreshSuggestions();
                }
                break;
            case Key.CLIP_CLEAR:
                clips.clear();
                applyLayer();
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
                learnWordBeforeCursor();
                commit(key.alt);
                if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
                return true;
            case Key.T9:
                finishComposing();
                commit(key.text);
                return true;
            case Key.SPACE:
            case Key.LANG: {
                finishComposing();
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

    // ---- Oddiy klaviatura ----

    private void typeChar(String text) {
        boolean letter = text.length() == 1 && lang.isWordChar(text.charAt(0)) && !symbols && !inPad();
        if (!letter) learnWordBeforeCursor();
        String t = text;
        if (!symbols && !inPad() && shift != KlaviaturaView.SHIFT_OFF) t = lang.upper(t);
        commit(t);
        if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
    }

    private void tapShift() {
        if (t9Layout() && isComposing()) {
            // T9 da yozilayotgan sözning şakli: kiçik → Boş harf → HAMMASI KATTA
            if (!capFirst && !capAll) capFirst = true;
            else if (capFirst && !capAll) capAll = true;
            else capFirst = capAll = false;
            showComposing();
            showSuggestionsFromRaw(predictive() ? t9Cands : shownRaw);
            return;
        }
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
    }

    /** Kursor oldidagi tugallanmagan söz. */
    private String wordBeforeCursor() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence before = ic.getTextBeforeCursor(48, 0);
        if (before == null) return "";
        int i = before.length();
        while (i > 0 && lang.isWordChar(before.charAt(i - 1))) i--;
        String w = before.subSequence(i, before.length()).toString();
        // söz boşi va oxiridagi tutuq belgisi sözga kirmaydi
        while (!w.isEmpty() && !Character.isLetter(w.charAt(0))) w = w.substring(1);
        return w;
    }

    private void learnWordBeforeCursor() {
        if (!learnOn || symbols || t9Layout()) return;
        String w = wordBeforeCursor();
        if (w.length() >= 2) dict.learn(lang.lower(w));
    }

    private void refreshSuggestions() {
        if (view == null) return;
        view.setStripVisible(true);
        if (!suggestOn || symbols || clipMode || !dict.isReady()) {
            setShown(new ArrayList<>(), "");
            return;
        }
        if (t9Layout()) {
            if (!isComposing()) setShown(new ArrayList<>(), "");
            return;
        }
        String typed = wordBeforeCursor();
        if (typed.isEmpty()) {
            setShown(new ArrayList<>(), "");
            return;
        }
        setShown(dict.complete(lang.lower(typed), SHOWN), typed);
    }

    private void setShown(List<String> raw, String typed) {
        shownRaw = raw;
        List<String> display = new ArrayList<>(raw.size());
        for (String w : raw) display.add(lang.matchCase(typed, w));
        if (view != null) view.setSuggestions(display);
    }

    private void pickSuggestion(int index) {
        if (index >= shownRaw.size()) return;
        String raw = shownRaw.get(index);
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (t9Layout() && isComposing()) {
            String w = applyCase(raw);
            ic.commitText(w + " ", 1);
            if (learnOn) dict.learn(raw);
            resetComposingState();
            refreshSuggestions();
            return;
        }
        String typed = wordBeforeCursor();
        ic.beginBatchEdit();
        if (!typed.isEmpty()) ic.deleteSurroundingText(typed.length(), 0);
        ic.commitText(lang.matchCase(typed, raw) + " ", 1);
        ic.endBatchEdit();
        if (learnOn) dict.learn(raw);
        if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
    }

    // ---- T9 ----

    private boolean isComposing() {
        return t9Seq.length() > 0 || mtWord.length() > 0;
    }

    private void resetComposingState() {
        t9Seq.setLength(0);
        t9Cands = new ArrayList<>();
        t9Raw = "";
        mtWord.setLength(0);
        mtLastDigit = 0;
        capFirst = capAll = false;
    }

    /** Yozilayotgan sözni matnga qöyib, holatni tozalaş. */
    private void finishComposing() {
        if (!isComposing()) return;
        InputConnection ic = getCurrentInputConnection();
        String raw = mtWord.length() > 0 ? mtWord.toString() : t9Raw;
        if (ic != null) ic.commitText(applyCase(raw), 1);
        learnComposed(raw);
        resetComposingState();
    }

    private void learnComposed(String raw) {
        if (!learnOn || raw.length() < 2) return;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!Character.isLetter(c) && c != 'ʼ') return;
        }
        // T9 da faqat luğatdan tanlangan yoki harfma-harf yozilgan sözlar eslab qolinadi.
        if (mtWord.length() > 0 || t9Cands.contains(raw)) dict.learn(raw);
    }

    private String applyCase(String raw) {
        if (capAll) return lang.upper(raw);
        if (capFirst) return lang.capitalize(raw);
        return raw;
    }

    private void captureCaps() {
        capFirst = shift != KlaviaturaView.SHIFT_OFF;
        capAll = shift == KlaviaturaView.SHIFT_LOCK;
        if (shift == KlaviaturaView.SHIFT_ONCE) setShift(KlaviaturaView.SHIFT_OFF);
    }

    private void t9Digit(char d) {
        if (predictive()) {
            if (d == '1' && (t9Seq.length() == 0 || dict.t9(t9Seq + "1", 1).isEmpty())) {
                finishComposing();
                punctTap();
                return;
            }
            punctActive = false;
            if (t9Seq.length() == 0) captureCaps();
            t9Seq.append(d);
            refreshT9();
        } else {
            multitap(d);
        }
    }

    private void refreshT9() {
        String seq = t9Seq.toString();
        t9Cands = dict.isReady() ? dict.t9(seq, 8) : new ArrayList<>();
        if (!t9Cands.isEmpty()) {
            t9Raw = t9Cands.get(0);
        } else {
            // Luğatda yöq — har tugmaning birinçi harfi bilan
            StringBuilder b = new StringBuilder(t9Raw.length() > seq.length()
                    ? t9Raw.substring(0, seq.length()) : t9Raw);
            for (int i = b.length(); i < seq.length(); i++) b.append(firstLetter(seq.charAt(i)));
            t9Raw = b.toString();
        }
        showComposing();
        showSuggestionsFromRaw(t9Cands);
    }

    private void showSuggestionsFromRaw(List<String> raw) {
        if (!suggestOn) return;
        List<String> top = new ArrayList<>(raw.subList(0, Math.min(SHOWN, raw.size())));
        shownRaw = top;
        List<String> display = new ArrayList<>();
        for (String w : top) display.add(applyCase(w));
        if (view != null) view.setSuggestions(display);
    }

    private void showComposing() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String raw = mtWord.length() > 0 ? mtWord.toString() : t9Raw;
        ic.setComposingText(applyCase(raw), 1);
    }

    private static char firstLetter(char digit) {
        String letters = digit == '1' ? "ʼ" : Layouts.T9_LETTERS[digit - '0'];
        return letters.charAt(0);
    }

    private String multitapLetters(char d) {
        return d == '1' ? "ʼ.,?!-" : Layouts.T9_LETTERS[d - '0'];
    }

    private void multitap(char d) {
        String letters = multitapLetters(d);
        long now = SystemClock.uptimeMillis();
        if (d == mtLastDigit && now - mtTime < MULTITAP_MS && mtWord.length() > 0) {
            mtIndex = (mtIndex + 1) % letters.length();
            mtWord.setCharAt(mtWord.length() - 1, letters.charAt(mtIndex));
        } else {
            if (mtWord.length() == 0) captureCaps();
            mtIndex = 0;
            mtWord.append(letters.charAt(0));
        }
        mtLastDigit = d;
        mtTime = now;
        showComposing();
        if (suggestOn && dict.isReady()) {
            showSuggestionsFromRaw(dict.complete(mtWord.toString(), SHOWN));
        }
    }

    /** "1" tugmasi: tinis belgisi; tez-tez bosilsa — keyingi belgiga almaşadi. */
    private void punctTap() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = SystemClock.uptimeMillis();
        if (punctActive && now - lastPunctTime < MULTITAP_MS) {
            ic.deleteSurroundingText(1, 0);
            punctIndex = (punctIndex + 1) % PUNCT.length();
        } else {
            punctIndex = 0;
        }
        ic.commitText(String.valueOf(PUNCT.charAt(punctIndex)), 1);
        lastPunctTime = now;
        punctActive = true;
    }

    // ---- Umumiy amallar ----

    private void applyLayer() {
        if (view == null || lang == null) return;
        if (clipMode) {
            view.setRows(clipRows(), true, lang);
        } else if (inPad()) {
            view.setRows(pad == 2 ? lang.phonePad : lang.numberPad, true, lang);
        } else {
            view.setRows(symbols ? lang.symbols : lang.letters, symbols, lang);
        }
        view.setShiftState(shift);
        view.setSpaceLabel(lang.label);
        view.setModeLabel(predictive() ? "T9" : "abc");
    }

    private void setShift(int s) {
        shift = s;
        if (view != null) view.setShiftState(s);
    }

    private void updateAutoCaps() {
        if (symbols || shift == KlaviaturaView.SHIFT_LOCK || isComposing()) return;
        if (inPad()) return;
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
        punctActive = false;
        if (t9Layout() && isComposing()) {
            if (mtWord.length() > 0) {
                mtWord.setLength(mtWord.length() - 1);
                mtLastDigit = 0;
                if (mtWord.length() == 0) {
                    clearComposingText();
                } else {
                    showComposing();
                    if (suggestOn && dict.isReady()) {
                        showSuggestionsFromRaw(dict.complete(mtWord.toString(), SHOWN));
                    }
                }
            } else {
                t9Seq.setLength(t9Seq.length() - 1);
                if (t9Seq.length() == 0) clearComposingText();
                else refreshT9();
            }
            return;
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
    }

    private void clearComposingText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText("", 1);
            ic.finishComposingText();
        }
        resetComposingState();
        updateAutoCaps();
        if (view != null) view.setSuggestions(new ArrayList<>());
        shownRaw = new ArrayList<>();
    }

    /** Böş joy; ikki marta bosilsa — nuqta qöyiladi. */
    private void handleSpace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (t9Layout() && isComposing()) {
            String raw = mtWord.length() > 0 ? mtWord.toString() : t9Raw;
            ic.commitText(applyCase(raw) + " ", 1);
            learnComposed(raw);
            resetComposingState();
            lastSpaceTap = SystemClock.uptimeMillis();
            return;
        }
        learnWordBeforeCursor();
        long now = SystemClock.uptimeMillis();
        boolean done = false;
        if (!symbols && !inPad() && now - lastSpaceTap < 700) {
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
        learnWordBeforeCursor();
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

    private void saveDictionaries() {
        final Context app = getApplicationContext();
        IO.execute(() -> {
            List<Dictionary> list;
            synchronized (KlaviaturaService.class) {
                list = new ArrayList<>(DICTS.values());
            }
            for (Dictionary d : list) d.save(app);
        });
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
