package com.diouma.ebankservice.mcp;

import com.diouma.ebankservice.entities.AccountTransaction;
import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.service.EbankService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * The banking capabilities exposed to the AI agent over MCP.
 *
 * <p>This class is the boundary between a language model and real business
 * operations, so it validates before it acts. Arguments arrive from a model,
 * not from a program: they are plausible, not trustworthy.
 *
 * <p>Amounts arrive as JSON numbers (doubles on the wire) and are turned into
 * BigDecimal here, at the edge, with {@link BigDecimal#valueOf(double)} - the
 * conversion that reads the decimal the model wrote rather than the binary
 * approximation of it. Past this point money is only ever BigDecimal.
 */
@Component
public class BankMcpTools {

    private final EbankService ebankService;

    public BankMcpTools(EbankService ebankService) {
        this.ebankService = ebankService;
    }

    @McpTool(name = "createAccount",
            description = "Opens a bank account for an existing customer. "
                    + "Fails if the customer does not exist or the type is not one of the listed values.")
    public BankAccount createAccount(
            @McpToolParam(description = "Account type: CURRENT-ACCOUNT or SAVING-ACCOUNT", required = true)
            String type,
            @McpToolParam(description = "Opening balance, zero or more", required = true)
            double balance,
            @McpToolParam(description = "Id of the customer who owns the account", required = true)
            long customerId) {

        // Parsing happens here rather than in the service so an invalid value
        // never reaches the domain; the exception message lists the accepted
        // values and is returned to the model, which can then retry correctly.
        AccountType parsedType = AccountType.from(type);

        return ebankService.createAccount(parsedType, BigDecimal.valueOf(balance), customerId);
    }

    @McpTool(name = "deposit",
            description = "Pays money into an account. Fails if the account does not exist "
                    + "or the amount is not strictly positive.")
    public BankAccount deposit(
            @McpToolParam(description = "Id of the account to pay into", required = true)
            String accountId,
            @McpToolParam(description = "Amount to pay in, strictly positive", required = true)
            double amount) {

        return ebankService.deposit(accountId, BigDecimal.valueOf(amount));
    }

    @McpTool(name = "withdraw",
            description = "Takes money out of an account. Fails if the account does not exist, "
                    + "the amount is not strictly positive, or the balance is too low.")
    public BankAccount withdraw(
            @McpToolParam(description = "Id of the account to take from", required = true)
            String accountId,
            @McpToolParam(description = "Amount to take out, strictly positive", required = true)
            double amount) {

        return ebankService.withdraw(accountId, BigDecimal.valueOf(amount));
    }

    @McpTool(name = "transfer",
            description = "Moves money from one account to another in a single operation. "
                    + "Fails if either account does not exist, the amount is not strictly positive, "
                    + "the two accounts are the same, or the source balance is too low. "
                    + "Nothing is moved when it fails.")
    public String transfer(
            @McpToolParam(description = "Id of the account the money leaves", required = true)
            String fromAccountId,
            @McpToolParam(description = "Id of the account the money arrives in", required = true)
            String toAccountId,
            @McpToolParam(description = "Amount to move, strictly positive", required = true)
            double amount) {

        BigDecimal value = BigDecimal.valueOf(amount);
        ebankService.transfer(fromAccountId, toAccountId, value);
        return "Transferred " + value + " from " + fromAccountId + " to " + toAccountId
                + ". Source account balance is now " + ebankService.getBalance(fromAccountId) + ".";
    }

    @McpTool(name = "getBalance",
            description = "Returns the current balance of an account. Fails if it does not exist.")
    public BigDecimal getBalance(
            @McpToolParam(description = "Id of the account to read", required = true)
            String accountId) {

        return ebankService.getBalance(accountId);
    }

    @McpTool(name = "getStatement",
            description = "Returns an account's movements, most recent first. "
                    + "Fails if the account does not exist.")
    public List<AccountTransaction> getStatement(
            @McpToolParam(description = "Id of the account whose statement to read", required = true)
            String accountId) {

        return ebankService.getStatement(accountId);
    }
}
