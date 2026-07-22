package com.montanaritech.contable.facturacion.importacion;

import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraService;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraCrearRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraLineaRequest;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaService;
import com.montanaritech.contable.facturacion.facturaventa.TipoIngreso;
import com.montanaritech.contable.facturacion.facturaventa.TipoLineaFactura;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaCrearRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaLineaRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cliente.ClienteService;
import com.montanaritech.contable.maestros.cliente.dto.ClienteCrearRequest;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorService;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorCrearRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestación del importador de facturas históricas en PDF (F4.6).
 *
 * <p><b>Importante:</b> {@link #confirmar} procesa cada fila con su propia
 * transacción independiente (delegando en {@code FacturaVentaService}/
 * {@code FacturaCompraService}, ya {@code @Transactional} cada uno) — a
 * propósito, este método NO lleva {@code @Transactional}: si una fila del
 * lote falla, las anteriores ya confirmadas/creadas no se revierten. Un
 * lote es "N documentos independientes", no una operación atómica.
 */
@Service
@RequiredArgsConstructor
public class ImportacionFacturaService {

    private static final String ESTADO_CONFIRMADO = "CONFIRMADO";

    private final ExtractorFacturaPdf extractor;
    private final ClienteRepository clienteRepo;
    private final ClienteService clienteService;
    private final ProveedorRepository proveedorRepo;
    private final ProveedorService proveedorService;
    private final MonedaRepository monedaRepo;
    private final FacturaVentaRepository facturaVentaRepo;
    private final FacturaVentaService facturaVentaService;
    private final FacturaCompraRepository facturaCompraRepo;
    private final FacturaCompraService facturaCompraService;

    public FilaImportacionPreviewResponse previsualizar(String nombreArchivo, byte[] bytesPdf) {
        String texto = extraerTexto(bytesPdf);
        CamposExtraidosPdf campos = extractor.extraer(texto);
        List<String> advertencias = new ArrayList<>(campos.advertencias());

        Long clienteId = null;
        String clienteNombre = null;
        Long proveedorId = null;
        String proveedorNombre = null;

        if (campos.cuitContraparte() != null) {
            if ("VENTA".equals(campos.tipoSugerido())) {
                Optional<Cliente> cliente = clienteRepo.findByCuit(campos.cuitContraparte());
                if (cliente.isPresent()) {
                    clienteId = cliente.get().getId();
                    clienteNombre = cliente.get().getNombre();
                } else {
                    advertencias.add("No existe un cliente con CUIT " + campos.cuitContraparte() + " — se puede dar de alta al confirmar.");
                }
            } else {
                Optional<Proveedor> proveedor = proveedorRepo.findByCuit(campos.cuitContraparte());
                if (proveedor.isPresent()) {
                    proveedorId = proveedor.get().getId();
                    proveedorNombre = proveedor.get().getNombre();
                } else {
                    advertencias.add("No existe un proveedor con CUIT " + campos.cuitContraparte() + " — se puede dar de alta al confirmar.");
                }
            }
        }

        Long monedaId = monedaRepo.findByCodigo(campos.monedaCodigo()).map(m -> m.getId()).orElse(null);

        return new FilaImportacionPreviewResponse(nombreArchivo, campos.tipoSugerido(), campos.tipoComprobante(),
                campos.puntoVenta(), campos.numero(), campos.fecha(), campos.cuitContraparte(),
                clienteId, clienteNombre, proveedorId, proveedorNombre,
                campos.monedaCodigo(), monedaId, campos.tipoCambio(), campos.netoGravado(), campos.alicuotaIva(),
                campos.total(), campos.cae(), advertencias, texto);
    }

    public List<FilaImportacionResultadoResponse> confirmar(List<FilaImportacionConfirmarRequest> filas) {
        List<FilaImportacionResultadoResponse> resultados = new ArrayList<>();
        for (FilaImportacionConfirmarRequest fila : filas) {
            resultados.add(procesarFila(fila));
        }
        return resultados;
    }

    private FilaImportacionResultadoResponse procesarFila(FilaImportacionConfirmarRequest fila) {
        try {
            return "VENTA".equalsIgnoreCase(fila.tipo())
                    ? procesarVenta(fila)
                    : procesarCompra(fila);
        } catch (Exception e) {
            return new FilaImportacionResultadoResponse(fila.nombreArchivo(), false, fila.tipo(), fila.numero(),
                    null, null, null, e.getMessage(), null);
        }
    }

    private FilaImportacionResultadoResponse procesarVenta(FilaImportacionConfirmarRequest fila) {
        Long clienteId = fila.clienteId() != null ? fila.clienteId() : altaRapidaCliente(fila);
        if (facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
                clienteId, fila.tipoComprobante(), fila.puntoVenta(), fila.numero())) {
            return rechazo(fila, "Ya importada (mismo cliente, tipo, punto de venta y número).");
        }

        FacturaVentaLineaRequest linea = new FacturaVentaLineaRequest(fila.descripcionLinea(), TipoLineaFactura.GRAVADO,
                fila.importeNeto(), fila.alicuotaIva(),
                fila.tipoIngreso() != null ? TipoIngreso.valueOf(fila.tipoIngreso()) : TipoIngreso.VENTA, null);
        FacturaVentaCrearRequest req = new FacturaVentaCrearRequest(clienteId, fila.proyectoId(), fila.fecha(),
                fila.fechaVencimiento(), fila.tipoComprobante(), fila.puntoVenta(), fila.numero(), null,
                fila.monedaId(), fila.tipoCambio(), fila.observaciones(), List.of(linea));

        FacturaVenta creada = facturaVentaService.crearBorrador(req);

        if (ESTADO_CONFIRMADO.equals(fila.estadoDestino())) {
            try {
                FacturaVenta confirmada = facturaVentaService.confirmar(creada.getId());
                Asiento asiento = confirmada.getAsiento();
                return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "VENTA", fila.numero(),
                        creada.getId(), ESTADO_CONFIRMADO, asiento != null ? asiento.getId() : null, null, null);
            } catch (Exception e) {
                return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "VENTA", fila.numero(),
                        creada.getId(), "BORRADOR", null, null,
                        "Se creó como borrador; no se pudo confirmar automáticamente: " + e.getMessage());
            }
        }
        return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "VENTA", fila.numero(),
                creada.getId(), "BORRADOR", null, null, null);
    }

    private FilaImportacionResultadoResponse procesarCompra(FilaImportacionConfirmarRequest fila) {
        Long proveedorId = fila.proveedorId() != null ? fila.proveedorId() : altaRapidaProveedor(fila);
        if (facturaCompraRepo.existsByProveedorIdAndTipoComprobanteAndPuntoVentaAndNumero(
                proveedorId, fila.tipoComprobante(), fila.puntoVenta(), fila.numero())) {
            return rechazo(fila, "Ya importada (mismo proveedor, tipo, punto de venta y número).");
        }
        if (fila.tipoCostoId() == null) {
            return rechazo(fila, "Falta seleccionar el tipo de costo de la línea.");
        }

        FacturaCompraLineaRequest linea = new FacturaCompraLineaRequest(fila.descripcionLinea(), fila.tipoCostoId(),
                fila.importeNeto(), fila.alicuotaIva(), null);
        FacturaCompraCrearRequest req = new FacturaCompraCrearRequest(proveedorId, fila.proyectoId(), fila.fecha(),
                fila.fechaVencimiento(), fila.tipoComprobante(), fila.puntoVenta(), fila.numero(),
                fila.monedaId(), fila.tipoCambio(), fila.observaciones(), List.of(linea), null);

        FacturaCompra creada = facturaCompraService.crearBorrador(req);

        if (ESTADO_CONFIRMADO.equals(fila.estadoDestino())) {
            try {
                FacturaCompra confirmada = facturaCompraService.confirmar(creada.getId());
                Asiento asiento = confirmada.getAsiento();
                return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "COMPRA", fila.numero(),
                        creada.getId(), ESTADO_CONFIRMADO, asiento != null ? asiento.getId() : null, null, null);
            } catch (Exception e) {
                return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "COMPRA", fila.numero(),
                        creada.getId(), "BORRADOR", null, null,
                        "Se creó como borrador; no se pudo confirmar automáticamente: " + e.getMessage());
            }
        }
        return new FilaImportacionResultadoResponse(fila.nombreArchivo(), true, "COMPRA", fila.numero(),
                creada.getId(), "BORRADOR", null, null, null);
    }

    private Long altaRapidaCliente(FilaImportacionConfirmarRequest fila) {
        return clienteService.crear(new ClienteCrearRequest(fila.altaRapidaNombre(), fila.altaRapidaCuit(),
                fila.altaRapidaJurisdiccionId(), null, null, null, null)).getId();
    }

    private Long altaRapidaProveedor(FilaImportacionConfirmarRequest fila) {
        return proveedorService.crear(new ProveedorCrearRequest(fila.altaRapidaNombre(), fila.altaRapidaCuit(),
                fila.altaRapidaJurisdiccionId(), null, null, null, null, null, null, null)).getId();
    }

    private FilaImportacionResultadoResponse rechazo(FilaImportacionConfirmarRequest fila, String motivo) {
        return new FilaImportacionResultadoResponse(fila.nombreArchivo(), false, fila.tipo(), fila.numero(),
                null, null, null, motivo, null);
    }

    private String extraerTexto(byte[] bytesPdf) {
        try (PDDocument documento = Loader.loadPDF(bytesPdf)) {
            return new PDFTextStripper().getText(documento);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el PDF", e);
        }
    }
}
