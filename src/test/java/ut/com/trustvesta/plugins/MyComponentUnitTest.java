package ut.com.trustvesta.plugins;

import org.junit.Test;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.trustvesta.plugins.api.MyPluginComponent;
import com.trustvesta.plugins.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
//    @ComponentImport
//    private final ApplicationLinkService applicationLinkService;
//    
//    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)  {
//        this.applicationLinkService = applicationLinkService;
//    }
    
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}