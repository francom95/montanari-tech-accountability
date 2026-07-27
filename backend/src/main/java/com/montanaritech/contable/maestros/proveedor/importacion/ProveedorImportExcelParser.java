package com.montanaritech.contable.maestros.proveedor.importacion;

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
 * Lee la hoja de importación de proveedores (F10.2, molde de
 * {@code ClienteImportExcelParser}): fila 0 encabezado, columnas fijas 0..1
 * en el orden documentado en {@link ProveedorImportFilaCruda}.
 */
@Component
public class ProveedorImportExcelParser implements ProveedorImportParser {

    private static final int COLUMNAS = 2;

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".xlsx") || n.endsWith(".xls");
    }

    @Override
    public List<ProveedorImportFilaCruda> parsear(InputStream in) throws IOException {
        List<ProveedorImportFilaCruda> filas = new ArrayList<>();
        DataFormatter formateador = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet hoja = workbook.getSheetAt(0);
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || esFilaVacia(fila, formateador)) {
                    continue;
                }
                String[] v = new String[COLUMNAS];
                for (int c = 0; c < COLUMNAS; c++) {
                    v[c] = formateador.formatCellValue(fila.getCell(c)).trim();
                }
                filas.add(new ProveedorImportFilaCruda(i + 1, v[0], v[1]));
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
