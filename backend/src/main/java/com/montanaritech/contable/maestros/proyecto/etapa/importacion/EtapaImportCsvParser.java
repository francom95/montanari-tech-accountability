package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

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
 * Lee CSV de importación de etapas (F2.5): misma disposición de columnas que
 * {@link EtapaImportExcelParser} (fila 0 encabezado, 12 columnas fijas).
 */
@Component
public class EtapaImportCsvParser implements EtapaImportParser {

    private static final int COLUMNAS = 12;

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".csv");
    }

    @Override
    public List<EtapaImportFilaCruda> parsear(InputStream in) throws IOException {
        List<EtapaImportFilaCruda> filas = new ArrayList<>();

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
                filas.add(new EtapaImportFilaCruda(numeroFila, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11]));
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
