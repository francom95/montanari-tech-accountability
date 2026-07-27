package com.montanaritech.contable.pendiente.importacion;

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
 * Lee CSV de importación de pendientes (F10.2): misma disposición de
 * columnas que {@link PendienteAdministrativoImportExcelParser} (fila 0 encabezado, columna única).
 */
@Component
public class PendienteAdministrativoImportCsvParser implements PendienteAdministrativoImportParser {

    @Override
    public boolean soporta(String nombreArchivo) {
        String n = nombreArchivo == null ? "" : nombreArchivo.toLowerCase();
        return n.endsWith(".csv");
    }

    @Override
    public List<PendienteAdministrativoImportFilaCruda> parsear(InputStream in) throws IOException {
        List<PendienteAdministrativoImportFilaCruda> filas = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(in, StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {
            String[] linea;
            int numeroFila = 1;
            while ((linea = reader.readNext()) != null) {
                numeroFila++;
                String titulo = linea.length > 0 ? linea[0].trim() : "";
                if (titulo.isBlank()) {
                    continue;
                }
                filas.add(new PendienteAdministrativoImportFilaCruda(numeroFila, titulo));
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV inválido: " + e.getMessage(), e);
        }
        return filas;
    }
}
