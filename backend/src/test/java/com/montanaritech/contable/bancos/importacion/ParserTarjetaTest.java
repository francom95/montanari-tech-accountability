package com.montanaritech.contable.bancos.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El texto de {@code TEXTO_REAL} es EXACTAMENTE el que devolvió PDFBox
 * ({@code PDFTextStripper} con {@code setSortByPosition(false)}) contra el
 * resumen real de tarjeta VISA Business (Banco Galicia) provisto para F5.2 —
 * capturado con un test temporal antes de escribir el parser, no
 * reconstruido a ojo (misma lección de F4.6: un texto idealizado puede
 * pasar los tests mientras el extractor real falla contra el archivo real).
 * El PDF no se versiona (es un resumen real del cliente); ver
 * outputs/F5_2_parsers_de_resumenes.md.
 */
class ParserTarjetaTest {

    private static final String TEXTO_REAL = """
              Resumen N° VI00000000000590311
             Tarjeta Crédito VISA BUSINESS
            ST SURFING RESERVATIONS SOCIED  Responsable inscripto CUIT Banco: 30-50000173-5
            CALLE 40 965 2, LA PLATA, B1902ANM  N° Cuenta: 1219793994 Sucursal: 172
            Resumen de tarjeta de credito VISA BUSINESS
            20260702070590311H
            Página 1 / 6
            324.469,25
            238,71
            28-May-26 08-Jun-26 02-Jul-26 13-Jul-26 30-Jul-26 10-Ago-26
                 \s
            PAGO MINIMO LÍMITES
            En pesos
            $ 150.330,00
            De compras en un pago y en cuotas
            $ 1.000.000,00
            De financiación
            $ 900.000,00
            TASAS Nominal Anual
                   En pesos 87,240%          En dólares 20,000%
            Efectiva mensual
                   En pesos 7,170%        En dólares 1,644%
             CONSOLIDADO PESOS DÓLARES
            SALDO ANTERIOR 169.285,36 102,22
            05-06-26  SU PAGO EN PESOS -125.969,64  \s
            05-06-26  SU PAGO EN USD -102,22
            08-06-26  DEV.IMP. RG 5617  30%(  144385,75) -43.315,72  \s
            DETALLE DEL CONSUMO \s
            FECHA REFERENCIA CUOTA COMPROBANTE PESOS DÓLARES
            31-05-26 * DONWEB 005035 25.115,50  \s
            31-05-26 K TWILIO SENDGRID  P19558602USD        0,64 740226 0,64
            01-06-26 K Google Workspace A71427191USD        5,88 704221 5,88
            01-06-26  Google Workspace A98113085USD       19,20 161807 19,20
            01-06-26 K Google CLOUD MTK A96812639USD       20,49 006222 20,49
            03-06-26 K TWILIO SENDGRID  P19722286USD       19,95 432265 19,95
            07-06-26 * DONWEB 004667 18.828,00  \s
            07-06-26  APPLE.COM/US              USD       99,00 975671 99,00
            09-06-26 * DONWEB 004932 16.597,00  \s
            10-06-26 * DONWEB 005055 62.453,00  \s
            12-06-26 * DONWEB 005338 20.992,50  \s
            15-06-26 K OPENAI *CHATGPT  in1TibZ1CUSD       20,00 100357 20,00
            19-06-26 * DONWEB 006160 25.214,00  \s
            19-06-26 * DONWEB 006277 2.985,60  \s
            25-06-26 F CREEM*NANO AI ST          USD       14,90 716276 14,90
            30-06-26 * DONWEB 007772 30.091,50  \s
            01-07-26  Google CLOUD 3FT A61798252USD       19,45 996909 19,45
            01-07-26  Google Workspace A31936564USD       19,20 540908 19,20
            TARJETA 2064 Total Consumos de FRANCO MONTANARI 202.277,10 238,71
            02-07-26  IMPUESTO DE SELLOS        $ 3.792,10  \s
            02-07-26  COMISIÓN MANT DE CTA. 5.867,00  \s
            02-07-26  DB IVA $ RESP INSC. 21%                  5.867,00 1.232,07  \s
            02-07-26  IMPUESTO DE SELLOS      P $ 4.258,54  \s
            02-07-26  PERCEPCION ING.BRUTOS TASA  4,000% 234,68  \s
            02-07-26  PERCEP.IVA RG2408 3,0% B.   5867,00 176,01  \s
            02-07-26  DB.RG 5617  30% (   355439,19 ) 106.631,75  \s
            TOTAL A PAGAR 324.469,25 238,71
            """;

