package org.acme.banquarkus.domain;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDADE DE DOMÍNIO: Conta Bancária
 * Esta coleção no MongoDB representa a camada operacional volátil. 
 * É o alvo principal de ataques de manipulação de saldo.
 */
@MongoEntity(collection = "contas", database = "banquarkus_operacional")
public class ContaBancaria extends PanacheMongoEntity {

    // Chave estrangeira que conecta a conta à identidade do usuário (Cliente).
    public ObjectId clienteId; 
    
    public String agencia;
    public String numeroConta;
    
    // SECURITY NOTE: O uso de BigDecimal é mandatório em sistemas financeiros.
    // Usar 'Double' ou 'Float' gera falhas de arredondamento em processadores 
    // de arquitetura x86, permitindo ataques de "Salami Slicing" (roubo de frações de centavos).
    public BigDecimal saldoAtual;
    public BigDecimal limiteTransferenciaDiaria;
    
    public String tipoConta; // "CORRENTE", "INVESTIMENTO"
    public String status; // "ATIVA", "ENCERRADA"
    
    public LocalDateTime dataAbertura;

    public ContaBancaria() {
        this.saldoAtual = BigDecimal.ZERO;
        this.dataAbertura = LocalDateTime.now();
    }
}