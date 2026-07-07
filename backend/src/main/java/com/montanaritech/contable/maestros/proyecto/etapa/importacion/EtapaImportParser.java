package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de etapas (F2.5). */
public interface EtapaImportParser {

    boolean soporta(String nombreArchivo);

    List<EtapaImportFilaCruda> parsear(InputStream in) throws IOException;
}
