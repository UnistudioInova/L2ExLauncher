# L2ExLauncher

L2ExLauncher é um launcher e downloader para clientes Lineage II. O projeto combina uma interface gráfica Swing com o backend de download embutido no mesmo aplicativo Java.

O backend não é um serviço separado: a interface e o pipeline de processamento são compilados no mesmo artefato executável.

## Funcionalidades

- Interface gráfica com tema escuro baseada em FlatLaf.
- Modo de linha de comando para automação e diagnóstico.
- Catálogo de versões e clientes por região e ramo.
- Download individual ou completo de clientes.
- Downloads simples, divididos por faixa de bytes, por partes CDN e estratégias híbridas.
- Processamento paralelo de downloads, com controle de concorrência.
- Armazenamento em memória, temporário ou híbrido.
- Descompressão LZMA e ZIP.
- Validação por hash e tamanho.
- Restauração de arquivos já válidos.
- Comparação com uma cópia local antes de acessar a CDN.
- Consulta da versão atual dos serviços NCSoft.
- Suporte a fontes NCSoft, 4game/Innova, UpNova e Akumu.
- Geração e exportação de catálogos de arquivos.

## Arquitetura

O projeto é organizado em duas camadas dentro do mesmo JAR:

```text
src/main/java/org/l2explorer/launcher/frontend/
└── L2ExLauncher.java       Interface gráfica Swing

src/main/java/org/l2explorer/launcher/backend/
├── Controller.java          Entrada do fluxo CLI
├── model/                   Geradores, manifestos, CDN e processamento
├── impl/                    Coordenação do pipeline
├── instancemanager/         Download, descompressão, hash e armazenamento
├── interfaces/              Contratos do backend
└── cli/                     Argumentos e comandos da linha de comando
```

O pipeline segue, em linhas gerais:

```text
fonte de versão → lista de arquivos → download → descompressão → validação → armazenamento
```

## Requisitos

- Java 25 ou compatível.
- Maven 3.9 ou superior.
- Acesso à internet para consultar catálogos e baixar arquivos.

Algumas dependências locais permanecem em `libs/`, incluindo `ConfigFieldParser`, `dom4j` e `json-simple`.

## Compilação

Na raiz do projeto:

```bash
mvn clean package
```

Os artefatos são gerados em `target/` e `build/`:

- JAR executável com dependências incorporadas;
- ZIP de distribuição com JAR, configurações e imagens necessárias.

O ponto de entrada da aplicação é:

```text
org.l2explorer.launcher.frontend.L2ExLauncher
```

## Execução gráfica

Após a compilação:

```bash
java -jar target/l2exlauncher-<versão>.jar
```

A interface permite selecionar região, ramo, cliente, versão e pasta de destino. O catálogo pode ser atualizado pela própria aplicação.

## Execução CLI

O backend também pode ser executado diretamente:

```bash
java -cp target/l2exlauncher-0.0.8.jar \
  org.l2explorer.launcher.backend.Controller \
  -cdn NC_SOFT_AMERICA \
  -version 215
```

Use `-help` para listar todos os argumentos disponíveis.

Principais opções:

| Opção | Descrição |
|---|---|
| `-cdn <source>` | Fonte do cliente ou CDN. |
| `-version <n>` | Versão/patch a baixar. |
| `-last_version` | Consulta a versão atual sem baixar o cliente. |
| `-path <dir>` | Diretório de saída. |
| `-download_mode <mode>` | `ALL_MEMORY`, `HYBRID` ou `ALL_TEMP`. |
| `-restore` | Reutiliza arquivos existentes e válidos. |
| `-hash` | Valida os arquivos por hash. |
| `-size` | Valida os arquivos pelo tamanho. |
| `-source_compare <dir>` | Compara ou reutiliza uma cópia local. |
| `-help` | Exibe todos os argumentos. |

Exemplo de consulta de versão:

```bash
java -jar target/l2exlauncher-<versão>.jar \
  -cdn NC_SOFT_AMERICA \
  -last_version
```

## Configuração

As configurações principais ficam em:

```text
config/Main.ini
config/launcher-config.xml
```

O arquivo `Main.ini` define valores padrão, enquanto os argumentos da CLI têm prioridade sobre a configuração.

O estado local do launcher, catálogos e hashes pode ser armazenado em `db/`. Diretórios de saída e artefatos de build não fazem parte do código-fonte versionado.

## Fontes suportadas

| Fonte | Descrição |
|---|---|
| `NC_SOFT_JAPANESE` | Cliente Lineage II do Japão. |
| `NC_SOFT_AMERICA` | Cliente Lineage II da América do Norte. |
| `NC_SOFT_TAIWAN` | Cliente Lineage II de Taiwan. |
| `NC_SOFT_KOREAN` | Cliente Lineage II da Coreia. |
| `FOUR_GAME` | Distribuição 4game/Innova. |
| `UP_NOVA_LAUNCHER` | Servidores privados baseados em UpNova. |
| `AKUMU` | Espelho HTTP baseado em Akumu/Anubis. |

## Diagnóstico e testes

O projeto contém ferramentas auxiliares para testar geradores de manifestos, verificar URLs, consultar versões e comparar fontes. Esses arquivos são mantidos localmente para desenvolvimento e não fazem parte da distribuição principal.

As versões consultadas são versões de patch do launcher, não necessariamente números de protocolo do Lineage II.

## Estado do projeto

O launcher está em desenvolvimento ativo. As fontes de distribuição, formatos de manifesto e serviços regionais podem sofrer alterações conforme os provedores atualizam seus clientes e endpoints.

## Histórico

O desenvolvimento atual reúne a interface gráfica e o backend de download em um único projeto Java. Contribuidores e versões anteriores podem ser registrados nesta seção sem manter o histórico antigo misturado à branch principal atual.
