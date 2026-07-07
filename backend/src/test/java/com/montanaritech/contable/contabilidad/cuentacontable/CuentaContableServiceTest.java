package com.montanaritech.contable.contabilidad.cuentacontable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableCrearRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableEditarRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableNodo;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableResponse;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.rubro.Rubro;
import com.montanaritech.contable.maestros.rubro.RubroRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaContableServiceTest {

    @Mock
    private CuentaContableRepository repo;

    @Mock
    private CuentaContableMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private RubroRepository rubroRepo;

    @Mock
    private ProyectoRepository proyectoRepo;

    private CuentaContableService service;

    private Categoria categoriaActivo;
    private Rubro rubroCajaBancos;

    @BeforeEach
    void setUp() {
        service = new CuentaContableService(repo, mapper, auditoria, rubroRepo, proyectoRepo);

        categoriaActivo = new Categoria();
        categoriaActivo.setId(1L);
        categoriaActivo.setNombre("Activo");
        categoriaActivo.setTipo(Categoria.TipoCategoria.ACTIVO);

        rubroCajaBancos = new Rubro();
        rubroCajaBancos.setId(1L);
        rubroCajaBancos.setNombre("Caja y Bancos");
        rubroCajaBancos.setCategoria(categoriaActivo);
    }

    private CuentaContableCrearRequest crearRequestBase(Long padreId, boolean imputable, Long rubroId) {
        return new CuentaContableCrearRequest("1.1.01", "Banco Galicia CC", padreId, "ACTIVO", rubroId, imputable, "DEUDOR", null);
    }

    @Test
    void crearCuentaImputableConRubroValido() {
        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(rubroRepo.findById(1L)).thenReturn(Optional.of(rubroCajaBancos));
        when(repo.save(any(CuentaContable.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaContable creada = service.crear(crearRequestBase(null, true, 1L));

        assertThat(creada.getCodigo()).isEqualTo("1.1.01");
        assertThat(creada.isImputable()).isTrue();
        assertThat(creada.isActivo()).isTrue();
        assertThat(creada.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.ACTIVO);
    }

    @Test
    void crearImputableSinRubroFalla() {
        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(crearRequestBase(null, true, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("rubro");
    }

    @Test
    void crearMadreSinRubroEsValido() {
        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(repo.save(any(CuentaContable.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaContable creada = service.crear(crearRequestBase(null, false, null));

        assertThat(creada.isImputable()).isFalse();
        assertThat(creada.getRubro()).isNull();
    }

    @Test
    void crearConCodigoDuplicadoLanzaConflicto() {
        CuentaContable existente = new CuentaContable();
        existente.setId(1L);
        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.crear(crearRequestBase(null, false, null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void crearConNaturalezaDeRubroInconsistenteFalla() {
        Categoria categoriaPasivo = new Categoria();
        categoriaPasivo.setId(2L);
        categoriaPasivo.setTipo(Categoria.TipoCategoria.PASIVO);
        Rubro rubroDeudas = new Rubro();
        rubroDeudas.setId(2L);
        rubroDeudas.setCategoria(categoriaPasivo);

        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(rubroRepo.findById(2L)).thenReturn(Optional.of(rubroDeudas));

        assertThatThrownBy(() -> service.crear(crearRequestBase(null, true, 2L)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("naturaleza");
    }

    @Test
    void crearHijaConNaturalezaDistintaDeLaMadreFalla() {
        CuentaContable madre = new CuentaContable();
        madre.setId(1L);
        madre.setNaturaleza(Categoria.TipoCategoria.PASIVO);
        madre.setImputable(false);

        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(repo.findById(1L)).thenReturn(Optional.of(madre));

        assertThatThrownBy(() -> service.crear(crearRequestBase(1L, true, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("naturaleza");
    }

    @Test
    void crearHijaApagaImputableDeLaMadreAutomaticamente() {
        CuentaContable madre = new CuentaContable();
        madre.setId(1L);
        madre.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        madre.setImputable(true);

        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(repo.findById(1L)).thenReturn(Optional.of(madre));
        when(rubroRepo.findById(1L)).thenReturn(Optional.of(rubroCajaBancos));
        when(repo.save(any(CuentaContable.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(crearRequestBase(1L, true, 1L));

        assertThat(madre.isImputable()).isFalse();
    }

    @Test
    void crearConPadreInexistenteLanzaNoEncontrado() {
        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(crearRequestBase(99L, false, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearExcediendoProfundidadMaximaFalla() {
        CuentaContable nivel5 = construirCadena(5);

        when(repo.findByCodigo("1.1.01")).thenReturn(Optional.empty());
        when(repo.findById(5L)).thenReturn(Optional.of(nivel5));

        assertThatThrownBy(() -> service.crear(crearRequestBase(5L, false, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("niveles");
    }

    @Test
    void editarIntentarImputableConHijasFalla() {
        CuentaContable c = new CuentaContable();
        c.setId(1L);
        c.setCodigo("1.1");
        c.setNombre("Caja y Bancos");
        c.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        c.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);
        c.setImputable(false);

        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.existsByPadreId(1L)).thenReturn(true);

        CuentaContableEditarRequest req = new CuentaContableEditarRequest(
                "1.1", "Caja y Bancos", null, "ACTIVO", null, true, "DEUDOR", null);

        assertThatThrownBy(() -> service.editar(1L, req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("hijas");
    }

    @Test
    void moverCuentaBajoSiMismaEsCiclo() {
        CuentaContable c = new CuentaContable();
        c.setId(1L);
        c.setCodigo("1.1");
        c.setNombre("X");
        c.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        c.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);
        c.setImputable(true);

        when(repo.findById(1L)).thenReturn(Optional.of(c));

        CuentaContableEditarRequest req = new CuentaContableEditarRequest(
                "1.1", "X", 1L, "ACTIVO", null, true, "DEUDOR", null);

        assertThatThrownBy(() -> service.editar(1L, req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("sí misma");
    }

    @Test
    void moverCuentaBajoUnDescendienteEsCiclo() {
        CuentaContable abuelo = new CuentaContable();
        abuelo.setId(1L);
        abuelo.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        abuelo.setImputable(false);
        abuelo.setCodigo("1");
        abuelo.setNombre("Activo");
        abuelo.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        CuentaContable padre = new CuentaContable();
        padre.setId(2L);
        padre.setPadre(abuelo);
        padre.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        padre.setImputable(false);
        padre.setCodigo("1.1");
        padre.setNombre("Caja y Bancos");
        padre.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        when(repo.findById(1L)).thenReturn(Optional.of(abuelo));
        when(repo.findById(2L)).thenReturn(Optional.of(padre));

        CuentaContableEditarRequest req = new CuentaContableEditarRequest(
                "1", "Activo", 2L, "ACTIVO", null, false, "DEUDOR", null);

        assertThatThrownBy(() -> service.editar(1L, req))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void moverCuentaDeRamaValidaRecalculaJerarquiaEnElArbol() {
        CuentaContable ramaA = new CuentaContable();
        ramaA.setId(1L);
        ramaA.setCodigo("1");
        ramaA.setNombre("Activo");
        ramaA.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        ramaA.setImputable(false);
        ramaA.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        CuentaContable ramaB = new CuentaContable();
        ramaB.setId(2L);
        ramaB.setCodigo("1.2");
        ramaB.setNombre("Otros Créditos");
        ramaB.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        ramaB.setImputable(true);
        ramaB.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        CuentaContable hoja = new CuentaContable();
        hoja.setId(3L);
        hoja.setCodigo("1.1.05");
        hoja.setNombre("Cuenta a mover");
        hoja.setPadre(ramaA);
        hoja.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        hoja.setImputable(true);
        hoja.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        when(repo.findById(3L)).thenReturn(Optional.of(hoja));
        when(repo.findById(2L)).thenReturn(Optional.of(ramaB));
        when(repo.findByPadreId(3L)).thenReturn(List.of());
        when(mapper.aResponse(any(CuentaContable.class))).thenReturn(mockResponse());

        CuentaContableEditarRequest req = new CuentaContableEditarRequest(
                "1.2.01", "Cuenta a mover", 2L, "ACTIVO", null, false, "DEUDOR", null);

        service.editar(3L, req);

        assertThat(hoja.getPadre()).isEqualTo(ramaB);
        assertThat(ramaB.isImputable()).isFalse();
    }

    @Test
    void desactivarMadreConHijasActivasFalla() {
        CuentaContable madre = new CuentaContable();
        madre.setId(1L);
        madre.setImputable(false);

        when(repo.findById(1L)).thenReturn(Optional.of(madre));
        when(repo.existsByPadreIdAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.desactivar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void desactivarCuentaSinHijasActivasEsValida() {
        CuentaContable c = new CuentaContable();
        c.setId(1L);
        c.setImputable(true);

        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(mapper.aResponse(any(CuentaContable.class))).thenReturn(mockResponse());

        service.desactivar(1L);

        assertThat(c.isActivo()).isFalse();
    }

    @Test
    void eliminarConHijasFalla() {
        CuentaContable c = new CuentaContable();
        c.setId(1L);

        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.existsByPadreId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarSinHijasBorraLaEntidad() {
        CuentaContable c = new CuentaContable();
        c.setId(1L);

        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.existsByPadreId(1L)).thenReturn(false);
        when(mapper.aResponse(any(CuentaContable.class))).thenReturn(mockResponse());

        service.eliminar(1L);
    }

    @Test
    void arbolArmaJerarquiaDesdeListaPlana() {
        CuentaContable raiz = new CuentaContable();
        raiz.setId(1L);
        raiz.setCodigo("1");
        raiz.setNombre("Activo");
        raiz.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        raiz.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);
        raiz.setImputable(false);

        CuentaContable hija = new CuentaContable();
        hija.setId(2L);
        hija.setCodigo("1.1");
        hija.setNombre("Caja y Bancos");
        hija.setPadre(raiz);
        hija.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
        hija.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);
        hija.setImputable(true);

        when(repo.findAllByOrderByCodigoAsc()).thenReturn(List.of(raiz, hija));

        List<CuentaContableNodo> arbol = service.arbol();

        assertThat(arbol).hasSize(1);
        assertThat(arbol.get(0).codigo()).isEqualTo("1");
        assertThat(arbol.get(0).hijos()).hasSize(1);
        assertThat(arbol.get(0).hijos().get(0).codigo()).isEqualTo("1.1");
    }

    private CuentaContable construirCadena(int niveles) {
        CuentaContable actual = null;
        for (int i = 1; i <= niveles; i++) {
            CuentaContable nueva = new CuentaContable();
            nueva.setId((long) i);
            nueva.setCodigo("nivel" + i);
            nueva.setNaturaleza(Categoria.TipoCategoria.ACTIVO);
            nueva.setImputable(false);
            nueva.setPadre(actual);
            actual = nueva;
        }
        return actual;
    }

    private CuentaContableResponse mockResponse() {
        return new CuentaContableResponse(1L, "1.1", "x", null, null, "ACTIVO", null, null, true, "DEUDOR", true, Set.of());
    }
}
