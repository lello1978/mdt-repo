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
package mdt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mdtQR.QRCode;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;

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
public class mdtBehaviours implements ContentServicePolicies.OnContentUpdatePolicy,NodeServicePolicies.OnAddAspectPolicy,
ContentServicePolicies.OnContentReadPolicy,
NodeServicePolicies.OnCreateNodePolicy,
NodeServicePolicies.OnUpdateNodePolicy,
NodeServicePolicies.OnDeleteNodePolicy {
	
    private static Log logger = LogFactory.getLog(mdtBehaviours.class);

    public static PolicyComponent policyComponent; 
 	public static NodeService nodeService;
    public static ContentService contentService;
    public static SearchService searchService;
    public static FileFolderService fileFolderService;
    public static StoreRef storeRef=new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
    /* Aspect names */
    public static final QName ASPECT_QRInfoAspect = QName.createQName("http://www.lc.com/model/mdt/1.0", "QRInfoAspect");
    public static final QName ASPECT_MecaDocTrackElementIDAspect = QName.createQName("http://www.lc.com/model/mdt/1.0", "MecaDocTrackElementIDAspect");
    
    /* Property names */
    public static final QName PROP_QRInfoString = QName.createQName("http://www.lc.com/model/mdt/1.0", "QRInfoString");
    public static final QName PROP_idElemento = QName.createQName("http://www.lc.com/model/mdt/1.0", "idElemento");
  
    /**
     * Spring initilaise method used to register the policy behaviours
     */
 
	public void init(){
    	
		System.out.println("MDT - Behaviours AMP class loading....");

		if (logger.isDebugEnabled()) logger.debug("MDT - Initializing policy logger behavior"); 

		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,ASPECT_QRInfoAspect,new JavaBehaviour(this, "onAddAspect", NotificationFrequency.FIRST_EVENT));
		System.out.println("MDT - OnAddAspectPolicy Behaviours with Aspect QRInfoAspect loaded");
		this.policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME,ASPECT_MecaDocTrackElementIDAspect,new JavaBehaviour(this, "onAddAspect", NotificationFrequency.FIRST_EVENT));
		System.out.println("MDT - OnAddAspectPolicy Behaviours with Aspect MecaDocTrackElementIDAspect loaded");
		/*
        //Register the policy behaviours
        this.policyComponent.bindClassBehaviour((ContentServicePolicies.OnContentReadPolicy.QNAME),ContentModel.ASPECT_DUBLINCORE,new JavaBehaviour(this, "onContentRead", NotificationFrequency.EVERY_EVENT));
        System.out.println("MDT - OCR  onContentRead Behaviours loaded/bind");
        this.policyComponent.bindClassBehaviour((ContentServicePolicies.OnContentUpdatePolicy.QNAME),ContentModel.ASPECT_DUBLINCORE,new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.EVERY_EVENT));
        System.out.println("MDT - OCR  onContentUpdate Behaviours loaded/bind");
        this.policyComponent.bindClassBehaviour((NodeServicePolicies.OnCreateNodePolicy.QNAME),ContentModel.ASPECT_DUBLINCORE,new JavaBehaviour(this, "onCreateNode", NotificationFrequency.EVERY_EVENT));
        System.out.println("MDT - OCR  onCreateNode Behaviours loaded");
        this.policyComponent.bindClassBehaviour((NodeServicePolicies.OnUpdateNodePolicy.QNAME),ContentModel.ASPECT_DUBLINCORE,new JavaBehaviour(this, "onUpdateNode", NotificationFrequency.EVERY_EVENT));
        System.out.println("MDT - OCR  onUpdateNode Behaviours loaded/bind");
        this.policyComponent.bindClassBehaviour((NodeServicePolicies.OnDeleteNodePolicy.QNAME),ContentModel.ASPECT_DUBLINCORE,new JavaBehaviour(this, "onDeleteNode", NotificationFrequency.EVERY_EVENT));
        System.out.println("MDT - OCR  onDeleteNode Behaviours loaded/bind");*/

		System.out.println("MDT - Behaviours binding finished....");
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
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName){
    	System.out.println("MDT - OnAddAspectPolicy Fired" + " NodeRef: "+ nodeRef.getId() +" Aspect: "+ aspectTypeQName.getLocalName());
    	ContentReader reader=null;
    	InputStream is=null;
    	
    	if(fileFolderService.getFileInfo(nodeRef).isFolder()==false){
    	 reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
 		 is =reader.getContentInputStream();
 		}
    	
 		ResultSet rs = null;
 		NodeRef destFolderRef=null;
 		
 		String siteID = null;
 		String qr=null;
 		
    	if(aspectTypeQName.getLocalName().equals(ASPECT_QRInfoAspect.getLocalName())){
    		System.out.println("MDT - Aspect QRInfoAspect applied - behaviours fired ");
    		//Read node content and get the stream for ZXING. 
    	
 		try {
 			System.out.println("MDT - Start QR engine finder....");
 			//Launch QR extractor and set qr code if found
 			//find folder containing the mailed files
 			ChildAssociationRef childAssociationRef = nodeService.getPrimaryParent(nodeRef);
			NodeRef parent = childAssociationRef.getParentRef();
			//find siteID from parent folder
			
			siteID=this.fileFolderService.getFileInfo(parent).getName();
			System.out.println("MDT - SiteID where to put content is: " + siteID);
			rs = searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:" + siteID + "/cm:documentLibrary/cm:PRODUZIONE");
			//if QR is found....
			System.out.println("MDT - Extract image from file content for QR searching");
			qr = extractQRfromPDF(is);
 			if (qr != null) {
 				System.out.println("MDT - QR code find. Code: '"+qr+"' Try to put content in referred MDT folder.");
 				//Set QRInfoString to QR code value.	
 				this.nodeService.setProperty(nodeRef, PROP_QRInfoString,qr);
 				//Search destination folder based on QR value
 				System.out.println("MDT - Search for match between QR and MDT destination folder name.");
 				System.out.println("MDT - Search for matching under path: " + "/app:company_home/st:sites/cm:" + siteID + "/cm:documentLibrary/cm:PRODUZIONE");
 				if (rs.length()>=1){
 					System.out.println("MDT - Search for matching with folders."); //+this.fileFolderService.getFileInfo(rs.getNodeRef(0)).getName());
 					destFolderRef = this.fileFolderService.searchSimple(rs.getNodeRef(0), qr);
 					
 				}
 				if (destFolderRef!=null){
 					System.out.println("MDT - QR Matching found on MDT folder Id:" + destFolderRef.getId());
 					//if destination is found, move the file to this folder 
 					this.nodeService.setProperty(nodeRef,ContentModel.PROP_DESCRIPTION,this.fileFolderService.getFileInfo(nodeRef).getName()); 
 					Map<QName,Serializable> aspectValues = new HashMap<QName,Serializable>();
 					aspectValues.put(PROP_idElemento, qr);
 					this.nodeService.addAspect(nodeRef, ASPECT_MecaDocTrackElementIDAspect,aspectValues);
 					this.fileFolderService.move(nodeRef,destFolderRef, nodeRef.getId());
 					
 					System.out.println("MDT - QR Code discovered, applyed to QRInfoString properties, and moved to correct destination folder: "+this.fileFolderService.getFileInfo(destFolderRef).getName());
 				 } else {
 					 //if Destination folder is NOT found i move file in the SMTP error path and property QRInfoString is set to DEST NOT FOUND
 					System.out.println("MDT - QR Matching NOT found. Moving document to "+"/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
 					rs = searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
 					if (rs.length()>=1) destFolderRef = rs.getNodeRef(0);
 	 				
 	 				this.nodeService.setProperty(nodeRef,ContentModel.PROP_DESCRIPTION,this.fileFolderService.getFileInfo(nodeRef).getName()); 
 	 				this.fileFolderService.move(nodeRef,destFolderRef,nodeRef.getId());
 	 				System.out.println("MDT - Unrecognized content moved in error folder. Set QRInfoString properties to \"DEST NOT FOUND\" ");
 					this.nodeService.setProperty(nodeRef, PROP_QRInfoString,"QRCODE: "+qr+" - ERROR: DEST NOT FOUND"); 
 				 }				
 			
 			}else{
 				//if QR is null the file is moved to SMTP error folder and the properties QRInfoString is set to QR NOT FOUND
 				System.out.println("MDT - QR code NOT found on document. Moving document to "+"/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
 				rs = searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
 				if (rs.length()>=1) destFolderRef = rs.getNodeRef(0);
 				
 				this.nodeService.setProperty(nodeRef,ContentModel.PROP_DESCRIPTION,this.fileFolderService.getFileInfo(nodeRef).getName()); 
 				this.fileFolderService.move(nodeRef,destFolderRef,nodeRef.getId());
 				System.out.println("MDT - Unrecognized content moved in error folder. Set QRInfoString properties to \"DEST NOT FOUND\" ");
 				this.nodeService.setProperty(nodeRef, PROP_QRInfoString,"QR NOT FOUND"); 
 			}
 			
 			
 			
		} catch (Exception e) {
			// Simply skip the QR extraction if error occurs.
			//TODO: Add alerting by email if QR code cannot be found.
			System.out.println("MDT - Error with exception on QR reading with ZXING. "+ e.toString());
			e.printStackTrace();
			try{
				System.out.println("MDT - QR code NOT found on document. Moving document to "+"/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
				rs = searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:smtpRouter/cm:"+siteID+"SMTP/cm:error");
				if (rs.length()>=1) destFolderRef = rs.getNodeRef(0);				
				this.nodeService.setProperty(nodeRef,ContentModel.PROP_DESCRIPTION,this.fileFolderService.getFileInfo(nodeRef).getName()); 
				this.fileFolderService.move(nodeRef,destFolderRef, nodeRef.getId());
				System.out.println("MDT - Error on QR procedure. Set QRInfoString properties to \"QR NOT FOUND OR ZXING EXCEPTION\" ");
				this.nodeService.setProperty(nodeRef, PROP_QRInfoString,"QR NOT FOUND OR ZXING EXCEPTION"); 
			} catch(Exception f){
				System.out.println("MDT - [SEVERE ERROR] Error on QR procedure. Something went wrong.");
				f.printStackTrace();
				fileFolderService.delete(nodeRef);				
			}
			
		}
    	}else if (aspectTypeQName.getLocalName().equals(ASPECT_MecaDocTrackElementIDAspect.getLocalName())){
    		System.out.println("MDT - Aspect MecaDocTrackElementIDAspect applied - behaviours fired ");
    		if(fileFolderService.getFileInfo(nodeRef).isFolder()==true){
    	    	
    		System.out.println("MDT - Try to generate A4 QR label PDF file ");
    		System.out.println("MDT - Loading mdtQR stylesheet for FO transformations");
    		
    		
    		try{
    			
    			
    			String tempFileName = String.valueOf(UUID.randomUUID())+".fo";
    			
    			try {
    				System.out.println("MDT - Reading content of fo file");
    				//TODO: VErificare che il file esista nel sito mdtAdmin
    			    ResultSet foFile= searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:mdtFo.fo");
    			    reader = contentService.getReader(foFile.getNodeRef(0), ContentModel.PROP_CONTENT);
    			    String fo = reader.getContentString();
    			    System.out.println("MDT - Reading content of barcodeFile XML file");
    			    //TODO: Verificare che il file esista nel sito mdtAdmin
    			    ResultSet barcodeFile= searchService.query(storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:mdtXmlQr.xml");
    			    reader = contentService.getReader(barcodeFile.getNodeRef(0), ContentModel.PROP_CONTENT);
    			    String barcode = reader.getContentString();
    			    System.out.println("MDT - Inject folder data before PDF creation.");
    			    //Sostituzione caratteri e stringhe MDT
    			    fo=fo.replaceAll("##idEelemento##", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
    			    fo=fo.replaceAll("##descrizione##", nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString());
    			    //fo=fo.replaceAll("##articolo##", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
    			    //fo=fo.replaceAll("##riferimento##", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
    			    //Sostituzione Barcode
    			    fo=fo.replaceAll("<fo:inline>mdtQRCODE</fo:inline>", barcode.replaceAll("MDTQRMDTQR", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()));
    			    //fo=fo.replaceAll("\\\\{mdtFolder\\\\}", nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
    			    System.out.println("MDT - Begin fo transfomation on disk.");
    			    File tempFile = new File(tempFileName);
                    FileWriter file = new FileWriter(tempFile);
                    BufferedWriter out = new BufferedWriter (file);
                    out.write(fo);
                    out.close();
                    System.out.println("MDT - Starting FOP from command line command.");
                    Process fop;
    			    String fopCommand = "/opt/fop-1.1/fop -fo "+tempFileName+" -pdf "+tempFileName+".pdf";
    			    fop= Runtime.getRuntime().exec(fopCommand);
    			    int fopExit =fop.waitFor();
    			    if (fopExit==0){
    			    	System.out.println("MDT - Conversion with FOP terminate WITHOUT error.");
    			    	QName contentQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
    			    	FileInfo pdfFile = fileFolderService.create(nodeRef,"QR"+nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+".pdf", contentQName);
    			    	NodeRef pdf = pdfFile.getNodeRef();
    			    	ContentWriter writer = contentService.getWriter(pdf, ContentModel.PROP_CONTENT, true);
    			    	System.out.println("MDT - Put PDF label file in MDT folder.");
    			    	writer.setMimetype("application/pdf");
    			    	writer.guessEncoding();
    			    	tempFile= new File(tempFileName+".pdf");
    			    	writer.putContent(tempFile);
    			    }
    			} finally {
    				System.out.println("MDT - Delete .fo and .pdf temporary files from disk.");
    				 File f = new File(tempFileName);
    				 if (f.exists()){f.delete();};
    				 f=  new File(tempFileName+".pdf");
    				 if (f.exists()){f.delete();};   
    				 System.out.println("MDT - Temporary file deleted");
    			}
    		
    			
    	} catch (Exception e){
    		System.out.println("MDT - Something went wrong in FOP conversion: error Stack: "+ e.getMessage());
    		e.printStackTrace();
    		
    		}
    	} else { System.out.println("MDT - Skip QR creation on this behaviours because node is type:content");}
    	}
    }
    
     
 	public void onContentUpdate(NodeRef nodeRef, boolean flag) {
 		System.out.println("MDT - Content update policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Content update policy fired");			
 	}

 	public void onContentRead(NodeRef nodeRef) {
 		System.out.println("MDT - Content read policy fired");
 		
 		
 		if (logger.isDebugEnabled()) logger.debug("Content read policy fired");		
 	}

 	public void onUpdateNode(NodeRef nodeRef) {
 		System.out.println("MDT - Node update policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Node update policy fired");		
 	}

 	public void onCreateNode(ChildAssociationRef childAssocRef) {
 		System.out.println("MDT - Node create policy fired");        
 		if (logger.isDebugEnabled()) logger.debug("Node create policy fired");		
 	}

	private String extractQRfromPDF(InputStream PDF  ) throws Exception
    {
		System.out.println("MDT - extractQRfromPDF starting....");
		//Initialize variable for QR decoding.

		PDDocument document = null;
		String password = "";
		String prefix = null;
		boolean addKey = false;
		String QR=null;
		try
		{
			//read PDF document 
			document = PDDocument.loadNonSeq(PDF, null, password);
			//Check permission to PDF
			AccessPermission ap = document.getCurrentAccessPermission();
			if( ! ap.canExtractContent() )
			{
				System.out.println("MDT Error - extractQRfromPDF - You do not have permission to extract images from PDF.");
				throw new IOException("MDT Error - extractQRfromPDF - You do not have permission to extract images from PDF.");
			}
			//Iterate throw the PDF pages. 
			List<?> pages = document.getDocumentCatalog().getAllPages();
			Iterator<?> iter = pages.iterator();
			while( iter.hasNext() )
			{
				PDPage page = (PDPage)iter.next();
				PDResources resources = page.getResources();
				// extract all XObjectImages which are part of the page resources
				System.out.println("MDT - extractQRfromPDF - Try to process image and find QR code");
				QR=processResources(resources, prefix, addKey);
			}

		}
		finally
		{
			if( document != null )
			{
				document.close();
			}

		}
		System.out.println("MDT - extractQRfromPDF finished. QR code string : " + QR);
		return QR;
    }

    

    private String processResources(PDResources resources, String prefix, boolean addKey) throws Exception
    {
    	//Find QR in image passed as resources
    	System.out.println("MDT - extractQRfromPDF - processResources. Starting.... ");
    	String r=null;
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
                    System.out.println(" MDT - extractQRfromPDF - processResources - Read image object from PDF file and extract it.");
                    System.out.println(" MDT - extractQRfromPDF - processResources - Write image on disk for check and debug. Filename qrImageInPDF");
                    System.out.println(" MDT - extractQRfromPDF - processResources - Extracted Image format - Suffix: "+ image.getSuffix() + " Height: "+ image .getHeight()+ " Widht: " + image.getWidth() );
                    image.write2file("qrImageInPDF");
                    r=QRCode.readQRCode(image.getRGBImage());
                    if (r !=null){break;};
                    
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
        return r;
    }

    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
 		System.out.println("MDT - Node delete policy fired");
 		if (logger.isDebugEnabled()) logger.debug("Node delete policy fired");		
 	}

    /**
     * Sets the policy component
     * 
     * @param policyComponent   the policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

      /** 
     * Sets the node service 
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
        
    /** 
     * Sets the content service 
     * 
     * @param nodeService   the node service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    
    /** 
     * Sets the node service 
     * 
     * @param nodeService   the node service
     */
    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }
    
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

}
