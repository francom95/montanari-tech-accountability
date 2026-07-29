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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (RuntimeException e) {
            // F11.2 A13: registra el intento fallido contra el usuario existente (si el email
            // matchea alguno); entidad_id es NOT NULL, así que un email inexistente no deja
            // registro acá (no hay entidad real a la que atribuirlo) — igual que el login
            // exitoso, es una búsqueda global (ver findByEmailGlobalParaLogin).
            usuarioRepository.findByEmailGlobalParaLogin(request.email()).stream().findFirst()
                    .ifPresent(u -> auditoriaService.registrarComo(u.getId(), AccionAuditoria.LOGIN_FALLIDO,
                            "Usuario", u.getId(), null, null, false, "Intento de login fallido"));
            throw e;
        }

        // F11.2 B2: búsqueda global a propósito, igual que CustomUserDetailsService —
        // el email es único solo dentro de cada tenant, no globalmente.
        Usuario usuario = usuarioRepository.findByEmailGlobalParaLogin(request.email()).stream()
                .findFirst()
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
