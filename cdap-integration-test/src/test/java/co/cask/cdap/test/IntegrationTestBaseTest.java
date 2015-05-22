/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.test;

import co.cask.cdap.StandaloneTester;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test for {@link IntegrationTestBase}.
 */
public class IntegrationTestBaseTest extends IntegrationTestBase {

  @ClassRule
  public static final StandaloneTester STANDALONE = new StandaloneTester();

  @Override
  protected String getInstanceURI() {
    return STANDALONE.getBaseURI().toString();
  }

  @Test
  public void testFlowManager() throws Exception {
    ApplicationManager applicationManager = deployApplication(TestApplication.class);
    FlowManager flowManager = applicationManager.startFlow(TestFlow.NAME);
    flowManager.stop();
  }

}
