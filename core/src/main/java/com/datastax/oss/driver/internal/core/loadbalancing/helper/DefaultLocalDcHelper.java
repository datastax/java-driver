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
package com.datastax.oss.driver.internal.core.loadbalancing.helper;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.metadata.DefaultNode;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLocalDcHelper extends BasicLocalDcHelper {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalDcHelper.class);

  public DefaultLocalDcHelper(
      @NonNull InternalDriverContext context,
      @NonNull DriverExecutionProfile profile,
      @NonNull String logPrefix) {
    super(context, profile, logPrefix);
  }

  /**
   * This implementation fetches the user-supplied datacenter, if any, from the programmatic
   * configuration API, or else, from the driver configuration. If no local datacenter is explicitly
   * defined, this implementation will consider two distinct situations:
   *
   * <ol>
   *   <li>If no explicit contact points were provided, this implementation will infer the local
   *       datacenter from the implicit contact point (localhost).
   *   <li>If explicit contact points were provided however, this implementation will throw {@link
   *       IllegalStateException}.
   * </ol>
   *
   * @return The local datacenter; always present.
   * @throws IllegalStateException if the local datacenter could not be inferred.
   */
  @NonNull
  @Override
  public Optional<String> discoverLocalDc() {
    Optional<String> optionalLocalDc = super.discoverLocalDc();
    if (optionalLocalDc.isPresent()) {
      return optionalLocalDc;
    }
    Set<DefaultNode> contactPoints = context.getMetadataManager().getContactPoints();
    if (context.getMetadataManager().wasImplicitContactPoint()) {
      // We only allow automatic inference of the local DC in this specific case
      assert contactPoints.size() == 1;
      Node contactPoint = contactPoints.iterator().next();
      String localDc = contactPoint.getDatacenter();
      if (localDc != null) {
        LOG.debug(
            "[{}] Local DC set from implicit contact point {}: {}",
            logPrefix,
            contactPoint,
            localDc);
        return Optional.of(localDc);
      } else {
        throw new IllegalStateException(
            "The local DC could not be inferred from implicit contact point, please set it explicitly (see "
                + DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER.getPath()
                + " in the config, or set it programmatically with SessionBuilder.withLocalDatacenter)");
      }
    } else {
      throw new IllegalStateException(
          "Since you provided explicit contact points, the local DC must be explicitly set (see "
              + DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER.getPath()
              + " in the config, or set it programmatically with SessionBuilder.withLocalDatacenter). "
              + "Current contact points are: "
              + formatNodes(contactPoints));
    }
  }
}
