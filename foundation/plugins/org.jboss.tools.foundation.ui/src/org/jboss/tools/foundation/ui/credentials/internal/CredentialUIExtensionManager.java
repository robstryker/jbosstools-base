package org.jboss.tools.foundation.ui.credentials.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.foundation.ui.credentials.ICredentialTypeUI;
import org.jboss.tools.foundation.ui.internal.FoundationUIPlugin;
public class CredentialUIExtensionManager {

	private static final String CREDENTIAL_TYPE_UI_EXT_PT = "org.jboss.tools.foundation.ui.credentialTypeUI";

	private static CredentialUIExtensionManager INSTANCE = new CredentialUIExtensionManager();
	public static CredentialUIExtensionManager getDefault() {
		return INSTANCE;
	}
	
	private Map<String, CredentialTypeUIWrapper> credentialTypeUIMap;
	private CredentialUIExtensionManager() {
		// Do nothing
	}
	
	public ICredentialTypeUI createCredentialUI(String typeId) {
		ensureCredentialTypesLoaded();
		CredentialTypeUIWrapper wrap = credentialTypeUIMap.get(typeId);
		if( wrap == null )
			return null;
		return wrap.createUI();
	}
	
	private synchronized void ensureCredentialTypesLoaded() {
		if( credentialTypeUIMap == null ) {
			credentialTypeUIMap = loadCredentialTypeUIs();
		}
	}
	
	private Map<String, CredentialTypeUIWrapper> loadCredentialTypeUIs() {
		IExtension[] extensions = findExtension(CREDENTIAL_TYPE_UI_EXT_PT);
		HashMap<String, CredentialTypeUIWrapper> map = new HashMap<String, CredentialTypeUIWrapper>();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement elements[] = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				String type = elements[j].getAttribute("credentialType");
				CredentialTypeUIWrapper wrap = new CredentialTypeUIWrapper(type, elements[j]);
				map.put(type, wrap);
			}
		}
		return map;
	}

	
	private static IExtension[] findExtension(String extensionId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry
				.getExtensionPoint(extensionId);
		return extensionPoint.getExtensions();
	}

	private static class CredentialTypeUIWrapper {
		String type;
		IConfigurationElement element;
		public CredentialTypeUIWrapper(String typeId, IConfigurationElement element) {
			this.type = typeId;
			this.element = element;
		}
		public ICredentialTypeUI createUI() {
			try {
				return (ICredentialTypeUI) element.createExecutableExtension("class");
			} catch (CoreException e) {
				FoundationUIPlugin.pluginLog().logError(e);
				return null;
			}
		}
	}
}
