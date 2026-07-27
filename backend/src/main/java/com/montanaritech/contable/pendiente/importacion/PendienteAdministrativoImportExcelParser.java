package com.montanaritech.contable.pendiente.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Lee la hoja de importación de pendientes (F10.2): fila 0 encabezado,
 * columna única 0 (título de la nota).
 */
@Component
public class PendienteAdministrativoImportExcelParser implements PendienteAdministrativoImportParser {

    private static final int COLUMNAS = 1;

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".xlsx") || n.endsWith(".xls");
    }

    @Override
    public List<PendienteAdministrativoImportFilaCruda> parsear(InputStream in) throws IOException {
        List<PendienteAdministrativoImportFilaCruda> filas = new ArrayList<>();
        DataFormatter formateador = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet hoja = workbook.getSheetAt(0);
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || esFilaVacia(fila, formateador)) {
                    continue;
                }
                String titulo = formateador.formatCellValue(fila.getCell(0)).trim();
                filas.add(new PendienteAdministrativoImportFilaCruda(i + 1, titulo));
            }
        }
        return filas;
    }

    private boolean esFilaVacia(Row fila, DataFormatter formateador) {
        for (int c = 0; c < COLUMNAS; c++) {
            if (!formateador.formatCellValue(fila.getCell(c)).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
