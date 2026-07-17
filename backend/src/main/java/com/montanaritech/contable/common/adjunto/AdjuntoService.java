package com.montanaritech.contable.common.adjunto;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adjuntos genéricos (F1.1 §6.5): guarda el archivo en filesystem (bajo
 * {@code app.adjuntos.directorio}, un subdirectorio por entidad) y la fila
 * de metadatos en {@code adjunto}. Nunca BLOB en MySQL. No valida tipo MIME
 * ni tamaño más allá del límite global de Spring (application.yml) — el
 * funcional solo pide "opcional", no restringe formatos.
 */
@Service
public class AdjuntoService {

    private final AdjuntoRepository repo;
    private final String directorioBase;

    public AdjuntoService(AdjuntoRepository repo, @Value("${app.adjuntos.directorio}") String directorioBase) {
        this.repo = repo;
        this.directorioBase = directorioBase;
    }

    @Transactional(readOnly = true)
    public List<Adjunto> listar(String entidadTipo, Long entidadId) {
        return repo.findByEntidadTipoAndEntidadIdOrderByIdAsc(entidadTipo, entidadId);
    }

    @Transactional
    public Adjunto subir(String entidadTipo, Long entidadId, MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new NegocioException("ADJUNTO_VACIO", "El archivo está vacío");
        }
        try {
            Path directorio = Path.of(directorioBase, entidadTipo, entidadId.toString());
            Files.createDirectories(directorio);

            String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
            String nombreGuardado = UUID.randomUUID() + "_" + nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destino = directorio.resolve(nombreGuardado);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            Adjunto a = new Adjunto();
            a.setEntidadTipo(entidadTipo);
            a.setEntidadId(entidadId);
            a.setNombreArchivo(nombreOriginal);
            a.setRuta(Path.of(entidadTipo, entidadId.toString(), nombreGuardado).toString());
            a.setMime(archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream");
            a.setTamanio(archivo.getSize());
            return repo.save(a);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el adjunto", e);
        }
    }

    @Transactional(readOnly = true)
    public Adjunto obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Adjunto " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public Resource descargar(Long id) {
        Adjunto a = obtener(id);
        try {
            Resource recurso = new UrlResource(Path.of(directorioBase, a.getRuta()).toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new RecursoNoEncontradoException("El archivo del adjunto " + id + " no está disponible");
            }
            return recurso;
        } catch (java.net.MalformedURLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Adjunto a = obtener(id);
        try {
            Files.deleteIfExists(Path.of(directorioBase, a.getRuta()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo del adjunto " + id, e);
        }
        repo.delete(a);
    }
}
