package io.github.vincemann.springrapid.core.test.service.result.matcher.compare.resolve;

import io.github.vincemann.springrapid.core.model.IdentifiableEntity;
import io.github.vincemann.springrapid.core.test.service.result.ServiceTestContext;

public interface CompareEntityPlaceholderResolver {

    public IdentifiableEntity resolve(CompareEntityPlaceholder compareEntityPlaceholder, ServiceTestContext testContext);
}