Pubblica la tua collezione e sincronizza i dati su più dispositivi utilizzando un server privato.

### 1. Creazione di pagine web
- **A cosa serve**: Converte la tua collezione dal database dell'app in pagine web (`.html`) nella tua cartella dati.
- **Come funziona**: Tocca **Crea pagine** per generare i file del catalogo. Una volta create, l'app carica queste pagine sul tuo server tramite SFTP.

### 2. URL del server web
- **Cosa significa**: L'indirizzo internet (ad esempio `http://miosito.it/collezione`) in cui sono ospitati i file e le foto della collezione.
- **Come viene utilizzato da questa app**:
  - **Caricamento foto**: Quando l'app viene eseguita su un dispositivo che non ha le foto salvate localmente, l'app usa questo URL per scaricare e visualizzare le foto dei giocattoli.
  - **Verifica aggiornamenti**: Toccando **Salva e controlla aggiornamenti web**, l'app controlla questo URL per rilevare file di catalogo più recenti e aggiornare la collezione senza password del server.

### 3. Informazioni e credenziali SFTP
- **Cosa significa**: SFTP (Secure File Transfer Protocol) è la connessione privata che questa app usa per trasferire file con lo spazio disco del server.
- **Come viene utilizzato da questa app**:
  - **Carica sul cloud**: L'app si connette tramite SFTP per caricare database, foto e pagine web sul tuo server.
  - **Scarica dal cloud**: L'app si connette tramite SFTP per scaricare gli aggiornamenti del database e le foto dal server su questo dispositivo.
  - **Verifica connessione**: L'app verifica le impostazioni del server prima del trasferimento.
- **Impostazioni**:
  - **Indirizzo e porta del server**: L'indirizzo di rete del tuo server e la porta (standard 22).
  - **Nome utente e password / Chiave**: Le tue credenziali per autenticare questa app sul server.
  - **Cartella remota**: Il percorso della cartella sul server in cui l'app salva e recupera i file della collezione.

### 4. Azioni di sincronizzazione cloud
- **Verifica connessione**: Controlla che il dispositivo riesca a connettersi al server prima del trasferimento.
- **Carica sul cloud**: Esamina i file locali e invia giocattoli, produttori, foto e pagine web nuovi o aggiornati al server.
- **Scarica dal cloud**: Esamina il server e scarica gli aggiornamenti per mantenere sincronizzato questo dispositivo.
