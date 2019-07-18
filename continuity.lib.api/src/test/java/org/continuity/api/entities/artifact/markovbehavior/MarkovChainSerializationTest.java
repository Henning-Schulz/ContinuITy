package org.continuity.api.entities.artifact.markovbehavior;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MarkovChainSerializationTest {

	private ObjectMapper mapper;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
	}

	@Test
	public void testWriteRead() throws IOException {
		testWriteRead(MarkovChainTestInstance.SIMPLE);
		testWriteRead(MarkovChainTestInstance.SIMPLE_WO_A);
		testWriteRead(MarkovChainTestInstance.SIMPLE_INSERT);
		testWriteRead(MarkovChainTestInstance.SIMPLE_WITH_INSERT);
		testWriteRead(MarkovChainTestInstance.SOCK_SHOP);
	}

	private void testWriteRead(MarkovChainTestInstance instance) throws IOException {
		String[][] csv = instance.getCsv();

		MarkovChain chain = MarkovChain.fromCsv(csv);
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chain);

		MarkovChain parsed = mapper.readValue(json, MarkovChain.class);

		assertThat(parsed.getTransitions().toString()).isEqualTo(chain.getTransitions().toString());
		assertThat(parsed.getTransitions().getClass()).isEqualTo(chain.getTransitions().getClass()).as("The map type should be TreeMap.");
		assertThat(parsed.getTransitions().values()).extracting(Map::getClass).extracting(Class.class::cast).containsOnly(TreeMap.class).as("The type of the inner maps should be TreeMap.");
	}

}
