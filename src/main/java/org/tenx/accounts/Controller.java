package org.tenx.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@SpringBootApplication
public class Controller {

    DatabaseOperations dbOps = new DatabaseOperations("jdbc:sqlite:10xBank");
    ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/getaccountstatus")
    public String getAccountStatus() throws IOException, SQLException {
        return dbOps.getAccountDetails();
    }

    @GetMapping("/getaccountstatus/{id}")
    public String getAccountStatus(@PathVariable long id) throws IOException, SQLException {
        return dbOps.getAccountDetails(id);
    }

    @PostMapping("/createaccount")
    public String createAccount(@RequestBody String body) throws IOException, SQLException {
        Account accountCreationRequest = objectMapper.readValue(body, Account.class);
        return dbOps.createAccountEntry(accountCreationRequest);
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody String body) throws IOException, SQLException {
        Transfer transfer = objectMapper.readValue(body, Transfer.class);
        return dbOps.handleTransaction(transfer);
    }

    @GetMapping("/clearalldata/{tablename}")
    public String clearAllData(@PathVariable String tablename) {
        return dbOps.clearTables(tablename);
    }

    @GetMapping("/")
    public String onError() {
        return "Would you like to make some money? Read the README for more info!";
    }
}
