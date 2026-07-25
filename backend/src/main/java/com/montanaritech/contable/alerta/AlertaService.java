package com.montanaritech.contable.alerta;

import com.montanaritech.contable.alerta.dto.AlertaResponse;
import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura y marcado de {@link Alerta} (F9.1). La sincronización en sí vive en
 * {@link MotorAlertasService}; este servicio solo expone lo que necesita el
 * frontend (listar, contar no leídas, marcar leída) y resuelve "leída" por
 * usuario contra {@link AlertaLectura}.
 */
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository repo;
    private final AlertaLecturaRepository lecturaRepo;
    private final UsuarioRepository usuarioRepo;
    private final MotorAlertasService motor;

    @Transactional(readOnly = true)
    public Page<AlertaResponse> listar(EstadoAlerta estado, TipoAlerta tipo, Long usuarioId, Pageable pageable) {
        Page<Alerta> pagina = repo.buscar(estado, tipo, pageable);
        List<Long> ids = pagina.getContent().stream().map(Alerta::getId).toList();
        Set<Long> leidas = ids.isEmpty() ? Set.of() : new HashSet<>(lecturaRepo.findAlertaIdsLeidasPorUsuario(ids, usuarioId));
        return pagina.map(a -> aResponse(a, leidas.contains(a.getId())));
    }

    @Transactional(readOnly = true)
    public long contarActivasNoLeidas(Long usuarioId) {
        return repo.contarActivasNoLeidasPorUsuario(usuarioId);
    }

    @Transactional
    public void marcarLeida(Long alertaId, Long usuarioId) {
        if (lecturaRepo.findByAlerta_IdAndUsuario_Id(alertaId, usuarioId).isPresent()) {
            return;
        }
        Alerta alerta = repo.findById(alertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta " + alertaId + " no encontrada"));
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + usuarioId + " no encontrado"));
        AlertaLectura lectura = new AlertaLectura();
        lectura.setAlerta(alerta);
        lectura.setUsuario(usuario);
        lectura.setLeidaEn(Instant.now());
        lecturaRepo.save(lectura);
    }

    /** Disparo manual (admin) para no esperar al cron diario — mismo tenant ya habilitado por el request HTTP. */
    @Transactional
    public void sincronizarManual() {
        motor.sincronizar();
    }

    private AlertaResponse aResponse(Alerta a, boolean leida) {
        return new AlertaResponse(a.getId(), a.getTipo().name(), a.getSeveridad().name(), a.getMensaje(),
                a.getEntidadTipo().name(), a.getEntidadRefId(), a.getFecha(), a.getEstado().name(), leida);
    }
}
