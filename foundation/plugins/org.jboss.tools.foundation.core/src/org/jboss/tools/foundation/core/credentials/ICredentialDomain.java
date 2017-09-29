/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.foundation.core.credentials;

import java.util.Map;

import org.eclipse.equinox.security.storage.StorageException;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICredentialDomain {

	/**
	 * Get the internal id for this credential domain
	 * @return
	 */
	public String getId();
	
	/**
	 * Get whether this credential domain can be removed by the user or is permanent.
	 * @return
	 */
	public boolean getRemovable();
	
	/**
	 * Get the user-visible name of this credential domain
	 * Does not return null, to prevent necessity to check for null at each call,
	 * since most UI clients calling this methods do not allow null. 
	 *   
	 * @return 
	 */
	public String getName();
	
	/**
	 * Does the given username exist in the model
	 * @param user
	 * @return
	 */
	public boolean userExists(String user);

	/**
	 * Does the given username exist in the model
	 * @param user
	 * @param type
	 * @return
	 */
	public boolean userExists(String user, ICredentialType type);

	/**
	 * Get a list of usernames persisted for this domain
	 * @return
	 */
	public String[] getUsernames();
	
	/**
	 * Get the list of credential types for the given user
	 * @param user
	 * @return
	 */
	public ICredentialType[] getCredentialTypes(String user);
	
	/**
	 * Get the password for the given username
	 * @param user
	 * @param credential type
	 * @return
	 * @throws StorageException
	 * @throws UsernameChangedException if the user has changed the username when prompted 
	 */
	public ICredentialResult getCredentials(String user, ICredentialType type) throws StorageException, UsernameChangedException;
	
	/**
	 * Get the password for the given username. 
	 * The user has no opportunity to change the username.
	 * 
	 * @param user
	 * @param credential type
	 * @return
	 * @throws StorageException
	 */
	public ICredentialResult getPassword(String user, ICredentialType type) throws StorageException;
	
	/**
	 * Get the default username for this domain, if one exists, or null
	 * @return
	 */
	public String getDefaultUsername();
	
	/**
	 * Set a single property for a given user/type combination
	 * @param user
	 * @param type
	 * @param key
	 * @param value
	 * @param save
	 */
	public void setCredentialProperty(String user, ICredentialType type, String key, String value, boolean save);
	
	/**
 	 * Set all properties for a given user/type combination
	 * @param user
	 * @param type
	 * @param details
	 * @param save
	 */
	public void setCredentialProperties(String user, ICredentialType type, Map<String, String> details, boolean save);

}
