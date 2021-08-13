package org.tenx.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseOperationsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String dbUrl = "jdbc:sqlite:10xBankTestDB";
    private final DatabaseOperations dbOps = new DatabaseOperations(dbUrl);

    @BeforeEach
    public void setup() throws SQLException {
        System.out.println("Running the setup method");
        dbOps.executeUnitStatement("DELETE FROM accounts");
        dbOps.executeUnitStatement("DELETE FROM transactions");
    }

    @AfterEach
    public void closeConnections() throws SQLException {
        dbOps.connection.close();
    }

    @Test
    public void test_can_create_account() throws IOException, SQLException {
        Account acc = new Account();
        acc.setId(1110111);
        acc.setBalance(10.10);
        acc.setCurrency("GBP");

        String expectedSuccessful = "Account " + acc.getId() + " created successfully";
        Assertions.assertEquals(expectedSuccessful, dbOps.createAccountEntry(acc));

    }

    @Test
    public void test_can_retrieve_account() throws IOException, SQLException {
        List<Account> accounts = createAndReturnAccounts();
        Account acc1 = accounts.get(0);
        String expectedAccountAcc1 = objectMapper.writeValueAsString(acc1);
        Assertions.assertEquals(expectedAccountAcc1, dbOps.getAccountDetails(acc1.getId()));

        String expectedAccounts = objectMapper.writeValueAsString(accounts);
        Assertions.assertEquals(expectedAccounts, dbOps.getAccountDetails());
    }

    @Test
    public void test_can_return_correct_response_on_creation() throws IOException, SQLException {
        Account acc = new Account();
        acc.setId(1110111);
        acc.setBalance(10.10);
        acc.setCurrency("GBP");

        String expectedSuccessful = "Account " + acc.getId() + " created successfully";
        Assertions.assertEquals(expectedSuccessful, dbOps.createAccountEntry(acc));

        String expectedAccountExists = "Account " + acc.getId() + " already exists.";
        Assertions.assertEquals(expectedAccountExists, dbOps.createAccountEntry(acc));
    }

    @Test
    public void can_handle_transaction() throws IOException, SQLException {
        createAndReturnAccounts();

        Transfer goodTransferShouldWork = new Transfer();
        goodTransferShouldWork.setSourceAccountId(1110111);
        goodTransferShouldWork.setDestinationAccountId(2220222);
        goodTransferShouldWork.setAmount(1.2);
        goodTransferShouldWork.setCurrency("GBP");

        String expectedSuccessfulFirstTransfer = "Transaction successful. Transaction ID [1]";
        String expectedSuccessfulSecondTransfer = "Transaction successful. Transaction ID [2]";
        Assertions.assertEquals(expectedSuccessfulFirstTransfer, dbOps.handleTransaction(goodTransferShouldWork));
        Assertions.assertEquals(expectedSuccessfulSecondTransfer, dbOps.handleTransaction(goodTransferShouldWork));
    }

    @Test
    public void can_retrieve_data_from_transactions_db() throws IOException, SQLException {
        createAndReturnAccounts();

        Transfer transfer = new Transfer();
        transfer.setSourceAccountId(1110111);
        transfer.setDestinationAccountId(2220222);
        transfer.setAmount(1.20);
        transfer.setCurrency("GBP");

        dbOps.handleTransaction(transfer);

        ResultSet rs = dbOps.executeStatement("select * from transactions");
        Transfer resultingTransfer = new Transfer();
        while(rs.next()) {
            resultingTransfer.setId(rs.getLong("transactionId"));
            resultingTransfer.setAmount(rs.getDouble("amount"));
            resultingTransfer.setSourceAccountId(rs.getLong("sourceId"));
            resultingTransfer.setDestinationAccountId(rs.getLong("destinationId"));
            resultingTransfer.setCreatedAt(rs.getString("createdAt"));
            resultingTransfer.setCurrency(rs.getString("currency"));
        }
        rs.close();

        Assertions.assertEquals(1, resultingTransfer.getId());
        Assertions.assertEquals(transfer.getSourceAccountId(), resultingTransfer.getSourceAccountId());
        Assertions.assertEquals(transfer.getDestinationAccountId(), resultingTransfer.getDestinationAccountId());
        Assertions.assertEquals(transfer.getAmount(), resultingTransfer.getAmount());
        Assertions.assertEquals(transfer.getCurrency(), resultingTransfer.getCurrency());
        Assertions.assertEquals(transfer.getCreatedAt(), resultingTransfer.getCreatedAt());

    }

    @Test
    public void should_handle_insufficient_balance() throws IOException, SQLException {
        createAndReturnAccounts();
        Transfer transferWithInsufficientBalance = new Transfer();
        transferWithInsufficientBalance.setSourceAccountId(1110111);
        transferWithInsufficientBalance.setDestinationAccountId(2220222);
        transferWithInsufficientBalance.setAmount(10000000);
        transferWithInsufficientBalance.setCurrency("GBP");

        String expectedFailure = "The source balance is insufficient for this transaction";
        Assertions.assertEquals(expectedFailure, dbOps.handleTransaction(transferWithInsufficientBalance));
    }

    @Test
    public void should_handle_same_account_transfer() throws IOException, SQLException {
        Transfer transferToSameAccount = new Transfer();
        transferToSameAccount.setSourceAccountId(1110111);
        transferToSameAccount.setDestinationAccountId(1110111);
        transferToSameAccount.setAmount(50);
        transferToSameAccount.setCurrency("GBP");

        String expectedFailureSameAccount = "Account ID's " + transferToSameAccount.getSourceAccountId() + " and " +
                transferToSameAccount.getDestinationAccountId() + " are the same. " +
                "Please correct either the source or destination account";
        Assertions.assertEquals(expectedFailureSameAccount, dbOps.handleTransaction(transferToSameAccount));
    }

    @Test
    public void should_handle_invalid_accounts() throws IOException, SQLException {
        createAndReturnAccounts();
        Transfer transferAccountDoesNotExist = new Transfer();
        transferAccountDoesNotExist.setSourceAccountId(11101112);
        transferAccountDoesNotExist.setDestinationAccountId(1110111);
        transferAccountDoesNotExist.setAmount(10000000);
        transferAccountDoesNotExist.setCurrency("GBP");

        String expectedFailureSourceAccountDoesNotExist = "Account ID " + transferAccountDoesNotExist.getSourceAccountId() +
                " not found. Please review the source account ID.";
        Assertions.assertEquals(expectedFailureSourceAccountDoesNotExist,
                dbOps.handleTransaction(transferAccountDoesNotExist));

        Transfer transferBothAccountsDontExist = new Transfer();
        transferBothAccountsDontExist.setSourceAccountId(11101112);
        transferBothAccountsDontExist.setDestinationAccountId(11101111);
        transferBothAccountsDontExist.setAmount(10000000);
        transferBothAccountsDontExist.setCurrency("GBP");

        String expectedWhenBothAccountsDontExist = "Account ID's " + transferBothAccountsDontExist.getDestinationAccountId() +
                " and " + transferBothAccountsDontExist.getSourceAccountId() + " not found. Please verify" +
                " both account ID's";
        Assertions.assertEquals(expectedWhenBothAccountsDontExist,
                dbOps.handleTransaction(transferBothAccountsDontExist));
    }

    @Test
    public void can_get_row_count() throws IOException, SQLException {
        final String transactionHistoryCountQueryFile = "src/main/resources/sql/TransactionHistoryCountQuery.sql";
        String transactionIdQuery = dbOps.getQueryFromFile(transactionHistoryCountQueryFile);

        Assertions.assertEquals(0, dbOps.getRowCount(transactionIdQuery));

        createAndReturnAccounts();
        Transfer transfer = new Transfer();
        transfer.setSourceAccountId(1110111);
        transfer.setDestinationAccountId(2220222);
        transfer.setAmount(10);
        transfer.setCurrency("GBP");
        dbOps.handleTransaction(transfer);

        Assertions.assertEquals(1, dbOps.getRowCount(transactionIdQuery));
    }

    @Test
    public void can_check_account_exists() throws IOException, SQLException {
        createAndReturnAccounts();
        Assertions.assertFalse(dbOps.isAccountExists(100151));
        Assertions.assertTrue(dbOps.isAccountExists(1110111));
    }

    public List<Account> createAndReturnAccounts() throws IOException, SQLException {
        List<Account> accounts = new ArrayList<>();

        Account acc1 = new Account();
        acc1.setId(1110111);
        acc1.setBalance(10.10);
        acc1.setCurrency("GBP");
        dbOps.createAccountEntry(acc1);

        Account acc2 = new Account();
        acc2.setId(2220222);
        acc2.setBalance(20.20);
        acc2.setCurrency("GBP");
        dbOps.createAccountEntry(acc2);

        accounts.add(acc1);
        accounts.add(acc2);

        return accounts;
    }



}
