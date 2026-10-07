# Anleitung zur Einrichtung des SFTP-Servers

Diese Anleitung erklärt, wie Sie einen **SFTP-Server** einrichten, um Ihre Spielzeug-Datenbank auf mehreren Geräten zu synchronisieren.

---

## Was ist ein SFTP-Server?
Ein **SFTP-Server (Secure File Transfer Protocol)** ist ein sicherer, privater Speicherplatz im Internet.

### Warum benötige ich einen?
1. **Automatische Synchronisierung**: Ihre Sammlungsdaten werden sicher auf Ihrem privaten Server gespeichert. Bei Verlust oder Zurücksetzen des Geräts gehen keine Daten verloren.
2. **Geräteübergreifende Synchronisierung**: Sie können die App auf Computer, Tablet und Smartphone ausführen und dieselbe Datenbank synchronisieren.

---

## Welche Informationen werden benötigt?
Um die Verbindung herzustellen, geben Sie diese **5 Angaben** auf der Registerkarte **Einstellungen** ein:

* **SFTP-Host**: Die Internetadresse Ihres Servers (z. B. `sftp.meinesammlung.de` oder `192.168.1.50`).
* **Port**: Der Kommunikationskanal für sichere Übertragungen (üblicherweise `22`).
* **Benutzername**: Das Konto auf dem Server für den Dateizugriff.
* **Passwort** (oder SSH-Schlüssel): Das sichere Authentifizierungsmerkmal.
* **SFTP-Verzeichnis (Entfernter Pfad)**: Der Zielordner auf dem Server für Datenbank und Fotos (z. B. `/home/benutzer/toydb/` oder `./toydb/`). Bleibt dieses Feld leer, wird das Standard-Heimatverzeichnis genutzt.

---

## Cloud-Anbieter
Sie können einen SFTP-Server bei verschiedenen Cloud-Dienstleistern einrichten:

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * *AWS Transfer Family* oder kostengünstig mit *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * Virtuelle Maschine auf *Google Compute Engine* (kostenlose Kontingente verfügbar).
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * Direkte *SFTP-Unterstützung für Azure Blob Storage*.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * Einfach einzurichtende *Droplets*.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Günstige virtuelle Linux-Server (VPS) mit standardmäßigem SFTP.
