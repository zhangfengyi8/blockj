package org.rockyang.blockj.client.cmd;

import org.rockyang.blockj.client.cmd.utils.CliContext;
import org.rockyang.blockj.client.rpc.BlockService;

import java.math.BigDecimal;

/**
 * @author yangjian
 */
public class Send extends Command
{

    public Send(BlockService service)
    {
        this.name = "send";
        this.fullName = "send";
        this.desc = "Send funds between accounts";
        this.blockService = service;
    }

    @Override
    public void action(CliContext context)
    {
        String from = context.getOption("from");
        String to = context.getOption("to");
        BigDecimal value = context.getBigDecimal("value");
        String param = context.getOption("param");

        String cid = blockService.sendMessage(from, to, value, param);
        System.out.printf("Send message, CID: %s\n", cid);
    }
}
