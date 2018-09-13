// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.login;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HTTPDeleteWithBody extends HttpEntityEnclosingRequestBase {
	public static final String METHOD_NAME = "DELETE";

	public String getMethod() {
		return METHOD_NAME;
	}

	public HTTPDeleteWithBody(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	public HTTPDeleteWithBody(final URI uri) {
		super();
		setURI(uri);
	}

	public HTTPDeleteWithBody() {
		super();
	}
}
