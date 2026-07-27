package com.montanaritech.contable.contabilidad.asiento.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AsientoImportServiceTest {

    private CuentaContableRepository cuentaContableRepo;
    private AsientoRepository asientoRepo;
    private AsientoService asientoService;
    private AsientoImportService service;

    private List<AsientoImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        cuentaContableRepo = mock(CuentaContableRepository.class);
        asientoRepo = mock(AsientoRepository.class);
        asientoService = mock(AsientoService.class);

        AsientoImportParser parserFalso = in -> filasAServir;

        service = new AsientoImportService(parserFalso, cuentaContableRepo, asientoRepo, asientoService);
    }

    private MockMultipartFile archivo() {
        return new MockMultipartFile("archivo", "libro-diario.csv", "text/csv", new byte[]{1});
    }

    private CuentaContable cuenta(long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        return c;
    }

    @Test
    void previsualizarResuelveCodigoYParseaImportes() {
        when(cuentaContableRepo.findByCodigo("1.1.2004.01")).thenReturn(Optional.of(cuenta(1L, "1.1.2004.01")));
        filasAServir = List.of(new AsientoImportFilaCruda(2, "100", "13/01/2026", "1.1.2004.01", "1.234,50", "", "Cobro"));

        List<AsientoImportFilaDto> resultado = service.previsualizar(archivo());

        assertThat(resultado.get(0).errores()).isEmpty();
        assertThat(resultado.get(0).cuentaContableId()).isEqualTo(1L);
        assertThat(resultado.get(0).debe()).isEqualByComparingTo("1234.50");
        assertThat(resultado.get(0).haber()).isEqualByComparingTo("0");
    }

    @Test
    void previsualizarCodigoNoEncontradoGeneraError() {
        when(cuentaContableRepo.findByCodigo("9.9.9999")).thenReturn(Optional.empty());
        filasAServir = List.of(new AsientoImportFilaCruda(2, "100", "13/01/2026", "9.9.9999", "100", "", ""));

        List<AsientoImportFilaDto> resultado = service.previsualizar(archivo());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("no encontrado en el plan de cuentas"));
    }

    @Test
    void previsualizarDebeYHaberALaVezGeneraError() {
        when(cuentaContableRepo.findByCodigo("1.1")).thenReturn(Optional.of(cuenta(1L, "1.1")));
        filasAServir = List.of(new AsientoImportFilaCruda(2, "100", "13/01/2026", "1.1", "100", "100", ""));

        List<AsientoImportFilaDto> resultado = service.previsualizar(archivo());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("no puede tener debe y haber a la vez"));
    }

    @Test
    void confirmarAgrupaPorNumeroOriginalYCreaUnSoloAsiento() {
        CuentaContable caja = cuenta(1L, "1.1.1");
        CuentaContable ventas = cuenta(2L, "4.1.1");
        AsientoImportFilaDto linea1 = new AsientoImportFilaDto(2, "100", LocalDate.of(2026, 1, 13), 1L, "1.1.1",
                BigDecimal.valueOf(1000), BigDecimal.ZERO, "Cobro venta", List.of());
        AsientoImportFilaDto linea2 = new AsientoImportFilaDto(3, "100", LocalDate.of(2026, 1, 13), 2L, "4.1.1",
                BigDecimal.ZERO, BigDecimal.valueOf(1000), "Cobro venta", List.of());

        when(asientoRepo.existsByFechaAndDescripcionAndEstado(LocalDate.of(2026, 1, 13),
                "Importación histórica — Asiento Excel N° 100", EstadoDocumento.CONFIRMADO)).thenReturn(false);
        Asiento borrador = new Asiento();
        borrador.setId(50L);
        Asiento confirmado = new Asiento();
        confirmado.setId(50L);
        confirmado.setNumero(1L);
        when(asientoService.crearBorrador(any())).thenReturn(borrador);
        when(asientoService.confirmar(50L)).thenReturn(confirmado);

        AsientoImportResultado resultado = service.confirmar(9L, List.of(linea1, linea2));

        assertThat(resultado.creados()).hasSize(1);
        assertThat(resultado.creados().get(0).cantidadLineas()).isEqualTo(2);
        assertThat(resultado.rechazadas()).isEmpty();
        verify(asientoService, org.mockito.Mockito.times(1)).crearBorrador(any());
    }

    @Test
    void confirmarRechazaTodoElGrupoSiUnaLineaTieneError() {
        AsientoImportFilaDto lineaOk = new AsientoImportFilaDto(2, "100", LocalDate.of(2026, 1, 13), 1L, "1.1.1",
                BigDecimal.valueOf(1000), BigDecimal.ZERO, "Cobro", List.of());
        AsientoImportFilaDto lineaError = new AsientoImportFilaDto(3, "100", LocalDate.of(2026, 1, 13), null, "9.9",
                BigDecimal.ZERO, BigDecimal.valueOf(1000), "Cobro", List.of("Código de cuenta '9.9' no encontrado en el plan de cuentas"));

        AsientoImportResultado resultado = service.confirmar(9L, List.of(lineaOk, lineaError));

        assertThat(resultado.creados()).isEmpty();
        assertThat(resultado.rechazadas()).hasSize(2);
        assertThat(resultado.rechazadas().get(0).errores()).anyMatch(e -> e.contains("otra línea del asiento N° 100 tiene error"));
        verify(asientoService, never()).crearBorrador(any());
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        AsientoImportFilaDto linea = new AsientoImportFilaDto(2, "100", LocalDate.of(2026, 1, 13), 1L, "1.1.1",
                BigDecimal.valueOf(1000), BigDecimal.ZERO, "Cobro", List.of());
        when(asientoRepo.existsByFechaAndDescripcionAndEstado(LocalDate.of(2026, 1, 13),
                "Importación histórica — Asiento Excel N° 100", EstadoDocumento.CONFIRMADO)).thenReturn(true);

        AsientoImportResultado resultado = service.confirmar(9L, List.of(linea));

        assertThat(resultado.creados()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(asientoService, never()).crearBorrador(any());
    }

    @Test
    void confirmarSiConfirmarAsientoFallaOtroGrupoSigueProcesandose() {
        AsientoImportFilaDto grupo1 = new AsientoImportFilaDto(2, "100", LocalDate.of(2026, 1, 13), 1L, "1.1.1",
                BigDecimal.valueOf(500), BigDecimal.ZERO, "Desbalanceado", List.of());
        AsientoImportFilaDto grupo2linea1 = new AsientoImportFilaDto(3, "200", LocalDate.of(2026, 1, 14), 1L, "1.1.1",
                BigDecimal.valueOf(1000), BigDecimal.ZERO, "Ok", List.of());
        AsientoImportFilaDto grupo2linea2 = new AsientoImportFilaDto(4, "200", LocalDate.of(2026, 1, 14), 2L, "4.1.1",
                BigDecimal.ZERO, BigDecimal.valueOf(1000), "Ok", List.of());

        when(asientoRepo.existsByFechaAndDescripcionAndEstado(any(), any(), any())).thenReturn(false);
        Asiento borradorMalo = new Asiento();
        borradorMalo.setId(60L);
        Asiento borradorBueno = new Asiento();
        borradorBueno.setId(61L);
        Asiento confirmadoBueno = new Asiento();
        confirmadoBueno.setId(61L);
        confirmadoBueno.setNumero(1L);

        when(asientoService.crearBorrador(any())).thenReturn(borradorMalo, borradorBueno);
        when(asientoService.confirmar(60L)).thenThrow(new NegocioException("ASIENTO_DESBALANCEADO", "No balancea"));
        when(asientoService.confirmar(61L)).thenReturn(confirmadoBueno);

        AsientoImportResultado resultado = service.confirmar(9L, List.of(grupo1, grupo2linea1, grupo2linea2));

        assertThat(resultado.creados()).hasSize(1);
        assertThat(resultado.rechazadas()).hasSize(1);
        assertThat(resultado.rechazadas().get(0).errores()).anyMatch(e -> e.contains("No se pudo confirmar el asiento N° 100"));
    }

    @Test
    void confirmarOrdenaGruposPorFechaAscendente() {
        AsientoImportFilaDto grupoTarde = new AsientoImportFilaDto(2, "200", LocalDate.of(2026, 3, 1), 1L, "1.1.1",
                BigDecimal.valueOf(100), BigDecimal.ZERO, "", List.of());
        AsientoImportFilaDto grupoTemprano = new AsientoImportFilaDto(3, "100", LocalDate.of(2026, 1, 1), 1L, "1.1.1",
                BigDecimal.valueOf(100), BigDecimal.ZERO, "", List.of());

        when(asientoRepo.existsByFechaAndDescripcionAndEstado(any(), any(), any())).thenReturn(false);
        Asiento b1 = new Asiento();
        b1.setId(1L);
        Asiento c1 = new Asiento();
        c1.setId(1L);
        c1.setNumero(1L);
        when(asientoService.crearBorrador(any())).thenReturn(b1);
        when(asientoService.confirmar(anyLong())).thenReturn(c1);

        java.util.List<String> ordenLlamadas = new java.util.ArrayList<>();
        org.mockito.Mockito.doAnswer(inv -> {
            var req = (com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest) inv.getArgument(0);
            ordenLlamadas.add(req.descripcion());
            return b1;
        }).when(asientoService).crearBorrador(any());

        service.confirmar(9L, List.of(grupoTarde, grupoTemprano));

        assertThat(ordenLlamadas).containsExactly(
                "Importación histórica — Asiento Excel N° 100",
                "Importación histórica — Asiento Excel N° 200");
    }
}
