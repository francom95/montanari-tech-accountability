package com.montanaritech.contable.inversion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInversionRepository extends JpaRepository<MovimientoInversion, Long> {

    Page<MovimientoInversion> findByInversion_IdOrderByFechaDesc(Long inversionId, Pageable pageable);

    List<MovimientoInversion> findByInversion_Id(Long inversionId);

    Optional<MovimientoInversion> findFirstByInversion_IdOrderByFechaDescIdDesc(Long inversionId);

    long countByInversion_Id(Long inversionId);

    /** F10.2: idempotencia del importador (misma inversión+fecha+tipo+cuotapartes = mismo movimiento del Excel). */
    boolean existsByInversion_IdAndFechaAndTipoAndCuotapartes(
            Long inversionId, java.time.LocalDate fecha, TipoMovimientoInversion tipo, java.math.BigDecimal cuotapartes);
}
