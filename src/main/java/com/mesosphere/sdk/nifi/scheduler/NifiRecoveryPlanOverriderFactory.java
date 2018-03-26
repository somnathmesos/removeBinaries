package com.mesosphere.sdk.nifi.scheduler;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesosphere.sdk.scheduler.plan.Plan;
import com.mesosphere.sdk.scheduler.recovery.RecoveryPlanOverrider;
import com.mesosphere.sdk.scheduler.recovery.RecoveryPlanOverriderFactory;
import com.mesosphere.sdk.state.StateStore;

public class NifiRecoveryPlanOverriderFactory implements RecoveryPlanOverriderFactory {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private static final String REPLACE_PLAN_NAME = "replace";
	
	public NifiRecoveryPlanOverriderFactory() {
		logger.info("Nifi Recovery Plan Override constructor.");
	}

	@Override
	public RecoveryPlanOverrider create(StateStore stateStore, Collection<Plan> plans) {
		logger.info("Create recovery plan override.");
		logger.info("No. of plan: " + plans.size());
		return new NifiRecoveryPlanOverrider(stateStore, getNodeReplacementPlan(plans));
	}
	
    private Plan getNodeReplacementPlan(Collection<Plan> plans) {
        Optional<Plan> planOptional = plans.stream()
        		.filter(plan -> {
        			logger.info(String.format("Plan name is %s.", plan.getName()));
        			return plan.getName().equalsIgnoreCase(REPLACE_PLAN_NAME);
        			})
                .findFirst();

        if (planOptional.isPresent()) {
            return planOptional.get();
        } else {
            throw new RuntimeException("Failed to find the plan.");
        }
    }	

}
