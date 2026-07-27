package com.montanaritech.contable.contabilidad.asiento.importacion;

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
 * Lee el CSV pre-decodificado del Libro Diario (F10.2): fila 0 encabezado,
 * columnas fijas 0..5 en el orden documentado en {@link AsientoImportFilaCruda}.
 * Las "filas basura" ({@code codigoDecodificado='#N/A'}, residuo de fórmulas
 * de Excel extendidas más allá de los datos reales, F10.1 §7) ya se
 * descartan en el pre-proceso Python — este parser asume el CSV limpio.
 */
@Component
public class AsientoImportCsvParser implements AsientoImportParser {

    private static final int COLUMNAS = 6;

    @Override
    public List<AsientoImportFilaCruda> parsear(InputStream in) throws IOException {
        List<AsientoImportFilaCruda> filas = new ArrayList<>();

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
                filas.add(new AsientoImportFilaCruda(numeroFila, v[0], v[1], v[2], v[3], v[4], v[5]));
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
