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

package com.liferay.portlet.portletconfiguration.action;

import com.liferay.portal.LARFileException;
import com.liferay.portal.LARFileSizeException;
import com.liferay.portal.LARTypeException;
import com.liferay.portal.LocaleException;
import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.PortletIdException;
import com.liferay.portal.kernel.lar.ExportImportHelper;
import com.liferay.portal.kernel.lar.MissingReferences;
import com.liferay.portal.kernel.lar.exportimportconfiguration.ExportImportConfigurationConstants;
import com.liferay.portal.kernel.lar.exportimportconfiguration.ExportImportConfigurationSettingsMapFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.staging.StagingUtil;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.ExportImportConfiguration;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.ExportImportConfigurationLocalServiceUtil;
import com.liferay.portal.service.ExportImportServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.dynamicdatamapping.StructureDuplicateStructureKeyException;
import com.liferay.portlet.layoutsadmin.action.ImportLayoutsAction;

import java.io.InputStream;
import java.io.Serializable;

import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @author Jorge Ferrer
 * @author Raymond Augé
 */
public class ExportImportAction extends ImportLayoutsAction {

	@Override
	public void processAction(
			ActionMapping actionMapping, ActionForm actionForm,
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse)
		throws Exception {

		Portlet portlet = null;

		try {
			portlet = ActionUtil.getPortlet(actionRequest);
		}
		catch (PrincipalException pe) {
			SessionErrors.add(actionRequest, pe.getClass());

			setForward(actionRequest, "portlet.portlet_configuration.error");

			return;
		}

		actionRequest = ActionUtil.getWrappedActionRequest(actionRequest, null);

		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);

