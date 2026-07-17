package com.montanaritech.contable.contabilidad.cuentacontable;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.AbstractIntegrationTest;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.categoria.CategoriaRepository;
import com.montanaritech.contable.maestros.rubro.RubroRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * F3.3 — verifica el seed del plan de cuentas (migración V17) sobre MySQL real.
 * No prueba lógica de negocio nueva: confirma que la carga inicial respeta las
 * reglas innegociables del paso (madres agrupan y no llevan rubro; solo las
 * imputables reciben rubro; jerarquía sin huérfanos) y que los datos del Excel
 * quedaron transcriptos con su naturaleza y saldo esperado.
 */
class SeedPlanDeCuentasIT extends AbstractIntegrationTest {

    @Autowired
    private CuentaContableRepository cuentaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private RubroRepository rubroRepository;

    @Test
    void elSeedCargaElPlanCompletoRespetandoLasReglas() {
        List<CuentaContable> cuentas = cuentaRepository.findAllByOrderByCodigoAsc();

        // Conteos esperados del Excel "Plan de Cuentas" (6 categorías: las 5
        // clásicas + "Otros Resultados" para la rama 6 de signo mixto).
        assertThat(categoriaRepository.count()).isEqualTo(6);
        assertThat(rubroRepository.count()).isEqualTo(14);
        // 72 = 71 del Excel + la madre intermedia "3.1 Patrimonio Neto" agregada.
        assertThat(cuentas).hasSize(72);
        assertThat(cuentas.stream().filter(CuentaContable::isImputable)).hasSize(57);
        assertThat(cuentas.stream().filter(c -> !c.isImputable())).hasSize(15);

        // Regla: solo las imputables llevan rubro; las madre nunca.
        assertThat(cuentas.stream()
                .filter(CuentaContable::isImputable)
                .filter(c -> c.getRubro() == null))
                .as("imputables sin rubro")
                .isEmpty();
        assertThat(cuentas.stream()
                .filter(c -> !c.isImputable())
                .filter(c -> c.getRubro() != null))
                .as("madres con rubro")
                .isEmpty();

        // Regla: toda cuenta salvo las 6 raíces cuelga de una madre.
        assertThat(cuentas.stream()
                .filter(c -> c.getPadre() == null)
                .map(CuentaContable::getCodigo))
                .containsExactlyInAnyOrder("1", "2", "3", "4", "5", "6");

        // Regla: ninguna cuenta imputable puede tener hijos.
        assertThat(cuentas.stream()
                .filter(c -> c.getPadre() != null && c.getPadre().isImputable()))
                .as("cuentas colgadas de una imputable")
                .isEmpty();

        // Regla: saldo esperado coherente con la naturaleza. Se excluyen las
        // cuentas de la sección "Otros Resultados" (la madre 6 es
        // OTROS_RESULTADOS; sus hijas son RP/RN y se verifican puntualmente
        // más abajo).
        assertThat(cuentas)
                .filteredOn(c -> c.getNaturaleza() != Categoria.TipoCategoria.OTROS_RESULTADOS)
                .allSatisfy(c -> {
                    CuentaContable.SaldoEsperado esperado = switch (c.getNaturaleza()) {
                        case ACTIVO, RN -> CuentaContable.SaldoEsperado.DEUDOR;
                        case PASIVO, PN, RP -> CuentaContable.SaldoEsperado.ACREEDOR;
                        case OTROS_RESULTADOS -> throw new IllegalStateException("OTROS_RESULTADOS fue filtrado");
                    };
                    assertThat(c.getSaldoEsperado())
                            .as("saldo esperado de %s", c.getCodigo())
                            .isEqualTo(esperado);
                });

        // Casos puntuales: una imputable típica y su cadena madre.
        CuentaContable banco = cuentaRepository.findByCodigo("1.1.2001").orElseThrow();
        assertThat(banco.isImputable()).isTrue();
        assertThat(banco.getNombre()).isEqualTo("Banco Galicia CC");
        assertThat(banco.getRubro().getNombre()).isEqualTo("1. Caja y Bancos");
        assertThat(banco.getPadre().getCodigo()).isEqualTo("1.1");

        // Patrimonio Neto: se agregó la madre intermedia 3.1 (no está en el
        // Excel) para dar el nivel intermedio 3 -> 3.1 -> hojas, como Activo/Pasivo.
        CuentaContable pnIntermedia = cuentaRepository.findByCodigo("3.1").orElseThrow();
        assertThat(pnIntermedia.isImputable()).isFalse();
        assertThat(pnIntermedia.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.PN);
        assertThat(pnIntermedia.getPadre().getCodigo()).isEqualTo("3");
        CuentaContable capital = cuentaRepository.findByCodigo("3.1.2001").orElseThrow();
        assertThat(capital.getPadre().getCodigo()).isEqualTo("3.1");

        // Rama 6 "Otros Ingresos y Egresos": raíz propia (fuera de Resultado
        // Negativo). Solo la madre es OTROS_RESULTADOS; sus hijas se clasifican
        // RP/RN según su tipo y conservan su saldo real.
        CuentaContable seisRaiz = cuentaRepository.findByCodigo("6").orElseThrow();
        assertThat(seisRaiz.getPadre()).isNull();
        assertThat(seisRaiz.isImputable()).isFalse();
        assertThat(seisRaiz.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.OTROS_RESULTADOS);

        CuentaContable comisionesGanadas = cuentaRepository.findByCodigo("6.4001").orElseThrow();
        assertThat(comisionesGanadas.getNombre()).isEqualTo("Comisiones Ganadas");
        assertThat(comisionesGanadas.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.RP);
        assertThat(comisionesGanadas.getSaldoEsperado()).isEqualTo(CuentaContable.SaldoEsperado.ACREEDOR);
        assertThat(comisionesGanadas.getPadre().getCodigo()).isEqualTo("6");

        // ...y las comisiones bancarias son Resultado Negativo (deudoras) en la
        // misma sección madre.
        CuentaContable comisionesBancarias = cuentaRepository.findByCodigo("6.4003").orElseThrow();
        assertThat(comisionesBancarias.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.RN);
        assertThat(comisionesBancarias.getSaldoEsperado()).isEqualTo(CuentaContable.SaldoEsperado.DEUDOR);
        assertThat(comisionesBancarias.getPadre().getCodigo()).isEqualTo("6");
    }
}
