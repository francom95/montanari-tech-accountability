package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CobroRepository extends JpaRepository<Cobro, Long> {

    @Query("""
            SELECT c FROM Cobro c
            WHERE (:estado IS NULL OR c.estado = :estado)
              AND (:clienteId IS NULL OR c.cliente.id = :clienteId)
              AND (:fechaDesde IS NULL OR c.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR c.fecha <= :fechaHasta)
            """)
    Page<Cobro> buscar(
            @Param("estado") EstadoDocumento estado,
            @Param("clienteId") Long clienteId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);
}
