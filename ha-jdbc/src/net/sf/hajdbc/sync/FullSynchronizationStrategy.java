/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2004 Paul Ferraro
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
package net.sf.hajdbc.sync;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import net.sf.hajdbc.Messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Database-independent synchronization strategy that only updates differences between two databases.
 * This strategy is best used when there are <em>many</em> differences between the active database and the inactive database (i.e. very much out of sync).
 * The following algorithm is used:
 * <ol>
 *  <li>Drop the foreign keys on the inactive database (to avoid integrity constraint violations)</li>
 *  <li>For each database table:
 *   <ol>
 *    <li>Delete all rows in the inactive database table</li>
 *    <li>Query all rows on the active database table</li>
 *    <li>For each row in active database table:
 *     <ol>
 *      <li>Insert new row into inactive database table</li>
 *     </ol>
 *    </li>
 *   </ol>
 *  </li>
 *  <li>Re-create the foreign keys on the inactive database</li>
 * </ol>
 * @author  Paul Ferraro
 * @version $Revision$
 * @since   1.0
 */
public class FullSynchronizationStrategy extends AbstractSynchronizationStrategy
{
	private static Log log = LogFactory.getLog(FullSynchronizationStrategy.class);

	private String truncateTableSQL = "DELETE FROM {0}";
	private int maxBatchSize = 100;

	/**
	 * @see net.sf.hajdbc.SynchronizationStrategy#synchronize(java.sql.Connection, java.sql.Connection, java.util.List)
	 */
	public void synchronize(Connection inactiveConnection, Connection activeConnection, List tableList) throws SQLException
	{
		inactiveConnection.setAutoCommit(true);
		String quote = inactiveConnection.getMetaData().getIdentifierQuoteString();
		
		// Drop foreign keys
		Key.executeSQL(inactiveConnection, ForeignKey.collect(inactiveConnection, tableList), this.dropForeignKeySQL);
		
		inactiveConnection.setAutoCommit(false);
		
		Iterator tables = tableList.iterator();
		
		while (tables.hasNext())
		{
			String table = (String) tables.next();
			
			String deleteSQL = MessageFormat.format(this.truncateTableSQL, new Object[] { quote + table + quote });
			String selectSQL = "SELECT * FROM " + quote + table + quote;

			if (log.isDebugEnabled())
			{
				log.debug(deleteSQL);
			}
			
			Statement deleteStatement = inactiveConnection.createStatement();

			Statement selectStatement = activeConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			selectStatement.setFetchSize(this.fetchSize);
			
			Thread deleteExecutor = new Thread(new StatementExecutor(deleteStatement, deleteSQL));
			deleteExecutor.start();

			ResultSet resultSet = selectStatement.executeQuery(selectSQL);
			
			try
			{
				deleteExecutor.join();
			}
			catch (InterruptedException e)
			{
				// Statement executor cannot be interrupted
			}
			
			int deletedRows = deleteStatement.getUpdateCount();
			
			if (deletedRows < 0)
			{
				throw deleteStatement.getWarnings();
			}
			
			log.info(Messages.getMessage(Messages.DELETE_COUNT, new Object[] { new Integer(deletedRows), table }));
			
			deleteStatement.close();
			
			StringBuffer insertSQL = new StringBuffer("INSERT INTO ").append(quote).append(table).append(quote).append(" (");

			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			
			int columns = resultSetMetaData.getColumnCount();
			
			for (int i = 1; i <= columns; ++i)
			{
				if (i > 1)
				{
					insertSQL.append(", ");
				}
				
				insertSQL.append(quote).append(resultSetMetaData.getColumnName(i)).append(quote);
			}
			
			insertSQL.append(") VALUES (");
			
			for (int i = 1; i <= columns; ++i)
			{
				if (i > 1)
				{
					insertSQL.append(", ");
				}
				
				insertSQL.append("?");
			}
			
			insertSQL.append(")");
			
			PreparedStatement insertStatement = inactiveConnection.prepareStatement(insertSQL.toString());
			int statementCount = 0;
			
			while (resultSet.next())
			{
				for (int i = 1; i <= columns; ++i)
				{
					Object object = resultSet.getObject(i);
					int type = resultSetMetaData.getColumnType(i);
					
					if (resultSet.wasNull())
					{
						insertStatement.setNull(i, type);
					}
					else
					{
						insertStatement.setObject(i, object, type);
					}
				}
				
				insertStatement.addBatch();
				statementCount += 1;
				
				if ((statementCount % this.maxBatchSize) == 0)
				{
					insertStatement.executeBatch();
					insertStatement.clearBatch();
				}
				
				insertStatement.clearParameters();
			}

			if ((statementCount % this.maxBatchSize) > 0)
			{
				insertStatement.executeBatch();
			}

			log.info(Messages.getMessage(Messages.INSERT_COUNT, new Object[] { new Integer(statementCount), table }));
			
			insertStatement.close();
			selectStatement.close();
			
			inactiveConnection.commit();
		}

		inactiveConnection.setAutoCommit(true);

		// Recreate foreign keys
		Key.executeSQL(inactiveConnection, ForeignKey.collect(activeConnection, tableList), this.createForeignKeySQL);
	}
	
	/**
	 * @return the maxBatchSize.
	 */
	public int getMaxBatchSize()
	{
		return this.maxBatchSize;
	}

	/**
	 * @param maxBatchSize the maxBatchSize to set.
	 */
	public void setMaxBatchSize(int maxBatchSize)
	{
		this.maxBatchSize = maxBatchSize;
	}

	/**
	 * @return the truncateTableSQL.
	 */
	public String getTruncateTableSQL()
	{
		return this.truncateTableSQL;
	}

	/**
	 * @param truncateTableSQL the truncateTableSQL to set.
	 */
	public void setTruncateTableSQL(String truncateTableSQL)
	{
		this.truncateTableSQL = truncateTableSQL;
	}
}
