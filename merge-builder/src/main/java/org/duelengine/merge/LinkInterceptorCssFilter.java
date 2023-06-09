package org.duelengine.merge;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.duelengine.css.ast.ContainerNode;
import org.duelengine.css.ast.CssNode;
import org.duelengine.css.ast.CssNodeType;
import org.duelengine.css.ast.FunctionNode;
import org.duelengine.css.ast.StringNode;
import org.duelengine.css.ast.ValueNode;
import org.duelengine.css.codegen.CssFilter;
import org.duelengine.css.codegen.CssFormatter;
import org.duelengine.css.parsing.CssLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkInterceptorCssFilter implements CssFilter {

	private static final Logger log = LoggerFactory.getLogger(LinkInterceptorCssFilter.class);
	private static final CssFormatter cssFormatter = new CssFormatter();
	private final BuildManager manager;
	private final String path;

	public LinkInterceptorCssFilter(BuildManager manager, String path) {
		this.manager = manager;
		this.path = path;
	}
	
	@Override
	public CssNode filter(CssNode node) {
		if (node.getNodeType() != CssNodeType.FUNCTION) {
			return node;
		}

		FunctionNode func = (FunctionNode)node;
		if (!"url".equals(func.getValue())) {
			// only supports explicit URLs
			return node;
		}

		ContainerNode children = func.getContainer();

		if (children.childCount() > 1) {
			// HACK: we need a consolidated value rather than expression
			StringBuilder buffer = new StringBuilder();
			try {
				cssFormatter.writeNode(buffer, children, null);
			} catch (IOException e) {
				return node;
			}
			String value = buffer.toString();
			if (value == null || value.isEmpty()) {
				return node;
			}
			children.getChildren().clear();
			children.getChildren().add(new ValueNode(value));
		}

		// zero or one child at this point
		for (CssNode child : children.getChildren()) {
			if (child instanceof ValueNode) {
				ValueNode valNode = ((ValueNode)child);
				String val = valNode.getValue();
				val = CssLexer.decodeString(val);
				if (val == null || val.isEmpty() || val.startsWith("data:")) {
					break;
				}

				String suffix = "";
				if (val.charAt(0) != '/') {
					URI context = getContextPath(path);
					if (context != null) {
						// resolve relative URLs and isolate the path part
						URI uri = context.resolve(val);
						if (uri.getHost() == null && uri.getScheme() == null) {
							val = uri.getPath();
							if (uri.getQuery() != null) {
								suffix += '?'+uri.getQuery();
							}
							if (uri.getFragment() != null) {
								suffix += '#'+uri.getFragment();
							}
						}
					}

				} else {
					// query and hash
					int query = val.indexOf('?');
					if (query >= 0) {
						suffix += val.substring(query);
						val = val.substring(0, query);
					}
					int hash = val.indexOf('#');
					if (hash >= 0) {
						suffix += val.substring(hash);
						val = val.substring(0, hash);
					}
				}

				manager.addChildLink(this.path, val);

				manager.ensureProcessed(val);

				String valHash = manager.getProcessedPath(val);
				if (valHash == null) {
					log.warn("Missing CSS reference: "+val);
					break;
				}

				// trim path to just filename
				// this will make URL relative from stylesheet
				valHash = valHash.substring(valHash.lastIndexOf('/')+1);

				val += suffix;
				valHash += suffix;

				if (child instanceof StringNode) {
					valHash = CssLexer.encodeString(valHash);
				}
				valNode.setValue(valHash);
				log.info("CSS url: "+val+" => "+valHash);

			} else {
				log.warn("Unexpected CSS url type: "+child.getNodeType());
			}
		}
		
		return node;
	}

	private URI getContextPath(String path) {
		try {
			return new URI(path);

		} catch (URISyntaxException ex) {
			return null;
		}
	}
}
