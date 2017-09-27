package org.jboss.tools.foundation.ui.credentials.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.foundation.core.credentials.internal.StringPasswordCredentialResult;
import org.jboss.tools.foundation.ui.credentials.ICredentialTypeUI;
import org.jboss.tools.foundation.ui.credentials.IValidationCallback;
import org.jboss.tools.foundation.ui.internal.FoundationUIPlugin;
import org.jboss.tools.foundation.ui.util.FormDataUtility;

public class UserPassTypeUI implements ICredentialTypeUI {
	private String pass;
	private boolean alwaysPrompt;
	private IValidationCallback callback;
	public void fillComposite(Composite main, boolean alwaysPrompt, IValidationCallback vc) {
		this.callback = vc;
		main.setLayout(new FormLayout());
		fillComposite2(main, alwaysPrompt);
	}
	private void fillComposite2(Composite main, boolean alwaysPrompt) {
		this.alwaysPrompt = alwaysPrompt;
		final Button promptBtn = new Button(main, SWT.CHECK);
		promptBtn.setText(CredentialMessages.AlwaysPromptForPasswordLabel);
		promptBtn.setSelection(alwaysPrompt);
		
		Label passLabel = new Label(main, SWT.None);
		passLabel.setText(CredentialMessages.PasswordLabel);
		final Text passText = new Text(main, SWT.SINGLE | SWT.BORDER);

		final Button showPassword = new Button(main, SWT.CHECK );
		showPassword.setText(CredentialMessages.ShowPasswordLabel);
		class SL implements SelectionListener {

			@Override
			public void widgetSelected(SelectionEvent e) {
				passwordVisibility();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				passwordVisibility();
			}

			protected void passwordVisibility() {
				boolean selected = showPassword.getSelection();
				if (selected) {
					passText.setEchoChar('\0');
				} else {
					passText.setEchoChar('*');
				}
			}
		}
		SL sl = new SL();
		showPassword.setSelection(false);
		passText.setEchoChar('*');
		showPassword.addSelectionListener(sl);
		sl.passwordVisibility();


		promptBtn.setLayoutData(new FormDataUtility().createFormData(0,	5, null, 0, 0, 10, 100, -10));
		
		passLabel.setLayoutData(new FormDataUtility().createFormData(promptBtn, 15,	null, 0, 0, 10, null, 0));
		passText.setLayoutData(	new FormDataUtility().createFormData(promptBtn,	11,	null, 0, 25, 0, 100, -10));
		showPassword.setLayoutData(new FormDataUtility().createFormData(passText,	11,	null, 0, passText, -140, 100, -10));
		promptBtn.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = !promptBtn.getSelection();
				passText.setEnabled(enabled);
				showPassword.setEnabled(enabled);
				UserPassTypeUI.this.alwaysPrompt = promptBtn.getSelection();
				validateInternal();
			}
		});
		passText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pass = passText.getText();
				validateInternal();
			}
		});

		if( alwaysPrompt ) {
			passText.setEnabled(false);
		}
	}

	@Override
	public IStatus validate() {
		if( !alwaysPrompt && ( pass == null || pass.isEmpty())) {
			return new Status(IStatus.ERROR, FoundationUIPlugin.PLUGIN_ID, CredentialMessages.PasswordCannotBeBlank);
		}
		return Status.OK_STATUS;
	}
	
	private void validateInternal() {
		IStatus s = validate();
		callback.setValidationStatus(s);
	}
	@Override
	public boolean isAlwaysPrompt() {
		return alwaysPrompt;
	}
	@Override
	public Map<String, String> getProperties() {
		HashMap<String, String> mp = new HashMap<>();
		mp.put(StringPasswordCredentialResult.PROPERTY_PASS, pass);
		return mp;
	}

}
