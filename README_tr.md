# Android için ZVT İstemci Kütüphanesi (Kotlin)

> [🇬🇧 English](README.md) | 🇹🇷 **Türkçe** | [🇩🇪 Deutsch](README_de.md)

Android için **ZVT Protokolü (v13.13)** uygulayan bir Kotlin kütüphanesi. Yazar Kasa (ECR) ile Ödeme Terminalleri (PT) arasında TCP/IP üzerinden iletişim sağlar.

## ZVT Nedir?

ZVT (Zahlungsverkehrstechnik), satış noktası (POS) sistemleri ile ödeme terminalleri arasındaki iletişimi sağlayan Alman standart protokolüdür. Almanya, Avusturya ve İsviçre'de kartlı ödeme işlemlerinde yaygın olarak kullanılır.

- **Spesifikasyon**: ZVT Protokol Spesifikasyonu v13.13 (PA00P015_13.13_final)
- **İletişim**: TCP/IP, varsayılan port **20007**
- **Kodlama**: İkili APDU (Uygulama Protokol Veri Birimi)

## Proje Yapısı

```
zvt-project/
├── app/                          # Tanıtım/test Android uygulaması
├── panda-zvt-library/                  # ZVT protokol kütüphanesi (yeniden kullanılabilir)
│   └── src/main/java/com/panda/zvt_library/
│       ├── ZvtClient.kt          # Ana istemci (TCP bağlantı, komut yürütme)
│       ├── ZvtCallback.kt        # Olay dinleyici arayüzü
│       ├── model/
│       │   └── Models.kt         # Veri modelleri (işlem sonucu, kart bilgisi vb.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # Tüm protokol sabitleri (komutlar, BMP'ler, hata kodları)
│       │   ├── ZvtPacket.kt      # APDU paket serileştirme/ayrıştırma
│       │   ├── ZvtCommandBuilder.kt  # Komut oluşturucular (kayıt, yetkilendirme vb.)
│       │   └── ZvtResponseParser.kt  # Yanıt ayrıştırıcı (BMP alanları, TLV kapsayıcıları)
│       └── util/
│           ├── TlvParser.kt      # TLV (Etiket-Uzunluk-Değer) ayrıştırıcı/oluşturucu
│           ├── BcdHelper.kt      # BCD kodlama/çözme yardımcıları
│           └── ByteExtensions.kt # Bayt dizisi uzantı fonksiyonları
└── gradle/
    └── libs.versions.toml        # Merkezi bağımlılık yönetimi
```

## Desteklenen Komutlar

| Komut | Kod | Açıklama |
|-------|-----|----------|
| Kayıt | `06 00` | ECR'yi terminale kaydet |
| Yetkilendirme | `06 01` | Ödeme işlemi |
| Oturum Kapatma | `06 02` | Terminal bağlantısını sonlandır |
| Ön Yetkilendirme | `06 22` | Ön yetkilendirme (otel, araç kiralama) |
| İptal | `06 30` | Önceki işlemi iptal et |
| İade | `06 31` | İade işlemi |
| Gün Sonu | `06 50` | Gün sonu kapanışı |
| Tanılama | `06 70` | Terminal durumunu sorgula |
| Durum Sorgulama | `05 01` | Terminal durumunu kontrol et |

## Kullanım

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Bağlan ve kayıt ol
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Ödeme yap (12,50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Ödeme başarılı! İzleme: ${result.traceNumber}")
    println("Kart: ${result.cardData?.cardType}")
} else {
    println("Ödeme başarısız: ${result.resultMessage}")
}

// Gün sonu
val eod = client.endOfDay()

