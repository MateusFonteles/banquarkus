# 🏦 banquarkus — Simulador Bancário para Treinamento de CyberSec

Este projeto é um laboratório prático de segurança cibernética e DevOps que simula o funcionamento de uma API bancária nacional (padrão pt-BR). O objetivo é explorar resiliência de infraestrutura, controle de acessos (RBAC), arquiteturas de defesa em profundidade e mitigação de vulnerabilidades (como NoSQL Injection).

---

## 💻 Arquitetura do Ambiente de Desenvolvimento

O laboratório foi estruturado em um modelo híbrido para simular com precisão o comportamento de servidores de produção modernos:

*   **Sistema Operacional Hospedeiro:** Windows 11 executando a IDE **VS Code**.
*   **Ambiente de Execução Principal:** **WSL2 (Windows Subsystem for Linux)** rodando uma instância do **Ubuntu**.
*   **Engine de Infraestrutura:** **Docker Engine** instalado de forma nativa e direta dentro do Ubuntu (WSL2), garantindo máxima performance de rede e isolamento de contêineres, sem dependência do Docker Desktop do Windows.
*   **Framework da Aplicação:** **Quarkus (versão 3.x)** rodando no ambiente de desenvolvimento do Linux, comunicando-se diretamente com o cluster Docker local.

---

## 🛠️ Passo 1: Inicialização do Projeto Quarkus

A estrutura base do sistema foi gerada utilizando a interface de linha de comando (CLI) do Quarkus diretamente no terminal do Ubuntu (WSL2). O projeto foi configurado com extensões nativas para manipulação de JSON e persistência simplificada em bancos não-relacionais.

### Comando de Criação:
```bash
quarkus create app org.acme:banquarkus \
    --extension='rest-jackson,mongodb-panache' \
    --no-code
```

### 🧠 O que esse comando faz?
*   `org.acme:banquarkus`: Define o grupo e o nome do artefato do projeto.
*   `rest-jackson`: Adiciona o suporte a rotas HTTP REST com serialização automática de objetos Java para o formato JSON.
*   `mongodb-panache`: Ativa o padrão *Active Record* do Quarkus Panache para o MongoDB, permitindo interagir com coleções do banco usando métodos simples em português diretamente nas entidades Java (ex: `.persist()`, `.listAll()`).
*   `--no-code`: Gera um projeto limpo, sem classes ou recursos de exemplo gerados automaticamente, permitindo construir a arquitetura do zero.

---

## 🗄️ Passo 2: Infraestrutura de Banco de Dados de Alta Disponibilidade

Para simular um ambiente bancário real e testar a resiliência contra ataques de Negação de Serviço (DoS), estruturamos um cluster MongoDB com três réplicas utilizando o mecanismo de **Replica Set**. 

Abaixo está o arquivo base `docker-compose.yml` utilizado para erguer os três nós na rede isolada `banco-network`:

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

networks:
  banco-network:
    driver: bridge

volumes:
  mongo1_data:
  mongo2_data:
  mongo3_data:
```

---

## ⚠️ Lições Aprendidas: O Loop Infinito de Desligamento e Erros de Flags

Durante as primeiras tentativas de subir o ambiente via `docker compose up`, deparamos com erros críticos de inicialização que faziam os contêineres entrarem em um loop eterno de crash e reinicialização.

### 🔍 Sintoma do Problema (Logs do Terminal):
```text
mongo1  | {"t":{"$date":"..."},"s":"I",  "c":"STORAGE",  "id":22279,   "ctx":"SignalHandler","msg":"shutdown: removing fs lock..."}
mongo1  | {"t":{"$date":"..."},"s":"I",  "c":"CONTROL",  "id":8423404, "ctx":"SignalHandler","msg":"mongod shutdown complete","attr":{"exitCode":0}}
```

### 🧠 Causa Raiz do Erro:
1. **Falta da flag de escopo de rede:** Inicialmente, o comando não possuía o parâmetro `--bind_ip_all`. Sem ele, o MongoDB se prendia apenas ao localhost interno do contêiner, impedindo que os nós se enxergassem e que o ambiente WSL2 fizesse a ponte de portas externa.
2. **A Pegadinha do Replica Set:** Ao passar a flag `command: ["--replSet", "banco-rs"]`, informamos ao MongoDB que ele faz parte de um cluster dinâmico. O motor do MongoDB inicia, mas exige **imediatamente** que um comando interno de inicialização (`rs.initiate()`) seja executado para decidir quem será o líder. Como o comando não encontrava uma configuração válida ativa, o processo sofria um desligamento limpo (`exitCode: 0`) e o Docker forçava o contêiner a subir de novo, gerando o loop infinito.

### 🛠️ Solução que Funcionou:
Para quebrar o loop, os contêineres precisavam ser iniciados em segundo plano (`-d`) e, logo em seguida, o comando de inicialização do cluster precisava ser injetado via terminal direto no contêiner principal:

```bash
# Iniciar sem travar o terminal
docker compose up -d

