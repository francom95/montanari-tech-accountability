package com.montanaritech.contable.facturacion.cuentasporcobrar;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.facturacion.cobro.AplicacionAnticipoClienteRepository;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarResponse;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.TotalPorMonedaResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas por cobrar (F4.5, molde PL-3 sobre el de referencia de F3.6
 * "Mayor"): no recalcula saldos — suma en bloque {@code CobroImputacion}/
 * {@code AplicacionAnticipoCliente} (los mismos repositorios de F4.4,
 * agregados por N facturas en una sola consulta cada uno para no repetir
 * el N+1 que tendría llamar a {@code CobroService.saldoFacturaVenta} una
 * vez por fila). Solo muestra facturas con saldo pendiente (saldo > 0):
 * "cuentas por cobrar" son las que todavía se deben, no el historial
 * completo de facturación (eso ya lo cubre F4.2).
 */
@Service
@RequiredArgsConstructor
public class CuentaPorCobrarService {

    private record Importe(BigDecimal original, BigDecimal ars) {
        Importe sumar(Importe otro) {
            return new Importe(original.add(otro.original), ars.add(otro.ars));
        }
    }

    private final FacturaVentaRepository facturaVentaRepo;
    private final CobroImputacionRepository cobroImputacionRepo;
    private final AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;

    @Transactional(readOnly = true)
    public CuentaPorCobrarResponse calcular(Long clienteId, Long proyectoId, Long monedaId,
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoVencimiento estadoVencimiento) {
        List<FacturaVenta> facturas = facturaVentaRepo.buscarConfirmadasParaReporte(clienteId, proyectoId, monedaId, fechaDesde, fechaHasta);
        if (facturas.isEmpty()) {
            return new CuentaPorCobrarResponse(List.of(), List.of());
        }

        List<Long> ids = facturas.stream().map(FacturaVenta::getId).toList();
        Map<Long, Importe> imputadoPorFactura = new HashMap<>();
        for (ImputadoFacturaVenta i : cobroImputacionRepo.sumarImputadoPorFactura(ids, EstadoDocumento.CONFIRMADO)) {
            imputadoPorFactura.merge(i.facturaVentaId(), new Importe(i.imputado(), i.imputadoArs()), Importe::sumar);
        }
        for (ImputadoFacturaVenta i : aplicacionAnticipoRepo.sumarAplicacionesPorFactura(ids)) {
            imputadoPorFactura.merge(i.facturaVentaId(), new Importe(i.imputado(), i.imputadoArs()), Importe::sumar);
        }

        LocalDate hoy = LocalDate.now();
        List<CuentaPorCobrarFilaResponse> filas = new ArrayList<>();
        Map<Long, Importe> totalesPorMoneda = new LinkedHashMap<>();
        Map<Long, String> codigoPorMoneda = new HashMap<>();

        for (FacturaVenta f : facturas) {
            Importe imputado = imputadoPorFactura.getOrDefault(f.getId(), new Importe(BigDecimal.ZERO, BigDecimal.ZERO));
            BigDecimal saldo = f.getTotal().subtract(imputado.original());
            BigDecimal saldoArs = f.getTotalArs().subtract(imputado.ars());
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            EstadoVencimiento estado = f.getFechaVencimiento() == null ? EstadoVencimiento.SIN_VENCIMIENTO
                    : f.getFechaVencimiento().isBefore(hoy) ? EstadoVencimiento.VENCIDO : EstadoVencimiento.POR_VENCER;
            if (estadoVencimiento != null && estado != estadoVencimiento) {
                continue;
            }

            filas.add(new CuentaPorCobrarFilaResponse(
                    f.getId(), f.getCliente().getId(), f.getCliente().getNombre(),
                    f.getProyecto() != null ? f.getProyecto().getId() : null,
                    f.getProyecto() != null ? f.getProyecto().getNombre() : null,
                    f.getNumero(), f.getFecha(), f.getFechaVencimiento(),
                    f.getMoneda().getId(), f.getMoneda().getCodigo(),
                    f.getTotal(), f.getTotalArs(), saldo, saldoArs, estado.name()));

            Long monId = f.getMoneda().getId();
            codigoPorMoneda.put(monId, f.getMoneda().getCodigo());
            totalesPorMoneda.merge(monId, new Importe(saldo, saldoArs), Importe::sumar);
        }

        List<TotalPorMonedaResponse> totales = totalesPorMoneda.entrySet().stream()
                .map(e -> new TotalPorMonedaResponse(e.getKey(), codigoPorMoneda.get(e.getKey()), e.getValue().original(), e.getValue().ars()))
                .toList();

        return new CuentaPorCobrarResponse(filas, totales);
    }
}
