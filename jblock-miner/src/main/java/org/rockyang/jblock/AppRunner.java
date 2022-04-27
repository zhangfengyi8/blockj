package org.rockyang.jblock;

import org.apache.commons.lang3.StringUtils;
import org.rockyang.jblock.base.crypto.Sign;
import org.rockyang.jblock.base.model.*;
import org.rockyang.jblock.base.store.Datastore;
import org.rockyang.jblock.base.store.RocksDatastore;
import org.rockyang.jblock.base.utils.CmdArgsParser;
import org.rockyang.jblock.base.utils.SerializeUtils;
import org.rockyang.jblock.service.AccountService;
import org.rockyang.jblock.service.BlockService;
import org.rockyang.jblock.service.WalletService;
import org.rockyang.jblock.miner.Miner;
import org.rockyang.jblock.miner.pow.PowMiner;
import org.rockyang.jblock.miner.pow.ProofOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AppRunner {

	static final Logger logger = LoggerFactory.getLogger(AppRunner.class);
	private String repo;
	private final CmdArgsParser parser;
	private Datastore datastore;

	public AppRunner(String[] args)
	{
		parser = new CmdArgsParser(args);
		repo = parser.getOption("repo");
		if (StringUtils.isEmpty(repo)) {
			repo = System.getProperty("JBLOCK_PATH");
			if (StringUtils.isEmpty(repo)) {
				repo = System.getProperty("user.home") + "/.jblock";
			}
		}
	}

	public boolean preRun() throws Exception
	{
		String opt = parser.getArgs(0);
		if (StringUtils.isEmpty(opt)) {
			System.out.println("No operation input");
			return false;
		}
		Block block;
		File dir = new File(repo);
		switch (opt) {
			case "genesis":
				logger.info("Try to create a genesis miner in {}", repo);
				if (dir.exists()) {
					throw new RuntimeException(String.format("A miner repo is already initialized in '%s'", dir));
				}
				datastore = new RocksDatastore(repo);
				// generate genesis block
				block = createGenesisBlock();
				// initialize the timestamp of block
				long createTime = block.getHeader().getCreateTime();
				block.getHeader().setTimestamp((createTime - (createTime % Miner.BLOCK_DELAY_SECS)) + Miner.BLOCK_DELAY_SECS);

				// save block
				saveBlock(block);
				logger.info("Initialize miner successfully, genesis block hash: {}", block.getHeader().getHash());

				// generate the genesis block file
				String genesisFile = System.getProperty("user.dir") + "/genesis.car";
				byte[] bytes = SerializeUtils.serialize(block);
				FileOutputStream fos = new FileOutputStream(genesisFile);
				fos.write(bytes);
				fos.close();
				logger.info("Generated the genesis file: {} successfully.", genesisFile);
				break;

			case "init":
				if (dir.exists()) {
					throw new RuntimeException(String.format("A miner repo is already initialized in '%s'", dir));
				}
				datastore = new RocksDatastore(repo);
				String genesis = parser.getOption("genesis");
				if (StringUtils.isEmpty(genesis)) {
					logger.error("must pass genesis file path");
					System.exit(0);
				}
				File file = new File(genesis);
				if (!file.exists()) {
					logger.error("file {} not exits", file);
					System.exit(0);
				}
				byte[] data = new byte[(int) file.length()];
				FileInputStream fis = new FileInputStream(genesis);
				int read = fis.read(data);
				if (read != file.length()) {
					logger.error("read file {} failed", file);
					System.exit(0);
				}
				fis.close();
				block = (Block) SerializeUtils.unSerialize(data);
				Wallet wallet = createMinerWallet();
				saveBlock(block);
				logger.info("Initialize miner successfully, repo: {}, miner: {}", repo, wallet.getAddress());
				// @TODO: create properties file for repo
				break;

			case "run":
				return true;
		}
		return false;
	}

	public Wallet createMinerWallet() throws Exception
	{
		// create the default wallet
		Wallet wallet = new Wallet();
		datastore.put(WalletService.WALLET_PREFIX + wallet.getAddress(), wallet);
		datastore.put(WalletService.MINER_ADDR_KEY, wallet.getAddress());
		// init the reward address balance
		Account rewardAccount = new Account(Miner.REWARD_ADDR, Miner.TOTAL_SUPPLY, null, 0);
		datastore.put(AccountService.ACCOUNT_PREFIX + rewardAccount.getAddress(), rewardAccount);
		return wallet;
	}

	public Block createGenesisBlock() throws Exception
	{
		Wallet wallet = createMinerWallet();

		// create the genesis message
		Message message = new Message();
		message.setFrom(Miner.REWARD_ADDR);
		message.setTo(wallet.getAddress());
		message.setParams("Miner Reward.");
		message.setCid(message.genMsgCid());
		message.setPubKey(wallet.getPubKey());
		message.setValue(Miner.GENESIS_ACCOUNT_BALANCE);
		// sign the message
		String sign = Sign.sign(wallet.getPriKey(), message.toSigned());
		message.setSign(sign);

		BlockHeader header = new BlockHeader(0, null);
		header.setNonce(PowMiner.GENESIS_BLOCK_NONCE);
		header.setDifficulty(ProofOfWork.getTarget());
		header.setHash(header.genCid());

		Block block = new Block(header);
		block.addMessage(message);

		// sign the block
		block.setPubKey(wallet.getPubKey());
		String blockSig = Sign.sign(wallet.getPriKey(), block.genCid());
		block.setBlockSign(blockSig);

		return block;
	}

	public void saveBlock(Block block)
	{
		// save the block
		datastore.put(BlockService.BLOCK_PREFIX + block.getHeader().getHash(), block);
		// add search index for block height
		datastore.put(BlockService.BLOCK_HEIGHT_PREFIX + block.getHeader().getHeight(), block.getHeader().getHash());

		// add index for messages in block
		block.getMessages().forEach(message -> datastore.put(BlockService.BLOCK_MESSAGE_PREFIX + message.getCid(), message));
		// update chain head
		datastore.put(BlockService.CHAIN_HEAD_KEY, block.getHeader().getHeight());
	}

	public void cleanRepo()
	{
		File file = new File(repo);
		file.delete();
	}

}
