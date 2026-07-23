package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;

/**
 * Componentes de una sub-liquidación de IIBB por jurisdicción (F6.2 §1.3/§1.4).
 * El {@code signo} es el aporte al resultado de la jurisdicción: las deducciones
 * restan.
 *
 * <p>A diferencia de IVA (F6.1), acá <b>ningún componente se lee de los asientos
 * del período</b>: el impuesto determinado es un campo de la jurisdicción
 * (base × alícuota, no un componente), las deducciones son manuales —su
 * atribución automática a una jurisdicción es ambigua porque la línea de asiento
 * no lleva jurisdicción (§1.3)— y el arrastre sale de la liquidación anterior.
 * Tampoco hay dos etapas: IIBB tiene una sola (no aplica el art. 24 de IVA).
 */
public enum TipoComponenteIibb {

    /** Percepciones de IIBB sufridas. Crédito contra el impuesto (Haber 1.1.2008). */
    PERCEPCIONES(-1, ConceptoContable.PERCEPCION_IIBB_SUFRIDA, "Percepciones de IIBB sufridas"),
    /** Retenciones de IIBB sufridas. Mismo tratamiento que las percepciones. */
    RETENCIONES(-1, ConceptoContable.PERCEPCION_IIBB_SUFRIDA, "Retenciones de IIBB sufridas"),
    /** SIRCREB (recaudación bancaria). Se imputa a 1.1.2008 igual que las percepciones (F5.3). */
    SIRCREB(-1, ConceptoContable.PERCEPCION_IIBB_SUFRIDA, "SIRCREB"),
    /** Pagos a cuenta / anticipos de IIBB del período. */
    PAGOS_A_CUENTA(-1, ConceptoContable.PERCEPCION_IIBB_SUFRIDA, "Pagos a cuenta de IIBB"),
    /** Saldo a favor de esta jurisdicción en la liquidación confirmada del mes anterior (arrastre). */
    SALDO_A_FAVOR_ANTERIOR(-1, ConceptoContable.IIBB_SALDO_A_FAVOR, "Saldo a favor del período anterior"),
    /** Ajuste manual de la jurisdicción (aporta positivo; el usuario puede cargar un importe negativo). */
    OTRO(1, null, "Otro concepto");

    private final int signo;
    private final ConceptoContable concepto;
    private final String descripcionPorDefecto;

    TipoComponenteIibb(int signo, ConceptoContable concepto, String descripcionPorDefecto) {
        this.signo = signo;
        this.concepto = concepto;
        this.descripcionPorDefecto = descripcionPorDefecto;
    }

    public int getSigno() {
        return signo;
    }

    /** {@code null} en {@link #OTRO}, que trae su cuenta propia. */
    public ConceptoContable getConcepto() {
        return concepto;
    }

    public String getDescripcionPorDefecto() {
        return descripcionPorDefecto;
    }

    /** El único que sale de la liquidación anterior; el resto son manuales. */
    public boolean esArrastre() {
        return this == SALDO_A_FAVOR_ANTERIOR;
    }

    /** Los que el sistema precarga al crear el borrador (hoy solo el arrastre). */
    public boolean esAutomatico() {
        return esArrastre();
    }
}
