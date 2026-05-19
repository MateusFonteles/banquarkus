import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TesteService {

    public void salvarTeste(String mensagemTxt) {
        Teste novoTeste = new Teste();
        novoTeste.mensagem = mensagemTxt;
        novoTeste.status = "PROCESSADO_FALSO"; // Simula que ainda não foi para o banco central
        novoTeste.persist();
    }

    public List<Teste> listarTodos() {
        return Teste.listAll();
    }
}
