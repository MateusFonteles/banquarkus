# 🏦 banquarkus — Simulador Bancário para Treinamento de CyberSec e DevOps

## 📜 Unidade 1: Introdução Geral e Propósito do Laboratório

O **Banquarkus** é um ecossistema projetado do zero para servir como um **Laboratório Prático de Segurança da Informação (CyberSec) e Engenharia de Software Avançada**. Diferente de projetos tradicionais de desenvolvimento de software que focam apenas em entregar funcionalidades operacionais ("caminho feliz"), este simulador foi concebido para expor o comportamento real de uma API de serviços financeiros brasileira (padrão nativo pt-BR) frente a desafios complexos de infraestrutura, consistência de dados e vetores de ataque cibernético. 🚀

O cerne do projeto baseia-se na criação de um ambiente controlado onde falhas de programação, configurações inadequadas de rede e erros de lógica de negócios possam ser analisados, explorados e, posteriormente, corrigidos utilizando as melhores práticas da indústria. 🛡️✨

---

### 🎯 Objetivos Estratégicos do Laboratório

Este laboratório visa preencher a lacuna entre o desenvolvimento de software corporativo e a segurança cibernética ativa, operando em duas frentes fundamentais de atuação:

#### 🔴 A Visão do Red Team (Segurança Ofensiva)
Para profissionais ou estudantes que desejam compreender táticas de ataque e exploração, o simulador fornece um "campo de tiro" ideal 🎯. Ele permite estudar como brechas sutis no código-fonte ou na modelagem de banco de dados podem ser usadas para comprometer a integridade de uma corporação. Os principais focos ofensivos incluem:
* 🔓 **Exploração de Falhas de Autorização (BOLA / IDOR):** Manipulação de parâmetros para acessar dados privados de outros correntistas.
* 💉 **Ataques de Injeção NoSQL:** Forjar payloads para burlar autenticações e extrair coleções completas do MongoDB.
* 🎭 **Manipulação de Lógica de Negócios:** Explorar a falta de atomicidade transacional e falhas de sinal numérico para criar saldos falsos ou executar fraudes financeiras.
* 💥 **Ataques de Negação de Serviço (DoS) na Camada de Aplicação:** Exaurir recursos do sistema através de requisições pesadas e não paginadas.

#### 🔵 A Visão do Blue Team (Segurança Defensiva e Engenharia)
Para engenheiros de software e administradores de sistemas, o laboratório ensina como construir barreiras robustas e projetar aplicações resilientes a falhas de infraestrutura e ataques maliciosos 🛡️. Os principais focos defensivos incluem:
* 🧱 **Arquiteturas de Defesa em Profundidade:** Isolar redes e segregar bancos de dados de forma que o comprometimento da API não resulte em perda financeira.
* 🔑 **Gestão de Identidade e Acessos (IAM/RBAC):** Centralizar autenticação e aplicar o princípio do privilégio mínimo através de controle baseado em funções.
* ⚖️ **Garantias Transacionais Estritas (ACID):** Mitigar condições de corrida (Race Conditions) e garantir a integridade dos dados mesmo sob quedas catastróficas de energia do servidor.
* 🔬 **Observabilidade e Auditoria Forense:** Estruturar logs e rastreamentos para identificar anomalias comportamentais e tentativas de intrusão em tempo real.

---

### 🏛️ A Filosofia Arquitetural: "Duas Aplicações em Uma"

Para simular o cenário de uma instituição financeira real, o Banquarkus implementa a divisão lógica de seus serviços em dois escopos de negócio completamente distintos, expondo-os em rotas de API separadas:

1. 📱 **O Lado Cliente (`/api/client/*`):** Simula a porta de entrada pública do banco, representando o comportamento de um aplicativo mobile ou internet banking para o correntista final. Esta interface lida com operações cotidianas como consulta de saldos, extratos e depósitos. Do ponto de vista de segurança, é a fronteira mais exposta e o alvo prioritário para ataques automatizados e manipulação de parâmetros por usuários maliciosos.
2. 🏢 **O Lado Backoffice / Admin (`/api/admin/*`):** Simula o painel gerencial interno utilizado exclusivamente por funcionários autorizados e administradores da instituição. Possui privilégios para listagem em massa de contas, abertura de novos cadastros e auditoria interna. Na dinâmica do laboratório, este escopo é o alvo para simulações de elevação de privilégios (*Privilege Escalation*).

