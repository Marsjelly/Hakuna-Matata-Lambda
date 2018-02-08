package com.amazonaws.lambda.hakunamatata;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.trendmicro.ds.platform.rest.object.alerts.Alert;
import com.trendmicro.ds.platform.rest.object.alerts.ListAlertsResponse;
import com.trendmicro.ds.platform.rest.object.util.HostStatusSummaryElement;
import com.trendmicro.ds.platform.rest.object.util.StatusSummaryElement;
import com.trendmicro.ds.restapi.DeepSecurityClient;

public class HakunaMatataSpeechlet implements SpeechletV2 {
	private static final Logger	log			= LogManager.getLogger(HakunaMatataSpeechlet.class);
	private Session				session;

	protected enum QuestionType {
		DSMStatus,
		DSMAlert,
		ShowFirstAlert,
		Suggestion
	}

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), requestEnvelope.getSession().getSessionId());

		setSession(requestEnvelope.getSession());

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		try {
			if ("DSMStatus".equals(intentName)) {
				log.info("onIntent DSMStatus");
				return getDSMStatus(intent);
			} else if ("DSMAlert".equals(intentName)) {
				log.info("onIntent DSMAlert");
				return getDSMAlert(intent);
			} else if ("MyName".equals(intentName)) {
				log.info("onIntent MyName");
				return setMyName(intent);
			} else if ("ShowFirstAlert".equals(intentName)) {
				log.info("onIntent ShowAlert");
				return showFirstAlert(intent);
			} else if ("Suggestion".equals(intentName)) {
				log.info("onIntent Suggestion");
				return getSuggestion(intent);
			} else if ("Option".equals(intentName)) {
				log.info("onIntent Options");
				return processOptions(intent);
			} else if ("AMAZON.HelpIntent".equals(intentName)) {
				return getHelp();
			} else if ("AMAZON.StopIntent".equals(intentName)) {
				PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
				outputSpeech.setText("Goodbye");

				return SpeechletResponse.newTellResponse(outputSpeech);
			} else if ("AMAZON.CancelIntent".equals(intentName)) {
				PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
				outputSpeech.setText("Goodbye");

				return SpeechletResponse.newTellResponse(outputSpeech);
			} else {
				String errorSpeech = "This is unsupported.  Please try something else.";
				return newAskResponse(errorSpeech, errorSpeech);
			}
		} catch (Exception ex) {
			log.error("Exception!", ex);
			String errorSpeech = "Something wrong!  Please try again!";
			return newAskResponse(errorSpeech, errorSpeech);
		}
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());

		String speechOutput = "Welcome to Hakuna Matata. Your Deep Security Manager intelligent robot. May I know who this is?";
		// If the user either does not reply to the welcome message or says
		// something that is not understood, they will be prompted again with this text.
		String repromptText = "Hello, you there? Just yell \"Help Me\" if you don't know what to do.";

		// Here we are prompting the user for input
		return newAskResponse(speechOutput, repromptText);
	}

	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());

		// any cleanup logic goes here
	}

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());
	}

	private SpeechletResponse getResponseNoEndSession(String text, String title) {
		return getResponse(text, title, false);
	}

	/**
	 * Creates a {@code SpeechletResponse} for the RecipeIntent.
	 *
	 * @param intent intent for the request
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getResponse(String text, String title, boolean shouldEndSession) {

		String sessionName = this.session.getAttribute("Name") != null ? (String)this.session.getAttribute("Name") : "";
		String speechText = sessionName.isEmpty() ? text : sessionName + ", " + text;

		log.info("getResponse: {}", speechText);

		if (speechText != null) {
			// If we have the recipe, return it to the user.
			PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
			outputSpeech.setText(speechText);

			SimpleCard card = new SimpleCard();
			card.setTitle(title);
			card.setContent(speechText);

			SpeechletResponse res = SpeechletResponse.newTellResponse(outputSpeech, card);
			res.setNullableShouldEndSession(shouldEndSession);
			return res;
		} else {
			// We don't have a recipe, so keep the session open and ask the user for another
			// item.
			String speechOutput = "I'm sorry, but I don't understand what you wnat me to do. What else can I help with?";
			String repromptSpeech = "What else can I help with?";
			return newAskResponse(speechOutput, repromptSpeech);
		}
	}

	/**
	 * Creates a {@code SpeechletResponse} for the HelpIntent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getHelp() {
		String speechOutput = "You can ask questions about your DSM or you can say exit... " + "Now, what can I help you with?";
		String repromptText = "Don't worry. Your DSM should be fine. Have a rest! Now, what can I help you with?";
		return newAskResponse(speechOutput, repromptText);
	}

	/**
	 * Wrapper for creating the Ask response. The OutputSpeech and {@link Reprompt} objects are created from the input strings.
	 *
	 * @param stringOutput the output to be spoken
	 * @param repromptText the reprompt for if the user doesn't reply or is misunderstood.
	 * @return SpeechletResponse the speechlet response
	 */
	private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		outputSpeech.setText(stringOutput);

		PlainTextOutputSpeech repromptOutputSpeech = new PlainTextOutputSpeech();
		repromptOutputSpeech.setText(repromptText);
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptOutputSpeech);

		return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
	}

	protected SpeechletResponse getDSMStatus(Intent intent) throws Exception {
		StatusSummaryElement statusSummary;
		try (DeepSecurityClient dsClient = getDeepSecurityClient()) {
			statusSummary = dsClient.getStatusSummary();
		}

		HostStatusSummaryElement s = statusSummary.getHostStatusSummary();
		String recipe = Recipes.get("dsmStatusSummary", s.getCriticalHosts(), s.getWarningHosts(), s.getOnlineHosts(), s.getUnmanageHosts());
		return getResponseNoEndSession(recipe, "DSM Status");
	}

	protected SpeechletResponse getDSMAlert(Intent intent) throws Exception {
		Slot dateSlot = intent.getSlot("Date");
		ListAlertsResponse alertsResponse;
		try (DeepSecurityClient dsClient = getDeepSecurityClient()) {
			alertsResponse = dsClient.listAlerts();
		}
		List<Alert> alerts = alertsResponse.getAlerts();
		Date date = null;
		if (dateSlot != null && dateSlot.getValue() != null && !dateSlot.getValue().isEmpty()) {
			log.info("Date: " + dateSlot.getValue());
			SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd");
			date = df.parse(dateSlot.getValue());
			long dateTime = date.getTime();
			long datePlusOneDayTime = DateUtils.addDays(date, 1).getTime();
			alerts = alerts.stream().filter(a -> a.getTimeRaised() < datePlusOneDayTime && a.getTimeRaised() >= dateTime).collect(Collectors.toList());
		}

		String recipe = alerts.size() == 0 ? Recipes.get("dsmNoAlert") : (date == null ? Recipes.get("dsmAlertCount", alerts.size()) : Recipes.get("dsmAlertCountOnDate", alerts.size(), date));
		return getResponseNoEndSession(recipe, "DSM Alert Count");
	}

	protected SpeechletResponse setMyName(Intent intent) {
		Slot nameSlot = intent.getSlot("Name");
		String name = (nameSlot != null) ? nameSlot.getValue() : "";
		if (name != null && !name.isEmpty()) {
			getSession().setAttribute("Name", name);
			String recipe = Recipes.get("WelcomeName");
			return getResponseNoEndSession(recipe, "Welcome to Hakuna Matata!");
		} else {
			String recipe = Recipes.get("WelcomeName");
			return getResponse(recipe, "Welcome to Hakuna Matata!", true);
		}
	}

	protected SpeechletResponse showFirstAlert(Intent intent) throws Exception {
		ListAlertsResponse alertsResponse;
		try (DeepSecurityClient dsClient = getDeepSecurityClient()) {
			alertsResponse = dsClient.listAlerts();
		}
		List<Alert> alerts = alertsResponse.getAlerts();
		if (alerts.size() == 0) {
			String recipe = Recipes.get("dsmNoAlert");
			return getResponseNoEndSession(recipe, "Alert Status");
		}

		setLastQuestion(QuestionType.ShowFirstAlert);

		Alert alert = alerts.get(0);
		getSession().setAttribute("LastAlert", alert.getName());
		String recipe = Recipes.get("showAlert", new Date(alert.getTimeRaised()), alert.getName());
		return getResponseNoEndSession(recipe, "Alert!");
	}

	protected SpeechletResponse getSuggestion(Intent intent) {
		QuestionType lastQuestion = getLastQuestion();
		switch (lastQuestion) {
		case ShowFirstAlert:
			Object obj = getSession().getAttribute("LastAlert");
			if (obj != null) {
				String alert = (String)obj;
				//TODO				if (alert.getName().contains("Memory")) {
				if (true) {
					ArrayList<Suggestion> suggestions = new ArrayList<>();
					suggestions.add(Suggestion.getSuggestion("ADD_MEMORY"));
					suggestions.add(Suggestion.getSuggestion("DEPLOY_NEW_NODE"));
					suggestions.add(Suggestion.getSuggestion("IGNORE_IT"));

					setLastQuestion(QuestionType.Suggestion);
					getSession().setAttribute("Suggestions", Suggestion.concatSuggestions(suggestions));

					String recipe = Recipes.get("showSuggestions", Suggestion.getSuggestionTexts(suggestions));
					return getResponseNoEndSession(recipe, "Suggestions");

				} else {
					// TODO return sorry I don't know
				}
			} else {
				//TODO return what
			}
			break;
		default:
			//TODO return what?
		}
		String recipe = Recipes.get("showNoSuggestion");
		return getResponseNoEndSession(recipe, "Sorry");
	}

	protected SpeechletResponse processOptions(Intent intent) throws Exception {
		Slot optionNumSlot = intent.getSlot("OptionNumber");
		if (optionNumSlot != null && !optionNumSlot.getValue().isEmpty()) {
			int op = Integer.valueOf(optionNumSlot.getValue()).intValue();
			String suggestionKeys = (String)getSession().getAttribute("Suggestions");
			String[] suggestionKeyArray = suggestionKeys.split(Suggestion.CONCAT);
			String suggetionKey = suggestionKeyArray[op - 1];
			Suggestion suggestion = Suggestion.getSuggestion(suggetionKey);
			suggestion.takeAction();
			String recipe = suggestion.getActionResponse();
			return getResponseNoEndSession(recipe, "Action");
		}
		return getResponseNoEndSession(Recipes.get("NoAction"), "Action");
	}

	protected void setLastQuestion(QuestionType type) {
		getSession().setAttribute("LastQuestion", type.toString());
	}
	
	protected QuestionType getLastQuestion() {
		Object lq = getSession().getAttribute("LastQuestion");
		return lq == null ? null : QuestionType.valueOf((String)lq);
	}

	private DeepSecurityClient getDeepSecurityClient() throws IOException {
		Properties prop = new Properties();
		prop.load(this.getClass().getResourceAsStream("dsm.properties"));
		DeepSecurityClient dsClient = new DeepSecurityClient(prop.getProperty("dsm.url"), prop.getProperty("dsm.user"), prop.getProperty("dsm.password"), null);
		dsClient.disableTrustManager();
		return dsClient;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
