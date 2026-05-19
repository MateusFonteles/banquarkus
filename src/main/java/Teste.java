import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection = "testes_operacionais", database = "banquarkus_operacional")
public class Teste extends PanacheMongoEntity {
    public String mensagem;
    public String status;
}
