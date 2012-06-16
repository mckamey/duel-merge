package org.duelengine.merge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JSPlaceholderGenerator implements PlaceholderGenerator {

	@Override
	public String getTargetExtension() {
		return ".js";
	}

	@Override
	public void build(BuildManager manager, File target, List<String> children) throws IOException {
		String nocache = '?'+target.getName();
		int dot = target.getName().lastIndexOf('.');
		if (dot > 0) {
			nocache = nocache.substring(0, dot);
		}

		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);

		try {
			writer
				.append("(function() {\n")
				.append("\t// simulate semantics of merged scripts but allow debugging the original files; append anti-caching suffix\n")
				.append("\ttry {\n");

			// concatenate references to children
			for (String child : children) {
				child = manager.getPlaceholderPath(child);

				// insert child files into outputFile
				writer
					.append("\t\tdocument.write('\\u003cscript type=\"text/javascript\" src=\"")
					.append(child.replace("'", "\\'")+nocache)
					.append("\">\\u003c/script>');\n");
			}

			writer
				.append("\t} catch(ex) {\n")
				.append("\t\tvar s, d=document, f=d.getElementsByTagName('script')[0], p=f.parentNode;\n");

			// concatenate references to children
			for (String child : children) {
				child = manager.getPlaceholderPath(child);

				// insert child files into outputFile
				writer
					.append("\t\ts=d.createElement('script');s.type='text/javascript';s.src='")
					.append(child.replace("'", "\\'"))
					.append(nocache)
					.append("';p.insertBefore(s,f);\n");
			}

			writer
				.append("\t}\n")
				.append("})();");

		} finally {
			writer.flush();
			writer.close();
		}
	}
}
