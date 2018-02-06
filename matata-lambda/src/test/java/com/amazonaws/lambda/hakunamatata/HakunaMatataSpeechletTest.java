package com.amazonaws.lambda.hakunamatata;

import org.junit.Test;

public class HakunaMatataSpeechletTest {

	@Test
	public void testGetDSMStatus() throws Exception {
		HakunaMatataSpeechlet hm = new HakunaMatataSpeechlet();
		hm.getDSMStatus(null);
	}

	@Test
	public void testGetRecipe() {
		System.out.println(Recipes.get("dsmStatusSummary", 3, 4, 5, 6));
	}
}
