package org.tenx.accounts;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.internal.matchers.Not;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class ControllerTest {

    private final String dbUrl = "jdbc:sqlite:10xBank";
    private final DatabaseOperations dbOps = new DatabaseOperations(dbUrl);
    @Autowired
    private MockMvc mvc;

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
    public void can_retrieve_account_status() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/getaccountstatus").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("[]")));

        mvc.perform(MockMvcRequestBuilders.post("/createaccount").content(
                "{\n" +
                "    \"id\": \"2220222\",\n" +
                "    \"balance\": 100000,\n" +
                "    \"currency\": \"GBP\"\n" +
                "}"
                ).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Account 2220222 created successfully")));

        mvc.perform(MockMvcRequestBuilders.get("/getaccountstatus").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("[{" +
                        "\"id\":2220222," +
                        "\"balance\":100000.0," +
                        "\"currency\":\"GBP\"," +
                        "\"createdAt\":")));
    }

    @Test
    public void can_create_and_transfer_between_accounts() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/createaccount").content(
                        "{\n" +
                        "    \"id\": \"2220222\",\n" +
                        "    \"balance\": 100000,\n" +
                        "    \"currency\": \"GBP\"\n" +
                        "}"
        ).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Account 2220222 created successfully")));

        mvc.perform(MockMvcRequestBuilders.post("/createaccount").content(
                        "{\n" +
                        "    \"id\": \"1110111\",\n" +
                        "    \"balance\": 0.20,\n" +
                        "    \"currency\": \"GBP\"\n" +
                        "}"
        ).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Account 1110111 created successfully")));


        mvc.perform(MockMvcRequestBuilders.post("/transfer").content(
                         "{\n" +
                         "   \"id\": \"1\",\n" +
                         "   \"sourceAccountId\" : 2220222,\n" +
                         "   \"destinationAccountId\" : 1110111,\n" +
                         "   \"amount\" : 10,\n" +
                         "   \"currency\" : \"GBP\"\n" +
                         "}"
        ).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Transaction successful. Transaction ID [1]")));

        mvc.perform(MockMvcRequestBuilders.get("/getaccountstatus/1110111").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("{" +
                        "\"id\":1110111," +
                        "\"balance\":10.2," +
                        "\"currency\":\"GBP\"," +
                        "\"createdAt\":")));
    }
}
