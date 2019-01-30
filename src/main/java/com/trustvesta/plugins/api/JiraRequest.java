/**
 * JiraRequest.java
 * 
 * Defines a REST endpoint module within Confluence providing a collection of APIs and helper methods used to
 * assemble and send requests to JIRA.  These requests are sent to JIRA via the configured Application Link between
 * Confluence/JIRA.  The Application Link handles all authentication.  The sent JIRA requests are always in the form of
 * a RESTful GET/POST/PUT/DELETE.  Response data is also handled.
 * 
 * @author michael.howard
 *     
 */

package com.trustvesta.plugins.api;

import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request.MethodType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

/** 
 * JiraRequest class definition and root of the REST endpoint. ApplicationLinkService and PageManager objects are
 * injected by Confluence via constructor-based component injection (v2 plugins).  This is handled by the Spring
 * framework in Confluence and allows the plugin module to have access to the core component object.
 * 
 */
@Path("/")
public class JiraRequest {

    private ApplicationLink jiraApplicationLink;
    private ApplicationLinkRequest request;

    @ComponentImport
    private final ApplicationLinkService applicationLinkService;

    /**
     * JiraRequest() constructor.
     * 
     * The Spring framework will inject the component obects at construction time.  These are stored
     * as private class members.
     *  
     * @param applicationLinkService
     * @param pageManager
     * 
     */
    @Autowired
    public JiraRequest(ApplicationLinkService applicationLinkService) { 
        this.applicationLinkService = applicationLinkService;
        jiraApplicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);
    }
  
    /**
     * issueReport()
     * 
     * Defines the /issuereport REST endpoint which assembles a JQL query string to send to JIRA.
     * Query parameters are passed in with requestData.
     * 
     * @param requestData is a JSON string containing query parameters
     * @return Response object containing the JIRA response.
     * 
     */
    @Path("/issuereport")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIssueReport(String requestData) {
        String issueReport = "";
	
	    	try {
            JsonObject requestJson = (JsonObject) new JsonParser().parse(requestData);
            String startDate = requestJson.get("startDate").getAsString();
            String endDate = requestJson.get("endDate").getAsString();
            String jql = "issuetype=sub-task and sprint is not EMPTY and worklogDate >= " + 
                    startDate + " and worklogDate <= " + endDate;
            
            // First grab all sub-tasks that logged work between our date range.
            JsonArray subtaskQuery = jqlQuery(jql);
            // Now compress this down and add up all the logged work for each sub-task.
            JsonArray compressedSubtaskList = parseSubtaskQuery(subtaskQuery, startDate, endDate);
            // Now compress down all sub-tasks to give a final worklog sum for each story.
            JsonArray storyList = parseStoryList(compressedSubtaskList);
            // Iterate over each story and add it's parent Epic.
            JsonArray epicList = queryEpic(storyList);
            // Iterate over each Epic and add it's parent Initiative.
            JsonArray initiativeList = queryInitiative(epicList);

            issueReport = formatCSV(initiativeList);

	    	} catch (Exception e) {
	    		System.out.println("Exception in getIssueReport(): " + e);
	    	}
	
	    	return Response.ok(issueReport).build();
    }
    
    /**
     * jqlQuery()
     * 
     * Performs a POST via the Application Link to JIRA. It hits the /rest/api/latest/search
     * endpoint.  The payload is formed with the passed in jql string argument.  "startAt" and 
     * "maxResults" are also set in the payload.  Make sure that maxResults is always higher
     * than the number of issues being queried as paging is not done here.
     * 
     * @param jql
     * @return JsonObject represent a JQL query of all subtasks with work logged between the start/end date.
     * 
     */
    JsonArray jqlQuery(String jql) {
        JsonArray aggregateResponse = new JsonArray();
        
        try {
            ApplicationLinkRequestFactory requestFactory = jiraApplicationLink.createAuthenticatedRequestFactory();
            String endpoint = "/rest/api/latest/search";
            request = requestFactory.createRequest(MethodType.POST, endpoint);
            request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            
            JsonObject requestBody = new JsonObject();

            requestBody.addProperty("jql", jql);
            requestBody.addProperty("maxResults", 50);

            int startAt = 0;
            int currentResults = 0;
            int totalResults = 0;

            while (currentResults <= totalResults) {
                requestBody.addProperty("startAt", startAt);
                System.out.println("Request to JIRA: " + endpoint + "\nBody: " + requestBody.toString());
                request.setRequestBody(requestBody.toString());
                JsonObject response = (JsonObject) new JsonParser().parse( request.execute() );
                aggregateResponse.addAll(response.get("issues").getAsJsonArray());
                totalResults = response.get("total").getAsInt();
                currentResults += response.get("maxResults").getAsInt();
                startAt = currentResults;
            }
            
        } catch (Exception e) {
            System.out.println("Exception in jqlQuery(): " + e);
        }
        
        return aggregateResponse;
    }
    
    /**
     * issueQuery()
     * 
     * Performs a GET via the Application Link to JIRA.  It hits the /rest/api/latest/issue/<issueKey> 
     * endpoint.  The issue key string is passed in as an argument and JSON structure for the key is returned.  
     * 
     * @param issue
     * @return JsonObject representing details for the issue key
     * 
     */
    JsonObject issueQuery(String issue) {
        JsonObject responseJson = new JsonObject();
        
        try {
            ApplicationLinkRequestFactory requestFactory = jiraApplicationLink.createAuthenticatedRequestFactory();
            String endpoint = "/rest/api/latest/issue/" + issue;
            request = requestFactory.createRequest(MethodType.GET, endpoint);
            request.setHeader("Content-Type", MediaType.APPLICATION_JSON);

            String response = request.execute();
            responseJson = (JsonObject) new JsonParser().parse(response);
        } catch (Exception e) {
            System.out.println("Exception in issueQuery(): " + e);
        }
        
        return responseJson;
    }
    
    /**
     * parseSubtaskQuery()
     * 
     * Given a JSON object with all the queried subtasks as well as a start/end date, 
     * this method sums up the worklog in each subtask and returns a JsonArray with 1
     * element for each subtask.  The subtask key, parent story key and aggregate work
     * logged are all returned.
     * 
     * @param subtasks
     * @param startDate
     * @param endDate
     * @return JsonArray representing each subtask with parent story and aggregate worklog.
     * 
     */
    JsonArray parseSubtaskQuery(JsonArray issues, String startDate, String endDate) {
        JsonArray compressedSubtasks = new JsonArray();
         
        try {
            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            Date formattedStartDate = (Date)dateFormatter.parse(startDate);
            Date formattedEndDate = (Date)dateFormatter.parse(endDate);

            for (int i=0; i<issues.size(); i++ ) {
                String issueKey = issues.get(i).getAsJsonObject().get("key").getAsString();
                JsonObject subtask = issueQuery(issueKey);
                JsonObject strippedSubtask = new JsonObject();
                strippedSubtask.addProperty("subtaskKey", subtask.get("key").getAsString());
                strippedSubtask.addProperty("storyKey", subtask.getAsJsonObject("fields").getAsJsonObject("parent").get("key").getAsString());
                JsonObject worklog = subtask.getAsJsonObject("fields").getAsJsonObject("worklog");
                JsonArray worklogs = worklog.getAsJsonArray("worklogs");
                int timeSpentSeconds = 0;
                for (int j=0; j<worklog.get("total").getAsInt(); j++) {
                    JsonObject worklogEntry = worklogs.get(j).getAsJsonObject();
                    String started = worklogEntry.get("started").getAsString();
                    Date formattedStarted = (Date)dateFormatter.parse(started.substring(0, 10)); // Grab date string only.  Perhaps later we'll add time as well.
                    if (formattedStarted.after(formattedStartDate) && formattedStarted.before(formattedEndDate)) {
                        timeSpentSeconds += worklogEntry.get("timeSpentSeconds").getAsInt();
                    }
                }
                strippedSubtask.addProperty("timeSpent", timeSpentSeconds);
                compressedSubtasks.add(strippedSubtask);
            }
        } catch (Exception e) {
            System.out.println("Exception in parseSubtaskQuery(); " + e);
        }
        
        return compressedSubtasks;
    }
    
    /**
     * parseStoryList()
     * 
     * Given a subtask list as an argument, this method sums all the subtask logged work and returns a JsonArray with 1
     * entry per story.  Each contains the story key and logged work.
     * 
     * @param subtaskList
     * @return JsonArray with 1 entry per story containing the logged work.
     * 
     */
    JsonArray parseStoryList(JsonArray subtaskList) {
        JsonArray list = new JsonArray();
        
        try {
            ArrayList<JsonObject> storyList = new ArrayList<JsonObject>();
            String storyKey = "";
            int timeSpent = 0;
            int i = 0;
            Iterator<JsonElement> iterator = subtaskList.iterator();
            while (iterator.hasNext()) {
                JsonObject subtask = iterator.next().getAsJsonObject();
              // Case 1: Start of the subtask list
                if (storyKey.isEmpty()) {  
                    JsonObject story = new JsonObject();
                    storyKey = subtask.get("storyKey").getAsString();
                    story.addProperty("storyKey", storyKey);
                    timeSpent += subtask.get("timeSpent").getAsInt();
                    story.addProperty("timeSpent", timeSpent);
                    storyList.add(story);
              // Case 2: Story already exists in list    
                } else if (storyKey.equals( subtask.get("storyKey").getAsString() ) ) {  
                    JsonObject story = storyList.get(i).getAsJsonObject();
                    timeSpent += subtask.get("timeSpent").getAsInt();
                    story.addProperty("timeSpent", timeSpent);
                    storyList.set(i, story);
              // Case 3: New story for the lists
                } else {  
                    i++;
                    JsonObject story = new JsonObject();
                    storyKey = subtask.get("storyKey").getAsString();
                    timeSpent = subtask.get("timeSpent").getAsInt();
                    story.addProperty("storyKey", storyKey);
                    story.addProperty("timeSpent", timeSpent);
                    storyList.add(story);
                }
            }
            
            // This is horrible!  Fix soon.
            for (int j=0; j<storyList.size(); j++)  {
                list.add(storyList.get(j));
            }
        } catch (Exception e) {
            System.out.println("Exception in parseStoryList(): " + e);
        }
        
        return list;
    }
    
    /**
     * queryEpic()
     * 
     * Given a JSON Array list of stories passed as an argument, this method walks each
     * and queries JIRA to get the parent Epic.  It's parsed from the JSON response.  Note 
     * that the customfield_xxxxx is different for each JIRA instance.
     * 
     * Note that fixVersion is added as well but only 1.  It is possible to have multiple fixVersion
     * strings.  If this is a valid workflow, it will need to be handled here.
     * 
     * The Epic key is added on as a new property to the list and returned.
     * 
     * @param storyList
     * @return JSON Array representing a list of all stories, parent epics, fixVersion and work logged.
     * 
     */
    JsonArray queryEpic(JsonArray storyList) {
        JsonArray epicList = new JsonArray();
        System.out.println("storyList size: " + storyList.size() );
        try {
            Iterator<JsonElement> iterator = storyList.iterator();
            while (iterator.hasNext()) {                JsonObject storyElement = iterator.next().getAsJsonObject();
                JsonObject story = issueQuery(storyElement.get("storyKey").getAsString());
                JsonArray fixVersions = story.get("fields").getAsJsonObject().get("fixVersions").getAsJsonArray();
                String fixVersion = "";
                if ( fixVersions.size() > 0 ) {
                    fixVersion = fixVersions.get(0).getAsJsonObject().get("name").getAsString();
                }
                // customfield_xxxxx is unique to each JIRA instance and represents the parent Epic
                JsonObject fields = story.get("fields").getAsJsonObject();
                String epicKey = "";
                if ( !fields.isJsonNull() ) {
                    try {
                        epicKey = fields.get("customfield_10001").getAsString();
                    } catch (Exception e) {
                        // If the epic field is not populated, don't do anything.
                        // Fill with an empty string and move on.
                        //System.out.println("Can't get customfield_10001");
                    }
                }
                storyElement.addProperty("fixVersion", fixVersion);
                storyElement.addProperty("epicKey", epicKey);
                epicList.add(storyElement);
            }
        } catch (Exception e) {
            System.out.println("Exception in queryEpic(): " + e);
        }
        
        return epicList;
    }
    
    /**
     * queryInitiative()
     * 
     * Given a JSON Array list containing epics passed as an argument, this method walks each 
     * and queries JIRA to get the parent Initiative.  It's parsed from the JSON response.  Note 
     * that the customfield_xxxxx is different for each JIRA instance.
     * 
     * The Initiative key is add on as a new property to the list and returned.
     * 
     * @param epicList
     * @return JSON Array representing a list of epics and there parent initiatives.
     * 
     */
    JsonArray queryInitiative(JsonArray epicList) {
        JsonArray initiativeList = new JsonArray();
        
        try {
            Iterator<JsonElement> iterator = epicList.iterator();
            while (iterator.hasNext()) {
                JsonObject epicElement = iterator.next().getAsJsonObject();
                String epicKey = epicElement.get("epicKey").getAsString();
                String initiativeKey = "";
                if (!epicKey.isEmpty()) {
                    JsonObject epic = issueQuery(epicKey);
                    JsonObject fields = epic.get("fields").getAsJsonObject();
                    // customfield_xxxxx is unique to each JIRA instance and represents the parent Initiative
                    try {
                        initiativeKey = fields.get("customfield_10007").getAsString();
                    } catch (Exception e) {
                        // If initiative field is not populated, don't do anything.
                        // Just populate with an empty string and move on.
                        //System.out.println("Can't get customfield_10007");
                    }
                }
                epicElement.addProperty("initiativeKey", initiativeKey);
                initiativeList.add(epicElement);
            }
        } catch (Exception e) {
            System.out.println("Exception in queryInitiative(): " + e);
        }
        
        return initiativeList;
    }
    
    /**
     * formatCSV()
     * 
     * Given a JsonArray list of issues passed in as an argument, format into a comma-delimited
     * string.
     * 
     * @param issueList
     * @return String representing CSV format of the data
     * 
     */
    String formatCSV(JsonArray issueList) {
        String response = "fixVersion,timeSpent,story,epic,initiative\n";
        try {
            Iterator<JsonElement> iterator = issueList.iterator();
            
            while (iterator.hasNext()) {
                JsonObject issueElement = iterator.next().getAsJsonObject();
                response += issueElement.get("fixVersion").getAsString() + ","
                        + issueElement.get("timeSpent").getAsString() + ","
                        + issueElement.get("storyKey").getAsString() + ","
                        + issueElement.get("epicKey").getAsString() + ","
                        + issueElement.get("initiativeKey").getAsString() 
                        + "\n";
            }
            
        } catch (Exception e) {
            System.out.println("Exception in formatCSV(): " + e);
        }
        return response;
    }

    /**
     * updateIssue()
     * 
     * Defines the /updateissue REST endpoint and allows for the creation/update of an issue
     * in JIRA.  Fields for this issue are passed in via requestData.  
     * 
     * @param requestData
     * @return Response object representing the response from JIRA
     * 
     */
    @Path("/updateissue")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateIssue(String requestData) {
        String response = appLinkPost(requestData);
         
        if (response.isEmpty()) {
            return Response.serverError().build();
        }

        return Response.ok(response).build();
    }
    
    /**
     * requestBody()
     * 
     * Helper method that assembles the request data object that will be sent to JIRA via
     * a POST.
     * 
     * All cached page properties are assigned to a JSON object in a format needed by JIRA.
     * 
     * This method is very much a work in progress since all desired custom fields in JIRA
     * that need to be set from Confluence must be mapped here.  As these fields are defined 
     * and mapped to the macro text areas, they should be entered below.
     * 
     * @return String conversion of the JSON object that will be sent to JIRA.
     * 
     */
    public String requestBody(String requestData) {
        JsonObject request = new JsonObject();
        JsonObject fields = new JsonObject();
        JsonObject project = new JsonObject();
        JsonObject issueType = new JsonObject();
        try {
            JsonObject requestJson = (JsonObject) new JsonParser().parse(requestData);
            
            // These are basic fields needed to create/update a JIRA issue
            project.addProperty("key", requestJson.get("project").getAsString());
            issueType.addProperty("name", requestJson.get("issueType").getAsString());

            fields.add("project", project);
            fields.addProperty("summary", requestJson.get("title").getAsString());
            fields.addProperty("description", requestJson.get("title").getAsString());
            fields.add("issuetype", issueType);
            String projectProperty = requestJson.get("project").getAsString();
            
            // Below are the JIRA custom fields.  The extent of these are still being determined and should be 
            // added here as needed.  Note that they are bypassed if using a project "TEST".  This allows 
            // development using a sandbox JIRA with a simple "TEST" project defined.
            if (!projectProperty.equals("TEST")) {
                fields.addProperty("customfield_11301", requestJson.get("submitDate").getAsString());
                //fields.addProperty("customfield_11601", "{value:" + pageProperties.get("customer").getAsString() + "}");
                fields.addProperty("customfield_11302", requestJson.get("summary").getAsString());
                fields.addProperty("customfield_10600", requestJson.get("deliveryDate").getAsString());
                // fields.addProperty("customfield_TBD", pageProperties.get("statement").getAsString());  // not in JIRA yet
                fields.addProperty("customfield_11303", requestJson.get("outcome").getAsString());
                fields.addProperty("customfield_11304", requestJson.get("justification").getAsString());
                fields.addProperty("customfield_11307", requestJson.get("revenue").getAsString());
                //fields.addProperty("customfield_10405", "{value:" + pageProperties.get("opportunity").getAsString() + "}");
                // fields.addProperty("customfield_TBD", pageProperties.get("expense").getAsString()); // not in JIRA yet
            }
            request.add("fields", fields);
        } catch (Exception e) {
            System.out.println("Exception on creating request body: " + e);
        }
        return request.toString();
    }
    
    /**
     * appLinkPost()
     * 
     * Helper method that will do the actual POST to JIRA.  It assembles a request object from the ApplicationLink
     * to JIRA.
     * 
     * This helper is used both to create and update JIRA issues.  The issueKey parameter (inside JSON container) defines which is used.  
     * If null, a new issue is created via a PUT.  Otherwise, the existing issue is updated via a POST.  The
     * request headers are set.  The body of the request is set via a call to the requestBody() helper method.
     * 
     * The response from the JIRA PUT/POST is returned.
     *   
     * @param requestData
     * @return String representing Application Link JIRA POST response.
     * 
     */
    public String appLinkPost(String requestData) {
        String response = "";

        try {
            JsonObject jsonRequest = (JsonObject) new JsonParser().parse(requestData);
            ApplicationLinkRequestFactory requestFactory = jiraApplicationLink.createAuthenticatedRequestFactory();
            String endpoint = "/rest/api/2/issue";
            String issueKey = jsonRequest.get("issueKey").getAsString();
            if (!issueKey.isEmpty()) {  // Update issue scenario
                endpoint += "/" + issueKey;
                request = requestFactory.createRequest(MethodType.PUT, endpoint);
            } else {   // Create issue scenario
                request = requestFactory.createRequest(MethodType.POST, endpoint);
            }
            
            request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            String requestBody = requestBody(requestData);
            System.out.println("Request to JIRA body: " + requestBody);
            request.setRequestBody(requestBody);
            response = request.execute();

            // If the response back from JIRA is empty, store it as {"key": issueKey}
            if (response.equals("")) {
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("key", issueKey);
                response = responseJson.toString();
            }
        } catch (Exception e) {
            System.out.println("Exception in appLinkPost(): " + e);
        }

        return response;
    }
    
    /**
     * getIssueStatus()
     * 
     * Fetch the details on a given JIRA issue passed in whith issueKey.  The complete JSON
     * response is returned and it is up to the consumer to parse out the issue status.
     *
     * @param issueKey
     * @return JSON representing the response from JIRA on the given issue
     * 
     */
    @Path("/issuestatus")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIssueStatus(String issueKey) {
        String response = "";

        try {
            ApplicationLinkRequestFactory requestFactory = jiraApplicationLink.createAuthenticatedRequestFactory();
            String endpoint = "/rest/api/2/issue/" + issueKey;
            request = requestFactory.createRequest(MethodType.GET, endpoint);
            
            request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            response = request.execute();
        } catch (Exception e) {
            System.out.println("Exception in getIssueStatus(): " + e);
        }

        return Response.ok(response).build();
    }
}
