package com.montanaritech.contable.contabilidad.asiento.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Adapter de lectura del CSV pre-decodificado del Libro Diario (F10.2).
 *
 * Solo CSV, deliberadamente: los códigos de cuenta corruptos por Excel
 * (F3.3, ver memoria de proyecto) se decodifican con Python fuera del código
 * de la app (lógica de una sola vez, ya validada en F10.1: 77/77 códigos
 * resuelven), no se porta a Java. El insumo de este parser ya trae el
 * código de cuenta en texto plano.
 */
public interface AsientoImportParser {

    List<AsientoImportFilaCruda> parsear(InputStream in) throws IOException;
}
