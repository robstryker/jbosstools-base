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
package org.jboss.tools.foundation.core.credentials.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.foundation.core.credentials.ICredentialListener;
import org.jboss.tools.foundation.core.credentials.ICredentialResult;
import org.jboss.tools.foundation.core.credentials.ICredentialType;
import org.jboss.tools.foundation.core.credentials.ICredentialsModel;
import org.jboss.tools.foundation.core.credentials.ICredentialsPrompter;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.foundation.core.internal.FoundationCorePlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class CredentialsModel implements ICredentialsModel {
	private static CredentialsModel instance = new CredentialsModel();
	
	// A preference key where we store the string
	static final String CREDENTIAL_BASE_KEY = "org.jboss.tools.foundation.core.credentials.CredentialsModel";
	
	/*
	 * Internal event types
	 */
	private static final int DOMAIN_ADDED = 1;
	private static final int DOMAIN_REMOVED = 2;
	private static final int CREDENTIAL_ADDED = 3;
	private static final int CREDENTIAL_REMOVED = 4;
	private static final int CREDENTIAL_CHANGED = 5;
	private static final int DEFAULT_CREDENTIAL_CHANGED = 6;
	
	
	public static CredentialsModel getDefault() {
		return instance;
	}
	
	private IEclipsePreferences prefs;
	private HashMap<String, ICredentialDomain> map;
	private ArrayList<ICredentialListener> listeners;
	public CredentialsModel() {
		loadModel();
	}
	
	private void loadModel() {
		map = new HashMap<String, ICredentialDomain>();
		listeners = new ArrayList<>();
		try {
			ICredentialDomain[] domains = loadDomainsFromPreferences();
			for( int i = 0; i < domains.length; i++ ) {
				map.put(domains[i].getId(), domains[i]);
			}
			
			
			// Static domains that must always be present.  For now, hard-coded, but maybe
			// Can be contributed via ext-pt later. 
			if( !map.containsKey(CredentialService.REDHAT_ACCESS)) {
				map.put(CredentialService.REDHAT_ACCESS, new CredentialDomain(CredentialService.REDHAT_ACCESS, CredentialService.REDHAT_ACCESS, false));
			}
			if( !map.containsKey(CredentialService.JBOSS_ORG)) {
				map.put(CredentialService.JBOSS_ORG, new CredentialDomain(CredentialService.JBOSS_ORG, CredentialService.JBOSS_ORG, false));
			}
		} catch(BackingStoreException bse) {
			FoundationCorePlugin.pluginLog().logError("Error loading saved credential data.", bse);
		}
	}
	
	
	/**
	 * Fire the events to listeners
	 * @param type
	 * @param domain
	 * @param user
	 */
	private void fireEvent(int type, ICredentialDomain domain, String user, ICredentialType credentialType) {
		Iterator<ICredentialListener> it = listeners.iterator();
		while(it.hasNext()) {
			switch(type) {
			case DOMAIN_ADDED:
				it.next().domainAdded(domain);
				break;
			case DOMAIN_REMOVED:
				it.next().domainRemoved(domain);
				break;
			case CREDENTIAL_ADDED:
				it.next().credentialAdded(domain, user, credentialType);
				break;
			case CREDENTIAL_REMOVED:
				it.next().credentialRemoved(domain, user, credentialType);
				break;
			case CREDENTIAL_CHANGED:
				it.next().credentialChanged(domain, user, credentialType);
				break;
			case DEFAULT_CREDENTIAL_CHANGED:
				it.next().defaultUsernameChanged(domain, user, credentialType);
				break;
			}
		}
	}

	public void addCredentials(ICredentialDomain domain, String user, String pass) {
		HashMap<String, String> props = new HashMap<String, String>();
		props.put("PASSWORD", pass); // TODO USE PROPER CONSTANT
		addCredentials(domain, user, false, null, props);
	}

	public void addPromptedCredentials(ICredentialDomain domain, String user) {
		addCredentials(domain, user, true, null, Collections.emptyMap());
	}

	@Override
	public void addCredentials(ICredentialDomain domain, ICredentialType type, String user,
			Map<String, String> properties) {
		addCredentials(domain, user, false, type, properties);
	}

	@Override
	public void addPromptedCredentials(ICredentialDomain domain, ICredentialType type, String user) {
		addCredentials(domain, user, true, type, Collections.emptyMap());
	}

	
	private void addCredentials(ICredentialDomain domain, String user, 
			boolean prompt, ICredentialType type, Map<String,String> props) {
		CredentialDomain cd = (CredentialDomain)domain;
		boolean existed = cd.userExists(user, type);
		String preDefault = cd.getDefaultUsername();
		
		if( !prompt )
			((CredentialDomain)domain).addCredentials(user, type, props);
		else
			((CredentialDomain)domain).addPromptedCredentials(user, type);
		
		String postDefault = cd.getDefaultUsername();
		
		// fire credential added or changed
		if( !existed )
			fireEvent(CREDENTIAL_ADDED, domain, user, type);
		else 
			fireEvent(CREDENTIAL_CHANGED, domain, user, type);
		
		
		if( !isEqual(preDefault, postDefault)) {
			fireEvent(DEFAULT_CREDENTIAL_CHANGED, domain, user, type);
		}
	}

	
	public boolean credentialRequiresPrompt(ICredentialDomain domain, String user) {
		return credentialRequiresPrompt(domain, getDefaultCredentialType(), user);
	}
	

	@Override
	public boolean credentialRequiresPrompt(ICredentialDomain domain, ICredentialType type, String user) {
		return ((CredentialDomain)domain).userRequiresPrompt(user, type);
	}
	
	public void removeCredentials(ICredentialDomain domain, ICredentialType type, String user) {
		CredentialDomain cd = (CredentialDomain)domain;
		String preDefault = cd.getDefaultUsername();
		((CredentialDomain)domain).removeCredential(user, type);
		String postDefault = cd.getDefaultUsername();
		fireEvent(CREDENTIAL_REMOVED, domain, user, type);
		if( !isEqual(preDefault, postDefault)) {
			fireEvent(DEFAULT_CREDENTIAL_CHANGED, domain, user, type);
		}
	}
	

	@Override
	public void removeCredentials(ICredentialDomain domain, String user) {
		removeCredentials(domain, getDefaultCredentialType(), user);
	}

	public ICredentialType getDefaultCredentialType() {
		return CredentialExtensionManager.getDefault().getDefaultCredentialType();
	}
	
	public ICredentialType[] getCredentialTypes() {
		return CredentialExtensionManager.getDefault().getCredentialTypes();
	}
	
	public ICredentialType getCredentialType(String id) {
		return CredentialExtensionManager.getDefault().getCredentialType(id);
	}

	
	private boolean isEqual(String one, String two) {
		if( one == null ) {
			return two == null;
		} else {
			return one.equals(two);
		}
	}

	public ICredentialDomain addDomain(String id, String name, boolean removable) {
		if( !map.containsKey(id)) {
			ICredentialDomain d = new CredentialDomain(id, name, removable);
			map.put(d.getId(), d);
			fireEvent(DOMAIN_ADDED, d, null, null);
			return d;
		}
		return null;
	}
	
	public ICredentialDomain[] getDomains() {
		ArrayList<ICredentialDomain> result = new ArrayList<ICredentialDomain>(map.values());
		Collections.sort(result, new Comparator<ICredentialDomain>() {
			public int compare(ICredentialDomain o1, ICredentialDomain o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return (ICredentialDomain[]) result.toArray(new ICredentialDomain[result.size()]);
	}
	
	public ICredentialDomain getDomain(String id) {
		return map.get(id);
	}
	
	public void removeDomain(ICredentialDomain domain) {
		if( domain != null && map.containsKey(domain.getId())) {
			map.remove(domain.getId());
			fireEvent(DOMAIN_REMOVED, domain, null, null);
		}
	}
	
	


	@Override
	public void setDefaultCredential(ICredentialDomain domain, ICredentialType type, String user) {
		String original = ((CredentialDomain)domain).getDefaultUsername();
		ICredentialType originalType = ((CredentialDomain)domain).getDefaultUserType();
		if( (user != null && !user.equals(original)) || (type != null && !type.equals(originalType))) {
			((CredentialDomain)domain).setDefaultUsername(user, type);
			fireEvent(DEFAULT_CREDENTIAL_CHANGED, domain, user, type);
		}
	}
	
	@Override
	public void setDefaultCredential(ICredentialDomain domain, String user) throws IllegalArgumentException {
	}

	
	private ICredentialDomain[] loadDomainsFromPreferences() throws BackingStoreException {
		ArrayList<ICredentialDomain> domains = new ArrayList<ICredentialDomain>();
		IEclipsePreferences root = InstanceScope.INSTANCE.getNode(FoundationCorePlugin.PLUGIN_ID);
		Preferences credentialRoot = root.node(CREDENTIAL_BASE_KEY);
		String[] childNodes = credentialRoot.childrenNames();
		for( int i = 0; i < childNodes.length; i++ ) {
			Preferences domain = credentialRoot.node(childNodes[i]);
			ICredentialDomain cd = new CredentialDomain(domain);
			domains.add(cd);
		}
		return (ICredentialDomain[]) domains.toArray(new ICredentialDomain[domains.size()]);
	}
	

	@Override
	public void saveModel() {
		save();
	}
	
	@Override
	public boolean save() {
		try {
			ISecurePreferences secureRoot = SecurePreferencesFactory.getDefault();
			ISecurePreferences secureCredentialRoot = secureRoot.node(CREDENTIAL_BASE_KEY);
			IEclipsePreferences root = InstanceScope.INSTANCE.getNode(FoundationCorePlugin.PLUGIN_ID);
			Preferences credentialRoot = root.node(CREDENTIAL_BASE_KEY);
			
			ArrayList<ICredentialDomain> domains = new ArrayList<ICredentialDomain>(map.values());
			Iterator<ICredentialDomain> it = domains.iterator();
			while(it.hasNext()) {
				ICredentialDomain d = it.next();
				ISecurePreferences secureDomainNode = secureCredentialRoot.node(d.getId());
				Preferences domainNode = credentialRoot.node(d.getId());
				((CredentialDomain)d).saveToPreferences(domainNode, secureDomainNode);
			}
			
			// Check for any removed domains
			String[] childrenNodes = credentialRoot.childrenNames();
			for( int i = 0; i < childrenNodes.length; i++ ) {
				if( getDomain(childrenNodes[i]) == null) {
					// Domain was deleted, delete the preference node
					credentialRoot.node(childrenNodes[i]).removeNode();
				}
			}
			
			
			credentialRoot.flush();
			secureCredentialRoot.flush();
		} catch(StorageException se) {
			if(se.getErrorCode() == StorageException.NO_PASSWORD) {
				return false;
			}
			FoundationCorePlugin.pluginLog().logError("Error saving credentials in secure storage", se);
		} catch(IOException ioe) {
			FoundationCorePlugin.pluginLog().logError("Error saving credentials in secure storage", ioe);
		} catch(BackingStoreException bse) {
			FoundationCorePlugin.pluginLog().logError("Error saving credentials in secure storage", bse);
		}
		return true;
	}
	
	IEclipsePreferences getPreferences() {
		if (prefs == null) {
			prefs = ConfigurationScope.INSTANCE.getNode(FoundationCorePlugin.PLUGIN_ID);
		}
		return prefs;
	}

	@Override
	public void addCredentialListener(ICredentialListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeCredentialListener(ICredentialListener listener) {
		listeners.remove(listener);
	}

	ICredentialResult promptForCredentials(ICredentialDomain domain, ICredentialType type, String user, boolean canChangeUser) throws UsernameChangedException {
		ICredentialsPrompter passwordProvider = CredentialExtensionManager.getDefault().createPasswordPrompt(type);
		passwordProvider.init(domain, type, user, canChangeUser);
		passwordProvider.prompt();
		String retUser = passwordProvider.getUsername();
		ICredentialResult retPass = passwordProvider.getPassword();
		if( retUser == null || retPass == null || retUser.isEmpty()) {
			return null;
		}
		boolean save = passwordProvider.saveChanges();
		if( save ) {
			// Update the credentials
			addCredentials(domain, retUser, false, retPass.getType(), retPass.toMap());
			save();
		}
		if(!user.equals(retUser)) {
			throw new UsernameChangedException(domain, user, retUser, retPass, passwordProvider.saveChanges());
		}
		return retPass;
	}

	
	ICredentialResult promptForCredentials(ICredentialDomain domain, ICredentialType type, String user) throws UsernameChangedException {
		return promptForCredentials(domain, type, user, true);
	}
	

	ICredentialResult promptForPassword(ICredentialDomain domain, ICredentialType type, String user) {
		try {
			return promptForCredentials(domain, type, user, true);
		} catch(UsernameChangedException uce) {
			// Should *never* happen
			FoundationCorePlugin.pluginLog().logError("Error: username has changed when not allowed", uce);
			return uce.getPassword();
		}
	}

}
