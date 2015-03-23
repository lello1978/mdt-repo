/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tnc.doctrack.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.tnc.doctrack.qr.QRCode;

import com.google.zxing.Result;

/**
 * This class contains the behaviour behind the 'mdt:QRInfoAspect' aspect.
 * <p>
 * Every time tha this aspect is applied, the ZXING qr try to find the qr string to indentify QR text.
 * 
 * 
 * 
 * @author Marcello Modica
 * @author Giorgio Draghetti
 */
public class docTrackBehaviours implements ContentServicePolicies.OnContentUpdatePolicy,NodeServicePolicies.OnAddAspectPolicy,
ContentServicePolicies.OnContentReadPolicy,
NodeServicePolicies.OnCreateNodePolicy,
NodeServicePolicies.OnUpdateNodePolicy,
NodeServicePolicies.OnDeleteNodePolicy {
	
    private static Log logger = LogFactory.getLog(docTrackBehaviours.class);

    public static PolicyComponent policyComponent; 
 	public static NodeService nodeService;
    public static ContentService contentService;
    public static SearchService searchService;
    public static FileFolderService fileFolderService;
    public static SiteService siteService;
    public static ActionService actionService;
    public static ServiceRegistry serviceRegistry; 
    public static StoreRef storeRef=new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    
    /* Aspect names */
    public static final QName ASPECT_docTrackAspect = QName.createQName("http://www.tipinoncomuni.com/model/doctrack/1.0", "docTrackAspect");
     
    /* Property names */
    public static final QName PROP_QRS = QName.createQName("http://www.tipinoncomuni.com/model/doctrack/1.0", "QRS");
    /* Property names */
    public static final QName PROP_zxingError = QName.createQName("http://www.tipinoncomuni.com/model/doctrack/1.0", "zxingError");
   
    /**
     * Spring initilaise method used to register the policy behaviours
     */
 
	public void init(){
    	System.out.println("TNC - DocTrack - Behaviours AMP class loading....");
		if (logger.isDebugEnabled()) logger.debug("TNC - DocTrack - Initializing policy logger behavior"); 
		docTrackBehaviours.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,ASPECT_docTrackAspect,new JavaBehaviour(this, "onAddAspect", NotificationFrequency.FIRST_EVENT));
		System.out.println("TNC - DocTrack - OnAddAspectPolicy Behaviours with Aspect docTrackAspect loaded");
		System.out.println("DocTrack - Behaviours binding finished....");
    }

    /**
     * onAddAspect policy behavior.
     * <p>
     * Sets the count started date to the date/time at which the QRInfoAspect aspect was
     * first applied.
     * 
     * @param nodeRef           the node reference
     * @param aspectTypeQName   the qname of the aspect being applied
     */
	public ArrayList<Serializable> QRs =new ArrayList<Serializable>();
	
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName){
    	System.out.println("TNC - DocTrack - OnAddAspectPolicy Fired  -  " + " NodeRef: "+ nodeRef.getId() +"  -  Aspect: "+ aspectTypeQName.getLocalName());
    	ContentReader reader=null;
    	InputStream is=null;
    	
    	
    	if(fileFolderService.getFileInfo(nodeRef).isFolder()==false){
    	 reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
 		 is = reader.getContentInputStream();
 		}
   
 		com.google.zxing.Result[] qr=null;
 		QRs.clear();
    	if(aspectTypeQName.getLocalName().equals(ASPECT_docTrackAspect.getLocalName())){
    		System.out.println("TNC - DocTrack - Aspect docTrackAspect applied - behaviours fired");    		
 		try {
 			System.out.println("TNC - DocTrack - Start QR engine finder....");
 			System.out.println("TNC - DocTrack - Extract image from file content for QR searching");
			qr = extractQRfromPDF(is);
			if (QRs != null) {
 				if (QRs.size()>0){
 					System.out.println("TNC - DocTrack - find QR codes : '"+StringUtils.join(QRs,";")+"' Try to put content in referred node properties.");
 					//Set QRInfoString to QR code value.	
 					//ArrayList<Serializable> qrList = new ArrayList<Serializable>(Arrays.asList(QRs));
 				
 					docTrackBehaviours.nodeService.setProperty(nodeRef, PROP_QRS,QRs);
 				}	
 			}
		} catch (Exception e) {
			// Simply skip the QR extraction if error occurs.
			System.out.println("TNC - DocTrack - (QRReaderOnBehaviourException) Error with exception on QR reading inside behaviours. "+ e.toString());
			e.printStackTrace();
			try{
				System.out.println("TNC - DocTrack - (QRReaderOnBehaviourException) Error on QR procedure. Set zxingError properties to : "+e.toString() );
				docTrackBehaviours.nodeService.setProperty(nodeRef, PROP_zxingError,e.toString()); 
			} catch(Exception f){
				System.out.println("TNC - DocTrack - (QRReaderOnBehaviourException) [SEVERE ERROR] Error on QR procedure. Something went wrong. We need to delete arrived content for security reason.");
				f.printStackTrace();
				fileFolderService.delete(nodeRef);				
			}
			
		}
 	}
}
    
     
 	public void onContentUpdate(NodeRef nodeRef, boolean flag) {
 		System.out.println("TNC - DocTrack  - Content update policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Content update policy fired");			
 	}

 	public void onContentRead(NodeRef nodeRef) {
 		System.out.println("TNC - DocTrack  - Content read policy fired");
 		
 		
 		if (logger.isDebugEnabled()) logger.debug("Content read policy fired");		
 	}

 	public void onUpdateNode(NodeRef nodeRef) {
 		System.out.println("TNC - DocTrack  - Node update policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Node update policy fired");		
 	}

 	public void onCreateNode(ChildAssociationRef childAssocRef) {
 		System.out.println("TNC - DocTrack  - Node create policy fired");        
 		if (logger.isDebugEnabled()) logger.debug("Node create policy fired");		
 	}

	private Result[] extractQRfromPDF(InputStream PDF  ) throws Exception
    {
		System.out.println("TNC - DocTrack  - extractQRfromPDF starting....");
		//Initialize variable for QR decoding.

		PDDocument document = null;
		String password = "";
		String prefix = null;
		boolean addKey = false;
		Result[] QR=null;
		try
		{
			//read PDF document 
			document = PDDocument.loadNonSeq(PDF, null, password);
			//Check permission to PDF
			AccessPermission ap = document.getCurrentAccessPermission();
			if( ! ap.canExtractContent() )
			{
				System.out.println("TNC - DocTrack  Error - extractQRfromPDF - You do not have permission to extract images from PDF.");
				throw new IOException("TNC - DocTrack  Error - extractQRfromPDF - You do not have permission to extract images from PDF.");
			}
			//Iterate throw the PDF pages. 
			List<?> pages = document.getDocumentCatalog().getAllPages();
			Iterator<?> iter = pages.iterator();
			while( iter.hasNext() )
			{
				PDPage page = (PDPage)iter.next();
				PDResources resources = page.getResources();
				// extract all XObjectImages which are part of the page resources
				System.out.println("TNC - DocTrack  - extractQRfromPDF - Try to process image and find QR code");
				QR=processResources(resources, prefix, addKey);
			}

		}
		finally
		{
			if( (document != null)){
				try {	
					document.close();
			}catch (Exception e) {
			
			}

		}
			}
		System.out.println("TNC - DocTrack  - extractQRfromPDF finished. QR code string : " + QR);
		return QR;
    }

    

    private com.google.zxing.Result[] processResources(PDResources resources, String prefix, boolean addKey) throws Exception
    {
    	//Find QR in image passed as resources
    	System.out.println("TNC - DocTrack  - extractQRfromPDF - processResources. Starting.... ");
    	com.google.zxing.Result[] results = null;
    	if (resources == null)
        {
            return null;
        }
        Map<String, PDXObject> xobjects = resources.getXObjects();
        if( xobjects != null )
        {
            Iterator<String> xobjectIter = xobjects.keySet().iterator();
        
            while( xobjectIter.hasNext() )
            {
                String key = xobjectIter.next();
                PDXObject xobject = xobjects.get( key );
                // write the images
                if (xobject instanceof PDXObjectImage)
                {
                    PDXObjectImage image = (PDXObjectImage)xobject;
                    System.out.println(" TNC - DocTrack  - extractQRfromPDF - processResources - Read image object from PDF file and extract it.");
                    System.out.println(" TNC - DocTrack  - extractQRfromPDF - processResources - Write image on disk for check and debug. Filename qrImageInPDF");
                    System.out.println(" TNC - DocTrack  - extractQRfromPDF - processResources - Extracted Image format - Suffix: "+ image.getSuffix() + " Height: "+ image .getHeight()+ " Widht: " + image.getWidth() );
                    image.write2file("qrImageInPDF_"+UUID.randomUUID().toString());
                    
                    results=org.tnc.doctrack.qr.QRCode.readQRCode(image.getRGBImage());
                    for (Result r : results ){
                    	System.out.println("TNC - DocTrack  - extractQRfromPDF - code discovered: " + r.getText());
                    	//Set<String> set = new HashSet<String>();
                    	//set.add(r.getText());
                    	//if (logger.isDebugEnabled()) System.out.println("TNC - DocTrack  - extractQRfromPDF - QRs arralist: " + r.getText());
                    	QRs.add(r.getText()); //= new ArrayList<String>(set);
                    
                    }
                    
                    
                }
                // maybe there are more images embedded in a form object
                else if (xobject instanceof PDXObjectForm)
                {
                    PDXObjectForm xObjectForm = (PDXObjectForm)xobject;
                    PDResources formResources = xObjectForm.getResources();
                    processResources(formResources, prefix, addKey);
                }
                
            }         
        }
        return results;
    }

    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
 		System.out.println("TNC - DocTrack  - Node delete policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Node delete policy fired");		
 	}

    /**
     * Sets the policy component
     * 
     * @param policyComponent   the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        docTrackBehaviours.policyComponent = policyComponent;
    }

      /** 
     * Sets the node service 
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        docTrackBehaviours.nodeService = nodeService;
    }
        
    /** 
     * Sets the content service 
     * 
     * @param nodeService   the node service
     */
    public void setContentService(ContentService contentService)
    {
        docTrackBehaviours.contentService = contentService;
    }

    
    /** 
     * Sets the node service 
     * 
     * @param nodeService   the node service
     */
    public void setSearchService(SearchService searchService)
    {
        docTrackBehaviours.searchService = searchService;
    }
    
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        docTrackBehaviours.fileFolderService = fileFolderService;
    }
    
    public void setSiteService(SiteService siteService)
    {
        docTrackBehaviours.siteService = siteService;
    }
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        docTrackBehaviours.serviceRegistry = serviceRegistry;
    }
    public void setActionService(ActionService actionService)
    {
        docTrackBehaviours.actionService = actionService;
    }
}
