package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoTarjetaRepository extends JpaRepository<PagoTarjeta, Long> {

    @Query("""
            SELECT p FROM PagoTarjeta p
            WHERE p.tarjetaCredito.id = :tarjetaCreditoId
            ORDER BY p.fecha DESC
            """)
    Page<PagoTarjeta> buscar(@Param("tarjetaCreditoId") Long tarjetaCreditoId, Pageable pageable);

    /** Saldo de la tarjeta (F5.4, {@code RecalculoSaldoService}): pagos confirmados posteriores al saldo inicial. */
    List<PagoTarjeta> findByTarjetaCredito_IdAndEstadoAndFechaAfter(Long tarjetaCreditoId, EstadoDocumento estado, LocalDate fecha);
}
