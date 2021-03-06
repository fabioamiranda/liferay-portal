/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.site.navigation.site.map.web.portlet.template;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portletdisplaytemplate.BasePortletDisplayTemplateHandler;
import com.liferay.portal.kernel.template.TemplateHandler;
import com.liferay.portal.kernel.template.TemplateVariableGroup;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.portletdisplaytemplate.util.PortletDisplayTemplateConstants;
import com.liferay.site.navigation.site.map.web.configuration.SiteMapWebConfigurationValues;
import com.liferay.site.navigation.site.map.web.constants.SiteMapPortletKeys;
import com.liferay.site.navigation.site.map.web.display.context.SiteMapDisplayContext;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.osgi.service.component.annotations.Component;

/**
 * @author Juan Fernández
 */
@Component(
	immediate = true,
	property = {"javax.portlet.name=" + SiteMapPortletKeys.SITE_MAP},
	service = TemplateHandler.class
)
public class SiteMapPortletDisplayTemplateHandler
	extends BasePortletDisplayTemplateHandler {

	@Override
	public String getClassName() {
		return LayoutSet.class.getName();
	}

	@Override
	public String getName(Locale locale) {
		ResourceBundle resourceBundle = ResourceBundle.getBundle(
			"content.Language", locale);

		String portletTitle = PortalUtil.getPortletTitle(
			SiteMapPortletKeys.SITE_MAP, resourceBundle);

		return portletTitle.concat(StringPool.SPACE).concat(
			LanguageUtil.get(locale, "template"));
	}

	@Override
	public String getResourceName() {
		return SiteMapPortletKeys.SITE_MAP;
	}

	@Override
	public Map<String, TemplateVariableGroup> getTemplateVariableGroups(
			long classPK, String language, Locale locale)
		throws Exception {

		Map<String, TemplateVariableGroup> templateVariableGroups =
			super.getTemplateVariableGroups(classPK, language, locale);

		TemplateVariableGroup templateVariableGroup =
			templateVariableGroups.get("fields");

		templateVariableGroup.empty();

		templateVariableGroup.addCollectionVariable(
			"pages", List.class, PortletDisplayTemplateConstants.ENTRIES,
			"page", Layout.class, "curPage", "getName(locale)");
		templateVariableGroup.addVariable(
			"site-map-display-context", SiteMapDisplayContext.class,
			"siteMapDisplayContext");

		return templateVariableGroups;
	}

	@Override
	protected String getTemplatesConfigPath() {
		return SiteMapWebConfigurationValues.DISPLAY_TEMPLATES_CONFIG;
	}

}