// Bağlantıyı kapat
client.disconnect()
```

## Protokol Akışı

```
ECR → PT:  Komut APDU (örn: yetkilendirme için 06 01)
PT  → ECR: Onay (80 00 00)
PT  → ECR: Ara Durum (04 FF) [tekrarlanan] - "Kartı takın", "PIN girin"...
ECR → PT:  Onay (80 00 00) [her biri için]
PT  → ECR: Durum Bilgisi (04 0F) - BMP alanlarıyla işlem sonucu
ECR → PT:  Onay (80 00 00)
PT  → ECR: Yazdırma Satırı (06 D1) [tekrarlanan] - Fiş satırları
ECR → PT:  Onay (80 00 00) [her biri için]
PT  → ECR: Tamamlandı (06 0F) veya İptal (06 1E)
ECR → PT:  Onay (80 00 00)
```

## Ayrıştırılan BMP Alanları

| BMP | Ad | Biçim |
|-----|-----|-------|
| `04` | Tutar | 6 bayt BCD |
| `06` | TLV Kapsayıcı | BER-TLV uzunluk |
| `0B` | İzleme Numarası | 3 bayt BCD |
| `0C` | Saat (SSDDSS) | 3 bayt BCD |
| `0D` | Tarih (AAGÜ) | 2 bayt BCD |
| `0E` | Son Kullanma Tarihi (YYAA) | 2 bayt BCD |
| `17` | Kart Sıra Numarası | 2 bayt BCD |
| `19` | Ödeme Türü | 1 bayt |
| `22` | PAN/EF_ID | LLVAR BCD |
| `27` | Sonuç Kodu | 1 bayt |
| `29` | Terminal Kimliği | 4 bayt BCD |
| `2A` | VU Numarası | 15 bayt ASCII |
| `37` | Orijinal İzleme | 3 bayt BCD |
| `3B` | AID | 8 bayt sabit |
| `3C` | Ek Veri/TLV | LLLVAR |
| `49` | Para Birimi Kodu | 2 bayt BCD |
| `87` | Fiş Numarası | 2 bayt BCD |
| `88` | Ciro Numarası | 3 bayt BCD |
| `8A` | Kart Türü | 1 bayt |
| `8B` | Kart Adı | LLVAR ASCII |
| `8C` | Kart Türü Ağ Kimliği | 1 bayt |
| `A0` | Sonuç Kodu AS | 1 bayt |
| `BA` | AID Parametresi | 5 bayt sabit |

## Sorun Giderme / Günlük Dosyaları

Uygulama otomatik olarak cihaz depolamasına günlük dosyaları yazar. Günlükler **30 gün** boyunca saklanır ve eski dosyalar otomatik olarak silinir.

**Günlük dosya konumu:**
```
Android/data/com.panda_erkan.zvtclientdemo/files/Download/logs/zvt_YYYY-AA-GG.log
```

**Erişim yöntemi:**
1. Cihazı USB ile bilgisayara bağlayın
2. Cihaz depolamasını açın ve yukarıdaki dizine gidin
3. Veya cihaz üzerinde bir dosya yöneticisi uygulaması kullanın

**Günlük biçimi:**
```
2026-02-10 14:30:15.123 D/ZvtClient: [ZvtResponseParser] BMP 0x04 alanı ayrıştırılıyor
2026-02-10 14:30:15.456 E/ZvtClient: 5000ms sonra bağlantı zaman aşımı
```

Bir sorunla karşılaştığınızda, hata bildirirken lütfen ilgili günlük dosyasını ekleyin.

## Derleme

```bash
./gradlew :panda-zvt-library:assembleDebug
```

## Gereksinimler

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

## Lisans

```
MIT Lisansı

Telif Hakkı (c) 2026 Erkan Kaplan

Bu yazılımın ve ilgili dokümantasyon dosyalarının ("Yazılım") bir kopyasını
edinmiş herhangi bir kişiye, Yazılımı kısıtlama olmaksızın kullanma, kopyalama,
değiştirme, birleştirme, yayınlama, dağıtma, alt lisanslama ve/veya satma
hakları da dahil olmak üzere, aşağıdaki koşullara tabi olarak ücretsiz olarak
izin verilmiştir:

Yukarıdaki telif hakkı bildirimi ve bu izin bildirimi, Yazılımın tüm
kopyalarına veya önemli bölümlerine dahil edilmelidir.

YAZILIM, TİCARİ ELVERİŞLİLİK, BELİRLİ BİR AMACA UYGUNLUK VE İHLAL
ETMEME GARANTİLERİ DAHİL ANCAK BUNLARLA SINIRLI OLMAMAK ÜZERE, AÇIK VEYA
ZIMNI HERHANGİ BİR GARANTİ OLMAKSIZIN "OLDUĞU GİBİ" SAĞLANMAKTADIR.
YAZARLAR VEYA TELİF HAKKI SAHİPLERİ, YAZILIMDAN VEYA YAZILIMIN KULLANIMINDAN
YA DA DİĞER İŞLEMLERDEN KAYNAKLANAN HERHANGİ BİR TALEP, HASAR VEYA DİĞER
YÜKÜMLÜLÜKLERDEN HİÇBİR ŞEKİLDE SORUMLU TUTULAMAZ.
```