# Injetar a configuração de consenso no nó principal
docker exec -it mongo1 mongosh --eval '
rs.initiate({
  _id: "banco-rs",
  members: [
    { _id: 0, host: "mongo1:27017" },
    { _id: 1, host: "mongo2:27017" },
    { _id: 2, host: "mongo3:27017" }
  ]
})
'
```
*Nota: Usar nomes de hosts internos do Docker (`mongo1:27017`) dentro da configuração do cluster garante que as réplicas se comuniquem pela rede virtual isolada, evitando problemas com IPs dinâmicos.*

---

## 🚀 Passo 3: Automação DevOps com Reset Total (`up-db.sh`)

Para garantir a **idempotência** do laboratório — ou seja, a capacidade de destruir e reconstruir o ambiente exatamente com o mesmo comportamento, independentemente do estado anterior da máquina —, desenvolvemos um script em Bash chamado `up-db.sh`.

Adotamos a estratégia de **Reset Total**. O script limpa o histórico, sobe os contêineres e aguarda dinamicamente a eleição do banco antes de injetar as configurações de segurança. Isso elimina falhas humanas de digitação de comandos no terminal.

### Arquivo `up-db.sh`:
```bash
#!/bin/bash
set -e

echo "🛑 0. Reset Total: Apagando contêineres e volumes antigos..."
docker compose down -v

echo "🚀 1. Iniciando instâncias limpas do MongoDB no Docker..."
docker compose up -d

echo "⏳ 2. Aguardando 6 segundos para inicialização dos sistemas..."
sleep 6

echo "🔄 3. Configurando o Replica Set (banco-rs)..."
docker exec -i mongo1 mongosh --eval '
  rs.initiate({
    _id: "banco-rs",
    members: [
      { _id: 0, host: "mongo1:27017" },
      { _id: 1, host: "mongo2:27017" },
      { _id: 2, host: "mongo3:27017" }
    ]
  });
'

echo "⏳ 4. Aguardando a eleição do líder (PRIMARY) se consolidar..."
MENSAGEM_LIDER=""
for i in {1..15}; do
  MENSAGEM_LIDER=$(docker exec -i mongo1 mongosh --quiet --eval "db.hello().isWritablePrimary" | tr -d '\r\n' || echo "false")
  if [ "$MENSAGEM_LIDER" = "true" ]; then
    echo "👑 Líder (PRIMARY) estabelecido e pronto para receber escritas!"
    break
  fi
  echo "⏳ Aguardando eleição terminar... (${i}s)"
  sleep 1
done

