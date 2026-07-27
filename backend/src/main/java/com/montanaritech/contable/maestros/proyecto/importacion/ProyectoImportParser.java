package com.montanaritech.contable.maestros.proyecto.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de proyectos (F10.2, hoja "Clientes"). */
public interface ProyectoImportParser {

    boolean soporta(String nombreArchivo);

    List<ProyectoImportFilaCruda> parsear(InputStream in) throws IOException;
}
