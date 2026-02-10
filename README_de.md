# ZVT-Client-Bibliothek für Android (Kotlin)

> [🇬🇧 English](README.md) | [🇹🇷 Türkçe](README_tr.md) | 🇩🇪 **Deutsch**

Eine Kotlin/Android-Bibliothek, die das **ZVT-Protokoll (v13.13)** für die Kommunikation zwischen einer Elektronischen Registrierkasse (ECR) und Zahlungsterminals (PT) über TCP/IP implementiert.

## Was ist ZVT?

ZVT (Zahlungsverkehrstechnik) ist das deutsche Standardprotokoll für die Kommunikation zwischen Kassensystemen und Zahlungsterminals. Es wird in Deutschland, Österreich und der Schweiz häufig für die Kartenzahlungsabwicklung eingesetzt.

- **Spezifikation**: ZVT-Protokollspezifikation v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, Standardport **20007**
- **Kodierung**: Binäre APDU (Anwendungsprotokolldateneinheit)

## Projektstruktur

```
zvt-project/
├── app/                          # Demo-/Test-Android-Anwendung
├── zvt-library/                  # ZVT-Protokollbibliothek (wiederverwendbar)
│   └── src/main/java/com/erkan/zvt/
│       ├── ZvtClient.kt          # Haupt-Client (TCP-Verbindung, Befehlsausführung)
│       ├── ZvtCallback.kt        # Ereignis-Listener-Schnittstelle
│       ├── model/
│       │   └── Models.kt         # Datenmodelle (Transaktionsergebnis, Kartendaten usw.)
│       ├── protocol/
│       │   ├── ZvtConstants.kt   # Alle Protokollkonstanten (Befehle, BMPs, Fehlercodes)
│       │   ├── ZvtPacket.kt      # APDU-Paket-Serialisierung/Deserialisierung
│       │   ├── ZvtCommandBuilder.kt  # Befehlsgeneratoren (Registrierung, Autorisierung usw.)
│       │   └── ZvtResponseParser.kt  # Antwort-Parser (BMP-Felder, TLV-Container)
│       └── util/
│           ├── TlvParser.kt      # TLV (Tag-Länge-Wert) Parser/Generator
│           ├── BcdHelper.kt      # BCD-Kodierung/Dekodierung
│           └── ByteExtensions.kt # Byte-Array-Erweiterungsfunktionen
└── gradle/
    └── libs.versions.toml        # Zentrale Abhängigkeitsverwaltung
```

## Unterstützte Befehle

| Befehl | Code | Beschreibung |
|--------|------|--------------|
| Registrierung | `06 00` | ECR am Terminal registrieren |
| Autorisierung | `06 01` | Zahlungstransaktion |
| Abmeldung | `06 02` | Verbindung zum Terminal trennen |
| Vorautorisierung | `06 22` | Vorautorisierung (Hotel, Mietwagen) |
| Stornierung | `06 30` | Vorherige Transaktion stornieren |
| Erstattung | `06 31` | Erstattung durchführen |
| Tagesabschluss | `06 50` | Tagesabschluss durchführen |
| Diagnose | `06 70` | Terminalstatus abfragen |
| Statusabfrage | `05 01` | Terminalzustand prüfen |

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

// Zahlung durchführen (12,50 EUR)
val result = client.authorize(amountInCents = 1250)
if (result.success) {
    println("Zahlung erfolgreich! Verfolgung: ${result.traceNumber}")
    println("Karte: ${result.cardData?.cardType}")
} else {
    println("Zahlung fehlgeschlagen: ${result.resultMessage}")
}

// Tagesabschluss
val eod = client.endOfDay()

