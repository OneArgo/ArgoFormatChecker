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
	private final boolean onlineMode;
	private final Path offlineBaseDir; // needed if onlineMode == false

	public static SpecIO getInstance() {
		if (specIOInstance == null) {
			throw new IllegalStateException("SpecIO not initialized");
		}
		return specIOInstance;
	}

	public static void init(boolean onlineMode, String offlineBaseDir) {
		if (specIOInstance != null) {
			return;
		}
		synchronized (SpecIO.class) {
			if (specIOInstance == null) {
				specIOInstance = new SpecIO(onlineMode, (offlineBaseDir == null ? null : Paths.get(offlineBaseDir)));
			}
		}
	}

	private SpecIO(boolean onlineMode, Path offlineBaseDir) {
		this.onlineMode = onlineMode;
		this.offlineBaseDir = offlineBaseDir;

		if (!onlineMode && offlineBaseDir == null) {
			throw new IllegalArgumentException("spec dir required in offline mode");
		}
	}

	// Public methods :
	public InputStream open(String fileName) throws IOException {

		if (onlineMode) {
			String resourcePath = RESOURCES_BASE_PATH + "/" + fileName; // ex: "/specs/spec.properties"
			InputStream in = SpecIO.class.getResourceAsStream(resourcePath);
			if (in == null) {
				throw new FileNotFoundException("File not found: " + resourcePath);
			}
			return in;
		} else {
			Path p = offlineBaseDir.resolve(fileName).normalize();
			if (!Files.exists(p)) {
				throw new FileNotFoundException("File not found: " + p);
			}
			return Files.newInputStream(p);
		}
	}

}
