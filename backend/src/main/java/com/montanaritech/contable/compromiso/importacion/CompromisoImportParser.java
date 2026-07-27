package com.montanaritech.contable.compromiso.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de compromisos (F10.2, hoja "Presupuesto de Pagos" §1). */
public interface CompromisoImportParser {

    boolean soporta(String nombreArchivo);

    List<CompromisoImportFilaCruda> parsear(InputStream in) throws IOException;
}
