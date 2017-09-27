/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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

public interface ICredentialType {
	/**
	 * Get an ID for this credential type
	 * @return
	 */
	public String getId();
	
	/**
	 * Load a saved key / token / password from secure storage, 
	 * or fetch a new key / token / etc on the fly given these details
	 * 
	 * @param domain
	 * @param user
	 * @param details
	 * @return
	 */
	public ICredentialResult resolveCredentials(ICredentialDomain domain, 
			String user, Map<String, String> details);
}
