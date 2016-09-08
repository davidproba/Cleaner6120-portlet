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

/**
 * Portlet implementation class Cleaner
 */
public class Cleaner extends MVCPortlet {
 
	public void cleanVersions(ActionRequest aReq, ActionResponse aRes) {
		ThemeDisplay themeDisplay = (ThemeDisplay)aReq.getAttribute(WebKeys.THEME_DISPLAY);
		int  noRest = ParamUtil.getInteger(aReq, "noRest");
		List<DLFileEntry> files;
		try {

			files = DLFileEntryLocalServiceUtil.getDLFileEntries(0, DLFileEntryLocalServiceUtil.getDLFileEntriesCount());
			if(themeDisplay.getPermissionChecker().isOmniadmin()||themeDisplay.getPermissionChecker().isCompanyAdmin()) {
				
				for(DLFileEntry file:files) {
					
					if(file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED)>noRest) {
						
						List<DLFileVersion> versions = DLFileVersionLocalServiceUtil.getFileVersions(file.getFileEntryId(), 0);
						List<DLFileVersion> subVersions = versions.subList(noRest, file.getFileVersionsCount(WorkflowConstants.STATUS_APPROVED));
						for(DLFileVersion ver : subVersions) {
							DLFileEntryLocalServiceUtil.deleteFileVersion(themeDisplay.getUserId(), file.getFileEntryId(), ver.getVersion());
						}
						
					}
					
					System.out.println(file.getTitle() + " bereiningt.");
				}
				System.out.println("===========================================================");
				System.out.println("======================= FERTIG ============================");
				System.out.println("===========================================================");
				SessionMessages.add(aReq, "cleaner-success");
			} else {
				SessionErrors.add(aReq, "permission-error");
			}
			sendRedirect(aReq, aRes);
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
