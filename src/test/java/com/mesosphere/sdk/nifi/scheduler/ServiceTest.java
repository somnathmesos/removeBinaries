package com.mesosphere.sdk.nifi.scheduler;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.mesos.Protos;
import org.junit.Test;

import com.mesosphere.sdk.testing.Expect;
import com.mesosphere.sdk.testing.Send;
import com.mesosphere.sdk.testing.ServiceTestRunner;
import com.mesosphere.sdk.testing.SimulationTick;

public class ServiceTest {
   @Test
    public void testSpec() throws Exception {
      new ServiceTestRunner().run();
    }
}
