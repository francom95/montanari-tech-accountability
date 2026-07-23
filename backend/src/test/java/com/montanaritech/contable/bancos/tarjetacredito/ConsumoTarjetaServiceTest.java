package com.montanaritech.contable.bancos.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ClasificarConsumoRequest;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Clasificación de consumos de tarjeta (F5.4 §2): manual y masiva por reglas. */
@ExtendWith(MockitoExtension.class)
class ConsumoTarjetaServiceTest {

    @Mock private ConsumoTarjetaRepository repo;
    @Mock private ReglaClasificacionConsumoRepository reglaRepo;
    @Mock private CuentaContableRepository cuentaContableRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private ConceptoRepository conceptoRepository;
    @Mock private ConsumoTarjetaMapper mapper;
    @Mock private AuditoriaService auditoria;

    private ConsumoTarjetaService service;
    private CuentaContable cuentaHosting;
    private CuentaContable cuentaSaaS;

    @BeforeEach
    void setUp() {
        service = new ConsumoTarjetaService(repo, reglaRepo, cuentaContableRepository, proveedorRepository,
                proyectoRepository, conceptoRepository, mapper, auditoria);

        cuentaHosting = new CuentaContable();
        cuentaHosting.setId(1L);
        cuentaHosting.setCodigo("5.3.3001");

        cuentaSaaS = new CuentaContable();
        cuentaSaaS.setId(2L);
        cuentaSaaS.setCodigo("5.3.3002");

        lenient().when(mapper.aResponse(any())).thenReturn(null);
    }

    private ConsumoTarjeta consumo(Long id, String descripcion) {
        ConsumoTarjeta c = new ConsumoTarjeta();
        c.setId(id);
        c.setFecha(LocalDate.of(2026, 6, 1));
        c.setDescripcion(descripcion);
        c.setImporte(new BigDecimal("-1000.00"));
        c.setImporteArs(new BigDecimal("-1000.00"));
        return c;
    }

    private ReglaClasificacionConsumo regla(String patron, CuentaContable cuenta) {
        ReglaClasificacionConsumo r = new ReglaClasificacionConsumo();
        r.setPatron(patron);
        r.setCuentaContable(cuenta);
        r.setActivo(true);
        return r;
    }

    @Test
    void clasificarManualSeteaCuentaYAsociacionesOpcionales() {
        ConsumoTarjeta c = consumo(1L, "DONWEB");
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(cuentaContableRepository.findById(1L)).thenReturn(Optional.of(cuentaHosting));
        Proveedor proveedor = new Proveedor();
        proveedor.setId(9L);
        when(proveedorRepository.findById(9L)).thenReturn(Optional.of(proveedor));

        ConsumoTarjeta resultado = service.clasificar(1L, new ClasificarConsumoRequest(1L, 9L, null, null));

        assertThat(resultado.getCuentaContable()).isEqualTo(cuentaHosting);
        assertThat(resultado.getProveedor()).isEqualTo(proveedor);
        assertThat(resultado.getProyecto()).isNull();
    }

    @Test
    void clasificarMasivamenteAplicaLaPrimeraReglaQueMatcheaLaDescripcion() {
        ConsumoTarjeta consumoDonweb = consumo(1L, "31-05-26 DONWEB 005035");
        ConsumoTarjeta consumoGoogle = consumo(2L, "Google Workspace A71427191");
        ConsumoTarjeta consumoSinRegla = consumo(3L, "TRF INMED PROVEED Alejo Del Gobbo");

        when(repo.findByTarjetaCredito_IdAndCuentaContableIsNull(5L))
                .thenReturn(List.of(consumoDonweb, consumoGoogle, consumoSinRegla));
        when(reglaRepo.findByActivoTrue())
                .thenReturn(List.of(regla("DONWEB", cuentaHosting), regla("GOOGLE", cuentaSaaS)));

        int clasificados = service.clasificarMasivamente(5L);

        assertThat(clasificados).isEqualTo(2);
        assertThat(consumoDonweb.getCuentaContable()).isEqualTo(cuentaHosting);
        assertThat(consumoGoogle.getCuentaContable()).isEqualTo(cuentaSaaS);
        assertThat(consumoSinRegla.getCuentaContable()).isNull();
    }

    @Test
    void clasificarMasivamenteSinConsumosPendientesNoHaceNada() {
        when(repo.findByTarjetaCredito_IdAndCuentaContableIsNull(5L)).thenReturn(List.of());

        int clasificados = service.clasificarMasivamente(5L);

        assertThat(clasificados).isEqualTo(0);
    }

    @Test
    void clasificarMasivamenteSinReglasActivasNoHaceNada() {
        when(repo.findByTarjetaCredito_IdAndCuentaContableIsNull(5L)).thenReturn(List.of(consumo(1L, "DONWEB")));
        when(reglaRepo.findByActivoTrue()).thenReturn(List.of());

        int clasificados = service.clasificarMasivamente(5L);

        assertThat(clasificados).isEqualTo(0);
    }
}
