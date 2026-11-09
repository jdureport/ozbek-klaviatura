import re, os, sys, csv, io, zipfile, collections
ALPHA = set('abdefghijklmnopqrstuvxyzöğşçʼ')
APOS = "‘’ʻʼ'`´"

def conv(w):
    w = w.lower()
    w = re.sub('o[' + APOS + ']', 'ö', w)
    w = re.sub('g[' + APOS + ']', 'ğ', w)
    w = re.sub('[' + APOS + ']', 'ʼ', w)
    w = w.replace('sh', 'ş').replace('ch', 'ç')
    w = w.strip('ʼ-')
    if len(w) < 1 or not set(w) <= ALPHA or 'ʼʼ' in w:
        return None
    return w


if __name__ == '__main__':
    D = sys.argv[1]; OUT = sys.argv[2]
    order, seen = [], set()
    def add(w):
        if w and w not in seen:
            seen.add(w); order.append(w)

    # 1) qöl bilan yozilgan asosiy sözlar (öz işimiz)
    for line in open(os.path.join(os.path.dirname(__file__), 'uz_core.txt'), encoding='utf-8'):
        w = line.strip().lower()
        if w and set(w) <= ALPHA: add(w)
    core_n = len(order)

    # 2) CC0 qonunlar korpusidan söz şakllari çastotasi
    cnt = collections.Counter()
    raw = os.path.join(D, 'uzbek-legal-corpus/data/raw')
    tok = re.compile("[a-zA-Z" + APOS + "]+")
    for f in os.listdir(raw):
        for t in tok.findall(open(os.path.join(raw, f), encoding='utf-8').read()):
            w = conv(t)
            if w and len(w) >= 2: cnt[w] += 1
    for w, c in cnt.most_common():
        if c >= 2: add(w)
    corpus_n = len(order) - core_n

    # 3) MIT (UzbekLemma) va Apache-2.0 (UzbekLemmaStems-POS) lemma röyxatlari
    lem = set()
    sd = os.path.join(D, 'UzbekLemma/suzlar')
    for f in os.listdir(sd):
        for line in open(os.path.join(sd, f), encoding='utf-8', errors='ignore'):
            w = conv(line.strip().strip('\\').strip())
            if w and len(w) >= 2: lem.add(w)
    z = zipfile.ZipFile(os.path.join(D, 'UzbekLemmaStems-POS-Dataset/CSV_files.zip'))
    for name in z.namelist():
        if not name.endswith('.csv'): continue
        for row in csv.reader(io.TextIOWrapper(z.open(name), encoding='utf-8', errors='ignore')):
            for cell in row[:2]:
                for part in re.split(r'[\s,;/]+', cell):
                    w = conv(part)
                    if w and len(w) >= 2: lem.add(w)
    for w in sorted(lem, key=lambda s: (len(s), s)): add(w)
    print('core', core_n, 'corpus', corpus_n, 'lemmas', len(order) - core_n - corpus_n, 'total', len(order))
    order = order[:60000]
    open(OUT, 'w', encoding='utf-8').write('\n'.join(order) + '\n')
    print(order[:40]); print(order[core_n:core_n+40])
