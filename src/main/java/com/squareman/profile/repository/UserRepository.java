package com.squareman.profile.repository;

import org.springframework.data.repository.CrudRepository;

import com.squareman.profile.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {
	User findByUsername(String userName);
	User findByEmail(String email);

}
