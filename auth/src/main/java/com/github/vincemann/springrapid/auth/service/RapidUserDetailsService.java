package com.github.vincemann.springrapid.auth.service;

import com.github.vincemann.aoplog.api.AopLoggable;
import com.github.vincemann.aoplog.api.LogInteraction;
import com.github.vincemann.springrapid.auth.domain.AbstractUser;
import com.github.vincemann.springrapid.auth.security.AuthenticatedPrincipalFactory;



import com.github.vincemann.springrapid.core.security.RapidAuthenticatedPrincipal;
import com.github.vincemann.springrapid.core.service.exception.EntityNotFoundException;
import com.github.vincemann.springrapid.core.slicing.ServiceComponent;
import com.github.vincemann.springrapid.core.util.Message;
import com.github.vincemann.springrapid.core.util.VerifyEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * UserDetailsService, as required by Spring Security.
 * 
 */
@ServiceComponent
@Slf4j
public class RapidUserDetailsService
		implements UserDetailsService, AopLoggable {

	private UserService userService;
	//keep it typeless...
	private AuthenticatedPrincipalFactory authenticatedPrincipalFactory;


	@Transactional
	@LogInteraction
	@Override
	public RapidAuthenticatedPrincipal loadUserByUsername(String email) throws UsernameNotFoundException {
		AbstractUser<?> user;
		try {
			Optional<AbstractUser<?>> byEmail = userService.findByEmail(email);
			VerifyEntity.isPresent(byEmail,"User with email: "+email+" not found");
			user = byEmail.get();
		} catch (EntityNotFoundException e) {
			throw new UsernameNotFoundException(
					Message.get("com.naturalprogrammer.spring.userNotFound", email)
					,e);
		}

		return authenticatedPrincipalFactory.create(user);
	}




	@Autowired
	public void injectPrincipalUserConverter(AuthenticatedPrincipalFactory authenticatedPrincipalFactory) {
		this.authenticatedPrincipalFactory = authenticatedPrincipalFactory;
	}


	@Autowired
	public void injectUserService(UserService userService) {
		this.userService = userService;
	}
}
