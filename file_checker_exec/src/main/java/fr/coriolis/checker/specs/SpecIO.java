package fr.coriolis.checker.specs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SpecIO {

	// --- Singleton ---
	private static volatile SpecIO specIOInstance;

	private final String RESOURCES_BASE_PATH = "/file_checker_spec";
	private final boolean internalSpecs;
	private final Path externalBaseDir; // needed if internalSpecs == false

	public static SpecIO getInstance() {
		if (specIOInstance == null) {
			throw new IllegalStateException("SpecIO not initialized");
		}
		return specIOInstance;
	}

	public static void init(boolean internalSpecs, String externBaseDir) {
		if (specIOInstance != null) {
			return;
		}
		synchronized (SpecIO.class) {
			if (specIOInstance == null) {
				specIOInstance = new SpecIO(internalSpecs, (externBaseDir == null ? null : Paths.get(externBaseDir)));
			}
		}
	}

	private SpecIO(boolean internalSpecs, Path externalBaseDir) {
		this.internalSpecs = internalSpecs;
		this.externalBaseDir = externalBaseDir;

		if (!internalSpecs && externalBaseDir == null) {
			throw new IllegalArgumentException("spec dir required in offline mode");
		}
	}

	// Public methods :
	public InputStream open(String fileName) throws IOException {

		if (internalSpecs) {
			String resourcePath = RESOURCES_BASE_PATH + "/" + fileName; // ex: "/specs/spec.properties"
			InputStream in = SpecIO.class.getResourceAsStream(resourcePath);
			if (in == null) {
				throw new FileNotFoundException("File not found: " + resourcePath);
			}
			return in;
		} else {
			Path p = externalBaseDir.resolve(fileName).normalize();
			if (!Files.exists(p)) {
				throw new FileNotFoundException("File not found: " + p);
			}
			return Files.newInputStream(p);
		}
	}

}
