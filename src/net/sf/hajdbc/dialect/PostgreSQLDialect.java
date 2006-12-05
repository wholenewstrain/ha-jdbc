/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2006 Paul Ferraro
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
package net.sf.hajdbc.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;

/**
 * @author  Paul Ferraro
 * @since   1.1
 */
public class PostgreSQLDialect extends DefaultDialect
{
	/**
	 * Default implementation will not block INSERT statements.
	 * Requires explicit exclusive mode table lock.
	 * <p><em>From PostgreSQL documentation</em></p>
	 * Unlike traditional database systems which use locks for concurrency control, PostgreSQL maintains data consistency by using a multiversion model (Multiversion Concurrency Control, MVCC).
	 * This means that while querying a database each transaction sees a snapshot of data (a database version) as it was some time ago, regardless of the current state of the underlying data.
	 * This protects the transaction from viewing inconsistent data that could be caused by (other) concurrent transaction updates on the same data rows, providing transaction isolation for each database session.	 * 
	 * @see net.sf.hajdbc.dialect.DefaultDialect#getLockTableSQL(java.sql.DatabaseMetaData, java.lang.String, java.lang.String)
	 */
	@Override
	public String getLockTableSQL(DatabaseMetaData metaData, String schema, String table) throws SQLException
	{
		return MessageFormat.format("LOCK TABLE {0} IN EXCLUSIVE MODE", this.qualifyTable(metaData, schema, table));
	}

	/**
	 * PostgreSQL uses the native type OID for BLOBs.  However the driver interprets them as INTEGERs.  OID columns should really be interpretted as BLOBs.
	 * @see net.sf.hajdbc.Dialect#getColumnType(java.sql.ResultSetMetaData, int)
	 */
	@Override
	public int getColumnType(ResultSetMetaData metaData, int column) throws SQLException
	{
		return metaData.getColumnTypeName(column).equals("oid") ? Types.BLOB : super.getColumnType(metaData, column);
	}

	/**
	 * @see net.sf.hajdbc.dialect.DefaultDialect#truncateTablePattern()
	 */
	@Override
	protected String truncateTablePattern()
	{
		return "TRUNCATE TABLE {0}";
	}
}
