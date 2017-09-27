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
package org.jboss.tools.foundation.ui.credentials.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.core.credentials.ICredentialDomain;
import org.jboss.tools.foundation.core.credentials.ICredentialType;
import org.jboss.tools.foundation.core.credentials.ICredentialsModel;
import org.jboss.tools.foundation.core.credentials.internal.CredentialExtensionManager;
import org.jboss.tools.foundation.ui.credentials.ICredentialTypeUI;
import org.jboss.tools.foundation.ui.credentials.IValidationCallback;
import org.jboss.tools.foundation.ui.util.FormDataUtility;

public class NewCredentialUserDialog extends TitleAreaDialog implements IValidationCallback {
	private static final int rightMargin = -10;
	
	private ICredentialsModel model;
	private ICredentialDomain selectedDomain;
	private ICredentialType selectedCredentialType;
	private String user;
	private String[] domainNames, typeNames;
	private ICredentialDomain[] allDomains;
	private ICredentialType[] allTypes;
	
	private boolean freezeUser = false;
	private boolean freezeDomain = false;
	private boolean freezeType = false;
	
	private boolean alwaysPrompt = false;
	
	/**
	 * Open a new user dialog.  The selected domain will be pre-selected, but not frozen.
	 * 
	 * @param parentShell
	 * @param model
	 * @param selected
	 */
	public NewCredentialUserDialog(Shell parentShell, ICredentialsModel model, ICredentialDomain selected) {
		super(parentShell);
		this.model = model;
		this.selectedDomain = selected;
		if( selected != null ) 
			freezeDomain = true;
		selectedCredentialType = CredentialService.getCredentialModel().getDefaultCredentialType();
	}
	
	public NewCredentialUserDialog(Shell parentShell, ICredentialsModel model, ICredentialDomain selected, ICredentialType type) {
		this(parentShell, model, selected);
		selectedCredentialType = type;
		if( selectedCredentialType != null ) {
			freezeType = true;
		}
	}
	
	/**
	 * Open a new user dialog.  The selected domain and username will be frozen. 
	 * 
	 * @param parentShell
	 * @param model
	 * @param selected
	 * @param user
	 */
	public NewCredentialUserDialog(Shell parentShell, ICredentialsModel model, ICredentialDomain selected, ICredentialType type, String user) {
		this(parentShell, model, selected, type);
		this.user = user;
		freezeUser = true;
		alwaysPrompt = model.credentialRequiresPrompt(selected, type, user);
	}

	@Override
	public void create() {
		super.create();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setBounds(shell.getLocation().x, shell.getLocation().y, 550, 500);
		Shell s2 = shell.getParent().getShell();
		if( s2 != null )
			shell.setLocation(s2.getLocation());

		if( freezeUser) {
			shell.setText(CredentialMessages.EditACredentialLabel);
		} else
			shell.setText(CredentialMessages.AddACredentialLabel);
	}
    protected int getShellStyle() {
        int ret = super.getShellStyle();
        return ret | SWT.RESIZE;
    }
    
    private void initTitleMessage() {
		if( freezeUser)  {
			setTitle(CredentialMessages.EditACredentialLabel);
			setMessage("Existing passwords will not shown.");
		} else {
			setTitle(CredentialMessages.AddACredentialLabel);
		}
    }
    
    // Return the bottom-most widget
    private Control createDomainWidgets(Composite main) {
		allDomains = model.getDomains();
		domainNames = new String[allDomains.length];
		for( int i = 0; i < allDomains.length; i++ ) {
			domainNames[i] = allDomains[i].getName();
		}
		final Combo domains = new Combo(main, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		Label l = new Label(main, SWT.NONE);
		l.setText(CredentialMessages.DomainLabel);
		l.setLayoutData(		new FormDataUtility().createFormData(0, 12,	null, 0, 0, 10, null, 0));
		domains.setLayoutData(	new FormDataUtility().createFormData(0, 8, 	null, 0, 25, 0, 100, rightMargin));

		domains.setItems(domainNames);
		if( selectedDomain != null ) {
			int sIndex = Arrays.asList(allDomains).indexOf(selectedDomain);
			if( sIndex != -1) {
				domains.select(sIndex);
			}
		}
		
		domains.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int i = domains.getSelectionIndex();
				if( i != -1 ) {
					selectedDomain = allDomains[i];
				}
				validate();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		if( freezeDomain) {
			domains.setEnabled(false);
		}
		
		return domains;
    }
    
    

    // Return the bottom-most widget
    private Control createTypeWidgets(Composite main, Control above) {
		allTypes = model.getCredentialTypes();
		typeNames = new String[allTypes.length];
		for( int i = 0; i < typeNames.length; i++ ) {
			typeNames[i] = allTypes[i].getId();
		}
		final Combo types = new Combo(main, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		Label l = new Label(main, SWT.NONE);
		l.setText("Credential Type:");
		l.setLayoutData(		new FormDataUtility().createFormData(above, 12,	null, 0, 0, 10, null, 0));
		types.setLayoutData(	new FormDataUtility().createFormData(above, 8, 	null, 0, 25, 0, 100, rightMargin));

		types.setItems(typeNames);
		if( selectedCredentialType != null ) {
			int sIndex = Arrays.asList(allTypes).indexOf(selectedCredentialType);
			if( sIndex != -1) {
				types.select(sIndex);
			}
		}
		
		types.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int i = types.getSelectionIndex();
				if( i != -1 ) {
					selectedCredentialType = allTypes[i];
				}
				updatePageBook();
				validate();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		if( freezeType) {
			types.setEnabled(false);
		}
		
		return types;
    }
    
    private Control createUsernameWidgets(Composite main, Control above) {

		Label nameLabel = new Label(main, SWT.None);
		nameLabel.setText(CredentialMessages.UsernameLabel);
		final Text nameText = new Text(main, SWT.SINGLE | SWT.BORDER);
		

		if( user != null ) {
			nameText.setText(user);
		}
		nameLabel.setLayoutData(new FormDataUtility().createFormData(above, 17,	null, 0, 0, 10, null, 0));
		nameText.setLayoutData(	new FormDataUtility().createFormData(above, 13,	null, 0, 25, 0, 100, rightMargin));
		
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				user = nameText.getText();
				validate();
			}
		});
		
