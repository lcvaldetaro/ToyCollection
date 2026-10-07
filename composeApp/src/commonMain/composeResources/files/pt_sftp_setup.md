# Guia de Configuração do Servidor SFTP

Este guia explica como configurar um **servidor SFTP** para sincronizar seu Banco de Dados de Brinquedos em múltiplos dispositivos.

---

## O que é um Servidor SFTP?
Um **Servidor SFTP (Secure File Transfer Protocol)** é um espaço de armazenamento privado e seguro na internet.

### Por que preciso de um?
1. **Sincronização Automática**: Os dados da sua coleção ficam guardados com segurança no seu servidor privado. Se você perder ou trocar seu telefone ou computador, seus dados nunca serão perdidos.
2. **Sincronização em Múltiplos Dispositivos**: Você pode executar este aplicativo no computador, tablet e celular, sincronizando o mesmo banco de dados entre todos eles por meio do seu servidor.

---

## Quais Informações São Necessárias?
Para conectar o aplicativo ao seu servidor, você precisará informar estes **5 dados fundamentais** na aba **Configuração** (Configurações) do aplicativo:

* **Host SFTP**: O endereço de internet do seu servidor (por exemplo, `sftp.minhacolecao.com` ou `192.168.1.50`).
* **Porta**: O canal de comunicação utilizado para transferências seguras (geralmente `22`).
* **Nome de Usuário**: O nome da conta criada no servidor para acessar seus arquivos.
* **Senha** (ou Chave SSH): A credencial secreta para autenticar sua identidade com segurança.
* **Diretório SFTP (Caminho Remoto)**: A pasta específica no servidor onde o arquivo de banco de dados e as fotos dos brinquedos serão armazenados (por exemplo, `/home/usuario/toydb/` ou `./toydb/`). Essa pasta é utilizada para manter os arquivos da sua coleção separados dos demais arquivos no servidor. Se deixada em branco, utiliza o diretório inicial padrão do servidor.

---

## Provedores em Nuvem
Você pode configurar um servidor SFTP em diversos provedores de computação em nuvem. Esses serviços geralmente oferecem um período de teste gratuito ou servidores virtuais de baixo custo.

Aqui estão os provedores mais confiáveis e populares:

1. **[Amazon Web Services (AWS)](https://aws.amazon.com/)**
   * O *AWS Transfer Family* oferece pontos de extremidade SFTP gerenciados, ou você pode executar um servidor virtual simples e econômico usando o *Amazon Lightsail*.
2. **[Google Cloud Platform (GCP)](https://cloud.google.com/)**
   * Você pode executar uma máquina virtual leve no *Google Compute Engine* para hospedar seu servidor SFTP, aproveitando as opções de nível gratuito.
3. **[Microsoft Azure](https://azure.microsoft.com/)**
   * O Azure oferece suporte a *SFTP no Azure Blob Storage* como uma solução direta e sem servidor.
4. **[DigitalOcean](https://www.digitalocean.com/)**
   * Reconhecida pela simplicidade, os *Droplets* (a partir de US$ 4 a US$ 5/mês) são muito fáceis de configurar com SFTP padrão.
5. **[Linode / Akamai](https://www.linode.com/)**
   * Outro provedor muito acessível e prático de servidores virtuais privados (VPS) com Linux e suporte a SFTP integrado.
