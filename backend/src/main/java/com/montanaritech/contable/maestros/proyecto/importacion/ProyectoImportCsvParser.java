package com.montanaritech.contable.maestros.proyecto.importacion;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lee CSV de importación de proyectos (F10.2): misma disposición de columnas
 * que {@link ProyectoImportExcelParser} (fila 0 encabezado, 16 columnas fijas).
 */
@Component
public class ProyectoImportCsvParser implements ProyectoImportParser {

    private static final int COLUMNAS = 16;

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".csv");
    }

    @Override
    public List<ProyectoImportFilaCruda> parsear(InputStream in) throws IOException {
        List<ProyectoImportFilaCruda> filas = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(in, StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {
            String[] linea;
            int numeroFila = 1;
            while ((linea = reader.readNext()) != null) {
                numeroFila++;
                if (esLineaVacia(linea)) {
                    continue;
                }
                String[] v = new String[COLUMNAS];
                for (int c = 0; c < COLUMNAS; c++) {
                    v[c] = c < linea.length ? linea[c].trim() : "";
                }
                filas.add(new ProyectoImportFilaCruda(numeroFila, v[0], v[1], v[2], v[3], v[4], v[5], v[6],
                        v[7], v[8], v[9], v[10], v[11], v[12], v[13], v[14], v[15]));
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV inválido: " + e.getMessage(), e);
        }
        return filas;
    }

    private boolean esLineaVacia(String[] linea) {
        for (String valor : linea) {
            if (valor != null && !valor.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
