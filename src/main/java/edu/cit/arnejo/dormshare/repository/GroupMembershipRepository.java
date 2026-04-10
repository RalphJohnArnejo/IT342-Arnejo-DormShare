package edu.cit.arnejo.dormshare.repository;

import edu.cit.arnejo.dormshare.entity.GroupMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembershipEntity, Long> {
    List<GroupMembershipEntity> findByUserId(Long userId);
    List<GroupMembershipEntity> findByGroupId(Long groupId);
    Optional<GroupMembershipEntity> findByUserIdAndGroupId(Long userId, Long groupId);
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
    void deleteByUserIdAndGroupId(Long userId, Long groupId);
    long countByGroupId(Long groupId);
}
