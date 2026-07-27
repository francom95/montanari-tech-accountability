package com.montanaritech.contable.pendiente.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de pendientes (F10.2, hojas "PENDIENTES"/"PENDIENTES AHORA"). */
public interface PendienteAdministrativoImportParser {

    boolean soporta(String nombreArchivo);

    List<PendienteAdministrativoImportFilaCruda> parsear(InputStream in) throws IOException;
}