---

### 🛡️ Princípios de Consistência Financeira e Defesa em Profundidade

Para sustentar a fidelidade do laboratório, dois pilares técnicos imutáveis governam a construção do ecossistema:

* 🧮 **Precisão Numérica Absoluta contra Fraudes:** Toda a manipulação monetária dentro do sistema utiliza estritamente o tipo de dado `BigDecimal` do Java. O uso de tipos de ponto flutuante tradicionais (`float` ou `double`) é expressamente proibido. Essa decisão neutraliza erros clássicos de arredondamento em processadores x86/x64, impedindo que vulnerabilidades matemáticas como o *Salami Slicing* (desvio sistemático de frações de centavos acumulados) sejam exploradas.
* 🚧 **Segregação Estrita de Redes de Dados:** O ambiente operacional onde a API pública transaciona (`banquarkus_operacional`) é isolado fisicamente do Livro-Razão centralizador (`banquarkus_central`), que funciona como o cofre forte imutável dos saldos consolidados da instituição. A aplicação Java exposta para a rede pública não possui credenciais nem conectividade direta para alcançar o cofre central, minimizando o raio de explosão (*Blast Radius*) de uma eventual invasão.

---

## 🏗️ Unidade 2: Construção da Aplicação, DevOps e Resolução de Problemas

Esta unidade funciona como o diário técnico de engenharia do nosso ecossistema 📓. Aqui estão documentados os passos exatos para reproduzir a criação da infraestrutura e da aplicação, a organização lógica dos arquivos e o relatório detalhado de erros reais enfrentados durante o processo, servindo como um guia de sobrevivência para cenários de implantação em produção.

---

### 📂 2.1 Organização Estrutural do Projeto (DDD Simplificado)

Para garantir que o código seja expansível e modular, adotamos uma versão simplificada do padrão **DDD (Domain-Driven Design)**. A separação em pacotes dentro de `src/main/java/org/acme/banquarkus/` segue uma lógica estrita de responsabilidades, o que facilita a auditoria de segurança:

```text
src/main/java/org/acme/banquarkus/
│
├── 📁 domain/
│   ├── Cliente.java         <- Entidade de identidade e privilégios (RBAC)
│   ├── ContaBancaria.java   <- Entidade financeira com saldos operacionais
│   └── Transacao.java       <- Registro imutável de movimentações (Ledger)
│
├── 📁 service/
│   ├── ContaService.java     <- Centraliza lógicas de busca e abertura de contas
│   └── TransacaoService.java <- Cérebro financeiro (validações e fluxo de caixa)
│
└── 📁 resource/
    ├── 📁 admin/
    │   └── AdminContaResource.java <- Endpoints restritos de retaguarda (Backoffice)
    └── 📁 client/
        └── ClientContaResource.java <- Endpoints públicos do aplicativo do cliente
```

---

### 💻 2.2 Inicialização do Projeto Quarkus

O projeto foi gerado utilizando a interface de linha de comando (CLI) do Quarkus diretamente no terminal do Ubuntu (WSL2), trazendo apenas os componentes estritamente necessários para manter a superfície de ataque reduzida:

```bash
quarkus create app org.acme:banquarkus \
    --extension='rest-jackson,mongodb-panache' \
    --no-code
```

* 🔄 **`rest-jackson`**: Atua como o tradutor universal da API, convertendo automaticamente as strings JSON trafegadas na rede em objetos Java.
* 🗄️ **`mongodb-panache`**: Injeta o padrão *Active Record*, permitindo que as entidades executem comandos de persistência diretos (como `.persist()` e `.update()`) sem a necessidade de repositórios complexos.
* 🧹 **`--no-code`**: Garante um projeto limpo, impedindo a geração de classes de exemplo que poluam a arquitetura.

#### 📝 Mapeamento de Funções e Isolação de Responsabilidades
Para garantir a reprodutibilidade e a manutenibilidade do laboratório, evitamos concentrar códigos em arquivos únicos, dividindo o ecossistema de forma lógica. Cada classe possui anotações educativas detalhadas sobre boas práticas de CyberSec 🎓:

