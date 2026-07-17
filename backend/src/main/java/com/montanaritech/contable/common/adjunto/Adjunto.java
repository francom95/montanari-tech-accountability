package com.montanaritech.contable.common.adjunto;

import com.montanaritech.contable.common.tenant.EntidadNegocio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Adjunto genérico (F1.1 §6.5): cualquier entidad puede tener 0..N adjuntos,
 * identificados por {@code entidadTipo}/{@code entidadId} (vínculo
 * polimórfico simple, sin FK real — la entidad dueña no necesita saber que
 * existe este módulo). El archivo vive en filesystem/volumen Docker,
 * <b>nunca</b> como BLOB en MySQL; {@code ruta} es relativa al directorio
 * configurado en {@code app.adjuntos.directorio}. Opcional en facturas
 * (funcional §5.4): esta tabla no exige ni un mínimo ni un máximo.
 */
@Entity
@Table(name = "adjunto")
@Getter
@Setter
public class Adjunto extends EntidadNegocio {

    @Column(name = "entidad_tipo", nullable = false, length = 40)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 500)
    private String ruta;

    @Column(nullable = false, length = 100)
    private String mime;

    @Column(nullable = false)
    private long tamanio;
}
