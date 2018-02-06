package com.amazonaws.lambda.hakunamatata;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

public final class HakunaMatataRequestStreamHandler extends SpeechletRequestStreamHandler {

	private static final Logger			log;
	private static final Set<String>	supportedApplicationIds;

	static {
		System.setProperty("log4j.configurationFile", "resources/log4j2.xml");
		log = LogManager.getLogger(HakunaMatataRequestStreamHandler.class);

		/*
		 * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant Alexa Skill and put the relevant
		 * Application Ids in this Set.
		 */
		supportedApplicationIds = new HashSet<String>();
		//		supportedApplicationIds.add("amzn1.ask.skill.60af5aa2-2218-4ee6-ab47-145b1c9d02ce");
	}

	public HakunaMatataRequestStreamHandler() {

		super(new HakunaMatataSpeechlet(), supportedApplicationIds);
	}

	//    @Override
	//    public String handleRequest(Object input, Context context) {
	//        context.getLogger().log("Input: " + input);
	//
	//        // TODO: implement your handler
	//        return "Hello from Lambda!";
	//    }

}
