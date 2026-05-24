package org.acme.banquarkus.domain;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDADE DE DOMÍNIO: Transação (Ledger)
 * Do ponto de vista de CyberSec, esta é uma tabela *Append-Only* (Apenas Inserção).
 * Registros aqui são o rastro de auditoria. Eles nunca devem ser atualizados (UPDATE) 
 * ou deletados (DELETE), garantindo a imutabilidade forense do sistema.
 */
@MongoEntity(collection = "transacoes", database = "banquarkus_operacional")
public class Transacao extends PanacheMongoEntity {

    public ObjectId contaOrigemId;
    public ObjectId contaDestinoId;
    
    public BigDecimal valor;
    public String tipoOperacao; // "DEPOSITO", "SAQUE", "TRANSFERENCIA"
    
    public String status; // "CONCLUIDA", "NEGADA", "SOB_REVISAO"
    
    // SECURITY NOTE: Metadados vitais para o Blue Team (Defesa).
    // O Timestamp exato e o IP de origem ajudam a detectar ataques de repetição 
    // e auxiliam em investigações de fraudes e acessos indevidos.
    public LocalDateTime dataHora;
    public String ipOrigem; 

    public Transacao() {
        this.dataHora = LocalDateTime.now();
    }
}