package org.acme.banquarkus.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.acme.banquarkus.domain.ContaBancaria;
import org.acme.banquarkus.domain.Transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SERVIÇO: Lógica de Movimentação Financeira
 * Centraliza as regras de crédito, débito e auditoria das operações.
 */
@ApplicationScoped
public class TransacaoService {

    @Inject
    ContaService contaService;

    public Transacao realizarDeposito(String agencia, String numeroConta, BigDecimal valor) {
        // SECURITY NOTE: Validação estrita de entrada (Input Validation).
        // Impede que um atacante envie valores negativos para tentar transformar
        // um depósito em um saque (falha clássica de lógica em sistemas legados).
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WebApplicationException("O valor do depósito deve ser maior que zero.", Response.Status.BAD_REQUEST);
        }

        ContaBancaria conta = contaService.buscarMinhaConta(agencia, numeroConta);
        if (conta == null) {
            throw new WebApplicationException("Conta não encontrada.", Response.Status.NOT_FOUND);
        }

        // 1. Atualiza o saldo da conta operacional
        conta.saldoAtual = conta.saldoAtual.add(valor);
        conta.update();

        // 2. Grava o registro imutável da transação
        Transacao transacao = new Transacao();
        transacao.contaOrigemId = conta.id; // Num depósito simples, origem e destino são a mesma entidade
        transacao.contaDestinoId = conta.id;
        transacao.valor = valor;
        transacao.tipoOperacao = "DEPOSITO";
        transacao.status = "CONCLUIDA";
        transacao.dataHora = LocalDateTime.now();
        
        // TODO: Em uma implementação futura, extrairemos o IP real do Header HTTP da requisição
        transacao.ipOrigem = "127.0.0.1"; 
        
        transacao.persist();

        return transacao;
    }
}