// Verbindung trennen
client.disconnect()
```

## Protokollablauf

```
ECR → PT:  Befehl-APDU (z.B. 06 01 für Autorisierung)
PT  → ECR: Bestätigung (80 00 00)
PT  → ECR: Zwischenstatus (04 FF) [wiederholt] - "Karte einführen", "PIN eingeben"...
ECR → PT:  Bestätigung (80 00 00) [für jeden]
PT  → ECR: Statusinformation (04 0F) - Transaktionsergebnis mit BMP-Feldern
ECR → PT:  Bestätigung (80 00 00)
PT  → ECR: Druckzeile (06 D1) [wiederholt] - Belegzeilen
ECR → PT:  Bestätigung (80 00 00) [für jede]
PT  → ECR: Abschluss (06 0F) oder Abbruch (06 1E)
ECR → PT:  Bestätigung (80 00 00)
```

## Geparste BMP-Felder

| BMP | Name | Format |
|-----|------|--------|
| `04` | Betrag | 6 Byte BCD |
| `06` | TLV-Container | BER-TLV-Länge |
| `0B` | Verfolgungsnummer | 3 Byte BCD |
| `0C` | Uhrzeit (HHMMSS) | 3 Byte BCD |
| `0D` | Datum (MMTT) | 2 Byte BCD |
| `0E` | Ablaufdatum (JJMM) | 2 Byte BCD |
| `17` | Kartenfolgenummer | 2 Byte BCD |
| `19` | Zahlungsart | 1 Byte |
| `22` | PAN/EF_ID | LLVAR BCD |
| `27` | Ergebniscode | 1 Byte |
| `29` | Terminal-ID | 4 Byte BCD |
| `2A` | VU-Nummer | 15 Byte ASCII |
| `37` | Originalverfolgung | 3 Byte BCD |
| `3B` | AID | 8 Byte fest |
| `3C` | Zusatzdaten/TLV | LLLVAR |
| `49` | Währungscode | 2 Byte BCD |
| `87` | Belegnummer | 2 Byte BCD |
| `88` | Umsatznummer | 3 Byte BCD |
| `8A` | Kartentyp | 1 Byte |
| `8B` | Kartenname | LLVAR ASCII |
| `8C` | Kartentyp-Netzwerk-ID | 1 Byte |
| `A0` | Ergebniscode AS | 1 Byte |
| `BA` | AID-Parameter | 5 Byte fest |

## Erstellen

```bash
./gradlew :zvt-library:assembleDebug
```

## Voraussetzungen

- Android SDK 36
- Kotlin 1.9.22
- Min SDK 24

## Lizenz

```
MIT-Lizenz

Urheberrecht (c) 2026 Erkan Kaplan

Hiermit wird jeder Person, die eine Kopie dieser Software und der zugehörigen
Dokumentationsdateien (die "Software") erhält, kostenlos die Erlaubnis erteilt,
die Software uneingeschränkt zu nutzen, einschließlich und ohne Einschränkung
der Rechte zur Nutzung, zum Kopieren, Ändern, Zusammenführen, Veröffentlichen,
Verteilen, Unterlizenzieren und/oder Verkaufen von Kopien der Software, und
Personen, denen die Software zur Verfügung gestellt wird, dies unter den
folgenden Bedingungen zu gestatten:

Der obige Urheberrechtshinweis und dieser Genehmigungshinweis müssen in allen
Kopien oder wesentlichen Teilen der Software enthalten sein.

DIE SOFTWARE WIRD "WIE BESEHEN" OHNE JEGLICHE AUSDRÜCKLICHE ODER
STILLSCHWEIGENDE GEWÄHRLEISTUNG BEREITGESTELLT, EINSCHLIESSLICH, ABER NICHT
BESCHRÄNKT AUF DIE GEWÄHRLEISTUNG DER MARKTGÄNGIGKEIT, DER EIGNUNG FÜR EINEN
BESTIMMTEN ZWECK UND DER NICHTVERLETZUNG. IN KEINEM FALL SIND DIE AUTOREN ODER
URHEBERRECHTSINHABER HAFTBAR FÜR JEGLICHE ANSPRÜCHE, SCHÄDEN ODER SONSTIGE
HAFTUNG, OB AUS VERTRAG, UNERLAUBTER HANDLUNG ODER ANDERWEITIG, DIE SICH AUS
DER SOFTWARE ODER DER NUTZUNG DER SOFTWARE ODER ANDEREN GESCHÄFTEN MIT DER
SOFTWARE ERGEBEN.
```