echo "🔒 5. Criando usuários e bases lógicas com privilégios restritos (RBAC)..."
docker exec -i mongo1 mongosh --eval '
  // 1. Criar Administrador de Infraestrutura
  var bancoAdmin = db.getSiblingDB("admin");
  bancoAdmin.createUser({
    user: "admin_geral",
    pwd: "senha123",
    roles: [ { role: "userAdminAnyDatabase", db: "admin" }, { role: "readWriteAnyDatabase", db: "admin" } ]
  });

  // 2. Criar Usuário Restrito do Quarkus (RBAC)
  var dbOperacional = db.getSiblingDB("banquarkus_operacional");
  dbOperacional.createUser({
    user: "app_banquarkus_user",
    pwd: "senha123",
    roles: [ { role: "readWrite", db: "banquarkus_operacional" } ]
  });

  // 3. Alimentar o Banco Central Protegido com Conta Base
  var dbCentral = db.getSiblingDB("banquarkus_central");
  dbCentral.contas_consolidadas.insertOne({
    titular: "Mateus Alvo",
    numero_conta: "10023-4",
    saldo_real: 5000.00,
    nivel_seguranca: "Main"
  });
  print("💰 Banco de dados populado com sucesso.");
'

echo "🚀 Ambiente limpo, seguro e pronto para o Quarkus!"
```

---

## ⚠️ Lições Aprendidas: Sincronização de Estados e Erros de Escrita

Durante o desenvolvimento deste script, enfrentamos falhas críticas que moldaram a sua versão final:

### 🔍 Sintoma do Problema 1: `MongoServerError: not primary`
*   **Causa Raiz:** O script original usava um tempo fixo de espera (`sleep 5`) após iniciar o Replica Set. No entanto, o processo de votação interna dos contêineres limpos no WSL2 oscilava de tempo. Se o script tentasse criar os usuários enquanto o `mongo1` ainda estivesse como `SECONDARY`, o comando quebrava, pois nós secundários bloqueiam operações de escrita.
*   **Solução:** Implementamos um laço de repetição (`for` inteligente de 15 segundos) que executa a função `db.hello().isWritablePrimary`. O script agora só avança para a criação de usuários no momento exato em que o banco confirma que se tornou o líder ativo della rede.

### 🔍 Sintoma do Problema 2: Permissão Negada e Arquivos não Encontrados
*   **Causa Raiz:** Bloqueios de segurança nativos do Linux impediam a execução direta do script pelo terminal, e incompatibilidades de caminhos no VS Code tentavam abrir arquivos novos em branco.
*   **Solução:** Forçamos explicitamente a permissão de execução no terminal via comando `chmod +x up-db.sh`, garantindo que o arquivo correto mapeado no diretório do WSL2 fosse salvo e lido de forma integrada com a IDE do Windows.

---

## 🔍 Passo 3.5: Validação Intermediária da Infraestrutura (Testes sem Java)

Antes de escrever qualquer linha de código no Quarkus, validamos a saúde do cluster, a replicação de dados e as permissões de segurança diretamente no terminal do WSL2 utilizando comandos nativos do MongoDB (`mongosh`).

### 1. Testar o Status do Cluster e Eleição de Líder
Para verificar se os contêineres estão se comunicando e se o processo de votação elegeu um líder (`PRIMARY`), execute:
```bash
docker exec -it mongo1 mongosh --eval "rs.status()"
```
*   **O que observar:** Verifique o objeto `members`. Um dos nós deve exibir obrigatoriamente `stateStr: "PRIMARY"` e os outros dois devem exibir `stateStr: "SECONDARY"`.

### 2. Testar Gravação e Replicação em Tempo Real
Para garantir que um dado bancário inserido no servidor principal é copiado instantaneamente para as réplicas de segurança, simulamos uma inserção manual.

**Etapa A: Gravar no nó líder (PRIMARY)**
```bash
docker exec -it mongo1 mongosh --eval '
  var meuBanco = db.getSiblingDB("banquarkus_operacional");
  meuBanco.testes_manuais.insertOne({
    titular: "Mateus Alvo",
    saldo: 5000.00
  });
'
```

**Etapa B: Ler no nó de réplica (SECONDARY)**
```bash
docker exec -it mongo2 mongosh --eval '
  db.getMongo().setReadPref("secondary");
  var meuBanco = db.getSiblingDB("banquarkus_operacional");
  meuBanco.testes_manuais.find();
