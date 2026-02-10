# ZVT-Client-Bibliothek fuer Android (Kotlin)

> [English](README.md) | [Turkce](README_tr.md) | **Deutsch**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kaplanerkan/panda-zvt-library)](https://central.sonatype.com/artifact/io.github.kaplanerkan/panda-zvt-library)
[![JitPack](https://jitpack.io/v/kaplanerkan/kotlin-zvt-library.svg)](https://jitpack.io/#kaplanerkan/kotlin-zvt-library)
[![GitHub Release](https://img.shields.io/github/v/release/kaplanerkan/kotlin-zvt-library)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)
[![APK herunterladen](https://img.shields.io/badge/Download-Demo%20APK-brightgreen?logo=android&logoColor=white)](https://github.com/kaplanerkan/kotlin-zvt-library/releases/latest)
[![Wiki](https://img.shields.io/badge/Wiki-Dokumentation-blue?logo=github&logoColor=white)](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-DE)

Eine Kotlin/Android-Bibliothek, die das **ZVT-Protokoll (v13.13)** fuer die Kommunikation zwischen einer Elektronischen Registrierkasse (ECR) und Zahlungsterminals (PT) ueber TCP/IP implementiert.

## 📚 Dokumentation

Die vollstaendige Dokumentation ist im **[Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-DE)** verfuegbar:

| | Seite | Inhalt |
|---|-------|--------|
| 🏠 | **[Startseite](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-DE)** | Funktionen, UI-Anleitung, Speichersicherheit, Fehlerbehebung |
| 💳 | **[Transaktionstypen](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Transaction-Types-DE)** | Alle 7 Operationen, Storno vs Gutschrift, Vorautorisierung-Ablauf, Testergebnisse |
| 📡 | **[ZVT-Protokollreferenz](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/ZVT-Protocol-Reference-DE)** | Hex-Codes, APDU-Format, BMP-Felder, Fehlercodes, Kartentypen |
| 🖥️ | **[Terminal-Simulator](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator-DE)** | REST-API (22 Endpunkte), Curl-Beispiele, Fehlersimulation, Architektur |

> Wiki verfuegbar in **[English](https://github.com/kaplanerkan/kotlin-zvt-library/wiki)**, **[Turkce](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Home-TR)** und **Deutsch**.

## Installation

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

> **Hinweis:** GitHub Packages erfordert Authentifizierung. Fuegen Sie Ihren GitHub-Benutzernamen und ein [Personal Access Token](https://github.com/settings/tokens) (mit `read:packages`-Berechtigung) in Ihre `~/.gradle/gradle.properties` ein:
> ```properties
> gpr.user=IHR_GITHUB_BENUTZERNAME
> gpr.key=IHR_GITHUB_TOKEN
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

## Was ist ZVT?

ZVT (Zahlungsverkehrstechnik) ist das deutsche Standardprotokoll fuer die Kommunikation zwischen Kassensystemen und Zahlungsterminals. Es wird in Deutschland, Oesterreich und der Schweiz haeufig fuer die Kartenzahlungsabwicklung eingesetzt.

- **Spezifikation**: ZVT-Protokollspezifikation v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, Standardport **20007**
- **Kodierung**: Binaere APDU (Anwendungsprotokolldateneinheit)
- **Ressourcen**: [terminalhersteller.de](https://www.terminalhersteller.de/downloads.aspx)

## Funktionen

- **Volles ZVT-Protokoll v13.13** - 14 Befehlstypen, 50+ Fehlercodes, 25+ Zwischenstatuscodes
- **Coroutine-basiert** - Alle I/O als Suspend-Funktionen, kein Main-Thread-Blocking
- **Broken-Pipe-Erkennung** - Automatische Trennung bei IOException, Buttons sofort deaktiviert
- **Verbindungsstatus-Schutz** - Buttons deaktiviert bis Terminal REGISTERED ist
- **Jetpack Compose UI** - Material 3, Tuerkis-Theme, operationsgefaerbte Buttons, Bottom-Navigation
- **Mehrsprachig** - Englisch, Tuerkisch, Deutsch (zur Laufzeit umschaltbar)
- **BCD / TLV Helfer** - Vollstaendige Kodierung/Dekodierung fuer Protokolldatenstrukturen
- **Thread-sicher** - Synchronisiertes Senden, IO-Dispatcher fuer alle Socket-Operationen
- **Datei-Logging** - 30-Tage rollierende Logdateien mit vollstaendigem Protokoll-Trace (TX/RX Hex)
- **Koin DI** - Singleton-Client-Lebenszyklus, ViewModel-Injektion

## Verwendung

```kotlin
val config = ZvtConfig(
    host = "192.168.1.100",
    port = 20007,
    password = "000000",
    currencyCode = 978, // EUR
    debugMode = true
)

val client = ZvtClient(config)

// Verbinden und registrieren
client.connect()
client.register(configByte = ZvtConstants.REG_INTERMEDIATE_STATUS)

// Zahlung durchfuehren (12,50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Zahlung erfolgreich! Verfolgung: ${result.traceNumber}")
    println("Karte: ${result.cardData?.cardType}")
}

// Vorautorisierung-Ablauf
val preAuth = client.preAuthorize(amountInCents = 5000) // 50,00 EUR reservieren
// Book Total erfordert Belegnummer, Trace und AID aus der Pre-Auth-Antwort
val booking = client.bookTotal(
    receiptNumber = preAuth.receiptNumber,
    amountInCents = 4200, // 42,00 EUR buchen (optional — weglassen fuer vollen Betrag)
    traceNumber = preAuth.traceNumber,
    aid = preAuth.cardData?.aid
)

// Vorautorisierung-Storno
val reversal = client.preAuthReversal(receiptNumber = preAuth.receiptNumber)

// Letzten Beleg wiederholen
val receipt = client.repeatReceipt()

// Tagesabschluss
val eod = client.endOfDay()

// Verbindung trennen
client.logOff()
client.disconnect()
```

## Projektstruktur

```
zvt-project/
+-- app/                          # Demo-Android-Anwendung
+-- panda-zvt-library/            # ZVT-Protokollbibliothek (wiederverwendbar)
|   +-- src/main/java/com/panda/zvt_library/
|       +-- ZvtClient.kt          # Haupt-Client (TCP-Verbindung, Befehlsausfuehrung)
|       +-- ZvtCallback.kt        # Ereignis-Listener-Schnittstelle
|       +-- model/
|       |   +-- Models.kt         # Datenmodelle (Transaktionsergebnis, Kartendaten usw.)
|       +-- protocol/
|       |   +-- ZvtConstants.kt   # Alle Protokollkonstanten (Befehle, BMPs, Fehlercodes)
|       |   +-- ZvtPacket.kt      # APDU-Paket-Serialisierung/Deserialisierung
|       |   +-- ZvtCommandBuilder.kt  # Befehlsgeneratoren (Registrierung, Autorisierung usw.)
|       |   +-- ZvtResponseParser.kt  # Antwort-Parser (BMP-Felder, TLV-Container)
|       +-- util/
|           +-- TlvParser.kt      # TLV (Tag-Laenge-Wert) Parser/Generator
|           +-- BcdHelper.kt      # BCD-Kodierung/Dekodierung
|           +-- ByteExtensions.kt # Byte-Array-Erweiterungsfunktionen
+-- panda-zvt-simulator/          # ZVT-Terminal-Simulator (Ktor, reines JVM)
|   +-- src/main/kotlin/com/panda/zvt/simulator/
|       +-- Main.kt               # Einstiegspunkt, CLI-Argumente
|       +-- SimulatorServer.kt    # Orchestrator (TCP + HTTP Server)
|       +-- protocol/             # APDU, BCD, BMP-Kodierung (eigenstaendig)
|       +-- handler/              # 13 Befehlshandler (Registrierung, Zahlung usw.)
|       +-- tcp/                  # Raw-TCP-Server (Port 20007)
|       +-- api/                  # HTTP-REST-Management-API (Port 8080)
+-- gradle/
    +-- libs.versions.toml        # Zentrale Abhaengigkeitsverwaltung
```

## Screenshots

| Zahlung | Vorautorisierung | Terminal |
|:-------:|:----------------:|:--------:|
| <img src="screenshots/02_payment_tab.png" width="250"/> | <img src="screenshots/03_preauth_tab.png" width="250"/> | <img src="screenshots/04_terminal_tab.png" width="250"/> |

| Verbindung & Protokoll | Transaktionsergebnis | Registrierungskonfiguration |
|:----------------------:|:--------------------:|:---------------------------:|
| <img src="screenshots/01_connection_log.png" width="250"/> | <img src="screenshots/08_transaction_result.png" width="250"/> | <img src="screenshots/05_registration_config.png" width="250"/> |

| Simulator-Modus | Echtes Terminal (CCV A920) |
|:---------------:|:--------------------------:|
| <img src="screenshots/09_simulator_mode.png" width="250"/> | <img src="screenshots/06_real_terminal_payment.jpeg" width="450"/> |

## Demo-App

Die Demo-Anwendung (`app/`) ist ein voll funktionsfaehiger ZVT-Client mit Jetpack Compose Material-3-UI.

| Bildschirm | Operationen |
|------------|------------|
| **Verbindung** | Verbinden mit Terminal ueber IP:Port, Auto-Registrierung, Trennen |
| **Zahlung** | Zahlung, Erstattung, Stornierung, Abbruch |
| **Vorautorisierung** | Vorautorisierung, Buchung (Book Total), Teilstorno |
| **Terminal** | Diagnose, Statusabfrage, Tagesabschluss, Belegwiederholung, Abmeldung |
| **Journal** | Transaktionsverlauf mit Filtern nach Operationstyp |
| **Log** | Live-Protokoll-Viewer mit Zeitstempeln und Schwerestufen |

## Terminal-Simulator

Das Modul `panda-zvt-simulator` simuliert ein ZVT-Zahlungsterminal. Entwickeln und testen Sie Ihren ECR-Client **ohne echte Hardware** — dieselben binaeren Antworten wie ein echtes CCV A920.

```bash
# Mit Standardeinstellungen starten (ZVT: 20007, HTTP API: 8080)
./gradlew :panda-zvt-simulator:run
```

Der Simulator bietet einen **ZVT-TCP-Server** (Port 20007) und eine **HTTP-REST-API** (Port 8080) mit 22 Endpunkten fuer Konfiguration, Fehlersimulation und direkte Operationsausloesung.

> Vollstaendige REST-API-Referenz, Curl-Beispiele, Fehlersimulation und Architektur im **[Simulator-Wiki](https://github.com/kaplanerkan/kotlin-zvt-library/wiki/Simulator-DE)**.

## Erstellen

```bash
./gradlew :panda-zvt-library:assembleDebug   # Nur Bibliothek
./gradlew :app:assembleDebug                 # Demo-App
./gradlew :panda-zvt-simulator:build         # Simulator
```

## Voraussetzungen & Technologie-Stack

| Kategorie | Technologie | Version |
|-----------|------------|---------|
| Sprache | Kotlin | 2.3.10 |
| Min SDK | Android | API 24 (Android 7.0) |
| Ziel/Compile SDK | Android | API 36 |
| JVM-Ziel | Java | 17 |
| Build-System | Gradle (AGP) | 8.13.2 |
| UI-Framework | Jetpack Compose (Material 3) | BOM 2025.06.01 |
| Architektur | MVVM | - |
| DI-Framework | Koin | 4.1.1 |
| Asynchron | Kotlin Coroutines | 1.10.2 |
| Navigation | Compose Navigation | 2.9.7 |
| Lebenszyklus | Jetpack Lifecycle (ViewModel, StateFlow) | 2.10.0 |
| Datenbank | Room | 2.8.4 |
| Logging | Timber | 5.0.1 |
| Speichererkennung | LeakCanary (nur Debug) | 2.14 |

## Lizenz

```
MIT-Lizenz

Urheberrecht (c) 2026 Erkan Kaplan

Hiermit wird jeder Person, die eine Kopie dieser Software und der zugehoerigen
Dokumentationsdateien (die "Software") erhaelt, kostenlos die Erlaubnis erteilt,
die Software uneingeschraenkt zu nutzen, einschliesslich und ohne Einschraenkung
der Rechte zur Nutzung, zum Kopieren, Aendern, Zusammenfuehren, Veroeffentlichen,
Verteilen, Unterlizenzieren und/oder Verkaufen von Kopien der Software, und
Personen, denen die Software zur Verfuegung gestellt wird, dies unter den
folgenden Bedingungen zu gestatten:

Der obige Urheberrechtshinweis und dieser Genehmigungshinweis muessen in allen
Kopien oder wesentlichen Teilen der Software enthalten sein.

DIE SOFTWARE WIRD "WIE BESEHEN" OHNE JEGLICHE AUSDRUECKLICHE ODER
STILLSCHWEIGENDE GEWAEHRLEISTUNG BEREITGESTELLT, EINSCHLIESSLICH, ABER NICHT
BESCHRAENKT AUF DIE GEWAEHRLEISTUNG DER MARKTGAENGIGKEIT, DER EIGNUNG FUER EINEN
BESTIMMTEN ZWECK UND DER NICHTVERLETZUNG. IN KEINEM FALL SIND DIE AUTOREN ODER
URHEBERRECHTSINHABER HAFTBAR FUER JEGLICHE ANSPRUECHE, SCHAEDEN ODER SONSTIGE
HAFTUNG, OB AUS VERTRAG, UNERLAUBTER HANDLUNG ODER ANDERWEITIG, DIE SICH AUS
DER SOFTWARE ODER DER NUTZUNG DER SOFTWARE ODER ANDEREN GESCHAEFTEN MIT DER
SOFTWARE ERGEBEN.
```
