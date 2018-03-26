package com.mesosphere.sdk.nifi.scheduler;

import java.io.File;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mesosphere.sdk.scheduler.DefaultScheduler;
import com.mesosphere.sdk.scheduler.SchedulerBuilder;
import com.mesosphere.sdk.scheduler.SchedulerConfig;
import com.mesosphere.sdk.scheduler.SchedulerRunner;
import com.mesosphere.sdk.specification.DefaultServiceSpec;
import com.mesosphere.sdk.specification.yaml.RawServiceSpec;

/**
 * Main entry point for the Scheduler.
 */
public class Main {
	protected static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected one file argument, got: " + Arrays.toString(args));
        }
        logger.info("Nifi scheduler.");

     // Read config from provided file, and assume any config nifis are in the same directory as the file:
        SchedulerRunner
			        .fromSchedulerBuilder(createSchedulerBuilder(new File(args[0])))
			        .run();        
    }
    
    private static SchedulerBuilder createSchedulerBuilder(File yamlSpecFile) throws Exception {
    	logger.info("Creating scheduler builder.");
    	RawServiceSpec rawServiceSpec = RawServiceSpec.newBuilder(yamlSpecFile).build();
        SchedulerConfig schedulerConfig = SchedulerConfig.fromEnv();

        SchedulerBuilder schedulerBuilder = DefaultScheduler.newBuilder(
                DefaultServiceSpec
                        .newGenerator(rawServiceSpec, schedulerConfig, yamlSpecFile.getParentFile())
                        .build(),
                schedulerConfig)
                .setCustomConfigValidators(Arrays.asList(new NifiZoneValidator()))
                .setPlansFrom(rawServiceSpec)
                .setRecoveryManagerFactory(new NifiRecoveryPlanOverriderFactory());
        logger.info("schedulerBuilder instance: " + schedulerBuilder);
        return schedulerBuilder;
    }    
}
