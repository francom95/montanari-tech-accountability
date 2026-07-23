package com.montanaritech.contable.impuestos.iva;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Resultado de una liquidación de IVA resuelto en las dos etapas del art. 24 de
 * la Ley 23.349 (F6.1 §1.3).
 *
 * <p>Vive aparte del servicio porque lo necesitan dos caminos distintos: la
 * liquidación persistida (con sus ajustes manuales) y la previsualización de un
 * período todavía no liquidado. Duplicar el cálculo en los dos lados sería la
 * forma más fácil de que se desincronicen.
 *
 * <p>Las tres cifras no son mutuamente excluyentes de a pares: {@code saldoTecnico}
 * y {@code saldoLibreDisponibilidad} <b>pueden coexistir</b> (un mes con más
 * crédito fiscal que débito y además percepciones sufridas). Lo que sí es
 * excluyente es {@code saldoAPagar} contra los otros dos.
 */
public record ResultadoIva(
        BigDecimal saldoAPagar,
        BigDecimal saldoTecnico,
        BigDecimal saldoLibreDisponibilidad
) {

    /**
     * @param componentes cualquier colección de componentes
     * @param etapaDe     cómo obtener la etapa de un componente
     * @param aporteDe    cómo obtener su aporte ya con signo
     */
    public static <T> ResultadoIva calcular(List<T> componentes,
                                            Function<T, TipoComponenteIva.Etapa> etapaDe,
                                            Function<T, BigDecimal> aporteDe) {
        BigDecimal tecnica = sumar(componentes, etapaDe, aporteDe, TipoComponenteIva.Etapa.TECNICA);
        BigDecimal directos = sumar(componentes, etapaDe, aporteDe, TipoComponenteIva.Etapa.INGRESOS_DIRECTOS);

        // Etapa 1: lo que sobra de crédito fiscal queda cautivo como saldo técnico.
        BigDecimal impuestoDeterminado = tecnica.max(BigDecimal.ZERO);
        BigDecimal saldoTecnico = tecnica.min(BigDecimal.ZERO).negate();

        // Etapa 2: los aportes de ingresos directos ya vienen negativos (restan).
        BigDecimal neto = impuestoDeterminado.add(directos);

        return new ResultadoIva(
                escalar(neto.max(BigDecimal.ZERO)),
                escalar(saldoTecnico),
                escalar(neto.min(BigDecimal.ZERO).negate()));
    }

    private static <T> BigDecimal sumar(List<T> componentes,
                                        Function<T, TipoComponenteIva.Etapa> etapaDe,
                                        Function<T, BigDecimal> aporteDe,
                                        TipoComponenteIva.Etapa etapa) {
        return componentes.stream()
                .filter(c -> etapaDe.apply(c) == etapa)
                .map(aporteDe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Scale fijo en 2: evita que un {@code BigDecimal} sin escalar salga en notación científica al serializar. */
    private static BigDecimal escalar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
