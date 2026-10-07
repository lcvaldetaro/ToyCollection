# Guide de Configuration du Serveur SFTP

Ce guide explique comment configurer un **serveur SFTP** pour synchroniser votre Base de Données de Jouets sur plusieurs appareils.

---

## Qu'est-ce qu'un Serveur SFTP ?
Un **Serveur SFTP (Secure File Transfer Protocol)** est un espace de stockage privé et sécurisé sur Internet.

### Pourquoi en ai-je besoin ?
1. **Synchronisation Automatique** : Les données de votre collection sont conservées en sécurité sur votre serveur privé. En cas de perte ou de réinitialisation de votre appareil, vos données ne sont jamais perdues.
2. **Synchronisation Multi-Appareils** : Vous pouvez utiliser cette application sur ordinateur, tablette et téléphone, et synchroniser la même collection entre tous vos appareils.

---

## De Quelles Informations Ai-je Besoin ?
Pour connecter l'application à votre serveur, saisissez ces **5 informations clés** dans l'onglet **Configuration** :

* **Hôte SFTP** : L'adresse Internet de votre serveur (par exemple `sftp.macollection.com` ou `192.168.1.50`).
* **Port** : Le canal de communication pour les transferts sécurisés (généralement `22`).
* **Nom d'utilisateur** : Le compte créé sur le serveur pour accéder à vos fichiers.
* **Mot de passe** (ou Clé SSH) : L'identifiant secret pour vous authentifier en toute sécurité.
* **Répertoire SFTP (Chemin distant)** : Le dossier spécifique sur le serveur où la base de données et les photos seront enregistrées (par exemple `/home/utilisateur/toydb/` ou `./toydb/`). Laissé vide, il utilise le répertoire personnel par défaut.

---

## Fournisseurs Cloud
Plusieurs fournisseurs de cloud permettent d'héberger un serveur SFTP facilement :

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * *AWS Transfer Family* ou une instance virtuelle simple avec *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * Une machine virtuelle légère sur *Google Compute Engine* (offre gratuite disponible).
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * Prise en charge du protocole *SFTP sur Azure Blob Storage*.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * Les *Droplets* sont simples et rapides à configurer.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Serveurs privés virtuels (VPS) Linux très abordables.
