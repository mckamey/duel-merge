package org.duelengine.merge;

import java.io.IOException;
import java.net.URI;

import org.cssless.css.ast.*;
import org.cssless.css.codegen.CssFilter;
import org.cssless.css.parsing.CssLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkInterceptorCssFilter implements CssFilter {

	private final Logger log = LoggerFactory.getLogger(LinkInterceptorCssFilter.class);
	private final BuildManager manager;
	private final URI context;

	public LinkInterceptorCssFilter(BuildManager manager, URI context) {
		this.manager = manager;
		this.context = context;
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
				new org.cssless.css.codegen.CssFormatter().writeNode(buffer, children, null);
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

		// zero or one child here
		for (CssNode child : children.getChildren()) {
			if (child instanceof ValueNode) {
				ValueNode valNode = ((ValueNode)child);
				String val = valNode.getValue();
				val = CssLexer.decodeString(val);
				if (val == null || val.isEmpty() || val.startsWith("data:")) {
					break;
				}

				String suffix = "";
				if (val.charAt(0) != '/' && context != null) {
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
}
