package uz.yangialifbo.klaviatura;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Söz luğati: takliflar va T9 uçun.
 * Asosiy luğat assets/dict/&lt;til&gt;.txt (sözlar köp işlatiliş tartibida),
 * foydalanuvçi yozgan sözlar filesDir/user_&lt;til&gt;.txt da saqlanadi.
 */
final class Dictionary {

    private final String name;
    private final boolean withT9;

    private String[] words = new String[0];   // alifbo tartibida
    private int[] scores = new int[0];        // katta — köp işlatiladi
    private String[] t9Seq = new String[0];   // T9 raqamlar ketma-ketligi, saralangan
    private int[] t9Word = new int[0];        // t9Seq[i] → words dagi indeks
    private final Map<String, Integer> user = new HashMap<>();
    /** Keyingi söz: boşlanğiç baza (assets/dict/<til>_next.txt) va foydalanuvçidan örganilgan juftliklar. */
    private final Map<String, String[]> seedNext = new HashMap<>();
    private final Map<String, Map<String, Integer>> userNext = new HashMap<>();
    private int boost = 1000;
    private volatile boolean ready;
    private boolean dirty;

    Dictionary(String name, boolean withT9) {
        this.name = name;
        this.withT9 = withT9;
    }

    boolean isReady() {
        return ready;
    }

