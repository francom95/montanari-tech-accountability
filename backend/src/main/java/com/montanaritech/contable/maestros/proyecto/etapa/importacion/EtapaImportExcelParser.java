package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

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
 * Lee la hoja de importación de etapas (F2.5): fila 0 es encabezado (se
 * ignora), columnas fijas 0..11 en el orden documentado en
 * {@link EtapaImportFilaCruda}. {@link DataFormatter} homogeneiza cualquier
 * tipo de celda (fecha, número, texto) a String tal como se ve en la planilla,
 * para que la validación posterior parsee siempre desde texto.
 */
@Component
public class EtapaImportExcelParser implements EtapaImportParser {

    private static final int COLUMNAS = 12;

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".xlsx") || n.endsWith(".xls");
    }

    @Override
    public List<EtapaImportFilaCruda> parsear(InputStream in) throws IOException {
        List<EtapaImportFilaCruda> filas = new ArrayList<>();
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
                filas.add(new EtapaImportFilaCruda(i + 1, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11]));
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
