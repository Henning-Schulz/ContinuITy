package org.continuity.idpa.config;

import org.continuity.idpa.storage.ApplicationModelRepositoryManager;
import org.continuity.idpa.storage.IdpaStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class StorageConfig {

	@Bean
	IdpaStorage idpaStorage(@Value("${storage.path:storage}") String storagePath) {
		return new IdpaStorage(storagePath);
	}

	@Bean
	ApplicationModelRepositoryManager systemModelRepositoryManager(IdpaStorage repositoy) {
		return new ApplicationModelRepositoryManager(repositoy);
	}

}
