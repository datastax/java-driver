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
package com.datastax.oss.driver.api.core.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.testinfra.CassandraRequirement;
import com.datastax.oss.driver.api.testinfra.ccm.CcmRule;
import com.datastax.oss.driver.api.testinfra.session.SessionRule;
import com.datastax.oss.driver.api.testinfra.session.SessionUtils;
import com.datastax.oss.driver.api.testinfra.utils.ConditionChecker;
import com.datastax.oss.driver.categories.ParallelizableTests;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(ParallelizableTests.class)
public class SchemaIT {

  @Rule public CcmRule ccmRule = CcmRule.getInstance();

  @Rule public SessionRule<CqlSession> sessionRule = SessionRule.builder(ccmRule).build();

  @Test
  public void should_expose_system_and_test_keyspace() {
    Map<CqlIdentifier, ? extends KeyspaceMetadata> keyspaces =
        sessionRule.session().getMetadata().getKeyspaces();
    assertThat(keyspaces)
        .containsKeys(
            // Don't test exhaustively because system keyspaces depend on the Cassandra version, and
            // keyspaces from other tests might also be present
            CqlIdentifier.fromInternal("system"),
            CqlIdentifier.fromInternal("system_traces"),
            sessionRule.keyspace());
    assertThat(keyspaces.get(CqlIdentifier.fromInternal("system")).getTables())
        .containsKeys(CqlIdentifier.fromInternal("local"), CqlIdentifier.fromInternal("peers"));
  }

  @Test
  public void should_filter_by_keyspaces() {
    DriverConfigLoader loader =
        SessionUtils.configLoaderBuilder()
            .withStringList(
                DefaultDriverOption.METADATA_SCHEMA_REFRESHED_KEYSPACES,
                Collections.singletonList(sessionRule.keyspace().asInternal()))
            .build();
    try (CqlSession session = SessionUtils.newSession(ccmRule, loader)) {

      assertThat(session.getMetadata().getKeyspaces()).containsOnlyKeys(sessionRule.keyspace());

      CqlIdentifier otherKeyspace = SessionUtils.uniqueKeyspaceId();
      SessionUtils.createKeyspace(session, otherKeyspace);

      assertThat(session.getMetadata().getKeyspaces()).containsOnlyKeys(sessionRule.keyspace());
    }
  }

