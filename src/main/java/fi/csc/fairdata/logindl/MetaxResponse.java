/**
 * 
 */
package fi.csc.fairdata.logindl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pj
 *
 */
public class MetaxResponse {
	private int code;
	private String content;
	private final static Logger LOG = LoggerFactory.getLogger(MetaxResponse.class);
	
	MetaxResponse(int code, String content) {
		this.code = code;
		this.content = content;
		LOG.info("new metax response created with code:{} message:{}", code, content);

	}

	public int getCode() {
		return this.code;
	}

	public String getContent() {
		return this.content;
	}
}
