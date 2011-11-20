package org.duelengine.merge;

public class CLI {

	private static final String SEPARATOR = "========================================";
	private static final String HELP = "java -jar merge-builder.jar\n"+
			"  --help               : this help text\n"+
			"  -in <source-dir>     : file path to the root of the webapp (required)\n"+
			"  -out <target-dir>    : file path to the root of the build output (default: <source-dir>)\n"+
			"  -map <cdn-map-file>  : path of the generated map resource file\n" +
			"                         (default: \"<target-dir>/cdn.properties\")\n"+
			"  -cdn <cdn-root-path> : relative URL path for the cdn output root (default: \"/cdn/\")\n"+
			"  -ext <file-ext-list> : quoted list of file extensions to add to CDN (default: none)\n" +
			"                         (example: \".png .jpg .gif .ico .woff .ttf .eot .svg\")\n";

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println(HELP);
			return;
		}

		Settings settings = new Settings();
		System.out.println(SEPARATOR);
		System.out.println("Merge Builder\n");
		for (int i=0; i<args.length; i++) {
			String arg = args[i];

			if ("-in".equals(arg)) {
				settings.setSourceDir(args[++i]);

			} else if ("-out".equals(arg)) {
				settings.setTargetDir(args[++i]);

			} else if ("-map".equals(arg)) {
				settings.setCDNMapFile(args[++i]);

			} else if ("-cdn".equals(arg)) {
				settings.setCDNRoot(args[++i]);

			} else if ("-ext".equals(arg)) {
				settings.setExtensionList(args[++i]);

			} else if ("--help".equalsIgnoreCase(arg)) {
				System.out.println(HELP);
				System.out.println(SEPARATOR);
				return;

			} else {
				System.out.println(HELP);
				System.out.println(SEPARATOR);
				return;
			}
		}

		try {
			new BuildManager(settings).execute();

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}