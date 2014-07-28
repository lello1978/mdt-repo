/**
 * Class to recognize QR code in images. The image is rotated in 45,90,135,180,225,270,315 degrees if QR is not found.
 * readQRocde is the main function. Rotating is made by rotateImage function and affineTrasform technique.
 */
/**
 * @author Marcello Modica
 *
 */
package mdtQR;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.QRCodeReader;
 
public class QRCode {
 
  public static void main(String[] args) throws WriterException, IOException,NotFoundException {
    Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
 

  }
 
  
  public static String readQRCode(InputStream is, String charset, Map<?, ?> hintMap) throws FileNotFoundException, IOException, NotFoundException {
	   
	  //get the data from the input stream
	  BufferedImage image = ImageIO.read(is);
	  //convert the image to a binary bitmap source
	  LuminanceSource source = new BufferedImageLuminanceSource(image);
	  BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
	  //decode the barcode
	  QRCodeReader reader = new QRCodeReader();

	  Result result;
	  try {
	    result = reader.decode(bitmap);
	    return result.getText();
	  } catch (ReaderException e) {
	   //the data is improperly formatted
	  			e.printStackTrace();
	  			return null;
	  }
	    //qrCodeResult. .decode(is,hintMap);
	    
	  }
  
  public static Result[] readQRCode(BufferedImage image) throws Exception {
	  int[] degrees ={45,90,135,180,225,270,315};
	  //QRCodeReader reader = new QRCodeReader();
	  Map<DecodeHintType,Object> hints = new EnumMap<>(DecodeHintType.class);
	  //hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8);
	  hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
	  Result[] results = null;
	  System.out.println("MDT - ZXING starting finding QR code in image.");
	  LuminanceSource source = new BufferedImageLuminanceSource(image);
	  BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
	  QRCodeMultiReader multiReader = new QRCodeMultiReader();
	  try{
		  results = multiReader.decodeMultiple(bitmap,hints);
		  System.out.println("MDT - ZXING There is a QR code in image!!!");
		  return results;
	  }
	  catch (ReaderException e){
		  System.out.println("MDT - ZXING Exception with 0 degree detection.");
		  e.printStackTrace();
		  System.out.println("MDT - ZXING Cannot find QR code in image. Continue with rotation.");
		  if (source.isRotateSupported()==true){
			  for (int d:degrees){
				  System.out.println("MDT - Rotating Image " + d + " degrees");
				  try {		
					  source=source.rotateCounterClockwise45();
					  bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
					  results = multiReader.decodeMultiple(bitmap,hints);
					  System.out.println("MDT - ZXING There is a QR code in image!!! QR Code discovered with rotation : " + d + " degrees");
					  System.out.println("MDT - ZXING Barocde format detected : " + results.toString());
					  System.out.println("MDT - ZXING Barocde UTF-8 string detected : " + results.toString());
					  return results;
				  } catch (ReaderException ex) {
					  //the data is improperly formatted
					  System.out.println("MDT - ZXING exception on search QR with image rotation with " + d + " degrees");
					  ex.printStackTrace();
				  }
			  }
		  } else { System.out.println("MDT - Rotation not supported on Image - ZXING exiting");}
		  
	  }
	  return null;
	  
  }


}