'
```
*   **O que observar:** O terminal deve retornar o JSON com o registro do "Mateus Alvo" e o `ObjectId` gerado, provando que a replicação automática em segundo plano via Docker está funcionando.

### 3. Testar a Persistência de Dados (O Cofre do HD)
Para validar que os volumes do Docker estão segurando as informações no disco rígido do WSL2 e que os dados não somem ao desligar os servidores, execute:
```bash
# Para os contêineres sem apagar os volumes
docker compose stop

# Inicia os contêineres novamente
docker compose start

# Consulta os dados de forma autenticada com o usuário admin criado pelo script
docker exec -it mongo1 mongosh -u admin_geral -p senha123 --authenticationDatabase admin --eval '
  var meuBanco = db.getSiblingDB("banquarkus_operacional");
  meuBanco.testes_manuais.find();
'
```
*   **O que observar:** Os dados inseridos antes do desligamento devem ser lidos na tela normalmente, confirmando que a persistência de volumes está ativa.

---

## 🔒 Passo 4: Configuração de Segurança e Conectividade do Quarkus

Para integrar a nossa aplicação Java à nossa infraestrutura de banco de dados, configuramos o arquivo `src/main/resources/application.properties`. 

Esta configuração força o Quarkus a interagir **estritamente com o banco operacional**, aplicando o conceito de **Defesa em Profundidade**: a aplicação consome credenciais de privilégio mínimo e não possui permissão de rede ou autenticação para acessar a base consolidada (`banquarkus_central`), protegendo o cofre do banco contra ataques vindos da aplicação.

### Arquivo `application.properties`:
```properties
# Nome do simulador bancário
quarkus.application.name=banquarkus

# String de conexão resiliente apontando para as três réplicas do banco operacional
quarkus.mongodb.connection-string=mongodb://app_banquarkus_user:senha123@127.0.0.1:27017,127.0.0.1:27018,127.0.0.1:27019/banquarkus_operacional?replicaSet=banco-rs&authSource=banquarkus_operacional&loadBalanced=false

