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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.foundation.core.credentials.ICredentialResult;
import org.jboss.tools.foundation.core.credentials.ICredentialType;
import org.jboss.tools.foundation.core.credentials.UsernameChangedException;
import org.jboss.tools.foundation.core.internal.FoundationCorePlugin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class CredentialDomain implements ICredentialDomain {

	// Properties used in the secure storage model
	static final String PROPERTY_ID = "id";
	static final String PROPERTY_NAME = "name";
	static final String PROPERTY_REMOVABLE = "removable";
	static final String PROPERTY_PASS = "pass";
	static final String PROPERTY_DEFAULT_USER = "default.user";
	static final String PROPERTY_DEFAULT_TYPE = "default.type";
	static final String PROPERTY_USER_LIST = "user.list";
	static final String PROPERTY_PROMPTED_USER_LIST = "user.list.prompted";
	
	static final Map<String, String> NOT_LOADED_MAP = new HashMap<String, String>();
	
	
	private String userVisibleName;
	private String id, defaultUsername;
	private ICredentialType defaultUserType;
	private boolean removable;
	private HashMap<UserType, Map<String, String>> credentials;
	private ArrayList<UserType> promptedCredentials;
	public CredentialDomain(String id, String name, boolean removable) {
		if(id == null) {
			throw new IllegalArgumentException("Id cannot be null.");
		}
		this.id = id;
		this.userVisibleName = name;
		this.removable = removable;
		this.defaultUsername = null;
		this.credentials = new HashMap<UserType, Map<String, String>>();
		this.promptedCredentials = new ArrayList<UserType>();
	}
	
	public CredentialDomain(Preferences pref) throws BackingStoreException {
		this.id = pref.get(PROPERTY_ID, ""); //Id cannot be null.
		this.userVisibleName = pref.get(PROPERTY_NAME, (String)null);;
		this.removable = pref.getBoolean(PROPERTY_REMOVABLE, true);
		this.defaultUsername = pref.get(PROPERTY_DEFAULT_USER, (String)null);

		credentials = new HashMap<UserType, Map<String, String>>();
		String usersList = pref.get(PROPERTY_USER_LIST, (String)null);
		if( usersList != null && !usersList.isEmpty()) {
			// Format is one line per user
			// username;credentialTypeId\n
			String[] users = (usersList == null ? new String[0] : usersList.split("\n"));
			for( int i = 0; i < users.length; i++ ) {
				String userLine = users[i];
				UserType ut = nodeNameToUserType(userLine);
				credentials.put(ut, NOT_LOADED_MAP);
			}
		}
		
		String promptedUserList = pref.get(PROPERTY_PROMPTED_USER_LIST, (String)null);
		promptedCredentials = new ArrayList<UserType>();
		if(promptedUserList != null && !promptedUserList.isEmpty()) {
			String[] users = (usersList == null ? new String[0] : promptedUserList.split("\n"));
			for( int i = 0; i < users.length; i++ ) {
				String userLine = users[i];
				String[] parts = userLine.split(";");
				if( parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) {
					UserType ut = new UserType(parts[0], getCredentialType(parts[1]));
					promptedCredentials.add(ut);
				} else {
					UserType ut = new UserType(parts[0], getDefaultCredentialType());
					promptedCredentials.add(ut);
				}
			}
		}
		
		if( defaultUsername == null || !userExists(defaultUsername)) {
			// The default name doesn't exist, so we need another. 
			String[] users = getUsernames();
			if( users.length > 0) {
				defaultUsername = users[0];
			}
		}
	}
	
	private ICredentialType getCredentialType(String id) {
		return CredentialExtensionManager.getDefault().getCredentialType(id);
	}
	private ICredentialType getDefaultCredentialType() {
		return CredentialExtensionManager.getDefault().getDefaultCredentialType();
	}
	public String getId() {
		return id;
	}
	public boolean getRemovable() {
		return removable;
	}

	/**
	 * Since returned value is used by UI, returning null may cause NPE, 
	 * and therefore should be checked for null at each call.
	 * It is better not to return null.  
	 */
	@Override
	public String getName() {
		return emptyOrNull(userVisibleName) ? (emptyOrNull(id) ? "" : id) : userVisibleName;
	}
	
	private boolean emptyOrNull(String s) {
		return s == null ? true : s.isEmpty();
	}
	

	@Override
	public boolean userExists(String user) {
		List<String> ls = credentials.keySet().stream()
				.filter(t -> t.getUser().equals(user))
				.map(UserType::getUser)
				.collect(Collectors.toList());
		List<String> ls2 = promptedCredentials.stream()
				.filter(t -> t.getUser().equals(user))
				.map(UserType::getUser)
				.collect(Collectors.toList());		
		return ls.size() > 0 || ls2.size() > 0;
	}
	@Override
	public boolean userExists(String user, ICredentialType type) {
		UserType ut = new UserType(user, type);
		return credentials.containsKey(ut) || promptedCredentials.contains(ut);
	}
	
	public boolean userRequiresPrompt(String user, ICredentialType type) {
		return promptedCredentials.contains(new UserType(user, type));
	}
	
	public String[] getUsernames() {
		SortedSet<String> ret = new TreeSet<String>();
		List<String> ls = credentials.keySet().stream().map(UserType::getUser).collect(Collectors.toList());
		List<String> ls2 = promptedCredentials.stream().map(UserType::getUser).collect(Collectors.toList());
		ret.addAll(ls);
		ret.addAll(ls2);
		return (String[]) ret.toArray(new String[ret.size()]);
	}
	

	@Override
	public ICredentialType[] getCredentialTypes(String user) {
		List<ICredentialType> ls = credentials.keySet().stream()
				.filter(t -> t.getUser().equals(user))
				.map(UserType::getType)
				.collect(Collectors.toList());
		List<ICredentialType> ls2 = promptedCredentials.stream()
				.filter(t -> t.getUser().equals(user))
				.map(UserType::getType)
				.collect(Collectors.toList());
		Set<ICredentialType> s = new HashSet<>();
		s.addAll(ls);
		s.addAll(ls2);
		return (ICredentialType[]) s.toArray(new ICredentialType[s.size()]);
	}
	
	protected void addCredentials(String user, ICredentialType type, Map<String, String> props) {
		if( defaultUsername == null )
			defaultUsername = user;
		UserType ut = new UserType(user, type);
		promptedCredentials.remove(ut);
		credentials.put(ut, props);
	}

	protected void addPromptedCredentials(String user, ICredentialType type) {
		if( defaultUsername == null ) 
			defaultUsername = user;
		UserType ut = new UserType(user, type);
		credentials.remove(ut);
		promptedCredentials.add(ut);
	}

	protected void removeCredential(String user, ICredentialType type) {
		credentials.remove(new UserType(user, type));
		promptedCredentials.remove(new UserType(user, type));
		if( user.equals(defaultUsername)) {
			String[] usernames = getUsernames();
			if( usernames.length == 0 ) {
				defaultUsername = null;
			} else {
				defaultUsername = usernames[0];
			}
		}
	}
	
	@Override
	public ICredentialResult getCredentials(String user, ICredentialType type) throws StorageException, UsernameChangedException {
		return getCredentials(user, type, true);
	}
	
	@Override
	public ICredentialResult getPassword(String user, ICredentialType type) throws StorageException {
		try {
			return getCredentials(user, type, false);
		} catch(UsernameChangedException uce) {
			// Should never happen
			FoundationCorePlugin.pluginLog().logError("User attempted to change username when not allowed", uce);
		}
		return null;
	}
	

	public ICredentialResult getCredentials(String user, ICredentialType type, boolean canChangeUser) throws StorageException, UsernameChangedException {
		if( userExists(user, type)) {
			if( !userRequiresPrompt(user, type)) {
				Map<String, String> properties = credentials.get(new UserType(user, type));
				return fetchCredentials(user, type, properties);
			}
		} 
		
		if( canChangeUser ) {
			return CredentialsModel.getDefault().promptForCredentials(this, type, user);
		} else if( user != null){
			return CredentialsModel.getDefault().promptForPassword(this, type, user);
		} else {
			return null;
		}
	}
	
	private ICredentialResult fetchCredentials(String user, ICredentialType type, Map<String, String> details) throws StorageException {
		if( NOT_LOADED_MAP.equals(details)) {
			ISecurePreferences secureRoot = SecurePreferencesFactory.getDefault();
			ISecurePreferences secureCredentialRoot = secureRoot.node(CredentialsModel.CREDENTIAL_BASE_KEY);
			ISecurePreferences secureDomain = secureCredentialRoot.node(getId());
			ISecurePreferences secureUser = secureDomain.node(getNodeName(new UserType(user, type)));
			String[] keys = secureUser.keys();
			details = new HashMap<String, String>();
			for( int i = 0; i < keys.length; i++ ) {
				String v = secureUser.get(keys[i], (String)null);
				details.put(keys[i], v);
			}
			credentials.put(new UserType(user, type),  details);
		}
		return toCredentialResult(user, type, details);
	}
	
	public void setCredentialProperties(String user, ICredentialType type, Map<String, String> details, boolean save) {
		credentials.put(new UserType(user, type),  details);
		if( save ) {
			CredentialService.getCredentialModel().save();
		}
	}
	
	public void setCredentialProperty(String user, ICredentialType type, String key, String value, boolean save) {
		Map<String, String> map = credentials.get(new UserType(user, type));
		if( map != null ) {
			map.put(key,  value);
		} else {
			// TODO error??
		}
		if( save ) {
			CredentialService.getCredentialModel().save();
		}
	}
	
	private ICredentialResult toCredentialResult(String user, ICredentialType type, Map<String, String> details) {
		return type.resolveCredentials(this, user, details);
	}
	
	private String toUserListString(Set<UserType> set) {
		StringBuilder sb = new StringBuilder();
		Iterator<UserType> it = set.iterator();
		UserType ut = null;
		while(it.hasNext()) {
			ut = it.next();
			sb.append(ut.getUser());
			sb.append(";");
			sb.append(ut.getType().getId());
			sb.append("\n");
		}
		return sb.toString().trim();
	}
	
	void saveToPreferences(Preferences prefs, ISecurePreferences securePrefs) throws StorageException {
		prefs.put(PROPERTY_ID, id);
		prefs.put(PROPERTY_NAME, getName());
		prefs.putBoolean(PROPERTY_REMOVABLE, removable);
		if( defaultUsername != null ) 
			prefs.put(PROPERTY_DEFAULT_USER, defaultUsername);
		if( defaultUserType != null ) 
			prefs.put(PROPERTY_DEFAULT_TYPE, defaultUserType.getId());
		
		Set<UserType> users = credentials.keySet();
		prefs.put(PROPERTY_USER_LIST, toUserListString(users));
		Set<UserType> promptedUsers = new HashSet<>(promptedCredentials);
		prefs.put(PROPERTY_PROMPTED_USER_LIST, toUserListString(promptedUsers));
		
		String[] childNodes = securePrefs.childrenNames();
		for( int i = 0; i < childNodes.length; i++ ) {
			// Delete old nodes that are no longer in the model
			ISecurePreferences userNode = securePrefs.node(childNodes[i]);
			UserType asUserType = nodeNameToUserType(childNodes[i]);
			if( !users.contains(asUserType)) {
				userNode.removeNode();
			} else {
				// Goal here is to force a secure-storage event
				userNode.get(PROPERTY_PASS, (String)null);
			}
		}
		
		// Save the password securely
		Iterator<UserType> it = users.iterator();
		while(it.hasNext()) {
			UserType ut = it.next();
			ISecurePreferences userNode = securePrefs.node(getNodeName(ut));
			Map<String, String> props = credentials.get(ut);
			if( props != NOT_LOADED_MAP) {
				Iterator<String> keyIt = props.keySet().iterator();
				while(keyIt.hasNext()) {
					String k = keyIt.next();
					String v = props.get(k);
					userNode.put(k, v, true);
				}
			}
		}
	}

	private String getNodeName(UserType ut) {
		return ut.getUser() + ";" + ut.getType().getId();
	}
	private UserType nodeNameToUserType(String nodeName) {
		String[] parts = nodeName.split(";");
		if( parts.length > 1 ) {
			return new UserType(parts[0], getCredentialType(parts[1]));
		} else {
			return new UserType(parts[0], getDefaultCredentialType());
		}
	}
	
	public String getDefaultUsername() {
		return defaultUsername;
	}

	public ICredentialType getDefaultUserType() {
		return defaultUserType;
	}
	
	public void setDefaultUsername(String user, ICredentialType type) throws IllegalArgumentException {
		if( !userExists(user)) {
			throw new IllegalArgumentException("User " + user + " does not exist for this domain.");
		}
		if( !userExists(user, type)) {
			throw new IllegalArgumentException("User " + user + " of type " + type.getId() + " does not exist for this domain.");
		}
		defaultUsername = user;
		defaultUserType = type;
	}
	
	private static class UserType {
		private ICredentialType type;
		private String user;

		public UserType(String user, ICredentialType type) {
			this.type = type;
			this.user = user; 
		}

		public ICredentialType getType() {
			return type;
		}

		public String getUser() {
			return user;
		}
		public int hashCode() {
			return (user + "/" + type.getId()).hashCode();
		}
		@Override
		public boolean equals(Object o) {
			if( o instanceof UserType ) {
				UserType other = (UserType)o;
				return getUser() != null && getUser().equals(other.getUser()) && 
						getType().getId().equals(other.getType().getId());
			}
			return false;
		}
	}

}