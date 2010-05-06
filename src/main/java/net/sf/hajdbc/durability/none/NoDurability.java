/*
 * HA-JDBC: High-Availability JDBC
 * Copyright 2004-2009 Paul Ferraro
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.durability.none;

import java.util.Map;

import net.sf.hajdbc.Database;
import net.sf.hajdbc.ExceptionFactory;
import net.sf.hajdbc.durability.Durability;
import net.sf.hajdbc.durability.InvocationEvent;
import net.sf.hajdbc.durability.InvokerEvent;
import net.sf.hajdbc.durability.TransactionIdentifier;
import net.sf.hajdbc.logging.Level;
import net.sf.hajdbc.logging.Logger;
import net.sf.hajdbc.logging.LoggerFactory;
import net.sf.hajdbc.sql.InvocationStrategy;
import net.sf.hajdbc.sql.Invoker;

/**
 * @author paul
 *
 */
public class NoDurability<Z, D extends Database<Z>> implements Durability<Z, D>
{
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.durability.Durability#getInvocationStrategy(net.sf.hajdbc.sql.InvocationStrategy)
	 */
	@Override
	public InvocationStrategy getInvocationStrategy(InvocationStrategy strategy, Phase phase, TransactionIdentifier transactionId)
	{
		return strategy;
	}

	/**
	 * {@inheritDoc}
	 * @see net.sf.hajdbc.durability.Durability#getInvoker(net.sf.hajdbc.sql.Invoker)
	 */
	@Override
	public <T, R, E extends Exception> Invoker<Z, D, T, R, E> getInvoker(Invoker<Z, D, T, R, E> invoker, Phase phase, TransactionIdentifier transactionId, ExceptionFactory<E> exceptionFactory)
	{
		return invoker;
	}

	@Override
	public void recover(Map<InvocationEvent, Map<String, InvokerEvent>> invokers)
	{
		this.logger.log(Level.WARN, invokers.toString());
	}
}