		try {
			if (Validator.isNotNull(cmd)) {
				String redirect = ParamUtil.getString(
					actionRequest, "redirect");

				if (cmd.equals(Constants.ADD_TEMP)) {
					addTempFileEntry(
						actionRequest,
						ExportImportHelper.TEMP_FOLDER_NAME +
							portlet.getPortletId());

					validateFile(
						actionRequest, actionResponse,
						ExportImportHelper.TEMP_FOLDER_NAME +
							portlet.getPortletId());
				}
				else if (cmd.equals("copy_from_live")) {
					StagingUtil.copyFromLive(actionRequest, portlet);

					sendRedirect(actionRequest, actionResponse);
				}
				else if (cmd.equals(Constants.DELETE_TEMP)) {
					deleteTempFileEntry(
						actionRequest, actionResponse,
						ExportImportHelper.TEMP_FOLDER_NAME +
							portlet.getPortletId());
				}
				else if (cmd.equals(Constants.EXPORT)) {
					hideDefaultSuccessMessage(actionRequest);

					exportData(actionRequest, actionResponse, portlet);

					sendRedirect(actionRequest, actionResponse, redirect);
				}
				else if (cmd.equals(Constants.IMPORT)) {
					hideDefaultSuccessMessage(actionRequest);

					importData(
						actionRequest, actionResponse,
						ExportImportHelper.TEMP_FOLDER_NAME +
							portlet.getPortletId());

					SessionMessages.add(
						actionRequest,
						PortalUtil.getPortletId(actionRequest) +
							SessionMessages.KEY_SUFFIX_CLOSE_REFRESH_PORTLET,
						portlet.getPortletId());

					sendRedirect(actionRequest, actionResponse, redirect);
				}
				else if (cmd.equals(Constants.PUBLISH_TO_LIVE)) {
					hideDefaultSuccessMessage(actionRequest);

					StagingUtil.publishToLive(actionRequest, portlet);

					sendRedirect(actionRequest, actionResponse);
				}
			}
		}
		catch (Exception e) {
			if (cmd.equals(Constants.ADD_TEMP) ||
				cmd.equals(Constants.DELETE_TEMP)) {

				handleUploadException(
					portletConfig, actionRequest, actionResponse,
					ExportImportHelper.TEMP_FOLDER_NAME +
						portlet.getPortletId(),
					e);
			}
			else {
				if ((e instanceof LARFileException) ||
					(e instanceof LARFileSizeException) ||
					(e instanceof LARTypeException) ||
					(e instanceof LocaleException) ||
					(e instanceof NoSuchLayoutException) ||
					(e instanceof PortletIdException) ||
					(e instanceof PrincipalException) ||
					(e instanceof StructureDuplicateStructureKeyException)) {

					SessionErrors.add(actionRequest, e.getClass());
				}
				else {
					_log.error(e, e);

					SessionErrors.add(
						actionRequest, ExportImportAction.class.getName());
				}
			}
		}
	}

	@Override
	public ActionForward render(
			ActionMapping actionMapping, ActionForm actionForm,
			PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse)
		throws Exception {

		Portlet portlet = null;

		try {
			portlet = ActionUtil.getPortlet(renderRequest);
		}
		catch (PrincipalException pe) {
			SessionErrors.add(renderRequest, pe.getClass());

			return actionMapping.findForward(
				"portlet.portlet_configuration.error");
		}

		renderResponse.setTitle(ActionUtil.getTitle(portlet, renderRequest));

		renderRequest = ActionUtil.getWrappedRenderRequest(renderRequest, null);

		String cmd = ParamUtil.getString(renderRequest, Constants.CMD);

		String forward = "portlet.portlet_configuration.export_import";

		if (cmd.equals(Constants.PUBLISH_TO_LIVE)) {
			forward = "portlet.portlet_configuration.staging";
		}

		return actionMapping.findForward(getForward(renderRequest, forward));
	}

	@Override
	public void serveResource(
			ActionMapping actionMapping, ActionForm actionForm,
			PortletConfig portletConfig, ResourceRequest resourceRequest,
			ResourceResponse resourceResponse)
		throws Exception {

		String cmd = ParamUtil.getString(resourceRequest, Constants.CMD);

		PortletContext portletContext = portletConfig.getPortletContext();

		PortletRequestDispatcher portletRequestDispatcher = null;

		if (cmd.equals(Constants.EXPORT)) {
			portletRequestDispatcher = portletContext.getRequestDispatcher(
				"/html/portlet/portlet_configuration/" +
					"export_portlet_processes.jsp");
		}
		else if (cmd.equals(Constants.IMPORT)) {
			portletRequestDispatcher = portletContext.getRequestDispatcher(
				"/html/portlet/portlet_configuration/" +
					"import_portlet_processes.jsp");
		}
		else if (cmd.equals(Constants.PUBLISH)) {
			portletRequestDispatcher = portletContext.getRequestDispatcher(
				"/html/portlet/portlet_configuration/" +
					"publish_portlet_processes.jsp");
		}
		else {
			portletRequestDispatcher = portletContext.getRequestDispatcher(
				"/html/portlet/portlet_configuration/" +
					"import_portlet_resources.jsp");
		}

		resourceRequest = ActionUtil.getWrappedResourceRequest(
			resourceRequest, null);

		portletRequestDispatcher.include(resourceRequest, resourceResponse);
	}

	protected void exportData(
			ActionRequest actionRequest, ActionResponse actionResponse,
			Portlet portlet)
		throws Exception {

		try {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			long plid = ParamUtil.getLong(actionRequest, "plid");
			long groupId = ParamUtil.getLong(actionRequest, "groupId");
			String fileName = ParamUtil.getString(
				actionRequest, "exportFileName");

			Map<String, Serializable> settingsMap =
				ExportImportConfigurationSettingsMapFactory.
					buildExportSettingsMap(
						themeDisplay.getUserId(), plid, groupId,
						portlet.getPortletId(), actionRequest.getParameterMap(),
						StringPool.BLANK, themeDisplay.getLocale(),
						themeDisplay.getTimeZone(), fileName);

			ExportImportConfiguration exportImportConfiguration =
				ExportImportConfigurationLocalServiceUtil.
					addExportImportConfiguration(
						themeDisplay.getUserId(), groupId, StringPool.BLANK,
						StringPool.BLANK,
						ExportImportConfigurationConstants.TYPE_EXPORT_PORTLET,
						settingsMap, WorkflowConstants.STATUS_DRAFT,
						new ServiceContext());

			ExportImportServiceUtil.exportPortletInfoAsFileInBackground(
				exportImportConfiguration);
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}

			SessionErrors.add(actionRequest, e.getClass(), e);
		}
	}

	@Override
	protected void importData(
			ActionRequest actionRequest, String fileName,
			InputStream inputStream)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long plid = ParamUtil.getLong(actionRequest, "plid");
		long groupId = ParamUtil.getLong(actionRequest, "groupId");

		Portlet portlet = ActionUtil.getPortlet(actionRequest);

		Map<String, Serializable> settingsMap =
			ExportImportConfigurationSettingsMapFactory.buildImportSettingsMap(
				themeDisplay.getUserId(), plid, groupId, portlet.getPortletId(),
				actionRequest.getParameterMap(), StringPool.BLANK,
				themeDisplay.getLocale(), themeDisplay.getTimeZone(),
				StringPool.BLANK);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addExportImportConfiguration(
					themeDisplay.getUserId(), groupId, StringPool.BLANK,
					StringPool.BLANK,
					ExportImportConfigurationConstants.TYPE_IMPORT_PORTLET,
					settingsMap, WorkflowConstants.STATUS_DRAFT,
					new ServiceContext());

		ExportImportServiceUtil.importPortletInfoInBackground(
			exportImportConfiguration, inputStream);
	}

	@Override
	protected MissingReferences validateFile(
			ActionRequest actionRequest, InputStream inputStream)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long plid = ParamUtil.getLong(actionRequest, "plid");
		long groupId = ParamUtil.getLong(actionRequest, "groupId");

		Portlet portlet = ActionUtil.getPortlet(actionRequest);

		Map<String, Serializable> settingsMap =
			ExportImportConfigurationSettingsMapFactory.buildImportSettingsMap(
				themeDisplay.getUserId(), plid, groupId, portlet.getPortletId(),
				actionRequest.getParameterMap(), StringPool.BLANK,
				themeDisplay.getLocale(), themeDisplay.getTimeZone(),
				StringPool.BLANK);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addExportImportConfiguration(
					themeDisplay.getUserId(), groupId, StringPool.BLANK,
					StringPool.BLANK,
					ExportImportConfigurationConstants.TYPE_IMPORT_PORTLET,
					settingsMap, WorkflowConstants.STATUS_DRAFT,
					new ServiceContext());

		return ExportImportServiceUtil.validateImportPortletInfo(
			exportImportConfiguration, inputStream);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ExportImportAction.class);

}