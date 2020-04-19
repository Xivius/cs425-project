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
        String username;
        String password;
        String sql = "";
        
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            
            conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres", "1234"
            );
            stmt = conn.createStatement();
            sql = "SELECT * FROM ";
            rs = stmt.executeQuery(sql);
            
            
            // LOGIN PART
            // Stay in loop while there are no matches for username and password
            String privilege = null;
            while(true){
                /* Login Prompt */
                System.out.print("Username: ");
                username = scan.nextLine();
                System.out.print("Password: ");
                password = scan.nextLine();
                
                // Find out the priviledge of the person who logged in.
                // Select privilege... where the username and password matches.
                /*
                String getPrivilege = "SELECT Privilege FROM Login JOIN Employee_Login"
                                        +"ON Login.UserID = Employee_Login.UserID"
                                        +"WHERE Employee_Login.Username = " + username
                                        + "AND Employee_Login.Password = " + password;
                */
                                        
                // Or, if the username and password are in the Login table:
                String getPrivilege = "SELECT Privilege FROM Login WHERE Username ="
                                        + username + "AND Password = " + password;
                
                ResultSet rset = stmt.executeQuery(getPriviledge);                
                while (rset.next()){
                    privilege = rset.getString(1);
                }
                
                // If there is no match, keep going in the loop, else break.
                if (privilege == null){
                    System.out.println("Username and password does not match");
                } else {
                    break;
                }
            }
            
            
            
                                    
            
            
            
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}
