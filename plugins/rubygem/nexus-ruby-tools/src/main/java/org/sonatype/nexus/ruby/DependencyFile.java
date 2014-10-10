/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.ruby;


/**
 * represents /api/v1/dependencies/{name}.json.rz
 * where the file content is the response of /api/v1/dependencies?gems={name}
 *
 * @author christian
 */
public class DependencyFile
    extends RubygemsFile
{
  DependencyFile(RubygemsFileFactory factory, String storage, String remote, String name) {
    super(factory, FileType.DEPENDENCY, storage, remote, name);
  }
}