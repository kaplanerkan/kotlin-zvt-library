# Android icin ZVT Istemci Kutuphanesi (Kotlin)

> [English](README.md) | **Turkce** | [Deutsch](README_de.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kaplanerkan/panda-zvt-library)](https://central.sonatype.com/artifact/io.github.kaplanerkan/panda-zvt-library)
[![JitPack](https://jitpack.io/v/kaplanerkan/kotlin-zvt-library.svg)](https://jitpack.io/#kaplanerkan/kotlin-zvt-library)
[![GitHub Release](https://img.shields.io/github/v/release/kaplanerkan/kotlin-zvt-library)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)
[![APK Indir](https://img.shields.io/badge/Indir-Demo%20APK-brightgreen?logo=android&logoColor=white)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)

Android icin **ZVT Protokolu (v13.13)** uygulayan bir Kotlin kutuphanesi. Yazar Kasa (ECR) ile Odeme Terminalleri (PT) arasinda TCP/IP uzerinden iletisim saglar.

## 📚 Dokumantasyon

Tam dokumantasyon **[Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-TR)**'de mevcuttur:

| | Sayfa | Icerik |
|---|-------|--------|
| 🏠 | **[Ana Sayfa](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-TR)** | Ozellikler, arayuz rehberi, bellek guvenligi, sorun giderme |
| 💳 | **[Islem Turleri](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Transaction-Types-TR)** | 7 odeme islemi, Storno vs Iade, On Yetki akisi, test sonuclari |
| 📡 | **[ZVT Protokol Referansi](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/ZVT-Protocol-Reference-TR)** | Hex kodlar, APDU formati, BMP alanlari, hata kodlari, kart tipleri |
| 🖥️ | **[Terminal Simulatoru](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator-TR)** | REST API (22 endpoint), curl ornekleri, hata simulasyonu, mimari |

> Wiki **[English](https://github.com/kaplanerkan/kotlin-zvt-library/wiki)**, **Turkce** ve **[Deutsch](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-DE)** olarak mevcuttur.

## Kurulum

### Maven Central

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.kaplanerkan:panda-zvt-library:1.0.0")
}
```

### GitHub Packages

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/kaplanerkan/kotlin-zvt-library")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// build.gradle.kts
dependencies {
    implementation("io.github.kaplanerkan:panda-zvt-library:1.0.0")
}
```

> **Not:** GitHub Packages kimlik dogrulama gerektirir. GitHub kullanici adinizi ve bir [Personal Access Token](https://github.com/settings/tokens) (`read:packages` yetkili) `~/.gradle/gradle.properties` dosyaniza ekleyin:
> ```properties
> gpr.user=GITHUB_KULLANICI_ADINIZ
> gpr.key=GITHUB_TOKENINIZ
> ```

### JitPack

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.kaplanerkan:kotlin-zvt-library:v1.0.1")
}
```

## ZVT Nedir?

ZVT (Zahlungsverkehrstechnik), satis noktasi (POS) sistemleri ile odeme terminalleri arasindaki iletisimi saglayan Alman standart protokoludur. Almanya, Avusturya ve Isvicre'de kartli odeme islemlerinde yaygin olarak kullanilir.

- **Spesifikasyon**: ZVT Protokol Spesifikasyonu v13.13 (PA00P015_13.13_final)
- **Iletisim**: TCP/IP, varsayilan port **20007**
- **Kodlama**: Ikili APDU (Uygulama Protokol Veri Birimi)
- **Kaynaklar**: [terminalhersteller.de](https://www.terminalhersteller.de/downloads.aspx)

## Ozellikler

- **Tam ZVT Protokol v13.13** - 14 komut tipi, 50+ hata kodu, 25+ ara durum kodu
- **Coroutine tabanli** - Tum I/O suspend fonksiyonlarla, ana thread'i bloke etmez
- **Broken Pipe algilama** - IOException'da otomatik baglanti kesme, butonlar aninda devre disi
- **Baglanti durumu korumasi** - Terminal REGISTERED olana kadar butonlar devre disi
- **Jetpack Compose UI** - Material 3, turkuaz tema, isleme gore renkli butonlar, alt gezinme
- **Coklu dil** - Ingilizce, Turkce, Almanca (calisma zamaninda degistirilebilir)
- **BCD / TLV yardimcilari** - Protokol veri yapilari icin hazir util siniflari
- **Thread-safe** - Senkronize gonderim, IO dispatcher uzerinde calisma
- **Dosya loglama** - 30 gunluk donen log dosyalari, tam protokol izleme (TX/RX hex)
- **Koin DI** - Singleton client yasam dongusu, ViewModel enjeksiyonu

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
// Book Total icin receipt, trace ve AID gereklidir
val booking = client.bookTotal(
    receiptNumber = preAuth.receiptNumber,
    amountInCents = 4200, // 42.00 EUR tahsil et (opsiyonel — gonderilmezse tam tutar)
    traceNumber = preAuth.traceNumber,
    aid = preAuth.cardData?.aid
)

// On Yetki Iptali
val reversal = client.preAuthReversal(receiptNumber = preAuth.receiptNumber)

// Son fisi tekrarla
val receipt = client.repeatReceipt()

// Gun sonu
val eod = client.endOfDay()

// Baglantıyi kapat
client.logOff()
client.disconnect()
```

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

## Ekran Goruntuleri

| Odeme Sekmesi | On Yetki Sekmesi | Terminal Sekmesi |
|:-------------:|:----------------:|:----------------:|
| <img src="screenshots/02_payment_tab.png" width="250"/> | <img src="screenshots/03_preauth_tab.png" width="250"/> | <img src="screenshots/04_terminal_tab.png" width="250"/> |

| Baglanti ve Protokol | Islem Sonucu | Kayit Yapilandirmasi |
|:--------------------:|:------------:|:--------------------:|
| <img src="screenshots/01_connection_log.png" width="250"/> | <img src="screenshots/08_transaction_result.png" width="250"/> | <img src="screenshots/05_registration_config.png" width="250"/> |

| Simulator Modu | Gercek Terminal (CCV A920) |
|:--------------:|:--------------------------:|
| <img src="screenshots/09_simulator_mode.png" width="250"/> | <img src="screenshots/06_real_terminal_payment.jpeg" width="450"/> |

## Demo Uygulama

Demo uygulama (`app/`), Jetpack Compose Material 3 arayuzlu tam islevsel bir ZVT istemcisidir.

| Ekran | Islemler |
|-------|---------|
| **Baglanti** | Terminal'e IP:Port ile baglan, otomatik kayit, baglanti kes |
| **Odeme** | Odeme, Iade, Iptal, Islem Durdur |
| **On Yetki** | On Yetkilendirme, On Yetki Kapatma (Book Total), Kismi Iptal |
| **Terminal** | Tanilama, Durum Sorgulama, Gun Sonu, Fis Tekrarlama, Oturum Kapatma |
| **Gunluk** | Islem gecmisi, islem turune gore filtre |
| **Log** | Canli protokol log goruntuleyici (zaman damgasi ve onem seviyeleri) |

## Terminal Simulatoru

`panda-zvt-simulator` modulu, bir ZVT odeme terminalini simule eder. **Gercek donanim olmadan** ECR istemcinizi gelistirin ve test edin — gercek CCV A920 ile ayni ikili yanitlar.

```bash
# Varsayilan ayarlarla baslat (ZVT: 20007, HTTP API: 8080)
./gradlew :panda-zvt-simulator:run
```

Simulator **ZVT TCP sunucusu** (port 20007) ve **HTTP REST API** (port 8080) ile 22 endpoint sunar: yapilandirma, hata simulasyonu ve dogrudan islem tetikleme.

> Tam REST API referansi, curl ornekleri, hata simulasyonu ve mimari icin **[Simulator Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator-TR)** sayfasina bakin.

## Derleme

```bash
./gradlew :panda-zvt-library:assembleDebug   # Sadece kutuphane
./gradlew :app:assembleDebug                 # Demo uygulama
./gradlew :panda-zvt-simulator:build         # Simulator
```

## Gereksinimler & Teknoloji Yigini

| Kategori | Teknoloji | Surum |
|----------|-----------|-------|
| Dil | Kotlin | 2.3.10 |
| Min SDK | Android | API 24 (Android 7.0) |
| Hedef/Derleme SDK | Android | API 36 |
| JVM Hedefi | Java | 17 |
| Derleme Sistemi | Gradle (AGP) | 8.13.2 |
| UI Framework | Jetpack Compose (Material 3) | BOM 2025.06.01 |
| Mimari | MVVM | - |
| DI Framework | Koin | 4.1.1 |
| Asenkron | Kotlin Coroutines | 1.10.2 |
| Gezinme | Compose Navigation | 2.9.7 |
| Yasam Dongusu | Jetpack Lifecycle (ViewModel, StateFlow) | 2.10.0 |
| Veritabani | Room | 2.8.4 |
| Loglama | Timber | 5.0.1 |
| Bellek Izleme | LeakCanary (sadece debug) | 2.14 |

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
