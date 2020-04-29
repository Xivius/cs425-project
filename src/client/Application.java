/*
 * Reporting and analytics
 *  - The admin will have the capability of running business analytics reports
 *    that will help them monitor business operations.
 *  - Total revenue from sale, associated employee and customer
 *  - Customer model bought and quantity to make prediction and understand trending
 *  - For each order, the associated parts and available inventory
 *  - Expense report, employee showing salary, bonus expense and part cost
 *  - New message.
 */

package client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Application {
    public static void main(String [] args) {
        Scanner scan = new Scanner(System.in);
        
        try {
            // Access Credentials from *.properties file called DBAccess.properties
            // (located outside repository for security reasons)
            InputStream input = new FileInputStream("../DBAccess.properties");
            Properties credentialsFile = new Properties();
            credentialsFile.load(input);
            
            // Connection to Database
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/" +
                        credentialsFile.getProperty("database"),
                credentialsFile.getProperty("username"),
                credentialsFile.getProperty("password")
            );
            Connection adminConn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/" +
                        credentialsFile.getProperty("database"),
                   credentialsFile.getProperty("admin.username"),
                   credentialsFile.getProperty("admin.password")
            );
            Connection engineerConn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/" +
                        credentialsFile.getProperty("database"),
                   credentialsFile.getProperty("engineer.username"),
                   credentialsFile.getProperty("engineer.password")
            );
            Connection HRConn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/" +
                        credentialsFile.getProperty("database"),
                   credentialsFile.getProperty("hr.username"),
                   credentialsFile.getProperty("hr.password")
            );
            Connection salesConn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/" +
                        credentialsFile.getProperty("database"),
                   credentialsFile.getProperty("sales.username"),
                   credentialsFile.getProperty("sales.password")
            );
            
            // Statements
            Statement stmt = conn.createStatement();
            Statement adminStmt = adminConn.createStatement();
            Statement engineerStmt = engineerConn.createStatement();
            Statement HRStmt = HRConn.createStatement();
            Statement salesStmt = salesConn.createStatement();
            
            // Strings for Simple SQL Queries
            String selectLogin = "SELECT * FROM Login";
            String selectEmployee = "SELECT * FROM Employee";
            String selectInventory = "SELECT * FROM Inventory";
            String selectCustomer = "SELECT * FROM Customer";
            String selectModel = "SELECT * FROM Model";
            String selectOrder = "SELECT * FROM SalesOrder";
            
            // Prepared statements that we might need.
            PreparedStatement selectEmployeeWhere = conn.prepareStatement(
                "SELECT * FROM Employee WHERE EmployeeID = ?"
            );
            PreparedStatement insertIntoLogin = conn.prepareStatement(
                "INSERT INTO Login (UserID, Password, Privilege) VALUES (?,?,?)"
            );
            PreparedStatement insertIntoOrderFull = conn.prepareStatement(
                "INSERT INTO SalesOrder (OrderNumber, CustomerID, EmployeeId, SalesValue, ItemID, ModelNumber) VALUES (?,?,?,?,?,?)"
            );
            PreparedStatement insertIntoOrderPartial = conn.prepareStatement(
                    "INSERT INTO SalesOrder (OrderNumber, CustomerID, EmployeeID, SalesValue) VALUES (?,?,?,?)"
                );
            PreparedStatement insertIntoEmployee = conn.prepareStatement(
                "INSERT INTO Employee (EmployeeID, FirstName, LastName, SSN, Salary, PayType, JobType) VALUES (?,?,?,?,?,?,?)"
            );
            PreparedStatement insertIntoInventory = conn.prepareStatement(
                "INSERT INTO Inventory (ItemID, Cost, LeadTime, CategoryType, CategoryNumber) VALUES (?,?,?,?,?)"
            );
            PreparedStatement insertIntoCustomer = conn.prepareStatement(
                "INSERT INTO Customer (CustomerID, FirstName, LastName) VALUES (?,?,?)"
            );
            PreparedStatement insertIntoModel = conn.prepareStatement(
                "INSERT INTO Model (ModelNumber, SalesPrice) VALUES (?,?)"
            );
            PreparedStatement grantRoleToEmployee = conn.prepareStatement(
                "GRANT ? TO ?"
            );
            
            // ResultSet to hold data for Queries
            ResultSet rs = null;
            
            // Initialize search_path to point to BusinessSchema
            // as defined in SQL DDL (deliverable 2)
            stmt.execute("SET search_path = 'BusinessSchema'");
            adminStmt.execute("SET search_path = 'BusinessSchema'");
            engineerStmt.execute("SET search_path = 'BusinessSchema'");
            HRStmt.execute("SET search_path = 'BusinessSchema'");
            salesStmt.execute("SET search_path = 'BusinessSchema'");
            
            // Booleans for while loops
            boolean authenticated, done, exitApplication;
            
            // Keep Application alive as long as User doesn't want to exit it
            do {
                // Reset looping conditions
                authenticated = false;
                done = false;
                exitApplication = false;
                
                // LOGIN Prompt
                int userID;
                String password;
                String privilege = null;
                
                do {
                    // Stay in loop while there are no matches for username and password
                    System.out.print("Username: ");
                    while (!scan.hasNextInt()) {
                    	System.out.print("Please enter a positive integer: ");
                    	scan.nextLine();
                    }
                    userID = scan.nextInt();
                    scan.nextLine(); // consume rest of line
                    System.out.print("Password: ");
                    password = scan.nextLine();
                    
                    // Find out the privilege of the person who logged in.
                    String getPrivilege = "SELECT Privilege FROM Login " +
                    		"WHERE UserID = ? AND Password = ?";
                    PreparedStatement selectLoginPrivilege =
                    		conn.prepareStatement(getPrivilege);
                    selectLoginPrivilege.setInt(1, userID);
                    selectLoginPrivilege.setString(2, password);
                    rs = selectLoginPrivilege.executeQuery();
                    while (rs.next()) {
                        privilege = rs.getString(1);
                    }
                    
                    // If there is no match, keep going in the loop, else break.
                    if (privilege == null) {
                        System.out.println("Username and/or password incorrect");
                    } else {
                        authenticated = true;
                    }
                } while (!authenticated);
                
                // Create while loops for each privilege (type of user):
                if (privilege.equalsIgnoreCase("admin")) {
                    while (!done) {
                        System.out.println("Options:");
                        System.out.println("(1) Create a new Employee");
                        System.out.println("(2) View/Update a table");
                        System.out.println("(3) Grant access to an Employee");
                        System.out.println("(4) Business analytics");
                        System.out.println("(5) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("5")) {
                            done = true;
                        } else if (userInput.equals("4")) {
                            // FIXME: analytics
                        } else if (userInput.equals("3")) {
                            // Ask for EmployeeID, and the permission, plug those values into the grantRole prepared statement, then exectue.
                            System.out.print("Enter EmployeeID of person to grant: ");
                            String employeeGrant = scan.nextLine();
                            System.out.print("Enter privilege to be granted (Admin, HR, Sales, Engineering): ");
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
                                case "SalesOrder":
                                    rset = stmt.executeQuery(selectOrder);
                                    while (rset.next()) {
                                        System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4)+rset.getString(5)+" "+rset.getString(6));
                                    }
                                    break;
                                default:
                                    System.out.println("No option selected");
                            }
                            
                            // Keep asking for update statements until user enters "".
                            String admin_statement = "";
                            while (!admin_statement.isEmpty()) {
                                System.out.println("Input update SQL statement. Press ENTER (empty string) to stop.");
                                admin_statement = scan.nextLine();
                                if (!admin_statement.isEmpty()) {
                                    stmt.executeUpdate(admin_statement);
                                }
                            }
                        } else if (userInput.equals("1")) {
                            System.out.println("Options:");
                            System.out.println("(1) Admin");
                            System.out.println("(2) Sales");
                            System.out.println("(3) Engineer");
                            System.out.println("(4) HR");
                            System.out.print("User Type (Type the number): ");
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
                            System.out.print("Pay Type (Hourly/Yearly): ");
                            payType = scan.nextLine();
                            System.out.print("Job Type (Admin, Sales, HR, or Engineer): ");
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
                            
                            insertIntoLogin.setInt(1, employeeID);
                            insertIntoLogin.setString(2, newPass);
                            insertIntoLogin.setString(3, jobType);
                            insertIntoLogin.executeUpdate();
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else if (privilege.equalsIgnoreCase("sales")) {
                    while (!done) {
                        System.out.println("Options:");
                        System.out.println("(1) View/update a Customer");
                        System.out.println("(2) Create an Order");
                        System.out.println("(3) Access sales reports");
                        System.out.println("(4) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("4")) {
                            done = true;
                        } else if (userInput.equals("3")) {
                            // FIXME: Need to write the code for each action
                        } else if (userInput.equals("2")) {
                            //(OrderNumber, CustomerID, EmployeeID, SalesValue)
                            System.out.print("Enter the order number: ");
                            String orderNumber = scan.nextLine();
                            
                            System.out.print("Enter the Customer ID: ");
                            String custID = scan.nextLine();
                            
                            System.out.print("Enter the Employee ID: ");
                            String emplID = scan.nextLine();
                            
                            System.out.print("Enter the Sales Value: ");
                            String saleVal = scan.nextLine();
                            
                            insertIntoOrderPartial.setString(1, orderNumber);
                            insertIntoOrderPartial.setString(2, custID);
                            insertIntoOrderPartial.setString(3, emplID);
                            insertIntoOrderPartial.setString(4, saleVal);
                            insertIntoOrderPartial.executeUpdate();
                        } else if (userInput.equals("1")) {
                            // Print out all the Customers table
                            System.out.println("Here is the Customers table:");
                            rs = stmt.executeQuery(selectCustomer);
                            while (rs.next()) {
                                System.out.println(rs.getString(1)+" "+rs.getString(2)+" "+rs.getString(3));
                            }
                            
                            // Let the user run SQL to edit the table
                            while (true) {
                                System.out.println("Input update SQL statement. Press ENTER (empty string) to stop.");
                                String admin_statement = scan.nextLine();
                                if (admin_statement.isEmpty()) {
                                    break;
                                }
                                stmt.executeUpdate(admin_statement);
                            }
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else if (privilege.equalsIgnoreCase("hr")) {
                    while (!done) {
                        System.out.println("Options:");
                        System.out.println("(1) View/update an Employee's information");
                        System.out.println("(2) View sales for an Employee");
                        System.out.println("(3) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("3")) {
                            done = true;
                        } else if (userInput.equals("2")) {
                            // TODO: Finish this section
                            PreparedStatement ps = HRConn.prepareStatement(
                                "SELECT EmployeeID, FirstName, LastName, " +
                                "COUNT(SalesValue) NumberOfSales, SUM(SalesValue) AS TotalSales" +
                                "FROM SalesOrder RIGHT JOIN Employee" +
                                "ON SalesOrder.EmployeeID = Employee.EmployeeID" +
                                "GROUP BY EmployeeID ORDER BY EmployeeID"
                            );
                            rs = ps.executeQuery();
                            
                            do {
                                System.out.println("Type a or all to see all employees.");
                                System.out.println("Type q or quit to quit.");
                                System.out.print("Enter Employee ID Number: ");
                                String userQueryID = scan.nextLine(); // prompt user for ID
                                
                                if (userInput.equalsIgnoreCase("a") ||
                                        userInput.equalsIgnoreCase("all")) { // see all option
                                    
                                }
                                while (rs.next()) {
                                    System.out.println(rs.getInt("EmployeeID") + ": $"
                                        + rs.getBigDecimal("Salary"));
                                }
                            } while (!userInput.equalsIgnoreCase("q") && 
                                 !userInput.equalsIgnoreCase("quit"));
                        } else if (userInput.equals("1")) {
                            String userQueryID = "";
                            
                            do {
                                System.out.println("Type a or all to see all employees.");
                                System.out.println("Type q or quit to quit.");
                                System.out.print("Enter Employee ID Number: ");
                                userQueryID = scan.nextLine(); // prompt user for ID
                                
                                if (userInput.equalsIgnoreCase("a") ||
                                        userInput.equalsIgnoreCase("all")) { // see all option
                                    selectEmployeeWhere.setInt(1, Integer.parseInt(userQueryID));
                                    rs = selectEmployeeWhere.executeQuery();
                                    System.out.println("List of Employees: ");
                                    while (rs.next()) { // print out all employees
                                        System.out.println(rs.getInt("EmployeeID") + ": " 
                                            + rs.getString("LastName") + ", " + rs.getString("FirstName"));
                                    }
                                    System.out.println("");
                                } else if (userInput.matches("\\d+")) { // valid ID format
                                    selectEmployeeWhere.setInt(1, Integer.parseInt(userQueryID));
                                    rs = selectEmployeeWhere.executeQuery();
                                    
                                    if (!rs.next()) { // ID not found
                                        System.out.println("Sorry; there is no employee with ID " + userQueryID + ".");
                                    } else { // found an employee with matching ID
                                        System.out.println(rs.toString()); // TODO: print all employee information
                                        System.out.print("Modify this information (y/n)? ");
                                        userInput = scan.nextLine();
                                        if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("yes")) {
                                            // modify user info:
                                            String firstName, lastName, payType, jobType;
                                            BigDecimal salary = null;
                                            int employeeID, SSN;
                                            
                                            // used to modify employee info
                                            PreparedStatement updateEmployee = null;
                                            
                                            do {
                                                System.out.println("Options: ");
                                                System.out.println("(1) Employee ID");
                                                System.out.println("(2) First Name");
                                                System.out.println("(3) Last Name");
                                                System.out.println("(4) SSN");
                                                System.out.println("(5) Salary");
                                                System.out.println("(6) Pay Type");
                                                System.out.println("(7) Job Type");
                                                System.out.println("(8) Quit");
                                                System.out.print("Choose a field to modify (Type number): ");
                                                userInput = scan.nextLine();
                                                
                                                switch (userInput) {
                                                    case "1":
                                                        System.out.print("Employee ID: ");
                                                        employeeID = scan.nextInt();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET EmployeeID = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setInt(1, employeeID);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        userQueryID = Integer.toString(employeeID); // keep user on this employee
                                                        break;
                                                    case "2":
                                                        System.out.print("First Name: ");
                                                        firstName = scan.nextLine();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET FirstName = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setString(1, firstName);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "3":
                                                        System.out.print("Last Name: ");
                                                        lastName = scan.nextLine();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET LastName = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setString(1, lastName);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "4":
                                                        System.out.print("SSN: ");
                                                        SSN = scan.nextInt();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET SSN = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setInt(1, SSN);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "5":
                                                        System.out.print("Salary: ");
                                                        salary = scan.nextBigDecimal();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET Salary = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setBigDecimal(1, salary);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "6":
                                                        System.out.print("Pay Type (Hourly/Yearly): ");
                                                        payType = scan.nextLine();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET PayType = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setString(1, payType);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "7":
                                                        System.out.print("Job Type (Admin, Sales, HR, or Engineer): ");
                                                        jobType = scan.nextLine();
                                                        updateEmployee = HRConn.prepareStatement(
                                                            "UPDATE Employee SET JobType = ? " +
                                                            "WHERE EmployeeID = ?"
                                                        );
                                                        updateEmployee.setString(1, jobType);
                                                        updateEmployee.setInt(2, Integer.parseInt(userQueryID));
                                                        updateEmployee.executeUpdate();
                                                        break;
                                                    case "8":
                                                        break;
                                                    default:
                                                        System.out.println("Invalid option.");
                                                }
                                            } while (!userInput.equals("8"));
                                        }
                                    }
                                    System.out.println("");
                                } else if (!userInput.equalsIgnoreCase("q") &&
                                           !userInput.equalsIgnoreCase("quit")) { // invalid ID format
                                    System.out.println("Invalid Employee ID. Please enter a positive integer value.\n");
                                }
                            } while (!userInput.equalsIgnoreCase("q") &&
                                     !userInput.equalsIgnoreCase("quit"));
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else if (privilege.equalsIgnoreCase("engineering")) {
                    while (!done) {
                        System.out.println("Options:");
                        System.out.println("(1) View/update the Inventory");
                        System.out.println("(2) View/update a Model");
                        System.out.println("(3) View Employee information");
                        System.out.println("(4) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("4")) {
                            done = true;
                        } else if (userInput.equals("3")) {
                            // FIXME: Need to write the code for each action
                        } else if (userInput.equals("2")) {
                            // FIXME: Need to write the code for each action
                        } else if (userInput.equals("1")) {
                            // FIXME: Need to write the code for each action
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else {
                    System.out.println("Something went wrong, no department matched...");
                }
            } while (!exitApplication);
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}
