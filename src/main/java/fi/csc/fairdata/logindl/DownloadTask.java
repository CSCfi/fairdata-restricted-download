/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/** 
 * Hakee yhden tiedoston UIDAsta ja lisää sen streamaten zipiin
 * 
 * 
 * @author pj
 *
 */
public class DownloadTask implements Callable<Boolean> {
	Tiedosto t;
	Zip z;
	String encoding;
	
	/**	 
	 * @param t Tiedosto tiedosto
	 * @param z Zip 
	 * @param encoding String
	 */
	
	public DownloadTask(Tiedosto t, Zip z, String e) {
		this.t = t;
		this.z = z;
		this.encoding = e;
	}
	
	
	public Boolean call() {
		HttpURLConnection con = null;
		z.entry(t.getFile_path());
		try { 
			URL url = new URL(DownloadApplication.getUidaURL()+t.getIdentifier()+"/download");
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty  ("Authorization", "Basic " + encoding);
			con.setRequestMethod("GET");
			InputStream is = con.getInputStream();
			if (null == is) {
				z.getZout().closeEntry();
			} else {
				BufferedInputStream in = new BufferedInputStream(is);	
				in.transferTo((OutputStream)z.getZout());	//virtaa suoraan käyttäjälle
				in.close();
				z.getZout().closeEntry();
				con.disconnect(); //??
			}
		}catch (IOException e2) {
			try {
				int respCode = ((HttpURLConnection)con).getResponseCode();
				InputStream es = ((HttpURLConnection)con).getErrorStream();
				if (null == es) {
					z.getZout().closeEntry();
				} else {
					int ret = 0;
					byte[] buf = new byte[8192];
					System.err.print("Ida zip virhetilanne "+respCode+": ");
					System.err.println(t.getIdentifier()+":");
					while ((ret = es.read(buf)) > 0) {
						z.getZout().write(buf);
						System.err.write(buf); 
						System.err.println();
					}
					es.close();
				}
				//return new MetaxResponse(respCode, buf.toString());
			} catch (IOException e3) {
				System.err.println(e3.getMessage());
			}
			System.err.println(e2.getMessage());
			return false;
		}
		return true;
	}
}


