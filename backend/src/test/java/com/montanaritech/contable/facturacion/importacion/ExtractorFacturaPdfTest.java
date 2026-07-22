package com.montanaritech.contable.facturacion.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.facturacion.TipoComprobante;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Casos reales (F4.6): texto TAL CUAL lo extrajo PDFBox de 7 comprobantes
 * reales de Montanari Tech (capturado contra el endpoint real, no
 * reconstruido a mano) — 2 compras de terceros ARCA-estándar, 2 ventas
 * ARCA-estándar, 1 venta de exportación, y 3 comprobantes de terceros con
 * layout propio: Mercado Pago, OpenAI y Dattatec/DonWeb. El orden lineal del
 * texto en varios de estos NO respeta la disposición visual (etiquetas
 * pegadas entre sí, valores pegados a la palabra siguiente sin espacio,
 * espacios de no separación U+00A0) — por eso varios casos verifican
 * específicamente los caminos alternativos del extractor, y el de
 * exportación (doc 3) documenta un límite real y aceptado: el total queda
 * sin extraer porque el valor aparece pegado ANTES de la etiqueta sin
 * ningún separador ("483,00Importe Total:"), un caso que corresponde a
 * "parsing avanzado" (etapa 2, fuera de alcance de F4.6) — el formulario de
 * carga asistida lo completa a mano.
 */
class ExtractorFacturaPdfTest {

    private final ExtractorFacturaPdf extractor = new ExtractorFacturaPdf("30-71833486-8", "MONTANARI TECHNOLOGIES");

    // ---- Doc 1: Lubenfeld (monotributista) -> Montanari, compra Factura C, sin IVA ----

