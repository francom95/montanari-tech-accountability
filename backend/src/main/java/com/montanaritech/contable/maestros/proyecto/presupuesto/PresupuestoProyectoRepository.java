package com.montanaritech.contable.maestros.proyecto.presupuesto;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresupuestoProyectoRepository extends JpaRepository<PresupuestoProyecto, Long> {

    Optional<PresupuestoProyecto> findByProyectoId(Long proyectoId);
}
