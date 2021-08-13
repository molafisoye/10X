package org.tenx.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;


public class DatabaseOperations {

    /* TODO
    refactor
    test
    readme
    exception handling
     */

    private final Logger logger = LoggerFactory.getLogger(DatabaseOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    Connection connection;

    private final String ROWCOUNT = "rowcount";
    private final String TRANSFER = "transfer";
    private final DateTimeFormatter dbTimeStampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DatabaseOperations(String dbUrl)  {
        logger.info("Initializing the 10X bank DB");
        try {
            setConnection(dbUrl);
            final String accountTableCreationFile = "src/main/resources/sql/AccountsTableCreation.sql";
            final String transactionHistoryTableCreationFile = "src/main/resources/sql/TransactionsHistoryTableCreation.sql";

            String accountsTableCreationQuery = getQueryFromFile(accountTableCreationFile);
            String transactionTableCreationQuery = getQueryFromFile(transactionHistoryTableCreationFile);
            executeUnitStatement(accountsTableCreationQuery);
            executeUnitStatement(transactionTableCreationQuery);
        } catch (Exception e) {
            logger.error("Error starting the DB. Please fix and restart program." + e.getMessage());
        }
    }

    public String getAccountDetails() throws IOException, SQLException {
        logger.info("Retrieving all account details");
        final String getAllAccountsQueryFile = "src/main/resources/sql/RetrieveAllAccountDetails.sql";
        String accountsQuery = getQueryFromFile(getAllAccountsQueryFile);
        ResultSet rs = executeStatement(accountsQuery);
        List<Account> accounts = getListOfAccountsFromRs(rs);
        return objectMapper.writeValueAsString(accounts);
    }

    public String getAccountDetails(long id) throws IOException, SQLException {
        logger.info("Retrieving account details for " + id);
        boolean accountExists = isAccountExists(id);

        if (accountExists) {
            ResultSet rs = getAccountResultSetFromDb(id);
            List<Account> accounts = getListOfAccountsFromRs(rs);
            return objectMapper.writeValueAsString(accounts.get(0));
        } else {
            return "Error the account with ID " + id + " does not exist.";
        }
    }

    public String createAccountEntry(Account account) throws IOException, SQLException {
        logger.info("Verifying the account " + account.getId() + " does not exist");

        if (!isAccountExists(account.getId())) {
            logger.info("Account " + account.getId() + " does not exist. It will be created");
            executeAccountEntryStatement(account);
            return "Account " + account.getId() + " created successfully";
        } else {
            return "Account " + account.getId() + " already exists.";
        }
    }

    public String handleTransaction(Transfer transactionDetails) throws IOException, SQLException {
        long sourceAccountId = transactionDetails.getSourceAccountId();
        long destinationAccountId = transactionDetails.getDestinationAccountId();
        double amount = transactionDetails.getAmount();

        ResultSet sourceRs = getAccountResultSetFromDb(sourceAccountId);
        ResultSet destinationRs = getAccountResultSetFromDb(destinationAccountId);

        boolean sourceAccExists = isAccountExists(sourceAccountId);
        boolean destinationAccExists = isAccountExists(destinationAccountId);
        boolean sourceDestinationAccountExist = sourceAccExists && destinationAccExists;
        boolean sourceAndDestinationDiffer = sourceAccountId != destinationAccountId;

        logger.info("Source and destination account exist - " + sourceDestinationAccountExist);

        if (sourceDestinationAccountExist && sourceAndDestinationDiffer) {

            Account sourceAccount = getListOfAccountsFromRs(sourceRs).get(0);
            Account destinationAccount = getListOfAccountsFromRs(destinationRs).get(0);

            BigDecimal newSourceBalance = BigDecimal.valueOf(sourceAccount.getBalance()).
                    subtract(BigDecimal.valueOf(amount));

            if (newSourceBalance.signum() >= 0) {
                logger.info("Balance is sufficient. Processing transaction.");

                String transactionTime = transactionDetails.getCreatedAt();

                updateAccountDetailsInDb(sourceAccountId, newSourceBalance);

                BigDecimal newDestinationBalance = BigDecimal.valueOf(destinationAccount.getBalance()).
                        add(BigDecimal.valueOf(amount));

                updateAccountDetailsInDb(destinationAccountId, newDestinationBalance);

                long transactionId = getTransactionId();

                updateTransactionsHistoryDb(sourceAccountId, destinationAccountId, amount, transactionTime,
                        transactionId, transactionDetails.getCurrency());

                logger.info("Transaction ID [" + transactionId + "]" + " complete.");
                return "Transaction successful. Transaction ID [" + transactionId + "]";
            } else {
                logger.error("insufficient source balance.");
                return "The source balance is insufficient for this transaction";
            }
        } else {
            logger.info("Transaction failed retrieving error message");
            return getAccountErrorString(sourceAccountId, destinationAccountId, sourceAccExists,
                    destinationAccExists, sourceAndDestinationDiffer);
        }
    }

    private String getAccountErrorString(long sourceAccountId, long destinationAccountId,
                                         boolean sourceAccExists, boolean destinationAccExists,
                                         boolean sourceAndDestinationDiffer) {
        if (!sourceAndDestinationDiffer) {
            return "Account ID's " + destinationAccountId + " and " + sourceAccountId + " are the same. " +
                    "Please correct either the source or destination account";
        } else if (sourceAccExists) {
            return "Account ID " + destinationAccountId + " not found. Please review the destination account ID.";
        } else if (destinationAccExists) {
            return "Account ID " + sourceAccountId + " not found. Please review the source account ID.";
        } else {
            return "Account ID's " + destinationAccountId + " and " + sourceAccountId + " not found. Please verify" +
                    " both account ID's";
        }
    }

    public String clearTables(String tableName) {
        logger.info("Attempting to delete " + tableName);
        try {
            if (tableName.equalsIgnoreCase("accounts")) {
                executeUnitStatement("DELETE FROM accounts");
                return "Accounts table cleared";
            } else if (tableName.equalsIgnoreCase("transactions")) {
                executeUnitStatement("DELETE FROM transactions");
                return "Transactions table cleared";
            } else {
                return "Invalid table name. Table name either [transactions] or [accounts]";
            }
        } catch (SQLException e) {
            logger.error("clearTable failed with exception " + e.getMessage());
            return "Failed to clear table " + e.getMessage();
        }
    }

    private void updateTransactionsHistoryDb(long sourceAccountId, long destinationAccountId, double amount,
                                             String transactionTime, long transactionId, String currency)
                                             throws IOException, SQLException {
        final String transactionUpdateQueryFile = "src/main/resources/sql/UpdateTransactionHistoryQuery.sql";
        String transactionsHistoryUpdateQuery = getQueryFromFile(transactionUpdateQueryFile);
        logger.info("Updating the transactions history table");
        PreparedStatement transactionHistoryStatement = connection.prepareStatement(transactionsHistoryUpdateQuery);
        transactionHistoryStatement.setLong(1, transactionId);
        transactionHistoryStatement.setDouble(2, amount);
        transactionHistoryStatement.setString(3, TRANSFER);
        transactionHistoryStatement.setLong(4, sourceAccountId);
        transactionHistoryStatement.setLong(5, destinationAccountId);
        transactionHistoryStatement.setString(6, currency);
        transactionHistoryStatement.setString(7, transactionTime);
        transactionHistoryStatement.execute();
        transactionHistoryStatement.close();
    }

    private void updateAccountDetailsInDb(long sourceAccountId, BigDecimal newSourceBalance) throws SQLException, IOException {
        final String accountUpdateQueryFile = "src/main/resources/sql/UpdateAccountEntry.sql";
        String accountUpdateQuery = getQueryFromFile(accountUpdateQueryFile);
        PreparedStatement sourceUpdateStatement = connection.prepareStatement(accountUpdateQuery);
        sourceUpdateStatement.setBigDecimal(1, newSourceBalance);
        sourceUpdateStatement.setLong(2, sourceAccountId);
        sourceUpdateStatement.execute();
        sourceUpdateStatement.close();
    }

    public boolean isAccountExists(long id) throws SQLException, IOException {
        final String tableCountQueryFile = "src/main/resources/sql/TableCountQuery.sql";
        String query = getQueryFromFile(tableCountQueryFile) + "'" + id + "'";
        return getRowCount(query) == 1;
    }

    private List<Account> getListOfAccountsFromRs(ResultSet rs) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        while (rs.next()) {
            Account account = new Account();
            account.setId(rs.getLong("id"));
            account.setBalance(rs.getDouble("balance"));
            account.setCurrency(rs.getString("currency"));
            account.setCreatedAt(rs.getString("createdAt"));
            accounts.add(account);
        }
        rs.close();
        return accounts;
    }

