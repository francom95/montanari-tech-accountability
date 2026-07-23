package com.montanaritech.contable.common.tenant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code logoClasspath} es el slot configurable del logo de reportes (F7.1):
 * ruta de un recurso ya empaquetado en el classpath (p. ej.
 * {@code logos/montanari.png}). {@code null} lo deja sin logo.
 */
public record TenantActualizarRequest(@NotBlank String nombre, String logoClasspath) {}
