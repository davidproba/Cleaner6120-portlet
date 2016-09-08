<%@ include file="/html/init.jsp" %>

<liferay-ui:error key="permission-error" message="Sie verfügen nicht über die notwendigen Berechtigungen für diese Aktion"/>
<liferay-ui:success key="cleaner-success" message="Das Aufräumen verlief erfolgreich" />

Dieses Tool dient dazu, einen Überblick über die im Portal enthaltenen Dokumente inkl Versionen zu erhalten sowie ggf. diese Fileversionen aufzuräumen. <br/><br /> 

Files gesamt: <%= DLFileEntryLocalServiceUtil.getDLFileEntriesCount() %><br/>
Versionen gesamt: <%= DLFileVersionLocalServiceUtil.getDLFileVersionsCount() %><br/>

<%
List<DLFileEntry> files = DLFileEntryLocalServiceUtil.getDLFileEntries(0, DLFileEntryLocalServiceUtil.getDLFileEntriesCount());
long size=0;
for(DLFileEntry file:files) {
	List<DLFileVersion> versions = DLFileVersionLocalServiceUtil.getFileVersions(file.getFileEntryId(), 0);
	for(DLFileVersion ver : versions) {
		size+=ver.getSize();
	}
}

long sizeP = 0;
String sizeS = "Bytes";
if(size>1024) {
	if(size>(1024*1024)) {
		if(size>(1024*1024*1024)) {
			sizeP=size/(1024*1024*1024);
			sizeS="GB";
		} else {
			sizeP=size/(1024*1024);
			sizeS="MB";
		}
	}
	else {
		sizeP=size/1024;
		sizeS="KB";
	}
}
%>
Gesamtgröße: ca. <%= sizeP %> <%= sizeS %><br/>
<%

PortletURL viewURL = renderResponse.createRenderURL();
viewURL.setWindowState(LiferayWindowState.NORMAL);

PortletURL actionURL = renderResponse.createActionURL();
actionURL.setParameter(ActionRequest.ACTION_NAME, "cleanVersions");
actionURL.setParameter("redirectURL", viewURL.toString());

%>


<c:if test='<%= themeDisplay.getPermissionChecker().isOmniadmin()||themeDisplay.getPermissionChecker().isCompanyAdmin() %>'>
<br/><br/>
	<aui:form action="<%= actionURL %>" method="POST" name="fm">
		<aui:select name="noRest" label="Anzahl zu belassener Versionen: ">
			<aui:option value="3">3</aui:option>
			<aui:option value="5" selected="true">5</aui:option>
			<aui:option value="10">10</aui:option>
		</aui:select>
		<aui:button type="submit" />
	</aui:form>

</c:if>
