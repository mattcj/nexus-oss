/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.search;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Component;

/**
 * Search {@link Facet}, providing means to index/de-index component metadata.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface SearchFacet
    extends Facet
{
  /**
   * Index component metadata.
   *
   * @param component to be indexed
   */
  void put(Component component);

  /**
   * Deletes component metadata from index.
   *
   * @param componentId id of the component to be deleted from index
   */
  // TODO: EntityId use here is wrong! Pending a discussion about this
  void delete(EntityId componentId);
}
