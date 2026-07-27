package com.montanaritech.contable.inversion.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de inversiones (F10.2, hoja "Inversiones en Fondos Fima"). */
public interface InversionImportParser {

    boolean soporta(String nombreArchivo);

    List<InversionImportFilaCruda> parsear(InputStream in) throws IOException;
}
