package org.sonatype.nexus.plugins.p2.repository.internal;

import static org.codehaus.plexus.util.FileUtils.deleteDirectory;
import static org.sonatype.nexus.plugins.p2.repository.P2Constants.P2_REPOSITORY_ROOT_PATH;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.createLink;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.localStorageOfRepositoryAsFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.retrieveFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.retrieveItem;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.safeRetrieveFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.safeRetrieveItem;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.storeItem;
import static org.sonatype.nexus.plugins.p2.repository.internal.P2ArtifactsEventsInspector.isP2ArtifactsXML;
import static org.sonatype.nexus.plugins.p2.repository.internal.P2MetadataEventsInspector.isP2ContentXML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.sonatype.nexus.mime.MimeUtil;
import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryGenerator;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryGeneratorConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.p2.bridge.ArtifactRepository;
import org.sonatype.p2.bridge.MetadataRepository;
import org.sonatype.p2.bridge.model.InstallableArtifact;
import org.sonatype.p2.bridge.model.InstallableUnit;
import org.sonatype.sisu.resource.scanner.helper.ListenerSupport;
import org.sonatype.sisu.resource.scanner.scanners.SerialScanner;

@Named
@Singleton
public class DefaultP2RepositoryGenerator
    implements P2RepositoryGenerator
{

    @Inject
    private Logger logger;

    private final Map<String, P2RepositoryGeneratorConfiguration> configurations;

    private final RepositoryRegistry repositories;

    private final MimeUtil mimeUtil;

    private final ArtifactRepository artifactRepository;

    private final MetadataRepository metadataRepository;

    @Inject
    public DefaultP2RepositoryGenerator( final RepositoryRegistry repositories, final MimeUtil mimeUtil,
                                         final ArtifactRepository artifactRepository,
                                         final MetadataRepository metadataRepository )
    {
        this.repositories = repositories;
        this.mimeUtil = mimeUtil;
        this.artifactRepository = artifactRepository;
        this.metadataRepository = metadataRepository;
        configurations = new HashMap<String, P2RepositoryGeneratorConfiguration>();
    }

    @Override
    public P2RepositoryGeneratorConfiguration getConfiguration( final String repositoryId )
    {
        return configurations.get( repositoryId );
    }

    @Override
    public void addConfiguration( final P2RepositoryGeneratorConfiguration configuration )
    {
        configurations.put( configuration.repositoryId(), configuration );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final StorageItem p2Dir = safeRetrieveItem( repository, P2_REPOSITORY_ROOT_PATH );
            // create if it does not exist
            if ( p2Dir == null )
            {
                final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
                try
                {
                    p2RepoUid.getLock().lock( Action.create );

                    createP2Repository( repository );
                }
                finally
                {
                    p2RepoUid.getLock().unlock();
                }
            }
        }
        catch ( final NoSuchRepositoryException e )
        {
            logger.warn( "Could not delete P2 repository [{}] as repository could not be found" );
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void removeConfiguration( final P2RepositoryGeneratorConfiguration configuration )
    {
        configurations.remove( configuration.repositoryId() );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            try
            {
                p2RepoUid.getLock().lock( Action.create );
                final ResourceStoreRequest request = new ResourceStoreRequest( P2_REPOSITORY_ROOT_PATH );
                repository.deleteItem( request );
            }
            finally
            {
                p2RepoUid.getLock().unlock();
            }
        }
        catch ( final Exception e )
        {
            logger.warn( String.format( "Could not delete P2 repository [%s:%s] due to [%s]",
                configuration.repositoryId(), P2_REPOSITORY_ROOT_PATH, e.getMessage() ), e );
        }
    }

    @Override
    public void updateP2Artifacts( final StorageItem item )
    {
        final P2RepositoryGeneratorConfiguration configuration = getConfiguration( item.getRepositoryId() );
        if ( configuration == null )
        {
            return;
        }
        logger.debug( "Updating P2 repository artifacts (update) for [{}:{}]", item.getRepositoryId(), item.getPath() );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            File destinationP2Repository = null;
            try
            {
                p2RepoUid.getLock().lock( Action.update );

                // copy repository artifacts to a temporary location
                destinationP2Repository = createTemporaryP2Repository();
                final File artifacts = getP2Artifacts( configuration, repository );
                final File tempArtifacts = new File( destinationP2Repository, artifacts.getName() );
                FileUtils.copyFile( artifacts, tempArtifacts );

                updateP2Artifacts( repository, retrieveFile( repository, item.getPath() ), destinationP2Repository );

                // copy repository artifacts back to exposed location
                FileUtils.copyFile( tempArtifacts, artifacts );
            }
            finally
            {
                p2RepoUid.getLock().unlock();
                deleteDirectory( destinationP2Repository );
            }
        }
        catch ( final Exception e )
        {
            logger.warn(
                String.format( "Could not update P2 repository [%s:%s] with [%s] due to [%s]",
                    configuration.repositoryId(), P2_REPOSITORY_ROOT_PATH, item.getPath(), e.getMessage() ), e );
        }
    }

    @Override
    public void removeP2Artifacts( final StorageItem item )
    {
        final P2RepositoryGeneratorConfiguration configuration = getConfiguration( item.getRepositoryId() );
        if ( configuration == null )
        {
            return;
        }
        logger.debug( "Updating P2 repository artifacts (remove) for [{}:{}]", item.getRepositoryId(), item.getPath() );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            File sourceP2Repository = null;
            File destinationP2Repository = null;
            try
            {
                p2RepoUid.getLock().lock( Action.update );

                // copy repository artifacts to a temporary location
                destinationP2Repository = createTemporaryP2Repository();
                final File artifacts = getP2Artifacts( configuration, repository );
                final File tempArtifacts = new File( destinationP2Repository, artifacts.getName() );
                FileUtils.copyFile( artifacts, tempArtifacts );

                // copy item artifacts to a temp location
                sourceP2Repository = createTemporaryP2Repository();
                FileUtils.copyFile( retrieveFile( repository, item.getPath() ), new File( sourceP2Repository,
                    "artifacts.xml" ) );

                artifactRepository.remove( sourceP2Repository.toURI(), destinationP2Repository.toURI() );

                // copy repository artifacts back to exposed location
                FileUtils.copyFile( tempArtifacts, artifacts );
            }
            finally
            {
                p2RepoUid.getLock().unlock();
                deleteDirectory( sourceP2Repository );
                deleteDirectory( destinationP2Repository );
            }
        }
        catch ( final Exception e )
        {
            logger.warn(
                String.format( "Could not update P2 repository [%s:%s] with [%s] due to [%s]",
                    configuration.repositoryId(), P2_REPOSITORY_ROOT_PATH, item.getPath(), e.getMessage() ), e );
        }
    }

    @Override
    public void updateP2Metadata( final StorageItem item )
    {
        final P2RepositoryGeneratorConfiguration configuration = getConfiguration( item.getRepositoryId() );
        if ( configuration == null )
        {
            return;
        }
        logger.debug( "Updating P2 repository metadata (update) for [{}:{}]", item.getRepositoryId(), item.getPath() );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            File destinationP2Repository = null;
            try
            {
                p2RepoUid.getLock().lock( Action.update );

                // copy repository content to a temporary location
                destinationP2Repository = createTemporaryP2Repository();
                final File content = getP2Content( configuration, repository );
                final File tempContent = new File( destinationP2Repository, content.getName() );
                FileUtils.copyFile( content, tempContent );

                updateP2Metadata( repository, retrieveFile( repository, item.getPath() ), destinationP2Repository );

                // copy repository content back to exposed location
                FileUtils.copyFile( tempContent, content );
            }
            finally
            {
                p2RepoUid.getLock().unlock();
                deleteDirectory( destinationP2Repository );
            }
        }
        catch ( final Exception e )
        {
            logger.warn(
                String.format( "Could not update P2 repository [%s:%s] with [%s] due to [%s]",
                    configuration.repositoryId(), P2_REPOSITORY_ROOT_PATH, item.getPath(), e.getMessage() ), e );
        }
    }

    @Override
    public void removeP2Metadata( final StorageItem item )
    {
        final P2RepositoryGeneratorConfiguration configuration = getConfiguration( item.getRepositoryId() );
        if ( configuration == null )
        {
            return;
        }
        logger.debug( "Updating P2 repository metadata (remove) for [{}:{}]", item.getRepositoryId(), item.getPath() );
        try
        {
            final Repository repository = repositories.getRepository( configuration.repositoryId() );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            File sourceP2Repository = null;
            File destinationP2Repository = null;
            try
            {
                p2RepoUid.getLock().lock( Action.update );

                // copy repository content to a temporary location
                destinationP2Repository = createTemporaryP2Repository();
                final File content = getP2Content( configuration, repository );
                final File tempContent = new File( destinationP2Repository, content.getName() );
                FileUtils.copyFile( content, tempContent );

                // copy item content to a temp location
                sourceP2Repository = createTemporaryP2Repository();
                FileUtils.copyFile( retrieveFile( repository, item.getPath() ), new File( sourceP2Repository,
                    "content.xml" ) );

                metadataRepository.remove( sourceP2Repository.toURI(), destinationP2Repository.toURI() );

                // copy repository content back to exposed location
                FileUtils.copyFile( tempContent, content );
            }
            finally
            {
                p2RepoUid.getLock().unlock();
                deleteDirectory( sourceP2Repository );
                deleteDirectory( destinationP2Repository );
            }
        }
        catch ( final Exception e )
        {
            logger.warn(
                String.format( "Could not update P2 repository [%s:%s] with [%s] due to [%s]",
                    configuration.repositoryId(), P2_REPOSITORY_ROOT_PATH, item.getPath(), e.getMessage() ), e );
        }
    }

    @Override
    public void scanAndRebuild( final String repositoryId )
    {
        logger.debug( "Rebuilding P2 repository for repository [{}]", repositoryId );

        final P2RepositoryGeneratorConfiguration configuration = getConfiguration( repositoryId );
        if ( configuration == null )
        {
            logger.warn(
                "Rebuilding P2 repository for [{}] not executed as P2 Repository Generator capability is not enabled for this repository",
                repositoryId );
            return;
        }

        try
        {
            final Repository repository = repositories.getRepository( repositoryId );
            final File scanPath = localStorageOfRepositoryAsFile( repository );
            final RepositoryItemUid p2RepoUid = repository.createUid( P2_REPOSITORY_ROOT_PATH );
            final File destinationP2Repository = createTemporaryP2Repository();
            try
            {
                p2RepoUid.getLock().lock( Action.update );

                // copy repository artifacts to a temporary location
                final File artifacts = getP2Artifacts( configuration, repository );
                final File tempArtifacts = new File( destinationP2Repository, artifacts.getName() );
                FileUtils.copyFile( artifacts, tempArtifacts );

                // copy repository content to a temporary location
                final File content = getP2Content( configuration, repository );
                final File tempContent = new File( destinationP2Repository, content.getName() );
                FileUtils.copyFile( content, tempContent );

                new SerialScanner().scan( scanPath, new ListenerSupport()
                {

                    @Override
                    public void onFile( final File file )
                    {
                        try
                        {
                            if ( isP2ArtifactsXML( file.getPath() ) )
                            {
                                updateP2Artifacts( repository, file, destinationP2Repository );
                            }
                            else if ( isP2ContentXML( file.getPath() ) )
                            {
                                updateP2Metadata( repository, file, destinationP2Repository );
                            }
                        }
                        catch ( final Exception e )
                        {
                            throw new RuntimeException( e );
                        }
                    }

                } );

                // copy artifacts back to exposed location
                FileUtils.copyFile( tempArtifacts, artifacts );
                // copy content back to exposed location
                FileUtils.copyFile( tempContent, content );
            }
            finally
            {
                p2RepoUid.getLock().unlock();

                deleteDirectory( destinationP2Repository );
            }
        }
        catch ( final Exception e )
        {
            logger.warn( String.format(
                "Rebuilding P2 repository not executed as repository [%s] could not be scanned due to [%s]",
                repositoryId, e.getMessage() ), e );
        }
    }

    @Override
    public void scanAndRebuild()
    {
        for ( final Repository repository : repositories.getRepositories() )
        {
            scanAndRebuild( repository.getId() );
        }
    }

    private void updateP2Artifacts( final Repository repository, final File sourceArtifacts,
                                    final File destinationP2Repository )
        throws Exception
    {
        final File sourceP2Repository = createTemporaryP2Repository();
        try
        {
            // copy artifacts to a temp location
            FileUtils.copyFile( sourceArtifacts, new File( sourceP2Repository, "artifacts.xml" ) );

            artifactRepository.merge( sourceP2Repository.toURI(), destinationP2Repository.toURI() );

            // create a link in /plugins directory back to original jar
            final Collection<InstallableArtifact> installableArtifacts =
                artifactRepository.getInstallableArtifacts( sourceP2Repository.toURI() );
            for ( final InstallableArtifact installableArtifact : installableArtifacts )
            {
                final String linkPath =
                    P2_REPOSITORY_ROOT_PATH + "/plugins/" + installableArtifact.getId() + "_"
                        + installableArtifact.getVersion() + ".jar";
                if ( installableArtifact.getRepositoryPath() != null )
                {
                    final StorageItem bundle = retrieveItem( repository, installableArtifact.getRepositoryPath() );
                    createLink( repository, bundle, linkPath );
                }
            }
        }
        finally
        {
            deleteDirectory( sourceP2Repository );
        }
    }

    private void updateP2Metadata( final Repository repository, final File sourceContent,
                                   final File destinationP2Repository )
        throws Exception
    {
        final File sourceP2Repository = createTemporaryP2Repository();
        try
        {
            // copy content to a temp location
            FileUtils.copyFile( sourceContent, new File( sourceP2Repository, "content.xml" ) );

            metadataRepository.merge( sourceP2Repository.toURI(), destinationP2Repository.toURI() );
        }
        finally
        {
            deleteDirectory( sourceP2Repository );
        }
    }

    private void storeItemFromFile( final String path, final File file, final Repository repository )
        throws Exception
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream( file );

            final ResourceStoreRequest request = new ResourceStoreRequest( path );

            storeItem( repository, request, in, mimeUtil.getMimeType( request.getRequestPath() ), null /* attributes */);
        }
        finally
        {
            IOUtil.close( in );
        }
    }

    private File getP2Artifacts( final P2RepositoryGeneratorConfiguration configuration, final Repository repository )
        throws Exception
    {
        // TODO handle compressed repository
        final String path = P2_REPOSITORY_ROOT_PATH + P2Constants.ARTIFACTS_XML;
        File file = safeRetrieveFile( repository, path );
        if ( !file.exists() )
        {
            createP2Repository( repository );
            file = retrieveFile( repository, path );
        }
        return file;
    }

    private File getP2Content( final P2RepositoryGeneratorConfiguration configuration, final Repository repository )
        throws Exception
    {
        // TODO handle compressed repository
        final String path = P2_REPOSITORY_ROOT_PATH + P2Constants.CONTENT_XML;
        File file = safeRetrieveFile( repository, path );
        if ( !file.exists() )
        {
            createP2Repository( repository );
            file = retrieveFile( repository, path );
        }
        return file;
    }

    private void createP2Repository( final Repository repository )
        throws Exception
    {
        File tempP2Repository = null;
        try
        {
            tempP2Repository = createTemporaryP2Repository();

            artifactRepository.write( tempP2Repository.toURI(), Collections.<InstallableArtifact> emptyList(),
                repository.getId(), null /** repository properties */
                , null /* mappings */);

            final String p2ArtifactsPath = P2_REPOSITORY_ROOT_PATH + P2Constants.ARTIFACTS_XML;

            storeItemFromFile( p2ArtifactsPath, new File( tempP2Repository, "artifacts.xml" ), repository );

            metadataRepository.write( tempP2Repository.toURI(), Collections.<InstallableUnit> emptyList(),
                repository.getId(), null /** repository properties */
            );

            final String p2ContentPath = P2_REPOSITORY_ROOT_PATH + "/" + P2Constants.CONTENT_XML;

            storeItemFromFile( p2ContentPath, new File( tempP2Repository, "content.xml" ), repository );
        }
        finally
        {
            FileUtils.deleteDirectory( tempP2Repository );
        }
    }

    static File createTemporaryP2Repository()
        throws IOException
    {
        File tempP2Repository;
        tempP2Repository = File.createTempFile( "nexus-p2-repository-plugin", "" );
        tempP2Repository.delete();
        tempP2Repository.mkdirs();
        return tempP2Repository;
    }

}
