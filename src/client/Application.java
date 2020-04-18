/*
 * Reporting and analytics
 *  - The admin will have the capability of running business analytics reports
 *    that will help them monitor business operations.
 *  - Total revenue from sale, associated employee and customer
 *  - Customer model bought and quantity to make prediction and understand trending
 *  - For each order, the associated parts and available inventory
 *  - Expense report, employee showing salary, bonus expense and part cost
 */

package client;

import java.sql.*;
import java.util.Scanner;

public class Application {
    public static void main(String [] args) {
        Scanner scan = new Scanner(System.in);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String username;
        String password;
        String sql = "";
        
        /* Login Prompt */
        System.out.print("Username: ");
        username = scan.nextLine();
        System.out.print("Password: ");
        password = scan.nextLine();
        
        try {
            conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres", "1234"
            );
            stmt = conn.createStatement();
            sql = "SELECT * FROM ";
            rs = stmt.executeQuery(sql);
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}