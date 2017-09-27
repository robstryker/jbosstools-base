package org.jboss.tools.foundation.core.credentials;

import java.util.Map;

/**
 * Represents a credential result. This could be a simple 
 * password type, or something with several tokens, etc. 
 */
public interface ICredentialResult {

	/**
	 * Get a string value for this result 
	 * 
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public String stringValue() throws UnsupportedOperationException;
	
	/**
	 * Return the details of this result in map form to be persisted
	 * in secure storage. 
	 * @return
	 */
	public Map<String, String> toMap();
	
	/**
	 * Return the credential type
	 * @return
	 */
	public ICredentialType getType();
}
