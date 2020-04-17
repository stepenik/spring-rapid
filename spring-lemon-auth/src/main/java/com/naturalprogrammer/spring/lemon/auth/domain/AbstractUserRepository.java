package com.naturalprogrammer.spring.lemon.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.Optional;

/**
 * Abstract UserRepository interface
 * 
 * @see <a href="http://stackoverflow.com/questions/27545276/how-to-implement-a-spring-data-repository-for-a-mappedsuperclass">how-to-implement-a-spring-data-repository-for-a-mappedsuperclass</a>
 * @author Sanjay Patel
 */
@NoRepositoryBean
public interface AbstractUserRepository
	<U extends AbstractUser<ID>, ID extends Serializable>
extends JpaRepository<U, ID> {
	
	Optional<U> findByEmail(String email);
}
