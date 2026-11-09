"""Keyingi söz bazasi: qöl bilan tuzilgan juftliklar (+ özbekça uçun CC0 korpus juftliklari)."""
import collections, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
OUT = sys.argv[1]            # app/src/main/assets/dict
CORPUS = sys.argv[2] if len(sys.argv) > 2 else None  # uzbek-legal-corpus/data/raw
HERE = os.path.dirname(os.path.abspath(__file__))
TOP = 4

def load_pairs(path, lower):
    nxt = collections.OrderedDict()
    for line in open(path, encoding='utf-8'):
        p = line.split()
        if len(p) != 2: continue
        a, b = lower(p[0]), lower(p[1])
        nxt.setdefault(a, [])
        if b not in nxt[a]: nxt[a].append(b)
    return nxt

def write(name, nxt):
    with open(os.path.join(OUT, name), 'w', encoding='utf-8') as f:
        for a, lst in nxt.items():
            if lst: f.write(a + '\t' + ' '.join(lst[:TOP]) + '\n')
    print(name, len(nxt))

low = lambda s: s.lower()
uz = load_pairs(os.path.join(HERE, 'uz_pairs.txt'), low)
if CORPUS:
    from prep_uz_free import conv  # noqa: E402  (bir xil alifbo öğiriş)
    words = set(l.strip() for l in open(os.path.join(OUT, 'uz.txt'), encoding='utf-8'))
    big = collections.Counter()
    APOS = "‘’ʻʼ'`´"
    tok = re.compile("[a-zA-Z" + APOS + "]+")
    for f in os.listdir(CORPUS):
        text = open(os.path.join(CORPUS, f), encoding='utf-8').read()
        for sent in re.split(r'[.!?;:\n()\[\]«»"]+', text):
            ws = [conv(t) for t in tok.findall(sent)]
            for a, b in zip(ws, ws[1:]):
                if a and b and a in words and b in words: big[(a, b)] += 1
    per = collections.defaultdict(list)
    for (a, b), c in big.most_common():
        if c < 3: break
        if len(per[a]) < TOP: per[a].append(b)
    for a, lst in per.items():
        uz.setdefault(a, [])
        for b in lst:
            if b not in uz[a]: uz[a].append(b)
write('uz_next.txt', uz)
write('en_next.txt', load_pairs(os.path.join(HERE, 'en_pairs.txt'), low))
write('ru_next.txt', load_pairs(os.path.join(HERE, 'ru_pairs.txt'), low))
