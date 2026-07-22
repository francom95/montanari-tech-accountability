package com.montanaritech.contable.facturacion.importacion;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.util.Map;

/**
 * Tabla pública de códigos de comprobante ARCA/AFIP (F4.6): la que
 * imprime cualquier factura electrónica en el recuadro "COD. NNN"/
 * "Código NN". Solo cubre los tipos que el sistema ya modela
 * ({@link TipoComprobante}) — recibos/tickets/otros comprobantes no
 * fiscales quedan fuera de esta tabla a propósito.
 */
final class CodigoComprobanteAfip {

    private static final Map<Integer, TipoComprobante> POR_CODIGO = Map.ofEntries(
            Map.entry(1, TipoComprobante.FACTURA_A),
            Map.entry(2, TipoComprobante.NOTA_DEBITO_A),
            Map.entry(3, TipoComprobante.NOTA_CREDITO_A),
            Map.entry(6, TipoComprobante.FACTURA_B),
            Map.entry(7, TipoComprobante.NOTA_DEBITO_B),
            Map.entry(8, TipoComprobante.NOTA_CREDITO_B),
            Map.entry(11, TipoComprobante.FACTURA_C),
            Map.entry(12, TipoComprobante.NOTA_DEBITO_C),
            Map.entry(13, TipoComprobante.NOTA_CREDITO_C),
            Map.entry(19, TipoComprobante.FACTURA_E),
            Map.entry(20, TipoComprobante.NOTA_DEBITO_E),
            Map.entry(21, TipoComprobante.NOTA_CREDITO_E));

    private CodigoComprobanteAfip() {
    }

    static TipoComprobante resolver(int codigo) {
        return POR_CODIGO.get(codigo);
    }
}
