package com.vcc.tie.sample.test.component;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * This test is run against a realistic instance of the sample microservice, dependencies have I/O
 * mocked, and as such the "authorization" server would be setup in an embedded wiremock (or
 * similiar tech).
 *
 * <p>This requires the certificate endpoint to be mocked (unless symetric keys are used) as well as
 * the endpoint used to obtain the token. This can either be done with or without signature
 * validation, depending on how the RemoteTokenServices is configured.
 */
public class SomeControllerCompTest {
  /** ignored until team decides this is a bad / good strategy
   *
   */
  @Ignore
  @Test
  public void dsa() {
    fail("not impl");
  }
}
