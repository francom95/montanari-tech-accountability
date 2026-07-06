package com.montanaritech.contable.auth;

import com.montanaritech.contable.auth.dto.LoginRequest;
import com.montanaritech.contable.auth.dto.TokenPairResponse;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        // Lanza BadCredentialsException si no matchea -> GlobalExceptionHandler la mapea a 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        usuario.setUltimoLoginEn(Instant.now());

        // El request de login es público (sin JWT todavía), así que el actor
        // no sale del SecurityContext: se pasa explícito (F1.1 §14.2 audita LOGIN).
        auditoriaService.registrarComo(
                usuario.getId(), AccionAuditoria.LOGIN, "Usuario", usuario.getId(), null, null, false, null);

        return emitirPar(usuario);
    }

    @Transactional
    public TokenPairResponse refrescar(String refreshTokenCrudo) {
        Usuario usuario = refreshTokenService.consumirYRotar(refreshTokenCrudo)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido, vencido o ya usado"));
        return emitirPar(usuario);
    }

    @Transactional
    public void logout(String refreshTokenCrudo) {
        refreshTokenService.revocar(refreshTokenCrudo);
    }

    private TokenPairResponse emitirPar(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = refreshTokenService.emitir(usuario);
        return new TokenPairResponse(accessToken, refreshToken);
    }
}
