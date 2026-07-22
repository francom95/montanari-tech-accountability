package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.common.error.NegocioException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Parser del "Resumen de Cuenta" de actividad de Mercado Pago (Excel, hoja
 * "sheet0"). El archivo real tiene 2 bloques: un resumen inicial
 * (INITIAL_BALANCE/CREDITS/DEBITS/FINAL_BALANCE, en inglés) que se ignora, y
 * el detalle de movimientos (RELEASE_DATE/TRANSACTION_TYPE/REFERENCE_ID/
 * TRANSACTION_NET_AMOUNT/PARTIAL_BALANCE). A diferencia de Galicia, todas
 * las celdas son texto — incluye separador decimal AR (coma) que hay que
 * parsear a mano.
 */
@Component
public class ParserMercadoPago implements ResumenParser {

    private static final String COL_FECHA = "RELEASE_DATE";
    private static final String COL_DESCRIPCION = "TRANSACTION_TYPE";
    private static final String COL_REFERENCIA = "REFERENCE_ID";
    private static final String COL_IMPORTE = "TRANSACTION_NET_AMOUNT";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public OrigenImportacionMovimiento origen() {
        return OrigenImportacionMovimiento.MERCADO_PAGO;
    }

    @Override
    public List<MovimientoParseado> parsear(byte[] contenido) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = wb.getSheetAt(0);
            int filaEncabezadoDetalle = buscarFilaEncabezadoDetalle(hoja);
            if (filaEncabezadoDetalle < 0) {
                throw new NegocioException("RESUMEN_MP_INVALIDO",
                        "No se encontró el bloque de detalle de movimientos (encabezado " + COL_FECHA + ")");
            }
            Row encabezado = hoja.getRow(filaEncabezadoDetalle);

            int colFecha = buscarColumna(encabezado, COL_FECHA);
            int colDescripcion = buscarColumna(encabezado, COL_DESCRIPCION);
            int colReferencia = buscarColumna(encabezado, COL_REFERENCIA);
            int colImporte = buscarColumna(encabezado, COL_IMPORTE);
            if (colFecha < 0 || colDescripcion < 0 || colImporte < 0) {
                throw new NegocioException("RESUMEN_MP_INVALIDO", "No se reconocen las columnas del detalle de movimientos");
            }

            List<MovimientoParseado> movimientos = new ArrayList<>();
            for (int i = filaEncabezadoDetalle + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                String fechaTexto = textoDeCelda(fila, colFecha);
                String importeTexto = textoDeCelda(fila, colImporte);
                if (fechaTexto == null || fechaTexto.isBlank() || importeTexto == null || importeTexto.isBlank()) {
                    continue; // fila basura / de cierre
                }

                LocalDate fecha = LocalDate.parse(fechaTexto.trim(), FORMATO_FECHA);
                BigDecimal importe = parsearImporteAr(importeTexto);
                String descripcion = textoDeCelda(fila, colDescripcion);
                String referencia = colReferencia >= 0 ? textoDeCelda(fila, colReferencia) : null;

                movimientos.add(new MovimientoParseado(fecha, descripcion, importe, null, referencia));
            }
            return movimientos;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el resumen de Mercado Pago", e);
        }
    }

    private int buscarFilaEncabezadoDetalle(Sheet hoja) {
        for (int i = hoja.getFirstRowNum(); i <= hoja.getLastRowNum(); i++) {
            Row fila = hoja.getRow(i);
            if (fila != null && buscarColumna(fila, COL_FECHA) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private int buscarColumna(Row fila, String texto) {
        for (Cell celda : fila) {
            String valor = textoDeCelda(fila, celda.getColumnIndex());
            if (valor != null && valor.trim().equalsIgnoreCase(texto)) {
                return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private String textoDeCelda(Row fila, int columna) {
        if (columna < 0) {
            return null;
        }
        Cell celda = fila.getCell(columna);
        if (celda == null) {
            return null;
        }
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(celda.getNumericCellValue());
            default -> null;
        };
    }

    /** "13.019,89" (miles con punto, decimales con coma) → 13019.89; conserva el signo "-" si está presente. */
    private BigDecimal parsearImporteAr(String texto) {
        String normalizado = texto.trim().toUpperCase(Locale.ROOT).replace("$", "").trim();
        boolean negativo = normalizado.startsWith("-");
        normalizado = normalizado.replace("-", "").replace(".", "").replace(",", ".");
        BigDecimal valor = new BigDecimal(normalizado);
        return negativo ? valor.negate() : valor;
    }
}
