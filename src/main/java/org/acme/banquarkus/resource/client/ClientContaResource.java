package org.acme.banquarkus.resource.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.banquarkus.domain.ContaBancaria;
import org.acme.banquarkus.domain.Transacao;
import org.acme.banquarkus.service.ContaService;
import org.acme.banquarkus.service.TransacaoService;

import java.math.BigDecimal;

/**
 * RESOURCE (CONTROLLER): Aplicativo do Cliente
 * Simula a porta de entrada para os correntistas comuns. É a fronteira principal
 * para testes de falhas de autorização (BOLA/IDOR).
 */
@Path("/api/client/minha-conta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientContaResource {

    @Inject
    ContaService contaService;

    @Inject
    TransacaoService transacaoService;

    // ROTA SUJEITA A BOLA (Broken Object Level Authorization)
    // Um atacante pode alterar a {agencia} e {numero} na URL e, como não há validação
    // de dono (Owner Validation), ele pode ver saldos de terceiros.
    @GET
    @Path("/{agencia}/{numero}")
    public Response consultarMeuSaldo(@PathParam("agencia") String agencia, @PathParam("numero") String numero) {
        ContaBancaria conta = contaService.buscarMinhaConta(agencia, numero);
        
        if (conta == null) {
            // SECURITY NOTE: Mensagem de erro genérica é uma boa prática.
            // Não confirmamos se a conta existe mas está bloqueada, ou se simplesmente
            // não existe. Isso dificulta a enumeração de usuários por um atacante.
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"erro\": \"Conta não encontrada ou acesso negado.\"}")
                           .build();
        }
        
        return Response.ok(conta).build();
    }

    @POST
    @Path("/{agencia}/{numero}/deposito")
    public Response depositar(@PathParam("agencia") String agencia, 
                              @PathParam("numero") String numero, 
                              BigDecimal valor) {
        
        Transacao recibo = transacaoService.realizarDeposito(agencia, numero, valor);
        return Response.status(Response.Status.CREATED).entity(recibo).build();
    }
}