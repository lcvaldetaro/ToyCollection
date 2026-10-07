Publiez votre collection et synchronisez vos données sur plusieurs appareils à l'aide d'un serveur privé.

### 1. Création de pages web
- **À quoi cela sert** : Transforme votre collection depuis la base de données de l'application en pages web (`.html`) dans votre dossier de données.
- **Comment cela fonctionne** : Appuyez sur **Créer les pages** pour générer les fichiers du catalogue. Une fois créées, l'application les téléverse sur votre serveur par SFTP.

### 2. URL du serveur web
- **Ce que cela signifie** : L'adresse internet (par exemple `http://monsite.fr/collection`) où les fichiers et photos de votre collection sont hébergés.
- **Comment cette application l'utilise** :
  - **Chargement des photos** : Lorsque l'application s'exécute sur un appareil ne disposant pas des photos localement, l'application utilise cette URL pour télécharger et afficher les photos des jouets.
  - **Recherche de mises à jour** : En appuyant sur **Enregistrer & vérifier les mises à jour web**, l'application consulte cette URL pour détecter les versions plus récentes du catalogue et mettre à jour votre collection sans mot de passe serveur.

### 3. Informations et identifiants SFTP
- **Ce que cela signifie** : SFTP (Protocole de Transfert Sécurisé de Fichiers) est la connexion privée que cette application utilise pour transférer des fichiers avec le disque de votre serveur.
- **Comment cette application l'utilise** :
  - **Téléverser vers le cloud** : L'application se connecte par SFTP pour envoyer votre base de données, vos photos et vos pages web sur le serveur.
  - **Télécharger depuis le cloud** : L'application se connecte par SFTP pour récupérer les mises à jour de la base de données et les photos depuis le serveur sur cet appareil.
  - **Tester la connexion** : L'application vérifie vos réglages serveur avant le transfert de fichiers.
- **Paramètres** :
  - **Adresse et port du serveur** : L'adresse réseau et le port (standard 22) de votre serveur.
  - **Nom d'utilisateur et mot de passe / Clé** : Vos identifiants pour authentifier l'application sur le serveur.
  - **Dossier distant** : Le chemin du dossier sur le serveur où l'application enregistre et récupère les fichiers de la collection.

### 4. Actions de synchronisation cloud
- **Tester la connexion** : Confirme que votre appareil peut se connecter au serveur avant le transfert.
- **Téléverser vers le cloud** : Analyse les fichiers locaux et envoie les nouveaux jouets, fabricants, photos et pages web vers le serveur.
- **Télécharger depuis le cloud** : Analyse le serveur et télécharge les mises à jour pour garder cet appareil synchronisé.
