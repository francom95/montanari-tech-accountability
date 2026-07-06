package com.montanaritech.contable.common.asiento;

/**
 * Numeración interna compartida por todas las líneas de un mismo asiento
 * (F1.1). La implementación real (F3.1) la respalda una tabla/secuencia en
 * base con lock para concurrencia; {@link NumeradorAsientoEnMemoria} es un
 * placeholder solo para que este molde compile y se pueda probar.
 */
public interface NumeradorAsiento {

    Long siguienteNumero();
}
