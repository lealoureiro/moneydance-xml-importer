package com.lealoureiro.moneydance.myexpensescore;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * @author Leandro Loureiro
 */
public class MyExpensesExporterLibrary {


    private static final Logger LOGGER = LogManager.getLogger(MyExpensesExporterLibrary.class);

    private final Cluster cluster;
    private Session session;
    private final String keySpace;

    private BoundStatement insertAccountDataStmtBoundStmt;
    private BoundStatement insertUserByAccountBoundStmt;
    private BoundStatement insertAccountsByUserBoundStmt;

    private BoundStatement insertTransactionDataBoundStatement;
    private BoundStatement addCategoryBoundStmt;
    private BoundStatement addSubCategoryBoundStmt;

    private BoundStatement insertTransactionsByTagBoundStmt;

    public MyExpensesExporterLibrary(final String address, final String keySpace) {
        cluster = Cluster.builder().addContactPoint(address).build();
        this.keySpace = keySpace;
    }

    public void showServerInformation() {
        final Metadata metadata = cluster.getMetadata();
        LOGGER.info(String.format("Connected to Cluster: %s", metadata.getClusterName()));

        for (final Host host : metadata.getAllHosts()) {
            LOGGER.info(String.format("Data Center %s | Host %s | Rack %s", host.getDatacenter(), host.getAddress(), host.getRack()));
        }
    }

    public void initStatements() throws Exception {

        if (session == null || session.isClosed()) {
            throw new Exception("Please connect to server first!");
        }

        final PreparedStatement insertAccountDataStmt = session.prepare(String.format("INSERT INTO %s.accounts (account_id,name,account_type,start_balance,currency,user_id) VALUES (?,?,?,?,?,?);", keySpace));
        insertAccountDataStmtBoundStmt = new BoundStatement(insertAccountDataStmt);

        final PreparedStatement insertUserByAccountStmt = session.prepare(String.format("INSERT INTO %s.user_by_account (account_id,user_id) VALUES (?,?);", keySpace));
        insertUserByAccountBoundStmt = new BoundStatement(insertUserByAccountStmt);

        final PreparedStatement insertAccountsByUserStmt = session.prepare(String.format("INSERT INTO %s.accounts_by_user (user_id,account_id) VALUES (?,?);", keySpace));
        insertAccountsByUserBoundStmt = new BoundStatement(insertAccountsByUserStmt);

        final PreparedStatement insertTransactionDataStmt = session.prepare(String.format("INSERT INTO %s.transactions (transaction_id,date,account_id,amount,description,category,sub_category,tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", keySpace));
        insertTransactionDataBoundStatement = new BoundStatement(insertTransactionDataStmt);

        final PreparedStatement addCategoryStmt = session.prepare(String.format("INSERT INTO %s.category (user_id, name) VALUES (?,?)", keySpace));
        addCategoryBoundStmt = new BoundStatement(addCategoryStmt);

        final PreparedStatement addSubCategoryStmt = session.prepare(String.format("INSERT INTO %s.sub_category (user_id, category_name, name) VALUES (?,?,?)", keySpace));
        addSubCategoryBoundStmt = new BoundStatement(addSubCategoryStmt);

        final PreparedStatement insertTransactionsByTagStmt = session.prepare(String.format("INSERT INTO %s.transactions_by_tag (tag,transaction_id) VALUES (?,?)", keySpace));
        insertTransactionsByTagBoundStmt = new BoundStatement(insertTransactionsByTagStmt);
    }

    public void connect() {
        if (session == null || session.isClosed()) {
            session = cluster.connect();
        }
    }

    public void addAccount(UUID accountId, UUID userId, String accountName, String accountType, long startBalance, String currency) {
        session.execute(insertAccountDataStmtBoundStmt.bind(accountId, accountName, accountType, startBalance, currency, userId));
        session.execute(insertUserByAccountBoundStmt.bind(accountId, userId));
        session.execute(insertAccountsByUserBoundStmt.bind(userId, accountId));
    }

    public void addTransaction(final UUID transactionId, final Date date, final UUID accountId, final long amount, final String description, final String category, final String subCategory, final Set<String> tags) {
        session.execute(insertTransactionDataBoundStatement.bind(transactionId, date, accountId, amount, description, category, subCategory, tags));
    }

    public void addCategory(final UUID userId, final String category) {
        session.execute(addCategoryBoundStmt.bind(userId, category));
    }

    public void addSubCategory(final UUID userId, final String category, final String subCategory) {
        session.execute(addSubCategoryBoundStmt.bind(userId, category, subCategory));
    }

    public void addTagToTransaction(final UUID transactionId, final String tag) {
        session.execute(insertTransactionsByTagBoundStmt.bind(tag, transactionId));
    }

    public void disconnect() {
        if (session != null && !session.isClosed()) {
            session.close();
        }

        if (cluster != null && !cluster.isClosed()) {
            cluster.close();
        }
    }
}
