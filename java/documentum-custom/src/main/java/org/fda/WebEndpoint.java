package org.fda;

import java.util.Properties;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.log4j.Logger;

@Path("/")
public class WebEndpoint {

	Logger log = Logger.getRootLogger();
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

	@GET()
	@Path("file/merge")
	public String mergeFile(@QueryParam("filePath") final String filePath) {
		String threadName = filePath.replaceAll("[^a-zA-Z0-9]", "");

		Thread t = getThread(threadName);

		if (t == null) {
			t = new Thread(threadName) {
				@Override
				public void run() {
					try {
						manager.mergeDocument(filePath);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			t.start();
		} else {
			return "1 - Operation canceled: There is one thread running for that file.";
		}

		return "0 - Thread started: " + threadName;
	}

	@GET()
	@Path("file/merge/stop")
	public String stopMerge(@QueryParam("filePath") final String filePath) {
		String threadName = filePath.replaceAll("[^a-zA-Z0-9]", "");

		Thread t = getThread(threadName);
		if (t == null) {
			return "1 - Thread not running.";
		}

		try {
			t.interrupt();
			t.stop();
		} catch (Exception e) {
			log.warn("Thread stoped", e);
		}
		return "0 - Thread " + threadName + " stopped";
	}

	private Thread getThread(String name) {
		Thread thread = null;
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getName().equals(name))
				thread = t;
			break;
		}
		return thread;
	}

	protected class CallMerge implements Callable<String> {
		@Override
		public String call() throws Exception {

			return null;
		}
	}

}
