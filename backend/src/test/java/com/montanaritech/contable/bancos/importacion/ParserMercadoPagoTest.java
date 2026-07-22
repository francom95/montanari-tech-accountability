package com.montanaritech.contable.bancos.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Replica EXACTAMENTE la estructura y los valores de "Resumen de Cuenta.xlsx"
 * (Mercado Pago, hoja "sheet0") capturados con openpyxl contra el archivo
 * real: fila 1 encabezado de resumen (inglés, se ignora), fila 2 valores del
 * resumen, fila 3 en blanco, fila 4 encabezado de detalle, fila 5 el único
 * movimiento real disponible (un crédito). El binario no se versiona (es un
 * resumen real del cliente); ver outputs/F5_2_parsers_de_resumenes.md.
 */
class ParserMercadoPagoTest {

    private final ParserMercadoPago parser = new ParserMercadoPago();

    @Test
    void parseaElUnicoMovimientoRealIgnorandoElBloqueDeResumen() {
        byte[] archivo = construirArchivoReal();

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(1);
        MovimientoParseado m = movimientos.get(0);
        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 6, 17));
        assertThat(m.descripcion()).isEqualTo("Liquidación de dinero Borboleta");
        assertThat(m.importe()).isEqualByComparingTo("13019.89");
        assertThat(m.referencia()).isEqualTo("164637834802");
        assertThat(m.monedaCodigo()).isNull();
    }

    @Test
    void unImporteConSignoNegativoExplicitoQuedaComoEgreso() {
        byte[] archivo = construirArchivoConFila("18-06-2026", "Pago de servicio", "555", "-1.234,56");

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).importe()).isEqualByComparingTo("-1234.56");
    }

    private byte[] construirArchivoReal() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("sheet0");
            escribirFila(hoja, 0, "INITIAL_BALANCE", "CREDITS", "DEBITS", "FINAL_BALANCE", null);
            escribirFila(hoja, 1, "113.161,55", "13.019,89", "0,00", "126.181,44", null);
            hoja.createRow(2); // fila en blanco entre bloques
            escribirFila(hoja, 3, "RELEASE_DATE", "TRANSACTION_TYPE", "REFERENCE_ID", "TRANSACTION_NET_AMOUNT", "PARTIAL_BALANCE");
            escribirFila(hoja, 4, "17-06-2026", "Liquidación de dinero Borboleta", "164637834802", "13.019,89", "126.181,44");
            return aBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] construirArchivoConFila(String fecha, String tipo, String referencia, String importe) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("sheet0");
            escribirFila(hoja, 0, "INITIAL_BALANCE", "CREDITS", "DEBITS", "FINAL_BALANCE", null);
            escribirFila(hoja, 1, "0,00", "0,00", "0,00", "0,00", null);
            hoja.createRow(2);
            escribirFila(hoja, 3, "RELEASE_DATE", "TRANSACTION_TYPE", "REFERENCE_ID", "TRANSACTION_NET_AMOUNT", "PARTIAL_BALANCE");
            escribirFila(hoja, 4, fecha, tipo, referencia, importe, "0,00");
            return aBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void escribirFila(Sheet hoja, int indice, String... valores) {
        Row fila = hoja.createRow(indice);
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] != null) {
                fila.createCell(i).setCellValue(valores[i]);
            }
        }
    }

    private byte[] aBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
