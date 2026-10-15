package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock RegistroAuditoriaRepository repository;
    @Mock UsuarioLogado usuarioLogado;
    @InjectMocks AuditoriaService service;

    @Test
    void compararIgnoraCamposSemMudanca() {
        List<AlteracaoCampo> lista = new ArrayList<>();
        AuditoriaService.comparar(lista, "logradouro", "Rua das Flores", "Rua das Flores");
        AuditoriaService.comparar(lista, "numero", "10", "12");
        assertEquals(1, lista.size());
        assertEquals("numero", lista.get(0).getCampo());
    }

    @Test
    void registroGuardaUsuarioPerfilEValores() {
        when(usuarioLogado.username()).thenReturn("atendente01");
        when(usuarioLogado.perfil()).thenReturn("ATENDENTE");
        when(repository.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistroAuditoria r = service.registrar(TipoOperacao.ALTERACAO_CLIENTE, "CLIENTE", "c1",
                List.of(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil")), null);

        assertEquals("atendente01", r.getUsuario());
        assertEquals("ATENDENTE", r.getPerfil());
        assertEquals("Rua das Flores", r.getAlteracoes().get(0).getValorAnterior());
        assertEquals("Av. Brasil", r.getAlteracoes().get(0).getValorNovo());
    }

    @Test
    void senhaNuncaEhGravada() {
        when(repository.insert(any())).thenAnswer(inv -> inv.getArgument(0));
        RegistroAuditoria r = service.registrar(TipoOperacao.ALTERACAO_USUARIO, "USUARIO", "u1",
                List.of(new AlteracaoCampo("senha", "antiga123", "nova123")), null);
        assertEquals("[protegido]", r.getAlteracoes().get(0).getValorAnterior());
        assertEquals("[protegido]", r.getAlteracoes().get(0).getValorNovo());
    }

    @Test
    void consultaMascaraTelefone() {
        RegistroAuditoria salvo = new RegistroAuditoria("atendente01", "ATENDENTE", LocalDateTime.now(),
                TipoOperacao.ALTERACAO_CLIENTE, "CLIENTE", "c1",
                List.of(new AlteracaoCampo("telefone", "(11) 98888-1234", "(11) 97777-5678")), null);
        when(repository.findTop200ByOrderByDataHoraDesc()).thenReturn(List.of(salvo));

        AlteracaoCampo a = service.consultar(null, null).get(0).getAlteracoes().get(0);
        assertEquals("*******1234", a.getValorAnterior());
        assertTrue(a.getValorNovo().endsWith("5678"));
    }
}
