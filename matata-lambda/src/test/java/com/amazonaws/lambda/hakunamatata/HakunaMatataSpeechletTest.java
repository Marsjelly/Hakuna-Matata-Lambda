package com.amazonaws.lambda.hakunamatata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.lambda.hakunamatata.HakunaMatataSpeechlet.QuestionType;
import com.trendmicro.ds.platform.rest.object.alerts.Alert;

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

	@Test
	public void testShowFirstAlert() throws Exception {
		Intent intent = Intent.builder().withName("ShowFirstAlert").build();
		Session session = Session.builder().withSessionId("ssss").build();
		HakunaMatataSpeechlet hakunaMatataSpeechlet = new HakunaMatataSpeechlet();
		hakunaMatataSpeechlet.setSession(session);

		SpeechletResponse response = hakunaMatataSpeechlet.showFirstAlert(intent);
		assertEquals(QuestionType.ShowFirstAlert, hakunaMatataSpeechlet.getLastQuestion());
		System.out.println(((SimpleCard)response.getCard()).getContent());
	}

	@Test
	public void testGetSuggestion() {
		Intent intent = Intent.builder().withName("Suggestion").build();
		Session session = Session.builder().withSessionId("ssss").build();
		session.setAttribute("LastAlert", new Alert().withName("Memory"));

		HakunaMatataSpeechlet hakunaMatataSpeechlet = new HakunaMatataSpeechlet();
		hakunaMatataSpeechlet.setSession(session);
		hakunaMatataSpeechlet.setLastQuestion(QuestionType.ShowFirstAlert);

		hakunaMatataSpeechlet.getSuggestion(intent);
		assertEquals(QuestionType.Suggestion, hakunaMatataSpeechlet.getLastQuestion());

		List<Suggestion> suggestions = (List<Suggestion>)session.getAttribute("Suggestions");
		assertEquals(3, suggestions.size());
		assertEquals(Suggestion.DEPLOY_NEW_NODE, suggestions.get(1));
	}
}
