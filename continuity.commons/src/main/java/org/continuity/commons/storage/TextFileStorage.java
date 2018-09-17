package org.continuity.commons.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class TextFileStorage extends FileStorage<String> {

	private static final String FILE_EXT = ".txt";

	public TextFileStorage(Path storagePath) {
		super(storagePath, "");
	}

	@Override
	protected void write(Path dirPath, String id, String entity) throws IOException {
		Files.write(toPath(dirPath, id), Arrays.asList(entity.split("\\n")), StandardOpenOption.CREATE);
	}

	@Override
	protected String read(Path dirPath, String id) throws IOException {
		return Files.readAllLines(toPath(dirPath, id)).stream().reduce((a, b) -> a + "\n" + b).get();
	}

	@Override
	protected boolean remove(Path dirPath, String id) throws IOException {
		return Files.deleteIfExists(toPath(dirPath, id));
	}

	private Path toPath(Path dirPath, String id) {
		return dirPath.resolve(id + FILE_EXT);
	}

}
