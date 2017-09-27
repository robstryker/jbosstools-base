package org.jboss.tools.foundation.core.credentials.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.foundation.core.credentials.ICredentialType;
import org.jboss.tools.foundation.core.credentials.ICredentialsPrompter;
import org.jboss.tools.foundation.core.internal.FoundationCorePlugin;

public class CredentialExtensionManager {

	private static final String CREDENTIAL_PROMPTER_EXT_PT = "org.jboss.tools.foundation.core.credentialPrompter";
	private static final String CREDENTIAL_TYPE_EXT_PT = "org.jboss.tools.foundation.core.credentialType";

	private static CredentialExtensionManager INSTANCE = new CredentialExtensionManager();
	public static CredentialExtensionManager getDefault() {
		return INSTANCE;
	}
	
	private Map<String, CredentialTypeWrapper> credentialTypeMap;
	private CredentialExtensionManager() {
		// Do nothing
	}
	
	
	/**
	 * Create a password prompter on the fly.
	 * @return
	 */
	public ICredentialsPrompter createPasswordPrompt(ICredentialType type) {
		IExtension[] extensions = findExtension(CREDENTIAL_PROMPTER_EXT_PT);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement elements[] = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				String typeId = elements[j].getAttribute("credentialType");
				if( typeId != null && typeId.equals(type.getId())) {
					try {
						return (ICredentialsPrompter) elements[j].createExecutableExtension("class");
					} catch (InvalidRegistryObjectException e) {
						FoundationCorePlugin.pluginLog().logError("Unable to load a credential prompter for extension point " + CREDENTIAL_PROMPTER_EXT_PT);
					} catch (CoreException e) {
						FoundationCorePlugin.pluginLog().logError("Unable to load a credential prompter for extension point " + CREDENTIAL_PROMPTER_EXT_PT);
					}
				}
			}
		}
		return null;
	}

	public ICredentialType[] getCredentialTypes() {
		ensureCredentialTypesLoaded();
		ArrayList<CredentialTypeWrapper> l = new ArrayList<>(credentialTypeMap.values());
		List<ICredentialType> l2 = l.stream().map(t -> t.type).collect(Collectors.toList());
		return (ICredentialType[]) l2.toArray(new ICredentialType[l2.size()]);
	}
	
	public ICredentialType getCredentialType(String id) {
		ensureCredentialTypesLoaded();
		CredentialTypeWrapper ret = credentialTypeMap.get(id);
		return ret == null ? null : ret.type;
	}
	
	public ICredentialType getDefaultCredentialType() {
		ensureCredentialTypesLoaded();
		List<CredentialTypeWrapper> l = credentialTypeMap.values().stream().filter(t -> t.def).collect(Collectors.toList());
		return l != null && l.size() > 0 && l.get(0) != null ? l.get(0).type : null;
	}

	
	private synchronized void ensureCredentialTypesLoaded() {
		if( credentialTypeMap == null ) {
			credentialTypeMap = loadCredentialTypes();
		}
	}
	
	private Map<String, CredentialTypeWrapper> loadCredentialTypes() {
		IExtension[] extensions = findExtension(CREDENTIAL_TYPE_EXT_PT);
		MultiStatus ms = new MultiStatus(FoundationCorePlugin.PLUGIN_ID, 0, "Errors while loading credential type extensions.", null);
		HashMap<String, CredentialTypeWrapper> map = new HashMap<String, CredentialTypeWrapper>();
		
		boolean foundDefault = false;
		
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement elements[] = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				try {
					ICredentialType type = (ICredentialType) elements[j].createExecutableExtension("class");
					if( type == null ) {
						ms.add(FoundationCorePlugin.statusFactory().errorStatus("Null credential type in extension point " + CREDENTIAL_TYPE_EXT_PT));
					} else if( type.getId() == null ) {
						ms.add(FoundationCorePlugin.statusFactory().errorStatus("Incomplete credential type in extension point " + CREDENTIAL_TYPE_EXT_PT + ": " + type.getClass().getName()));
					} else if( map.get(type.getId()) != null ) {
						ms.add(FoundationCorePlugin.statusFactory().errorStatus("Competing implementations for credential type " + type.getId() + " in extension point " + CREDENTIAL_TYPE_EXT_PT));
					} else {
						String def = elements[j].getAttribute("default");
						boolean b = Boolean.parseBoolean(def);
						if( foundDefault && b ) {
							ms.add(FoundationCorePlugin.statusFactory().errorStatus("More than one credential type marked as default. Default status removed from credential type " + type.getId() + " in extension point " + CREDENTIAL_TYPE_EXT_PT));
							b = false;
						} else if( b ) {
							foundDefault = true;
						}
						map.put(type.getId(), new CredentialTypeWrapper(type, b));
					}
				} catch (InvalidRegistryObjectException e) {
					ms.add(FoundationCorePlugin.statusFactory().errorStatus("Unable to load a credential type via extension point " + CREDENTIAL_TYPE_EXT_PT,e));
				} catch (CoreException e) {
					ms.add(FoundationCorePlugin.statusFactory().errorStatus("Unable to load a credential type via extension point " + CREDENTIAL_TYPE_EXT_PT,e));
				}
			}
		}
		if( !ms.isOK()) {
			FoundationCorePlugin.pluginLog().logStatus(ms);
		}
		return map;
	}

	
	private static IExtension[] findExtension(String extensionId) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry
				.getExtensionPoint(extensionId);
		return extensionPoint.getExtensions();
	}

	private static class CredentialTypeWrapper {
		ICredentialType type;
		boolean def = false;
		public CredentialTypeWrapper(ICredentialType type, boolean def) {
			this.type = type;
			this.def = def;
		}
	}
}
