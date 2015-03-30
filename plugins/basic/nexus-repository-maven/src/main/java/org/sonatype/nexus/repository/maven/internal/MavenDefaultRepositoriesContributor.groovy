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
package org.sonatype.nexus.repository.maven.internal

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2HostedRecipe
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2ProxyRecipe
import org.sonatype.nexus.repository.proxy.ChecksumPolicy
import org.sonatype.nexus.repository.maven.internal.policy.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy


/**
 * Provide default hosted and proxy repositories for Maven.
 * @since 3.0
 */
@Named
@Singleton
class MavenDefaultRepositoriesContributor
    implements DefaultRepositoriesContributor
{
  @Override
  List<Configuration> getRepositoryConfigurations() {
    return [
        new Configuration(repositoryName: 'releases', recipeName: Maven2HostedRecipe.NAME, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.RELEASE.toString(),
                    strictContentTypeValidation: false
                ],
                storage: [
                    writePolicy: WritePolicy.ALLOW_ONCE.toString()
                ]

            ]
        ),
        new Configuration(repositoryName: 'snapshots', recipeName: Maven2HostedRecipe.NAME, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.SNAPSHOT.toString(),
                    strictContentTypeValidation: false
                ],
                storage: [
                    writePolicy: WritePolicy.ALLOW.toString()
                ]
            ]
        ),
        new Configuration(repositoryName: 'central', recipeName: Maven2ProxyRecipe.NAME, attributes:
            [
                maven  : [
                    versionPolicy              : VersionPolicy.MIXED.toString(),
                    strictContentTypeValidation: false
                ],
                proxy  : [
                    remoteUrl     : 'http://repo1.maven.org/maven2/',
                    artifactMaxAge: 3600,
                    checksumPolicy: ChecksumPolicy.WARN.toString(),
                ],
                httpclient: [
                    connection: [
                        timeout: 1500,
                        retries: 3
                    ]    
                ],
                storage: [
                    writePolicy: WritePolicy.ALLOW.toString()
                ]
            ]
        )
    ]
  }
}
