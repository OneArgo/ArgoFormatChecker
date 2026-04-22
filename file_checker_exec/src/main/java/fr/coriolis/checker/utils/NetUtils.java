package fr.coriolis.checker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NetUtils {
	private static final int TIMEOUT_MS = 15_000;

	public static InputStream openInputStream(String url) throws IOException {

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setInstanceFollowRedirects(true); // follow redirects
		conn.setConnectTimeout(TIMEOUT_MS);
		conn.setReadTimeout(TIMEOUT_MS);
		conn.setRequestProperty("Accept", "application/ld+json");
		conn.setRequestMethod("GET");
		conn.connect();

		int status = conn.getResponseCode();

		// client/server error response
		if (status >= 400) {
			try (InputStream err = conn.getErrorStream()) {
				if (err != null) {
					byte[] buf = new byte[1024];
					while (err.read(buf) != -1) {
						/* drain */ }
				}
			}
			throw new IOException("HTTP " + status + " for " + url);
		}

		return conn.getInputStream();
	}

}
