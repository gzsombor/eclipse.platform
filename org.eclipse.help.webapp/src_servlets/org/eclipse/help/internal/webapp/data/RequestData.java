/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.help.internal.webapp.data;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.HelpSystem;
import org.eclipse.help.internal.webapp.servlet.*;

/**
 * Helper class for contents.jsp initialization
 */
public class RequestData {
	public final static int MODE_WORKBENCH = HelpSystem.MODE_WORKBENCH;
	public final static int MODE_INFOCENTER = HelpSystem.MODE_INFOCENTER;
	public final static int MODE_STANDALONE = HelpSystem.MODE_STANDALONE;
	
	protected ServletContext context;
	protected HttpServletRequest request;
	protected String locale;
	protected WebappPreferences preferences;
	/**
	 * Constructs the data for a request.
	 * @param context
	 * @param request
	 */
	public RequestData(ServletContext context, HttpServletRequest request) {
		this.context = context;
		this.request = request;
		preferences=new WebappPreferences();

		locale = UrlUtil.getLocale(request);
	}

	/**
	 * Returns the preferences object
	 */
	public WebappPreferences getPrefs() {
		return preferences;
	}

	public boolean isGecko() {
		return UrlUtil.isGecko(request);
	}

	public boolean isIE() {
		return UrlUtil.isIE(request);
	}

	public boolean isKonqueror() {
		return UrlUtil.isKonqueror(request);
	}

	public boolean isMozilla() {
		return UrlUtil.isMozilla(request);
	}
	
	public boolean isOpera() {
		return UrlUtil.isOpera(request);
	}
	
	public String getLocale() {
		return locale;
	}
	
	public int getMode(){
		return HelpSystem.getMode();
	}
}