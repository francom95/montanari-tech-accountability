package com.montanaritech.contable.contabilidad.mapeocuenta;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve un {@link ConceptoContable} a la {@link CuentaContable} a usar en
 * un asiento automático (F4.1 §1.2). Algoritmo:
 * <ol>
 *   <li>Fila específica: concepto + discriminador que matchea el contexto.</li>
 *   <li>Fila por defecto: concepto con discriminador NULL.</li>
 *   <li>Si no hay ninguna → {@code MAPEO_CUENTA_FALTANTE} (422). El documento
 *       ya se pudo guardar como borrador; esto solo bloquea la
 *       confirmación (F3.1: sin datos completos no se confirma).</li>
 * </ol>
 * La cuenta resuelta se valida imputable y activa antes de devolverla — el
 * generador no repite esa validación (la vuelve a chequear igual
 * {@code AsientoService} al confirmar, en defensa de profundidad).
 */
@Service
@RequiredArgsConstructor
public class ResolutorCuentas {

    private final MapeoCuentaRepository repo;

    @Transactional(readOnly = true)
    public CuentaContable resolver(ConceptoContable concepto, String discriminadorTipo, String discriminadorValor) {
        MapeoCuenta mapeo = (discriminadorTipo != null
                ? repo.findByConceptoAndDiscriminadorTipoAndDiscriminadorValorAndActivoTrue(concepto, discriminadorTipo, discriminadorValor)
                : java.util.Optional.<MapeoCuenta>empty())
                .or(() -> repo.findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(concepto))
                .orElseThrow(() -> new NegocioException("MAPEO_CUENTA_FALTANTE",
                        "No hay una cuenta configurada para el concepto %s%s".formatted(
                                concepto, discriminadorValor != null ? " (" + discriminadorTipo + "=" + discriminadorValor + ")" : "")));

        CuentaContable cuenta = mapeo.getCuentaContable();
        if (!cuenta.isImputable()) {
            throw new NegocioException("CUENTA_NO_IMPUTABLE",
                    "La cuenta mapeada para %s (%s) es una cuenta madre".formatted(concepto, cuenta.getCodigo()));
        }
        if (!cuenta.isActivo()) {
            throw new NegocioException("CUENTA_INACTIVA",
                    "La cuenta mapeada para %s (%s) está inactiva".formatted(concepto, cuenta.getCodigo()));
        }
        return cuenta;
    }

    /** Concepto sin discriminador (siempre resuelve por la fila por defecto). */
    @Transactional(readOnly = true)
    public CuentaContable resolver(ConceptoContable concepto) {
        return resolver(concepto, null, null);
    }
}
