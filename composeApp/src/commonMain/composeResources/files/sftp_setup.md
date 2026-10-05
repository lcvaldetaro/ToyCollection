# SFTP Server Setup Guide

This guide explains how to set up an **SFTP server** to synchronize and back up your Toy Database across multiple devices.

---

## What is an SFTP Server?
An **SFTP (Secure File Transfer Protocol) Server** is a secure, private storage space on the internet. 

### Why do I need one?
1. **Automatic Backup**: Your collection data is stored safely on your private server. If you lose or reset your phone or computer, your data is never lost.
2. **Multi-Device Synchronization**: You can run this app on your computer, tablet, and phone, and sync the same database among all of them via your server.

---

## What Information Do I Need?
To connect the Toy Database app to your server, you will need to input these **5 key pieces of information** in the **Setup** (Settings) tab of the app:

* **SFTP Host**: The internet address of your server (e.g., `sftp.mycollection.com` or `192.168.1.50`).
* **Port**: The communication channel used for secure transfers (usually `22`).
* **Username**: The account name created on the server to access your files.
* **Password** (or SSH Key): The secret key to securely authenticate your identity.
* **SFTP Directory (Remote Path)**: The specific folder on the server where the database file and toy images will be stored (e.g., `/home/username/toydb/` or `./toydb/`). This folder is created or used to keep your collection files separate from other files on the server. If left empty, it uses the server's default home directory.

---

## Cloud Providers
You can set up an SFTP server using many cloud computing providers. These services usually offer a free trial or a low-cost virtual computer where your server runs.

Here are the largest and most reliable providers you can use:

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * *AWS Transfer Family* offers managed SFTP endpoints, or you can run a simple, low-cost virtual server using *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * You can run a small virtual machine on *Google Compute Engine* to host your SFTP server, which falls under their free tier options.
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * Azure offers *Azure Blob Storage SFTP support* as a direct, serverless SFTP storage option.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * Famous for simplicity, their *Droplets* (starting at $4-$5/month) are very easy to set up with standard SFTP.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Another highly affordable, simple provider of virtual private servers (VPS) that run Linux and support SFTP out of the box.
