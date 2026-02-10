# Android icin ZVT Istemci Kutuphanesi (Kotlin)

> [English](README.md) | **Turkce** | [Deutsch](README_de.md)
>
> **Terminal Simulatoru:** [panda-zvt-simulator/README.md](panda-zvt-simulator/README.md) — REST API, curl ornekleri, yapilandirma

Android icin **ZVT Protokolu (v13.13)** uygulayan bir Kotlin kutuphanesi. Yazar Kasa (ECR) ile Odeme Terminalleri (PT) arasinda TCP/IP uzerinden iletisim saglar.

## ZVT Nedir?

ZVT (Zahlungsverkehrstechnik), satis noktasi (POS) sistemleri ile odeme terminalleri arasindaki iletisimi saglayan Alman standart protokoludur. Almanya, Avusturya ve Isvicre'de kartli odeme islemlerinde yaygin olarak kullanilir.

- **Spesifikasyon**: ZVT Protokol Spesifikasyonu v13.13 (PA00P015_13.13_final)
- **Iletisim**: TCP/IP, varsayilan port **20007**
- **Kodlama**: Ikili APDU (Uygulama Protokol Veri Birimi)
- **Kaynaklar**: Protokol spesifikasyonlari, dokumantasyon ve indirmeler icin [terminalhersteller.de](https://www.terminalhersteller.de/downloads.aspx) adresini ziyaret edebilirsiniz

## Ozellikler

- **Tam ZVT Protokol v13.13** - 14 komut tipi, 50+ hata kodu, 25+ ara durum kodu
- **Coroutine tabanli** - Tum I/O suspend fonksiyonlarla, ana thread'i bloke etmez
- **Broken Pipe algilama** - IOException'da otomatik baglanti kesme, butonlar aninda devre disi
- **Baglanti durumu korumasi** - Terminal REGISTERED olana kadar butonlar devre disi
- **Material 3 UI** - Turkuaz tema, isleme gore renkli butonlar, alt gezinme
- **Coklu dil** - Ingilizce, Turkce, Almanca (calisma zamaninda degistirilebilir)
- **BCD / TLV yardimcilari** - Protokol veri yapilari icin hazir util siniflari
- **Thread-safe** - Senkronize gonderim, IO dispatcher uzerinde calisma
- **Dosya loglama** - 30 gunluk donen log dosyalari, tam protokol izleme (TX/RX hex)
- **Koin DI** - Singleton client yasam dongusu, ViewModel enjeksiyonu

## Proje Yapisi

```
zvt-project/
+-- app/                          # Demo Android uygulamasi
+-- panda-zvt-library/            # ZVT protokol kutuphanesi (yeniden kullanilabilir)
|   +-- src/main/java/com/panda/zvt_library/
|       +-- ZvtClient.kt          # Ana istemci (TCP baglanti, komut yurutme)
|       +-- ZvtCallback.kt        # Olay dinleyici arayuzu
|       +-- model/
|       |   +-- Models.kt         # Veri modelleri (islem sonucu, kart bilgisi vb.)
|       +-- protocol/
|       |   +-- ZvtConstants.kt   # Tum protokol sabitleri (komutlar, BMP'ler, hata kodlari)
|       |   +-- ZvtPacket.kt      # APDU paket serilestirme/ayristirma
|       |   +-- ZvtCommandBuilder.kt  # Komut olusturucular (kayit, yetkilendirme vb.)
|       |   +-- ZvtResponseParser.kt  # Yanit ayristirici (BMP alanlari, TLV kapsayicilari)
|       +-- util/
|           +-- TlvParser.kt      # TLV (Etiket-Uzunluk-Deger) ayristirici/olusturucu
|           +-- BcdHelper.kt      # BCD kodlama/cozme yardimcilari
|           +-- ByteExtensions.kt # Bayt dizisi uzanti fonksiyonlari
+-- panda-zvt-simulator/          # ZVT terminal simulatoru (Ktor, saf JVM)
|   +-- src/main/kotlin/com/panda/zvt/simulator/
|       +-- Main.kt               # Giris noktasi, CLI argumanlari
|       +-- SimulatorServer.kt    # Orkestrator (TCP + HTTP sunuculari)
|       +-- protocol/             # APDU, BCD, BMP kodlama (bagimsiz)
|       +-- handler/              # 13 komut isleyici (Kayit, Odeme vb.)
|       +-- tcp/                  # Ham TCP sunucu (port 20007)
|       +-- api/                  # HTTP REST yonetim API (port 8080)
+-- gradle/
    +-- libs.versions.toml        # Merkezi bagimlilik yonetimi
```

## Demo Uygulama

Demo uygulama (`app/`), Material 3 arayuzlu tam islevsel bir ZVT istemcisidir.

### Neler Yapilabilir

| Ekran | Islemler |
|-------|---------|
| **Baglanti** | Terminal'e IP:Port ile baglan, otomatik kayit, baglanti kes |
| **Odeme** | Odeme, Iade, Iptal, Islem Durdur |
| **On Yetki** | On Yetkilendirme, On Yetki Kapatma (Book Total), Kismi Iptal |
| **Terminal** | Tanilama, Durum Sorgulama, Gun Sonu, Fis Tekrarlama, Oturum Kapatma |
| **Log** | Canli protokol log goruntuleyici (zaman damgasi ve onem seviyeleri) |

### Arayuz Ozellikleri

- Isleme gore renkli butonlar (turkuaz: odeme, cyan: sorgulama, amber: iptal, kirmizi: durdur/kes)
- Terminal baglanip kayit olana kadar tum butonlar devre disi
- Gercek zamanli ara durum gosterimi ("Kartinizi takin", "PIN girin" vb.)
- Islem sonuc karti (kart bilgisi, trace/fis numaralari, tarih/saat)
- Fis yazdirma destegi (terminalden gelen print line'lar)
- Baglanti durum gostergesi (kirmizi/yesil nokta)
- Dil degistirici (EN/TR/DE) toolbar'da
- Kayit yapilandirma penceresi (configByte ozellestirme)

### Kayit Yapilandirma Penceresi (Registration Config)

Baglanti ekranindaki disli ikonu ile acilan bu pencere, Registration (06 00) komutunda gonderilen **configByte** degerini ozellestirmeye yarar. Her onay kutusu configByte'taki bir bite karsilik gelir:

| Onay Kutusu | Bit | Hex | Aciklama |
|-------------|-----|-----|----------|
| Receipt Payment | 1 | `0x02` | Odeme fisini ECR yazdirir (aksi halde terminal yazdirir) |
| Receipt Admin | 2 | `0x04` | Yonetim/kapanis fisini ECR yazdirir |
| Intermediate Status | 3 | `0x08` | Terminal ara durum mesajlari gonderir ("Kartinizi takin", "PIN girin") |
| Allow Payment | 4 | `0x10` | Odeme islevini ECR kontrol eder |
| Allow Admin | 5 | `0x20` | Yonetim islevlerini ECR kontrol eder |
| TLV Support | - | - | BMP 06 TLV container ile izin verilen komutlari gonderir (ayri bayrak) |

**Varsayilan:** `0x08` (yalnizca Intermediate Status aktif). ConfigByte hex ve ondalik olarak gercek zamanli gosterilir. Ayarlar SharedPreferences'ta saklanir.

| Kayit Yapilandirma Penceresi |
|:-:|
| ![Kayit Yapilandirma](screenshots/scrcpy_ojUZoVHZXh.png) |

## Ekran Goruntuleri

| Odeme Ekrani | Odeme (kaydir) | Terminal Islemleri |
|:-:|:-:|:-:|
| ![Odeme](screenshots/scrcpy_9WEEeROGaO.png) | ![Odeme kaydir](screenshots/scrcpy_dA8DSZDT7u.png) | ![Terminal](screenshots/scrcpy_McYozJ43by.png) |

| Protokol Loglari | Kayit Yapilandirmasi | CCV A920 ile Canli Odeme |
|:-:|:-:|:-:|
| ![Loglar](screenshots/scrcpy_Mj1fauW23I.png) | ![Ayarlar](screenshots/scrcpy_Z0vxa09x5T.png) | ![Donanim](screenshots/WhatsApp%20Image%202026-02-10%20at%2013.41.11.jpeg) |

| Islem Sonucu (Detay Popup) |
|:-:|
| ![Islem Sonucu](screenshots/scrcpy_9qoKBcqRmI.png) |

## Tum Islem Turleri Bir Bakista

| Buton | Komut | Ne yapar | Gerekli |
|-------|-------|---------|---------|
| **Odeme** | `06 01` | Musteri kartindan tutar tahsil eder | Tutar + Kart |
| **Iade** | `06 31` | Yeni bagimsiz iade islemi — para musteriye geri doner | Tutar + Kart |
| **Storno** | `06 30` | Onceki odemeyi iptal eder (sanki hic olmamis gibi) | Receipt no + Kart |
| **On Yetki** | `06 22` | Kartta tutar bloke eder (tahsil etmeden) | Tutar + Kart |
| **Book Total** | `06 24` | On Yetki'yi tamamlayarak gercek tutari tahsil eder | Receipt no + Trace + AID |
| **On Yetki Iptali** | `06 25` | On Yetki'yi tamamen iptal eder, bloke tutari serbest birakir | Receipt no |
| **Islem Durdur** | `06 B0` | Devam eden islemi iptal eder | - |

**Odeme (06 01):** Standart kart odemesi. Musteri kartini okutturur, tutar aninda tahsil edilir. Receipt number, trace number ve kart bilgileri doner.

**Iade (06 31) - Gutschrift:** Tamamen yeni ve bagimsiz bir islem olarak musterinin kartina para geri gonderir. Onceki isleme referans vermez. Gunler veya haftalar sonra yapilabilir. Islem ucreti alinir.

**Storno (06 30) - Reversal:** Mevcut bir odemeyi receipt number ile iptal eder. Orijinal islem host'ta silinir. Ayni gun icinde (gun sonu kapanindan once) yapilmalidir. Islem ucreti alinmaz.

**On Yetki (06 22) - Vorautorisierung:** Kartta tutar bloke eder ama tahsil etmez. Otel, arac kiralama, restoran gibi senaryolarda kullanilir. Book Total icin gerekli olan receipt number doner.

**Book Total (06 24) - Buchung:** On Yetki'yi tamamlayarak gercek tutari tahsil eder. Tahsil edilen tutar, bloke edilen tutardan az veya esit olabilir — aradaki fark otomatik serbest kalir. On Yetki cevabindan gelen **receipt number** (BMP 0x87), **trace number** (BMP 0x0B) ve **AID** (BMP 0x3B) zorunludur — spec, rezervasyon kapatmasi icin trace ve AID gönderilmesini sart kosar. Password gönderilmez (Payment/Refund'dan farkli). Kart tekrar okutulmalidir.

**On Yetki Iptali (06 25) - Pre-Auth Reversal:** Bir on yetkilendirmeyi tamamen iptal eder, bloke edilen tutarin tamami musterinin kartina serbest birakilir. Password gönderilmez. On Yetki'den gelen **receipt number** gereklidir. Tutar opsiyoneldir. Kart tekrar okutulmalidir. Not: Gercek "Kismi Iptal" (bloke tutari azaltma) komutu `06 23`'tür ve farkli bir komuttur.

**Islem Durdur (06 B0) - Abort:** Terminalde devam eden islemi iptal eder (ornegin musteri kart okunurken fikrini degistirirse).

### Test Sonuclari (CCV A920, Subat 2025)

Tüm 7 islem gercek CCV A920 terminalde Debit Mastercard ve girocard ile test edilmistir:

| # | Islem | Komut | Test | Sonuc |
|---|-------|-------|------|-------|
| 1 | Odeme | `06 01` | 0.10 EUR, girocard | Basarili |
| 2 | Iade (Gutschrift) | `06 31` | 0.10 EUR, Debit Mastercard | Basarili |
| 3 | Storno (Reversal) | `06 30` | Receipt number ile iptal | Basarili |
| 4 | On Yetki (Pre-Auth) | `06 22` | 0.30 EUR, Debit Mastercard | Basarili |
| 5 | On Yetki Kapatma (Book Total) | `06 24` | Trace + AID ile kapatma | Basarili |
| 6 | On Yetki Iptali | `06 25` | Receipt number ile iptal | Basarili |
| 7 | Islem Durdur (Abort) | `06 B0` | Devam eden islemi iptal | Basarili |
| 8 | Tanilama (Diagnosis) | `06 70` | Terminal host baglanti kontrolu | Basarili |
| 9 | Durum Sorgulama (Status Enquiry) | `05 01` | Terminal durum sorgusu | Basarili |
| 10 | Gun Sonu (End of Day) | `06 50` | Gunluk toplu kapanisi (toplam 0,60 EUR) | Basarili |
| 11 | Fis Tekrarlama (Repeat Receipt) | `06 20` | Son fis kopyasi (tam detay) | Basarili |
| 12 | Oturum Kapatma (Log Off) | `06 02` | Duzgun baglanti kesme | Basarili |

## Terminal Islemleri (Detay Popup)

Tum terminal islemleri sonuclarini, kullanici OK'a basana kadar acik kalan **tam ekran detay popup**'inda gosterir:

| Islem | Popup Icerigi |
|-------|--------------|
| **Tanilama** | Durum, baglanti durumu, Terminal ID, hata detaylari |
| **Durum Sorgulama** | Baglanti durumu, Terminal ID, durum mesaji |
| **Gun Sonu** | Durum, toplam tutar, mesaj + otomatik son fis |
| **Fis Tekrarlama** | Tam islem detaylari: tutar, trace, fis, kart bilgisi, tarih/saat, fis satirlari |
| **Oturum Kapatma** | Basari/basarisizlik durumu |

### Gun Sonu + Otomatik Fis Tekrarlama

Gun Sonu basariyla tamamlandiginda, uygulama **otomatik olarak Fis Tekrarlama**'yi tetikleyerek son islem fisini getirir. Her iki sonuc tek bir popup'ta birlestirilir:
- Gun Sonu durumu ve toplam tutar (BMP 0x04'ten)
- Terminalden gelen detayli fis satirlari (Fis Tekrarlama araciligiyla)

Bu, kullaniciya gunluk kapanisinin son islem detaylariyla birlikte tek ekranda tam bir gorunumunu saglar.

## Bellek ve Kaynak Guvenligi

Kutuphane ve uygulama, bellek sizintilari ve kaynak sizintilarina karsi koruyucu onlemler icerir:

| Bilesen | Koruma |
|---------|--------|
| **ZvtClient** | Thread-safe erisim icin `@Volatile` callback alani |
| **ZvtClient** | Fis satirlari icin `Collections.synchronizedList()` (IO coroutine'lerinden eslzamanli erisim) |
| **ZvtClient** | Baglanti hatasi yollarinda socket temizligi (dosya tanimlayici sizintisini onler) |
| **ZvtClient** | `disconnect()` / `destroy()` icin tam temizlik: callback, fis satirlari, ara durum |
| **ZvtClient** | `handleConnectionLost()` temizlik oncesi callback'i yerel degiskene kaydeder (use-after-clear onlenir) |
| **ProgressStatusDialog** | Handler callback'leri `_binding != null` kontrolu ile korunur (destroy sonrasi cokmeleri onler) |
| **ProgressStatusDialog** | `onCancelClick` lambda'si `onDestroyView()`'da temizlenir (Activity/Fragment sizintisini onler) |
| **FileLoggingTree** | Executor temizligi icin `shutdown()` metodu |
| **Tum Fragment'lar** | `onDestroyView()`'da `_binding = null`, observer'lar `viewLifecycleOwner` kapsaminda |

## Storno (Reversal) ve Gutschrift (Refund) Farki

Bu iki islem siklikla karistirilir. Iste temel fark:

**Storno / Reversal (06 30):**
- Yapilan bir odemeyi **geri alir** (sanki hic olmamis gibi)
- Orijinal isleme receipt number ile referans verir
- Genellikle **ayni gun icinde** yapilmali (gun sonu kapanindan once)
- Musteri kartini tekrar okutmasi **gerekebilir** (terminal bagli)
- Host'ta orijinal islem **silinir/iptal edilir**
- Komisyon/islem ucreti genellikle **alinmaz**
- Ornek: Kasada yanlis tutar girildi, hemen duzeltme

**Gutschrift / Refund (06 31):**
- Tamamen **yeni ve bagimsiz** bir islem
- Orijinal isleme referans **vermez**
- Gun sonu kapanindan **sonra da** yapilabilir (haftalarca sonra bile)
- Musteri kartini **okutmak zorunlu**
- Host'ta **ayri bir islem** olarak kaydedilir
- Komisyon/islem ucreti **alinir** (yeni islem oldugu icin)
- Ornek: Musteri urun iade etti, parasini geri ver

**Ozet:**

| | Storno (Reversal) | Gutschrift (Refund) |
|---|---|---|
| Islem tipi | Orijinali iptal | Yeni bagimsiz islem |
| Zaman siniri | Ayni gun (gun sonu oncesi) | Sinir yok |
| Receipt no | Gerekli | Gerekli degil |
| Komisyon | Yok | Var |
| Kart | Terminal bagli | Okutmak zorunlu |
| Host kaydi | Orijinal silinir | Ayri kayit olusur |

Kisacasi: Storno = "bu islem hic olmadi", Gutschrift = "yeni bir para iadesi yap".

## On Yetkilendirme Akisi (Pre-Authorization / Vorautorisierung)

On Yetkilendirme, musterinin kartindan **tutar ayirmak (bloke etmek)** icin kullanilir, gercek tahsilat yapilmaz. Gercek tahsilat daha sonra Book Total ile yapilir veya bloke kismen ya da tamamen serbest birakilir.

**Kullanim senaryolari:**
- **Otel:** Check-in'de 500 EUR bloke et, check-out'ta gercek tutari tahsil et
- **Arac kiralama:** Depozito olarak 1000 EUR ayir, iade sonrasi gercek tutari al
- **Restoran:** Hesap tutarini bloke et, bahsisle birlikte son tutari tahsil et

**Akis:**

```
1. Pre-Auth (06 22)       -> Kartta tutari bloke et -> receipt number doner
   Musteri hizmeti alir...
2. Book Total (06 24)     -> Gercek tutari tahsil et (receipt number gerekli)
   VEYA
   Partial Reversal (06 25) -> Blokenin bir kismini serbest birak
```

**Adim 1: On Yetkilendirme (06 22)**
- Tutar ve para birimini terminale gonderir
- Musteri kartini okutmalidir (temas/temassiz)
- Tutar musterinin hesabinda **bloke edilir** ama **cekilmez**
- Bir **receipt number** doner — bu sonraki adim icin gereklidir

**Adim 2a: Book Total (06 24)**
- On yetkilendirmeyi tamamlayarak gercek tutari tahsil eder
- Pre-Auth cevabindan gelen uc deger gereklidir:
  - **Receipt number** (BMP 0x87) — orijinal on yetkilendirmeyi tanimlar
  - **Trace number** (BMP 0x0B) — spec, rezervasyon kapatmasi icin zorunlu kilar
  - **AID** (BMP 0x3B) — spec, rezervasyon kapatmasi icin zorunlu kilar
- Password gönderilmez (Payment/Refund/Reversal'dan farkli)
- Tahsil edilen tutar, bloke edilen tutardan **az veya esit** olabilir (tutar opsiyoneldir — gönderilmezse tam tutar kitlenir)
- Kart tekrar okutulmalidir
- Ornek: Pre-Auth 100 EUR, Book Total 85 EUR (kalan 15 EUR serbest kalir)

**APDU formati:** `06 24 xx  87(receipt-no) [04(tutar)] 0B(trace) 3B(AID)`

**Adim 2b: On Yetki Iptali / Pre-Auth Reversal (06 25)**
- On yetkilendirmeyi tamamen iptal eder, bloke edilen tutarin tamami serbest kalir
- **Receipt number** (BMP 0x87) gereklidir
- Password gönderilmez (Book Total ile ayni)
- Tutar opsiyoneldir (gönderilmezse tam tutar iptal edilir)
- Kart tekrar okutulmalidir
- Ornek: Pre-Auth 100 EUR, Pre-Auth Reversal → 100 EUR serbest kalir

**APDU formati:** `06 25 xx  87(receipt-no) [04(tutar)]`

**Ozet:**

| Komut | Hex | Amac | Gerekli |
|-------|-----|------|---------|
| On Yetkilendirme | `06 22` | Kartta tutar bloke et | Tutar + Kart |
| Book Total | `06 24` | Bloke tutari tahsil et | Receipt number + Trace + AID (+ opsiyonel Tutar) |
| On Yetki Iptali | `06 25` | On yetkiyi iptal et, blokeyi kaldir | Receipt number (+ opsiyonel Tutar) |

## ZVT Komut Hex Kodlari

### ECR -> Terminal Komutlari

| Komut | Hex Kodu | Aciklama |
|-------|----------|----------|
| Kayit (Registration) | `06 00` | ECR'yi terminale kaydet, para birimi ve konfigurasyonu ayarla |
| Yetkilendirme (Authorization) | `06 01` | Odeme islemi (tutar BCD formatinda) |
| Oturum Kapatma (Log Off) | `06 02` | Terminalden duzgun sekilde ayril |
| Islem Durdur (Abort) | `06 B0` | Devam eden islemi iptal et (ECR→PT) |
| Fis Tekrarlama (Repeat Receipt) | `06 20` | Son fisi tekrar yazdir |
| On Yetkilendirme (Pre-Auth) | `06 22` | Tutar ayir (otel, arac kiralama) |
| On Yetki Kapatma (Book Total) | `06 24` | On yetkilendirmeyi tamamla |
| Kismi Iptal (Partial Reversal) | `06 25` | Islemi kismen iptal et |
| Iptal (Reversal) | `06 30` | Onceki islemi iptal et |
| Iade (Refund) | `06 31` | Iade islemi |
| Gun Sonu (End of Day) | `06 50` | Gunluk toplu kapanisi yap |
| Tanilama (Diagnosis) | `06 70` | Terminal durumunu sorgula |
| Durum Sorgulama (Status Enquiry) | `05 01` | Terminal durumunu kontrol et |

### Terminal -> ECR Yanitlari

| Yanit | Hex Kodu | Aciklama |
|-------|----------|----------|
| ACK | `80 00 00` | Olumlu onay |
| NACK | `84 00 00` | Olumsuz onay (komut reddedildi) |
| Tamamlandi (Completion) | `06 0F` | Islem bitti |
| Durum Bilgisi (Status Info) | `04 0F` | BMP alanlariyla islem sonucu |
| Ara Durum (Intermediate Status) | `04 FF` | "Kartinizi takin", "PIN girin" vb. |
| Yazdir (Print Line) | `06 D1` | Tek fis satiri |
| Metin Blogu (Print Text Block) | `06 D3` | Fis metin blogu |
| Islem Durduruldu (Abort) | `06 1E` | Terminal tarafindan islem durduruldu |

> **Abort Hakkinda Not (06 B0 ve 06 1E):** ZVT protokolunde iki farkli abort kodu vardir. `06 B0` ECR→PT yonunde kullanilir — ECR tarafindan devam eden islemi iptal etmek icin gonderilir. `06 1E` ise PT→ECR yonunde terminalin ECR'ye "islem iptal edildi" bildirimi gondermesidir (ornegin kullanici terminalden iptal tusuna bastiginda). `06 1E`'yi ECR→PT komutu olarak gondermek calismaz — terminal bu kodu tanimaz cunku bu bir yanit kodudur, komut degil.

### APDU Paket Formati

```
[CMD_CLASS(1)] [CMD_INSTR(1)] [LEN(1-3)] [DATA(N)]

Uzunluk kodlamasi:
  0x00-0xFE  -> 1 bayt uzunluk (0-254 bayt veri)
  0xFF LL HH -> 3 bayt uzatilmis uzunluk (little-endian, 65535 bayta kadar)

Ornekler:
  06 00 08 ...        -> Kayit, 8 bayt veri
  06 01 07 ...        -> Yetkilendirme, 7 bayt veri
  80 00 00            -> ACK (veri yok)
  04 FF 03 ...        -> Ara Durum, 3 bayt veri
```

### Ornek: Kayit Paketi

```
TX: 06 00 08 00 00 00 08 09 78 03 00
     |  |  |  |        |  |     |  |
     |  |  |  +--------+  |     |  +-- Servis bayt degeri (0x00)
     |  |  |  Sifre        |     +-- BMP 0x03 (Servis bayt etiketi)
     |  |  |  (000000 BCD) |
     |  |  |               +-- Para birimi kodu (0978 = EUR, konumsal)
     |  |  +-- Uzunluk: 8 bayt
     |  +-- Talimat: 0x00
     +-- Sinif: 0x06 (Kayit)
```

### Ornek: Yetkilendirme Paketi

```
TX: 06 01 07 04 00 00 00 01 00 19 40
     |  |  |  |  |              |  |
     |  |  |  |  +--------------+  +-- Odeme tipi EC-Cash (0x40)
     |  |  |  |  Tutar: 000001 00      BMP 0x19
     |  |  |  |  = 1.00 EUR (BCD)
     |  |  |  +-- BMP 0x04 (Tutar etiketi)
     |  |  +-- Uzunluk: 7 bayt
     +--+-- Komut: 06 01 (Yetkilendirme)
```

## Sonuc Kodlari (BMP 0x27)

| Kod | Sabit | Aciklama |
|-----|-------|----------|
| `00` | `RC_SUCCESS` | Basarili |
| `64` | `RC_CARD_NOT_READABLE` | Kart okunamiyor (LRC/parite hatasi) |
| `65` | `RC_CARD_DATA_NOT_PRESENT` | Kart verisi mevcut degil |
| `66` | `RC_PROCESSING_ERROR` | Isleme hatasi |
| `6C` | `RC_ABORT_VIA_TIMEOUT` | Zaman asimi veya iptal tusu ile durdurma |
| `6F` | `RC_WRONG_CURRENCY` | Yanlis para birimi |
| `78` | `RC_CARD_EXPIRED` | Kartin suresi dolmus |
| `7D` | `RC_COMMUNICATION_ERROR` | Iletisim hatasi |
| `9A` | `RC_ZVT_PROTOCOL_ERROR` | ZVT protokol hatasi |
| `A0` | `RC_RECEIVER_NOT_READY` | Alici hazir degil |
| `B4` | `RC_ALREADY_REVERSED` | Zaten iptal edilmis |
| `B5` | `RC_REVERSAL_NOT_POSSIBLE` | Iptal mumkun degil |
| `C2` | `RC_DIAGNOSIS_REQUIRED` | Tanilama gerekli |
| `C3` | `RC_MAX_AMOUNT_EXCEEDED` | Maksimum tutar asildi |
| `FF` | `RC_SYSTEM_ERROR` | Sistem hatasi |

> Tam liste: `ZvtConstants.kt` dosyasinda 50+ sonuc kodu tanimli

## Ara Durum Kodlari (04 FF)

| Kod | Mesaj |
|-----|-------|
| `00` | Tutar onay bekleniyor |
| `01` | Lutfen PIN-pad'e bakin |
| `04` | FEP yaniti bekleniyor |
| `0A` | Kartinizi takin |
| `0B` | Lutfen kartinizi cikartin |
| `0E` | Lutfen bekleyin |
| `15` | Yanlis PIN |
| `18` | PIN deneme limiti asildi |
| `1C` | Onaylandi, urunleri alin |
| `1D` | Reddedildi |

## Ayristirilan BMP Alanlari

| BMP | Ad | Bicim |
|-----|-----|-------|
| `04` | Tutar | 6 bayt BCD |
| `06` | TLV Kapsayici | BER-TLV uzunluk |
| `0B` | Izleme Numarasi | 3 bayt BCD |
| `0C` | Saat (SSDDSS) | 3 bayt BCD |
| `0D` | Tarih (AAGU) | 2 bayt BCD |
| `0E` | Son Kullanma Tarihi (YYAA) | 2 bayt BCD |
| `17` | Kart Sira Numarasi | 2 bayt BCD |
| `19` | Odeme Turu | 1 bayt |
| `22` | PAN/EF_ID | LLVAR BCD |
| `27` | Sonuc Kodu | 1 bayt |
| `29` | Terminal Kimligi | 4 bayt BCD |
| `2A` | VU Numarasi | 15 bayt ASCII |
| `37` | Orijinal Izleme | 3 bayt BCD |
| `3B` | AID | 8 bayt sabit |
| `3C` | Ek Veri/TLV | LLLVAR |
| `49` | Para Birimi Kodu | 2 bayt BCD |
| `87` | Fis Numarasi | 2 bayt BCD |
| `88` | Ciro Numarasi | 3 bayt BCD |
| `8A` | Kart Turu | 1 bayt |
| `8B` | Kart Adi | LLVAR ASCII |
| `8C` | Kart Turu Ag Kimligi | 1 bayt |
| `A0` | Sonuc Kodu AS | 1 bayt |
| `BA` | AID Parametresi | 5 bayt sabit |

## Kart Tipi Kimlikleri (BMP 0x8A)

ZVT Spec v13.13, Bolum 12: "ZVT-card-type ID Listesi" temel alinmistir.

| ID (dec) | Hex | Kart Tipi |
|----------|-----|-----------|
| 5 | `0x05` | girocard |
| 6 | `0x06` | Mastercard |
| 8 | `0x08` | American Express |
| 10 | `0x0A` | Visa |
| 11 | `0x0B` | Visa Electron |
| 12 | `0x0C` | Diners |
| 13 | `0x0D` | V PAY |
| 14 | `0x0E` | JCB |
| 30 | `0x1E` | Geldkarte |
| 46 | `0x2E` | Maestro |
| 87 | `0x57` | Bancontact |
| 97 | `0x61` | Alipay |
| 198 | `0xC6` | CUP (China UnionPay) |
| 232 | `0xE8` | Discover |
| 255 | `0xFF` | TLV tag 41'e bakin (ID >= 256) |

> Tam liste: ZVT Spec Bolum 12'de 280+ kart tipi tanimli. 256 ve uzeri ID'ler BMP 0x8A yerine TLV tag 41 kullanir.

## Kullanim

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Baglan ve kayit ol
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Odeme yap (12,50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Odeme basarili! Izleme: ${result.traceNumber}")
    println("Kart: ${result.cardData?.cardType}")
}

// On Yetkilendirme akisi
val preAuth = client.preAuthorize(amountInCents = 5000) // 50.00 EUR ayir
val booking = client.bookTotal(amountInCents = 4200, receiptNumber = preAuth.receiptNumber) // 42.00 tahsil et

// Kismi Iptal
val partial = client.partialReversal(amountInCents = 500, receiptNumber = 123) // 5.00 kismi iptal

// Son fisi tekrarla
val receipt = client.repeatReceipt()

// Gun sonu
val eod = client.endOfDay()

// Baglantıyi kapat
client.logOff()
client.disconnect()
```

## Protokol Akisi

```
ECR -> PT:  Komut APDU (orn: yetkilendirme icin 06 01)
PT  -> ECR: Onay (80 00 00)
PT  -> ECR: Ara Durum (04 FF) [tekrarlanan] - "Kartinizi takin", "PIN girin"...
ECR -> PT:  Onay (80 00 00) [her biri icin]
PT  -> ECR: Durum Bilgisi (04 0F) - BMP alanlariyla islem sonucu
ECR -> PT:  Onay (80 00 00)
PT  -> ECR: Yazdirma Satiri (06 D1) [tekrarlanan] - Fis satirlari
ECR -> PT:  Onay (80 00 00) [her biri icin]
PT  -> ECR: Tamamlandi (06 0F) veya Durduruldu (06 1E)
ECR -> PT:  Onay (80 00 00)
```

## Sorun Giderme / Gunluk Dosyalari

Uygulama otomatik olarak cihaz depolamasina gunluk dosyalari yazar. Gunlukler **30 gun** boyunca saklanir ve eski dosyalar otomatik olarak silinir.

**Gunluk dosya konumu:**
```
Android/data/com.panda_erkan.zvtclientdemo/files/Download/logs/zvt_YYYY-AA-GG.log
```

**Erisim yontemi:**
1. Cihazi USB ile bilgisayara baglayin
2. Cihaz depolamasini acin ve yukaridaki dizine gidin
3. Veya cihaz uzerinde bir dosya yoneticisi uygulamasi kullanin

**Gunluk bicimi:**
```
2026-02-10 14:30:15.123 D/ZVT: ECR -> PT | TX Registration (06 00) | 11 bytes | 06-00-08-00-00-00-08-09-78-03-00
2026-02-10 14:30:15.456 D/ZVT: PT -> ECR | RX ACK (80 00) | 3 bytes | 80-00-00
```

Bir sorunla karsilastiginizda, hata bildirirken lutfen ilgili gunluk dosyasini ekleyin.

## Derleme

```bash
./gradlew :panda-zvt-library:assembleDebug   # Sadece kutuphane
./gradlew :app:assembleDebug                 # Demo uygulama
./gradlew :panda-zvt-simulator:build         # Simulator
```

## Terminal Simulatoru (panda-zvt-simulator)

Gercek bir ZVT odeme terminalini simule eden saf Kotlin/JVM uygulamasi. **Gercek donanim olmadan** ECR istemcinizi gelistirin ve test edin — simulator, gercek bir CCV A920 terminalinin dondurdugu ayni ikili yanitlari dondurur.

**Detayli simulator dokumantasyonu, tum curl ornekleri, yapilandirma secenekleri ve mimari icin:** bkz. [panda-zvt-simulator/README.md](panda-zvt-simulator/README.md)

### Simulatoru Calistirma

```bash
# Varsayilan ayarlarla baslat (ZVT: 20007, API: 8080)
./gradlew :panda-zvt-simulator:run

# Ozel portlar ve terminal ID
./gradlew :panda-zvt-simulator:run --args="--zvt-port 20007 --api-port 8080 --terminal-id 29001234"
```

Simulator iki sunucu baslatir:
- **ZVT TCP** port `20007` — ikili ZVT protokolu (Android uygulamanizi buraya baglayin)
- **HTTP API** port `8080` — REST yonetim API (kart verisi, gecikmeler, hatalar yapilandirin)

### Android'den Baglanti

Demo uygulamada **Simulator Modu** acma/kapama dugmesi vardir. Acildiginda Ktor simulator sunucusu **Android cihazin uzerinde** baslar (TCP:20007 + HTTP:8080).

| Mod | Simulator IP | HTTP API | Aciklama |
|-----|-------------|----------|----------|
| **Gomulu (toggle ON)** | Cihazin kendi IP'si | `http://<cihaz_ip>:8080` | Simulator cihazda calisir |
| **Bagimsiz (PC)** | PC'nin LAN IP'si | `http://<pc_ip>:8080` | Simulator PC'de JVM olarak calisir |

### REST Yonetim API

| Metod | Yol | Aciklama |
|-------|-----|----------|
| `GET` | `/api/status` | Simulator durumu |
| `GET` | `/api/config` | Guncel yapilandirma |
| `PUT` | `/api/config` | Tam yapilandirmayi guncelle |
| `PUT` | `/api/error` | Hata simulasyonu (etkinlestir, yuzde, zorunlu kod) |
| `PUT` | `/api/card` | Kart verisini degistir (PAN, tip, isim, AID) |
| `PUT` | `/api/delays` | Yanit zamanlama (ara durum, isleme, ACK zaman asimi) |
| `GET` | `/api/transactions` | Islemleri listele |
| `GET` | `/api/transactions/last` | Son islem |
| `DELETE` | `/api/transactions` | Islemleri temizle |
| `POST` | `/api/reset` | Tam sifirlama |

### Islem Endpoint'leri (REST)

| Metod | Yol | Govde | Aciklama |
|-------|-----|-------|----------|
| `POST` | `/api/operations/payment` | `{"amount": 12.50}` | Odeme |
| `POST` | `/api/operations/refund` | `{"amount": 12.50}` | Iade |
| `POST` | `/api/operations/reversal` | `{"receiptNo": 1}` | Storno |
| `POST` | `/api/operations/pre-auth` | `{"amount": 50.00}` | On Yetki |
| `POST` | `/api/operations/book-total` | `{"amount": 50.00, "receiptNo": 1}` | On Yetki Kapat |
| `POST` | `/api/operations/pre-auth-reversal` | `{"receiptNo": 1}` | On Yetki Iptal |
| `POST` | `/api/operations/end-of-day` | _(bos)_ | Gun Sonu |
| `POST` | `/api/operations/diagnosis` | _(bos)_ | Tanilama |
| `POST` | `/api/operations/status-enquiry` | _(bos)_ | Durum Sorgu |
| `POST` | `/api/operations/repeat-receipt` | _(bos)_ | Fis Tekrarla |
| `POST` | `/api/operations/registration` | _(bos)_ | Kayit |
| `POST` | `/api/operations/log-off` | _(bos)_ | Oturum Kapat |
| `POST` | `/api/operations/abort` | _(bos)_ | Iptal |

### Curl Ornekleri

```bash
# Odeme
curl -X POST http://localhost:8080/api/operations/payment \
  -H "Content-Type: application/json" -d '{"amount": 25.50}'

# Iade
curl -X POST http://localhost:8080/api/operations/refund \
  -H "Content-Type: application/json" -d '{"amount": 5.00}'

# Storno
curl -X POST http://localhost:8080/api/operations/reversal \
  -H "Content-Type: application/json" -d '{"receiptNo": 1}'

# On Yetki
curl -X POST http://localhost:8080/api/operations/pre-auth \
  -H "Content-Type: application/json" -d '{"amount": 100.00}'

# On Yetki Kapat
curl -X POST http://localhost:8080/api/operations/book-total \
  -H "Content-Type: application/json" -d '{"amount": 85.00, "receiptNo": 1}'

# Gun Sonu
curl -X POST http://localhost:8080/api/operations/end-of-day

# Tanilama
curl -X POST http://localhost:8080/api/operations/diagnosis

# Hata simulasyonu etkinlestir
curl -X PUT http://localhost:8080/api/error \
  -H "Content-Type: application/json" -d '{"enabled": true, "errorCode": 104}'

# Kart verisini Visa yap
curl -X PUT http://localhost:8080/api/card \
  -H "Content-Type: application/json" \
  -d '{"pan": "4111111111111111", "cardType": 10, "cardName": "Visa"}'

# Simulator sifirla
curl -X POST http://localhost:8080/api/reset
```

> **Tam dokumantasyon icin [panda-zvt-simulator/README.md](panda-zvt-simulator/README.md) dosyasina bakin:** 22 REST endpoint'in curl ornekleri, yapilandirma, hata simulasyonu, mimari ve daha fazlasi.

## Gereksinimler & Teknoloji Yigini

| Kategori | Teknoloji | Surum |
|----------|-----------|-------|
| Dil | Kotlin | 2.3.10 |
| Min SDK | Android | API 24 (Android 7.0) |
| Hedef/Derleme SDK | Android | API 36 |
| JVM Hedefi | Java | 17 |
| Derleme Sistemi | Gradle (AGP) | 8.13.2 |
| UI Framework | Material Design 3 | 1.13.0 |
| Mimari | MVVM | - |
| DI Framework | Koin | 4.1.1 |
| Asenkron | Kotlin Coroutines | 1.10.2 |
| Gezinme | Jetpack Navigation | 2.9.7 |
| Yasam Dongusu | Jetpack Lifecycle (ViewModel, LiveData, StateFlow) | 2.10.0 |
| View Binding | Android Data Binding | - |
| Loglama | Timber | 5.0.1 |
| Bellek Izleme | LeakCanary (sadece debug) | 2.14 |
| UI Bilesenleri | RecyclerView, CardView, ConstraintLayout | guncel |

## Lisans

```
MIT Lisansi

Telif Hakki (c) 2026 Erkan Kaplan

Bu yazilimin ve ilgili dokumantasyon dosyalarinin ("Yazilim") bir kopyasini
edinmis herhangi bir kisiye, Yazilimi kisitlama olmaksizin kullanma, kopyalama,
degistirme, birlestirme, yayinlama, dagitma, alt lisanslama ve/veya satma
haklari da dahil olmak uzere, asagidaki kosullara tabi olarak ucretsiz olarak
izin verilmistir:

Yukaridaki telif hakki bildirimi ve bu izin bildirimi, Yazilimin tum
kopyalarina veya onemli bolumlerine dahil edilmelidir.

YAZILIM, TICARI ELVERISLILIK, BELIRLI BIR AMACA UYGUNLUK VE IHLAL
ETMEME GARANTILERI DAHIL ANCAK BUNLARLA SINIRLI OLMAMAK UZERE, ACIK VEYA
ZIMNI HERHANGI BIR GARANTI OLMAKSIZIN "OLDUGU GIBI" SAGLANMAKTADIR.
```
