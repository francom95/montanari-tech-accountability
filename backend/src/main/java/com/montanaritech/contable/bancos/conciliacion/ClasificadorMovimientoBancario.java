package com.montanaritech.contable.bancos.conciliacion;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imputación rápida por descripción (F5.3 §1): para un movimiento bancario
 * {@code PENDIENTE} sin match contra un asiento existente, sugiere una
 * {@link CuentaContable} vía patrones de texto conocidos + {@link ResolutorCuentas}
 * (F4.1) — nunca imputa por sí mismo, solo sugiere (el usuario siempre decide,
 * regla de negocio explícita del paso).
 *
 * <p>SIRCREB y percepciones bancarias no tienen conceptos propios: son la
 * misma percepción de IIBB/IVA "sufrida" que ya resuelven
 * {@code PERCEPCION_IIBB_SUFRIDA}/{@code PERCEPCION_IVA_SUFRIDA} en facturas
 * de compra (F4.3) — reusarlas evita mapeo_cuenta duplicado para el mismo
 * crédito fiscal, venga de una factura o de un extracto bancario.
 */
@Service
@RequiredArgsConstructor
public class ClasificadorMovimientoBancario {

    private record Regla(ConceptoContable concepto, List<String> palabrasClave) {}

    private static final List<Regla> REGLAS = List.of(
            new Regla(ConceptoContable.COMISION_BANCARIA, List.of("COMISION", "COMISIÓN")),
            new Regla(ConceptoContable.IMPUESTO_DEBITOS_CREDITOS_BANCARIOS, List.of("LEY 25413", "25413")),
            new Regla(ConceptoContable.PERCEPCION_IIBB_SUFRIDA, List.of("SIRCREB", "ING. BRUTOS", "INGRESOS BRUTOS", "IIBB")),
            new Regla(ConceptoContable.PERCEPCION_IVA_SUFRIDA, List.of("PERCEP. IVA", "PERCEPCION IVA", "PERCEP.IVA", "PERCEP. I.V.A"))
    );

    private final ResolutorCuentas resolutorCuentas;

    public record CuentaSugerida(CuentaContable cuenta, ConceptoContable concepto) {}

    /**
     * @return vacío si ninguna regla matchea, o si matchea pero no hay
     *         mapeo_cuenta configurado para ese concepto (F4.1: sin mapeo no
     *         se puede resolver una cuenta, no es un error de este paso).
     */
    @Transactional(readOnly = true)
    public Optional<CuentaSugerida> clasificar(String descripcion, OrigenImportacionMovimiento origen) {
        if (descripcion == null) {
            return Optional.empty();
        }
        String normalizada = descripcion.toUpperCase(Locale.ROOT);
        for (Regla regla : REGLAS) {
            if (regla.palabrasClave().stream().anyMatch(normalizada::contains)) {
                try {
                    CuentaContable cuenta = origen != null
                            ? resolutorCuentas.resolver(regla.concepto(), "ORIGEN_IMPORTACION", origen.name())
                            : resolutorCuentas.resolver(regla.concepto());
                    return Optional.of(new CuentaSugerida(cuenta, regla.concepto()));
                } catch (NegocioException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
