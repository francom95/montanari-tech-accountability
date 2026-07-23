package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;

/**
 * Componentes de la liquidación mensual de IVA (F6.1 §1.2), con la composición
 * corregida tras la validación del contador.
 *
 * <p>Cada componente declara tres cosas: en qué <b>etapa</b> del cálculo entra
 * (§1.3), con qué <b>signo</b> aporta, y —los automáticos— de qué <b>lado</b> de
 * qué cuenta se lee.
 *
 * <p>El lado importa y no es un detalle de implementación: en la cuenta de
 * débito fiscal, el haber son las ventas y el <b>debe son las notas de crédito
 * emitidas</b>, cuyo IVA no reduce el débito fiscal sino que <b>aumenta el
 * crédito fiscal</b> (art. 12 inc. b, Ley 23.349). Simétricamente, el haber de
 * la cuenta de crédito fiscal son las notas de crédito recibidas, que aumentan
 * el débito fiscal (art. 11, último párrafo). Netear por naturaleza de cuenta
 * —como hacía la primera versión— da el mismo total pero compone mal el saldo,
 * y con las dos especies del art. 24 separadas esa composición sí cambia los
 * números.
 */
public enum TipoComponenteIva {

    DEBITO_FISCAL(1, Etapa.TECNICA, ConceptoContable.IVA_DEBITO_FISCAL, Lado.HABER,
            "Débito fiscal"),
    /**
     * IVA de las notas de crédito <b>emitidas</b>. Se computa como crédito
     * fiscal, no como menor débito (art. 12 inc. b): por eso resta, y por eso
     * es un renglón propio y no un neteo silencioso dentro del débito fiscal.
     */
    RESTITUCION_CREDITO_FISCAL(-1, Etapa.TECNICA, ConceptoContable.IVA_DEBITO_FISCAL, Lado.DEBE,
            "Restitución de crédito fiscal (notas de crédito emitidas)"),
    CREDITO_FISCAL(-1, Etapa.TECNICA, ConceptoContable.IVA_CREDITO_FISCAL, Lado.DEBE,
            "Crédito fiscal"),
    /**
     * IVA de las notas de crédito <b>recibidas</b>: restituye crédito fiscal ya
     * computado, así que aumenta el débito fiscal (art. 11, último párrafo).
     */
    RESTITUCION_DEBITO_FISCAL(1, Etapa.TECNICA, ConceptoContable.IVA_CREDITO_FISCAL, Lado.HABER,
            "Restitución de débito fiscal (notas de crédito recibidas)"),
    /** Saldo técnico del mes anterior: solo computable contra débitos fiscales (art. 24, 1er párrafo). */
    SALDO_TECNICO_ANTERIOR(-1, Etapa.TECNICA, ConceptoContable.IVA_SALDO_A_FAVOR, null,
            "Saldo técnico del período anterior"),

    /**
     * Percepciones y retenciones de IVA sufridas. Un único componente porque
     * {@code PERCEPCION_IVA_SUFRIDA} y {@code RETENCION_IVA_SUFRIDA} mapean a la
     * misma cuenta (1.1.2007) desde F4.3, así que una sola consulta captura las
     * percepciones de compras, las retenciones sufridas en cobros y las
     * percepciones bancarias imputadas en la conciliación de F5.3. Son "ingresos
     * directos": entran en la segunda etapa y lo que sobra es libre disponibilidad.
     */
    PERCEPCIONES(-1, Etapa.INGRESOS_DIRECTOS, ConceptoContable.PERCEPCION_IVA_SUFRIDA, null,
            "Percepciones y retenciones de IVA sufridas"),
    /** Libre disponibilidad acumulada del mes anterior (art. 24, 2do párrafo). */
    SALDO_LIBRE_DISPONIBILIDAD_ANTERIOR(-1, Etapa.INGRESOS_DIRECTOS,
            ConceptoContable.IVA_SALDO_LIBRE_DISPONIBILIDAD, null,
            "Saldo de libre disponibilidad del período anterior"),

    /** Ajuste manual sobre la etapa técnica. */
    OTRO_TECNICO(1, Etapa.TECNICA, null, null, "Otro concepto (técnico)"),
    /** Ajuste manual sobre ingresos directos (p. ej. una percepción no registrada). */
    OTRO_INGRESO_DIRECTO(-1, Etapa.INGRESOS_DIRECTOS, null, null, "Otro ingreso directo");

    /**
     * Las dos etapas del art. 24. La técnica determina el impuesto del período
     * contra los créditos propios del IVA; la de ingresos directos aplica lo
     * retenido y percibido. El sobrante de cada una es un saldo a favor de
     * especie distinta, y por eso no se pueden sumar en una sola cuenta.
     */
    public enum Etapa {
        TECNICA,
        INGRESOS_DIRECTOS
    }

    /** De qué lado de la cuenta se lee el componente ({@code null} = neto debe−haber). */
    public enum Lado {
        DEBE,
        HABER
    }

    private final int signo;
    private final Etapa etapa;
    private final ConceptoContable concepto;
    private final Lado lado;
    private final String descripcionPorDefecto;

    TipoComponenteIva(int signo, Etapa etapa, ConceptoContable concepto, Lado lado, String descripcionPorDefecto) {
        this.signo = signo;
        this.etapa = etapa;
        this.concepto = concepto;
        this.lado = lado;
        this.descripcionPorDefecto = descripcionPorDefecto;
    }

    public int getSigno() {
        return signo;
    }

    public Etapa getEtapa() {
        return etapa;
    }

    /** {@code null} en los componentes manuales, que traen su cuenta propia. */
    public ConceptoContable getConcepto() {
        return concepto;
    }

    public Lado getLado() {
        return lado;
    }

    public String getDescripcionPorDefecto() {
        return descripcionPorDefecto;
    }

    /**
     * Los que se leen de los asientos del período. Los arrastres también tienen
     * concepto (su cuenta espejo, que usa el asiento), pero su importe sale de la
     * liquidación anterior, no de los movimientos del mes.
     *
     * <p>Ojo con la condición: {@code lado} es {@code null} en los componentes
     * que se leen por el neto de la cuenta —como {@link #PERCEPCIONES}—, así que
     * exigirlo acá los dejaría fuera del cálculo.
     */
    public boolean seCalculaDesdeAsientos() {
        return concepto != null && !esArrastre();
    }

    public boolean esArrastre() {
        return this == SALDO_TECNICO_ANTERIOR || this == SALDO_LIBRE_DISPONIBILIDAD_ANTERIOR;
    }

    /** Los que el motor recalcula solo; el resto los carga el usuario. */
    public boolean esAutomatico() {
        return concepto != null;
    }
}
