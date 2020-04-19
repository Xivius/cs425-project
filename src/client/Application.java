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
                System.out.println("Username: ");
                username = scan.nextLine();
                System.out.println("Password: ");
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
                    System.out.println("Username and/or password incorrect");
                } else {
                    break;
                }
            }
            
            
            // Create while loops for each privilege (type of user):
            if (privilege == "admin"){
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) Create a new Employee");
                    System.out.println("(2) Update a table");
                    System.out.println("(3) Grant access to an Employee");
                    System.out.println("(4) Business analytics");
                    System.out.println("(5) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput == "5"){
                        break;
                    } else if (userInput == "4"){
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else if (privilege == "sales"){
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update a Customer");
                    System.out.println("(2) Create an Order");
                    System.out.println("(3) Access sales reports");
                    System.out.println("(4) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput == "4"){
                        break;
                    } else if (userInput == "3"){
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else if (privilege == "hr"){
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update an Employee's information");
                    System.out.println("(2) View sales for an Employee");
\                   System.out.println("(3) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput == "3"){
                        break;
                    } else if (userInput == "2"){
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else if (privilege == "engineering"){
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update the Inventory");
                    System.out.println("(2) View/update a Model");
\                   System.out.println("(3) View Employee information");
                    System.out.println("(4) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput == "4"){
                        break;
                    } else if (userInput == "3"){
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else {
                System.out.println("Something went wrong, no department matched...");

            }
            
            
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}