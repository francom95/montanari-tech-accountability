package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de comisiones (F10.2, hoja "Comisiones por ventas"). */
public interface ComisionProyectoImportParser {

    boolean soporta(String nombreArchivo);

    List<ComisionProyectoImportFilaCruda> parsear(InputStream in) throws IOException;
}
