package org.acme.banquarkus.domain;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;

/**
 * ENTIDADE DE DOMÍNIO: Cliente
 * Representa os dados sensíveis do usuário. Em conformidade com leis de 
 * proteção de dados (LGPD), o acesso a esta tabela deve ser rigorosamente auditado.
 */
@MongoEntity(collection = "clientes", database = "banquarkus_operacional")
public class Cliente extends PanacheMongoEntity {
    
    public String nomeCompleto;
    public String cpf;
    public String email;
    
    // SECURITY NOTE: Senhas nunca devem ser armazenadas em texto puro (Plain Text).
    // Na Fase de IAM (Gestão de Identidade), este campo armazenará um Hash (ex: BCrypt ou Argon2)
    // para mitigar vazamentos em caso de SQL/NoSQL Injection.
    public String senha; 
    
    // SECURITY NOTE: Controle de Acesso Baseado em Funções (RBAC).
    // Define se o usuário é um cliente comum ou um administrador.
    public String role; // "ADMIN" ou "CLIENTE"
    public String status; // "ATIVO", "BLOQUEADO", "EM_ANALISE"
    
    public LocalDateTime dataCriacao;
    
    public Cliente() {
        this.dataCriacao = LocalDateTime.now();
    }
}