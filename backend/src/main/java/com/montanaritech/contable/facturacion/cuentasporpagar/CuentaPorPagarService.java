package com.montanaritech.contable.facturacion.cuentasporpagar;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.pago.AplicacionAnticipoProveedorRepository;
import com.montanaritech.contable.facturacion.pago.PagoImputacionRepository;
import com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.TotalPorMonedaResponse;
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

/** Cuentas por pagar (F4.5), simétrico a {@code CuentaPorCobrarService}. */
@Service
@RequiredArgsConstructor
public class CuentaPorPagarService {

    private record Importe(BigDecimal original, BigDecimal ars) {
        Importe sumar(Importe otro) {
            return new Importe(original.add(otro.original), ars.add(otro.ars));
        }
    }

    private final FacturaCompraRepository facturaCompraRepo;
    private final PagoImputacionRepository pagoImputacionRepo;
    private final AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;

    @Transactional(readOnly = true)
    public CuentaPorPagarResponse calcular(Long proveedorId, Long proyectoId, Long monedaId,
            LocalDate fechaDesde, LocalDate fechaHasta, EstadoVencimiento estadoVencimiento) {
        List<FacturaCompra> facturas = facturaCompraRepo.buscarConfirmadasParaReporte(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta);
        if (facturas.isEmpty()) {
            return new CuentaPorPagarResponse(List.of(), List.of());
        }

        List<Long> ids = facturas.stream().map(FacturaCompra::getId).toList();
        Map<Long, Importe> imputadoPorFactura = new HashMap<>();
        for (ImputadoFacturaCompra i : pagoImputacionRepo.sumarImputadoPorFactura(ids, EstadoDocumento.CONFIRMADO)) {
            imputadoPorFactura.merge(i.facturaCompraId(), new Importe(i.imputado(), i.imputadoArs()), Importe::sumar);
        }
        for (ImputadoFacturaCompra i : aplicacionAnticipoRepo.sumarAplicacionesPorFactura(ids)) {
            imputadoPorFactura.merge(i.facturaCompraId(), new Importe(i.imputado(), i.imputadoArs()), Importe::sumar);
        }

        LocalDate hoy = LocalDate.now();
        List<CuentaPorPagarFilaResponse> filas = new ArrayList<>();
        Map<Long, Importe> totalesPorMoneda = new LinkedHashMap<>();
        Map<Long, String> codigoPorMoneda = new HashMap<>();

        for (FacturaCompra f : facturas) {
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

            filas.add(new CuentaPorPagarFilaResponse(
                    f.getId(), f.getProveedor().getId(), f.getProveedor().getNombre(),
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

        return new CuentaPorPagarResponse(filas, totales);
    }
}
