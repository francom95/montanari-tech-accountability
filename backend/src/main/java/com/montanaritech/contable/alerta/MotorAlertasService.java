package com.montanaritech.contable.alerta;

import com.montanaritech.contable.bancos.conciliacion.ConciliacionService;
import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.facturacion.cuentasporcobrar.CuentaPorCobrarService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.pendiente.PendienteAdministrativo;
import com.montanaritech.contable.pendiente.PendienteAdministrativoService;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de sincronización de alertas (F9.1): para cada uno de los 9
 * {@link TipoAlerta} calcula el conjunto de condiciones vigentes ahora mismo
 * y lo compara contra las {@link Alerta} ya persistidas de ese tipo —
 * crea las nuevas, reactiva/actualiza las que siguen vigentes, y auto-resuelve
 * las que ya no aplican (decisión F9.1: la resolución es automática, no
 * depende de que nadie la lea). Se invoca una vez por tenant desde el job
 * {@code AlertaScheduler}, con el filtro Hibernate de tenant ya habilitado
 * por el caller.
 */
@Service
@RequiredArgsConstructor
public class MotorAlertasService {

    private record DatosAlerta(String mensaje, LocalDate fecha, SeveridadAlerta severidad) {}

    private final AlertaRepository alertaRepo;
    private final ConfiguracionAlertasRepository configuracionRepo;
    private final VencimientoService vencimientoService;
    private final CompromisoService compromisoService;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final RecalculoSaldoService recalculoSaldoService;
    private final MovimientoBancarioRepository movimientoBancarioRepo;
    private final ConciliacionService conciliacionService;
    private final PendienteAdministrativoService pendienteAdministrativoService;
    private final AlertChannel alertChannel;

    /** Corre las 9 sincronizaciones sobre el tenant ya habilitado en la sesión actual (ver {@code AlertaScheduler}). */
    @Transactional
    public void sincronizar() {
        ConfiguracionAlertas config = configuracionRepo.findFirstByOrderByIdAsc().orElseGet(ConfiguracionAlertas::new);
        Integer dias = config.getDiasAnticipacion();
        Integer diasAtrasoCxc = config.getDiasAtrasoCxc();

        sincronizarTipo(TipoAlerta.VENCIMIENTO_PROXIMO, TipoEntidadAlerta.VENCIMIENTO, vencimientoProximoVigentes(dias));
        sincronizarTipo(TipoAlerta.VENCIMIENTO_VENCIDO, TipoEntidadAlerta.VENCIMIENTO, vencimientoVencidoVigentes());
        sincronizarTipo(TipoAlerta.COMPROMISO_PROXIMO, TipoEntidadAlerta.COMPROMISO, compromisoProximoVigentes(dias));
        sincronizarTipo(TipoAlerta.CXP_PROXIMO, TipoEntidadAlerta.FACTURA_COMPRA, cxpProximoVigentes(dias));
        sincronizarTipo(TipoAlerta.CXC_ATRASADA, TipoEntidadAlerta.FACTURA_VENTA, cxcAtrasadaVigentes(diasAtrasoCxc));
        sincronizarTipo(TipoAlerta.SALDO_BAJO, TipoEntidadAlerta.CUENTA_BANCARIA, saldoBajoVigentes());
        sincronizarTipo(TipoAlerta.MOVIMIENTO_BANCARIO_PENDIENTE, TipoEntidadAlerta.MOVIMIENTO_BANCARIO, movimientoBancarioPendienteVigentes());
        sincronizarTipo(TipoAlerta.CONCILIACION_DIFERENCIA, TipoEntidadAlerta.CUENTA_BANCARIA, conciliacionDiferenciaVigentes());
        sincronizarTipo(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO, TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO, pendienteAdministrativoProximoVigentes(dias));
    }

    private void sincronizarTipo(TipoAlerta tipo, TipoEntidadAlerta entidadTipo, Map<Long, DatosAlerta> vigentes) {
        for (Map.Entry<Long, DatosAlerta> entry : vigentes.entrySet()) {
            Long entidadRefId = entry.getKey();
            DatosAlerta datos = entry.getValue();
            var existente = alertaRepo.findByTipoAndEntidadTipoAndEntidadRefId(tipo, entidadTipo, entidadRefId);
            if (existente.isEmpty()) {
                Alerta a = new Alerta();
                a.setTipo(tipo);
                a.setEntidadTipo(entidadTipo);
                a.setEntidadRefId(entidadRefId);
                a.setMensaje(datos.mensaje());
                a.setFecha(datos.fecha());
                a.setSeveridad(datos.severidad());
                a.setEstado(EstadoAlerta.ACTIVA);
                Alerta guardada = alertaRepo.save(a);
                alertChannel.notificar(guardada);
            } else {
                Alerta a = existente.get();
                a.setMensaje(datos.mensaje());
                a.setFecha(datos.fecha());
                a.setSeveridad(datos.severidad());
                if (a.getEstado() == EstadoAlerta.RESUELTA) {
                    a.setEstado(EstadoAlerta.ACTIVA);
                    a.setResueltaEn(null);
                }
            }
        }
        for (Alerta activa : alertaRepo.findByTipoAndEstado(tipo, EstadoAlerta.ACTIVA)) {
            if (!vigentes.containsKey(activa.getEntidadRefId())) {
                activa.setEstado(EstadoAlerta.RESUELTA);
                activa.setResueltaEn(Instant.now());
            }
        }
    }

