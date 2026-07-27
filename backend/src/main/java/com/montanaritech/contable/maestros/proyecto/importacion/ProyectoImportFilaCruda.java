package com.montanaritech.contable.maestros.proyecto.importacion;

/**
 * Fila cruda de la hoja "Clientes" (F10.1 §1), columnas fijas en orden:
 * {@code Proyecto} (texto compuesto "Cliente - (Fase: X)"), {@code Responsable/s},
 * {@code País}, {@code Detalle Tipo} (no migra), {@code Tipo de Persona} (no migra),
 * {@code Condición frente a IVA} (no migra), {@code Monto total proyecto sin IVA (USD)},
 * {@code Pago 1..8}, {@code Comentarios}.
 */
public record ProyectoImportFilaCruda(
        int numeroFila,
        String proyectoCompuesto,
        String responsables,
        String pais,
        String detalleTipo,
        String tipoPersona,
        String condicionIva,
        String montoTotalUsd,
        String pago1,
        String pago2,
        String pago3,
        String pago4,
        String pago5,
        String pago6,
        String pago7,
        String pago8,
        String comentarios
) {}
