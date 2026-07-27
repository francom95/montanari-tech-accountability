package com.montanaritech.contable.maestros.proveedor.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Adapter de lectura de archivo de importación de proveedores (F10.2, hoja "Proveedores de servicios"). */
public interface ProveedorImportParser {

    boolean soporta(String nombreArchivo);

    List<ProveedorImportFilaCruda> parsear(InputStream in) throws IOException;
}
