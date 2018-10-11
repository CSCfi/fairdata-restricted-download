/**
 * 
 */
package fi.csc.fairdata.logindl;

/**
 * @author pj
 *
 */
public class MetaxResponse {
	private int code;
	private String content;
	
	MetaxResponse(int code, String content) {
		this.code = code;
		this.content = content;
	}

	public int getCode() {
		return this.code;
	}

	public String getContent() {
		return this.content;
	}
}
