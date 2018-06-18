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
package com.datastax.oss.driver.internal.querybuilder.update;

import com.datastax.oss.driver.api.querybuilder.term.Term;
import com.datastax.oss.driver.internal.querybuilder.lhs.LeftOperand;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

@Immutable
public class CounterAssignment extends DefaultAssignment {

  public CounterAssignment(
      @NonNull LeftOperand leftOperand, @NonNull String operator, @NonNull Term rightOperand) {
    super(leftOperand, operator, rightOperand);
  }

  @Override
  public boolean isIdempotent() {
    return false;
  }
}
