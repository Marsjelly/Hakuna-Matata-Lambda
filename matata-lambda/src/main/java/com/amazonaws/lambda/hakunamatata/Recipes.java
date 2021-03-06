package com.amazonaws.lambda.hakunamatata;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Recipes {
	private static final Logger					log		= LogManager.getLogger(Recipes.class);
	private static final Map<String, String>	recipes	= new HashMap<String, String>();

	private Recipes() {
	}

	static {
		recipes.put("dsmStatusSummary",
			"Your DSM is having %d computers in critical state, <break time=\"300ms\"/>%d computers in warning state, <break time=\"300ms\"/>%d computers in managed state <break time=\"300ms\"/>and %d computers in unmanaged state.");
		recipes.put("dsmNoAlert", "No alert has been raised. Very healthy! Great!");
		recipes.put("dsmAlertCount", "Your DSM is having %d alerts. <break time=\"300ms\"/> Buddy, it's okay. Everything will be fine. Hakuna Matata!");
		recipes.put("dsmAlertCountOnDate", "Your DSM raised %d alerts on %t. Let bygones be bygones!");
		recipes.put("WelcomeName", "Welcome to Hakuna Matata! What can I do for you?");
		recipes.put("showAlert", "The alert raised at %tR is <break time=\"300ms\"/><emphasis level=\"strong\">%s</emphasis>.");
		recipes.put("showSuggestions", "My suggestion is: %s Please choose one.");
		recipes.put("showNoSuggestion", "Sorry, I don't have any suggestion to this. What else can I help you?");
		recipes.put("NoAction", "Sorry, I don't know what to do.");
		recipes.put("usage", "<p>Current %s usage is %s</p>");
	}

	public static String get(String item, Object... strings) {
		String r = String.format(recipes.get(item), strings);
		log.info(r);
		return r;
	}
}
