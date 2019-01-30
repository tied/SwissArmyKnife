/**
 * CharterForm.java
 * 
 * Defines a Confluence macro that provides a form to allow the user to enter data in multiple text areas.
 * A button is provided to submit the form data to JIRA and create/update an initiative issue type.
 * 
 * @author michael.howard
 * 
 */
package com.trustvesta.plugins.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import java.util.Map;

/** 
 * CharterForm class definition implements com.atlassian.confluence.macro.Macro.
 * 
 * No component injection is needed here.
 *
 */
public class CharterForm implements Macro {

    private ConversionContext conversionContext;

    /**
     * execute()
     * 
     * Entry point into the macro.  It creates a context, renders CharterForm.vm into it and
     * returns.
     * 
     * @param parameters
     * @param body
     * @param conversionContext
     * @throws MacroExecutionException
     * @return String representing the context with the rendered template.
     */
    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException {
        this.conversionContext = conversionContext;
        
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        contextMap.put("CharterFormMacroObject", this);
        return VelocityUtils.getRenderedTemplate("templates/CharterForm.vm", contextMap);
    }

    /**
     * setProperty()
     * 
     * Used to set a page property
     * 
     * @param property
     * @param value
     * 
     */
    public void setProperty(String property, String value) {
        conversionContext.getEntity().getProperties().setStringProperty(property, value);
    }

    /**
     * getProperty()
     * 
     * Used to get a page property
     * 
     * @param property
     * @return String representing the property value.
     * 
     */
    public String getProperty(String property) {
        String value = conversionContext.getEntity().getProperties().getStringProperty(property);
        if (value == null) { value = ""; }
        return value;
    }

    /**
     * getTitle()
     * 
     * Fetch the Confluence page title.
     * 
     * @return String represening the title.
     * 
     */
    public String getTitle() {
        return conversionContext.getEntity().getTitle();
    }

    /**
     * getCreator()
     * 
     * Fetch the creator of the Confluence page the macro is on.
     * 
     * @return String representing the creator's userid.
     * 
     */
    public String getCreator() {
        String name = "";
        try {
        ConversionContext context = conversionContext;
        ContentEntityObject entity = context.getEntity();
        ConfluenceUser user = entity.getCreator();
        if (user != null) {
            name = user.getFullName();
        }
        } catch (Exception e) {
            System.out.println("Exception in getCreator(): " + e);
        }
        
        return name;
    }
    
    /**
     * getBodyType()
     * 
     * Return the type contained within the macro body.
     * 
     * @return BodyType
     * 
     */
    public BodyType getBodyType() { return BodyType.NONE; }
    
    /**
     * getOutputType()
     * 
     * Return the type that the macro outputs.
     *
     * @return OutputType
     * 
     */
    public OutputType getOutputType() { return OutputType.BLOCK; }
}