* 👤 **`org.acme.banquarkus.domain.Cliente`**: Gerencia dados de identidade e controle de privilégios (`role`). É a base para as futuras defesas de RBAC e armazenamento seguro de credenciais.
* 💰 **`org.acme.banquarkus.domain.ContaBancaria`**: Modela os dados financeiros das contas correntes. É nela que fica fixado o uso mandatório do tipo `BigDecimal saldoAtual` contra fraudes de arredondamento (*Salami Slicing*).
* 📜 **`org.acme.banquarkus.domain.Transacao`**: Modela o livro-razão (*Ledger*). Funciona em regime *Append-Only* (apenas inserções são permitidas) e registra dados periciais críticos como o `ipOrigem` capturado na requisição HTTP.
* ⚙️ **`org.acme.banquarkus.service.ContaService`**: Concentra as lógicas administrativas e de busca direcionada de contas operacionais.
* 🧠 **`org.acme.banquarkus.service.TransacaoService`**: O cérebro das operações financeiras. É onde validamos de forma estrita se o input do depósito é positivo (`valor.compareTo(BigDecimal.ZERO) <= 0`), bloqueando injeções de lógica de negócios (como tentar enviar valores negativos para transformar depósitos em saques).
* 🏢 **`org.acme.banquarkus.resource.admin.AdminContaResource`**: Expõe endpoints restritos de retaguarda (Backoffice) para criação e listagem total de contas por usuários gerenciais.
* 📱 **`org.acme.banquarkus.resource.client.ClientContaResource`**: Porta de entrada do aplicativo do cliente final, expondo as rotas de depósito e consulta parametrizada que usaremos para testar nossas vulnerabilidades lógicas.

Este mapa estrutural garante que o desenvolvedor e o auditor saibam exatamente onde isolar as regras de negócio das portas de entrada HTTP, servindo como um complemento prático aos comentários educativos inseridos diretamente nos arquivos-fonte.

---

#### 🗂️ A Anatomia Funcional do Ecossistema
Para que a orquestração entre o Linux (WSL2), o Docker e o Java funcione perfeitamente, o laboratório conta com um conjunto de arquivos de configuração que atuam como a espinha dorsal do projeto. Entender a responsabilidade de cada um é vital 💡:

* 📦 **`pom.xml` (O Coração do Maven):** É o manifesto da aplicação Java. Funciona como a "lista de compras" e a planta baixa do projeto. Nele declaramos a versão do Java (21), a versão do Quarkus e todas as extensões necessárias (como Panache, Jackson e OpenAPI). Quando o Docker ou o terminal executam o comando `mvn`, é este arquivo que dita de onde baixar as bibliotecas e como compilar o código fonte.
* 🗺️ **`docker-compose.yml` (O Mapa da Infraestrutura):** É o maestro da nossa rede virtual. Em vez de subir contêiner por contêiner manualmente no terminal decorando dezenas de parâmetros de rede, este arquivo mapeia toda a topologia do sistema. Ele define quais imagens baixar (`mongo:7.0`), como os três bancos se conectam na rede fechada `banco-network`, como as portas são expostas para o seu Windows e quais pastas do seu HD (volumes) guardarão os dados físicos.
* 🐳 **`Dockerfile.dev` (A Receita do Contêiner):** Como mencionado, é o passo a passo para criar o "computador virtual" onde o Quarkus vai morar. Ele foi configurado estrategicamente para acelerar o processo de inicialização do ambiente de desenvolvimento, isolando o download de dependências pesadas do Maven na camada de cache do Docker, garantindo que recompilações futuras do código sejam quase instantâneas.
* 🤖 **`run.sh` (O Orquestrador DevOps):** É o nosso script de automação e segurança. Enquanto o `docker-compose` apenas "liga as máquinas virtuais", o `run.sh` lida com a inteligência de negócios. É ele quem garante a **idempotência** do laboratório: derruba estados antigos sem apagar seus dados financeiros do HD, injeta o comando de eleição do cluster, espera os bancos estarem saudáveis e aplica as regras rígidas de segurança (RBAC), dispensando a execução de comandos manuais no terminal.

---

### 🗄️ 2.3 Orquestração Unificada da Infraestrutura (Docker Compose)

Para simular o ambiente de alta disponibilidade do banco operacional e testar cenários de resiliência e tolerância a falhas, configuramos três nós de MongoDB interligados em uma rede isolada. 🗺️

O arquivo `docker-compose.yml` abaixo centraliza o cluster e a API, permitindo o espelhamento de volumes para persistência estável no HD do WSL2 e ativando o recurso de *Live Reload* (o contêiner Quarkus recompila o código instantaneamente a cada salvamento realizado no VS Code):

