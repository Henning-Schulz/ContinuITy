package org.continuity.system.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.EnumSet;

import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Application;
import org.continuity.system.model.SystemModelTestInstance;
import org.idpa.application.model.entities.ModelElementReference;
import org.idpa.application.model.entities.SystemChange;
import org.idpa.application.model.entities.SystemChangeReport;
import org.idpa.application.model.entities.SystemChangeType;
import org.idpa.application.model.repository.SystemModelRepository;
import org.idpa.application.model.repository.SystemModelRepositoryManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Henning Schulz
 *
 */
public class SystemModelRepositoryManagerTest {

	private SystemModelRepository repositoryMock;

	private SystemModelRepositoryManager manager;

	@Before
	public void setup() {
		repositoryMock = Mockito.mock(SystemModelRepository.class);
		manager = new SystemModelRepositoryManager(repositoryMock);
	}

	@Test
	public void testSaveSameModel() {
		testWithSameModel(SystemModelTestInstance.FIRST.get());
		testWithSameModel(SystemModelTestInstance.SECOND.get());
		testWithSameModel(SystemModelTestInstance.THIRD.get());
	}

	private void testWithSameModel(Application systemModel) {
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(systemModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", systemModel);
		assertThat(report.getSystemChanges()).as("Expect the changes of the report to be empty.").isEmpty();
		assertThat(report.getIgnoredSystemChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();
	}

	@Test
	public void testSaveModelWithAddedInterface() throws IOException {
		Application firstModel = SystemModelTestInstance.FIRST.get();
		Application secondModel = SystemModelTestInstance.SECOND.get();
		Application thirdModel = SystemModelTestInstance.THIRD.get();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", secondModel);
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added").containsExactly("logout");
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
		.as("Expected that (except for the addition of logout) the parameters user and logoutuser are added as only changes.")
		.extracting(SystemChange::getChangedElement).extracting(ModelElementReference::getId).containsExactlyInAnyOrder("logoutuser", "user");
		assertThat(report.getIgnoredSystemChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue()).as("Expected the second model to be stored").isEqualTo(secondModel);

		// Ignoring INTERFACE_ADDED
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", secondModel,
				EnumSet.of(SystemChangeType.INTERFACE_ADDED, SystemChangeType.PARAMETER_ADDED, SystemChangeType.PARAMETER_REMOVED));
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
				.as("Expected that (except for the addition of logout) the parameters user and logoutuser are added as only ignored changes.").extracting(SystemChange::getChangedElement)
				.extracting(ModelElementReference::getId).containsExactlyInAnyOrder("logoutuser", "user");
		assertThat(report.getSystemChanges()).as("Expect the changes of the report to be empty.").isEmpty();

		Mockito.verify(repositoryMock, Mockito.times(0)).save(Mockito.anyString(), Mockito.any());

		// Removing an interface at the same time
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_ADDED));
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface logout has been added to the ignored changes").containsExactly("logout");
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED))
		.as("Expected that there are no other ignored changes than the addition of logout").isEmpty();
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected the login interface to be removed").containsExactly("login");
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getChangedElement)
		.as("Expected that there are no other changes than the removal of login").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getEndpoints()).as("Expected the stored model to be empty").isEmpty();
	}

	@Test
	public void testSaveModelWithRemovedInterface() throws IOException {
		Application firstModel = SystemModelTestInstance.FIRST.get();
		Application secondModel = SystemModelTestInstance.SECOND.get();
		Application thirdModel = SystemModelTestInstance.THIRD.get();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(secondModel);

		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel);
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed").containsExactly("login");
		assertThat(report.getIgnoredSystemChanges()).as("Expect the ignored changes of the report to be empty.").isEmpty();

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue()).as("Expected the third model to be stored").isEqualTo(thirdModel);

		// Cannot check for absence of other changes, since the user parameter was added

		// Ignoring INTERFACE_REMOVED
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(secondModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_REMOVED));
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED))
		.as("Expected that there are no other ignored changes than the removal of login").isEmpty();

		// Adding an interface at the same time
		firstModel = SystemModelTestInstance.FIRST.get();
		secondModel = SystemModelTestInstance.SECOND.get();
		thirdModel = SystemModelTestInstance.THIRD.get();
		Mockito.reset(repositoryMock);
		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);

		report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", thirdModel, EnumSet.of(SystemChangeType.INTERFACE_REMOVED));
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_REMOVED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected that the interface login has been removed as an ignored change").containsExactly("login");
		assertThat(report.getIgnoredSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_REMOVED))
		.as("Expected that there are no other ignored changes than the removal of login").isEmpty();
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() == SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).as("Expected the logout interface to be added").containsExactly("logout");
		assertThat(report.getSystemChanges().stream().filter(change -> change.getType() != SystemChangeType.INTERFACE_ADDED)).extracting(SystemChange::getChangedElement)
		.as("Expected that there are no other changes than the addition of logout").isEmpty();

		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());
		assertThat(modelCaptor.getValue().getEndpoints()).extracting(Endpoint::getId).as("Expected the stored model to contain exactly the interfaces login and logout")
		.containsExactlyInAnyOrder("login", "logout");
	}

	@Test
	public void testWithIgnoredParameterRemovals() throws IOException {
		Application firstModel = SystemModelTestInstance.FIRST.get();
		Application secondModel = SystemModelTestInstance.SECOND.get();

		Mockito.when(repositoryMock.readLatestBefore(Mockito.anyString(), Mockito.any())).thenReturn(firstModel);
		SystemChangeReport report = manager.saveOrUpdate("SystemModelRepositoryManagerTest", secondModel, EnumSet.of(SystemChangeType.PARAMETER_REMOVED));

		assertThat(report.getSystemChanges()).filteredOn(change -> change.getType() == SystemChangeType.PARAMETER_ADDED).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).containsExactly("logoutuser");

		assertThat(report.getIgnoredSystemChanges()).filteredOn(change -> change.getType() == SystemChangeType.PARAMETER_REMOVED).extracting(SystemChange::getChangedElement)
		.extracting(ModelElementReference::getId).containsExactly("user");

		ArgumentCaptor<Application> modelCaptor = ArgumentCaptor.forClass(Application.class);
		Mockito.verify(repositoryMock).save(Mockito.eq("SystemModelRepositoryManagerTest"), modelCaptor.capture());

		assertThat(modelCaptor.getValue().getEndpoints()).filteredOn(interf -> "login".equals(interf.getId())).extracting(interf -> (HttpEndpoint) interf)
		.flatExtracting(Endpoint::getParameters).extracting(Parameter::getId).containsExactlyInAnyOrder("user", "logoutuser");
	}

}
