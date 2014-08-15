/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm;

import java.io.File;
import java.util.Arrays;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.hazelcast.HazelcastProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator;

/**
 * @author Sebastian Menski
 */
@WebListener
public class EngineStarter implements ServletContextListener {

  public static ProcessEngine processEngine;

  protected static File hazel = new File("hazel");
  protected static File deploy = new File("deploy");

  public void contextInitialized(ServletContextEvent sce) {
    if (hazel.exists()) {

      HazelcastProcessEngineConfiguration.members.clear();
      HazelcastProcessEngineConfiguration.members.addAll(Arrays.asList(
        "localhost",
        "raspberrypi.fritz.box",
        "beaglebone.fritz.box",
        "cubox.fritz.box",
        "cubie.fritz.box"
      ));
      HazelcastProcessEngineConfiguration.manager = "beaglebone.fritz.box";
      processEngine = new HazelcastProcessEngineConfiguration().setHistory(ProcessEngineConfiguration.HISTORY_NONE).buildProcessEngine();

    }
    else {
      StandaloneProcessEngineConfiguration processEngineConfiguration = new StandaloneProcessEngineConfiguration();
      processEngineConfiguration
        .setJdbcDriver("org.postgresql.Driver")
        .setJdbcUrl("jdbc:postgresql://192.168.115.110:5432/process-engine")
        .setJdbcUsername("postgres")
        .setJdbcPassword("postgres")
        .setIdGenerator(new StrongUuidGenerator());

      if (deploy.exists()) {
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_CREATE_DROP);
      }
      processEngine = processEngineConfiguration.setHistory(ProcessEngineConfiguration.HISTORY_NONE).buildProcessEngine();
    }

    if (deploy.exists()) {
      processEngine.getRepositoryService().createDeployment()
        .addClasspathResource("SequencePerformanceTest.asyncSequence1Step.bpmn")
        .addClasspathResource("SequencePerformanceTest.asyncSequence5Steps.bpmn")
        .addClasspathResource("SequencePerformanceTest.asyncSequence15Steps.bpmn")
        .addClasspathResource("NestedSubProcess.bpmn20.xml")
        .deploy();
    }
  }

  public void contextDestroyed(ServletContextEvent sce) {
    processEngine.close();
  }
}
