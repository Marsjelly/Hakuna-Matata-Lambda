package com.amazonaws.lambda.hakunamatata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;

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

	@Test
	public void testSetMyName() {
		final String name = "Jack";
		Intent intent = Intent.builder().withName("MyName").withSlot(Slot.builder().withName("Name").withValue(name).build()).build();
		Session session = Session.builder().withSessionId("ssss").build();
		HakunaMatataSpeechlet hakunaMatataSpeechlet = new HakunaMatataSpeechlet();
		hakunaMatataSpeechlet.setSession(session);
		SpeechletResponse response = hakunaMatataSpeechlet.setMyName(intent);
		PlainTextOutputSpeech outSpeech = (PlainTextOutputSpeech)response.getOutputSpeech();
		assertEquals(name, session.getAttribute("Name"));
		assertTrue(outSpeech.getText().startsWith(name));
	}
}
