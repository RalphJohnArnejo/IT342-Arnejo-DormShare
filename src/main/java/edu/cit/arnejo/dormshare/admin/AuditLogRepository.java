package edu.cit.arnejo.dormshare.admin;

import edu.cit.arnejo.dormshare.admin.entity.AuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
