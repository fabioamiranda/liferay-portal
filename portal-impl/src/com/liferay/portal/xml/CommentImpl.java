/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.xml;

import com.liferay.portal.kernel.xml.Comment;
import com.liferay.portal.kernel.xml.Visitor;

/**
 * @author Brian Wing Shun Chan
 */
public class CommentImpl extends NodeImpl implements Comment {

	public CommentImpl(org.dom4j.Comment comment) {
		super(comment);

		_comment = comment;
	}

	public <T, V extends Visitor<T>> T accept(V visitor) {
		return visitor.visitComment(this);
	}

	public boolean equals(Object obj) {
		org.dom4j.Comment comment = ((CommentImpl)obj).getWrappedComment();

		return _comment.equals(comment);
	}

	public org.dom4j.Comment getWrappedComment() {
		return _comment;
	}

	public int hashCode() {
		return _comment.hashCode();
	}

	public String toString() {
		return _comment.toString();
	}

	private org.dom4j.Comment _comment;

}