		if( freezeUser ) {
			nameText.setEnabled(false);
		}
		return nameText;
    }
	protected Control createDialogArea(Composite parent) {
		initTitleMessage();
		Composite main = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		main.setLayoutData(new GridData(GridData.FILL_BOTH));
		main.setLayout(new FormLayout());
		
		Control domains = createDomainWidgets(main);
		Control types = createTypeWidgets(main, domains);
		Control nameText = createUsernameWidgets(main, types);
		
		Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new FormDataUtility().createFormData(nameText, 29,	null, 0, 0, 10, 100, rightMargin));

		fillDetailsSection(main, separator);
		return main;
	}
	
	
	private ICredentialTypeUI selectedUI;
	private ScrolledPageBook preferencePageBook;
	private Object EMPTY = new Object();
	private Map<ICredentialType, ICredentialTypeUI> typeToUI;
	private void fillDetailsSection(Composite main, Control above) {
		typeToUI = new HashMap<ICredentialType, ICredentialTypeUI>();
		preferencePageBook = new ScrolledPageBook(main, SWT.NONE);
		preferencePageBook.setLayoutData(new FormDataUtility().createFormData(above, 21, 80, -5, 0, 10, 100, rightMargin));
		Composite noTypeSelected = preferencePageBook.createPage(EMPTY);
		noTypeSelected.setLayout(new FillLayout());
		Label selectType = new Label(noTypeSelected, SWT.NONE);
		selectType.setText("Please select a credential type.");
		preferencePageBook.showPage(EMPTY);
		if( selectedCredentialType != null ) {
			updatePageBook();
		}
	}
	
	private void updatePageBook() {
		if( selectedCredentialType != null ) {
			selectedUI = discoverSelectedUI(selectedCredentialType);
			if( selectedUI != null ) {
				if( !preferencePageBook.hasPage(selectedUI)) {
					Composite uiComposite = preferencePageBook.createPage(selectedUI);
					selectedUI.fillComposite(uiComposite, alwaysPrompt, this);
				}
				preferencePageBook.showPage(selectedUI);
			} else {
				preferencePageBook.showPage(EMPTY);
			}
		} else {
			preferencePageBook.showPage(EMPTY);
		}
	}
	
	private ICredentialTypeUI discoverSelectedUI(ICredentialType type) {
		if( typeToUI.get(type) != null ) {
			return typeToUI.get(type);
		}
		ICredentialTypeUI ui = CredentialUIExtensionManager.getDefault().createCredentialUI(type.getId());
		if( ui != null ) {
			typeToUI.put(type,  ui);
		}
		return ui;
	}
	
	private void validate() {
		if( selectedDomain == null ) {
			setMessage(CredentialMessages.SelectDomain, IMessageProvider.ERROR);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return;
		}
		if( user == null || user.isEmpty()) {
			setMessage(CredentialMessages.UsernameCannotBeBlank, IMessageProvider.ERROR);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return;
		}

		String[] names =  selectedDomain.getUsernames();
		if( !freezeUser && Arrays.asList(names).contains(user)) {
			setMessage(NLS.bind(CredentialMessages.UsernameAlreadyExists, user, selectedDomain.getName()), IMessageProvider.ERROR);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return;
		}
		
		IStatus s = selectedUI.validate();
		setValidationStatus(s);
	}

	@Override
	public void setValidationStatus(IStatus s) {
		if( !s.isOK()) {
			// TODO fix this garbage w icon 
			setMessage(s.getMessage(), IMessageProvider.ERROR);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return;
		}
		setMessage(null, IMessageProvider.NONE);
		getButton(IDialogConstants.OK_ID).setEnabled(true);
	}
	
	public ICredentialDomain getDomain() {
		return selectedDomain;
	}
	public String getUser() {
		return user;
	}
	public ICredentialType getCredentialType() {
		return selectedCredentialType;
	}
	public boolean isAlwaysPrompt() {
		return selectedUI.isAlwaysPrompt();
	}
	public Map<String, String> getProperties() {
		return selectedUI.getProperties();
	}
}