  @Test
  public void should_not_load_schema_if_disabled_in_config() {
    DriverConfigLoader loader =
        SessionUtils.configLoaderBuilder()
            .withBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED, false)
            .build();
    try (CqlSession session = SessionUtils.newSession(ccmRule, loader)) {

      assertThat(session.isSchemaMetadataEnabled()).isFalse();
      assertThat(session.getMetadata().getKeyspaces()).isEmpty();
    }
  }

  @Test
  public void should_enable_schema_programmatically_when_disabled_in_config() {
    DriverConfigLoader loader =
        SessionUtils.configLoaderBuilder()
            .withBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED, false)
            .build();
    try (CqlSession session = SessionUtils.newSession(ccmRule, loader)) {

      assertThat(session.isSchemaMetadataEnabled()).isFalse();
      assertThat(session.getMetadata().getKeyspaces()).isEmpty();

      session.setSchemaMetadataEnabled(true);
      assertThat(session.isSchemaMetadataEnabled()).isTrue();

      ConditionChecker.checkThat(
              () -> assertThat(session.getMetadata().getKeyspaces()).isNotEmpty())
          .becomesTrue();
      assertThat(session.getMetadata().getKeyspaces())
          .containsKeys(
              CqlIdentifier.fromInternal("system"),
              CqlIdentifier.fromInternal("system_traces"),
              sessionRule.keyspace());

      session.setSchemaMetadataEnabled(null);
      assertThat(session.isSchemaMetadataEnabled()).isFalse();
    }
  }

  @Test
  public void should_disable_schema_programmatically_when_enabled_in_config() {
    CqlSession session = sessionRule.session();
    session.setSchemaMetadataEnabled(false);
    assertThat(session.isSchemaMetadataEnabled()).isFalse();

    // Create a table, metadata should not be updated
    DriverExecutionProfile slowProfile = SessionUtils.slowProfile(session);
    sessionRule
        .session()
        .execute(
            SimpleStatement.builder("CREATE TABLE foo(k int primary key)")
                .withExecutionProfile(slowProfile)
                .build());
    assertThat(session.getMetadata().getKeyspace(sessionRule.keyspace()).get().getTables())
        .doesNotContainKey(CqlIdentifier.fromInternal("foo"));

    // Reset to config value (true), should refresh and load the new table
    session.setSchemaMetadataEnabled(null);
    assertThat(session.isSchemaMetadataEnabled()).isTrue();
    ConditionChecker.checkThat(
            () ->
                assertThat(
                        session.getMetadata().getKeyspace(sessionRule.keyspace()).get().getTables())
                    .containsKey(CqlIdentifier.fromInternal("foo")))
        .becomesTrue();
  }

  @Test
  public void should_refresh_schema_manually() {
    DriverConfigLoader loader =
        SessionUtils.configLoaderBuilder()
            .withBoolean(DefaultDriverOption.METADATA_SCHEMA_ENABLED, false)
            .build();
    try (CqlSession session = SessionUtils.newSession(ccmRule, loader)) {

      assertThat(session.isSchemaMetadataEnabled()).isFalse();
      assertThat(session.getMetadata().getKeyspaces()).isEmpty();

      Metadata newMetadata = session.refreshSchema();
      assertThat(newMetadata.getKeyspaces())
          .containsKeys(
              CqlIdentifier.fromInternal("system"),
              CqlIdentifier.fromInternal("system_traces"),
              sessionRule.keyspace());

      assertThat(session.getMetadata()).isSameAs(newMetadata);
    }
  }

  @CassandraRequirement(min = "4.0", description = "virtual tables introduced in 4.0")
  @Test
  public void should_get_virtual_metadata() {
    Metadata md = sessionRule.session().getMetadata();
    KeyspaceMetadata kmd = md.getKeyspace("system_views").get();

    // Keyspace name should be set, marked as virtual, and have a clients table.
    // All other values should be defaulted since they are not defined in the virtual schema tables.
    assertThat(kmd.getTables().size() >= 2);
    assertThat(kmd.isVirtual()).isTrue();
    assertThat(kmd.isDurableWrites()).isFalse();
    assertThat(kmd.getName().asCql(true)).isEqualTo("system_views");
    assertThat(kmd.getUserDefinedTypes().size()).isEqualTo(0);
    assertThat(kmd.getFunctions().size()).isEqualTo(0);
    assertThat(kmd.getViews().size()).isEqualTo(0);
    assertThat(kmd.getAggregates().size()).isEqualTo(0);
    assertThat(kmd.describe(true))
        .isEqualTo(
            "/* VIRTUAL KEYSPACE system_views WITH replication = { 'class' : 'null' } "
                + "AND durable_writes = false; */");
    // Table name should be set, marked as virtual, and it should have columns set.
    // indexes, views, clustering column, clustering order and id are not defined in the virtual
    // schema tables.
    TableMetadata tm = kmd.getTable("clients").get();
    assertThat(tm).isNotNull();
    assertThat(tm.getName().toString()).isEqualTo("clients");
    assertThat(tm.isVirtual()).isTrue();
    assertThat(tm.getColumns().size()).isEqualTo(12);
    assertThat(tm.getIndexes().size()).isEqualTo(0);
    assertThat(tm.getIndexes().size()).isEqualTo(0);
    assertThat(tm.getClusteringColumns().size()).isEqualTo(0);
    assertThat(tm.getId().isPresent()).isFalse();
    assertThat(tm.getOptions().size()).isEqualTo(0);
    assertThat(tm.getKeyspace()).isEqualTo(kmd.getName());
    assertThat(tm.describe(true))
        .isEqualTo(
            "/* VIRTUAL TABLE system_views.clients (\n"
                + "    address inet,\n"
                + "    port int,\n"
                + "    connection_stage text,\n"
                + "    driver_name text,\n"
                + "    driver_version text,\n"
                + "    hostname text,\n"
                + "    protocol_version int,\n"
                + "    request_count bigint,\n"
                + "    ssl_cipher_suite text,\n"
                + "    ssl_enabled boolean,\n"
                + "    ssl_protocol text,\n"
                + "    username text,\n"
                + "    PRIMARY KEY (())\n"
                + "); */");
    // ColumnMetadata is as expected
    ColumnMetadata cm = tm.getColumn("driver_name").get();
    assertThat(cm).isNotNull();
    assertThat(cm.getParent()).isEqualTo(tm.getName());
    assertThat(cm.getType()).isEqualTo(DataTypes.TEXT);
    assertThat(cm.getName().toString()).isEqualTo("driver_name");
  }
}