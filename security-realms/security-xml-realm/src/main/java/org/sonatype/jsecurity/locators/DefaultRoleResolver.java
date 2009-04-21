package org.sonatype.jsecurity.locators;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.jsecurity.model.CPrivilege;
import org.sonatype.jsecurity.model.CRole;
import org.sonatype.jsecurity.realms.privileges.PrivilegeDescriptor;
import org.sonatype.jsecurity.realms.tools.ConfigurationManager;
import org.sonatype.jsecurity.realms.tools.NoSuchPrivilegeException;
import org.sonatype.jsecurity.realms.tools.NoSuchRoleException;

@Component( role = RoleResolver.class )
public class DefaultRoleResolver implements RoleResolver
{
    
    @Requirement( role = ConfigurationManager.class, hint = "resourceMerging" )
    private ConfigurationManager configuration;
    
    @Requirement(role=PrivilegeDescriptor.class)
    private List<PrivilegeDescriptor> privilegeDescriptors;

    public Set<String> resolvePermissions( Set<String> roleIds )
    {
        // Maybe we should just call resolveRoles first, but this seems more optimized
        // less looking up object in lists (and we already had this code)
        LinkedList<String> rolesToProcess = new LinkedList<String>();
        Set<String> rolesResult = new LinkedHashSet<String>();
        Set<String> permissionIds = new LinkedHashSet<String>();
        
        if ( roleIds != null )
        {
            rolesToProcess.addAll( roleIds );
        }
        
        while ( !rolesToProcess.isEmpty() )
        {
            String roleId = rolesToProcess.removeFirst();
            if ( !rolesResult.contains( roleId ) )
            {
                CRole role;
                try
                {
                    role = configuration.readRole( roleId );
                    rolesResult.add( roleId );

                    // process the roles this role has
                    rolesToProcess.addAll( role.getRoles() );

                    // add the permissions this role has
                    List<String> privilegeIds = role.getPrivileges();
                    for ( String privilegeId : privilegeIds )
                    {
                        permissionIds.addAll( getPermissions( privilegeId ) );
                    }
                }
                catch ( NoSuchRoleException e )
                {
                    // skip
                }
            }
        }
        return permissionIds;
    }

    public Set<String> resolveRoles( Set<String> roleIds )
    {
        LinkedList<String> rolesToProcess = new LinkedList<String>();
        
        Set<String> rolesResult = new LinkedHashSet<String>();
        
        if ( roleIds != null )
        {
            rolesToProcess.addAll( roleIds );
        }
        
        while ( !rolesToProcess.isEmpty() )
        {
            String roleId = rolesToProcess.removeFirst();
            if ( !rolesResult.contains( roleId ) )
            {
                CRole role;
                try
                {
                    role = configuration.readRole( roleId );
                    rolesResult.add( roleId );

                    // process the roles this role has
                    rolesToProcess.addAll( role.getRoles() );                    
                }
                catch ( NoSuchRoleException e )
                {
                    // skip
                }
            }
        }
        
        return rolesResult;
    }
    
    private Set<String> getPermissions( String privilegeId )
    {
        try
        {
            CPrivilege privilege = configuration.readPrivilege( privilegeId );
            
            for ( PrivilegeDescriptor descriptor : privilegeDescriptors )
            {
                String permission = descriptor.buildPermission( privilege );
                
                if ( permission != null )
                {
                    return Collections.singleton( permission );
                }
            }
            
            return Collections.emptySet();
        }
        catch ( NoSuchPrivilegeException e )
        {
            return Collections.emptySet();
        }
    }
    
    public Set<String> effectiveRoles( Set<String> roleIds )
    {
        Set<String> rolesResult = new HashSet<String>();

        //first add the passed in role
        rolesResult.addAll( roleIds );
        
        for ( CRole role : configuration.listRoles() )
        {
            // Get list of all roles in this role
            Set<String> resolvedRoleIds = resolveRoles( Collections.singleton( role.getId() ) );
            
            for ( String roleId : roleIds )
            {
                // if contains the role we are querying against, add it
                if ( resolvedRoleIds.contains( roleId ) )
                {
                    rolesResult.add( role.getId() );
                    break;
                }
            }
        }

        return rolesResult;
    }

}
