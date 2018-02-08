package com.amazonaws.lambda.hakunamatata;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Suggestion {
	private static final Logger						log			= LogManager.getLogger(Suggestion.class);

	public final static String						CONCAT		= ":";
	private final static Map<String, Suggestion>	suggestions	= new HashMap<>();

	public static Suggestion getSuggestion(String key) {
		return suggestions.get(key);
	}

	static {
		suggestions.put("DEPLOY_NEW_NODE", new Suggestion("DEPLOY_NEW_NODE") {

			@Override
			void takeAction() {
				try {
					Properties prop = new Properties();
					prop.load(this.getClass().getResourceAsStream("dsm.properties"));
					URL url = new URL(prop.getProperty("slave.url"));
					HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
					con.setRequestMethod("GET");
					con.connect();

					InputStream input = con.getInputStream();
					for (int c = input.read(); c != -1; c = input.read())
						System.out.print((char)c);
					input.close();
				} catch (Exception ex) {
					log.error("Exception: ", ex);
				}
			}

			@Override
			String getSuggestionText() {
				return "Creating and adding a new DSM node into the cluster.";
			}

			@Override
			String getActionResponse() {
				return "A new DSM node is deploying";
			}

		});

		suggestions.put("ADD_MEMORY", new Suggestion("ADD_MEMORY") {

			@Override
			void takeAction() {
				// Nothing can do
			}

			@Override
			String getSuggestionText() {
				return "Adding more memory for your DSM server.";
			}

			@Override
			String getActionResponse() {
				// TODO Auto-generated method stub
				return null;
			}

		});

		suggestions.put("IGNORE_IT", new Suggestion("IGNORE_IT") {

			@Override
			void takeAction() {
				// Nothing can do
			}

			@Override
			String getSuggestionText() {
				return "Forgetting about it.";
			}

			@Override
			String getActionResponse() {
				return "I suggest ";
			}

		});
	}

	String suggestion;

	public Suggestion(String s) {
		this.suggestion = s;
	}

	public String getKey() {
		return suggestion;
	}

	public static String getSuggestionTexts(List<Suggestion> suggestions) {

		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (Suggestion suggestion : suggestions) {
			sb.append("Option " + i + " ");
			sb.append(suggestion.getSuggestionText());
			i++;
		}
		return sb.toString();
	}

	public static String concatSuggestions(List<Suggestion> suggestions) {
		return suggestions.stream().map(s -> s.getKey()).collect(Collectors.joining(CONCAT));
	}

	abstract void takeAction() throws Exception;

	abstract String getSuggestionText();

	abstract String getActionResponse();
}
