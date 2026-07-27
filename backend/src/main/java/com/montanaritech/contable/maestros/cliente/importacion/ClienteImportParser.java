package com.montanaritech.contable.maestros.cliente.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de clientes (F10.2, hoja "Base de datos - Clientes"). */
public interface ClienteImportParser {

    boolean soporta(String nombreArchivo);

    List<ClienteImportFilaCruda> parsear(InputStream in) throws IOException;
}
