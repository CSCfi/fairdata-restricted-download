/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
//import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
//import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
//import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
 
/**
 * @author pj
 *
 */
public class Zip {
	
	//ByteArrayOutputStream baos;
	ZipOutputStream zout;
	BufferedOutputStream bos;
	//HttpServletResponse response;
	//private final Semaphore available = new Semaphore(1);
	private final static Logger LOG = LoggerFactory.getLogger(Zip.class);

	public Zip(HttpServletResponse r) {
		try {
			this.bos = new BufferedOutputStream(r.getOutputStream());
			this.zout = new ZipOutputStream(this.bos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//this.response = r;
	}

	public void entry(String path) {
		
		String clean = path.trim();
		if (clean.startsWith("/")) // poistetaan jottei zip:ssa ole absolutti polkuja
			clean = clean.substring(1);
		try {
			//available.acquire();
			zout.putNextEntry(new ZipEntry(clean));
		} catch (IOException e) {
			LOG.debug( e.getMessage() );
			//available.release();
		} /*catch (InterruptedException e) {
			System.err.println("Zip jonotus keskeytyi: entry");
			e.printStackTrace();
		}	*/
	}
	

	public void sendFinal() {

		try {
			zout.finish();
		} catch (ZipException ze) {
			LOG.error("ZIP file error has occurred when finishing zip: {}", ze.getMessage());
		} catch (IOException e) {
			LOG.error("an I/O exception has occurred when finishing zip: {}", e.getMessage());
		}

		try {
			zout.flush();
		} catch (IOException e) {
			LOG.error("zout flush: {}", e.getMessage());
		}
		try {
			bos.flush();
		} catch (IOException e) {
			LOG.error("bos flush: {}", e.getMessage());
		}
		try {
			zout.close();
		} catch (IOException e) {
			LOG.error("zout flush: {}", e.getMessage());
		}
		try {
			bos.close();
		} catch (IOException e) {
			LOG.error("bos flush: {}", e.getMessage());
		}
	}
	ZipOutputStream getZout() {
		return zout;
	}
	
	public void release() {
		//available.release();
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
