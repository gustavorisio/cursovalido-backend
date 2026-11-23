package com.cursovalido.backend.authentication.validation.convite;

import com.cursovalido.backend.authentication.dto.CadastroUsuarioRequest;
import com.cursovalido.backend.authentication.entity.ConviteAcesso;
import com.cursovalido.backend.authentication.entity.Perfil;
import com.cursovalido.backend.authentication.entity.StatusAprovacao;
import com.cursovalido.backend.authentication.entity.Usuario;
import com.cursovalido.backend.authentication.exception.ConviteAcessoInvalidoException;
import com.cursovalido.backend.authentication.repository.UsuarioRepository;
import com.cursovalido.backend.authentication.service.PoliticaService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConviteValidationTest {
        private final PoliticaService politicas = mock(PoliticaService.class);
        private final UsuarioRepository usuarios = mock(UsuarioRepository.class);

        @Test
        void cadastroPeloConviteConfereEmailTermosEConta() {
                var convite = new ConviteAcesso("prof@example.com", Perfil.PROFESSOR, "h",
                                LocalDateTime.now().plusDays(1));
                when(politicas.versaoAtiva("v1")).thenReturn(true);

                assertThatThrownBy(() -> ConviteValidation.validarCadastro(convite,
                                cadastro("outro@example.com", "v1", true),
                                politicas, usuarios)).hasMessageContaining("mesmo e-mail");
                assertThatThrownBy(() -> ConviteValidation.validarCadastro(convite,
                                cadastro("prof@example.com", "v0", true),
                                politicas, usuarios)).hasMessageContaining("termos foram atualizados");
                assertThatThrownBy(() -> ConviteValidation.validarCadastro(convite,
                                cadastro("prof@example.com", "v1", false),
                                politicas, usuarios)).hasMessageContaining("Aceite os Termos");
                when(usuarios.existsByEmail("prof@example.com")).thenReturn(true);
                assertThatThrownBy(() -> ConviteValidation.validarCadastro(convite,
                                cadastro("prof@example.com", "v1", true),
                                politicas, usuarios)).hasMessageContaining("já possui uma conta");
        }

        @Test
        void convitePendenteSoBloqueiaEnquantoValido() {
                assertThatCode(() -> ConviteValidation.validarConvitePendente(null)).doesNotThrowAnyException();
                var expirado = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().minusMinutes(1));
                assertThatCode(() -> ConviteValidation.validarConvitePendente(expirado)).doesNotThrowAnyException();
                var valido = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().plusDays(1));
                assertThatThrownBy(() -> ConviteValidation.validarConvitePendente(valido))
                                .isInstanceOf(ConviteAcessoInvalidoException.class);
        }

        @Test
        void reenvioDeConviteNaoPendenteERecusado() {
                var convite = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().plusDays(1));
                convite.concluir();
                assertThatThrownBy(() -> ConviteValidation.validarReenvio(convite))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void reenvioRespeitaIntervaloELimiteDeEnvios() {
                var convite = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().plusDays(1));
                assertThatThrownBy(() -> ConviteValidation.validarReenvio(convite))
                                .hasMessageContaining("Aguarde 2 minutos");

                ReflectionTestUtils.setField(convite, "enviadoEm", LocalDateTime.now().minusMinutes(3));
                assertThatCode(() -> ConviteValidation.validarReenvio(convite)).doesNotThrowAnyException();

                ReflectionTestUtils.setField(convite, "quantidadeEnvios", 5);
                assertThatThrownBy(() -> ConviteValidation.validarReenvio(convite))
                                .hasMessageContaining("limite de reenvios");
        }

        @Test
        void conviteExpiradoNaoPermiteCadastro() {
                var convite = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().minusMinutes(1));
                assertThatThrownBy(() -> ConviteValidation.validarCadastro(convite, cadastro("a@x.com", "v1", true),
                                politicas, usuarios)).hasMessageContaining("expirado");
        }

        @Test
        void aprovacaoManualSoValeParaCadastrosPendentes() {
                Usuario usuario = new Usuario();
                usuario.setStatusAprovacao(StatusAprovacao.APROVADO);
                assertThatThrownBy(() -> ConviteValidation.validarUsuarioPendente(usuario))
                                .hasMessageContaining("não está mais aguardando");
                usuario.setStatusAprovacao(StatusAprovacao.PENDENTE_APROVACAO);
                assertThatCode(() -> ConviteValidation.validarUsuarioPendente(usuario)).doesNotThrowAnyException();
        }

        @Test
        void renovarGeraNovoEnvioEReabreOConvite() {
                var convite = new ConviteAcesso("a@x.com", Perfil.PROFESSOR, "h", LocalDateTime.now().plusDays(1));
                convite.concluir();
                convite.renovar("novo", LocalDateTime.now().plusDays(2));
                assertThat(convite.getTokenHash()).isEqualTo("novo");
                assertThat(convite.getQuantidadeEnvios()).isEqualTo(2);
                assertThat(convite.isEmailConfirmado()).isFalse();
                assertThat(convite.expirado()).isFalse();
        }

        private CadastroUsuarioRequest cadastro(String email, String versao, boolean aceitou) {
                return new CadastroUsuarioRequest("Professor", email, "Senha@1", "Senha@1", true, aceitou, versao,
                                true, "1.0");
        }
}
