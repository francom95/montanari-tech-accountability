package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;

/**
 * Componentes de la liquidación mensual de IVA (F6.1 §1.2). El {@code signo}
 * es el aporte del componente al resultado: positivo suma a lo que hay que
 * pagar, negativo lo reduce.
 *
 * <p>Los cuatro primeros se calculan automáticamente desde los asientos
 * confirmados del período (leyendo la cuenta que resuelve su {@code concepto},
 * no un código hardcodeado). {@link #RESTITUCIONES} y {@link #OTRO} son
 * exclusivamente manuales y por eso no tienen concepto asociado: el usuario
 * indica la cuenta contable en el propio componente.
 */
public enum TipoComponenteIva {

    DEBITO_FISCAL(1, ConceptoContable.IVA_DEBITO_FISCAL, "Débito fiscal"),
    CREDITO_FISCAL(-1, ConceptoContable.IVA_CREDITO_FISCAL, "Crédito fiscal"),
    /**
     * Percepciones y retenciones de IVA sufridas. Un único componente porque
     * {@code PERCEPCION_IVA_SUFRIDA} y {@code RETENCION_IVA_SUFRIDA} mapean a
     * la misma cuenta (1.1.2007) desde F4.3 — así una sola consulta captura las
     * percepciones de compras, las retenciones sufridas en cobros y las
     * percepciones bancarias imputadas en la conciliación de F5.3.
     */
    PERCEPCIONES(-1, ConceptoContable.PERCEPCION_IVA_SUFRIDA, "Percepciones y retenciones de IVA sufridas"),
    /** Saldo a favor de la liquidación confirmada del mes anterior (arrastre automático). */
    SALDO_TECNICO_ANTERIOR(-1, ConceptoContable.IVA_SALDO_A_FAVOR, "Saldo técnico del período anterior"),
    /**
     * Sin cálculo automático a propósito (F6.1 §1.5): no hay en el sistema
     * ninguna fuente que identifique una restitución, y su tratamiento
     * normativo depende del caso concreto. Pendiente de validar con el contador.
     */
    RESTITUCIONES(1, null, "Restituciones"),
    OTRO(1, null, "Otro concepto");

    private final int signo;
    private final ConceptoContable concepto;
    private final String descripcionPorDefecto;

    TipoComponenteIva(int signo, ConceptoContable concepto, String descripcionPorDefecto) {
        this.signo = signo;
        this.concepto = concepto;
        this.descripcionPorDefecto = descripcionPorDefecto;
    }

    public int getSigno() {
        return signo;
    }

    /** {@code null} en los componentes manuales, que traen su cuenta propia. */
    public ConceptoContable getConcepto() {
        return concepto;
    }

    public String getDescripcionPorDefecto() {
        return descripcionPorDefecto;
    }

    /** Los que el motor recalcula desde los asientos; el resto los carga el usuario. */
    public boolean esAutomatico() {
        return this == DEBITO_FISCAL || this == CREDITO_FISCAL || this == PERCEPCIONES || this == SALDO_TECNICO_ANTERIOR;
    }
}
