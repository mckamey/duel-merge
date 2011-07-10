package org.duelengine.merge;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.cssless.css.ast.*;
import org.cssless.css.codegen.CssFilter;
import org.cssless.css.parsing.CssLexer;

public class LinkInterceptorCssFilter implements CssFilter {

	private final Logger log = Logger.getLogger(LinkInterceptorCssFilter.class.getCanonicalName());
	private final Map<String, String> linkMap;

	public LinkInterceptorCssFilter(Map<String, String> linkMap) {
		this.linkMap = linkMap;
	}
	
	@Override
	public CssNode filter(CssNode node) {
		if (node.getNodeType() != CssNodeType.FUNCTION) {
			return node;
		}

		FunctionNode func = (FunctionNode)node;
		if (!"url".equals(func.getValue())) {
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

		// should be only one child
		for (CssNode child : children.getChildren()) {
			if (child instanceof ValueNode) {
				ValueNode valNode = ((ValueNode)child);
				String val = valNode.getValue();
				val = CssLexer.decodeString(val);
				if (val == null || val.isEmpty()) {
					break;
				}

				String valHash = this.linkMap.get(val);
				if (valHash == null) {
					log.warning("Missing CSS reference: "+val);
					break;
				}

				// trim path to just filename
				// this will make URL relative from stylesheet
				valHash = valHash.substring(valHash.lastIndexOf('/')+1);

				if (child instanceof StringNode) {
					valHash = CssLexer.encodeString(valHash);
				}
				valNode.setValue(valHash);
				log.info("CSS url: "+val+" => "+valHash);

			} else {
				log.warning("Unexpected CSS url type: "+child.getNodeType());
			}
		}
		
		return node;
	}
}
