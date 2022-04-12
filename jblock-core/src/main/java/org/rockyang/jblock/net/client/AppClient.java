package org.rockyang.jblock.net.client;

import org.apache.commons.lang3.StringUtils;
import org.rockyang.jblock.net.base.MessagePacket;
import org.rockyang.jblock.net.base.MessagePacketType;
import org.rockyang.jblock.net.base.Peer;
import org.rockyang.jblock.net.conf.AppConfig;
import org.rockyang.jblock.utils.SerializeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tio.client.ClientChannelContext;
import org.tio.client.ReconnConf;
import org.tio.client.TioClient;
import org.tio.client.TioClientConfig;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.utils.lock.ReadLockHandler;
import org.tio.utils.lock.SetWithLock;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Tio client starter
 *
 * @author yangjian
 */
@Component
public class AppClient {

	private static final Logger logger = LoggerFactory.getLogger(AppClient.class);

	private TioClient client;
	private final AppConfig appConfig;
	private final TioClientConfig clientConfig;

	public AppClient(AppConfig appConfig, AppClientHandler clientHandler, AppClientListener clientListener)
	{
		// set the auto reconnect
		ReconnConf reconnConf = new ReconnConf(5000L, 20);
		// init client config
		TioClientConfig clientConfig = new TioClientConfig(clientHandler, clientListener, reconnConf);
		clientConfig.setHeartbeatTimeout(AppConfig.HEART_TIMEOUT);
		this.clientConfig = clientConfig;
		this.appConfig = appConfig;
	}

	@PostConstruct
	public void run()
	{
		new Thread(() -> {
			try {
				this.client = new TioClient(clientConfig);
				// try to connect the genesis node
				connect(new Peer(appConfig.getGenesisAddress(), appConfig.getGenesisPort()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void sendGroup(MessagePacket messagePacket)
	{
		Tio.sendToGroup(clientConfig, AppConfig.CLIENT_GROUP_NAME, messagePacket);
	}

	// connect a new node
	public void connect(Peer peer) throws Exception
	{
		if (StringUtils.equals(peer.getIp(), appConfig.getServerAddress()) && peer.getPort() == appConfig.getServerPort()) {
			logger.info("skip self connections, {}", peer.toString());
			return;
		}

		SetWithLock<ChannelContext> setWithLock = Tio.getByGroup(clientConfig, AppConfig.CLIENT_GROUP_NAME);
		if (setWithLock == null) {
			doConnect(peer);
			return;
		}
		setWithLock.handle((ReadLockHandler<Set<ChannelContext>>) set -> {
			for (ChannelContext channelContext : set) {
				// if the node is connected, skip it
				if (channelContext.getClientNode().equals(peer)) {
					logger.info("skip connected peer {}", peer);
					return;
				}
			}
			try {
				doConnect(peer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void doConnect(Peer peer) throws Exception
	{
		ClientChannelContext channelContext = client.connect(peer);
		// send self server connection info
		Peer server = new Peer(appConfig.getServerAddress(), appConfig.getServerPort());
		MessagePacket packet = new MessagePacket();
		packet.setType(MessagePacketType.REQ_NEW_PEER);
		packet.setBody(SerializeUtils.serialize(server));
		if (Tio.send(channelContext, packet)) {
			Tio.bindGroup(channelContext, AppConfig.CLIENT_GROUP_NAME);
		}
	}
}
