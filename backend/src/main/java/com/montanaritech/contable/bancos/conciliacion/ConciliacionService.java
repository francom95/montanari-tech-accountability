package com.montanaritech.contable.bancos.conciliacion;

import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionMovimientoResponse;
import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import com.montanaritech.contable.bancos.conciliacion.dto.CuentaSugeridaResponse;
import com.montanaritech.contable.bancos.conciliacion.dto.MatchSugeridoResponse;
import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.mayor.MayorService;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conciliación bancaria (F5.3): compara movimientos bancarios importados
 * (F5.1/F5.2) contra el sistema (asientos ya confirmados — Cobro, Pago,
 * u otro movimiento bancario ya resuelto), sugiere matches por importe
 * exacto + fecha con tolerancia configurable, y ofrece imputación rápida
 * para lo que no matchea. Nunca resuelve nada por sí mismo — el usuario
 * siempre decide (regla de negocio explícita del paso): esta capa solo
 * lee y sugiere; las acciones (confirmar/imputar/asociar/descartar) siguen
 * siendo las de {@code MovimientoBancarioService} (F5.1).
 */
@Service
@RequiredArgsConstructor
public class ConciliacionService {

    private final MovimientoBancarioRepository movimientoRepo;
    private final AsientoLineaRepository asientoLineaRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final MayorService mayorService;
    private final ClasificadorMovimientoBancario clasificador;

    @Transactional(readOnly = true)
    public ConciliacionResumenResponse resumen(Long cuentaBancariaId, LocalDate fechaDesde, LocalDate fechaHasta, int toleranciaDias) {
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new NegocioException("RANGO_FECHAS_INVALIDO", "La fecha desde no puede ser posterior a la fecha hasta");
        }
        CuentaBancaria cuenta = cuentaBancariaRepo.findById(cuentaBancariaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + cuentaBancariaId + " no encontrada"));

        List<MovimientoBancario> movimientos = movimientoRepo.buscarParaConciliacion(cuentaBancariaId, fechaDesde, fechaHasta);

        List<AsientoLinea> candidatos = new ArrayList<>(asientoLineaRepo.buscarCandidatosConciliacion(
                cuentaBancariaId, fechaDesde.minusDays(toleranciaDias), fechaHasta.plusDays(toleranciaDias), EstadoDocumento.CONFIRMADO));

        List<ConciliacionMovimientoResponse> filas = new ArrayList<>();
        for (MovimientoBancario m : movimientos) {
            MatchSugeridoResponse match = null;
            CuentaSugeridaResponse sugerida = null;
            if (m.getEstado() == EstadoMovimientoBancario.PENDIENTE) {
                AsientoLinea candidato = buscarYQuitarCandidato(candidatos, m, toleranciaDias);
                if (candidato != null) {
                    match = new MatchSugeridoResponse(candidato.getAsiento().getId(), candidato.getAsiento().getNumero(),
                            candidato.getAsiento().getFecha(), candidato.getAsiento().getOrigenTipo(),
                            candidato.getAsiento().getOrigenId(), candidato.getAsiento().getDescripcion());
                } else {
                    sugerida = clasificador.clasificar(m.getDescripcion(), m.getOrigenImportacion())
                            .map(cs -> new CuentaSugeridaResponse(cs.cuenta().getId(), cs.cuenta().getCodigo(),
                                    cs.cuenta().getNombre(), cs.concepto().name()))
                            .orElse(null);
                }
            }
            filas.add(new ConciliacionMovimientoResponse(m.getId(), m.getFecha(), m.getDescripcion(), m.getImporte(),
                    m.getMoneda().getCodigo(), m.getEstado().name(), match, sugerida,
                    m.getAsiento() != null ? m.getAsiento().getId() : null,
                    m.getAsiento() != null ? m.getAsiento().getNumero() : null));
        }

        BigDecimal saldoBanco = calcularSaldoBanco(cuenta, fechaHasta);
        BigDecimal saldoSistema = mayorService.calcular(cuenta.getCuentaContable().getId(), null, null, null, null,
                null, null, null, fechaHasta).saldoFinal();

        return new ConciliacionResumenResponse(cuenta.getId(), cuenta.getAlias(), cuenta.getMoneda().getCodigo(),
                fechaDesde, fechaHasta, saldoBanco, saldoSistema, saldoBanco.subtract(saldoSistema), filas);
    }

    /** Saldo inicial + todo movimiento no descartado hasta fechaHasta (con o sin fecha propia todavía, F5.2). */
    private BigDecimal calcularSaldoBanco(CuentaBancaria cuenta, LocalDate fechaHasta) {
        BigDecimal saldo = cuenta.getSaldoInicial();
        for (MovimientoBancario m : movimientoRepo.buscarParaConciliacion(cuenta.getId(), cuenta.getFechaSaldoInicial(), fechaHasta)) {
            if (m.getEstado() != EstadoMovimientoBancario.DESCARTADO) {
                saldo = saldo.add(m.getImporte());
            }
        }
        return saldo;
    }

    /** Primer candidato que matchea por importe (ARS) exacto y fecha dentro de la tolerancia; lo remueve del pool para no re-sugerirlo. */
    private AsientoLinea buscarYQuitarCandidato(List<AsientoLinea> candidatos, MovimientoBancario m, int toleranciaDias) {
        if (m.getFecha() == null) {
            return null;
        }
        for (AsientoLinea linea : candidatos) {
            BigDecimal netoLinea = linea.getDebe().subtract(linea.getHaber());
            boolean mismoImporte = netoLinea.compareTo(m.getImporteArs()) == 0;
            boolean dentroDeTolerancia = Math.abs(ChronoUnit.DAYS.between(m.getFecha(), linea.getAsiento().getFecha())) <= toleranciaDias;
            if (mismoImporte && dentroDeTolerancia) {
                candidatos.remove(linea);
                return linea;
            }
        }
        return null;
    }
}
