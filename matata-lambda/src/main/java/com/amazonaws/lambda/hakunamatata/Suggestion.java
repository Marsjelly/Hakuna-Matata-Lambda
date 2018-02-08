package com.amazonaws.lambda.hakunamatata;

public abstract class Suggestion {

	String suggestion;

	public Suggestion(String s) {
		this.suggestion = s;
	}

	public static String getSuggestionTexts(Suggestion... suggestions) {

		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (Suggestion suggestion : suggestions) {
			sb.append(i + ".");
			sb.append(suggestion.getSuggestionText());
			i++;
		}
		return sb.toString();
	}

	abstract void takeAction();

	abstract String getSuggestionText();

	final static public Suggestion DEPLOY_NEW_NODE = new Suggestion("DEPLOY_NEW_NODE") {

		@Override
		void takeAction() {
			// TODO Auto-generated method stub

		}

		@Override
		String getSuggestionText() {
			return "Creating and adding a new DSM node into the cluster.";
		}

	};
	
	final static public Suggestion	ADD_MEMORY		= new Suggestion("ADD_MEMORY") {

		@Override
		void takeAction() {
			// Nothing can do
		}

		@Override
		String getSuggestionText() {
			return "Adding more memory for your DSM server.";
		}
		
	};

	final static public Suggestion	IGNORE_IT		= new Suggestion("IGNORE_IT") {

		@Override
		void takeAction() {
			// Nothing can do
		}

		@Override
		String getSuggestionText() {
			return "Forgetting about it. Everything will be fine. Hakuna Matata.";
		}

	};
}