    /** Fon oqimida çaqiriladi. */
    void load(Context context) {
        List<String> list = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                context.getAssets().open("dict/" + name + ".txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
        } catch (IOException ignored) {
            // Luğat bölmasa ham klaviatura işlayveradi.
        }
        int n = list.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        final List<String> src = list;
        Arrays.sort(order, (a, b) -> src.get(a).compareTo(src.get(b)));
        String[] w = new String[n];
        int[] s = new int[n];
        for (int i = 0; i < n; i++) {
            w[i] = list.get(order[i]);
            s[i] = n - order[i];
        }
        String[] seq = new String[0];
        int[] seqWord = new int[0];
        if (withT9) {
            final String[] codes = new String[n];
            int count = 0;
            for (int i = 0; i < n; i++) {
                codes[i] = t9Code(w[i]);
                if (codes[i] != null) count++;
            }
            Integer[] idx = new Integer[count];
            int k = 0;
            for (int i = 0; i < n; i++) if (codes[i] != null) idx[k++] = i;
            Arrays.sort(idx, (a, b) -> codes[a].compareTo(codes[b]));
            seq = new String[count];
            seqWord = new int[count];
            for (int i = 0; i < count; i++) {
                seq[i] = codes[idx[i]];
                seqWord[i] = idx[i];
            }
        }
        Map<String, Integer> learned = readUserFile(context);
        Map<String, String[]> seeds = new HashMap<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                context.getAssets().open("dict/" + name + "_next.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) seeds.put(line.substring(0, tab), line.substring(tab + 1).trim().split(" "));
            }
        } catch (IOException ignored) {
            // bu til uçun boşlanğiç baza yöq
        }
        Map<String, Map<String, Integer>> learnedNext = readPairsFile(context);
        synchronized (this) {
            words = w;
            scores = s;
            t9Seq = seq;
            t9Word = seqWord;
            boost = Math.max(1000, n / 8);
            user.putAll(learned);
            seedNext.putAll(seeds);
            for (Map.Entry<String, Map<String, Integer>> e : learnedNext.entrySet()) {
                userNext.put(e.getKey(), e.getValue());
            }
        }
        ready = true;
    }

    /** Söz → T9 raqamlari ("salom" → "72566"); mos kelmasa null. */
    static String t9Code(String word) {
        StringBuilder b = new StringBuilder(word.length());
        outer:
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            for (int d = 1; d < Layouts.T9_LETTERS.length; d++) {
                if (Layouts.T9_LETTERS[d].indexOf(c) >= 0) {
                    b.append((char) ('0' + d));
                    continue outer;
                }
            }
            return null;
        }
        return b.toString();
    }

    /** Kiçik harfdagi boşlanişga mos eng köp işlatiladigan sözlar. */
    synchronized List<String> complete(String prefix, int max) {
        List<String> out = new ArrayList<>();
        List<Integer> outScore = new ArrayList<>();
        if (prefix.isEmpty()) return out;
        int i = lowerBound(words, prefix);
        for (; i < words.length && words[i].startsWith(prefix); i++) {
            Integer u = user.get(words[i]);
            int score = scores[i] + (u == null ? 0 : u * boost);
            insert(out, outScore, words[i], score, max);
        }
        for (Map.Entry<String, Integer> e : user.entrySet()) {
            String w = e.getKey();
            if (w.startsWith(prefix) && Arrays.binarySearch(words, w) < 0) {
                insert(out, outScore, w, e.getValue() * boost, max);
            }
        }
        return out;
    }

    /** T9: raqamlar ketma-ketligiga mos sözlar. Aynan uzunligi teng sözlar oldinda. */
    synchronized List<String> t9(String seq, int max) {
        List<String> out = new ArrayList<>();
        List<Integer> outScore = new ArrayList<>();
        if (seq.isEmpty()) return out;
        int big = Integer.MAX_VALUE / 4;
        int i = lowerBound(t9Seq, seq);
        for (; i < t9Seq.length && t9Seq[i].startsWith(seq); i++) {
            int wi = t9Word[i];
            Integer u = user.get(words[wi]);
            int score = scores[wi] + (u == null ? 0 : u * boost);
            if (t9Seq[i].length() == seq.length()) score += big;
            insert(out, outScore, words[wi], score, max);
        }
        for (Map.Entry<String, Integer> e : user.entrySet()) {
            String w = e.getKey();
            String code = t9Code(w);
            if (code != null && code.startsWith(seq) && Arrays.binarySearch(words, w) < 0) {
                int score = e.getValue() * boost + (code.length() == seq.length() ? big : 0);
                insert(out, outScore, w, score, max);
            }
        }
        return out;
    }

    /** Oldingi sözdan keyin kelişi mumkin bölgan sözlar. */
    synchronized List<String> next(String prev, int max) {
        List<String> out = new ArrayList<>();
        List<Integer> outScore = new ArrayList<>();
        Map<String, Integer> u = userNext.get(prev);
        if (u != null) {
            for (Map.Entry<String, Integer> e : u.entrySet()) {
                insert(out, outScore, e.getKey(), 1000 + e.getValue() * 100, max);
            }
        }
        String[] s = seedNext.get(prev);
        if (s != null) {
            for (int i = 0; i < s.length; i++) insert(out, outScore, s[i], 500 - i, max);
        }
        return out;
    }

    /** "prev word" juftligini eslab qoliş. */
    synchronized void learnPair(String prev, String w) {
        if (prev.isEmpty() || w.length() < 1 || prev.length() > 40 || w.length() > 40) return;
        Map<String, Integer> m = userNext.get(prev);
        if (m == null) {
            if (userNext.size() > 20000) return;
            m = new HashMap<>();
            userNext.put(prev, m);
        }
        Integer c = m.get(w);
        m.put(w, c == null ? 1 : Math.min(c + 1, 50));
        if (m.size() > 12) {
            // eng kam işlatilganini olib taşlaymiz
            String min = null;
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                if (!e.getKey().equals(w) && (min == null || e.getValue() < m.get(min))) min = e.getKey();
            }
            if (min != null) m.remove(min);
        }
        dirty = true;
    }

    synchronized boolean contains(String w) {
        return Arrays.binarySearch(words, w) >= 0 || user.containsKey(w);
    }

    /** Foydalanuvçi yozgan sözni eslab qoliş. */
    synchronized void learn(String w) {
        if (w.length() < 2 || w.length() > 40) return;
        Integer c = user.get(w);
        user.put(w, c == null ? 1 : Math.min(c + 1, 50));
        dirty = true;
    }

    synchronized void clearLearned(Context context) {
        user.clear();
        userNext.clear();
        dirty = false;
        //noinspection ResultOfMethodCallIgnored
        userFile(context).delete();
        //noinspection ResultOfMethodCallIgnored
        pairsFile(context).delete();
    }

    /** Fon oqimida çaqiriladi. */
    void save(Context context) {
        Map<String, Integer> copy;
        StringBuilder pairs = new StringBuilder();
        synchronized (this) {
            if (!dirty) return;
            copy = new HashMap<>(user);
            for (Map.Entry<String, Map<String, Integer>> e : userNext.entrySet()) {
                for (Map.Entry<String, Integer> n : e.getValue().entrySet()) {
                    pairs.append(e.getKey()).append('\t').append(n.getKey()).append('\t')
                            .append(n.getValue()).append('\n');
                }
            }
            dirty = false;
        }
        File pf = pairsFile(context);
        File ptmp = new File(pf.getPath() + ".tmp");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(ptmp), StandardCharsets.UTF_8)) {
            out.write(pairs.toString());
        } catch (IOException ignored) {
            // juftliklarni saqlab bölmasa — keyingi safar
        }
        //noinspection ResultOfMethodCallIgnored
        ptmp.renameTo(pf);
        File f = userFile(context);
        File tmp = new File(f.getPath() + ".tmp");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Integer> e : copy.entrySet()) {
                out.write(e.getKey());
                out.write('\t');
                out.write(Integer.toString(e.getValue()));
                out.write('\n');
            }
        } catch (IOException e) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        tmp.renameTo(f);
    }

    private Map<String, Integer> readUserFile(Context context) {
        Map<String, Integer> m = new HashMap<>();
        File f = userFile(context);
        if (!f.exists()) return m;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab <= 0) continue;
                try {
                    m.put(line.substring(0, tab), Integer.parseInt(line.substring(tab + 1).trim()));
                } catch (NumberFormatException ignored) {
                    // buzilgan qatorni tuşirib qoldiramiz
                }
            }
        } catch (IOException ignored) {
            // fayl öqilmasa — böş
        }
        return m;
    }

    private Map<String, Map<String, Integer>> readPairsFile(Context context) {
        Map<String, Map<String, Integer>> m = new HashMap<>();
        File f = pairsFile(context);
        if (!f.exists()) return m;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length != 3) continue;
                try {
                    Map<String, Integer> inner = m.get(p[0]);
                    if (inner == null) {
                        inner = new HashMap<>();
                        m.put(p[0], inner);
                    }
                    inner.put(p[1], Integer.parseInt(p[2].trim()));
                } catch (NumberFormatException ignored) {
                    // buzilgan qator
                }
            }
        } catch (IOException ignored) {
            // fayl öqilmasa — böş
        }
        return m;
    }

    private File pairsFile(Context context) {
        return new File(context.getFilesDir(), "juft_" + name + ".txt");
    }

    private File userFile(Context context) {
        return new File(context.getFilesDir(), "user_" + name + ".txt");
    }

    private static int lowerBound(String[] a, String key) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid].compareTo(key) < 0) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private static void insert(List<String> out, List<Integer> outScore, String w, int score, int max) {
        int pos = out.indexOf(w);
        if (pos >= 0) {
            if (outScore.get(pos) >= score) return;
            out.remove(pos);
            outScore.remove(pos);
        }
        int at = 0;
        while (at < outScore.size() && outScore.get(at) >= score) at++;
        if (at >= max) return;
        out.add(at, w);
        outScore.add(at, score);
        if (out.size() > max) {
            out.remove(out.size() - 1);
            outScore.remove(outScore.size() - 1);
        }
    }

    static List<String> empty() {
        return Collections.emptyList();
    }
}
