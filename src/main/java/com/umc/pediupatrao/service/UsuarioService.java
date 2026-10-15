package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario salvarUsuario(Usuario dados) {
        String username = normalizarUsername(dados.getUsername());
        Perfil perfil = Perfil.deTexto(dados.getRole());
        validarSenha(dados.getPassword(), true);
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RegraNegocioException("Já existe um usuário com o login " + username + ".");
        }

        Usuario novo = new Usuario();
        novo.setUsername(username);
        novo.setRole(perfil.name());
        novo.setPassword(passwordEncoder.encode(dados.getPassword()));
        Usuario salvo = usuarioRepository.save(novo);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "username", null, salvo.getUsername());
        AuditoriaService.comparar(alteracoes, "perfil", null, salvo.getRole());
        auditoriaService.registrar(TipoOperacao.INCLUSAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    public Usuario atualizarUsuario(String id, Usuario dados) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        String username = normalizarUsername(dados.getUsername());
        Perfil novoPerfil = Perfil.deTexto(dados.getRole());
        boolean trocarSenha = dados.getPassword() != null && !dados.getPassword().isBlank();
        if (trocarSenha) {
            validarSenha(dados.getPassword(), true);
        }

        usuarioRepository.findByUsername(username)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("Já existe um usuário com o login " + username + ".");
                });

        boolean ehOProprio = existente.getUsername().equals(usuarioLogado.username());
        boolean mudouPerfil = !novoPerfil.name().equals(existente.getRole());
        if (ehOProprio && mudouPerfil) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (ehOProprio && !username.equals(existente.getUsername())) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio login.");
        }
        if (mudouPerfil && Perfil.ADMIN.name().equals(existente.getRole()) && contarAdmins() <= 1) {
            throw new RegraNegocioException("O sistema precisa ter pelo menos um ADMIN.");
        }

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "username", existente.getUsername(), username);
        AuditoriaService.comparar(alteracoes, "perfil", existente.getRole(), novoPerfil.name());
        if (trocarSenha) {
            // o valor é substituído por "[protegido]" no AuditoriaService
            alteracoes.add(new AlteracaoCampo("senha", null, null));
        }

        existente.setUsername(username);
        existente.setRole(novoPerfil.name());
        if (trocarSenha) {
            existente.setPassword(passwordEncoder.encode(dados.getPassword()));
        }
        Usuario salvo = usuarioRepository.save(existente);

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO,
                    salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    public void deletarUsuario(String id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (existente.getUsername().equals(usuarioLogado.username())) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário.");
        }
        if (Perfil.ADMIN.name().equals(existente.getRole()) && contarAdmins() <= 1) {
            throw new RegraNegocioException("O sistema precisa ter pelo menos um ADMIN.");
        }
        usuarioRepository.deleteById(id);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        AuditoriaService.comparar(alteracoes, "username", existente.getUsername(), null);
        AuditoriaService.comparar(alteracoes, "perfil", existente.getRole(), null);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO,
                id, alteracoes, null);
    }

    private long contarAdmins() {
        return usuarioRepository.countByRole(Perfil.ADMIN.name());
    }

    private static String normalizarUsername(String username) {
        if (username == null || !username.trim().matches("[A-Za-z0-9._-]{3,30}")) {
            throw new RegraNegocioException("Login deve ter de 3 a 30 caracteres (letras, números, ponto, hífen ou _).");
        }
        return username.trim().toLowerCase();
    }

    private static void validarSenha(String senha, boolean obrigatoria) {
        if (senha == null || senha.isBlank()) {
            if (obrigatoria) {
                throw new RegraNegocioException("Informe a senha.");
            }
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }
}
