package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Asiento contable (F3.1 §3, F3.4): cabecera + 2..N líneas
 * ({@link AsientoLinea}). {@code numero} es el molde PL-5 más la numeración
 * interna de F3.1 §3.2: nulo en borrador, correlativo y definitivo al
 * confirmar (ver {@link com.montanaritech.contable.common.asiento.NumeradorAsientoPersistente}).
 * Las dimensiones analíticas (proyecto, cliente, proveedor, etc.) van por
 * línea, no acá (F3.1 §3.1, decisión D-1): la cabecera solo lleva los datos
 * de la operación como un todo.
 */
@Entity
@Table(name = "asiento", uniqueConstraints = @UniqueConstraint(name = "uk_asiento_tenant_numero", columnNames = {"tenant_id", "numero"}))
@Getter
@Setter
public class Asiento extends EntidadNegocio {

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    /** Nulo hasta confirmar; correlativo, inmutable y nunca reusado desde entonces. */
    @Column
    private Long numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenAsiento origen = OrigenAsiento.MANUAL;

    @Column(name = "origen_tipo", length = 60)
    private String origenTipo;

    @Column(name = "origen_id")
    private Long origenId;

    @Column(length = 2000)
    private String observaciones;

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<AsientoLinea> lineas = new ArrayList<>();
}
