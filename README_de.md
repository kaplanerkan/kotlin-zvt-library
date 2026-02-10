# ZVT-Client-Bibliothek fuer Android (Kotlin)

> [English](README.md) | [Turkce](README_tr.md) | **Deutsch**

Eine Kotlin/Android-Bibliothek, die das **ZVT-Protokoll (v13.13)** fuer die Kommunikation zwischen einer Elektronischen Registrierkasse (ECR) und Zahlungsterminals (PT) ueber TCP/IP implementiert.

## Was ist ZVT?

ZVT (Zahlungsverkehrstechnik) ist das deutsche Standardprotokoll fuer die Kommunikation zwischen Kassensystemen und Zahlungsterminals. Es wird in Deutschland, Oesterreich und der Schweiz haeufig fuer die Kartenzahlungsabwicklung eingesetzt.

- **Spezifikation**: ZVT-Protokollspezifikation v13.13 (PA00P015_13.13_final)
- **Transport**: TCP/IP, Standardport **20007**
- **Kodierung**: Binaere APDU (Anwendungsprotokolldateneinheit)
- **Ressourcen**: Protokollspezifikationen, Dokumentation und Downloads finden Sie unter [terminalhersteller.de](https://www.terminalhersteller.de/downloads.aspx)

## Funktionen

- **Volles ZVT-Protokoll v13.13** - 14 Befehlstypen, 50+ Fehlercodes, 25+ Zwischenstatuscodes
- **Coroutine-basiert** - Alle I/O als Suspend-Funktionen, kein Main-Thread-Blocking
- **Broken-Pipe-Erkennung** - Automatische Trennung bei IOException, Buttons sofort deaktiviert
- **Verbindungsstatus-Schutz** - Buttons deaktiviert bis Terminal REGISTERED ist
- **Material 3 UI** - Tuerkis-Theme, operationsgefaerbte Buttons, Bottom-Navigation
- **Mehrsprachig** - Englisch, Tuerkisch, Deutsch (zur Laufzeit umschaltbar)
- **BCD / TLV Helfer** - Vollstaendige Kodierung/Dekodierung fuer Protokolldatenstrukturen
- **Thread-sicher** - Synchronisiertes Senden, IO-Dispatcher fuer alle Socket-Operationen
- **Datei-Logging** - 30-Tage rollierende Logdateien mit vollstaendigem Protokoll-Trace (TX/RX Hex)
- **Koin DI** - Singleton-Client-Lebenszyklus, ViewModel-Injektion

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
+-- gradle/
    +-- libs.versions.toml        # Zentrale Abhaengigkeitsverwaltung
```

## Demo-App

Die Demo-Anwendung (`app/`) ist ein voll funktionsfaehiger ZVT-Client mit Material-3-UI.

### Was Sie tun koennen

| Bildschirm | Operationen |
|------------|------------|
| **Verbindung** | Verbinden mit Terminal ueber IP:Port, Auto-Registrierung, Trennen |
| **Zahlung** | Zahlung, Erstattung, Stornierung, Abbruch |
| **Vorautorisierung** | Vorautorisierung, Buchung (Book Total), Teilstorno |
| **Terminal** | Diagnose, Statusabfrage, Tagesabschluss, Belegwiederholung, Abmeldung |
| **Log** | Live-Protokoll-Viewer mit Zeitstempeln und Schwerestufen |

### UI-Funktionen

- Operationsgefaerbte Buttons (Tuerkis fuer Zahlungen, Cyan fuer Abfragen, Amber fuer Stornos, Rot fuer Abbruch/Trennen)
- Alle Buttons deaktiviert bis Terminal verbunden und registriert ist
- Echtzeit-Zwischenstatus-Anzeige ("Karte einfuehren", "PIN eingeben" usw.)
- Transaktionsergebnis-Karte mit Kartendaten, Trace-/Belegnummern, Datum/Uhrzeit
- Belegdruck-Unterstuetzung (Druckzeilen vom Terminal)
- Verbindungsstatus-Anzeige (roter/gruener Punkt)
- Sprachumschalter (EN/TR/DE) in der Toolbar
- Registrierungskonfigurationsdialog zur Anpassung des ConfigBytes

### Registrierungskonfiguration (Registration Config)

Der Registrierungskonfigurationsdialog (Zahnrad-Symbol auf dem Verbindungsbildschirm) ermoeglicht die Anpassung des **configByte**, das bei der Registrierung (06 00) gesendet wird. Jede Checkbox entspricht einem Bit im Config-Byte:

| Checkbox | Bit | Hex | Beschreibung |
|----------|-----|-----|--------------|
| Receipt Payment | 1 | `0x02` | ECR druckt Zahlungsbelege (sonst druckt das Terminal) |
| Receipt Admin | 2 | `0x04` | ECR druckt Verwaltungs-/Abschlussbelege |
| Intermediate Status | 3 | `0x08` | Terminal sendet Zwischenstatusmeldungen ("Karte einfuehren", "PIN eingeben") |
| Allow Payment | 4 | `0x10` | ECR steuert die Zahlungsfunktion |
| Allow Admin | 5 | `0x20` | ECR steuert Verwaltungsfunktionen |
| TLV Support | - | - | BMP 06 TLV-Container mit erlaubten Befehlen senden (separates Flag) |

**Standard:** `0x08` (nur Intermediate Status aktiviert). Das ConfigByte wird in Echtzeit als Hex- und Dezimalwert angezeigt. Einstellungen werden in SharedPreferences gespeichert.

| Registrierungskonfigurationsdialog |
|:-:|
| ![Registrierungskonfiguration](screenshots/scrcpy_ojUZoVHZXh.png) |

## Screenshots

| Zahlungsbildschirm | Zahlung (scrollen) | Terminal-Operationen |
|:-:|:-:|:-:|
| ![Zahlung](screenshots/scrcpy_9WEEeROGaO.png) | ![Zahlung scrollen](screenshots/scrcpy_dA8DSZDT7u.png) | ![Terminal](screenshots/scrcpy_McYozJ43by.png) |

| Protokoll-Logs | Registrierungskonfiguration | Live-Zahlung mit CCV A920 |
|:-:|:-:|:-:|
| ![Logs](screenshots/scrcpy_Mj1fauW23I.png) | ![Konfiguration](screenshots/scrcpy_Z0vxa09x5T.png) | ![Hardware](screenshots/WhatsApp%20Image%202026-02-10%20at%2013.41.11.jpeg) |

| Transaktionsergebnis (Detail-Popup) |
|:-:|
| ![Transaktionsergebnis](screenshots/scrcpy_9qoKBcqRmI.png) |

## Storno (Reversal) vs Gutschrift (Refund)

Diese beiden Vorgaenge werden oft verwechselt. Hier ist der wesentliche Unterschied:

**Storno / Reversal (06 30):**
- **Storniert** eine bestehende Zahlung (als waere sie nie passiert)
- Referenziert die Originaltransaktion ueber die Belegnummer
- Muss in der Regel **am selben Tag** erfolgen (vor dem Tagesabschluss)
- Kunde muss die Karte ggf. erneut vorlegen (terminalabhaengig)
- Die Originaltransaktion wird auf dem Host **geloescht/storniert**
- In der Regel **keine Bearbeitungsgebuehr**
- Beispiel: Falscher Betrag an der Kasse eingegeben, sofort korrigieren

**Gutschrift / Refund (06 31):**
- Eine voellig **neue und unabhaengige** Transaktion
- Referenziert die Originaltransaktion **nicht**
- Kann **Tage oder Wochen spaeter** durchgefuehrt werden (keine Frist)
- Kunde **muss** die Karte vorlegen
- Erstellt einen **separaten Datensatz** auf dem Host
- Bearbeitungsgebuehr **wird berechnet** (es ist eine neue Transaktion)
- Beispiel: Kunde gibt Ware zurueck, Geld zurueckerstatten

**Zusammenfassung:**

| | Storno (Reversal) | Gutschrift (Refund) |
|---|---|---|
| Transaktionstyp | Storniert Original | Neue unabhaengige Transaktion |
| Zeitlimit | Gleicher Tag (vor Tagesabschluss) | Kein Limit |
| Belegnummer | Erforderlich | Nicht erforderlich |
| Bearbeitungsgebuehr | Keine | Ja |
| Karte | Terminalabhaengig | Muss vorgelegt werden |
| Host-Datensatz | Original wird geloescht | Separater Datensatz |

Kurz gesagt: Storno = "diese Transaktion hat nie stattgefunden", Gutschrift = "eine neue Rueckerstattung durchfuehren".

## ZVT-Befehls-Hex-Codes

### ECR -> Terminal Befehle

| Befehl | Hex-Code | Beschreibung |
|--------|----------|--------------|
| Registrierung | `06 00` | ECR am Terminal registrieren, Waehrung und Konfiguration setzen |
| Autorisierung | `06 01` | Zahlungstransaktion (Betrag in BCD) |
| Abmeldung (Log Off) | `06 02` | Ordnungsgemaesse Trennung vom Terminal |
| Abbruch (Abort) | `06 B0` | Laufende Operation abbrechen (ECR→PT) |
| Belegwiederholung | `06 20` | Letzten Beleg erneut drucken |
| Vorautorisierung | `06 22` | Betrag reservieren (Hotel, Mietwagen) |
| Buchung (Book Total) | `06 24` | Vorautorisierung abschliessen |
| Teilstorno | `06 25` | Transaktion teilweise stornieren |
| Stornierung (Reversal) | `06 30` | Vorherige Transaktion stornieren |
| Erstattung (Refund) | `06 31` | Erstattung durchfuehren |
| Tagesabschluss | `06 50` | Tagesabschluss durchfuehren |
| Diagnose | `06 70` | Terminalstatus abfragen |
| Statusabfrage | `05 01` | Terminalzustand pruefen |

### Terminal -> ECR Antworten

| Antwort | Hex-Code | Beschreibung |
|---------|----------|--------------|
| ACK | `80 00 00` | Positive Bestaetigung |
| NACK | `84 00 00` | Negative Bestaetigung (Befehl abgelehnt) |
| Abschluss (Completion) | `06 0F` | Transaktion abgeschlossen |
| Statusinformation | `04 0F` | Transaktionsergebnis mit BMP-Feldern |
| Zwischenstatus | `04 FF` | "Karte einfuehren", "PIN eingeben" usw. |
| Druckzeile (Print Line) | `06 D1` | Einzelne Belegzeile |
| Textblock (Print Text) | `06 D3` | Beleg-Textblock |
| Abbruch (Abort) | `06 1E` | Transaktion vom Terminal abgebrochen |

> **Hinweis zu Abort (06 B0 vs 06 1E):** Das ZVT-Protokoll verwendet zwei verschiedene Abort-Codes je nach Richtung. `06 B0` ist der ECR→PT-Befehl, um eine laufende Transaktion von der ECR-Seite abzubrechen. `06 1E` ist die PT→ECR-Antwort, die das Terminal sendet, um die ECR darueber zu informieren, dass eine Transaktion abgebrochen wurde (z.B. wenn der Benutzer die Abbruchtaste am Terminal drueckt). `06 1E` als ECR→PT-Befehl zu senden funktioniert nicht — das Terminal ignoriert es, da es ein Antwortcode ist und kein Befehl.

### APDU-Paketformat

```
[CMD_CLASS(1)] [CMD_INSTR(1)] [LEN(1-3)] [DATA(N)]

Laengenkodierung:
  0x00-0xFE  -> 1 Byte Laenge (0-254 Bytes Daten)
  0xFF LL HH -> 3 Byte erweiterte Laenge (Little-Endian, bis 65535 Bytes)

Beispiele:
  06 00 08 ...        -> Registrierung, 8 Bytes Daten
  06 01 07 ...        -> Autorisierung, 7 Bytes Daten
  80 00 00            -> ACK (keine Daten)
  04 FF 03 ...        -> Zwischenstatus, 3 Bytes Daten
```

### Beispiel: Registrierungspaket

```
TX: 06 00 08 00 00 00 08 09 78 03 00
     |  |  |  |        |  |     |  |
     |  |  |  +--------+  |     |  +-- Service-Byte-Wert (0x00)
     |  |  |  Passwort     |     +-- BMP 0x03 (Service-Byte-Tag)
     |  |  |  (000000 BCD) |
     |  |  |               +-- Waehrungscode (0978 = EUR, positionell)
     |  |  +-- Laenge: 8 Bytes
     |  +-- Instruktion: 0x00
     +-- Klasse: 0x06 (Registrierung)
```

### Beispiel: Autorisierungspaket

```
TX: 06 01 07 04 00 00 00 01 00 19 40
     |  |  |  |  |              |  |
     |  |  |  |  +--------------+  +-- Zahlungsart EC-Cash (0x40)
     |  |  |  |  Betrag: 000001 00     BMP 0x19
     |  |  |  |  = 1,00 EUR (BCD)
     |  |  |  +-- BMP 0x04 (Betrag-Tag)
     |  |  +-- Laenge: 7 Bytes
     +--+-- Befehl: 06 01 (Autorisierung)
```

## Ergebniscodes (BMP 0x27)

| Code | Konstante | Beschreibung |
|------|-----------|--------------|
| `00` | `RC_SUCCESS` | Erfolgreich |
| `64` | `RC_CARD_NOT_READABLE` | Karte nicht lesbar (LRC/Paritaetsfehler) |
| `65` | `RC_CARD_DATA_NOT_PRESENT` | Kartendaten nicht vorhanden |
| `66` | `RC_PROCESSING_ERROR` | Verarbeitungsfehler |
| `6C` | `RC_ABORT_VIA_TIMEOUT` | Abbruch ueber Timeout oder Abbruchtaste |
| `6F` | `RC_WRONG_CURRENCY` | Falsche Waehrung |
| `78` | `RC_CARD_EXPIRED` | Karte abgelaufen |
| `7D` | `RC_COMMUNICATION_ERROR` | Kommunikationsfehler |
| `9A` | `RC_ZVT_PROTOCOL_ERROR` | ZVT-Protokollfehler |
| `A0` | `RC_RECEIVER_NOT_READY` | Empfaenger nicht bereit |
| `B4` | `RC_ALREADY_REVERSED` | Bereits storniert |
| `B5` | `RC_REVERSAL_NOT_POSSIBLE` | Stornierung nicht moeglich |
| `C2` | `RC_DIAGNOSIS_REQUIRED` | Diagnose erforderlich |
| `C3` | `RC_MAX_AMOUNT_EXCEEDED` | Maximalbetrag ueberschritten |
| `FF` | `RC_SYSTEM_ERROR` | Systemfehler |

> Vollstaendige Liste: 50+ Ergebniscodes definiert in `ZvtConstants.kt`

## Zwischenstatuscodes (04 FF)

| Code | Nachricht |
|------|-----------|
| `00` | Warten auf Betragsbestaetigung |
| `01` | Bitte PIN-Pad beobachten |
| `04` | Warten auf FEP-Antwort |
| `0A` | Karte einfuehren |
| `0B` | Bitte Karte entfernen |
| `0E` | Bitte warten |
| `15` | Falsche PIN |
| `18` | PIN-Versuchslimit ueberschritten |
| `1C` | Genehmigt, bitte Ware entnehmen |
| `1D` | Abgelehnt |

## Geparste BMP-Felder

| BMP | Name | Format |
|-----|------|--------|
| `04` | Betrag | 6 Byte BCD |
| `06` | TLV-Container | BER-TLV-Laenge |
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
| `49` | Waehrungscode | 2 Byte BCD |
| `87` | Belegnummer | 2 Byte BCD |
| `88` | Umsatznummer | 3 Byte BCD |
| `8A` | Kartentyp | 1 Byte |
| `8B` | Kartenname | LLVAR ASCII |
| `8C` | Kartentyp-Netzwerk-ID | 1 Byte |
| `A0` | Ergebniscode AS | 1 Byte |
| `BA` | AID-Parameter | 5 Byte fest |

## Kartentyp-IDs (BMP 0x8A)

Basierend auf ZVT-Spezifikation v13.13, Kapitel 12: "Liste der ZVT-Kartentyp-IDs".

| ID (dez) | Hex | Kartentyp |
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
| 255 | `0xFF` | Siehe TLV-Tag 41 (ID >= 256) |

> Vollstaendige Liste: 280+ Kartentypen in ZVT-Spezifikation Kapitel 12 definiert. IDs >= 256 verwenden TLV-Tag 41 anstelle von BMP 0x8A.

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
val booking = client.bookTotal(amountInCents = 4200, receiptNumber = preAuth.receiptNumber) // 42,00 buchen

// Teilstorno
val partial = client.partialReversal(amountInCents = 500, receiptNumber = 123) // 5,00 teilstornieren

// Letzten Beleg wiederholen
val receipt = client.repeatReceipt()

// Tagesabschluss
val eod = client.endOfDay()

// Verbindung trennen
client.logOff()
client.disconnect()
```

## Protokollablauf

```
ECR -> PT:  Befehl-APDU (z.B. 06 01 fuer Autorisierung)
PT  -> ECR: Bestaetigung (80 00 00)
PT  -> ECR: Zwischenstatus (04 FF) [wiederholt] - "Karte einfuehren", "PIN eingeben"...
ECR -> PT:  Bestaetigung (80 00 00) [fuer jeden]
PT  -> ECR: Statusinformation (04 0F) - Transaktionsergebnis mit BMP-Feldern
ECR -> PT:  Bestaetigung (80 00 00)
PT  -> ECR: Druckzeile (06 D1) [wiederholt] - Belegzeilen
ECR -> PT:  Bestaetigung (80 00 00) [fuer jede]
PT  -> ECR: Abschluss (06 0F) oder Abbruch (06 1E)
ECR -> PT:  Bestaetigung (80 00 00)
```

## Fehlerbehebung / Protokolldateien

Die Anwendung schreibt automatisch Protokolldateien auf den Geraetespeicher. Die Protokolle werden **30 Tage** lang aufbewahrt und alte Dateien werden automatisch geloescht.

**Speicherort der Protokolldateien:**
```
Android/data/com.panda_erkan.zvtclientdemo/files/Download/logs/zvt_JJJJ-MM-TT.log
```

**Zugriffsmethode:**
1. Verbinden Sie das Geraet per USB mit einem Computer
2. Oeffnen Sie den Geraetespeicher und navigieren Sie zum obigen Pfad
3. Oder verwenden Sie eine Dateimanager-App auf dem Geraet

**Protokollformat:**
```
2026-02-10 14:30:15.123 D/ZVT: ECR -> PT | TX Registration (06 00) | 11 bytes | 06-00-08-00-00-00-08-09-78-03-00
2026-02-10 14:30:15.456 D/ZVT: PT -> ECR | RX ACK (80 00) | 3 bytes | 80-00-00
```

Wenn ein Problem auftritt, fuegen Sie bitte die entsprechende Protokolldatei bei der Fehlermeldung bei.

## Erstellen

```bash
./gradlew :panda-zvt-library:assembleDebug   # Nur Bibliothek
./gradlew :app:assembleDebug                 # Demo-App
```

## Voraussetzungen & Technologie-Stack

| Kategorie | Technologie | Version |
|-----------|------------|---------|
| Sprache | Kotlin | 2.3.10 |
| Min SDK | Android | API 24 (Android 7.0) |
| Ziel/Compile SDK | Android | API 36 |
| JVM-Ziel | Java | 17 |
| Build-System | Gradle (AGP) | 8.13.2 |
| UI-Framework | Material Design 3 | 1.13.0 |
| Architektur | MVVM | - |
| DI-Framework | Koin | 4.1.1 |
| Asynchron | Kotlin Coroutines | 1.10.2 |
| Navigation | Jetpack Navigation | 2.9.7 |
| Lebenszyklus | Jetpack Lifecycle (ViewModel, LiveData, StateFlow) | 2.10.0 |
| View Binding | Android Data Binding | - |
| Logging | Timber | 5.0.1 |
| Speichererkennung | LeakCanary (nur Debug) | 2.14 |
| UI-Komponenten | RecyclerView, CardView, ConstraintLayout | aktuell |

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
