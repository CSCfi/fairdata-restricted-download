/**
 * 
 */
package fi.csc.fairdata.logindl;

/**
 * Tiedoston tietoja idassa
 * 
 * @author pj
 *
 */
public class Tiedosto {

	final String file_path;
	final String identifier;
	
	/**
	 * 
	 * @param file_path String polku from metax parent_directory
	 * @param id String ida-tunniste from file_storage
	 */
	public Tiedosto(String file_path, String id) {
		this.file_path = file_path;
		this.identifier =id;
	}

	public String getFile_path() {
		return file_path;
	}

	public String getIdentifier() {
		return identifier;
	}		
	
	public String toString() {
		return "["+identifier+" "+file_path+"]";
	}
}
