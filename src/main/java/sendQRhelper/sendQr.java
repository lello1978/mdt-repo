/**
 * 
 */
package sendQRhelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;















import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.template.TemplateNode;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;

/**
 * @author marcello
 *
 */
public class sendQr implements Runnable {

	public NodeRef mdtFolder;
	public String QUERY_qrLabels;
	public ResultSet qrLabels;
	public String templatePATH;
	public ResultSet template;
	public static  NodeService nodeService=mdt.mdtBehaviours.serviceRegistry.getNodeService();
	public static  SearchService searchService=mdt.mdtBehaviours.serviceRegistry.getSearchService();
	public static  StoreRef storeRef=new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	public static  ActionService actionService=mdt.mdtBehaviours.serviceRegistry.getActionService();
	public static  NamespaceService nameSpaceService=mdt.mdtBehaviours.serviceRegistry.getNamespaceService();

   public sendQr(NodeRef mdtFolder ){
	   this.mdtFolder=mdtFolder;
    }
   
   public void sendQrMail() {
		
	   

			this.QUERY_qrLabels = "(TYPE:\"{http://www.lc.com/model/mdt/1.0}qrLabel\") AND (PATH:\""
					+ sendQr.nodeService.getPath(this.mdtFolder)
							.toPrefixString(
									sendQr.nameSpaceService) + "/*\")";
			this.qrLabels = sendQr.searchService.query(sendQr.storeRef,
					SearchService.LANGUAGE_LUCENE, QUERY_qrLabels);
			this.templatePATH = "PATH:\"/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:mdtMailTemplate/cm:mailQrLabels.ftl\"";

			System.out.println("MDT -Search for MDT Email Template "
					+ templatePATH + " for QR label sending found.");
			this.template = sendQr.searchService.query(sendQr.storeRef,
					SearchService.LANGUAGE_LUCENE, templatePATH);
			if (this.template.length() == 0) {
				System.out.println("MDT -ERROR Email Template " + templatePATH
						+ " for QR label sending not found.");
				return;
			}
			  
			   String nodeCeator=(String) sendQr.nodeService.getProperty(mdtFolder,ContentModel.PROP_CREATOR);
			   NodeRef person= mdt.mdtBehaviours.serviceRegistry.getPersonService().getPersonOrNull(nodeCeator);
			   String toEmailAddress=(String) sendQr.nodeService.getProperty(person,ContentModel.PROP_EMAIL);
			Action mailAction = sendQr.actionService
					.createAction(MailActionExecuter.NAME);
			mailAction.setParameterValue(MailActionExecuter.PARAM_SUBJECT,
					"Hai nuovi QR code per MecaDocTrack.");
			mailAction.setParameterValue(MailActionExecuter.PARAM_TO,
					toEmailAddress);
			mailAction.setParameterValue(MailActionExecuter.PARAM_FROM,
					"robot@mecadoctrack.it");
			mailAction.setParameterValue(MailActionExecuter.PARAM_TEXT,
					"Hai nuovi QR code per MecaDocTrack.");
			NodeRef template = this.template.getNodeRef(0);
			mailAction.setParameterValue(MailActionExecuter.PARAM_TEMPLATE,
					template);
			// Define parameters for the model (set fields in the ftl like : 
			// args.workflowTitle)
			Map<String, Object> templateArgs = new HashMap<String, Object>();
			List<TemplateNode> list = new ArrayList<TemplateNode>();
			for (int i = 0; i < qrLabels.length(); i++) {
		          NodeRef node = qrLabels.getNodeRef(i);
		          list.add(new TemplateNode(node, mdt.mdtBehaviours.serviceRegistry, null));
		        }
			templateArgs.put("qrLabels", list);
			templateArgs.put("mdtFolder",this.mdtFolder);
			Map<String, Serializable> templateModel = new HashMap<String, Serializable>();
			templateModel.put("args", (Serializable) templateArgs);
			mailAction.setParameterValue(
					MailActionExecuter.PARAM_TEMPLATE_MODEL,
					(Serializable) templateModel);
			sendQr.actionService.executeAction(mailAction, null);
		}

		public void run() {
			AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>()

					{
				@Override
				public Void doWork() throws Exception

				{
					try{
						
						
						  Thread.sleep(60000); //sleep for 1000 ms
						  sendQrMail();
						  return null;
						}
						catch(InterruptedException ie){
						return null;
						}
					
					

				}

					}
					);


		}


}

	


