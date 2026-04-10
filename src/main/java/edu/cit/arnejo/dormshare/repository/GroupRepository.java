package edu.cit.arnejo.dormshare.repository;

import edu.cit.arnejo.dormshare.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByInviteCode(String inviteCode);
}
