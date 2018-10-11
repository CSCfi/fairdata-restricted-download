/**
 * 
 */
package fi.csc.fairdata.logindl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * Tiedostojen nouto UIDAsta
 * 
 * @author pj
 *
 */
public class Tiedostonkäsittely  {

	private HttpServletResponse response;
	String encoding = null; //UIDAn kirjautumistiedot
	
	/**
	 * encoding sisältää UIDAn kirjautumistiedot
	 */
	public Tiedostonkäsittely(HttpServletResponse response) {
		this.response = response;
		try {
			encoding = Base64.getEncoder().encodeToString((DownloadApplication.getUida()).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void tiedosto(Tiedosto t) {
		HttpURLConnection con = null;
		BufferedOutputStream bof = null;

		try { 
			bof = new BufferedOutputStream(response.getOutputStream());
			//URL url = new URL("https://avaa.tdata.fi/tmp/paituli_78835516.zip");
			URL url = new URL(DownloadApplication.getUidaURL()+t.getIdentifier()+"/download");
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty  ("Authorization", "Basic " + encoding);
			con.setRequestMethod("GET");
			response.setContentLengthLong(con.getContentLength()); //idabytes?
			response.setContentType("application/octet-stream; charset=UTF-8");
			String[] sa = t.getFile_path().split("/");
			String filename = sa[sa.length-1];
			try {
				response.addHeader("Content-Disposition", "attachment; filename=\""+filename
						+ "\"; filename*=UTF-8''" +URLEncoder.encode(filename, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.err.println("UTF-8 ei muka löydy!");
				e.printStackTrace();
			}

			BufferedInputStream in = new BufferedInputStream(con.getInputStream());	
			in.transferTo((OutputStream)bof);
			bof.flush();
			in.close();
			con.disconnect(); //??

		}catch (IOException e2) {
			try {
				int respCode = ((HttpURLConnection)con).getResponseCode();
				InputStream es = ((HttpURLConnection)con).getErrorStream();
				int ret = 0;
				byte[] buf = new byte[8192];
				System.err.print("Ida virhetilanne "+respCode+": ");
				System.err.println(t.getIdentifier()+":");
				while ((ret = es.read(buf)) > 0) {
					bof.write(buf);
					System.err.write(buf); 
					System.err.println();
				}
				es.close();
				//return new MetaxResponse(respCode, buf.toString());
			} catch (IOException e3) {
				System.err.println(e3.getMessage());
			}
			System.err.println(e2.getMessage());
		}

	}
	
	/**
	 * Noutaa UIDAsta ja zippaa joukon tiedostoja. 
	 * 
	 * @param tl List<Tiedosto> lista zipattavista tiedostoista
	 * @param dsid String dataset ID, tulee zipin nimeksi
	 * @param metadata String Datasetin metadata metaxista
	 */
	public void zip(List<Tiedosto> tl, String dsid, String metadata) {
		System.out.println("Zippattavaksi tuli " + tl.size());
		Zip z = new Zip(response);
		String zipfilename = dsid+".zip";
		response.setContentType("application/octet-stream; charset=UTF-8");
		try {
			response.addHeader("Content-Disposition", "attachment; filename=\""+zipfilename
					+ "\"; filename*=UTF-8''" +URLEncoder.encode(zipfilename, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("UTF-8 ei muka löydy!");
			e.printStackTrace();
		}

		tl.forEach(t -> zippaa(t, z));
		/*try {
			z.entry("Metadata.json");
			z.write(metadata);
		} catch (Exception e) {
			
		}*/

		z.sendFinal();
	}

	/**
	 * Hakee yhden tiedoston UIDAsta ja lisää sen streamaten zipiin
	 * 
	 * @param t Tiedosto tiedosto
	 * @param z Zip 
	 */
	private void zippaa(Tiedosto t, Zip z) {
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
		}
	}
}