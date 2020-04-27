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

import java.math.BigDecimal;
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
            while (true) {
                /* Login Prompt */
                System.out.println("Username: ");
                username = scan.nextLine();
                System.out.println("Password: ");
                password = scan.nextLine();
                
                // Find out the privilege of the person who logged in.
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
                
                ResultSet rset = stmt.executeQuery(getPrivilege);                
                while (rset.next()) {
                    privilege = rset.getString(1);
                }
                
                // If there is no match, keep going in the loop, else break.
                if (privilege == null) {
                    System.out.println("Username and/or password incorrect");
                } else {
                    break;
                }
            }
            
            
            // Prepared statements that we might need.
            PreparedStatement createNewEmployee = conn.prepareStatement(
                "insert into Login (UserID, Password, Privilege) values (?,?,?)"
                );
            PreparedStatement insertIntoEmployee = conn.prepareStatement(
                "insert into Employee (EmployeeID, FirstName, LastName, SSN, Salary, PayType, JobType) values (?,?,?,?,?,?,?)"
                );
            PreparedStatement insertIntoInventory = conn.prepareStatement(
                "insert into Inventory (ItemID, Cost, LeadTime, CategoryType, CategoryNumber) values (?,?,?,?,?)"
                );
            PreparedStatement insertIntoCustomer = conn.prepareStatement(
                "insert into Customer (CustomerID, FirstName, LastName) values (?,?,?)"
                );
            PreparedStatement insertIntoModel = conn.prepareStatement(
                "insert into Model (ModelNumber, SalesPrice) values (?,?)"
                );
            PreparedStatement insertIntoOrder = conn.prepareStatement(
                "insert into Order (OrderNumber, CustomerID, EmployeeID, SalesValue) values (?,?,?,?)"
                );
            PreparedStatement grantRoleToEmployee = conn.prepareStatement(
                "grant ? to ?"
                );
            PreparedStatement createOrder = conn.prepareStatement(
                "insert into Order (OrderNumber, CustomerID, EmployeeId, SalesValue, ItemID, ModelNumber) values (?,?,?,?,?,?)"
                );
            
            
            // Other statements
            String selectLogin = "select * from Login";
            String selectEmployee = "select * from Employee";
            String selectInventory = "slect * from Inventory";
            String selectCustomer = "slect * from Customer";
            String selectModel = "slect * from Model";
            String selectOrder = "slect * from Order";
            
            
            // Create while loops for each privilege (type of user):
            if (privilege.equals("admin")) {
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) Create a new Employee");
                    System.out.println("(2) View/Update a table");
                    System.out.println("(3) Grant access to an Employee");
                    System.out.println("(4) Business analytics");
                    System.out.println("(5) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput.equals("5")) {
                        break;
                    } else if (userInput.equals("4")) {
                        // FIXME: analytics
                    } else if (userInput.equals("3")) {
                        // Ask for EmployeeID, and the permission, plug those values into the grantRole prepared statement, then exectue.
                        System.out.println("Enter EmployeeID of person to grant:");
                        String employeeGrant = scan.nextLine();
                        System.out.println("Enter privilege to grant to the EmployeeID (Admin, HR, Sales, Engineering):");
                        String employeePrivilege = scan.nextLine();
                        
                        grantRoleToEmployee.setString(1, employeePrivilege);
                        grantRoleToEmployee.setString(2, employeeGrant);
                        grantRoleToEmployee.executeUpdate();
                        
                    } else if (userInput.equals("2")) {
                        // User names a table, and it is printed.
                        // Then, the user may enter some SQL to update any table.
                        
                        System.out.println("Enter table name:");
                        String tname = scan.nextLine();
                        
                        ResultSet rset = null;
                        switch(tname) {
                            case "Login":
                                rset = stmt.executeQuery(selectLogin);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4));
                                }
                                break;
                            case "Employee":
                                rset = stmt.executeQuery(selectEmployee);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4)+rset.getString(5)+" "+rset.getString(6)+" "+rset.getString(7));
                                }
                                break;
                            case "Inventory":
                                rset = stmt.executeQuery(selectInventory);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4)+rset.getString(5));
                                }
                                break;
                            case "Customer":
                                rset = stmt.executeQuery(selectCustomer);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3));
                                }
                                break;
                            case "Model":
                                rset = stmt.executeQuery(selectModel);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                                break;
                            case "Order":
                                rset = stmt.executeQuery(selectOrder);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4)+rset.getString(5)+" "+rset.getString(6));
                                }
                                break;
                            default:
                                System.out.println("No option selected");
                        }
                        
                        // Keep asking for update statements until user enters "".
                        while (true) {
                            System.out.println("Input update SQL statement. Press ENTER (empty string) to stop.");
                            String admin_statement = scan.nextLine();
                            if (admin_statement.isEmpty()) {
                                break;
                            }
                            stmt.executeUpdate(admin_statement);
                            
                        }
                        
                        
                        
                    } else if (userInput.equals("1")) {
                    	System.out.println("User Type: ");
                        System.out.println("(1) Admin");
                        System.out.println("(2) Sales");
                        System.out.println("(3) Engineer");
                        System.out.println("(4) HR");
                        String employeeType = scan.nextLine();
                        
                        String firstName, lastName, payType, jobType, newPass;
                        BigDecimal salary = null;
                        int employeeID, SSN;
                    	System.out.print("Employee ID: ");
                    	employeeID = scan.nextInt();
                        System.out.print("First Name: ");
                    	firstName = scan.nextLine();
                        System.out.print("Last Name: ");
                        lastName = scan.nextLine();
                        System.out.print("SSN: ");
                        SSN = scan.nextInt();
                        System.out.print("Salary: ");
                        salary = scan.nextBigDecimal();
                        System.out.print("Pay Type: ");
                        payType = scan.nextLine();
                        System.out.print("Job Type: ");
                        jobType = scan.nextLine();
                        
                        insertIntoEmployee.setInt(1, 2);
                        insertIntoEmployee.setString(2, firstName);
                        insertIntoEmployee.setString(3, lastName);
                        insertIntoEmployee.setInt(4, SSN);
                        insertIntoEmployee.setBigDecimal(5, salary);
                        insertIntoEmployee.setString(6, payType);
                        insertIntoEmployee.setString(7, jobType);
                        insertIntoEmployee.executeUpdate();
                        
                        
                        System.out.println("Username: " + employeeID);
                        System.out.println("Password: ");
                        newPass = scan.nextLine();
                        System.out.println("Privilege: " + jobType);
                        
                        createNewEmployee.setInt(1, employeeID);
                        createNewEmployee.setString(2, newPass);
                        createNewEmployee.setString(3, jobType);
                    }
                    
                    
                    
                }
            } else if (privilege.equals("sales")) {
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update a Customer");
                    System.out.println("(2) Create an Order");
                    System.out.println("(3) Access sales reports");
                    System.out.println("(4) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput.equals("4")) {
                        break;
                    } else if (userInput.equals("3")) {
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else if (privilege.equals("hr")) {
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update an Employee's information");
                    System.out.println("(2) View sales for an Employee");
                    System.out.println("(3) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput.equals("3")) {
                        break;
                    } else if (userInput.equals("2")) {
                        // FIXME: Need to write the code for each action
                    }
                    
                    
                    
                }
            } else if (privilege.equals("engineering")) {
                while (true) {
                    System.out.println("What would you like to do? (Type number) Options:");
                    System.out.println("(1) View/update the Inventory");
                    System.out.println("(2) View/update a Model");
                    System.out.println("(3) View Employee information");
                    System.out.println("(4) Logout");
                    String userInput = scan.nextLine();
                    
                    // For each option, write the code to satisfy it.
                    if (userInput.equals("4")) {
                        break;
                    } else if (userInput.equals("3")) {
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
