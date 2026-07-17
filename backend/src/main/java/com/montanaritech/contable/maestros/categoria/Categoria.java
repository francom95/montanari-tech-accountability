package com.montanaritech.contable.maestros.categoria;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Molde F1.8 PL-1. Categoría contable: nombre, descripción, tipo (Activo/Pasivo/PN/R+/R-), activo.
 */
@Entity
@Table(name = "categoria_contable", uniqueConstraints = @UniqueConstraint(name = "uk_categoria_tenant_nombre", columnNames = {"tenant_id", "nombre"}))
@Getter
@Setter
public class Categoria extends EntidadNegocio {

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCategoria tipo;

    @Column(nullable = false)
    private boolean activo = true;

    public enum TipoCategoria {
        ACTIVO,
        PASIVO,
        PN,
        RP,   // Resultado Positivo
        RN,   // Resultado Negativo
        // Otros Resultados: sección madre que agrupa cuentas de signo mixto; sus
        // cuentas hijas se clasifican como RP o RN según su tipo (no como
        // OTROS_RESULTADOS). Solo la cuenta madre lleva esta categoría.
        // (Extendido en F3.3 a pedido del negocio.)
        OTROS_RESULTADOS
    }
}
