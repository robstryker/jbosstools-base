package org.jboss.tools.foundation.core.credentials.internal;

import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.foundation.core.credentials.ICredentialResult;
import org.jboss.tools.foundation.core.credentials.ICredentialType;

public class StringPasswordCredentialResult implements ICredentialResult {
 	public static final String PROPERTY_PASS = "pass";
	private Map<String, String> map;
	private ICredentialType type;
	public StringPasswordCredentialResult(ICredentialType type, Map<String, String> map) {
		this.map = map;
		this.type = type;
	}
	@Override
	public String stringValue() throws UnsupportedOperationException {
		return map.get(PROPERTY_PASS);
	}

	@Override
	public Map<String, String> toMap() {
		return new HashMap<String,String>(map);
	}

	@Override
	public ICredentialType getType() {
		return type;
	}

}