```yaml
version: '3.8'

services:
  mongo1:
    image: mongo:7.0
    container_name: mongo1
    command: ["--replSet", "banco-rs", "--bind_ip_all"]
    ports:
      - "127.0.0.1:27017:27017"
    volumes:
      - mongo1_data:/data/db
    networks:
      - banco-network

  mongo2:
    image: mongo:7.0
    container_name: mongo2
    command: ["--replSet", "banco-rs", "--bind_ip_all"]
    ports:
      - "127.0.0.1:27018:27017"
    volumes:
      - mongo2_data:/data/db
    networks:
      - banco-network

  mongo3:
    image: mongo:7.0
    container_name: mongo3
    command: ["--replSet", "banco-rs", "--bind_ip_all"]
    ports:
      - "127.0.0.1:27019:27017"
    volumes:
      - mongo3_data:/data/db
    networks:
      - banco-network

  quarkus-app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    container_name: banquarkus-api
    ports:
      - "8080:8080"
      - "5005:5005"
    volumes:
      - ./src:/workspace/app/src
      - ./pom.xml:/workspace/app/pom.xml
    depends_on:
      - mongo1
      - mongo2
      - mongo3
    networks:
      - banco-network

networks:
  banco-network:
    driver: bridge

volumes:
  mongo1_data:
  mongo2_data:
  mongo3_data:
```

