package com.mesosphere.sdk.nifi.scheduler;

import java.util.Collection;
import java.util.Optional;

import com.mesosphere.sdk.config.validate.ConfigValidationError;
import com.mesosphere.sdk.config.validate.ConfigValidator;
import com.mesosphere.sdk.specification.ServiceSpec;
import com.mesosphere.sdk.specification.validation.ZoneValidator;

public class NifiZoneValidator implements ConfigValidator<ServiceSpec> {
	static final String NIFI_POD_TYPE = "nifi";
	
	@Override
	public Collection<ConfigValidationError> validate(Optional<ServiceSpec> oldConfig, ServiceSpec newConfig) {
		return ZoneValidator.validate(oldConfig, newConfig, NIFI_POD_TYPE);
	}

}
