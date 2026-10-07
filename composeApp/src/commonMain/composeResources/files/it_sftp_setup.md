# Guida alla Configurazione del Server SFTP

Questa guida spiega come configurare un **server SFTP** per sincronizzare il Database di Giocattoli su più dispositivi.

---

## Cos'è un Server SFTP?
Un **Server SFTP (Secure File Transfer Protocol)** è uno spazio di archiviazione privato e sicuro su Internet.

### Perché ne ho bisogno?
1. **Sincronizzazione Automatica**: I dati della tua collezione sono salvati in sicurezza sul tuo server privato. Se perdi o sostituisci il dispositivo, non perderai mai i tuoi dati.
2. **Sincronizzazione Multi-Dispositivo**: Puoi usare l'app su computer, tablet e smartphone, sincronizzando la stessa collezione tra tutti i dispositivi.

---

## Quali Informazioni Servono?
Per connettere l'app al tuo server, dovrai inserire queste **5 informazioni chiave** nella scheda **Configurazione**:

* **Host SFTP**: L'indirizzo Internet del server (es. `sftp.miacollezione.com` o `192.168.1.50`).
* **Porta**: Il canale di comunicazione per i trasferimenti sicuri (generalmente `22`).
* **Nome Utente**: L'account creato sul server per accedere ai file.
* **Password** (o Chiave SSH): La credenziale segreta per autenticarsi in modo sicuro.
* **Percorso Remoto SFTP**: La cartella specifica sul server in cui archiviare il database e le foto (es. `/home/utente/toydb/` o `./toydb/`). Se vuoto, usa la directory predefinita.

---

## Provider Cloud
Puoi configurare un server SFTP con molti provider cloud affidabili:

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * *AWS Transfer Family* oppure un server economico con *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * Macchina virtuale su *Google Compute Engine* (opzioni gratuite disponibili).
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * Supporto *SFTP su Azure Blob Storage*.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * *Droplet* semplici da attivare e gestire.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Server privati virtuali (VPS) economici con Linux e SFTP.
