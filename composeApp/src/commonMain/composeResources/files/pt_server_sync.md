Publique sua coleção e sincronize seus dados em vários dispositivos usando um servidor privado.

### 1. Criar Páginas Web
- **Para que serve**: Converte sua coleção a partir do banco de dados do aplicativo em páginas web (`.html`) na sua pasta de dados.
- **Como funciona**: Toque em **Criar Páginas** para gerar os arquivos do catálogo. Uma vez criadas, o aplicativo pode enviar essas páginas para o seu servidor via SFTP.

### 2. URL do Servidor Web
- **O que significa**: O endereço na internet (por exemplo, `http://meusite.com/catalogo`) onde os arquivos e fotos da sua coleção estão hospedados.
- **Como este aplicativo usa**:
  - **Carregamento de Fotos**: Quando o aplicativo é executado em um dispositivo que não possui as fotos salvas localmente, o app usa essa URL para baixar e exibir as fotos dos brinquedos.
  - **Verificação de Atualizações**: Ao tocar em **Salvar e Verificar Atualizações**, o app consulta essa URL em busca de versões mais recentes do catálogo, atualizando sua coleção sem precisar de senhas do servidor.

### 3. Informações e Credenciais SFTP
- **O que significa**: SFTP (Secure File Transfer Protocol) é a conexão privada que este aplicativo usa para transferir arquivos diretamente com o disco do seu servidor.
- **Como este aplicativo usa**:
  - **Enviar para a Nuvem**: O aplicativo conecta via SFTP para enviar seu banco de dados, fotos e páginas web para o seu servidor.
  - **Baixar da Nuvem**: O aplicativo conecta via SFTP para baixar atualizações do banco de dados e fotos do servidor para este dispositivo.
  - **Testar Conexão**: O aplicativo verifica as configurações do servidor antes de transferir qualquer arquivo.
- **Configurações**:
  - **Endereço e Porta do Servidor**: O endereço de rede e a porta (geralmente 22) do seu servidor.
  - **Usuário e Senha / Chave**: Seus dados de acesso para autenticar este aplicativo no servidor.
  - **Pasta Remota**: O caminho da pasta no servidor onde o aplicativo armazena e lê os arquivos da coleção.

### 4. Ações de Sincronização na Nuvem
- **Testar Conexão**: Confirma que o aplicativo consegue se conectar ao servidor antes de iniciar a transferência.
- **Enviar para a Nuvem**: Examina os arquivos locais e envia brinquedos, fabricantes, fotos e páginas web novos ou modificados para o seu servidor.
- **Baixar da Nuvem**: Examina o servidor e baixa atualizações para manter este dispositivo sincronizado.
