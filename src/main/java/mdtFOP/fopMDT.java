/**
 * TIPINONCOMUNI SRLS 07-02-2015
 * This package contains class to generate PDF with QR COE. It use FOP.
 * @author Marcello Modica
 */
package mdtFOP;

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

import sendQRhelper.sendQr;

/**
 * This class contains all functions to generate pdf files starting with all the ".fo" files contained in :
 * /app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:fo path.
 * For each .fo file the strings: ##idEelemento## , ##descrizione##, ##articolo## are replaced with same properties from MDT folder.
 * To get the function GenerateLabelPaper FOP must be installed and running on the operating system. The folder for FOP is: /opt/fop-1.1/fop
 * To setup the barcode4j http://barcode4j.sourceforge.net/ FOP extension, do the following:
 *   	- Add barcode4j.jar and barcode4j-fop-ext.jar to the classpath.
 *  	- Alternatively, you can use the combined JAR: barcode4j-fop-ext-complete.jar which combines both Barcode4J and the FOP extension.
 *  
 * @author Marcello Modica
 *
 */

public class fopMDT {
	private static String fo="";
	private static String labelTitle="";
	private static String labelDesc="";
	public static final QName PROP_articolo = QName.createQName("http://www.lc.com/model/mdt/1.0", "articolo");

	/** 
	 * 
	 */
	public fopMDT() {
		// TODO Auto-generated constructor stub
	}
	