E este é o nosso **`Dockerfile.dev`**, configurado estrategicamente para acelerar o processo de inicialização do ambiente de desenvolvimento, isolando o download de dependências pesadas do Maven na camada de cache do Docker:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21
WORKDIR /workspace/app
COPY pom.xml .
# Baixa as dependências antes de copiar o código para aproveitar o cache do Docker
RUN mvn dependency:go-offline
COPY src src
EXPOSE 8080 5005
CMD ["mvn", "quarkus:dev", "-Dquarkus.http.host=0.0.0.0"]
```

---

### 🐉 2.3.1 A Jornada da Alta Disponibilidade: O "Mongo de 3 Cabeças"

Em um cenário corporativo crítico (como o setor financeiro), um servidor de banco de dados isolado (*Standalone*) representa um Ponto Único de Falha (*Single Point of Failure - SPOF*). Se o servidor queimar, o banco para e o dinheiro some 💸. Para o nosso laboratório, tomamos a decisão arquitetural de simular uma topologia corporativa real criando um **Replica Set** do MongoDB com três nós (o nosso "Mongo de 3 cabeças" 🐲).

#### 🛠️ A Engenharia do `docker-compose.yml`
A criação deste cluster exigiu configurações estritas de rede no Docker Compose:
1. **Instanciamento Triplo:** Criamos três serviços irmãos (`mongo1`, `mongo2`, `mongo3`), todos anexados à mesma ponte de rede (`banco-network`).
2. **A Flag `--replSet`:** Injetada no comando de inicialização de cada contêiner, esta instrução avisa ao MongoDB: *"Você não está sozinho. Você faz parte do cluster chamado 'banco-rs'"*.
3. **A Flag `--bind_ip_all`:** Crucial para ambientes de contêineres. Sem ela, por medidas de segurança paranoicas do MongoDB, o banco escutaria apenas o localhost interno de si mesmo, tornando as três cabeças cegas umas para as outras.

#### 📈 A Evolução do Script `run.sh`
Ter os três contêineres rodando não significava ter um banco funcional. No MongoDB, o cluster só começa a receber leituras e escritas após ocorrer uma eleição para definir quem é o Líder (`PRIMARY`) e quem são as réplicas de segurança (`SECONDARY`). 

Nossa jornada com a automação passou por três estágios de maturidade 🚀:
* 🚶‍♂️ **Estágio 1 (Manual):** Subíamos o `docker-compose up -d` e entrávamos manualmente no contêiner para digitar o comando `rs.initiate()`. Processo lento, sujeito a erros de digitação e impossível de ser incorporado em uma esteira de CI/CD.
* 🏃‍♂️ **Estágio 2 (A Condição de Corrida):** Criamos a primeira versão do nosso script Bash. Ele aplicava o `rs.initiate()` e aguardava estáticos 5 segundos (`sleep 5`) antes de tentar criar os usuários de segurança do banco. Isso quebrou o laboratório inúmeras vezes. O tempo de eleição entre as três máquinas oscilava, e o script frequentemente tentava gravar credenciais no `mongo1` enquanto ele ainda era apenas uma réplica de leitura, gerando o erro de *Race Condition* `MongoServerError: not primary`.
* 🏎️ **Estágio 3 (A Automação DevOps Definitiva):** Reescrevemos o `run.sh` com inteligência cibernética. Ele agora aplica o comando de eleição, mas entra em um laço de repetição dinâmico, interrogando os três nós a cada segundo com a query `db.hello().isWritablePrimary`. Somente quando uma das "três cabeças" levanta a mão e se confirma como o Líder definitivo é que o script avança para injetar as regras de segurança. Adicionalmente, removemos a flag destrutiva `-v` (que apagava volumes) e adicionamos checagens JavaScript para só criar usuários se eles não existirem, tornando o laboratório 100% idempotente e a prova de reinícios.

---

### 🛠️ 2.4 Relatório Sintético de Engenharia: Erros, Motivos e Soluções

Durante a construção do laboratório, nos deparamos com desafios complexos de redes virtuais, sincronia de estados em sistemas distribuídos e conversões de dados. Abaixo está o registro dessas ocorrências para fins de documentação técnica e facilidade de reprodução 📑:

#### 🚨 1. O Loop Infinito de Queda dos Contêineres do MongoDB
* 🩺 **Sintoma:** Os contêineres do banco iniciavam, mas sofriam desligamento limpo (`exitCode: 0`) de forma cíclica e contínua, entrando em um loop eterno de crash.
* 🔍 **Motivo:** Ao declarar a flag `--replSet`, o MongoDB entra em modo de cluster distribuído e exige uma inicialização interna imediata (`rs.initiate()`). Como os nós nasciam isolados e sem configuração prévia de liderança, o processo encerrava por segurança. Além disso, a falta do parâmetro `--bind_ip_all` impedia que as réplicas se enxergassem fora do localhost do próprio contêiner.
* 💊 **Solução:** Ajustamos o script de automação (`run.sh`) para subir as instâncias em segundo plano (`-d`) e, logo em seguida, injetar o comando de consenso via `mongosh` diretamente na rede virtual.

#### 🚨 2. Falhas Aleatórias de Escrita (`MongoServerError: not primary`)
* 🩺 **Sintoma:** O script de automação falhava de forma intermitente e imprevisível ao tentar criar os usuários administrativos e operacionais do banco.
* 🔍 **Motivo (Race Condition):** O script original utilizava uma pausa estática (`sleep 5`) após ligar o Replica Set. No entanto, o tempo de votação interna entre os nós oscilava. Se o script tentasse gravar privilégios de segurança antes de a eleição acabar, a requisição batia em um nó que ainda operava em estado `SECONDARY` (Apenas Leitura), disparando o erro de negação de escrita.
* 💊 **Solução:** Desenvolvemos um algoritmo dinâmico de varredura no `run.sh` que interroga individualmente os nós da lista em um laço `for` de até 15 segundos, utilizando a propriedade `db.hello().isWritablePrimary`. O script monitora o cluster em tempo real, descobre quem é o líder de escrita legítimo e direciona os comandos de RBAC estritamente a ele.

#### 🚨 3. Erros de Resolução de Nomes e Queda de Failover (`UnknownHostException`)
* 🩺 **Sintoma:** A aplicação Java funcionava perfeitamente em cenários normais, mas perdia completamente a conectividade com o banco de dados se o nó principal caísse, inviabilizando os testes de resiliência.
* 🔍 **Motivo:** Com o Quarkus rodando solto no ambiente local do WSL2, ele usava a configuração de porta mapeada no `application.properties` apontando para o endereço local (`127.0.0.1:27017`). Porém, no momento do *failover* (queda do líder), o driver oficial interrogava o Replica Set e descobria os nomes internos dos nós (`mongo2:27017`). O sistema Linux do WSL2 não conseguia resolver o nome literal do contêiner Docker, gerando erros de timeout e conexões recusadas.
* 💊 **Solução:** **Conteinerização completa da aplicação.** Ao encapsular a API dentro da mesma rede virtual (`banco-network`) por meio do `Dockerfile.dev`, o Quarkus passou a fazer uso do DNS interno nativo do Docker. As propriedades de conexão no `application.properties` foram ajustadas e mantidas para herdar a resiliência e a segurança corporativa correta:
    ```properties
    quarkus.mongodb.write-concern.w=majority
    quarkus.mongodb.read-preference=primary
    ```
    *Nota didática de CyberSec:* Estas linhas deixaram de ser apenas um ajuste de rede de desenvolvimento e passaram a garantir a consistência forte do sistema financeiro. As operações só retornam sucesso se gravadas na maioria dos nós do cluster (evitando perda de saldo em quedas de energia) e as consultas leem estritamente do nó líder ativo, blindando o sistema contra saldos desatualizados induzidos por latência de replicação.

#### 🚨 4. O Paradoxo do Cache e o Erro 404 no Swagger UI
* 🩺 **Sintoma:** A dependência `quarkus-smallrye-openapi` era adicionada ao projeto, mas o endereço `/q/swagger-ui/` retornava erro de página não encontrada (404).
* 🔍 **Motivo:** O volume configurado no Docker Compose estava espelhando ativamente apenas o diretório de código-fonte (`src/`). Alterações estruturais que modificam a fundação da aplicação (como adições de bibliotecas no `pom.xml`) ocorrem na raiz e não sensibilizam o ambiente que já foi compilado em cache na memória do contêiner. Além disso, a dependência havia sido inserida incorretamente no bloco `<dependencyManagement>` (que serve apenas como catálogo de versões das dependências do ecossistema Quarkus) em vez do bloco `<dependencies>` (que realiza a importação real dos arquivos executáveis).
* 💊 **Solução:** Corrigimos o posicionamento da tag XML dentro do `pom.xml` para garantir sua importação ativa e acionamos o comando de reconstrução forçada da imagem para quebrar o cache do contêiner antes de reexecutar o orquestrador:
    ```bash
    docker compose down && docker compose build --no-cache quarkus-app && ./run.sh
    ```

---

## 🔍 Unidade 3: Auditoria de Segurança, Roadmap e Laboratório Hacker

Com a infraestrutura do simulador rodando de forma estável, o foco do projeto se volta inteiramente para a exploração e mitigação de falhas cibernéticas 🕵️‍♂️. O ambiente atual foi deixado intencionalmente vulnerável em pontos arquiteturais chave para servir de objeto de estudo.

### 🚨 3.1 Relatório de Auditoria: Vulnerabilidades Atuais (By Design)

Uma varredura estrita nas classes Java do nosso MVP revela quatro fragilidades críticas comuns em APIs corporativas do mundo real ⚠️:

1. 🔓 **Mass Assignment e Vazamento de Abstração:** Nossos endpoints REST expõem as entidades diretas do banco de dados (ex: `ContaBancaria`) para a internet. Isso não só revela detalhes da nossa tecnologia subjacente (como a exigência de um `ObjectId` do MongoDB), como permite que um atacante injete campos que não deveriam ser modificados pelo usuário.
2. 🏎️ **Insegurança Transacional (Race Conditions):** Na classe `TransacaoService`, as lógicas de débito/crédito e a persistência do recibo ocorrem em passos separados. Sem garantia de atomicidade (ACID), uma queda de servidor entre esses milissegundos gera uma inconsistência financeira: o saldo é alterado, mas o registro de auditoria não é gravado.
3. 🛑 **Negação de Serviço (DoS) na Camada de Aplicação:** O endpoint administrativo de listagem de contas (`listAll()`) busca toda a coleção de uma só vez no banco de dados. Em um cenário simulado com milhões de contas, isso causará um esgotamento de memória (*Out of Memory*) e derrubará a API instantaneamente.
4. 🕳️ **Information Leakage (Vazamento de Informações):** A ausência de um tratamento global de exceções faz com que erros internos do Quarkus e falhas do MongoDB devolvam *stacktraces* completas (rastros de código) no payload HTTP, mapeando toda a nossa tecnologia para o atacante.

---

### 🗺️ 3.2 Roadmap: O Futuro do Simulador (Escopo de Médio Prazo)

Para fechar essas vulnerabilidades e elevar o laboratório a um padrão de missão crítica, a evolução do projeto está dividida nas seguintes fases estruturais de defesa (Blue Team) 🛡️:

* **Fase 1: Identidade, Proteção e IAM (Gestão de Acessos)** 🛂
    * Subida de um contêiner **Keycloak (OIDC)** via Docker para emissão de Tokens JWT.
    * Substituição do acesso público por rotas protegidas por anotações de RBAC (`@RolesAllowed`).
    * Implementação do **Padrão DTO** para blindar as entidades do banco contra Mass Assignment e esconder campos sensíveis.
* **Fase 2: Robustez Financeira e Defesa Ativa** 🏦
    * Adição de blocos atômicos (`@Transactional`) para garantir a consistência das operações financeiras.
    * Criação de um `ExceptionMapper` global para higienizar e padronizar todas as respostas de erro HTTP.
    * Construção de um serviço autônomo (Scheduler) no Quarkus que lida com a reconciliação assíncrona entre o banco operacional e o cofre central.
* **Fase 3: Observabilidade e Mensageria (Nível Avançado)** 📊
    * Adoção do **Apache Kafka** para criar uma esteira antifraude de auditoria de eventos.
    * Inserção do **PostgreSQL** para separar o processamento ACID de auditorias não estruturadas do MongoDB.
    * Integração de **OpenTelemetry**, **Prometheus** e **Grafana** para detectar visualmente picos de brute-force ou ataques volumétricos (como NoSQL Injection).

---

### 🏴‍☠️ 3.3 Laboratório Hacker: Práticas Ofensivas (Red Team)

Para aprender a defender, é preciso aprender a atacar ⚔️. A metodologia do nosso laboratório dispensa o uso de scripts complexos de automação no terminal neste primeiro momento, focando na lógica de exploração utilizando as ferramentas que já temos à disposição.

**A Metodologia de Interceptação:**
Enxergaremos o ambiente através de duas lentes 🔎:
* 🌐 **O Swagger UI** (`http://localhost:8080/q/swagger-ui/`): É a nossa "vitrine". Simula a interface gráfica de um aplicativo bancário convencional.
* 🎧 **As DevTools do Navegador** (Aba *Network*): É a nossa "escuta telefônica". Através do filtro *Fetch/XHR*, capturaremos os pacotes de dados invisíveis que transitam entre a vitrine e o Quarkus, inspecionando Cabeçalhos, Payloads e Códigos de Resposta.

