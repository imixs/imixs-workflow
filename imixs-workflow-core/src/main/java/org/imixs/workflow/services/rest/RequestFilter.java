package org.imixs.workflow.services.rest;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface RequestFilter {
	public void filter(HttpURLConnection connection) throws IOException;
}