    private final ParserTarjeta parser = new ParserTarjeta();

    @Test
    void extraeSoloLasFilasQueEmpiezanConFechaIgnorandoEncabezadosYTotales() {
        List<MovimientoParseado> movimientos = parser.parsearTexto(TEXTO_REAL);

        // 3 CONSOLIDADO + 18 DETALLE DEL CONSUMO + 7 impuestos/comisiones = 28
        assertThat(movimientos).hasSize(28);
        assertThat(movimientos).noneMatch(m -> m.descripcion().contains("Total Consumos"));
        assertThat(movimientos).noneMatch(m -> m.descripcion().contains("TOTAL A PAGAR"));
        assertThat(movimientos).noneMatch(m -> m.descripcion().contains("SALDO ANTERIOR"));
    }

    @Test
    void suPagoEnPesosQuedaComoEgresoEnArs() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "SU PAGO EN PESOS");

        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(m.importe()).isEqualByComparingTo("-125969.64");
        assertThat(m.monedaCodigo()).isEqualTo("ARS");
    }

    @Test
    void suPagoEnUsdQuedaComoEgresoEnDolares() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "SU PAGO EN USD");

        assertThat(m.importe()).isEqualByComparingTo("-102.22");
        assertThat(m.monedaCodigo()).isEqualTo("USD");
    }

    @Test
    void devolucionDeImpuestoConParentesisEmbebidoTomaElUltimoImporte() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "DEV.IMP");

        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(m.importe()).isEqualByComparingTo("-43315.72");
    }

    @Test
    void consumoEnPesosDeDonwebQuedaComoEgresoConComprobante() {
        List<MovimientoParseado> movimientos = parser.parsearTexto(TEXTO_REAL);
        MovimientoParseado m = movimientos.stream()
                .filter(mm -> mm.fecha().equals(LocalDate.of(2026, 5, 31)) && mm.descripcion().contains("DONWEB"))
                .findFirst().orElseThrow();

        assertThat(m.descripcion()).isEqualTo("DONWEB");
        assertThat(m.referencia()).isEqualTo("005035");
        assertThat(m.importe()).isEqualByComparingTo("-25115.50");
        assertThat(m.monedaCodigo()).isEqualTo("ARS");
    }

    @Test
    void consumoEnDolaresGlueadoALaReferenciaQuedaComoEgresoEnUsd() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "TWILIO SENDGRID P19558602");

        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(m.importe()).isEqualByComparingTo("-0.64");
        assertThat(m.monedaCodigo()).isEqualTo("USD");
        assertThat(m.referencia()).isEqualTo("740226");
    }

    @Test
    void impuestoDeSellosConSignoDePesosSueltoQuedaComoEgreso() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "IMPUESTO DE SELLOS");

        assertThat(m.importe()).isEqualByComparingTo("-3792.10");
        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 7, 2));
    }

    @Test
    void dbIvaConBaseDeCalculoEmbebidaTomaElUltimoImporte() {
        MovimientoParseado m = buscar(parser.parsearTexto(TEXTO_REAL), "DB IVA");

        assertThat(m.importe()).isEqualByComparingTo("-1232.07");
    }

    private MovimientoParseado buscar(List<MovimientoParseado> movimientos, String textoDescripcion) {
        return movimientos.stream()
                .filter(m -> m.descripcion().contains(textoDescripcion))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró movimiento con descripción que contenga: " + textoDescripcion));
    }
}
