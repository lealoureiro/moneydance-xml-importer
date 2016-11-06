package com.lealoureiro.moneydance;


import com.lealoureiro.moneydance.model.Account;
import com.lealoureiro.moneydance.model.Category;
import com.lealoureiro.moneydance.model.Transaction;
import com.lealoureiro.moneydance.myexpensescore.MyExpensesExporterLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class App {

    private static final Logger LOGGER = LogManager.getLogger(App.class);

    private static final String CASSANDRA_CONTACT_POINT = "127.0.0.1";

    public static void main(String[] args) {

        LOGGER.info("Moneydance XML importer");

        try {

            final File fXmlFile = new File(args[0]);
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(fXmlFile);
            LOGGER.info(String.format("Root element : %s", doc.getDocumentElement().getNodeName()));

            final Map<String, Account> accounts = new HashMap<>();
            final Map<String, Account> accountsMap = new HashMap<>();
            final Map<String, Category> categories = new HashMap<>();
            importBankAccounts(doc, accounts, categories);

            LOGGER.info("Accounts found:");
            for (final Map.Entry<String, Account> account : accounts.entrySet()) {
                accountsMap.put(account.getValue().getId(), account.getValue());
                LOGGER.info(String.format("MONEYDANCE ID: %10s | Account Name: %25s | ID: %s | Start Balance: %10d ", account.getKey(), account.getValue().getName(), account.getValue().getId(), account.getValue().getStartBalanceInCents()));
            }

            final List<Transaction> transactions = importTransactions(doc, accounts, categories);
            LOGGER.info(String.format("Transactions found: %d", transactions.size()));

            for (final Transaction transaction : transactions) {
                accountsMap.get(transaction.getAccountId()).addBalance(transaction.getAmount());
            }

            LOGGER.info("Accounts Imported:");
            for (final Map.Entry<String, Account> account : accountsMap.entrySet()) {
                LOGGER.info(String.format("Account Name: %25s | ID: %s | Balance: %10d ", account.getValue().getName(), account.getValue().getId(), account.getValue().getStartBalanceInCents() + account.getValue().getBalance()));
            }

            final Scanner scanner = new Scanner(System.in);
            System.out.print("Enter user uuid: ");
            final String userId = scanner.nextLine().trim();

            LOGGER.info(String.format("Filled user id: [%s]", userId));

            if (!isValidUUID(userId)) {
                LOGGER.fatal("Invalid UUID!");
                return;
            }

            LOGGER.info("Connecting to Cassandra Database");
            final MyExpensesExporterLibrary myExpensesExporterLibrary = new MyExpensesExporterLibrary(CASSANDRA_CONTACT_POINT, args[1]);
            myExpensesExporterLibrary.connect();
            myExpensesExporterLibrary.showServerInformation();
            myExpensesExporterLibrary.initStatements();

            LOGGER.info("Importing accounts...");
            int i = 1;
            for (final Account account : accountsMap.values()) {
                myExpensesExporterLibrary.addAccount(UUID.fromString(account.getId()), UUID.fromString(userId), account.getName(), "Bank", account.getStartBalanceInCents(), account.getCurrency());
                System.out.print(String.format("\r%d imported of %d", i, accounts.size()));
                i++;
            }
            System.out.println();

            LOGGER.info("Importing categories...");
            i = 1;
            for (final Category category : categories.values()) {
                if (category.getParentId() != null) {
                    final String categoryName = categories.get(category.getParentId()).getName();
                    myExpensesExporterLibrary.addSubCategory(UUID.fromString(userId), categoryName, category.getName());
                } else {
                    myExpensesExporterLibrary.addCategory(UUID.fromString(userId), category.getName());
                }
                System.out.print(String.format("\r%d imported of %d", i, categories.size()));
                i++;
            }
            System.out.println();

            LOGGER.info("Importing transactions...");
            i = 1;
            for (final Transaction transaction : transactions) {

                myExpensesExporterLibrary.addTransaction(UUID.fromString(transaction.getId()), transaction.getDate(), UUID.fromString(transaction.getAccountId()), transaction.getAmount(), transaction.getDescription(), transaction.getId(), transaction.getCategory(), transaction.getSubCategory());
                for (final String tag : transaction.getTags()) {
                    if (tag != null && !"".equals(tag)) {
                        myExpensesExporterLibrary.addTagToTransaction(UUID.fromString(transaction.getId()), tag);
                    }
                }
                System.out.print(String.format("\r%d imported of %d", i, transactions.size()));
                i++;
            }
            System.out.println();

            myExpensesExporterLibrary.disconnect();

        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private static void importBankAccounts(final Document doc, final Map<String, Account> accounts, final Map<String, Category> categories) {

        final NodeList nodeList = doc.getElementsByTagName("ACCOUNT");
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                final Element element = (Element) node;
                final String accountType = element.getElementsByTagName("TYPE").item(0).getTextContent();

                if ("B".equals(accountType)) {
                    final String accountName = element.getElementsByTagName("NAME").item(0).getTextContent();
                    final String startBalanceText = element.getElementsByTagName("STARTBAL").item(0).getTextContent();
                    long startBalance = (long) (Double.parseDouble(startBalanceText) * 100.0);
                    final Account account = new Account(accountName, startBalance, "EUR");

                    final Map<String, String> attributesMap = new HashMap<>();
                    final Node accountParams = element.getElementsByTagName("ACCTPARAMS").item(0);

                    if (accountParams.getNodeType() == Node.ELEMENT_NODE) {
                        final Element element2 = (Element) accountParams;
                        final NodeList attributes = element2.getElementsByTagName("PARAM");
                        for (int j = 0; j < attributes.getLength(); j++) {
                            final String key = element.getElementsByTagName("KEY").item(j).getTextContent();
                            final String value = element.getElementsByTagName("VAL").item(j).getTextContent();
                            attributesMap.put(key, value);
                        }
                    }

                    accounts.put(attributesMap.get("id"), account);
                } else if ("E".equals(accountType) || "I".equals(accountType)) {
                    final String categoryName = element.getElementsByTagName("NAME").item(0).getTextContent();
                    final Map<String, String> attributesMap = new HashMap<>();
                    final Node accountParams = element.getElementsByTagName("ACCTPARAMS").item(0);

                    if (accountParams.getNodeType() == Node.ELEMENT_NODE) {
                        final Element element2 = (Element) accountParams;
                        final NodeList attributes = element2.getElementsByTagName("PARAM");
                        for (int j = 0; j < attributes.getLength(); j++) {
                            final String key = element.getElementsByTagName("KEY").item(j).getTextContent();
                            final String value = element.getElementsByTagName("VAL").item(j).getTextContent();
                            attributesMap.put(key, value);
                        }
                        final Category category = new Category(attributesMap.get("id"), categoryName, attributesMap.get("parentid"));
                        categories.put(category.getId(), category);
                    }
                }
            }
        }

        for (final Category category : categories.values()) {
            if (!categories.containsKey(category.getParentId())) {
                category.setParentId(null);
            }
        }
    }

    private static List<Transaction> importTransactions(final Document doc, final Map<String, Account> accounts, final Map<String, Category> categories) {

        final List<Transaction> transactions = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy.MM.dd");

        final NodeList nodeList = doc.getElementsByTagName("PTXN");

        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                final Element element = (Element) node;
                final String description = element.getElementsByTagName("DESC").item(0).getTextContent();
                final String date = element.getElementsByTagName("DATE").item(0).getTextContent();

                final Map<String, String> attributesMap = new HashMap<>();
                final Node baseTransaction = element.getElementsByTagName("TAGS").item(0);
                if (baseTransaction.getNodeType() == Node.ELEMENT_NODE) {
                    final Element element2 = (Element) baseTransaction;
                    final NodeList attributes = element2.getElementsByTagName("TAG");
                    for (int j = 0; j < attributes.getLength(); j++) {
                        final String key = element2.getElementsByTagName("KEY").item(j).getTextContent();
                        final String value = element2.getElementsByTagName("VAL").item(j).getTextContent();
                        attributesMap.put(key, value);
                    }
                }

                final Account account = accounts.get(attributesMap.get("acctid"));
                final long transactionAmount = Long.parseLong(attributesMap.get("0.pamt"));
                final DateTime dt = formatter.parseDateTime(date);
                final Category category = categories.get(attributesMap.get("0.acctid"));
                String categoryName;
                String subCategoryName = "";

                if (category.getParentId() != null) {
                    categoryName = categories.get(category.getParentId()).getName();
                    subCategoryName = category.getName();
                } else {
                    categoryName = category.getName();
                }
                final Transaction transaction = new Transaction(account.getId(), description, transactionAmount, dt.toDate(), categoryName, subCategoryName);
                transaction.getTags().add(attributesMap.get("0.tags"));
                transactions.add(transaction);
            }
        }

        return transactions;
    }

    private static boolean isValidUUID(final String uuid) {

        try {
            UUID.fromString(uuid);
            return true;
        } catch (final IllegalArgumentException e) {
            LOGGER.warn(e.getMessage());
            return false;
        }
    }

}
