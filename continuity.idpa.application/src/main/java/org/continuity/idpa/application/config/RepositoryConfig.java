package org.continuity.idpa.application.config;

import org.continuity.idpa.application.repository.SystemModelRepository;
import org.continuity.idpa.application.repository.SystemModelRepositoryManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RepositoryConfig {

	@Bean
	SystemModelRepository systemModelRepository(@Value("${storage.path:storage}") String storagePath) {
		return new SystemModelRepository(storagePath);
	}

	@Bean
	SystemModelRepositoryManager systemModelRepositoryManager(SystemModelRepository repositoy) {
		return new SystemModelRepositoryManager(repositoy);
	}

}
