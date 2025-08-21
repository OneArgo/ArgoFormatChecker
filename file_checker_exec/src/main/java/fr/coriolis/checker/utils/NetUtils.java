package fr.coriolis.checker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class NetUtils {
	private static final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.build(); // need to automatically follow redirection

	public static InputStream openInputStream(String url) throws IOException, InterruptedException {

		// create request
		HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
				.header("Accept", "application/ld+json").GET().build();

		// send request :
		HttpResponse<InputStream> res = httpClient.send(req, BodyHandlers.ofInputStream());

		// client error response
		if (res.statusCode() >= 400) {
			// close stream if serveur send one
			try (InputStream body = res.body()) {
				/* ensure release */ }
			throw new IOException("HTTP " + res.statusCode() + " for " + url);
		}

		return res.body();
	}

}
