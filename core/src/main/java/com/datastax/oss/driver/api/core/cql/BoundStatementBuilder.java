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

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.internal.core.cql.DefaultBoundStatement;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.ByteBuffer;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class BoundStatementBuilder extends StatementBuilder<BoundStatementBuilder, BoundStatement>
    implements Bindable<BoundStatementBuilder> {

  @NonNull private final PreparedStatement preparedStatement;
  @NonNull private final ColumnDefinitions variableDefinitions;
  @NonNull private final ByteBuffer[] values;
  @NonNull private final CodecRegistry codecRegistry;
  @NonNull private final ProtocolVersion protocolVersion;

  public BoundStatementBuilder(
      @NonNull PreparedStatement preparedStatement,
      @NonNull ColumnDefinitions variableDefinitions,
      @NonNull ByteBuffer[] values,
      @Nullable CqlIdentifier routingKeyspace,
      @NonNull CodecRegistry codecRegistry,
      @NonNull ProtocolVersion protocolVersion) {
    this.preparedStatement = preparedStatement;
    this.variableDefinitions = variableDefinitions;
    this.routingKeyspace = routingKeyspace;
    this.values = values;
    this.codecRegistry = codecRegistry;
    this.protocolVersion = protocolVersion;
  }

  public BoundStatementBuilder(@NonNull BoundStatement template) {
    super(template);
    this.preparedStatement = template.getPreparedStatement();
    this.variableDefinitions = template.getPreparedStatement().getVariableDefinitions();
    this.routingKeyspace = template.getRoutingKeyspace();
    this.values = template.getValues().toArray(new ByteBuffer[this.variableDefinitions.size()]);
    this.codecRegistry = template.codecRegistry();
    this.protocolVersion = template.protocolVersion();
  }

  @Override
  public int firstIndexOf(@NonNull CqlIdentifier id) {
    return variableDefinitions.firstIndexOf(id);
  }

  @Override
  public int firstIndexOf(@NonNull String name) {
    return variableDefinitions.firstIndexOf(name);
  }

  @NonNull
  @Override
  public BoundStatementBuilder setBytesUnsafe(int i, ByteBuffer v) {
    values[i] = v;
    return this;
  }

  @Override
  public ByteBuffer getBytesUnsafe(int i) {
    return values[i];
  }

  @Override
  public int size() {
    return values.length;
  }

  @NonNull
  @Override
  public DataType getType(int i) {
    return variableDefinitions.get(i).getType();
  }

  @NonNull
  @Override
  public CodecRegistry codecRegistry() {
    return codecRegistry;
  }

  @NonNull
  @Override
  public ProtocolVersion protocolVersion() {
    return protocolVersion;
  }

  @NonNull
  @Override
  public BoundStatement build() {
    return new DefaultBoundStatement(
        preparedStatement,
        variableDefinitions,
        values,
        configProfileName,
        configProfile,
        routingKeyspace,
        routingKey,
        routingToken,
        buildCustomPayload(),
        idempotent,
        tracing,
        timestamp,
        pagingState,
        codecRegistry,
        protocolVersion);
  }
}
