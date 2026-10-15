package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria um usuário de cada perfil e alguns produtos na primeira execução,
 * e ajusta usuários antigos que estavam sem perfil definido.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarios, ProdutoRepository produtos,
            PasswordEncoder encoder, @Value("${app.seed.senha-inicial}") String senhaInicial) {
        return args -> {
            criarUsuarioSeNaoExistir(usuarios, encoder, "admin", Perfil.ADMIN, senhaInicial);
            criarUsuarioSeNaoExistir(usuarios, encoder, "gerente01", Perfil.GERENTE, senhaInicial);
            criarUsuarioSeNaoExistir(usuarios, encoder, "atendente01", Perfil.ATENDENTE, senhaInicial);
            corrigirPerfisAntigos(usuarios);

            if (produtos.count() == 0) {
                produtos.save(produto("Pizza Calabresa", "Pizza", 54.90));
                produtos.save(produto("Pizza Margherita", "Pizza", 52.00));
                produtos.save(produto("Pizza Quatro Queijos", "Pizza", 62.50));
                produtos.save(produto("Esfiha de Carne", "Esfiha", 7.50));
                produtos.save(produto("Refrigerante 2L", "Bebida", 14.00));
                log.info("Produtos de exemplo cadastrados.");
            }
        };
    }

    private void criarUsuarioSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder,
            String username, Perfil perfil, String senha) {
        if (repo.findByUsername(username).isEmpty()) {
            Usuario user = new Usuario();
            user.setUsername(username);
            user.setRole(perfil.name());
            user.setPassword(encoder.encode(senha));
            repo.save(user);
            // a senha não é escrita no log de propósito
            log.info("Usuário {} criado com perfil {}.", username, perfil);
        }
    }

    /** O projeto original salvava "admin" sem role e usava USER para os demais. */
    private void corrigirPerfisAntigos(UsuarioRepository repo) {
        for (Usuario u : repo.findAll()) {
            String role = u.getRole();
            boolean valido = role != null && (role.equals("ADMIN") || role.equals("GERENTE") || role.equals("ATENDENTE"));
            if (!valido) {
                u.setRole("admin".equals(u.getUsername()) ? Perfil.ADMIN.name() : Perfil.ATENDENTE.name());
                repo.save(u);
                log.info("Perfil do usuário {} ajustado para {}.", u.getUsername(), u.getRole());
            }
        }
    }

    private static Produto produto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }
}
