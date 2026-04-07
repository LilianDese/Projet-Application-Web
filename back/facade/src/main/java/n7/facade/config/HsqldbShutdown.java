package n7.facade.config;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class HsqldbShutdown {

	private final DataSource dataSource;

	@Autowired
	public HsqldbShutdown(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@PreDestroy
	public void shutdown() {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("SHUTDOWN");
		} catch (Exception e) {
			// best-effort; container shutdown should not fail because of this
		}
	}
}
