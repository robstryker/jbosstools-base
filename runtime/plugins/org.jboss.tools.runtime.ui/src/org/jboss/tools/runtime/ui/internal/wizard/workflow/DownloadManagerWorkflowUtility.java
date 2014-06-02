/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.jboss.tools.runtime.ui.internal.wizard.workflow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.internal.preferences.Base64;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.foundation.core.xml.IMemento;
import org.jboss.tools.foundation.core.xml.XMLMemento;
import org.jboss.tools.foundation.ui.xpl.taskwizard.WizardFragment;
import org.jboss.tools.runtime.core.model.DownloadRuntime;
import org.jboss.tools.runtime.ui.RuntimeUIActivator;

/**
 * A utility class for running the remote download-manager header commands
 * to verify if the downloadRuntime is set to be downloaded. 
 */
public class DownloadManagerWorkflowUtility {
	public static final int AUTHORIZED = 1;
	public static final int CREDENTIALS_FAILED = 2;
	public static final int WORKFLOW_FAILED = 3;
	
	
	public static int getWorkflowStatus(DownloadRuntime dr, String userS, String passS) 
			throws CoreException, MalformedURLException, IOException {
		int response = headerOnlyStatusCode(dr, userS, passS);
		if( response == 401 ) {
			// 401 means bad credentials, change nothing
			return CREDENTIALS_FAILED;
		} else if( response == 403 || response == 200) { 
			// 403 means workflow incomplete / forbidden, need a child page
			return WORKFLOW_FAILED;
		} else if( response == 302 ) {
			// 302 means all's clear / redirect,  no child page needed
			return AUTHORIZED;
		}
		throw new CoreException(new Status(IStatus.ERROR, RuntimeUIActivator.PLUGIN_ID, "Unknown response code: " + response));

	}
	
	private static int headerOnlyStatusCode(DownloadRuntime dr, String userS, String passS)
			throws CoreException, MalformedURLException, IOException {
		HttpURLConnection con = getWorkflowConnection(dr, userS, passS, "HEAD", true);
		int response = con.getResponseCode();
		con.disconnect();
		return response;
	}

	
	// This is a connection to see where we stand in the workflow
	private static HttpURLConnection getWorkflowConnection(DownloadRuntime dr, 
			String user, String pass, String requestMethod, boolean useXMLHeader) 
			throws IOException, MalformedURLException {
		String url = dr.getUrl();
		HttpURLConnection con =
				(HttpURLConnection) new URL(url).openConnection();
		con.setInstanceFollowRedirects(false);
		String userCredentials = user+ ":" + pass;
		String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
		con.setRequestProperty ("Authorization", basicAuth);
		if( useXMLHeader ) {
			con.setRequestProperty("Content-Type", "application/xml");
		    con.setRequestProperty("Accept", "application/xml");
		}
		con.setRequestMethod(requestMethod);
		return con;
	}

	private static String findNextStep(String responseContent) {
		XMLMemento m = XMLMemento.createReadRoot(new ByteArrayInputStream(responseContent.getBytes()));
		IMemento workflow = m.getChild("workflow");
		IMemento step = workflow.getChild("step");
		String nextStep = ((XMLMemento)step).getTextData();
		return nextStep;
	}
	
	public static String getWorkflowResponseContent(DownloadRuntime dr, String userS, String passS) throws IOException {
		String result = "";
		HttpURLConnection con = getWorkflowConnection(dr, userS, passS, "GET", true);
		
		// We need to get the content of this response to see what the next step is
        InputStream stream = con.getInputStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = br.readLine()) != null) {
	        result+= line;
        }

        con.disconnect();
        br.close();
        return result;
	}
	
	public static WizardFragment getNextWorkflowFragment(String response) {
		String nextStep = findNextStep(response);
		if( "termsAndConditions".equals(nextStep)) { //$NON-NLS-1$
			// return the tc page
			return new DownloadManagerTermsAndConditionsFragment();
		}
		return null;
	}
}
