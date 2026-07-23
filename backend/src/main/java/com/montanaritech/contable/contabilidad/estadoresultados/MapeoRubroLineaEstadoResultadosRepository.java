package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.maestros.categoria.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MapeoRubroLineaEstadoResultadosRepository extends JpaRepository<MapeoRubroLineaEstadoResultados, Long> {

    @Query("SELECT m FROM MapeoRubroLineaEstadoResultados m JOIN FETCH m.rubro r ORDER BY r.nombre ASC")
    List<MapeoRubroLineaEstadoResultados> listarOrdenadoPorRubro();

    Optional<MapeoRubroLineaEstadoResultados> findByRubroIdAndNaturaleza(Long rubroId, Categoria.TipoCategoria naturaleza);
}
