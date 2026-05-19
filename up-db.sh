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
# Loop que verifica a cada 1 segundo se o mongo1 virou PRIMARY ou se algum líder foi eleito
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
