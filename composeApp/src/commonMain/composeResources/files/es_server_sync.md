Publique su colección y sincronice datos en varios dispositivos mediante un servidor privado.

### 1. Creación de páginas web
- **Para qué sirve**: Convierte su colección desde la base de datos de la aplicación en páginas web (`.html`) en su carpeta de datos.
- **Cómo funciona**: Pulse **Crear páginas** para generar los archivos del catálogo. Una vez creados, esta aplicación los sube a su servidor mediante SFTP.

### 2. URL del servidor web
- **Qué significa**: La dirección de internet (por ejemplo, `http://miservidor.com/catalogo`) donde se alojan los archivos y fotos de su colección.
- **Cómo la utiliza esta aplicación**:
  - **Carga de fotos**: Cuando la aplicación se ejecuta en un dispositivo que no tiene las fotos guardadas localmente, utiliza esta URL para descargar y mostrar las fotos de los juguetes.
  - **Comprobación de actualizaciones**: Al pulsar **Guardar y comprobar actualizaciones web**, la aplicación comprueba esta URL en busca de archivos más recientes para actualizar su colección sin necesidad de contraseñas del servidor.

### 3. Información y credenciales SFTP
- **Qué significa**: SFTP (Protocolo de Transferencia Segura de Archivos) es la conexión privada que utiliza esta aplicación para transferir archivos al disco del servidor.
- **Cómo la utiliza esta aplicación**:
  - **Subir a la nube**: La aplicación se conecta por SFTP para subir su base de datos, fotos y páginas web a su servidor.
  - **Descargar de la nube**: La aplicación se conecta por SFTP para descargar actualizaciones de la base de datos y fotos desde el servidor a este dispositivo.
  - **Probar conexión**: La aplicación verifica la configuración del servidor antes de transferir archivos.
- **Ajustes**:
  - **Dirección y puerto del servidor**: Dirección de red y puerto (habitualmente 22) de su servidor.
  - **Usuario y contraseña / Clave**: Sus credenciales para autenticar esta aplicación en el servidor.
  - **Carpeta remota**: La ruta en el servidor donde la aplicación guarda y descarga los archivos de la colección.

### 4. Acciones de sincronización en la nube
- **Probar conexión**: Confirma que el dispositivo puede conectarse al servidor antes de transferir archivos.
- **Subir a la nube**: Examina los archivos locales y envía juguetes, fabricantes, fotos y páginas web nuevos o modificados al servidor.
- **Descargar de la nube**: Examina el servidor y descarga actualizaciones para mantener este dispositivo al día.