    public long getTransactionId() throws IOException, SQLException {
        final String transactionHistoryCountQueryFile = "src/main/resources/sql/TransactionHistoryCountQuery.sql";
        String transactionIdQuery = getQueryFromFile(transactionHistoryCountQueryFile);
        return getRowCount(transactionIdQuery) + 1;
    }

    public long getRowCount(String query) throws SQLException {
        ResultSet rs = executeStatement(query);
        rs.next();
        long rowCount = rs.getLong(ROWCOUNT);
        rs.close();
        return rowCount;
    }

    private ResultSet getAccountResultSetFromDb(long id) throws IOException, SQLException {
        final String accountRetrievalQueryFile = "src/main/resources/sql/AccountRetrieval.sql";
        String accountRetrievalQuery = getQueryFromFile(accountRetrievalQueryFile);
        PreparedStatement accountRetrievalStatement = createPreparedStatement(accountRetrievalQuery);
        accountRetrievalStatement.setLong(1, id);
        return accountRetrievalStatement.executeQuery();
    }

    private void executeAccountEntryStatement(Account account) throws SQLException, IOException {
        final String accountEntryCreationFile = "src/main/resources/sql/AddAccountEntry.sql";
        String accountEntryQuery = getQueryFromFile(accountEntryCreationFile);
        PreparedStatement accEntryStatement = createPreparedStatement(accountEntryQuery);
        accEntryStatement.setLong(1, account.getId());
        accEntryStatement.setDouble(2, account.getBalance());
        accEntryStatement.setString(3, account.getCurrency());
        accEntryStatement.setString(4, LocalDateTime.now().format(dbTimeStampFormat));
        accEntryStatement.execute();
        accEntryStatement.close();
    }

    public PreparedStatement createPreparedStatement(String query) throws SQLException {
        return connection.prepareStatement(query);
    }

    public void executeUnitStatement(String query) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(query);
        statement.close();
    }

    public ResultSet executeStatement(String query) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(query);
    }

    public String getQueryFromFile(String fileUri) throws IOException {
        return Files.readString(new File(fileUri).toPath());
    }

    public void setConnection(String dbUrl) throws SQLException {
        connection = DriverManager.getConnection(dbUrl);
    }
}
