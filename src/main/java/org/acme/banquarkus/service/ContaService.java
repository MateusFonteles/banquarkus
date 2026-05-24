package org.acme.banquarkus.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.banquarkus.domain.ContaBancaria;
import java.util.List;

/**
 * SERVIÇO: Lógica de Negócios de Contas
 * Separa a regra de manipulação do banco de dados das rotas HTTP (Resources).
 */
@ApplicationScoped
public class ContaService {

    // Método Gerencial: Retorna a coleção inteira.
    // POTENCIAL FALHA: Sem paginação. Vulnerável a vazamento em massa se a rota Admin for comprometida.
    public List<ContaBancaria> listarTodasAsContas() {
        return ContaBancaria.listAll();
    }

    // Método Cliente: Busca direcionada.
    // SECURITY NOTE: Na Fase 2, esta busca não confiará cegamente nos parâmetros passados.
    // Ela validará se a 'agencia' e 'numeroConta' solicitadas pertencem ao usuário logado (Token JWT),
    // mitigando ataques de BOLA/IDOR.
    public ContaBancaria buscarMinhaConta(String agencia, String numeroConta) {
        return ContaBancaria.find("agencia = ?1 and numeroConta = ?2", agencia, numeroConta).firstResult();
    }

    public ContaBancaria abrirConta(ContaBancaria novaConta) {
        novaConta.persist();
        return novaConta;
    }
}