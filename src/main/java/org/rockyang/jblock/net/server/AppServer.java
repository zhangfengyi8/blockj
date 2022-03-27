package org.rockyang.jblock.net.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;

import java.io.IOException;

/**
 * Tio server starter
 * @author yangjian
 */
@Component
public class AppServer {

	@Value("${tio.server.address}")
	private String address;
	@Value("${tio.server.port}")
	private int port;

	public AppServer(TioServerConfig serverConfig) throws IOException
	{
		TioServer server = new TioServer(serverConfig);
		server.start(address, port);
	}
}