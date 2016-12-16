package com.liferay.cleaner;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.service.LockLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFileEntryConstants;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryMetadataLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileVersionLocalServiceUtil;
import com.liferay.portlet.documentlibrary.store.DLStoreUtil;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
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

	private static Logger _log = LoggerFactory.getLogger(Cleaner.class.toString());
	
	public void cleanVersions(ActionRequest aReq, ActionResponse aRes) {

		/** 
		 * themeDisplay Objekt notwendig f�r userId und permissionChecker 
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
			 * (siehe liferay-portlet.xml und liferay-display.xml), so darf nur ein Omniadmin oder companyadmin diese Funktion ausfuehren
			 */
			if(themeDisplay.getPermissionChecker().isOmniadmin()||themeDisplay.getPermissionChecker().isCompanyAdmin()) {
				int i=0;

				
				/**
				 * Interation durch alle Files
				 */
				for(DLFileEntry file:files) {

					
					/**
					 * Abfrage, ob Anzahl der Versionen vom aktuellen File den voreingestellten Wert uebersteigt; da kein Workflow f�r Dokumente
					 * hinterlegt sind, haben alle Files und Versions den Status Approved 
					 */
					_log.info(i++ + " || Starte Bereinigung von " + file.getTitle());
					if(file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED)>noRest) {

						
						/**
						 * Liste aller Versions f�r dieses File
						 */
						List<DLFileVersion> versions = DLFileVersionLocalServiceUtil.getFileVersions(file.getFileEntryId(), 0);
						_log.info("Anzahl Versionen gesamt: " + versions.size());

						
						/**
						 * Subliste nach Abzug von noRest Eintraegen; versions hat am ersten Index standardmaessig dem aktuellsten Eintrag
						 */
						List<DLFileVersion> subVersions = versions.subList(noRest, file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED));
						_log.info("Anzahl ueberfluessiger Versionen: " + subVersions.size());

						
						/**
						 * FileLock setzen
						 */
						_log.info("==> FileLock wird gesetzt");
						try {
							if (!DLFileEntryLocalServiceUtil.hasFileEntryLock(themeDisplay.getUserId(), file.getFileEntryId())) {
								LockLocalServiceUtil.lock(themeDisplay.getUserId(), DLFileEntry.class.getName(), file.getFileEntryId(),
										null, false, DLFileEntryConstants.LOCK_EXPIRATION_TIME);
							}
						} catch (Exception e) {
							_log.error("Fehler beim Setzen vom FileLock:");
							_log.error(e.getMessage());
						}

						
						/**
						 * Iteration durch subVersions, um ueberfluessige Versions zu loeschen;
						 * DLFileEntryLocalServiceUtill.deleteFileVersions() loescht entsprechende Versions aus DB, Filesystem und zugehoerige Expandowerte
						 * Zum detaillierten Testen und Loggen werden die Schritte hier einzeln abgearbeitet
						 */
						int x = 1;
						for(DLFileVersion ver : subVersions) {

							
							/**
							 * Loesche Expandos 
							 */
							try {
								_log.info(x + ".) ==> Loesche FileVersion Expandos");
								ExpandoTable exTable = ExpandoTableLocalServiceUtil.getDefaultTable(themeDisplay.getCompanyId(), DLFileEntry.class.getName());
								_log.info(x + ".) ==> ExpandoTable: " + exTable.toString());
								ExpandoRowLocalServiceUtil.deleteRow(exTable.getTableId(), ver.getFileVersionId());
							} catch (Exception e) {
								_log.error("Fehler beim Loeschen von Expandos:");
								_log.error(e.getMessage());
							}
							
							
							/** 
							 * Bereinige Filesystem
							 */
							try {
								_log.info(x + ".) ==> Loesche Eintraege im Filesystem");
								DLStoreUtil.deleteFile(file.getCompanyId(), file.getDataRepositoryId(), file.getName(), ver.getVersion());
							} catch (Exception e) {
								_log.error("Fehler beim Loeschen des Eintrags im Filesystem:");
								_log.error(e.getMessage());
							}

							
							/** 
							 * Loesche Versionsmetadaten
							 */
							try {
								_log.info(x + ".) ==> Loesche Versionsmetadaten");
								DLFileEntryMetadataLocalServiceUtil.deleteFileVersionFileEntryMetadata(ver.getFileVersionId());
							} catch (Exception e) {
								_log.error("Fehler beim Loeschen von Versionsmetadaten:");
								_log.error(e.getMessage());
							}
							
							
							/** 
							 * Loesche Version
							 */
							try {
								_log.info(x + ".) ==> Loesche Version");
								DLFileVersionLocalServiceUtil.deleteDLFileVersion(ver.getFileVersionId());
							} catch (Exception e) {
								_log.error("Fehler beim Loeschen von Version:");
								_log.error(e.getMessage());
							}

							x++;
						}

						
						/**
						 * FileLock wieder aufheben
						 */
						_log.info("==> FileLock wird aufgehoben");
						try {
							LockLocalServiceUtil.unlock(DLFileEntry.class.getName(), file.getFileEntryId());
						} catch (Exception e) {
							_log.error("Fehler beim Aufheben vom FileLock:");
							_log.error(e.getMessage());
						}
					} else {
						_log.info("Zu wenig Versionen, keine Bereinigung notwendig");
					}
					_log.info(file.getTitle() + " ist fertig.");
					_log.info("------------------------------");
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
		} catch (IOException e) {
			_log.error("=======================================");
			_log.error("IOException bei sendRedirect im Cleaner");
			_log.error(e.getStackTrace().toString());
		}
	}

}