    private Map<Long, DatosAlerta> vencimientoProximoVigentes(int dias) {
        LocalDate hoy = LocalDate.now();
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (Vencimiento v : vencimientoService.proximos(dias)) {
            if (v.getFecha().isBefore(hoy)) {
                continue; // ya vencido: lo cubre VENCIMIENTO_VENCIDO, no lo duplicamos acá
            }
            vigentes.put(v.getId(), new DatosAlerta(
                    v.getDescripcion() + " vence el " + v.getFecha(), v.getFecha(), SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> vencimientoVencidoVigentes() {
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (Vencimiento v : vencimientoService.vencidos()) {
            vigentes.put(v.getId(), new DatosAlerta(
                    v.getDescripcion() + " está vencido desde el " + v.getFecha(), v.getFecha(), SeveridadAlerta.CRITICA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> compromisoProximoVigentes(int dias) {
        LocalDate hoy = LocalDate.now();
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (Compromiso c : compromisoService.porRangoDeFechas(hoy, hoy.plusDays(dias))) {
            if (c.getEstado() != EstadoCompromiso.PENDIENTE) {
                continue;
            }
            vigentes.put(c.getId(), new DatosAlerta(
                    c.getConcepto() + " previsto para el " + c.getFechaPrevista(), c.getFechaPrevista(), SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> cxpProximoVigentes(int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (CuentaPorPagarFilaResponse f : cuentaPorPagarService.calcular(
                null, null, null, null, null, EstadoVencimiento.POR_VENCER).filas()) {
            if (f.fechaVencimiento() == null || f.fechaVencimiento().isAfter(limite)) {
                continue;
            }
            vigentes.put(f.facturaCompraId(), new DatosAlerta(
                    "Factura " + f.numero() + " a " + f.proveedorNombre() + " vence el " + f.fechaVencimiento(),
                    f.fechaVencimiento(), SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> cxcAtrasadaVigentes(int diasAtraso) {
        LocalDate limite = LocalDate.now().minusDays(diasAtraso);
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (CuentaPorCobrarFilaResponse f : cuentaPorCobrarService.calcular(
                null, null, null, null, null, EstadoVencimiento.VENCIDO).filas()) {
            if (f.fechaVencimiento() == null || !f.fechaVencimiento().isBefore(limite)) {
                continue;
            }
            vigentes.put(f.facturaVentaId(), new DatosAlerta(
                    "Factura " + f.numero() + " de " + f.clienteNombre() + " vencida desde el " + f.fechaVencimiento(),
                    f.fechaVencimiento(), SeveridadAlerta.CRITICA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> saldoBajoVigentes() {
        LocalDate hoy = LocalDate.now();
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (CuentaBancaria cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            BigDecimal minimo = cuenta.getSaldoMinimoAlerta();
            if (minimo == null) {
                continue;
            }
            BigDecimal saldoActual = recalculoSaldoService.recalcularCuentaBancariaHasta(cuenta, hoy);
            if (saldoActual.compareTo(minimo) >= 0) {
                continue;
            }
            vigentes.put(cuenta.getId(), new DatosAlerta(
                    "Saldo de " + cuenta.getAlias() + " ($" + saldoActual + ") por debajo del mínimo ($" + minimo + ")",
                    hoy, SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> movimientoBancarioPendienteVigentes() {
        LocalDate hoy = LocalDate.now();
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (MovimientoBancario m : movimientoBancarioRepo
                .buscar(null, EstadoMovimientoBancario.PENDIENTE, null, null, Pageable.unpaged())) {
            vigentes.put(m.getId(), new DatosAlerta(
                    "Movimiento bancario pendiente de revisar: " + m.getDescripcion(),
                    m.getFecha() != null ? m.getFecha() : hoy, SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    /** Ventana fija (desde el saldo inicial de la cuenta) porque la diferencia no se persiste — se recalcula cada corrida. */
    private Map<Long, DatosAlerta> conciliacionDiferenciaVigentes() {
        LocalDate hoy = LocalDate.now();
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (CuentaBancaria cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            LocalDate desde = cuenta.getFechaSaldoInicial().isBefore(hoy) ? cuenta.getFechaSaldoInicial() : hoy;
            ConciliacionResumenResponse resumen = conciliacionService.resumen(cuenta.getId(), desde, hoy, 3);
            if (resumen.diferencia().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            vigentes.put(cuenta.getId(), new DatosAlerta(
                    "Diferencia de conciliación en " + cuenta.getAlias() + ": $" + resumen.diferencia(),
                    hoy, SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }

    private Map<Long, DatosAlerta> pendienteAdministrativoProximoVigentes(int dias) {
        Map<Long, DatosAlerta> vigentes = new LinkedHashMap<>();
        for (PendienteAdministrativo p : pendienteAdministrativoService.proximosAVencer(dias)) {
            vigentes.put(p.getId(), new DatosAlerta(
                    p.getTitulo() + " vence el " + p.getFechaEstimadaResolucion(),
                    p.getFechaEstimadaResolucion(), SeveridadAlerta.ADVERTENCIA));
        }
        return vigentes;
    }
}
