package org.fda;

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/")
public class WebEndpoint {

	private Manager manager;

	public WebEndpoint(Properties p) {
		manager = new Manager(p.getProperty("dfc.globalregistry.repository"),
				p.getProperty("dfc.globalregistry.username"),
				p.getProperty("dfc.globalregistry.password"),
				p.getProperty("tmp.folder"));
	}

	@GET
	@Path("ping")
	public String ping() {
		return "pong";
	}

	@POST()
	@Path("file/merge")
	public String mergeFile(String fileName) {

		return "ok";
	}

}
