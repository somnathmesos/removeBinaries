package com.mesosphere.sdk.nifi.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesosphere.sdk.scheduler.plan.DefaultPhase;
import com.mesosphere.sdk.scheduler.plan.Phase;
import com.mesosphere.sdk.scheduler.plan.Plan;
import com.mesosphere.sdk.scheduler.plan.PodInstanceRequirement;
import com.mesosphere.sdk.scheduler.plan.Step;
import com.mesosphere.sdk.scheduler.plan.strategy.SerialStrategy;
import com.mesosphere.sdk.scheduler.recovery.DefaultRecoveryStep;
import com.mesosphere.sdk.scheduler.recovery.RecoveryPlanOverrider;
import com.mesosphere.sdk.scheduler.recovery.RecoveryType;
import com.mesosphere.sdk.scheduler.recovery.constrain.UnconstrainedLaunchConstrainer;
import com.mesosphere.sdk.state.StateStore;

public class NifiRecoveryPlanOverrider implements RecoveryPlanOverrider {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private final StateStore stateStore;
    private final Plan plan;
    
	public NifiRecoveryPlanOverrider(StateStore stateStore, Plan plan) {
		logger.info("Initialization of Recovery overrider.");
		this.stateStore = stateStore;
		this.plan = plan;
		logger.info(String.format("The plan name is: %s", plan.getName()));
	}

	@Override
	public Optional<Phase> override(PodInstanceRequirement stoppedPod) {
		logger.info("Within override execution.");
		int replaceIndex = stoppedPod.getPodInstance().getIndex();
		List<Step> restartSteps = (this.plan.getChildren().stream()
        		.map(phase -> {
        			return phase.getChildren().stream()
			            .filter(step -> step.getPodInstanceRequirement().get().getPodInstance().getIndex() == replaceIndex)
			            .map(step -> {
			                PodInstanceRequirement restartPodInstanceRequirement =
			                        PodInstanceRequirement.newBuilder(
			                                step.getPodInstanceRequirement().get().getPodInstance(),
			                                step.getPodInstanceRequirement().get().getTasksToLaunch())
			                                .recoveryType(RecoveryType.TRANSIENT)
			                                .build();
			
			                return new DefaultRecoveryStep(
			                        step.getName(),
			                        restartPodInstanceRequirement,
			                        new UnconstrainedLaunchConstrainer(),
			                        stateStore);
			                }).collect(Collectors.toList());
			        })
            .collect(Collectors.toList()).stream().flatMap(List:: stream).collect(Collectors.toList()));

        logger.info("Plan override executed.");
	    return Optional.of(new DefaultPhase(
	    		this.plan.getChildren().get(0).getName(),
	    		restartSteps,
	            new SerialStrategy<>(),
	            Collections.emptyList()));
	}
}
