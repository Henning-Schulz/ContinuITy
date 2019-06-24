package org.continuity.idpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Date;

import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.SystemModelTestInstance;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.continuity.idpa.storage.IdpaStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class SystemModelRepositoryTest {

	private static final DateFormat DATE_FORMAT = ApiFormats.DATE_FORMAT;

	private static final String TAG = "SystemModelRepositoryTest";

	private static final Application FIRST = SystemModelTestInstance.FIRST.get();
	private static final Application SECOND = SystemModelTestInstance.SECOND.get();
	private static final Application THIRD = SystemModelTestInstance.THIRD.get();

	private Path firstPath;
	private Path secondPath;
	private Path thirdPath;

	private Path pathMock;
	private File fileMock;

	private IdpaYamlSerializer<Application> serializerMock;

	private IdpaStorage repository;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		pathMock = Mockito.mock(Path.class, "root");
		fileMock = Mockito.mock(File.class);

		serializerMock = Mockito.mock(IdpaYamlSerializer.class);

		firstPath = createPathMock("first");
		secondPath = createPathMock("second");
		thirdPath = createPathMock("third");

		Mockito.when(pathMock.resolve(Mockito.anyString())).thenReturn(pathMock);
		Mockito.when(pathMock.resolve(DATE_FORMAT.format(FIRST.getTimestamp()))).thenReturn(firstPath);
		Mockito.when(pathMock.resolve(DATE_FORMAT.format(SECOND.getTimestamp()))).thenReturn(secondPath);
		Mockito.when(pathMock.resolve(DATE_FORMAT.format(THIRD.getTimestamp()))).thenReturn(thirdPath);

		Mockito.when(serializerMock.readFromYaml(firstPath)).thenReturn(FIRST);
		Mockito.when(serializerMock.readFromYaml(secondPath)).thenReturn(SECOND);
		Mockito.when(serializerMock.readFromYaml(thirdPath)).thenReturn(THIRD);

		Mockito.when(pathMock.toFile()).thenReturn(fileMock);

		Mockito.when(fileMock.list()).thenReturn(new String[] { DATE_FORMAT.format(FIRST.getTimestamp()), DATE_FORMAT.format(SECOND.getTimestamp()), DATE_FORMAT.format(THIRD.getTimestamp()) });

		repository = new IdpaStorage(pathMock, serializerMock, null);
	}

	private Path createPathMock(String name) {
		Path path = Mockito.mock(Path.class, name);
		File file = Mockito.mock(File.class, name);

		Mockito.when(path.toFile()).thenReturn(file);
		Mockito.when(path.resolve(Mockito.anyString())).thenReturn(path);

		return path;
	}

	@Test
	public void test() throws IOException {
		repository.save(TAG, FIRST);

		checkFirst();

		repository.save(TAG, SECOND);

		checkFirst();
		checkSecond();

		repository.save(TAG, THIRD);

		checkFirst();
		checkSecond();
		checkThird();

		checkNext();
	}

	private void checkFirst() throws IOException {
		Idpa read = repository.readLatestBefore(TAG, new Date(100000000));
		assertThat(read.getApplication()).as("Check the system model at a date after the first date").isEqualTo(FIRST);

		read = repository.readLatestBefore(TAG, new Date(0));
		assertThat(read).as("Check the system model at a date before the first date").isNull();

		read = repository.readLatestBefore(TAG, FIRST.getTimestamp());
		assertThat(read.getApplication()).as("Check the system model at the first date").isEqualTo(FIRST);
	}

	private void checkSecond() throws IOException {
		Idpa read = repository.readLatestBefore(TAG, new Date(200000000));
		assertThat(read.getApplication()).as("Check the system model at a date after the second date").isEqualTo(SECOND);

		read = repository.readLatestBefore(TAG, SECOND.getTimestamp());
		assertThat(read.getApplication()).as("Check the system model at the second date").isEqualTo(SECOND);
	}

	private void checkThird() throws IOException {
		Idpa read = repository.readLatestBefore(TAG, new Date(300000000));
		assertThat(read.getApplication()).as("Check the system model at a date after the third date").isEqualTo(THIRD);

		read = repository.readLatestBefore(TAG, THIRD.getTimestamp());
		assertThat(read.getApplication()).as("Check the system model at the third date").isEqualTo(THIRD);
	}

	private void checkNext() throws IOException {
		Idpa read = repository.readOldestAfter(TAG, new Date(0));
		assertThat(read.getApplication()).as("Check the next system model after a date before the first date").isEqualTo(FIRST);

		read = repository.readOldestAfter(TAG, FIRST.getTimestamp());
		assertThat(read.getApplication()).as("Check the next system model after the first date").isEqualTo(SECOND);

		read = repository.readOldestAfter(TAG, new Date(100000000));
		assertThat(read.getApplication()).as("Check the next system model after a date before the second date").isEqualTo(SECOND);

		read = repository.readOldestAfter(TAG, SECOND.getTimestamp());
		assertThat(read.getApplication()).as("Check the next system model after the second date").isEqualTo(THIRD);

		read = repository.readOldestAfter(TAG, new Date(200000000));
		assertThat(read.getApplication()).as("Check the next system model after a date before the third date").isEqualTo(THIRD);

		read = repository.readOldestAfter(TAG, THIRD.getTimestamp());
		assertThat(read).as("Check the next system model after the third date").isNull();

		read = repository.readOldestAfter(TAG, new Date(300000000));
		assertThat(read).as("Check the next system model after a date after the third date").isNull();
	}

}