    @Test
    void doc1LubenfeldCompraFacturaCSinIva() {
        String texto = """
                Fecha de Emisión:
                ORIGINAL
                LUBENFELD DAMIAN ALEJANDRO
                Campichuelo 585 Piso:4 Dpto:B - Ciudad de
                Buenos Aires
                Período Facturado Desde: Hasta: Fecha de Vto. para el pago:
                Condición de venta:
                Condición frente al IVA:
                Apellido y Nombre / Razón Social:
                Domicilio:
                01/07/2026 01/07/2026 01/07/2026
                01/07/2026
                20288405167
                ST SURFING RESERVATIONS SOCIEDAD DE
                RESPONSABILIDAD LIMITADA
                40 965 Piso:2 - La Plata Noroeste Calle 50, Buenos Aires
                Transferencia Bancaria
                CUIT:
                Ingresos Brutos:
                Fecha de Inicio de Actividades:
                Punto de Venta: Comp. Nro:00003 00000105
                Domicilio Comercial:
                Razón Social:
                LUBENFELD DAMIAN ALEJANDRO
                Condición frente al IVA:
                FACTURAC
                COD. 011
                Responsable Monotributo
                IVA Responsable Inscripto
                20-28840516-7
                01/02/2021
                CUIT: 30718334868
                Código Producto / Servicio Cantidad U. Medida Precio Unit. % Bonif Imp. Bonif. Subtotal
                Pago 2/2 Rediseño APP Facoextrema 1,00 unidades 1450000,00 1450000,000,00 0,00
                0,00
                1450000,00
                1450000,00
                Subtotal: $
                Importe Otros Tributos: $
                Importe Total: $
                CAE N°:
                Fecha de Vto. de CAE:
                Comprobante Autorizado
                Esta Agencia no se responsabiliza por los datos ingresados en el detalle de la operación
                11/07/2026
                86262004442157Pág. 1/1""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("COMPRA");
        assertThat(campos.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA_C);
        assertThat(campos.puntoVenta()).isEqualTo("00003");
        assertThat(campos.numero()).isEqualTo("00000105");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(campos.cuitContraparte()).isEqualTo("20-28840516-7");
        assertThat(campos.monedaCodigo()).isEqualTo("ARS");
        assertThat(campos.total()).isEqualByComparingTo("1450000.00");
        assertThat(campos.netoGravado()).isEqualByComparingTo("1450000.00");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("0");
        assertThat(campos.cae()).isEqualTo("86262004442157");
    }

    // ---- Doc 2: Montanari -> Asociación Mutual, venta Factura B, IVA Contenido ----

    @Test
    void doc2MontanariVentaFacturaBConIvaContenido() {
        String texto = """
                Fecha de Emisión:
                ORIGINAL
                ST SURFING RESERVATIONS SOCIEDAD DE
                RESPONSABILIDAD LIMITADA
                40 965 Piso:2 - La Plata Noroeste Calle 50,
                Buenos Aires
                Período Facturado Desde: Hasta: Fecha de Vto. para el pago:
                Condición de venta:
                Condición frente al IVA:
                Apellido y Nombre / Razón Social:
                Domicilio:
                13/03/2026 13/03/2026 13/03/2026
                13/03/2026
                30718334868
                ASOCIACION MUTUAL AMARILLA DE TRABAJADORES
                45 859 - La Plata Noroeste Calle 50, Buenos Aires
                Transferencia Bancaria
                CUIT:
                Ingresos Brutos:
                Fecha de Inicio de Actividades:
                Punto de Venta: Comp. Nro:00002 00000049
                Domicilio Comercial:
                Razón Social:
                MONTANARI TECHNOLOGIES
                Condición frente al IVA:
                FACTURAB
                COD. 006
                IVA Responsable Inscripto
                IVA Sujeto Exento
                30718334868
                01/12/2023
                CUIT: 30707986952
                Código Producto / Servicio Cantidad U. Medida Precio Unit. % Bonif Imp. Bonif. Subtotal
                Conexión de formulario web a Hoja de
                cálculo de Google
                1,00 unidades 338800,00 338800,000,00 0,00
                0,00
                338800,00
                338800,00
                Subtotal: $
                Importe Otros Tributos: $
                Importe Total: $
                CAE N°:
                Fecha de Vto. de CAE:
                Comprobante Autorizado
                Esta Agencia no se responsabiliza por los datos ingresados en el detalle de la operación
                23/03/2026
                86117468159586
                "Montanari Technologies"
                Pág. 1/1
                Régimen de Transparencia Fiscal al Consumidor (Ley 27.743)
                IVA Contenido: $ 58800,00""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("VENTA");
        assertThat(campos.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA_B);
        assertThat(campos.puntoVenta()).isEqualTo("00002");
        assertThat(campos.numero()).isEqualTo("00000049");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 3, 13));
        assertThat(campos.cuitContraparte()).isEqualTo("30-70798695-2");
        assertThat(campos.total()).isEqualByComparingTo("338800.00");
        assertThat(campos.netoGravado()).isEqualByComparingTo("280000.00");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("21");
        assertThat(campos.cae()).isEqualTo("86117468159586");
    }

    // ---- Doc 3: Montanari -> Jarp Inc (Puerto Rico), venta de exportación en USD ----
    // Límite real y aceptado: el valor del total queda pegado ANTES de su etiqueta sin
    // separador ("483,00Importe Total:") — fuera de alcance de la extracción básica.

    @Test
    void doc3MontanariVentaExportacionUsdSinCuitExtranjero() {
        String texto = """
                Fecha de Emisión:
                ORIGINAL
                Señor(es): Domicilio:
                17/04/2026
                30718334868
                Jarp Inc Flor del Valle 599 mayaguez PR 00680
                CUIT:
                Ingresos Brutos:
                Fecha de Inicio de Actividades:
                Compr. Nro: 00003-00000008
                Domicilio Comercial: 40 965 Piso:2 - La Plata Noroeste Calle 50,
                Buenos Aires, Argentina
                Razón Social: ST SURFING RESERVATIONS SOCIEDAD DE
                RESPONSABILIDAD LIMITADA
                MONTANARI TECHNOLOGIES
                Condición frente al IVA: IVA Responsable Inscripto
                FACTURA DE EXPORTACIÓNE
                COD. 19
                30718334868
                01/12/2023
                CUIT País: 55000002215 (ESTADO LIBRE ASOCIADO DE PUERTO RICO (Estado asoc. a EEUU) - Persona Jurídica)
                ID Impositivo: 66-1038429
                IVA EXENTO OPERACIÓN DE EXPORTACIÓN
                Divisa: USD - Dólar Estadounidense
                Destino del Comprobante: PUERTO RICO
                Ítem Descripción Cantidad
                Forma de Pago: Transferencia Bancaria - Moneda
                Extranjera
                Incoterms:
                Precio Unit. (USD) Total por ítem (USD)
                17/04/2026Fecha de Pago:
                Pago 8/8 desarrollo de Sistema Web + APP para gestión de concesionarias
                U. Medida:
                483,000000 483,000001 1,000000
                unidades
                483,00Importe Total:
                CAE N°:
                Fecha de Vto. de CAE:
                Comprobante Autorizado
                Esta Agencia no se responsabiliza por la veracidad de los datos ingresados en el detalle de la operación
                17/04/2026
                86161961743380
                "Montanari Technologies"
                Divisa: USD - Dólar Estadounidense
                USD
                Tipo de Cambio: 1358.000000""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("VENTA");
        assertThat(campos.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA_E);
        assertThat(campos.puntoVenta()).isEqualTo("00003");
        assertThat(campos.numero()).isEqualTo("00000008");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 4, 17));
        // Cliente del exterior sin CUIT AR: "CUIT País: 55000002215" tiene prefijo inválido (55) y se descarta a propósito.
        assertThat(campos.cuitContraparte()).isNull();
        assertThat(campos.monedaCodigo()).isEqualTo("USD");
        assertThat(campos.tipoCambio()).isEqualByComparingTo("1358.000000");
        assertThat(campos.cae()).isEqualTo("86161961743380");
        // Límite real y aceptado (ver javadoc de la clase): el valor queda pegado antes de la etiqueta.
        assertThat(campos.total()).isNull();
        assertThat(campos.advertencias()).isNotEmpty();
    }

    // ---- Doc 4: Montanari -> Kakaroto M4, venta Factura A con desglose de IVA por alícuota ----

    @Test
    void doc4MontanariVentaFacturaAConDesgloseIva() {
        String texto = """
                Fecha de Emisión:
                ORIGINAL
                ST SURFING RESERVATIONS SOCIEDAD
                DE RESPONSABILIDAD LIMITADA
                40 965 Piso:2 - La Plata Noroeste Calle 50,
                Buenos Aires
                Período Facturado Desde: Hasta: Fecha de Vto. para el pago:
                CUIT:
                Condición de venta:
                Condición frente al IVA:
                Apellido y Nombre / Razón Social:
                Domicilio Comercial:
                05/07/2026 05/07/2026 05/07/2026
                05/07/2026
                30718334868
                30718728823 KAKAROTO M4 S.R.L
                Azopardo 1673 - Valeria Del Mar, Buenos Aires
                Transferencia Bancaria
                CUIT:
                Ingresos Brutos:
                Fecha de Inicio de Actividades:
                Punto de Venta: Comp. Nro:00002 00000068
                Domicilio Comercial:
                Razón Social:
                MONTANARI TECHNOLOGIES
                Condición frente al IVA:
                FACTURAA
                COD. 01
                IVA Responsable Inscripto
                IVA Responsable Inscripto
                01/12/2023
                30718334868
                Código Producto / Servicio Cantidad U. medida Precio Unit. % Bonif Subtotal Alicuota
                IVA Subtotal c/IVA
                Corrección de dominio + SSL spa-wellness.com.
                ar
                1,00 unidades 47933,8840 0,00 47933,88 21% 58000,00
                Pago 1/2 por Rediseño y Desarrollo de sitio web
                apartdelsol.com.ar
                1,00 unidades 490750,0000 0,00 490750,00 21% 593807,50
                CAE N°:
                Fecha de Vto. de CAE:
                Comprobante Autorizado
                Esta Agencia no se responsabiliza por los datos ingresados en el detalle de la operación
                15/07/2026
                86272582929502
                "Montanari Technologies"
                Importe Otros Tributos: $ 0,00
                Importe Neto Gravado: $ 538683,88
                IVA 27%: $ 0,00
                IVA 21%: $ 113123,62
                IVA 10.5%: $ 0,00
                IVA 5%: $ 0,00
                IVA 2.5%: $ 0,00
                Importe Otros Tributos: $ 0,00
                Importe Total: $ 651807,50
                IVA 0%: $ 0,00
                Pág. 1/1""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("VENTA");
        assertThat(campos.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA_A);
        assertThat(campos.puntoVenta()).isEqualTo("00002");
        assertThat(campos.numero()).isEqualTo("00000068");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(campos.cuitContraparte()).isEqualTo("30-71872882-3");
        assertThat(campos.total()).isEqualByComparingTo("651807.50");
        assertThat(campos.netoGravado()).isEqualByComparingTo("538683.88");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("21");
        assertThat(campos.cae()).isEqualTo("86272582929502");
    }

    // ---- Doc 5: Mercado Pago -> Montanari, compra, layout propio (no ARCA estándar) ----

    @Test
    void doc5MercadoPagoCompraLayoutPropio() {
        String texto = """
                Hoja 1/1
                A
                Original
                001
                Factura
                0061-00696016
                Fecha de emisión: 12.05.2026
                Fecha de Vto: 12.05.2026
                CUIT: 30-70308853-4
                ING. BRUTOS C.M: 30-70308853-4
                INICIO DE ACTIVIDAD: 15.07.1999
                MercadoLibre S.R.L.
                Av. Caseros 3039, Piso 2 (C1264AAK)
                Capital Federal - Argentina
                IVA RESPONSABLE INSCRIPTO
                Tel: (5411)4640-8000
                Nro. de cliente: 49650356
                Nombre: ST SURFING RESERVATIONS SOCIEDAD DE RESPONSABILIDAD LIMITADA
                Dirección: 40 965 Piso:2 T:B
                Ciudad: BUENOS AIRES
                I.V.A.: Responsable Inscripto
                CUIT: 30-71833486-8
                Localidad: LA PLATA NOROESTE CALLE 50
                CP: 1900
                CARGOS POR USO DE LA PLATAFORMA DE MERCADO PAGO 940,12
                ...
                TEST
                SUBTOTAL: 940,12
                I.V.A. INSC.21,00% 197,42
                TOTAL: 1.137,54
                CAE: 86195352711654
                Vencimiento CAE: 22.05.2026
                Mercado pago ofrece servicios de pago y no está autorizado por el Banco Central a operar
                como entidad financiera. Los fondos acreditados en cuentas de pago no constituyen
                depósitos en una entidad financiera ni están garantizados conforme legislación aplicable
                a depósitos en entidades financieras.""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("COMPRA");
        assertThat(campos.puntoVenta()).isEqualTo("0061");
        assertThat(campos.numero()).isEqualTo("00696016");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(campos.cuitContraparte()).isEqualTo("30-70308853-4");
        assertThat(campos.monedaCodigo()).isEqualTo("ARS");
        assertThat(campos.total()).isEqualByComparingTo("1137.54");
        assertThat(campos.netoGravado()).isEqualByComparingTo("940.12");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("21");
        assertThat(campos.cae()).isEqualTo("86195352711654");
        // Sin "COD."/"Código" en este layout: el tipo de comprobante queda para completar a mano.
        assertThat(campos.tipoComprobante()).isNull();
        assertThat(campos.advertencias()).isNotEmpty();
    }

    // ---- Doc 6: OpenAI -> Montanari, compra, factura en inglés sin CUIT/CAE ----

    @Test
    void doc6OpenAiCompraFacturaEnInglesSinCuitNiCae() {
        String texto = """
                Page 1 of 1
                Invoice
                Invoice number 607B8FB0-0015
                Date of issue June 15, 2026
                Date due June 15, 2026
                OpenAI OpCo, LLC
                1455 3rd Street
                San Francisco, California 94158
                United States
                ar@openai.com
                Bill to
                ST SURFING RESERVATIONS SRL
                40 St. 965
                1900 La Plata
                Buenos Aires
                Argentina
                +54 9 221 463-1884
                montanarifranco11@gmail.com
                $20.00 USD due June 15, 2026
                Pay online
                Description Qty Unit price Amount
                ChatGPT Plus Subscription (per seat)
                Jun 15–Jul 15, 2026
                1 $20.00 $20.00
                Subtotal $20.00
                Total $20.00
                Amount due $20.00 USD""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("COMPRA");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(campos.monedaCodigo()).isEqualTo("USD");
        assertThat(campos.total()).isEqualByComparingTo("20.00");
        assertThat(campos.netoGravado()).isEqualByComparingTo("20.00");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("0");
        // Sin CUIT, sin CAE, sin punto de venta/número reconocible: todo queda para carga manual.
        assertThat(campos.cuitContraparte()).isNull();
        assertThat(campos.cae()).isNull();
        assertThat(campos.puntoVenta()).isNull();
        assertThat(campos.tipoComprobante()).isNull();
        assertThat(campos.advertencias()).isNotEmpty();
    }

    // ---- Doc 7: Dattatec/DonWeb -> Montanari, compra Factura A, layout propio con SUB-TOTAL ----
    // Nota: el valor real de TOTAL viene separado de su etiqueta por un espacio de no
    // separación (U+00A0), no un espacio normal — por eso la normalización en extraer() importa acá.

    @Test
    void doc7DattatecCompraFacturaAConSubTotal() {
        String texto = """
                Dattatec.com S.R.L.
                Tel: (+54 341) 436-0555
                Cordoba 3753 - Rosario (2000) - Santa Fe
                info@donweb.com | https://donweb.com
                I.V.A. RESPONSABLE INSCRIPTO
                Código 01
                Factura
                Nº 0002 - 01793484
                Fecha:07/06/2026
                CUIT: 30-71017365-2
                Ing. Brutos: CM 9717926787
                Inic. Activ.: 04/07/2007
                Página 1 de 1
                Nombre y Apellido o Razón Social: ST Surfing Reservations SRL
                Domicilio: 40 nº 965 / Localidad: La Plata
                Provincia: Buenos Aires / Pais: Argentina
                I.V.A. IVA Responsable Inscripto CUIT:30718334868
                Condiciones de Venta Contado     Cta. Corriente
                Cant. Descripción Unitario TOTAL
                1
                631120 CLAE Cloud Server. Fecha
                desde: 14/06/2026 hasta: 14/07/2026.
                Producto: Cloud Server
                15560.33 15560.33
                Observaciones:
                SUB-TOTAL 15560.33
                DESCUENTO 0.00
                I.V.A. INSC. % 3267.67
                TOTAL $ 18828.00


                CAE Nº  86238737315298       Vto. del CAE:17/06/2026

                Comprobante Autorizado
                ORIGINAL
                CAE Nº  86238737315298
                Vto. del CAE:  17/06/2026""";

        CamposExtraidosPdf campos = extractor.extraer(texto);

        assertThat(campos.tipoSugerido()).isEqualTo("COMPRA");
        assertThat(campos.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA_A);
        assertThat(campos.puntoVenta()).isEqualTo("0002");
        assertThat(campos.numero()).isEqualTo("01793484");
        assertThat(campos.fecha()).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(campos.cuitContraparte()).isEqualTo("30-71017365-2");
        assertThat(campos.total()).isEqualByComparingTo("18828.00");
        assertThat(campos.netoGravado()).isEqualByComparingTo("15560.33");
        assertThat(campos.alicuotaIva()).isEqualByComparingTo("21");
        assertThat(campos.cae()).isEqualTo("86238737315298");
    }
}
