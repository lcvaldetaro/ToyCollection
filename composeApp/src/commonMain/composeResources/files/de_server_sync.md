<!-- !web -->
Veröffentlichen Sie Ihre Sammlung und synchronisieren Sie Daten über einen privaten Server auf mehreren Geräten.

### 1. Webseiten erstellen
- **Wozu es dient**: Wandelt Ihre Sammlung aus der App-Datenbank in Webseiten (`.html`) in Ihrem Datenordner um.
- **Wie es funktioniert**: Tippen Sie auf **Seiten erstellen**, um die Katalogdateien zu erzeugen. Die App kann diese Seiten anschließend per SFTP auf Ihren Server übertragen.

### 2. Webserver-URL
<!-- /!web -->
<!-- web -->
Synchronisieren Sie Katalog-Updates und zeigen Sie Sammlungsfotos über Ihren privaten Webserver an.

### Webserver-URL
<!-- /web -->
- **Was es bedeutet**: Die Internetadresse (z. B. `http://meineserver.de/datenbank`), unter der Ihre Sammlungsdateien und Fotos gehostet werden.
- **Wie diese App es verwendet**:
  - **Fotos laden**: Wenn die App auf einem Gerät ausgeführt wird, das Fotos nicht lokal gespeichert hat, lädt die App Fotos über diese URL herunter und zeigt sie an.
  - **Aktualisierungen prüfen**: Beim Tippen auf **Speichern & Web-Updates prüfen** prüft die App diese URL auf neuere Katalogdateien und aktualisiert Ihre Sammlung ohne Server-Passwörter.
<!-- !web -->

### 3. SFTP-Informationen & Zugangsdaten
- **Was es bedeutet**: SFTP (Secure File Transfer Protocol) ist die private Verbindung, mit der diese App Dateien auf die Serverfestplatte überträgt und von dort abruft.
- **Wie diese App es verwendet**:
  - **In die Cloud hochladen**: Die App verbindet sich über SFTP, um Ihre Datenbank, Fotos und Webseiten auf Ihren Server hochzuladen.
  - **Aus der Cloud herunterladen**: Die App verbindet sich über SFTP, um Datenbank-Updates und Fotos vom Server auf dieses Gerät herunterzuladen.
  - **Verbindung testen**: Die App überprüft Ihre Servereinstellungen vor der Dateiübertragung.
- **Einstellungen**:
  - **Serveradresse & Port**: Die Netzwerkadresse und der Port (normalerweise 22) Ihres Servers.
<!-- desktop -->
  - **Benutzername & Passwort / Schlüssel**: Ihre Zugangsdaten zur Authentifizierung dieser App auf dem Server (Passwort oder private Schlüsseldatei).
<!-- /desktop -->
<!-- android -->
  - **Benutzername & Passwort**: Ihre Zugangsdaten zur Authentifizierung dieser App auf dem Server.
<!-- /android -->
  - **Entfernter Ordner**: Der Ordnerpfad auf dem Server, in dem die App Sammlungsdateien speichert und abruft.

### 4. Synchronisierungsfunktionen
- **Verbindung testen**: Prüft vor der Dateiübertragung, ob die App den Server erreichen kann.
- **In die Cloud hochladen**: Prüft lokale Dateien und sendet neue oder geänderte Spielzeuge, Hersteller, Fotos und Webseiten an den Server.
- **Aus der Cloud herunterladen**: Prüft den Server und lädt Aktualisierungen herunter, um dieses Gerät synchron zu halten.
<!-- /!web -->
