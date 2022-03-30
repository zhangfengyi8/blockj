package org.rockyang.jblock.chain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * wallet account
 * @author yangjian
 */
public class Account implements Serializable {

	private String address;
	private BigDecimal balance;
	private String pubKey;
	private int messageNonce;

	public Account(String address, BigDecimal balance, String pubKey, int messageNonce)
	{
		this.address = address;
		this.balance = balance;
	}

	public Account() {}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public BigDecimal getBalance()
	{
		return balance;
	}

	public void setBalance(BigDecimal balance)
	{
		this.balance = balance;
	}

	public String getPubKey()
	{
		return pubKey;
	}

	public void setPubKey(String pubKey)
	{
		this.pubKey = pubKey;
	}

	public int getMessageNonce()
	{
		return messageNonce;
	}

	public void setMessageNonce(int messageNonce)
	{
		this.messageNonce = messageNonce;
	}
}
