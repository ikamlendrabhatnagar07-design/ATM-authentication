package com.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Scanner;

public class ATMApplication {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("================================");
        System.out.println("       JAVA ATM SYSTEM");
        System.out.println("================================");

        try (Connection connection = Database.getConnection()) {
            Account account = login(connection);
            if (account == null) {
                System.out.println("Too many failed attempts. Exiting.");
                return;
            }
            showMenu(connection, account);
        } catch (SQLException e) {
            System.out.println("Could not connect to MySQL.");
            System.out.println("Check that MySQL is running and your environment variables are correct.");
            System.out.println("Details: " + e.getMessage());
        }
        System.out.println("Thank you for using the ATM.");
    }

    private static Account login(Connection connection) throws SQLException {
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Enter account number: ");
            String accountNumber = scanner.nextLine().trim();
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine().trim();

            String sql = "SELECT account_number, customer_name, balance FROM accounts " +
                         "WHERE account_number = ? AND pin = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, accountNumber);
                statement.setString(2, pin);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        System.out.println("\\nWelcome, " + result.getString("customer_name") + "!");
                        return new Account(
                            result.getString("account_number"),
                            result.getString("customer_name")
                        );
                    }
                }
            }
            System.out.println("Invalid account number or PIN. Attempts left: " + (3 - attempt));
        }
        return null;
    }

    private static void showMenu(Connection connection, Account account) throws SQLException {
        boolean running = true;
        while (running) {
            System.out.println("\\n----------- ATM MENU -----------");
            System.out.println("1. Check balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transaction history");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> checkBalance(connection, account.accountNumber());
                case "2" -> makeTransaction(connection, account.accountNumber(), true);
                case "3" -> makeTransaction(connection, account.accountNumber(), false);
                case "4" -> showHistory(connection, account.accountNumber());
                case "5" -> running = false;
                default -> System.out.println("Invalid choice. Please select 1 to 5.");
            }
        }
    }

    private static void checkBalance(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    System.out.println("Available balance: ₹" + result.getBigDecimal("balance"));
                } else {
                    System.out.println("Account not found.");
                }
            }
        }
    }

    private static void makeTransaction(Connection connection, String accountNumber,
                                        boolean deposit) throws SQLException {
        String label = deposit ? "deposit" : "withdraw";
        System.out.print("Enter amount to " + label + ": ₹");
        BigDecimal amount;
        try {
            amount = new BigDecimal(scanner.nextLine().trim()).setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            System.out.println("Invalid amount. Enter a valid amount with at most 2 decimal places.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Amount must be greater than zero.");
            return;
        }

        connection.setAutoCommit(false);
        try {
            String lockSql = "SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE";
            BigDecimal currentBalance;
            try (PreparedStatement lockStatement = connection.prepareStatement(lockSql)) {
                lockStatement.setString(1, accountNumber);
                try (ResultSet result = lockStatement.executeQuery()) {
                    if (!result.next()) {
                        System.out.println("Account not found.");
                        connection.rollback();
                        return;
                    }
                    currentBalance = result.getBigDecimal("balance");
                }
            }

            if (!deposit && currentBalance.compareTo(amount) < 0) {
                System.out.println("Insufficient funds. Your balance is ₹" + currentBalance);
                connection.rollback();
                return;
            }

            BigDecimal newBalance = deposit
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE accounts SET balance = ? WHERE account_number = ?")) {
                update.setBigDecimal(1, newBalance);
                update.setString(2, accountNumber);
                update.executeUpdate();
            }

            String type = deposit ? "DEPOSIT" : "WITHDRAWAL";
            try (PreparedStatement history = connection.prepareStatement(
                    "INSERT INTO transaction_history " +
                    "(account_number, transaction_type, amount, balance_after) VALUES (?, ?, ?, ?)")) {
                history.setString(1, accountNumber);
                history.setString(2, type);
                history.setBigDecimal(3, amount);
                history.setBigDecimal(4, newBalance);
                history.executeUpdate();
            }

            connection.commit();
            System.out.println((deposit ? "Deposit" : "Withdrawal") + " successful.");
            System.out.println("Updated balance: ₹" + newBalance);
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void showHistory(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT transaction_type, amount, balance_after, transaction_time " +
                     "FROM transaction_history WHERE account_number = ? " +
                     "ORDER BY transaction_time DESC, transaction_id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet result = statement.executeQuery()) {
                System.out.println("\\n------ TRANSACTION HISTORY ------");
                boolean found = false;
                while (result.next()) {
                    found = true;
                    System.out.printf("%s | ₹%s | Balance: ₹%s | %s%n",
                        result.getString("transaction_type"),
                        result.getBigDecimal("amount"),
                        result.getBigDecimal("balance_after"),
                        result.getTimestamp("transaction_time"));
                }
                if (!found) {
                    System.out.println("No transactions yet.");
                }
            }
        }
    }

    private record Account(String accountNumber, String customerName) { }
}