#### ⚔️ Exercício Prático: Exploração de Falha de Autorização (BOLA / IDOR)

A falha BOLA (*Broken Object Level Authorization*) ocorre quando um sistema acessa um dado verificando apenas se ele existe, mas falha em checar se quem está pedindo tem autorização para visualizá-lo. Vamos explorar essa brecha na nossa rota de clientes 🕵️:

1. 🎭 **Reconhecimento (Criando o Cenário):** No Swagger, vá até a rota irrestrita administrativa (`POST /api/admin/contas`) e crie duas contas distintas enviando um JSON limpo (sem os IDs do MongoDB).
   * Crie a **Sua Conta:** Agência `0001`, Conta `12345-6`.
   * Crie a **Conta do Alvo:** Agência `0001`, Conta `99999-9`.
2. 🚶‍♂️ **Tráfego Legítimo:** Abra as DevTools (`F12`) na aba *Network*. No Swagger, vá até a rota de cliente (`GET /api/client/minha-conta/{agencia}/{numero}`) e faça uma consulta pelo saldo da **sua** conta (`12345-6`).
3. 🎣 **A Interceptação:** Clique na requisição capturada na aba *Network* das DevTools. Inspecione a aba *Headers* (para entender a URL chamada) e a aba *Response* (para ver o seu saldo retornado em JSON).
4. 🥷 **A Forja e o Ataque:** Clique com o botão direito nesta requisição na aba *Network* e selecione **Copy > Copy as cURL (bash)**.
   * Abra o seu terminal Linux (WSL2).
   * Cole o comando copiado.
   * Navegue na linha de texto e altere manualmente o final da URL (os parâmetros de consulta), substituindo a sua conta (`12345-6`) pela conta do alvo (`99999-9`).
5. 💥 **O Vazamento:** Pressione Enter. A API do Quarkus não fará o cruzamento de identidade e devolverá o saldo da conta de terceiros diretamente no seu terminal, confirmando a vulnerabilidade crítica de autorização no sistema.

*Este exercício encerra a configuração base do nosso laboratório. As próximas interações envolverão a refatoração do código-fonte para mitigar esta e outras falhas catalogadas.* ✅
