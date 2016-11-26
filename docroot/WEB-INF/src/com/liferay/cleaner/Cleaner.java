package com.liferay.cleaner;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileVersionLocalServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import java.io.IOException;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Portlet implementation class Cleaner
 */
public class Cleaner extends MVCPortlet {
	private static Logger _log = LoggerFactory.getLogger(Cleaner.class);
 
	public void cleanVersions(ActionRequest aReq, ActionResponse aRes) {
		/** 
		 * themeDisplay Objekt notwendig für userId und permissionChecker 
		 */
		ThemeDisplay themeDisplay = (ThemeDisplay)aReq.getAttribute(WebKeys.THEME_DISPLAY);

		/** 
		 * Anzahl zu belassener Fileversionen aus select; 3, 5 oder 10; Standardwert ist 5 
		 */
		int  noRest = ParamUtil.getInteger(aReq, "noRest");
		List<DLFileEntry> files;

		try {
			/** 
			 * Liste aller Files 
			 */
			files = DLFileEntryLocalServiceUtil.getDLFileEntries(0, DLFileEntryLocalServiceUtil.getDLFileEntriesCount());

			/** 
			 * Permissionabfrage; auch wenn das Portlet nur im Control Panel einsehbar ist und sich in der category.hidden befindet, 
			 * (siehe liferay-portlet.xml und liferay-display.xml), so darf nur ein Omniadmin oder companyadmin diese Funktion ausführen
			 */
			if(themeDisplay.getPermissionChecker().isOmniadmin()||themeDisplay.getPermissionChecker().isCompanyAdmin()) {
				int i=0;
				/**
				 * Iteration durch alle Files
				 */
				for(DLFileEntry file:files) {
					/**
					 * Abfrage, ob Anzahl der Versionen vom aktuellen File den voreingestellten Wert übersteigt; da kein Workflow für Dokumente
					 * hinterlegt sind, haben alle Files und Versions den Status Approved 
					 */
					if(file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED)>noRest) {
						/**
						 * Liste aller Versions für dieses File
						 */
						List<DLFileVersion> versions = DLFileVersionLocalServiceUtil.getFileVersions(file.getFileEntryId(), 0);
						/**
						 * Subliste nach Abzug von noRest Einträgen; versions hat am ersten Index standardmäßig den aktuellsten Eintrag
						 */
						List<DLFileVersion> subVersions = versions.subList(noRest, file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED));
						/**
						 * Iteration durch subVersions, um überflüssige Versions zu löschen;
						 * DLFileEntryLocalServiceUtill.deleteFileVersions() löscht entsprechende Versions aus DB, Filesystem und zugehörige Expandowerte
						 */
						for(DLFileVersion ver : subVersions) {
							DLFileEntryLocalServiceUtil.deleteFileVersion(themeDisplay.getUserId(), file.getFileEntryId(), ver.getVersion());
						}
						
					}
					_log.info(i++ + ". " + file.getTitle() + " bereiningt.");
				}
				_log.info("===========================================================");
				_log.info("======================= FERTIG ============================");
				_log.info("===========================================================");
				SessionMessages.add(aReq, "cleaner-success");
			} else {
				SessionErrors.add(aReq, "permission-error");
			}
			sendRedirect(aReq, aRes);
		} catch (SystemException e) {
			_log.error("=======================================");
			_log.error("SystemException im Cleaner");
			_log.error(e.getStackTrace().toString());
		} catch (PortalException e) {
			_log.error("=======================================");
			_log.error("PortalException bei DLFileEntryLocalServiceUtil.deleteFileVersion im Cleaner");
			_log.error(e.getStackTrace().toString());
		} catch (IOException e) {
			_log.error("=======================================");
			_log.error("IOException bei sendRedirect im Cleaner");
			_log.error(e.getStackTrace().toString());
		}
	}

}
