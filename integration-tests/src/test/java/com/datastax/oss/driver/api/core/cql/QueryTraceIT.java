/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.api.core.cql;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverExecutionException;
import com.datastax.oss.driver.api.testinfra.ccm.CcmRule;
import com.datastax.oss.driver.api.testinfra.cluster.SessionRule;
import com.datastax.oss.driver.categories.ParallelizableTests;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

@Category(ParallelizableTests.class)
public class QueryTraceIT {

  @ClassRule public static CcmRule ccmRule = CcmRule.getInstance();
  @ClassRule public static SessionRule<CqlSession> sessionRule = new SessionRule<>(ccmRule);
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_not_have_tracing_id_when_tracing_disabled() {
    ExecutionInfo executionInfo =
        sessionRule
            .session()
            .execute("SELECT release_version FROM system.local")
            .getExecutionInfo();

    assertThat(executionInfo.getTracingId()).isNull();

    // Should get a DriverExecutionException with an underlying IllegalStateException indicating
    // Tracing was disabled.
    thrown.expect(DriverExecutionException.class);
    String expectedMessage = "Tracing was disabled for this request";
    thrown.expectCause(
        new TypeSafeMatcher<Throwable>() {
          @Override
          public void describeTo(Description description) {
            description.appendText(
                "Expected IllegalStateException with message of '" + expectedMessage + "'");
          }

          @Override
          protected boolean matchesSafely(Throwable item) {
            return item instanceof IllegalStateException
                && item.getMessage().equals(expectedMessage);
          }
        });
    executionInfo.getQueryTrace();
  }

  @Test
  public void should_fetch_trace_when_tracing_enabled() {
    ExecutionInfo executionInfo =
        sessionRule
            .session()
            .execute(
                SimpleStatement.builder("SELECT release_version FROM system.local")
                    .withTracing()
                    .build())
            .getExecutionInfo();

    assertThat(executionInfo.getTracingId()).isNotNull();

    QueryTrace queryTrace = executionInfo.getQueryTrace();
    assertThat(queryTrace.getTracingId()).isEqualTo(executionInfo.getTracingId());
    assertThat(queryTrace.getRequestType()).isEqualTo("Execute CQL3 query");
    assertThat(queryTrace.getDurationMicros()).isPositive();
    assertThat(queryTrace.getCoordinator())
        .isEqualTo(ccmRule.getContactPoints().iterator().next().getAddress());
    assertThat(queryTrace.getParameters())
        .containsEntry("consistency_level", "LOCAL_ONE")
        .containsEntry("page_size", "5000")
        .containsEntry("query", "SELECT release_version FROM system.local")
        .containsEntry("serial_consistency_level", "SERIAL");
    assertThat(queryTrace.getStartedAt()).isPositive();
    // Don't want to get too deep into event testing because that could change across versions
    assertThat(queryTrace.getEvents()).isNotEmpty();
  }
}
