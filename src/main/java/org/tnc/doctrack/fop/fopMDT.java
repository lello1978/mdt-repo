/**
 * 
 */
package org.tnc.doctrack.fop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.UUID;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.myfaces.shared_impl.util.StringUtils;
import org.tnc.doctrack.behaviours.docTrackBehaviours;
import org.tnc.doctrack.sendQrHelper.sendQr;

/**
 * @author marcello
 *
 */
public class fopMDT {
	private static String fo="";
	private static String labelTitle="";
	private static String labelDesc="";

	/** 
	 * 
	 */
	public fopMDT() {
		// TODO Auto-generated constructor stub
	}
	
	public static void GenerateLabelPaper(NodeRef nodeRef){

		ContentReader reader=null;
		System.out.println("MDT - Begin Generate label paper procedure . Class: mdtFOP. Method : GenerateLabelPaper(nodeRef) ...");
		if(docTrackBehaviours.fileFolderService.getFileInfo(nodeRef).isFolder()==true){
			System.out.println("MDT - Try to generate A4 QR label PDF file for just created folder.");
			System.out.println("MDT - Loading mdtQR stylesheet for FO transformations");
			
			try {
				System.out.println("MDT - Reading content of barcodeFile XML file");
				String barcode= readBarcodeXML();
				System.out.println("MDT - Locate and Iterate trought fo files in mdtAdmin site");
				ResultSet fopFolder = docTrackBehaviours.searchService.query(docTrackBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:fo");
				List<FileInfo> foFiles = docTrackBehaviours.fileFolderService.listFiles(fopFolder.getNodeRef(0));
				System.out.println("MDT - find " + foFiles.size() +" .fo files in mdtAdmin site folder: " +docTrackBehaviours.fileFolderService.getFileInfo(fopFolder.getNodeRef(0)).getName());
				for (FileInfo foFile:foFiles){System.out.println("MDT - " + foFile.getName());}
				for (FileInfo foFile:foFiles){
					System.out.println("MDT - Begin label creation for file "+ foFile.getName()+". Read content...");
					reader = docTrackBehaviours.contentService.getReader(foFile.getNodeRef(), ContentModel.PROP_CONTENT);
					labelTitle=((docTrackBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_TITLE)==null)?"N/D":docTrackBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_TITLE).toString());
					labelDesc=((docTrackBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_DESCRIPTION)==null)?"N/D":docTrackBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_DESCRIPTION).toString());
					fo = reader.getContentString();
					System.out.println("MDT - DEBUG - fo content: " + "\r" + fo);
					System.out.println("MDT - Inject folder data before PDF creation from folder: " + docTrackBehaviours.fileFolderService.getFileInfo(nodeRef).getName());
					System.out.println("MDT - Inject idElemento: " + docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
					fo=StringUtils.replace(fo, "##idEelemento##", docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
					System.out.println("MDT - Inject descrizione: " + ((docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					fo=StringUtils.replace(fo, "##descrizione##", ((docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					System.out.println("MDT - Inject articolo: "+ ((docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					fo=StringUtils.replace(fo, "##articolo##",  ((docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					System.out.println("MDT - Inject barcode data before PDF creation...");
					ChildAssociationRef childAssociationRef = docTrackBehaviours.nodeService.getPrimaryParent(nodeRef);
					NodeRef parent = childAssociationRef.getParentRef();
					String parentName= docTrackBehaviours.nodeService.getProperty(parent,ContentModel.PROP_NAME).toString();
					if (parentName.equals("PRODUZIONE")){
						barcode=StringUtils.replace(barcode, "MDTQRMDTQR",docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+":p");
					}else if (parentName.equals("MATERIA")){
						barcode=StringUtils.replace(barcode, "MDTQRMDTQR", docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+":m");
					}
					fo=StringUtils.replace(fo,"<fo:inline>mdtQRCODE</fo:inline>",barcode);
					String tempFileName = String.valueOf(UUID.randomUUID())+".fo";
					System.out.println("MDT - Begin fo transfomation on disk for file: " + foFile.getName());
					try{
						System.out.println("MDT - TempFileName: " + tempFileName);
						File tempFile = new File(tempFileName);
						FileWriter file = new FileWriter(tempFile);
						BufferedWriter out = new BufferedWriter (file);
						out.write(fo);
						out.close();
						System.out.println("MDT - Starting FOP from command line: ");
						Process fop;
						String fopCommand = "/opt/fop-1.1/fop -fo "+tempFileName+" -pdf "+tempFileName+".pdf";
						System.out.println("MDT - " + fopCommand);
						fop= Runtime.getRuntime().exec(fopCommand);
						int fopExit =fop.waitFor();
						if (fopExit==0){
							System.out.println("MDT - Conversion with FOP terminate WITHOUT error.");
							QName contentQName = QName.createQName("{http://www.alfresco.org/model/content/1.0}content");
							FileInfo pdfFile = docTrackBehaviours.fileFolderService.create(nodeRef,"QR-"+tempFileName+"-"+docTrackBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+".pdf", contentQName);
							NodeRef pdf = pdfFile.getNodeRef();
							docTrackBehaviours.nodeService.setProperty(pdf, ContentModel.PROP_DESCRIPTION,labelDesc);
							docTrackBehaviours.nodeService.setProperty(pdf, ContentModel.PROP_TITLE,labelTitle);
							ContentWriter writer = docTrackBehaviours.contentService.getWriter(pdf, ContentModel.PROP_CONTENT, true);
							System.out.println("MDT - Put PDF label file, " +pdfFile.getName()+" in MDT folder: " + docTrackBehaviours.fileFolderService.getFileInfo(nodeRef).getName() );
							writer.setMimetype("application/pdf");
							writer.guessEncoding();
							tempFile= new File(tempFileName+".pdf");
							writer.putContent(tempFile);
							QName TYPE_QNAME_qrLabel = QName.createQName("http://www.lc.com/model/mdt/1.0", "qrLabel");
						    docTrackBehaviours.nodeService.setType(pdf, TYPE_QNAME_qrLabel);
						  }
					}catch (Exception e){
						System.out.println("MDT - Something went wrong during FOP conversion: error Stack: "+ e.getMessage());
						e.printStackTrace();
					} finally {
						System.out.println("MDT - Delete .fo and .pdf temporary files from disk.");
						File f = new File(tempFileName);
						if (f.exists()){f.delete();};
						f=  new File(tempFileName+".pdf");
						if (f.exists()){f.delete();};   
						System.out.println("MDT - Temporary file deleted");

					}
				}

			} catch (Exception e){
				System.out.println("MDT - Something went wrong in FOP conversion routine: error Stack: "+ e.getMessage());
				e.printStackTrace();

			}

		} else { System.out.println("MDT - Skip QR creation on this behaviours because node is type:content");
		}
		try{
			System.out.println("MDT - Sending email with QR labels to folder owner");
			sendQr m = new org.tnc.doctrack.sendQrHelper.sendQr(nodeRef);
	        Thread t = new Thread(m);
	        t.start();
			//sendQrMail(nodeRef);
		}catch(Exception e){
			System.out.println("MDT - ERROR Sending email with QR labels to folder owner.");
			e.printStackTrace();	
		
		}		
	}
    			
	
	
	

public static String readBarcodeXML(){
	ContentReader reader=null;
	ResultSet barcodeConfigFile= docTrackBehaviours.searchService.query(docTrackBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:barcodeConfig.properties");
	reader = docTrackBehaviours.contentService.getReader(barcodeConfigFile.getNodeRefs().get(0), ContentModel.PROP_CONTENT);
    String barcodeXMLFileName = reader.getContentString();
	System.out.println("MDT - Barcode file name from : "+ docTrackBehaviours.fileFolderService.getFileInfo(barcodeConfigFile.getNodeRef(0)).getName());
	
    ResultSet barcodeFile= docTrackBehaviours.searchService.query(docTrackBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:"+barcodeXMLFileName);
    System.out.println("MDT - Using Barcode file name : "+ docTrackBehaviours.fileFolderService.getFileInfo(barcodeFile.getNodeRef(0)).getName());
    reader = docTrackBehaviours.contentService.getReader(barcodeFile.getNodeRef(0), ContentModel.PROP_CONTENT);
    String barcode = reader.getContentString();
    System.out.println("MDT - DEBUG : QR barcode XML file content: " + "\r" + barcode);
    return barcode;
}


}
