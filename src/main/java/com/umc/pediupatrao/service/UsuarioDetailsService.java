package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(perfilDe(usuario).name()) // gera a authority ROLE_<PERFIL>
                .build();
    }

    /**
     * Usuários antigos (role nula ou "USER") recebem o perfil de menor privilégio.
     * Um perfil desconhecido nunca vira ADMIN por engano.
     */
    static Perfil perfilDe(Usuario usuario) {
        try {
            return Perfil.deTexto(usuario.getRole());
        } catch (IllegalArgumentException e) {
            return Perfil.ATENDENTE;
        }
    }
}
