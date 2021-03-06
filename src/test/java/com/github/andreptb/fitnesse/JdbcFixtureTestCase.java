package com.github.andreptb.fitnesse;

import java.util.ArrayList;
import java.util.Collection;

import org.hsqldb.jdbc.JDBCDriver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import com.github.andreptb.fitnesse.JdbcFixture;

/**
 * Jdbc Fixture unit testing
 */

public class JdbcFixtureTestCase {

	private static final String DB_URL_PREFIX = "jdbc:hsqldb:mem:";
	private static final String DB_DRIVER_CLASS = JDBCDriver.class.getName();
	private static final String DB_USERNAME = "sa";
	private static final String DB_PASSWORD = "";

	private Collection<EmbeddedDatabase> createdDatabases;

	@Before
	public void initCreatedDatabases() {
		this.createdDatabases = new ArrayList<>();
	}

	@Test
	public void testCreateTableInsertUserAndSelectInMultipleDatabases() throws ReflectiveOperationException {
		String[] dbs = new String[]{ "testdb1", "testdb2", "testdb3" };
		JdbcFixture fixture = new JdbcFixture();
		for(String db : dbs) {
			createTestDatabase(db, fixture);
			String userNameToInsert = "user" + db;
			String passwordToInsert = "password" + db;
			fixture.runInTheSql(db, String.format("INSERT INTO USER (NAME, PASSWORD) VALUES ('%s', '%s')", userNameToInsert, passwordToInsert));
			String result = fixture.queryInWithSql(db, String.format("SELECT PASSWORD FROM USER WHERE NAME = '%s'", userNameToInsert));
			Assert.assertEquals("SQL result", passwordToInsert, result);
		}
	}

	@Test
	public void testSelectWithNoResult() throws ReflectiveOperationException {
		String db = "testdb4";
		JdbcFixture fixture = new JdbcFixture();
		createTestDatabase(db, fixture);
		String result = fixture.queryInWithSql(db, "SELECT PASSWORD FROM USER");
		Assert.assertNull("SQL Result", result);
	}

	@Test
	public void testSuccessfullUpdate() throws ReflectiveOperationException {
		String db = "testdb5";
		JdbcFixture fixture = new JdbcFixture();
		createTestDatabase(db, fixture);
		String userNameToInsert = "user1";
		String passwordToInsert = "password1";
		fixture.runInTheSql(db, String.format("INSERT INTO USER (NAME, PASSWORD) VALUES ('%s', '%s')", userNameToInsert, passwordToInsert));
		Assert.assertEquals("SQL result before update", passwordToInsert, fixture.queryInWithSql(db, String.format("SELECT PASSWORD FROM USER WHERE NAME = '%s'", userNameToInsert)));
		String passwordToUpdate = "password2";
		Assert.assertEquals("Update result", "1", fixture.queryInWithSql(db, String.format("UPDATE USER SET PASSWORD='%s' WHERE NAME = '%s'", passwordToUpdate, userNameToInsert)));
		Assert.assertEquals("SQL result after update", passwordToUpdate, fixture.queryInWithSql(db, String.format("SELECT PASSWORD FROM USER WHERE NAME = '%s'", userNameToInsert)));
	}

	@Test
	public void testUpdateThatDoesntAffectAnyRows() throws ReflectiveOperationException {
		String db = "testdb5";
		JdbcFixture fixture = new JdbcFixture();
		createTestDatabase(db, fixture);
		Assert.assertEquals("Update result", "0", fixture.queryInWithSql(db, "UPDATE USER SET PASSWORD='passwordx' WHERE NAME = 'userx'"));
	}

	@Test
	public void testCommandInNonRegisteredDatabase() throws ReflectiveOperationException {
		String[] dbs = new String[]{ "testdb6", "testdb7", "testdb8" };
		JdbcFixture fixture = new JdbcFixture();
		for(String db : dbs) {
			createTestDatabase(db, fixture);
		}
		try {
			fixture.queryInWithSql("testdbx", "SELECT 1");
			Assert.fail("should throw IllegalArgumentException");
		} catch(IllegalArgumentException e) {
			Assert.assertEquals("Wrong exception message", "No database registered for name 'testdbx'. Registered databases: [testdb6, testdb7, testdb8]", e.getMessage());
		}
	}


	private void createTestDatabase(String databaseName, JdbcFixture fixture) throws ReflectiveOperationException {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).setName(databaseName);
		String url = JdbcFixtureTestCase.DB_URL_PREFIX + databaseName;
		fixture.connectJdbcOnWithUrlAndDriverAndUsernameAndPassword(databaseName, url, JdbcFixtureTestCase.DB_DRIVER_CLASS, JdbcFixtureTestCase.DB_USERNAME, JdbcFixtureTestCase.DB_PASSWORD);
		fixture.runInTheSql(databaseName, "CREATE TABLE USER (ID BIGINT IDENTITY, NAME VARCHAR(255) NOT NULL, PASSWORD VARCHAR(255) NOT NULL)");
		this.createdDatabases.add(builder.build());
	}

	@After
	public void shutdownCreatedDatabases() {
		for(EmbeddedDatabase db : this.createdDatabases) {
			db.shutdown();
		}
	}

}
