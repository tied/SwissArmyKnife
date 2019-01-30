/**
 * JiraReport.java
 * 
 * Defines a Confluence macro which allows user input a form which can then submit a JQL query
 * to JIRA.
 * 
 * @author michael.howard
 * 
 */
package com.trustvesta.plugins.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import java.util.Map;

/**
 * JiraReport class definition.  This must implement the Atlassian SDK Macro class.
 *
 */
public class JiraReport implements Macro {

    /**
     * execute()
     * 
     * Entry point to the macro and is run upon accessing the macro via the Confluence page.  The UI is defined
     * via a velocity deplate which must be returned through the VelocityUtils object.
     * 
     * @param parameters: parameters set when editing the Confluence macro.
     * @param body: text body when editing Confluence macro.
     * @param conversionContext: ConversionContext object from Confluence page.
     * 
     * @throws MacroExecutionException
     * 
     * @return String representing the velocity rendered template
     * 
     */
    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException {
        
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        contextMap.put("JiraReportMacroObject", this);
        return VelocityUtils.getRenderedTemplate("templates/JiraReportForm.vm", contextMap);
    }

    /**
     * getBodyType()
     * 
     * Defines if the macro has a body.
     * 
     * @return BodyType.NONE
     */
    public BodyType getBodyType() { return BodyType.NONE; }
    
    /**
     * getOutputType()
     * 
     * Defines the macro output type.
     * 
     * @return OutputType.BLOCK
     * 
     */
    public OutputType getOutputType() { return OutputType.BLOCK; }
}