package com.montanaritech.contable.alerta;

import com.montanaritech.contable.alerta.dto.AlertaResponse;
import com.montanaritech.contable.alerta.dto.ContadorAlertasResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Alertas (F9.1): lectura por usuario, badge del header, sincronización manual (admin). */
@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Alerta")
public class AlertaController {

    private final AlertaService service;

    @GetMapping
    public Page<AlertaResponse> listar(
            @RequestParam(required = false) EstadoAlerta estado,
            @RequestParam(required = false) TipoAlerta tipo,
            Pageable pageable,
            Authentication authentication) {
        return service.listar(estado, tipo, usuarioId(authentication), pageable);
    }

    @GetMapping("/contador")
    public ContadorAlertasResponse contador(Authentication authentication) {
        return new ContadorAlertasResponse(service.contarActivasNoLeidas(usuarioId(authentication)));
    }

    @PostMapping("/{id}/marcar-leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, Authentication authentication) {
        service.marcarLeida(id, usuarioId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sincronizar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> sincronizar() {
        service.sincronizarManual();
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
