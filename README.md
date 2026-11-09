# Özbek klaviatura (yangi alifbo)

Özbek tilining 2026-yilgi lotin alifbosi uçun Android klaviaturasi: 28 harf va ʼ belgisi.

```
1 2 3 4 5 6 7 8 9 0
q e r t y u i o ö p
a s ş d f g ğ h j k l
⇧ z x ç v b n m ʼ ⌫
123  😊  ,  [◂ til ▸]  .  ↵
```

## Yuklab oliş

Har bir özgarişdan keyin GitHub Actions APK'ni avtomatik yiğadi. Tayyor faylni **Releases** böliminden `ozbek-klaviatura.apk` nomi bilan yuklab olasiz.

## Imkoniyatlar

- ⇧ bir marta — bitta boş harf, ikki marta — doimiy boş harf
- Gap boşida boş harf avtomatik yoqiladi
- Yuqori qatordagi harfni bosib turiş — raqam
- Böş joyni ikki marta bosiş — nuqta
- Böş joyni bosib turiş — boşqa klaviaturaga ötiş
- Yoruğ va qorönği mavzu
- Tillar: özbekça, özbekça T9, qoraqalpoqça, inglizça, rusça (🌐 tugmasi)
- Söz takliflari va yangi sözlarni eslab qoliş
- Raqam, telefon va SMS-kod maydonlari uçun katta raqamli klaviatura
- Ranglarni tanlaş: tayyor mavzular va maxsus ranglar
- Harf joylarini özgartiriş (har bir til uçun)
- Bufer paneli (📋) va sozlamalar tugmasi (⚙)
- Balandlik (Ixçam / Oddiy / Baland), pastki böşliq, bosilgan harfni kattalaştirib körsatiş
- Gboard uslubi: raqamlar qatori, harflar ustida belgilar, dumaloq 123 va ↵
- Emoji paneli (😊), böş joyni surib til almaştiriş, 123 ni bosib turib raqamli klaviatura
- Alohida, zamonaviy sozlamalar ekrani

## Luğatlar

- Özbekça: öz röyxatimiz + [javohirmat/uzbek-legal-corpus](https://github.com/javohirmat/uzbek-legal-corpus) (CC0) + [ddasturbek/UzbekLemma](https://github.com/ddasturbek/UzbekLemma) (MIT) + [MaksudSharipov/UzbekLemmaStems-POS-Dataset](https://github.com/MaksudSharipov/UzbekLemmaStems-POS-Dataset) (Apache-2.0), yangi alifboga öğirilgan
- Qoraqalpoqça: [shagalalab/sozlik-android](https://github.com/shagalalab/sozlik-android) (MIT), 2016-yilgi lotin alifbosiga öğirilgan
- Inglizça, rusça: [hermitdave/FrequencyWords](https://github.com/hermitdave/FrequencyWords) (CC BY-SA 4.0)

## Google Play uçun imzolaş

AAB fayli (`ozbek-klaviatura-play.aab`) GitHub Secrets'dagi yuklaş kaliti bilan imzolanadi:
`UPLOAD_KEYSTORE_B64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`.
Ular qöşilmagan bölsa, AAB sinov kaliti bilan imzolanadi — bunday faylni Play'ga yuklamang.
