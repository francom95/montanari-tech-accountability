package com.montanaritech.contable.common.asiento;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Placeholder en memoria (no persiste, no es seguro entre instancias). F3.1
 * lo reemplaza por una secuencia real en base antes de que exista ningún
 * asiento de verdad — no usar esta implementación pasado ese punto.
 */
@Component
public class NumeradorAsientoEnMemoria implements NumeradorAsiento {

    private final AtomicLong contador = new AtomicLong(0);

    @Override
    public Long siguienteNumero() {
        return contador.incrementAndGet();
    }
}