# Força o Quarkus a ignorar o roteamento interno do Docker e se prender aos mapeamentos de portas locais
quarkus.mongodb.write-concern.w=majority
quarkus.mongodb.read-preference=primary
```

---

## ⚠️ Lições Aprendidas: Resolução de Nomes de Rede e Falha de Resiliência Local

Durante a homologação da conectividade entre a aplicação (rodando no WSL2) e o banco de dados (rodando nos contêineres Docker), enfrentamos um erro de infraestrutura que impactou os testes de queda (*failover*).

### 🔍 Sintoma do Problema: `com.mongodb.MongoTimeoutException` e `UnknownHostException`
```text
Exception in monitor thread while connecting to server mongo2:27017: com.mongodb.MongoSocketException: mongo2
Caused by: java.net.UnknownHostException: mongo2: Name or service not known
caused by {java.net.ConnectException: Connection refused}}
```

### 🧠 Causa Raiz:
1. **O Aperto de Mão (Handshake) Rígido:** Ao iniciar, o Quarkus se conecta com sucesso ao `127.0.0.1:27017`. Contudo, o driver oficial do MongoDB interroga o Replica Set e descobre que os membros do cluster se chamam internamente `mongo1:27017`, `mongo2:27017` e `mongo3:27017`.
2. **A Perda de Rota no Failover:** O driver Java passa a ignorar os IPs informados na propriedade e tenta abrir conexões diretas usando os nomes literais (`mongoX`). Como o Quarkus está fora do Docker (no ecossistema do WSL2), o Linux não consegue converter os nomes `mongoX` em IPs locais válidos.
3. **O Conflito de Portas:** Mesmo mapeando os nomes para `127.0.0.1` via arquivo de hosts, os nós secundários escutam nas portas externas `27018` e `27019` da máquina hospedeira. Quando o líder sofre o ataque DoS e cai, o driver tenta acessar o novo líder (`mongo2`) na porta `27017` (que pertencia ao contêiner derrubado), resultando em conexões recusadas e timeout da aplicação.

### 🛠️ Próxima Resolução Estratégica (Roteiro):
Para solucionar esse comportamento sem a necessidade de alterar arquivos de configuração de hosts (`/etc/hosts`) do sistema operacional do desenvolvedor, a solução definitiva planejada para o projeto é a **Conteinerização Completa da Aplicação**. Ao encapsular o Quarkus em um contêiner Docker anexado à mesma rede virtual `banco-network`, a aplicação resolverá nativamente os endereços `mongo1:27017`, `mongo2:27017` e `mongo3:27017` por meio do DNS interno do Docker, destravando a resiliência automática de alta disponibilidade.

---

## ☕ Passo 5: Desenvolvimento do MVP (Mínimo Produto Viável)

Para validar a comunicação, as credenciais de segurança e o funcionamento das rotas sem adicionar complexidade arquitetural precoce, criamos um MVP simplificado. 

Removemos temporariamente a estrutura tradicional de subpastas e pacotes do Java, posicionando as três classes diretamente no diretório raiz do código (`src/main/java/org/acme/banquarkus/`). Essa abordagem direta permite focar exclusivamente na validação da conectividade com o banco operacional.

### 1. A Entidade de Dados (`Teste.java`)
Esta classe representa a estrutura da tabela (coleção) que será salva no MongoDB. Ela utiliza o padrão *Active Record* do Panache, onde a própria classe herda métodos de banco de dados.

```java
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection = "testes_operacionais", database = "banquarkus_operacional")
public class Teste extends PanacheMongoEntity {
    public String mensagem;
    public String status;
}
```
*   `@MongoEntity`: Define explicitamente o nome della coleção (`testes_operacionais`) e força o escopo para a base de dados operacional correta.
*   `PanacheMongoEntity`: Fornece automaticamente um atributo `id` do tipo `ObjectId` gerenciado pelo MongoDB e expõe métodos estáticos de persistência.

---

### 2. A Camada de Serviço (`TesteService.java`)
Esta classe centraliza as regras de negócio e gerencia a comunicação direta com a entidade Panache para ler e gravar dados.

```java
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TesteService {

    public void salvarTeste(String mensagemTxt) {
        Teste novoTeste = new Teste();
        novoTeste.mensagem = mensagemTxt;
        novoTeste.status = "PROCESSADO_FALSO"; // Simula uma transação que ainda não foi consolidada no cofre central
        novoTeste.persist();
    }

    public List<Teste> listarTodos() {
        return Teste.listAll();
    }
}
```
*   `@ApplicationScoped`: Define que uma única instância desta classe será criada e compartilhada por toda a aplicação Quarkus (*Singleton*).
*   `.persist()` e `.listAll()`: Métodos herdados do Panache que executam os comandos nativos do MongoDB nos bastidores.

---

### 3. O Controlador REST (`TesteResource.java`)
Esta classe expõe as portas de entrada da aplicação, transformando rotas de texto em requisições lógicas executadas pelo Java.

```java
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/teste")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TesteResource {

    @Inject
    TesteService service;

    @POST
    @Path("/{mensagem}")
    public String criarRegistro(@PathParam("mensagem") String mensagem) {
        service.salvarTeste(mensagem);
        return "{\"resultado\": \"Gravado com sucesso no banco operacional!\"}";
    }

    @GET
    public List<Teste> buscarRegistros() {
        return service.listarTodos();
    }
}
```
*   `@Path("/teste")`: Define o endereço base da URL da nossa API (ex: `http://localhost:8080/teste`).
*   `@Inject`: Realiza a Injeção de Dependência, trazendo a instância do nosso `TesteService`.
*   `@POST`: Define que a rota de criação exige um envio do tipo POST. O parâmetro `{mensagem}` captura o texto enviado diretamente pela URL.

---

## ⚠️ Lições Aprendidas: Resolução de Escopo do Panache e Métodos HTTP

Durante a implementação e os primeiros testes das rotas do nosso MVP Java, enfrentamos dois erros críticos na camada da aplicação: um relacionado ao mapeamento do framework e outro ao protocolo HTTP.

