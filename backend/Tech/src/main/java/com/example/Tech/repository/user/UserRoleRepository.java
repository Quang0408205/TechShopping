package com.example.Tech.repository.user;

import com.example.Tech.entity.user.UserRole;
import com.example.Tech.entity.user.UserRoleId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findAllByIdUserId(Long userId);

    /** Role names of a user, sorted alphabetically (the "roles" claim and API responses use this). */
    @Query("select r.name from UserRole ur join ur.role r where ur.id.userId = :userId order by r.name")
    List<String> findRoleNamesByUserId(Long userId);

    /** Role names of several users in one query (admin user list), sorted by role name. */
    @Query("select ur.id.userId as userId, r.name as roleName from UserRole ur join ur.role r "
            + "where ur.id.userId in :userIds order by r.name")
    List<UserRoleName> findRoleNamesByUserIds(Collection<Long> userIds);

    boolean existsByRoleName(String roleName);

    /** Users holding the role that can still log in (not soft-deleted, not deactivated). */
    @Query("select count(ur) from UserRole ur join ur.user u where ur.role.name = :roleName "
            + "and u.deletedAt is null and (u.active is null or u.active = true)")
    long countEnabledUsersWithRole(String roleName);
}
