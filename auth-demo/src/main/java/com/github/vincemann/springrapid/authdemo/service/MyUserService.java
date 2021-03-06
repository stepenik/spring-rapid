package com.github.vincemann.springrapid.authdemo.service;

import com.github.vincemann.springrapid.authdemo.domain.User;
import com.github.vincemann.springrapid.authdemo.repositories.UserRepository;
import com.github.vincemann.springrapid.auth.service.AbstractUserService;

//@ServiceComponent
//@Primary
public class MyUserService extends AbstractUserService<User, Long, UserRepository> {

	@Override
    public User newUser() {
        return new User();
    }

	@Override
	public Long toId(String id) {
		return Long.valueOf(id);
	}

	@Override
	public Class<?> getTargetClass() {
		return MyUserService.class;
	}

}