### 🔍 Problema 1: `java.lang.IllegalArgumentException` no Panache
```text
The database attribute was not set for the @MongoEntity annotation and neither was the database property configured for the default Mongo Client (via 'quarkus.mongodb.database')
```
*   **Causa Raiz:** Por padrão, quando configuramos uma String de Conexão complexa contendo múltiplos hosts no arquivo `application.properties`, o módulo `mongodb-panache` ignora a propriedade global `quarkus.mongodb.database` e exige uma declaração explícita de escopo.
*   **Solução Aplicada:** Forçamos o escopo do banco diretamente no metadado da nossa classe de entidade Java utilizando os atributos da anotação `@MongoEntity`:
    ```java
    @MongoEntity(collection = "testes_operacionais", database = "banquarkus_operacional")
    ```

### 🔍 Problema 2: `HTTP Error 405 - Method Not Allowed` no Navegador
*   **Causa Raiz:** O método de gravação foi anotado estritamente como **`@POST`**. No entanto, quando digitamos uma URL diretamente na barra de endereços de um navegador web, o navegador dispara uma requisição do tipo **`GET`**, resultando na rejeição 405 pelo Quarkus.
*   **Solução Aplicada:** Mantivemos a integridade arquitetural do código Java utilizando o método `@POST` e migramos a estratégia de testes do navegador para o terminal através da ferramenta **`curl`**.

---

## 🌐 Passo 6: Roteiro de Testes e Homologação HTTP (O MVP em Ação)

Com o código do MVP compilado e o Quarkus rodando em modo de desenvolvimento (`./mvnw quarkus:dev`), iniciamos a validação das nossas rotas bancárias simuladas.

### 🛑 Fase 1: A Tentativa Inicial pelo Navegador (Browser)
A tentativa de acessar `http://localhost:8080/teste/deposito_sucesso` diretamente pelo navegador falhou com o erro `HTTP 405`, conforme detalhado na seção de lições aprendidas, devido à limitação dos navegadores que utilizam apenas o verbo `GET` na barra de endereços.

### 🚀 Fase 2: O Roteiro Final das 6 Requisições via `curl`
Para respeitar a arquitetura REST e enviar os verbos HTTP corretos, migramos nossos testes para o terminal do WSL2 utilizando a ferramenta `curl`. Abra uma nova aba no terminal e execute o roteiro oficial de 6 etapas:

#### 📥 Fluxo de Escrita (Gravação no Banco Operacional)
```bash
# 1. Registrar um depósito legítimo
curl -X POST http://localhost:8080/teste/deposito_sucesso

# 2. Simular o bloqueio de uma fraude
curl -X POST http://localhost:8080/teste/tentativa_fraude_bloqueada

# 3. Registrar um saque efetuado
curl -X POST http://localhost:8080/teste/saque_efetuado
```
*   *Resposta esperada para os três comandos:* `{"resultado": "Gravado com sucesso no banco operacional!"}`

#### 📤 Fluxo de Leitura (Consulta de Persistência)
```bash
# 4. Listar todas as transações operacionais via GET
curl http://localhost:8080/teste
```
*   *Resposta esperada:* Um array JSON contendo os 3 registros completos e seus respectivos `id` (ObjectId) gerados de forma persistente pelo MongoDB.

#### ⚡ Fluxo de Resiliência a Ataques DoS (Failover)
```bash
# 5. Derrubar o nó líder atual do banco de dados (Simulação de ataque DoS)
docker kill mongo1

# 6. Refazer a leitura imediatamente com o líder fora do ar
curl http://localhost:8080/teste
```
*   *Nota de Observação:* No cenário híbrido atual (aplicação fora do Docker), este passo disparará a exceção de timeout/conexão recusada detalhada no Passo 4 devido ao conflito de portas externas mapeadas em localhost, validando a necessidade da conteinerização que faremos a seguir.

---

## 🐙 Passo 6.5: Versionamento e Publicação no GitHub

Para garantir o backup seguro do código e permitir que o laboratório seja compartilhado como portfólio profissional, estruturamos o versionamento do projeto utilizando o Git diretamente pelo terminal do WSL2.

