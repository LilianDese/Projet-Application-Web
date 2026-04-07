//=============================================================================
// SEULEMENT UTILE POUR REPARER DATABASE SI MODIFICATION SANS DEVOIR HARD RESET
//=============================================================================
package n7.facade.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class HsqldbSchemaFix {

	private final DataSource dataSource;

	@Autowired
	public HsqldbSchemaFix(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@PostConstruct
	public void applyFixes() {
		try (Connection connection = dataSource.getConnection()) {
			String product = connection.getMetaData().getDatabaseProductName();
			if (product == null || !product.toLowerCase().contains("hsql")) {
				return;
			}

			ensureColumnExists(connection, "JOUEUR", "WINS",
					"ALTER TABLE JOUEUR ADD COLUMN WINS INTEGER DEFAULT 0 NOT NULL");
			ensureColumnExists(connection, "JOUEUR", "GAMES_PLAYED",
					"ALTER TABLE JOUEUR ADD COLUMN GAMES_PLAYED INTEGER DEFAULT 0 NOT NULL");
		} catch (Exception e) {
			// best-effort; do not prevent app start on schema fix attempts
		}
	}

	private static void ensureColumnExists(Connection connection, String tableName, String columnName, String alterSql)
			throws Exception {
		if (columnExists(connection, tableName, columnName)) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute(alterSql);
		}
	}

	private static boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
		DatabaseMetaData meta = connection.getMetaData();
		// HSQLDB stores unquoted identifiers uppercase. Use a couple of schema patterns for robustness.
		if (columnExists(meta, null, tableName, columnName)) {
			return true;
		}
		return columnExists(meta, "PUBLIC", tableName, columnName);
	}

	private static boolean columnExists(DatabaseMetaData meta, String schema, String tableName, String columnName)
			throws Exception {
		try (ResultSet rs = meta.getColumns(null, schema, tableName, columnName)) {
			return rs.next();
		}
	}
}
