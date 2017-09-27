package org.jboss.tools.foundation.core.credentials;

import java.util.Map;

import org.jboss.tools.foundation.core.credentials.internal.StringPasswordCredentialResult;

public class UserPasswordCredentialType implements ICredentialType {

	public static final String TYPE_ID = "user-pass";
	public UserPasswordCredentialType() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getId() {
		return TYPE_ID;
	}

	@Override
	public ICredentialResult resolveCredentials(ICredentialDomain domain, String user, Map<String, String> details) {
		return new StringPasswordCredentialResult(this, details);
	}

}