### 1. Configurar o Arquivo `.gitignore`
Na raiz do projeto, certifique-se de que o arquivo `.gitignore` contenha as seguintes linhas para evitar o envio de arquivos desnecessários:
```text
target/
.quarkus/
.target/
*.log
.idea/
.vscode/
*.suo
.banco_data/
```

### 2. Inicializar o Repositório e Fazer o Primeiro Commit
```bash
git init
git branch -M main
git add .
git commit -m "feat: infraestrutura do cluster mongodb e mvp quarkus homologados"
```

### 3. Vincular e Subir para o GitHub
```bash
git remote add origin https://github.com
git push -u origin main
```

---

## 🎯 Conclusão e Próximos Passos (Roadmap do Projeto)

Este MVP validou com sucesso a fundação do nosso laboratório híbrido. Conseguimos estabelecer um cluster MongoDB com Replica Set tolerante a falhas, configuramos políticas básicas de controle de acesso (RBAC) e implementamos um fluxo funcional de escrita e leitura em português.

O planejamento estratégico para a evolução deste ecossistema está dividido em três etapas claras:

### 🚀 Próximas Etapas Imediatas:
1.  **Migração para a Opção A (Persistência Avançada de Volumes):** Reescrever o script `up-db.sh` para abolir a flag `-v`. O objetivo é criar uma lógica de checagem profunda capaz de reaproveitar os volumes existentes no disco rígido do WSL2 de forma segura, impedindo conflitos de metadados quando o cluster for reiniciado.
2.  **Criação das Coleções Reais do Banco (Padrão pt-BR):** Substituir a estrutura de "Teste" pelo ecossistema do simulador bancário nacional, criando as entidades Java `ContaBancaria.java` e `Movimentacao.java`.
3.  **Implementação do Serviço Oculto de Consolidação:** Desenvolver um componente agendador (`@Scheduled` no Quarkus) que rodará em segundo plano. Ele utilizará as credenciais do `admin_geral` para extrair as movimentações aprovadas na base operacional (`banquarkus_operacional`) e atualizar os saldos reais e imutáveis dentro do cofre protegido (`banquarkus_central`).
No arquivo bash, colocar no log qual foi o container que ganhou a eleição.
No próximo passo ele está chamando o mongo1 nominalmente, isso não poderia causar uma falha?  
---

## 🔮 O Vislumbre do Futuro: O Projeto em Nível Avançado

Após consolidarmos a base, este laboratório foi desenhado para se transformar em uma plataforma robusta de simulação de ameaças e defesa digital. Eis como este projeto ficará após a implementação completa dos módulos avançados de CyberSec:

### 1. Laboratório de Exploração de Vulnerabilidades (Red Team)
*   **NoSQL Injection Real:** Vamos programar intencionalmente uma brecha na rota de busca de contas (ex: aceitando parâmetros do MongoDB como `{"$gt": ""}`). Você aprenderá a criar scripts maliciosos para burlar a autenticação da API e extrair dados sem possuir a senha dos clientes.
*   **Burlar Limites de Transação (Business Logic Flaws):** Criaremos rotas sem validação de concorrência ou com falhas de sinal (permitindo transferências com valores negativos), simulando ataques que tentam quebrar a lógica de negócios para duplicar saldos de forma fraudulenta.

### 2. Mecanismos de Defesa em Profundidade (Blue Team)
*   **Monitoramento de Logs de Segurança (SIEM/Audit):** Integração de logs estruturados em português que disparam alertas em tempo real no terminal sempre que uma transação fugir do comportamento padrão de um cliente (ex: 10 transferências de alto valor em menos de 1 minuto).
*   **Criptografia de Dados em Repouso (Crypto-at-Rest):** Implementação de criptografia nas coleções do MongoDB para garantir que, se um atacante conseguir invadir o servidor Linux e roubar os arquivos físicos do HD (os volumes do Docker), ele ainda assim veja apenas dados embaralhados, protegendo as informações confidenciais do banco.
