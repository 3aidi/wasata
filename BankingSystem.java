package com.lab;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BankingSystem {

    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = getDatabaseConnection();
            if (conn != null) {
                // DDL first (MySQL auto-commits DDL anyway)
                createDatabase(conn);
                try (Statement useStmt = conn.createStatement()) {
                    useStmt.executeUpdate("USE BankDB");
                }
                createTables(conn);

                // Switch to manual transaction control for DML operations
                conn.setAutoCommit(false);

                // Create two customers and their accounts
                Customer customer1 = new Customer(0, "John Doe", "123 Main St", 500.00);
                createCustomerAccount(conn, customer1, customer1.getBalance());

                Customer customer2 = new Customer(0, "Jane Smith", "456 Oak St", 1000.00);
                createCustomerAccount(conn, customer2, customer2.getBalance());

                // Update customer 1's address
                customer1.setAddress("456 New Address");
                updateCustomerDetails(conn, customer1);

                // View all customers + accounts
                System.out.println("---- All customers ----");
                viewAllCustomers(conn);

                // Delete account id 1 (belongs to John Doe)
                deleteCustomerAccount(conn, 1);

                // View again to confirm the deletion
                System.out.println("---- After deletion ----");
                viewAllCustomers(conn);
            }
        } catch (SQLException e) {
            System.err.println("Main SQLException: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                    System.out.println("Auto-commit restored and connection closed.");
                } catch (SQLException e) {
                    System.err.println("Error closing the database connection: " + e.getMessage());
                }
            }
        }
    }

    // Task 1: Create a new customer and their account
    public static void createCustomerAccount(Connection conn, Customer customer, double initialBalance) {
        String customerInsertSQL = "INSERT INTO customers (name, address) VALUES (?, ?)";
        String accountInsertSQL  = "INSERT INTO accounts (customer_id, balance) VALUES (?, ?)";

        try (PreparedStatement customerStmt = conn.prepareStatement(
                customerInsertSQL, Statement.RETURN_GENERATED_KEYS)) {

            // TODO 1: insert the customer into the customers table
            customerStmt.setString(1, customer.getName());
            customerStmt.setString(2, customer.getAddress());
            customerStmt.executeUpdate();

            // Retrieve the auto-generated customer id
            int customerId;
            try (ResultSet generatedKeys = customerStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customerId = generatedKeys.getInt(1);
                    customer.setId(customerId);
                } else {
                    throw new SQLException("Creating customer failed, no ID obtained.");
                }
            }

            // TODO 2: insert new account into accounts table
            try (PreparedStatement accountStmt = conn.prepareStatement(accountInsertSQL)) {
                accountStmt.setInt(1, customerId);
                accountStmt.setDouble(2, initialBalance);
                accountStmt.executeUpdate();
            }

            // Commit the transaction
            conn.commit();
            System.out.println("Account created for " + customer.getName() + " successfully.");

        } catch (SQLException e) {
            // TODO 3: if any error occurs, rollback the transaction
            try {
                conn.rollback();
                System.err.println("Transaction rolled back: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
        }
    }

    // Task 2: Update customer details
    public static void updateCustomerDetails(Connection conn, Customer customer) {
        String updateSQL = "UPDATE customers SET address = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
            stmt.setString(1, customer.getAddress());
            stmt.setInt(2, customer.getId());

            int rowsAffected = stmt.executeUpdate();
            conn.commit();

            if (rowsAffected > 0) {
                System.out.println(customer.getName() + " details updated successfully.");
            } else {
                System.out.println("Customer wasn't found.");
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            System.err.println("SQLException during update: " + e.getMessage());
        }
    }

    // Task 3: Delete a customer account
    public static void deleteCustomerAccount(Connection conn, int accountId) {
        String deleteAccountSQL  = "DELETE FROM accounts WHERE id = ?";
        String deleteCustomerSQL = "DELETE FROM customers WHERE id = ?";

        try {
            int customerId = getCustomerIdFromAccountId(conn, accountId);
            if (customerId == -1) {
                System.out.println("Account " + accountId + " not found.");
                return;
            }

            // Delete the account
            try (PreparedStatement accountStmt = conn.prepareStatement(deleteAccountSQL)) {
                accountStmt.setInt(1, accountId);
                accountStmt.executeUpdate();
            }

            // If the customer no longer has any accounts, remove them too
            if (!hasOtherAccounts(conn, customerId)) {
                try (PreparedStatement customerStmt = conn.prepareStatement(deleteCustomerSQL)) {
                    customerStmt.setInt(1, customerId);
                    customerStmt.executeUpdate();
                }
                System.out.println("Customer " + customerId + " also removed (no remaining accounts).");
            }

            conn.commit();
            System.out.println("Bank account belonging to customer " + customerId + " deleted successfully.");

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            System.err.println("SQLException during deletion: " + e.getMessage());
        }
    }

    // Task 4: View all customers and their account details
    public static void viewAllCustomers(Connection conn) {
        String query = "SELECT c.name, c.address, a.id AS account_id, a.balance "
                     + "FROM customers c JOIN accounts a ON c.id = a.customer_id";

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name    = rs.getString("name");
                String address = rs.getString("address");
                int accountId  = rs.getInt("account_id");
                double balance = rs.getDouble("balance");
                System.out.printf("Customer: %s, Address: %s, Account: %d, Balance: %.2f%n",
                        name, address, accountId, balance);
            }
        } catch (SQLException e) {
            System.err.println("SQLException while viewing customers: " + e.getMessage());
        }
    }

    // Helper: get customer id from account id
    private static int getCustomerIdFromAccountId(Connection conn, int accountId) throws SQLException {
        String query = "SELECT customer_id FROM accounts WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                }
            }
        }
        return -1;
    }

    // Helper: does this customer have any remaining accounts?
    private static boolean hasOtherAccounts(Connection conn, int customerId) throws SQLException {
        String query = "SELECT COUNT(*) FROM accounts WHERE customer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // Create the BankDB database
    public static void createDatabase(Connection conn) throws SQLException {
        String query = "CREATE DATABASE IF NOT EXISTS BankDB";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            System.out.println("Database 'BankDB' created successfully (if it didn't exist).");
        }
    }

    // Create the customers and accounts tables
    public static void createTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String createCustomersTableSQL = "CREATE TABLE IF NOT EXISTS customers ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255),"
                    + "address VARCHAR(255))";

            String createAccountsTableSQL = "CREATE TABLE IF NOT EXISTS accounts ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "customer_id INT,"
                    + "balance DOUBLE,"
                    + "FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE)";

            stmt.execute(createCustomersTableSQL);
            stmt.execute(createAccountsTableSQL);

            System.out.println("Tables created successfully.");
        } catch (SQLException e) {
            System.err.println("SQLException while creating tables: " + e.getMessage());
        }
    }

    public static Connection getDatabaseConnection() {
        String url = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "root";
        String password = "password";

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.out.println("Connection failed SQLException: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Connection failed Exception: " + e.getMessage());
            return null;
        }
    }
}
