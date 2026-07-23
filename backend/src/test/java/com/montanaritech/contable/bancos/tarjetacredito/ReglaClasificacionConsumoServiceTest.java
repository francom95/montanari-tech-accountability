package com.montanaritech.contable.bancos.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ReglaClasificacionCrearRequest;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** CRUD de reglas de clasificación masiva (F5.4 §2). */
@ExtendWith(MockitoExtension.class)
class ReglaClasificacionConsumoServiceTest {

    @Mock private ReglaClasificacionConsumoRepository repo;
    @Mock private CuentaContableRepository cuentaContableRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private ConceptoRepository conceptoRepository;
    @Mock private ReglaClasificacionMapper mapper;
    @Mock private AuditoriaService auditoria;

    private ReglaClasificacionConsumoService service;
    private CuentaContable cuenta;

    @BeforeEach
    void setUp() {
        service = new ReglaClasificacionConsumoService(repo, cuentaContableRepository, proveedorRepository,
                proyectoRepository, conceptoRepository, mapper, auditoria);

        cuenta = new CuentaContable();
        cuenta.setId(1L);
        cuenta.setCodigo("5.3.3001");

        lenient().when(cuentaContableRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mapper.aResponse(any())).thenReturn(null);
    }

    @Test
    void crearGuardaLaReglaActivaConLaCuentaResuelta() {
        var req = new ReglaClasificacionCrearRequest("DONWEB", 1L, null, null, null);

        ReglaClasificacionConsumo creada = service.crear(req);

        assertThat(creada.isActivo()).isTrue();
        assertThat(creada.getPatron()).isEqualTo("DONWEB");
        assertThat(creada.getCuentaContable()).isEqualTo(cuenta);
    }

    @Test
    void desactivarCambiaElEstadoAInactivo() {
        ReglaClasificacionConsumo r = new ReglaClasificacionConsumo();
        r.setId(1L);
        r.setActivo(true);
        when(repo.findById(1L)).thenReturn(Optional.of(r));

        service.desactivar(1L);

        assertThat(r.isActivo()).isFalse();
    }
}