	/** 
	 * 
	 */
	public static void GenerateLabelPaper(NodeRef nodeRef){

		ContentReader reader=null;
		System.out.println("MDT - Begin Generate label paper procedure . Class: mdtFOP. Method : GenerateLabelPaper(nodeRef) ...");
		if(mdt.mdtBehaviours.fileFolderService.getFileInfo(nodeRef).isFolder()==true){
			System.out.println("MDT - Try to generate A4 QR label PDF file for just created folder.");
			System.out.println("MDT - Loading mdtQR stylesheet for FO transformations");
			
			try {
				System.out.println("MDT - Reading content of barcodeFile XML file");
				String barcode= readBarcodeXML();
				System.out.println("MDT - Locate and Iterate trought fo files in mdtAdmin site");
				ResultSet fopFolder = mdt.mdtBehaviours.searchService.query(mdt.mdtBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:fo");
				List<FileInfo> foFiles = mdt.mdtBehaviours.fileFolderService.listFiles(fopFolder.getNodeRef(0));
				System.out.println("MDT - find " + foFiles.size() +" .fo files in mdtAdmin site folder: " +mdt.mdtBehaviours.fileFolderService.getFileInfo(fopFolder.getNodeRef(0)).getName());
				for (FileInfo foFile:foFiles){System.out.println("MDT - " + foFile.getName());}
				for (FileInfo foFile:foFiles){
					System.out.println("MDT - Begin label creation for file "+ foFile.getName()+". Read content...");
					reader = mdt.mdtBehaviours.contentService.getReader(foFile.getNodeRef(), ContentModel.PROP_CONTENT);
					labelTitle=((mdt.mdtBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_TITLE)==null)?"N/D":mdt.mdtBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_TITLE).toString());
					labelDesc=((mdt.mdtBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_DESCRIPTION)==null)?"N/D":mdt.mdtBehaviours.nodeService.getProperty(foFile.getNodeRef(), ContentModel.PROP_DESCRIPTION).toString());
					fo = reader.getContentString();
					System.out.println("MDT - DEBUG - fo content: " + "\r" + fo);
					System.out.println("MDT - Inject folder data before PDF creation from folder: " + mdt.mdtBehaviours.fileFolderService.getFileInfo(nodeRef).getName());
					System.out.println("MDT - Inject idElemento: " + mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
					fo=StringUtils.replace(fo, "##idEelemento##", mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString());
					System.out.println("MDT - Inject descrizione: " + ((mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					fo=StringUtils.replace(fo, "##descrizione##", ((mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION) == null) ? "N/D" : mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).toString()));
					System.out.println("MDT - Inject articolo: "+ ((mdt.mdtBehaviours.nodeService.getProperty(nodeRef, PROP_articolo) == null) ? "N/D" : mdt.mdtBehaviours.nodeService.getProperty(nodeRef, PROP_articolo).toString()));
					fo=StringUtils.replace(fo, "##articolo##",  ((mdt.mdtBehaviours.nodeService.getProperty(nodeRef, PROP_articolo) == null) ? "N/D" : mdt.mdtBehaviours.nodeService.getProperty(nodeRef,PROP_articolo).toString()));
					System.out.println("MDT - Inject barcode data before PDF creation...");
					ChildAssociationRef childAssociationRef = mdt.mdtBehaviours.nodeService.getPrimaryParent(nodeRef);
					NodeRef parent = childAssociationRef.getParentRef();
					String parentName= mdt.mdtBehaviours.nodeService.getProperty(parent,ContentModel.PROP_NAME).toString();
					if (parentName.equals("PRODUZIONE")){
						barcode=StringUtils.replace(barcode, "MDTQRMDTQR", mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+":p");
					}else if (parentName.equals("MATERIA")){
						barcode=StringUtils.replace(barcode, "MDTQRMDTQR", mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+":m");
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
							FileInfo pdfFile = mdt.mdtBehaviours.fileFolderService.create(nodeRef,"QR-"+tempFileName+"-"+mdt.mdtBehaviours.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString()+".pdf", contentQName);
							NodeRef pdf = pdfFile.getNodeRef();
							mdt.mdtBehaviours.nodeService.setProperty(pdf, ContentModel.PROP_DESCRIPTION,labelDesc);
							mdt.mdtBehaviours.nodeService.setProperty(pdf, ContentModel.PROP_TITLE,labelTitle);
							ContentWriter writer = mdt.mdtBehaviours.contentService.getWriter(pdf, ContentModel.PROP_CONTENT, true);
							System.out.println("MDT - Put PDF label file, " +pdfFile.getName()+" in MDT folder: " + mdt.mdtBehaviours.fileFolderService.getFileInfo(nodeRef).getName() );
							writer.setMimetype("application/pdf");
							writer.guessEncoding();
							tempFile= new File(tempFileName+".pdf");
							writer.putContent(tempFile);
							QName TYPE_QNAME_qrLabel = QName.createQName("http://www.lc.com/model/mdt/1.0", "qrLabel");
						    mdt.mdtBehaviours.nodeService.setType(pdf, TYPE_QNAME_qrLabel);
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
			sendQr m = new sendQRhelper.sendQr(nodeRef);
	        Thread t = new Thread(m);
	        t.start();
			//sendQrMail(nodeRef);
		}catch(Exception e){
			System.out.println("MDT - ERROR Sending email with QR labels to folder owner.");
			e.printStackTrace();	
		
		}		
	}
    			
	
	
	/** 
	 * This method return the barcode4j XML string, reading the parameters from: 
	 * /app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:barcodeConfig.properties
	 * In this text file there is the path for XML string content in repository.
	 */

public static String readBarcodeXML(){
	ContentReader reader=null;
	ResultSet barcodeConfigFile= mdt.mdtBehaviours.searchService.query(mdt.mdtBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:barcodeConfig.properties");
	reader = mdt.mdtBehaviours.contentService.getReader(barcodeConfigFile.getNodeRefs().get(0), ContentModel.PROP_CONTENT);
    String barcodeXMLFileName = reader.getContentString();
	System.out.println("MDT - Barcode file name from : "+ mdt.mdtBehaviours.fileFolderService.getFileInfo(barcodeConfigFile.getNodeRef(0)).getName());
	
    ResultSet barcodeFile= mdt.mdtBehaviours.searchService.query(mdt.mdtBehaviours.storeRef, SearchService.LANGUAGE_XPATH, "/app:company_home/st:sites/cm:mdtadmin/cm:documentLibrary/cm:xmlTemplate/cm:FOP/cm:"+barcodeXMLFileName);
    System.out.println("MDT - Using Barcode file name : "+ mdt.mdtBehaviours.fileFolderService.getFileInfo(barcodeFile.getNodeRef(0)).getName());
    reader = mdt.mdtBehaviours.contentService.getReader(barcodeFile.getNodeRef(0), ContentModel.PROP_CONTENT);
    String barcode = reader.getContentString();
    System.out.println("MDT - DEBUG : QR barcode XML file content: " + "\r" + barcode);
    return barcode;
}


}
