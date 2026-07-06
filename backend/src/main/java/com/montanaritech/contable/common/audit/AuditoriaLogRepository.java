package com.montanaritech.contable.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {
}
