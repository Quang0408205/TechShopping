package com.example.Tech.repository.user;

/**
 * Projection: one (user id, role name) pair, used to load the roles of a whole page of users in one query.
 */
public interface UserRoleName {

    Long getUserId();

    String getRoleName();
}
