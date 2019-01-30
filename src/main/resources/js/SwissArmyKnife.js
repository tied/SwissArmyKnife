/* SwissArmyKnife.js
 * 
 * Defines a set of helper functions which can be invoked from the macro template elements.  
 * This is a controller-layer module which helps tie in the Velocity template UI module (view-layer) to
 * REST endpoints (model-layer) defined in the plugin.
 * 
 * @author michael.howard
 * 
 */


/* download()
 * 
 * Write a data string to a file and save in the browser downloads folder.
 * 
 * @param filename
 * @param text
 * 
 */
function download(filename, text) {
	  var element = document.createElement('a');
	  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
	  element.setAttribute('download', filename);

	  element.style.display = 'none';
	  document.body.appendChild(element);

	  element.click();

	  document.body.removeChild(element);
}

/* jiraReport()
 * 
 * Action invoked by the submit button of the template
 * 
 * @param startDate
 * @param endDate
 * 
 */
function jiraReport(startDate, endDate) {
	
	function response(response, status) {
		if (status == "success") {
			AJS.log("Downloading the JIRA query\n");
			var today = new Date();
			var date = today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate();
			var time = today.getHours() +'-'+today.getMinutes();
			var dateTime = date+'_'+time;
			download("JIRA_query_" + dateTime + ".csv", response);
		} else {
			alert("Error generating report\n" + JSON.stringify(response));
		}
		AJS.$('.button-spinner').spinStop();
	}
	
    AJS.log("In jiraReport(): " + startDate + " " + endDate);
    var payload = new Object();
    payload.startDate = startDate;
    payload.endDate = endDate;
    AJS.$('.button-spinner').spin();
	jQuery.ajax({
		type: "POST",
		contentType: "application/json",
		url: AJS.contextPath() + "/rest/jirarequest/1.0/issuereport",
		data: JSON.stringify(payload),
		dataType: "text",
		success: response,
		error: response 
	});
}

/* baseData()
 * 
 * Assemble the payload data to be sent on the updateInitiative().  Variables are read
 * from the textarea ids in the template.  NOTE: Any new textareas added to the template must
 * also add here!!  TBD, one day maybe auto scan?
 * 
 */
function baseData() {
	var requestData = {};
	requestData.issueKey = issuekey.value;
	requestData.issueType = issuetype.value;
	requestData.title = title.value;
	requestData.submitDate = submitdate.value;
	requestData.customer = customer.value;
	requestData.project = project.value;
	requestData.summary = summary.value;
	requestData.deliveryDate = deliverydate.value;
	requestData.statement = statement.value;
	requestData.outcome = outcome.value;
	requestData.justification = justification.value;
	requestData.revenue = revenue.value;
	requestData.opportunity = opportunity.value;
	requestData.expense = expense.value;
	
	return requestData;
}

/* extendedData()
 * 
 * Expanded payload data.  These are appended to the updateInitiative() POST only after
 * the JIRA initiative has passed the Initiative Approved status in the workflow.
 * 
 */
function extendedData() {
	var requestData = baseData();
	requestData.introduction = introduction.value;
	requestData.audience = audience.value;
	requestData.background = background.value;
	requestData.objective = objective.value;
	requestData.stakeholders = stakeholders.value;
	requestData.assumptions = assumptions.value;
	requestData.outofscope = outofscope.value;
	requestData.dependencies = dependencies.value;
	requestData.techimpact = techimpact.value;
	requestData.opimpact = opimpact.value;
	requestData.overview = overview.value;
	requestData.description = description.value;
	requestData.performance = performance.value;
	requestData.constraints = constraints.value;
	requestData.milestones = milestones.value;
	requestData.analysis = analysis.value;
	requestData.risks = risks.value;
	requestData.payImpact = payimpact.value;
	requestData.rmImpact = rmimpact.value;
	requestData.finImpact = finimpact.value;
	requestData.isImpact = isimpact.value;
	requestData.eaImpact = eaimpact.value;
	requestData.nfRequirements = nfrequirements.value;
	requestData.devEstimate = devestimate.value;
	requestData.otherEstimate = otherestimate.value;
	requestData.approval = approval.value;
	requestData.score = score.value;

	return requestData;
}

/* updateInitiative()
* 
* Action invoked from the Create/Update Initiative button in the template.  The response back
* is used to set the issuekey/issuelink/issuestatus elements in the template for display.
* 
*/
function updateInitiative() {
	function response(response, status) {
		if (status == "success") {
			jsonResponse = JSON.parse(response);
			var issueKey = jsonResponse.key;
			document.getElementById("issuekey").value = issueKey;
			
		    if (jsonResponse.self) {
			    var self = jsonResponse.self.split("rest");
			    var link = self[0] + "browse/" + issueKey;
				document.getElementById("issuelink").href = link;
		    }
			document.getElementById("issuestatus").value = status;
			alert("Successfully updated " + issueKey);
		} else {
			alert("Failed to update Initiative.\nError: " + JSON.stringify(response));
		}
		window.location.reload();
	}
    
	var baseStatuses = ["New Initiative", "Inception Backlog", "HLE", "Status not set", "To Do", ""];
	var data = baseData();
	if ( !baseStatuses.includes(issuestatus.value) ) {
		data = extendedData();
	}

	jQuery.ajax({
		type: "POST",
		contentType: "application/json",
		url: AJS.contextPath() + "/rest/jirarequest/1.0/updateissue",
		data: JSON.stringify(data),
		dataType: "text",
		success: response,
		error: response 
	});
}

/* getIssueStatus()
 * 
 * Called on each page load.  It fetches the JIRA issue key from the issuekey element 
 * and POSTs to the internal endpoint.  The resulting response is forwarded from JIRA
 * and status/link are parsed out and used to update the issuestatus/issuelink elements
 * in the template.
 *  
 */
function getIssueStatus() {
	function response(response, textStatus) {
		if (textStatus == "success") {
			jsonResponse = JSON.parse(response);
			if (jsonResponse) {
			    var status = jsonResponse.fields.status.name;
			    var issueKey = jsonResponse.key;
			    var self = jsonResponse.self.split("rest");
			    var link = self[0] + "browse/" + issueKey;
				document.getElementById("issuestatus").value = status;
				document.getElementById("issuelink").href = link;
			//<span class="aui-lozenge aui-lozenge-success" >
			}			
		} 
	}
	
	jQuery.ajax({
		type: "POST",
		contentType: "application/json",
		url: AJS.contextPath() + "/rest/jirarequest/1.0/issuestatus",
		data: issuekey.value,
		dataType: "text",
		success: response,
		error: response 
	});	
}

/*
 * The following are run on loading the page.
 * 
 * Set the text on the button, update the initiative status/link and show/hide the extened text areas
 * depending on if the status has passed "Initiative Approved".
 * 
 */
AJS.$( document ).ready(function() {

	if (issuekey.value == "") {
		document.getElementById("updateinitiative").value = "Create Initiative";
	}
	
    getIssueStatus();
    
    baseStatuses = ["New Initiative", "Inception Backlog", "HLE", "Status not set", "To Do", ""];
	if ( !baseStatuses.includes(issuestatus.value) ) {
		document.getElementById("extendedtext").style.visibility = "visible";
	}

});	
