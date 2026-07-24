package com.montanaritech.contable.vencimientos;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** VENCIDO se deriva en lectura (F8.1), nunca se persiste — mismo criterio que EstadoVencimiento de F4.5. */
class VencimientoMapperTest {

    private final VencimientoMapper mapper = new VencimientoMapper();

    private Vencimiento base(LocalDate fecha, EstadoVencimientoObligacion estado) {
        Vencimiento v = new Vencimiento();
        v.setId(1L);
        v.setDescripcion("IVA julio");
        v.setTipo(TipoVencimiento.IVA);
        v.setFecha(fecha);
        v.setImporteEstimado(new BigDecimal("1000"));
        Moneda moneda = new Moneda();
        moneda.setId(5L);
        moneda.setCodigo("ARS");
        v.setMoneda(moneda);
        v.setRecurrencia(TipoRecurrencia.UNICA);
        v.setEstado(estado);
        v.setOrigenGeneracion(OrigenGeneracionVencimiento.MANUAL);
        return v;
    }

    @Test
    void pendienteConFechaPasadaSeMuestraComoVencido() {
        Vencimiento v = base(LocalDate.now().minusDays(3), EstadoVencimientoObligacion.PENDIENTE);

        assertThat(mapper.aResponse(v).estado()).isEqualTo("VENCIDO");
    }

    @Test
    void pendienteConFechaFuturaSeMuestraComoPendiente() {
        Vencimiento v = base(LocalDate.now().plusDays(3), EstadoVencimientoObligacion.PENDIENTE);

        assertThat(mapper.aResponse(v).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void pagadoConFechaPasadaNoSeMuestraComoVencido() {
        Vencimiento v = base(LocalDate.now().minusDays(3), EstadoVencimientoObligacion.PAGADO);

        assertThat(mapper.aResponse(v).estado()).isEqualTo("PAGADO");
    }
}
