package com.diouma.ebankservice.mcp;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.service.EbankService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The banking capabilities exposed to the AI agent over MCP.
 *
 * <p>This class is the boundary between a language model and real business
 * operations, so it validates before it acts. Arguments arrive from a model,
 * not from a program: they are plausible, not trustworthy.
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

        return ebankService.createAccount(parsedType, balance, customerId);
    }
}
