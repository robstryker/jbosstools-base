/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.common.model.ui.objecteditor;

import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.common.model.options.PreferenceModelUtilities;
import org.jboss.tools.common.model.ui.wizards.query.AbstractQueryWizard;
import org.jboss.tools.common.model.ui.wizards.query.AbstractQueryWizardView;

public class ExtendedPropertiesWizard extends AbstractQueryWizard {
	
	public static void run(ExtendedProperties attributes) {
		ExtendedPropertiesWizard wizard = new ExtendedPropertiesWizard();
		Properties p = new Properties();
		p.put("extendedProperties", attributes); //$NON-NLS-1$
		p.put("model", PreferenceModelUtilities.getPreferenceModel());		 //$NON-NLS-1$
		wizard.setObject(p);
		wizard.execute();
	}

	public ExtendedPropertiesWizard() {
		setView(new ExtendedPropertiesWizardView());
	}
}

class ExtendedPropertiesWizardView extends AbstractQueryWizardView {
	private ExtendedPropertiesEditor objectEditor = new ExtendedPropertiesEditor();

	public ExtendedPropertiesWizardView() {}

	public String[] getCommands() {
		return new String[]{CLOSE, HELP};
	}

	public String getDefaultCommand() {
		return CLOSE;
	}

	public void setObject(Object data) {
		super.setObject(data);
		Properties p = findProperties(data);
		ExtendedProperties attributes = (ExtendedProperties)p.get("extendedProperties"); //$NON-NLS-1$
		objectEditor.setExtendedProperties(attributes);
		boolean viewMode = p != null && "true".equals(p.getProperty("viewMode")); //$NON-NLS-1$ //$NON-NLS-2$
		objectEditor.setReadOnly(viewMode);
		setWindowTitle("Attributes");
		String nodeName = attributes.getNodeName();
		setTitle((nodeName != null) ? "<" + nodeName + ">" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 10;
		layout.marginHeight = 10;
		layout.verticalSpacing = 10;
		layout.marginWidth = 10;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
		return objectEditor.createControl(composite);
	}
	
	public void stopEditing() {
		if(objectEditor != null) objectEditor.stopEditing();
	}
	
	public void dispose() {
		super.dispose();
		if (objectEditor != null) {
			objectEditor.setExtendedProperties(null);
			objectEditor.dispose();
		}
	}

	public void action(String command) {
		stopEditing();
		if(CLOSE.equals(command)) {
			setCode(0);
			dispose();
		}
		else super.action(command);
	}

}
