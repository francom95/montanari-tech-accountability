package com.montanaritech.contable.contabilidad.asiento.apertura;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F10.3: arma el asiento de apertura del sistema (saldos de cuentas
 * patrimoniales al arranque, 01/09/2025) a partir del EECC.pdf real de "ST
 * Surfing Reservations SRL" (balance cerrado 31/08/2025), mapeado 1:1 contra
 * el plan de cuentas real (validado con el usuario, ver
 * {@code outputs/F10_3_...md} para la tabla completa de mapeo).
 *
 * <p>Solo arma el {@code AsientoCrearRequest} y delega en
 * {@link AsientoService#crearBorrador(AsientoCrearRequest, OrigenAsiento, boolean, String)}
 * con {@code origen=APERTURA} — queda en BORRADOR a propósito (checkpoint
 * humano del contador antes de confirmar el asiento real de arranque).
 */
@Service
@RequiredArgsConstructor
public class AsientoAperturaService {

    private static final LocalDate FECHA_APERTURA = LocalDate.of(2025, 9, 1);
    private static final String DESCRIPCION = "Asiento de apertura — saldos iniciales al 01/09/2025 (EECC 31/08/2025)";

    private final CuentaContableRepository cuentaContableRepo;
    private final MonedaRepository monedaRepo;
    private final AsientoService asientoService;

    /**
     * Línea del mapeo EECC→plan de cuentas (codigo, monto, esDebe, leyenda).
     * Verificado que Σdebe = Σhaber = 10.260.910,91 (activo 9.766.116,15 +
     * ajuste ejercicios anteriores 494.794,76, contra pasivo 7.113.860,77 +
     * capital 100.000,00 + ajuste al capital 373.098,29 + resultados
     * acumulados 2.673.951,85).
     */
    private record LineaApertura(String codigo, BigDecimal monto, boolean esDebe, String leyenda) {}

    private static final List<LineaApertura> LINEAS = List.of(
            // Activo — disponibilidades
            new LineaApertura("1.1.2016", new BigDecimal("109274.68"), true, "Caja al 31/08/2025"),
            new LineaApertura("1.1.2001", new BigDecimal("2584068.07"), true, "Banco Galicia CC al 31/08/2025"),
            new LineaApertura("1.1.2017", new BigDecimal("817.00"), true, "Banco Nación al 31/08/2025"),
            new LineaApertura("1.1.2002", new BigDecimal("126522.00"), true,
                    "Banco Galicia USD al 31/08/2025 (equivalente en pesos; saldo bancario en USD pendiente de TC real)"),
            new LineaApertura("1.1.2018", new BigDecimal("10282.68"), true, "Banco Provincia al 31/08/2025"),
            // Activo — créditos por ventas
            new LineaApertura("1.1.2004", new BigDecimal("5396077.62"), true, "Deudores por Servicios Prestados al 31/08/2025"),
            new LineaApertura("1.1.2013", new BigDecimal("1101150.00"), true, "Anticipo a Proveedores al 31/08/2025"),
            // Activo — otros créditos
            new LineaApertura("1.1.2019", new BigDecimal("192890.04"), true, "Retenciones de Ganancias sufridas al 31/08/2025"),
            new LineaApertura("1.1.2011", new BigDecimal("85620.22"), true, "Anticipo de Ganancias al 31/08/2025"),
            new LineaApertura("1.1.2009", new BigDecimal("159413.84"), true, "Ley 25413 crédito computable al 31/08/2025"),
            // Pasivo
            new LineaApertura("2.1.2018", new BigDecimal("289810.10"), false, "Proveedores al 31/08/2025"),
            new LineaApertura("2.1.2019", new BigDecimal("334.68"), false, "Seguro de Vida Obligatorio a pagar al 31/08/2025"),
            new LineaApertura("2.1.2020", new BigDecimal("32919.00"), false, "ART a pagar al 31/08/2025"),
            new LineaApertura("2.1.2021", new BigDecimal("2000000.00"), false, "Honorarios Directores a pagar al 31/08/2025"),
            new LineaApertura("2.1.2008", new BigDecimal("4086555.91"), false, "IVA Débito Fiscal a pagar al 31/08/2025"),
            new LineaApertura("2.1.2022", new BigDecimal("290500.88"), false, "Plan de Facilidades a pagar al 31/08/2025"),
            new LineaApertura("2.1.2010", new BigDecimal("413740.20"), false, "IIBB a pagar al 31/08/2025 (901 + 902 combinado)"),
            // Patrimonio Neto
            new LineaApertura("3.1.2001", new BigDecimal("100000.00"), false, "Capital Social al 31/08/2025"),
            new LineaApertura("3.1.2003", new BigDecimal("373098.29"), false, "Ajuste al Capital al 31/08/2025"),
            new LineaApertura("3.1.2002", new BigDecimal("2673951.85"), false, "Resultados Acumulados al 31/08/2025"),
            new LineaApertura("3.1.2004", new BigDecimal("494794.76"), true, "Ajuste Ejercicios Anteriores al 31/08/2025")
    );

    /** Arma y guarda el BORRADOR del asiento de apertura (no lo confirma — ver Javadoc de la clase). */
    @Transactional
    public Asiento generarBorrador() {
        Long monedaArsId = monedaRepo.findByCodigo("ARS")
                .orElseThrow(() -> new NegocioException("MONEDA_NO_ENCONTRADA", "No se encontró la moneda ARS"))
                .getId();

        List<AsientoLineaRequest> lineas = new ArrayList<>();
        for (LineaApertura l : LINEAS) {
            Long cuentaId = cuentaContableRepo.findByCodigo(l.codigo())
                    .orElseThrow(() -> new NegocioException("CUENTA_NO_ENCONTRADA",
                            "No se encontró la cuenta contable " + l.codigo() + " para el asiento de apertura"))
                    .getId();
            BigDecimal debe = l.esDebe() ? l.monto() : BigDecimal.ZERO;
            BigDecimal haber = l.esDebe() ? BigDecimal.ZERO : l.monto();
            lineas.add(new AsientoLineaRequest(cuentaId, debe, haber, monedaArsId, null, null, l.leyenda(),
                    null, null, null, null, null));
        }

        AsientoCrearRequest req = new AsientoCrearRequest(FECHA_APERTURA, DESCRIPCION, null, lineas);
        return asientoService.crearBorrador(req, OrigenAsiento.APERTURA, false, null);
    }
}
