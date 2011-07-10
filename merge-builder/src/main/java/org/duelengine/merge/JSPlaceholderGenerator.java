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
	public void build(File target, List<String> children) throws IOException {
		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);

		try {
			writer.append("(function() {\n\tvar s, d=document, f=d.getElementsByTagName('script')[0], p=f.parentNode;\n");

			// concatenate references to children
			for (String child : children) {
				// insert child files into outputFile
				writer
					.append("\ts=d.createElement('script');s.type='text/javascript';s.src='")
					.append(child.replace("'", "\\'"))
					.append("';p.insertBefore(s,f);\n");
			}

			writer.append("})();");

		} finally {
			writer.flush();
			writer.close();
		}
	}
}
