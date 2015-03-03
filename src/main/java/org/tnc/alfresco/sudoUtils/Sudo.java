/**
 * @author marcello
 *
 */
package org.tnc.alfresco.sudoUtils;

import java.util.Set;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class Sudo extends BaseScopableProcessorExtension {
    private AuthorityService authorityService;
    /*
    * Sets the node service 
    * 
    * @param nodeService   the node service
    */
   public void setAuthorityService(AuthorityService authorityService)
   {
       this.authorityService = authorityService;
   }
    
    
    public void sudo(final Function func) throws Exception  {
        final Context cx = Context.getCurrentContext();
        final Scriptable scope = getScope();
        String user = AuthenticationUtil.getRunAsUser();
 
        Set<String> groups = authorityService.getContainingAuthorities(AuthorityType.GROUP, user, false);
        if (!groups.contains("GROUP_SUDOERS"))
            throw new Exception("MDT - User '" + user + "' cannot use sudo");
 
        RunAsWork<Object> raw = new RunAsWork<Object>() {
            public Object doWork() throws Exception {
                func.call(cx, scope, scope, new Object[] {});
                return null;
            }
        };
 
        AuthenticationUtil.runAs(raw, AuthenticationUtil.getAdminUserName());
    }
    
    
    
}

