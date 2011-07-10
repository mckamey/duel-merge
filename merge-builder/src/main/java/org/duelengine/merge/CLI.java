package org.duelengine.merge;

public class CLI {

	private static final String HELP =
		"Usage:\n" +
		"\tjava -jar merge-builder.jar <webapp-directory> <cdn-map-file>\n"+
		"\tjava -jar merge-builder.jar <webapp-directory> <cdn-map-file> <output-directory>\n"+
		"\tjava -jar merge-builder.jar <webapp-directory> <cdn-map-file> <output-directory> <cdn-url-root>\n\n"+
		"\twebapp-directory: file path to the root of the webapp (required)\n"+
		"\tcdn-map-file: path of the generated map resource file (required)\n"+
		"\toutput-directory: file path to the root of the output (default: <webapp-directory>)\n"+
		"\tcdn-url-root: relative URL path for the cdn output root (default: \"/cdn/\")\n";

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println(HELP);
			return;
		}

		MergeBuilder builder = new MergeBuilder();
		builder.setWebAppDir(args[0]);
		builder.setCDNMapFile(args[2]);

		if (args.length > 2) {
			builder.setOutputDir(args[3]);

			if (args.length > 3) {
				builder.setCDNRoot(args[1]);
			}
		}

		try {
			builder.execute();

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}