#!/bin/bash
set -e

echo "🛑 0. Reset Seguro: Parando contêineres (PRESERVANDO VOLUMES)..."
# Removida a flag -v. Agora os dados sobrevivem aos reboots!
docker compose down

echo "🚀 1. Iniciando infraestrutura e API Quarkus no Docker..."
docker compose up -d

echo "⏳ 2. Aguardando 6 segundos para inicialização dos motores..."
sleep 6

echo "🔄 3. Checando/Configurando o Replica Set (banco-rs)..."
# Tenta ver se o Replica Set já está ativo. Se der erro ou retornar 0, ele inicia.
RS_STATUS=$(docker exec -i mongo1 mongosh --quiet --eval "rs.status().ok" 2>/dev/null || echo "0")

if [ "$RS_STATUS" != "1" ]; then
  echo "   ⚙️ Primeira execução detectada. Iniciando Replica Set..."
  docker exec -i mongo1 mongosh --quiet --eval '
    rs.initiate({
      _id: "banco-rs",
      members: [
        { _id: 0, host: "mongo1:27017" },
        { _id: 1, host: "mongo2:27017" },
        { _id: 2, host: "mongo3:27017" }
      ]
    });
  ' > /dev/null
else
  echo "   ✅ Replica Set já estava configurado (Volumes persistidos!)."
fi

echo "⏳ 4. Caçando o líder (PRIMARY) da rede..."
CONTAINER_LIDER=""
LIDER_OK=0
CONTAINERS=("mongo1" "mongo2" "mongo3")

for i in {1..15}; do
  for container in "${CONTAINERS[@]}"; do
    RESPOSTA=$(docker exec -i "$container" mongosh --quiet --eval "db.hello().isWritablePrimary" | tr -d '\r\n ' 2>/dev/null || echo "false")
    if [ "$RESPOSTA" = "true" ]; then
      CONTAINER_LIDER="$container"
      LIDER_OK=1
      break 2
    fi
  done
  sleep 1
done

if [ $LIDER_OK -ne 1 ]; then
  echo "❌ Erro Crítico: O cluster não conseguiu eleger um líder em 15 segundos."
  exit 1
fi

echo "   👑 Líder estabelecido! O nó comandante atual é: [$CONTAINER_LIDER]"

echo "🔒 5. Aplicando Segurança (RBAC) e populando dados (Modo Idempotente)..."
# Usamos o líder detectado dinamicamente para aplicar as regras
docker exec -i "$CONTAINER_LIDER" mongosh --quiet --eval '
  // 1. Criação condicional de Admin
  var bancoAdmin = db.getSiblingDB("admin");
  if (bancoAdmin.getUser("admin_geral") == null) {
    bancoAdmin.createUser({
      user: "admin_geral",
      pwd: "senha123",
      roles: [ { role: "userAdminAnyDatabase", db: "admin" }, { role: "readWriteAnyDatabase", db: "admin" } ]
    });
  }

  // 2. Criação condicional de Usuário Quarkus
  var dbOperacional = db.getSiblingDB("banquarkus_operacional");
  if (dbOperacional.getUser("app_banquarkus_user") == null) {
    dbOperacional.createUser({
      user: "app_banquarkus_user",
      pwd: "senha123",
      roles: [ { role: "readWrite", db: "banquarkus_operacional" } ]
    });
  }

  // 3. População Inicial Condicional (Só insere se a coleção estiver vazia)
  var dbCentral = db.getSiblingDB("banquarkus_central");
  if (dbCentral.contas_consolidadas.countDocuments({}) === 0) {
    dbCentral.contas_consolidadas.insertOne({
      sistema: "Quarkus App",
      status: "Ativo",
      descricao: "Primeiro registro no banco central!"
    });
  }
' > /dev/null

echo "   ✅ Bancos validados com dados iniciais e regras de acesso."

echo "🔍 6. Validando persistência e permissões de leitura do Quarkus..."
docker exec -i "$CONTAINER_LIDER" mongosh --quiet -u app_banquarkus_user -p senha123 --authenticationDatabase banquarkus_operacional --eval 'db.getSiblingDB("banquarkus_operacional").contas.findOne();' > /dev/null
echo "   ✅ Acesso da API validado."

echo ""
echo "==========================================================="
echo "🎉 AMBIENTE PRONTO E SEGURO!"
echo "👑 Líder atual: $CONTAINER_LIDER"
echo "==========================================================="
echo  "🚀 A API Quarkus está rodando em: http://localhost:8080/"
echo ""
echo "📋 COMANDOS ÚTEIS PARA O SEU LABORATÓRIO:"
echo "-----------------------------------------------------------"
echo "🔹 Acompanhar logs da API (Quarkus) ao vivo:"
echo "   docker logs -f banquarkus-api"
echo ""
echo "🔹 Realizar um depósito na API (Gravação):"
echo "   curl -X POST http://localhost:8080/teste/simulacao_deposito"
echo ""
echo "🔹 Consultar todos os registros da API (Leitura):"
echo "   curl http://localhost:8080/"
echo ""
echo "🔹 Simular ataque DoS (Derrubar o banco líder):"
echo "   docker kill mongo1"
echo ""
echo "🔹 Acessar o console do banco como Administrador:"
echo "   docker exec -it mongo1 mongosh -u admin_geral -p senha123 --authenticationDatabase admin"
echo "   docker kill mongo1"
echo ""
echo "🔹 Derrubar, atualizar e reiniciar o ambiente, para quando fizer mudanças no POM:"
echo "  docker compose down && docker compose build --no-cache quarkus-app && ./run.sh"
echo "-----------------------------------------------------------"