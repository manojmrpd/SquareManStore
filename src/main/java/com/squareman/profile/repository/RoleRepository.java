package com.squareman.profile.repository;

import org.springframework.data.repository.CrudRepository;

import com.squareman.profile.entity.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {
	Role findByname(String name);
	
}
