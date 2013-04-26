package org.jboss.tools.stacks.core;

import org.eclipse.osgi.util.NLS;

public class Messages {
	private static final String RESOURCE_NAME = "org.jboss.tools.stacks.core.messages"; //$NON-NLS-1$

	public static String ECFExamplesTransport_Downloading;
	public static String ECFExamplesTransport_Internal_Error;
	public static String ECFExamplesTransport_IO_error;
	public static String ECFExamplesTransport_Loading;
	public static String ECFExamplesTransport_ReceivedSize_Of_FileSize_At_RatePerSecond; 
	public static String ECFExamplesTransport_Server_redirected_too_many_times;
	public static String ECFTransport_Operation_canceled;
	static {
		// initialize resource bundle
		NLS.initializeMessages(RESOURCE_NAME, Messages.class);
	}

	public Messages() {
	}

}
