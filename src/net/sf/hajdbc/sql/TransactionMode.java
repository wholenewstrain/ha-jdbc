/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2007 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql;

import java.util.concurrent.ExecutorService;

import net.sf.hajdbc.util.Enums;
import net.sf.hajdbc.util.concurrent.SynchronousExecutor;

/**
 * @author Paul Ferraro
 *
 */
public enum TransactionMode
{
	PARALLEL(null), SERIAL(new SynchronousExecutor());
	
	private final ExecutorService executor;
	
	private TransactionMode(ExecutorService executor)
	{
		this.executor = executor;
	}
	
	/**
	 * Used by JiBX to unmarshal a transaction mode
	 * @param value transaction mode
	 * @return transaction mode enum
	 */
	public static TransactionMode deserialize(String value)
	{
		return Enums.valueOf(TransactionMode.class, value);
	}
	
	/**
	 * Used by JiBX to marshal a transaction mode
	 * @param mode transaction mode enum
	 * @return transaction mode
	 */
	public static String serialize(TransactionMode mode)
	{
		return Enums.id(mode);
	}
	
	public ExecutorService getTransactionExecutor(ExecutorService executor)
	{
		return (this.executor != null) ? this.executor : executor;
	}
}