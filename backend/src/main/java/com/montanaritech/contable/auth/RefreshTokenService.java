package com.montanaritech.contable.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens opacos (no JWT): valor aleatorio de alta entropía, se
 * persiste solo su hash SHA-256 (F1.1 diccionario — nunca el valor crudo).
 * Rotación en cada uso: el token viejo se revoca y se emite uno nuevo, para
 * limitar el daño de un token robado que se reutiliza.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Transactional
    public String emitir(Usuario usuario) {
        String tokenCrudo = generarTokenCrudo();
        RefreshToken entidad = new RefreshToken();
        entidad.setUsuario(usuario);
        entidad.setTokenHash(hashear(tokenCrudo));
        entidad.setExpiraEn(Instant.now().plus(Duration.ofDays(refreshTokenTtlDays)));
        refreshTokenRepository.save(entidad);
        return tokenCrudo;
    }

    /**
     * Valida el token, lo revoca (rotación) y devuelve el usuario asociado.
     * Vacío si el token no existe, ya fue usado/revocado, o venció.
     */
    @Transactional
    public Optional<Usuario> consumirYRotar(String tokenCrudo) {
        return refreshTokenRepository.findByTokenHashAndRevocadoFalse(hashear(tokenCrudo))
                .filter(rt -> rt.getExpiraEn().isAfter(Instant.now()))
                .map(rt -> {
                    rt.setRevocado(true);
                    return rt.getUsuario();
                });
    }

    @Transactional
    public void revocar(String tokenCrudo) {
        refreshTokenRepository.findByTokenHashAndRevocadoFalse(hashear(tokenCrudo))
                .ifPresent(rt -> rt.setRevocado(true));
    }

    private static String generarTokenCrudo() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashear(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
