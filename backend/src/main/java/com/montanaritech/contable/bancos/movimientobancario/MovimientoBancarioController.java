package com.montanaritech.contable.bancos.movimientobancario;

import com.montanaritech.contable.bancos.movimientobancario.dto.AsociarMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.ContadorPendientesResponse;
import com.montanaritech.contable.bancos.movimientobancario.dto.CorregirMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.DescartarMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.ImputarMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.MovimientoBancarioResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movimientos-bancarios")
@RequiredArgsConstructor
@Tag(name = "MovimientoBancario")
public class MovimientoBancarioController {

    private final MovimientoBancarioService service;
    private final MovimientoBancarioMapper mapper;

    @GetMapping
    public Page<MovimientoBancarioResponse> listar(
            @RequestParam(required = false) Long cuentaBancariaId,
            @RequestParam(required = false) EstadoMovimientoBancario estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            Pageable p) {
        return service.listar(cuentaBancariaId, estado, fechaDesde, fechaHasta, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public MovimientoBancarioResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @GetMapping("/pendientes/contador")
    public ContadorPendientesResponse contarPendientes(@RequestParam(required = false) Long cuentaBancariaId) {
        return new ContadorPendientesResponse(service.contarPendientes(cuentaBancariaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse crear(@Valid @RequestBody CrearMovimientoBancarioRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse corregir(@PathVariable Long id, @Valid @RequestBody CorregirMovimientoBancarioRequest req) {
        return mapper.aResponse(service.corregir(id, req));
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id));
    }

    @PatchMapping("/{id}/imputar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse imputar(@PathVariable Long id, @Valid @RequestBody ImputarMovimientoBancarioRequest req) {
        return mapper.aResponse(service.imputar(id, req.cuentaContableId()));
    }

    @PatchMapping("/{id}/asociar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse asociar(@PathVariable Long id, @Valid @RequestBody AsociarMovimientoBancarioRequest req) {
        return mapper.aResponse(service.asociar(id, req.asientoNumero()));
    }

    @PatchMapping("/{id}/descartar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public MovimientoBancarioResponse descartar(@PathVariable Long id, @Valid @RequestBody DescartarMovimientoBancarioRequest req) {
        return mapper.aResponse(service.descartar(id, req.motivo()));
    }
}
