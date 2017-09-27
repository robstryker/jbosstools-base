package org.jboss.tools.foundation.ui.credentials;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;

public interface ICredentialTypeUI {
	public void fillComposite(Composite parent, boolean alwaysPrompt, IValidationCallback vc);
	
	public IStatus validate();

	public boolean isAlwaysPrompt();

	public Map<String, String> getProperties();
}
