package org.fda;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.jboss.resteasy.core.Dispatcher;

public class WebApplication extends Application{
    private Set<Object> singletons = new HashSet();
    
    public WebApplication(@Context Dispatcher dispatcher) {
    	
    	try {
    		Properties p = new Properties();
			p.load(getClass().getResourceAsStream("/dfc.properties"));
			singletons.add(new WebEndpoint(p));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
 
    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
