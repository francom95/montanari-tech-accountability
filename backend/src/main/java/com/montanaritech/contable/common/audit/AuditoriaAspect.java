package com.montanaritech.contable.common.audit;

import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    @AfterReturning(pointcut = "@annotation(auditado)", returning = "resultado")
    public void auditarDespuesDeCrear(Auditado auditado, Object resultado) {
        auditoriaService.registrar(
                auditado.accion(),
                auditado.entidadTipo(),
                extraerId(resultado),
                null,
                resultado);
    }

    private Long extraerId(Object resultado) {
        if (resultado == null) {
            return null;
        }
        try {
            Method getId = resultado.getClass().getMethod("getId");
            return (Long) getId.invoke(resultado);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
