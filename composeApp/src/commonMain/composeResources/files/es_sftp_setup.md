# Guía de Configuración del Servidor SFTP

Esta guía explica cómo configurar un **servidor SFTP** para sincronizar su Base de Datos de Juguetes entre múltiples dispositivos.

---

## ¿Qué es un Servidor SFTP?
Un **Servidor SFTP (Protocolo de Transferencia Segura de Archivos)** es un espacio de almacenamiento privado y seguro en internet.

### ¿Por qué lo necesito?
1. **Sincronización Automática**: Los datos de su colección se guardan de forma segura en su servidor privado. Si pierde o cambia su teléfono o computadora, nunca perderá sus datos.
2. **Sincronización Multidispositivo**: Puede ejecutar esta aplicación en su computadora, tableta y teléfono, sincronizando la misma base de datos entre todos ellos a través de su servidor.

---

## ¿Qué Información Necesito?
Para conectar la aplicación a su servidor, necesitará ingresar estos **5 datos clave** en la pestaña **Configuración** de la aplicación:

* **Host SFTP**: La dirección de internet de su servidor (por ejemplo, `sftp.micoleccion.com` o `192.168.1.50`).
* **Puerto**: El canal de comunicación utilizado para transferencias seguras (generalmente `22`).
* **Nombre de Usuario**: El nombre de la cuenta creada en el servidor para acceder a sus archivos.
* **Contraseña** (o Clave SSH): La credencial secreta para autenticar su identidad de forma segura.
* **Directorio SFTP (Ruta Remota)**: La carpeta específica en el servidor donde se almacenarán la base de datos y las fotos (por ejemplo, `/home/usuario/toydb/` o `./toydb/`). Si se deja vacío, utilizará el directorio principal predeterminado del servidor.

---

## Proveedores en la Nube
Puede configurar un servidor SFTP utilizando diversos proveedores de computación en la nube:

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * *AWS Transfer Family* ofrece puntos SFTP administrados, o puede usar un servidor virtual de bajo costo con *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * Puede ejecutar una máquina virtual en *Google Compute Engine* dentro de las opciones de nivel gratuito.
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * Azure ofrece soporte de *SFTP en Azure Blob Storage*.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * Muy sencillo de configurar con *Droplets*.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Servidores virtuales económicos con Linux y SFTP integrado.
