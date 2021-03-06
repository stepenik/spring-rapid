package com.github.vincemann.springrapid.auth.service;

import com.github.vincemann.aoplog.Severity;
import com.github.vincemann.aoplog.api.AopLoggable;
import com.github.vincemann.aoplog.api.LogException;
import com.github.vincemann.aoplog.api.LogInteraction;
import com.github.vincemann.springrapid.auth.domain.AbstractUser;
import com.github.vincemann.springrapid.auth.domain.dto.ChangePasswordForm;
import com.github.vincemann.springrapid.auth.domain.dto.RequestEmailChangeForm;
import com.github.vincemann.springrapid.auth.domain.dto.ResetPasswordForm;
import com.github.vincemann.springrapid.auth.AuthProperties;
import com.github.vincemann.springrapid.auth.service.token.BadTokenException;
import com.github.vincemann.springrapid.auth.util.UserVerifyUtils;
import com.github.vincemann.springrapid.core.service.CrudService;
import com.github.vincemann.springrapid.core.service.exception.BadEntityException;
import com.github.vincemann.springrapid.core.service.exception.EntityNotFoundException;
import com.github.vincemann.springrapid.core.slicing.ServiceComponent;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@Validated
@ServiceComponent
//@LogException
public interface UserService<U extends AbstractUser<ID>, ID extends Serializable>
        extends AopLoggable, CrudService<U,ID>
{

    public Map<String, Object> getContext();

//    @Validated(UserVerifyUtils.SignUpValidation.class)
    // todo do captcha validation programatically in service witch mockable captcha validator for tests
    public U signup( U user) throws BadEntityException, AlreadyRegisteredException;

    public void resendVerificationMail(U user) throws EntityNotFoundException, BadEntityException;

    @LogInteraction(Severity.TRACE)
    public Optional<U> findByEmail( @NotBlank String email);

    public U verifyUser(U user,@NotBlank String verificationCode) throws EntityNotFoundException,  BadEntityException;
    public void forgotPassword( @NotBlank String email) throws EntityNotFoundException;
    public U resetPassword(ResetPasswordForm form) throws EntityNotFoundException,  BadEntityException;
    public void changePassword(U user, ChangePasswordForm changePasswordForm) throws EntityNotFoundException, BadEntityException;

//    @Validated(UserVerifyUtils.ChangeEmailValidation.class)
    public void requestEmailChange(U user, RequestEmailChangeForm emailChangeForm) throws EntityNotFoundException, AlreadyRegisteredException;

    public U changeEmail(U user,  @NotBlank String changeEmailCode) throws EntityNotFoundException, BadEntityException;



    @LogInteraction(Severity.TRACE)
    public String createNewAuthToken(String targetUserEmail);

    public String createNewAuthToken();

//    @LogInteraction(Severity.TRACE)
//    public Map<String, String> fetchFullToken(String authHeader);

    public U createAdminUser(AuthProperties.Admin admin) throws BadEntityException, AlreadyRegisteredException;

    @LogInteraction(Severity.TRACE)
    public abstract ID toId(String id);

//    @Validated(UserVerifyUtils.UpdateValidation.class)
    @Override
    U update(U entity, Boolean full) throws EntityNotFoundException,  BadEntityException;

}
