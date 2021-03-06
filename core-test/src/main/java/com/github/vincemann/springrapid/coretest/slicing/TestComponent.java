package com.github.vincemann.springrapid.coretest.slicing;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Inherited
@Profile(RapidTestProfiles.TEST)
@Component
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestComponent {
}
