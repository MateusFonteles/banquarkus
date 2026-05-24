package org.acme.banquarkus.resource.admin;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.banquarkus.domain.ContaBancaria;
import org.acme.banquarkus.service.ContaService;

import java.util.List;

/**
 * RESOURCE (CONTROLLER): Backoffice Administrativo
 * Esta rota simula operações de retaguarda. No futuro, ela será fortemente protegida
 * exigindo um token JWT com a role (permissão) específica de "ADMIN".
 */
@Path("/api/admin/contas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminContaResource {

    @Inject
    ContaService contaService;

    @GET
    public List<ContaBancaria> listarTodas() {
        return contaService.listarTodasAsContas();
    }

    @POST
    public Response criarConta(ContaBancaria conta) {
        // SECURITY NOTE: Atualmente sujeito a Mass Assignment (Vazamento de Abstração).
        // Um atacante pode enviar o ID e tentar forçar a sobrescrita de uma conta existente.
        // A mitigação futura envolverá a implementação do Padrão DTO.
        ContaBancaria contaCriada = contaService.abrirConta(conta);
        return Response.status(Response.Status.CREATED).entity(contaCriada).build();
    }
}