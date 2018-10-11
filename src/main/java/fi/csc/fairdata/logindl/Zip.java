/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
//import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
 
/**
 * @author pj
 *
 */
public class Zip {
	
	ByteArrayOutputStream baos;
	ZipOutputStream zout;
	HttpServletResponse response;
	
	public Zip(HttpServletResponse r) {
		
		try {
			this.zout = new ZipOutputStream(r.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.response = r;
	}

	public void entry(String path) {
		
		String clean = path.trim();
		if (clean.startsWith("/")) // poistetaan jottei zip:ssa ole absolutti polkuja
			clean = clean.substring(1);
		try {
			zout.putNextEntry(new ZipEntry(clean));
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	

	public void sendFinal() {
		try {
			zout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	ZipOutputStream getZout() {
		return zout;
	}

	/**
	 * Kirjoitta merkkijonon tiedostoksi zippiin. Tiedoston nimi tulee edeltävästä entry kutsusta
	 *  
	 * @param metadata String
	 */
	public void write(String metadata) {
		byte[] b = metadata.getBytes(StandardCharsets.UTF_8);
		try {
			zout.write(b);
		} catch (IOException e) {
			System.err.println("Can't write Metadata.json");
			e.printStackTrace();
		}
		
	}
}
