package com.montanaritech.contable.common.tenant;

import com.montanaritech.contable.common.tenant.dto.TenantActualizarRequest;
import com.montanaritech.contable.common.tenant.dto.TenantResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nombre de empresa y slot de logo del tenant actual (F7.1), usados por
 * {@link com.montanaritech.contable.common.reporte.ReportExportService} en
 * el encabezado de todos los reportes exportables.
 */
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant")
public class TenantController {

    private final TenantRepository tenantRepository;

    @GetMapping
    public TenantResponse consultar() {
        return aResponse(tenantActual());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public TenantResponse actualizar(@Valid @RequestBody TenantActualizarRequest request) {
        Tenant tenant = tenantActual();
        tenant.setNombre(request.nombre());
        tenant.setLogoClasspath(request.logoClasspath());
        return aResponse(tenantRepository.save(tenant));
    }

    private Tenant tenantActual() {
        return tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant no encontrado: " + TenantContext.getTenantId()));
    }

    private TenantResponse aResponse(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getNombre(), tenant.getCuit(), tenant.getLogoClasspath());
    }
}
