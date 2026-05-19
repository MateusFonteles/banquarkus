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
