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
            
            // Report queries
            String totalRevenueReport = "SELECT EmployeeID, SUM(SalesValue) FROM SalesOrder GROUP BY EmployeeID ORDER BY EmployeeID";
            String modelNumbersBoughtReport = "SELECT ModelNumber, COUNT(ModelNumber) FROM Order_Details GROUP BY ModelNumber ORDER BY ModelNumber";
            String partsInInventoryReport = "SELECT ItemID, SUM(CategoryNumber) FROM Inventory GROUP BY ItemID ORDER BY itemID";
            
            // Strings for Simple SQL Queries
            String selectLogin = "SELECT * FROM Login";
            String selectEmployee = "SELECT * FROM Employee";
            String selectInventory = "SELECT * FROM Inventory";
            String selectCustomer = "SELECT * FROM Customer";
            String selectModel = "SELECT * FROM Model";
            String selectOrder = "SELECT * FROM SalesOrder";
            String selectEmployeeView = "SELECT * FROM EmployeeEngineerView";
            
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
            PreparedStatement insertIntoOrderDetails= conn.prepareStatement(
                    "INSERT INTO Order_Details (OrderNumber, ItemID, ModelNumber) VALUES (?,?,?)"
            );
            PreparedStatement insertIntoOrderPartial = conn.prepareStatement(
                    "INSERT INTO SalesOrder (OrderNumber, CustomerID, EmployeeID, SalesValue) VALUES (?,?,?,?)"
            );
            PreparedStatement insertIntoEmployee = conn.prepareStatement(
                "INSERT INTO Employee (EmployeeID, FirstName, LastName, SSN, Salary, PayType, JobType) VALUES (?,?,?,?,?,?,?)"
            );
            PreparedStatement insertIntoInventory = conn.prepareStatement(
                "INSERT INTO Inventory (ItemID, Cost, LeadTime, CategoryType, CategoryNumber) "
                + "VALUES (?,?,INTERVAL '?' DAY,?,?)"
            );
            PreparedStatement insertIntoCustomer = conn.prepareStatement(
                "INSERT INTO Customer (CustomerID, FirstName, LastName) VALUES (?,?,?)"
            );
            PreparedStatement insertIntoModel = engineerConn.prepareStatement(
                "INSERT INTO Model (ModelNumber, SalesPrice) VALUES (?,?)"
            );
            PreparedStatement grantRoleToEmployee = adminConn.prepareStatement(
                "UPDATE Login SET Privilege = ? WHERE UserID = ?"
            );
            
            // ResultSet to hold data for Queries
            ResultSet rs = null;
            ResultSet rset = null; // I used a different name for my code
            
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
                    while (!scan.hasNextInt()) { // validate input value
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
                        System.out.println("\nOptions:");
                        System.out.println("(1) Create a new Employee");
                        System.out.println("(2) View/Update a table");
                        System.out.println("(3) Grant access to an Employee");
                        System.out.println("(4) Business analytics and reports");
                        System.out.println("(5) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("5")) {
                            done = true;
                            System.out.println(""); // print extra line for readability
                        } else if (userInput.equals("4")) {
                            // Analytics options shown. After selection, they query is executed and results printed.
                            System.out.println("Available report options:");
                            System.out.println("(1) Total revenue");
                            System.out.println("(2) Model numbers bought");
                            System.out.println("(3) Total parts in inventory");
                            System.out.print("What would you like to do? (Type number): ");
                            String userRNum = scan.nextLine();
                            if (userRNum.equals("1")) {
                                rset = stmt.executeQuery(totalRevenueReport);
                                System.out.println(""); // print extra line for readability
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            } else if (userRNum.equals("2")) {
                                rset = stmt.executeQuery(modelNumbersBoughtReport);
                                System.out.println(""); // print extra line for readability
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            } else if (userRNum.equals("3")) {
                                rset = stmt.executeQuery(partsInInventoryReport);
                                System.out.println(""); // print extra line for readability
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            }
                        } else if (userInput.equals("3")) {
                            // Ask for EmployeeID, and the permission, plug those values into the grantRole prepared statement, then exectue.
                            System.out.print("Enter EmployeeID of person to grant: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int empID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            System.out.print("Enter privilege to be granted (Admin, HR, Sales, Engineering, None): ");
                            String employeePrivilege = scan.nextLine();
                            while (!employeePrivilege.equals("Admin") && !employeePrivilege.equals("Engineer")
                            		&& !employeePrivilege.equals("HR") && !employeePrivilege.equals("Sales")) { // validate input value
                                if (employeePrivilege.equalsIgnoreCase("Admin")) { // Fix value (CaseSensitive)
                                	employeePrivilege = "Admin";
                                } else if (employeePrivilege.equalsIgnoreCase("Engineer")) {
                                	employeePrivilege = "Engineer";
                                } else if (employeePrivilege.equalsIgnoreCase("HR")) {
                                	employeePrivilege = "HR";
                                } else if (employeePrivilege.equalsIgnoreCase("Sales")) {
                                	employeePrivilege = "Sales";
                                } else if (employeePrivilege.equalsIgnoreCase("None")) {
                                	employeePrivilege = "NULL";
                                	break;
                                } else {
                                	System.out.print("Please enter a valid Job Type " 
                                			+ "(Admin, Engineer, HR, Sales): ");
                                	employeePrivilege = scan.nextLine();
                                }
                            }
                            
                            grantRoleToEmployee.setString(1, employeePrivilege);
                            grantRoleToEmployee.setInt(2, empID);
                            grantRoleToEmployee.executeUpdate();
                        } else if (userInput.equals("2")) {
                            // User names a table, and it is printed.
                            // Then, the user may enter some SQL to update any table.
                            
                            System.out.print("Enter table name: ");
                            String tname = scan.nextLine();
                            
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
                                        System.out.println(rset.getString(1)+" "+rset.getString(2)+" "+rset.getString(3)+" "+rset.getString(4)+" "+rset.getString(5)+" "+rset.getString(6)+" "+rset.getString(7));
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
                            String firstName, lastName, payType, jobType, newPass;
                            BigDecimal salary = null;
                            int employeeID, SSN;
                            System.out.print("Employee ID: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            employeeID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            System.out.print("First Name: ");
                            firstName = scan.nextLine();
                            System.out.print("Last Name: ");
                            lastName = scan.nextLine();
                            System.out.print("SSN: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a valid SSN (without dashes): ");
                                scan.nextLine();
                            }
                            SSN = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            System.out.print("Salary: ");
                            while (!scan.hasNextBigDecimal()) { // validate input value
                                System.out.print("Please enter a valid salary: ");
                                scan.nextLine();
                            }
                            salary = scan.nextBigDecimal();
                            scan.nextLine(); // consume rest of line
                            System.out.print("Pay Type (Hourly/Yearly): ");
                            payType = scan.nextLine();
                            while (!payType.equals("Hourly") && !payType.equals("Yearly")) { // validate input value
                                if (payType.equalsIgnoreCase("Hourly")) { // Fix value (CaseSensitive)
                                    payType = "Hourly";
                                } else if (payType.equalsIgnoreCase("Yearly")) {
                                	payType = "Yearly";
                                } else { // invalid entry
                                	System.out.print("Please enter a valid Pay Type (Hourly or Yearly): ");
                                	payType = scan.nextLine();
                                }
                            }
                            System.out.print("Job Type (Admin, Sales, HR, or Engineer): ");
                            jobType = scan.nextLine();
                            while (!jobType.equals("Admin") && !jobType.equals("Engineer")
                            		&& !jobType.equals("HR") && !jobType.equals("Sales")) { // validate input value
                                if (jobType.equalsIgnoreCase("Admin")) { // Fix value (CaseSensitive)
                                	jobType = "Admin";
                                } else if (jobType.equalsIgnoreCase("Engineer")) {
                                	jobType = "Engineer";
                                } else if (jobType.equalsIgnoreCase("HR")) {
                                	jobType = "HR";
                                } else if (jobType.equalsIgnoreCase("Sales")) {
                                	jobType = "Sales";
                                } else {
                                	System.out.print("Please enter a valid Job Type " 
                                			+ "(Admin, Engineer, HR, Sales): ");
                                	jobType = scan.nextLine();
                                }
                            }
                            
                            insertIntoEmployee.setInt(1, employeeID);
                            insertIntoEmployee.setString(2, firstName);
                            insertIntoEmployee.setString(3, lastName);
                            insertIntoEmployee.setInt(4, SSN);
                            insertIntoEmployee.setBigDecimal(5, salary);
                            insertIntoEmployee.setString(6, payType);
                            insertIntoEmployee.setString(7, jobType);
                            insertIntoEmployee.executeUpdate();
                            
                            System.out.println("Username: " + employeeID);
                            System.out.print("Password: ");
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
                        System.out.println("\nOptions:");
                        System.out.println("(1) View/update a Customer");
                        System.out.println("(2) Create an Order");
                        System.out.println("(3) Access sales, and other reports");
                        System.out.println("(4) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("4")) {
                            done = true;
                            System.out.println(""); // print extra line for readability
                        } else if (userInput.equals("3")) {
                            // Analytics options shown. After selection, they query is executed and results printed.
                            System.out.println("Available report options:");
                            System.out.println("(1) Total revenue");
                            System.out.println("(2) Model numbers bought");
                            System.out.println("(3) Total parts in inventory");
                            System.out.print("What would you like to do? (Type number): ");
                            String userRNum = scan.nextLine();
                            if (userRNum.equals("1")) {
                                rset = stmt.executeQuery(totalRevenueReport);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            } else if (userRNum.equals("2")) {
                                rset = stmt.executeQuery(modelNumbersBoughtReport);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            } else if (userRNum.equals("3")) {
                                rset = stmt.executeQuery(partsInInventoryReport);
                                while (rset.next()) {
                                    System.out.println(rset.getString(1)+" "+rset.getString(2));
                                }
                            }
                        } else if (userInput.equals("2")) {
                            //(OrderNumber, CustomerID, EmployeeID, SalesValue)
                            System.out.print("Enter the order number: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int orderNumber = scan.nextInt();
                            scan.hasNextLine(); // consume rest of line
                            
                            System.out.print("Enter the Customer ID: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int custID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            
                            System.out.print("Enter the Employee ID: ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int emplID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            
                            System.out.print("Enter the Sales Value: ");
                            while (!scan.hasNextBigDecimal()) { // validate input value
                            	System.out.print("Please enter a valid salary: ");
                                scan.nextLine();
                            }
                            BigDecimal saleVal = scan.nextBigDecimal();
                            scan.nextLine(); // consume rest of line
                            
                            insertIntoOrderPartial.setInt(1, orderNumber);
                            insertIntoOrderPartial.setInt(2, custID);
                            insertIntoOrderPartial.setInt(3, emplID);
                            insertIntoOrderPartial.setBigDecimal(4, saleVal);
                            insertIntoOrderPartial.executeUpdate();
                            
                            System.out.print("Enter Item ID (Inventory's Item): ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int itemID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            
                            System.out.print("Enter Model Number (Order's Model): ");
                            while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int modelNumber = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                            
                            insertIntoOrderDetails.setInt(1, orderNumber);
                            insertIntoOrderDetails.setInt(2, itemID);
                            insertIntoOrderDetails.setInt(3, modelNumber);
                            insertIntoOrderDetails.execute();
                            
                        } else if (userInput.equals("1")) {
                            // Print out all the Customers table
                            System.out.println("Here is the Customer table:");
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
                                rset = stmt.executeQuery(admin_statement);
                                while (rset.next()) {
                                	for (int i = 0; i < rset.getFetchSize(); i++)
                                		System.out.print(rset.getString(i) + " ");
                                	System.out.println("");
                                }
                            }
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else if (privilege.equalsIgnoreCase("hr")) {
                    while (!done) {
                        System.out.println("\nOptions:");
                        System.out.println("(1) View/update an Employee's information");
                        System.out.println("(2) View sales for an Employee");
                        System.out.println("(3) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("3")) {
                            done = true;
                            System.out.println(""); // print extra line for readability
                        } else if (userInput.equals("2")) {
                        	System.out.print("Enter employee ID whose sales you want to see: ");
                        	while (!scan.hasNextInt()) { // validate input value
                                System.out.print("Please enter a positive integer: ");
                                scan.nextLine();
                            }
                            int employeeID = scan.nextInt();
                            scan.nextLine(); // consume rest of line
                        	
                            PreparedStatement selectEmployeeSales = HRConn.prepareStatement(
                                "SELECT Employee.EmployeeID, FirstName, LastName, " +
                                "COUNT(SalesValue) NumberOfSales, SUM(SalesValue) AS TotalSales " +
                                "FROM SalesOrder RIGHT JOIN Employee " +
                                "ON SalesOrder.EmployeeID = Employee.EmployeeID " +
                                "WHERE Employee.EmployeeID = ? " +
                                "GROUP BY Employee.EmployeeID ORDER BY Employee.EmployeeID"
                            );
                            selectEmployeeSales.setInt(1, employeeID);
                            rs = selectEmployeeSales.executeQuery();
                            while (rs.next()) {
                            	System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " " +
                            			rs.getString(3) + "\nNumber of Sales: " + 
                            			rs.getInt(4) + "\nTotal Sales $" + rs.getBigDecimal(5));
                            }
                        } else if (userInput.equals("1")) {
                            String userQueryID = "";
                            
                            System.out.println("Type a or all to see all employees.");
                            System.out.print("Enter Employee ID Number: ");
                            userQueryID = scan.nextLine(); // prompt user for ID
                            
                            if (userQueryID.equalsIgnoreCase("a") ||
                                    userInput.equalsIgnoreCase("all")) { // see all option
                                rs = HRStmt.executeQuery(selectEmployee);
                                System.out.println("List of Employees: ");
                                while (rs.next()) { // print out all employees
                                    System.out.println("\t" + rs.getInt("EmployeeID") + ": " 
                                        + rs.getString("LastName") + ", " + rs.getString("FirstName"));
                                }
                            } else if (userQueryID.matches("\\d+")) { // valid ID format
                                selectEmployeeWhere.setInt(1, Integer.parseInt(userQueryID));
                                rs = selectEmployeeWhere.executeQuery();
                                
                                if (!rs.next()) { // ID not found
                                    String userAns = "";
                                	System.out.println("No employee with ID " + userQueryID + ".\n"
                                    		+ "Create one instead (y/n)? ");
                                	userAns = scan.nextLine();
                                    if (userAns.equalsIgnoreCase("y") || userAns.equalsIgnoreCase("yes")) {
                                    	String firstName, lastName, payType, jobType, newPass;
                                        BigDecimal salary = null;
                                        int employeeID, SSN;
                                        System.out.print("Employee ID: ");
                                        while (!scan.hasNextInt()) { // validate input value
                                            System.out.print("Please enter a positive integer: ");
                                            scan.nextLine();
                                        }
                                        employeeID = scan.nextInt();
                                        scan.nextLine(); // consume rest of line
                                        System.out.print("First Name: ");
                                        firstName = scan.nextLine();
                                        System.out.print("Last Name: ");
                                        lastName = scan.nextLine();
                                        System.out.print("SSN: ");
                                        while (!scan.hasNextInt()) { // validate input value
                                            System.out.print("Please enter a valid SSN (without dashes): ");
                                            scan.nextLine();
                                        }
                                        SSN = scan.nextInt();
                                        scan.nextLine(); // consume rest of line
                                        System.out.print("Salary: ");
                                        while (!scan.hasNextBigDecimal()) { // validate input value
                                            System.out.print("Please enter a valid salary: ");
                                            scan.nextLine();
                                        }
                                        salary = scan.nextBigDecimal();
                                        scan.nextLine(); // consume rest of line
                                        System.out.print("Pay Type (Hourly/Yearly): ");
                                        payType = scan.nextLine();
                                        while (!payType.equals("Hourly") && !payType.equals("Yearly")) { // validate input value
                                            if (payType.equalsIgnoreCase("Hourly")) { // Fix value (CaseSensitive)
                                                payType = "Hourly";
                                            } else if (payType.equalsIgnoreCase("Yearly")) {
                                            	payType = "Yearly";
                                            } else { // invalid entry
                                            	System.out.print("Please enter a valid Pay Type (Hourly or Yearly): ");
                                            	payType = scan.nextLine();
                                            }
                                        }
                                        System.out.print("Job Type (Admin, Sales, HR, or Engineer): ");
                                        jobType = scan.nextLine();
                                        while (!jobType.equals("Admin") && !jobType.equals("Engineer")
                                        		&& !jobType.equals("HR") && !jobType.equals("Sales")) { // validate input value
                                            if (jobType.equalsIgnoreCase("Admin")) { // Fix value (CaseSensitive)
                                            	jobType = "Admin";
                                            } else if (jobType.equalsIgnoreCase("Engineer")) {
                                            	jobType = "Engineer";
                                            } else if (jobType.equalsIgnoreCase("HR")) {
                                            	jobType = "HR";
                                            } else if (jobType.equalsIgnoreCase("Sales")) {
                                            	jobType = "Sales";
                                            } else {
                                            	System.out.print("Please enter a valid Job Type " 
                                            			+ "(Admin, Engineer, HR, Sales): ");
                                            	jobType = scan.nextLine();
                                            }
                                        }
                                        
                                        insertIntoEmployee.setInt(1, employeeID);
                                        insertIntoEmployee.setString(2, firstName);
                                        insertIntoEmployee.setString(3, lastName);
                                        insertIntoEmployee.setInt(4, SSN);
                                        insertIntoEmployee.setBigDecimal(5, salary);
                                        insertIntoEmployee.setString(6, payType);
                                        insertIntoEmployee.setString(7, jobType);
                                        insertIntoEmployee.executeUpdate();
                                    }
                                } else { // found an employee with matching ID
                                	System.out.println(rs.getString(1)+" "+rs.getString(2)+" "+rs.getString(3)+" "+rs.getString(4)+" "+rs.getString(5)+" "+rs.getString(6)+" "+rs.getString(7));
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
                                            System.out.println("\nOptions: ");
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
                                                    while (!scan.hasNextInt()) { // validate input value
                                                        System.out.print("Please enter a positive integer: ");
                                                        scan.nextLine();
                                                    }
                                                    employeeID = scan.nextInt();
                                                    scan.nextLine(); // consume rest of line
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
                                                    while (!scan.hasNextInt()) { // validate input value
                                                        System.out.print("Please enter a SSN (without dashes): ");
                                                        scan.nextLine();
                                                    }
                                                    SSN = scan.nextInt();
                                                    scan.nextLine(); // consume rest of line
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
                                                    while (!scan.hasNextBigDecimal()) { // validate input value
                                                        System.out.print("Please enter a valid salary: ");
                                                        scan.nextLine();
                                                    }
                                                    salary = scan.nextBigDecimal();
                                                    scan.nextLine(); // consume rest of line
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
                                                    while (!payType.equals("Hourly") && !payType.equals("Yearly")) { // validate input value
                                                        if (payType.equalsIgnoreCase("Hourly")) { // Fix value (CaseSensitive)
                                                            payType = "Hourly";
                                                        } else if (payType.equalsIgnoreCase("Yearly")) {
                                                        	payType = "Yearly";
                                                        } else { // invalid entry
                                                        	System.out.print("Please enter a valid Pay Type (Hourly or Yearly): ");
                                                        	payType = scan.nextLine();
                                                        }
                                                    }
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
                                                    while (!jobType.equals("Admin") && !jobType.equals("Engineer")
                                                    		&& !jobType.equals("HR") && !jobType.equals("Sales")) { // validate input value
                                                        if (jobType.equalsIgnoreCase("Admin")) { // Fix value (CaseSensitive)
                                                        	jobType = "Admin";
                                                        } else if (jobType.equalsIgnoreCase("Engineer")) {
                                                        	jobType = "Engineer";
                                                        } else if (jobType.equalsIgnoreCase("HR")) {
                                                        	jobType = "HR";
                                                        } else if (jobType.equalsIgnoreCase("Sales")) {
                                                        	jobType = "Sales";
                                                        } else {
                                                        	System.out.print("Please enter a valid Job Type " 
                                                        			+ "(Admin, Engineer, HR, Sales): ");
                                                        	jobType = scan.nextLine();
                                                        }
                                                    }
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
                            } else {
                            	System.out.println("Invalid ID format.");
                            }
                        } else {
                            System.out.println("Invalid Option.");
                        }
                    }
                } else if (privilege.equalsIgnoreCase("engineer")) {
                    while (!done) {
                        System.out.println("\nOptions:");
                        System.out.println("(1) View/update the Inventory");
                        System.out.println("(2) View/update a Model");
                        System.out.println("(3) View Employee information");
                        System.out.println("(4) Logout");
                        System.out.print("What would you like to do? (Type number): ");
                        String userInput = scan.nextLine();
                        
                        // For each option, write the code to satisfy it.
                        if (userInput.equals("4")) {
                            done = true;
                            System.out.println(""); // print extra line for readability
                        } else if (userInput.equals("3")) {
                        	System.out.println("\nEmployee Viewing Options:");
                        	System.out.println("(1) See All");
                        	System.out.println("(2) Search for Specific Employee");
                        	System.out.print("What would you like to do? (Type number): ");
                        	String answer = scan.nextLine();
                        	
                        	PreparedStatement selectEmployeeViewWhere = null;
                        	
                        	switch (answer) {
                        		case "1":
                        			System.out.println("\nEmployees:");
                                    rset = engineerStmt.executeQuery(selectEmployeeView);
                                    while (rset.next()) {
                                    	System.out.println(rset.getInt(1) + ": " + rset.getString(2) + " " 
                            					+ rset.getString(3) + "; " + rset.getString(4));
                                    }
                        			break;
                        		case "2":
                        			System.out.print("Enter Employee ID: ");
                        			while (!scan.hasNextInt()) { // validate input value
    	                                System.out.print("Please enter a positive integer: ");
    	                                scan.nextLine();
    	                            }
                        			int emplID = scan.nextInt();
                        			scan.nextLine(); // consume rest of line
                        			
                        			selectEmployeeViewWhere = engineerConn.prepareStatement(
                        					"SELECT * FROM EmployeeEngineerView WHERE EmployeeID = ?");
                        			selectEmployeeViewWhere.setInt(1, emplID);
                        			rset = selectEmployeeViewWhere.executeQuery();
                        			while (rset.next()) {
                                    	System.out.println(rset.getInt(1) + ": " + rset.getString(2) + " " 
                            					+ rset.getString(3) + "; " + rset.getString(4));
                                    }
                        			break;
                        		default:
                        			System.out.println("Invalid Option.");
                        	}
                        } else if (userInput.equals("2")) {
                            System.out.println("\nModel Table:");
                            rs = engineerStmt.executeQuery(selectModel);
                            while (rs.next()) {
                            	System.out.println(rs.getInt(1) + " " + rs.getBigDecimal(2));
                            }
                            
                            System.out.println("\nModel Table Options:");
                            System.out.println("(1) Add new entry");
                            System.out.println("(2) Modify existing entry");
                            System.out.println("(3) Leave table as is");
                            System.out.print("What would you like to do? (Type number): ");
                            String answer = scan.nextLine();
                            
                            if (answer.equals("1")) {
                            	System.out.print("Model Number: ");
                            	while (!scan.hasNextInt()) { // validate input value
	                                System.out.print("Please enter a positive integer: ");
	                                scan.nextLine();
	                            }
                            	int modelNumber = scan.nextInt();
                            	scan.nextLine(); // consume rest of line
                            	System.out.print("Sales Price: ");
                            	while (!scan.hasNextBigDecimal()) { // validate input value
                                    System.out.print("Please enter a valid dollar amount: ");
                                    scan.nextLine();
                                }
                        		BigDecimal salesPrice = scan.nextBigDecimal();
                        		scan.nextLine(); // consume rest of line
                            	
                            	insertIntoModel.setInt(1, modelNumber);
                            	insertIntoModel.setBigDecimal(2, salesPrice);
                            	insertIntoModel.executeUpdate();
                            } else if (answer.equals("2")) {
	                            System.out.print("Enter Model Number of entry to modify: ");
	                            while (!scan.hasNextInt()) { // validate input value
	                                System.out.print("Please enter a positive integer: ");
	                                scan.nextLine();
	                            }
	                            int modelNumber = scan.nextInt();
	                            scan.nextLine(); // consume rest of line
	                            
	                            PreparedStatement updateModel = null;
	                            String userAnswer = "";
	                            
	                            do {
	                            	System.out.println("\nOptions:");
	                            	System.out.println("(1) Model Number");
	                            	System.out.println("(2) Sales Price");
	                            	System.out.println("(3) Quit");
	                            	System.out.print("Which field to modify? (Type number): ");
	                            	userAnswer = scan.nextLine();
	                            	
	                            	switch (userAnswer) {
		                            	case "1":
		                            		System.out.print("Model Number: ");
		                            		while (!scan.hasNextInt()) { // validate input value
		    	                                System.out.print("Please enter a positive integer: ");
		    	                                scan.nextLine();
		    	                            }
		                            		int newModelNumber = scan.nextInt();
		                            		scan.nextLine(); // consume rest of line
		                            		
		                            		updateModel = engineerConn.prepareStatement(
		                            			"UPDATE Model SET ModelNumber = ? WHERE ModelNumber = ?");
		                            		updateModel.setInt(1, newModelNumber);
		                            		updateModel.setInt(2, modelNumber);
		                            		updateModel.executeUpdate();
		                            		modelNumber = newModelNumber;
		                            		break;
		                            	case "2":
		                            		System.out.print("Sales Price: ");
		                            		while (!scan.hasNextBigDecimal()) { // validate input value
		                                        System.out.print("Please enter a valid dollar amount: ");
		                                        scan.nextLine();
		                                    }
		                            		BigDecimal salesPrice = scan.nextBigDecimal();
		                            		scan.nextLine(); // consume rest of line
		                            		
		                            		updateModel = engineerConn.prepareStatement(
		                            			"UPDATE Model SET SalesPrice = ? WHERE ModelNumber = ?");
		                            		updateModel.setBigDecimal(1, salesPrice);
		                            		updateModel.setInt(2, modelNumber);
		                            		updateModel.executeUpdate();
		                            		break;
		                            	case "3":
		                            		break;
	                            		default:
	                            			System.out.println("Invalid field option.");
	                            			break;
	                            	}
	                            } while (!userAnswer.equals("3"));
                            } else if (!answer.equals("3")) {
                            	System.out.println("Invalid Option.");
                            }
                        } else if (userInput.equals("1")) {
                        	System.out.println("\nInventory Table:");
                            rs = engineerStmt.executeQuery(selectInventory);
                            while (rs.next()) {
                            	System.out.println(rs.getInt(1) + " " + rs.getBigDecimal(2) + " " + rs.getString(3) + " " + rs.getString(4) + " " + rs.getInt(5));
                            }
                            
                            System.out.println("\nModel Table Options:");
                            System.out.println("(1) Add new entry");
                            System.out.println("(2) Modify existing entry");
                            System.out.println("(3) Leave table as is");
                            System.out.print("What would you like to do? (Type number): ");
                            String answer = scan.nextLine();
                            
                            if (answer.equals("1")) {
                            	System.out.print("Item ID: ");
                        		while (!scan.hasNextInt()) { // validate input value
	                                System.out.print("Please enter a positive integer: ");
	                                scan.nextLine();
	                            }
                        		int itemID = scan.nextInt();
                        		scan.nextLine(); // consume rest of line
                        		System.out.print("Cost: ");
                        		while (!scan.hasNextBigDecimal()) { // validate input value
                                    System.out.print("Please enter a valid cost: ");
                                    scan.nextLine();
                                }
                        		BigDecimal cost = scan.nextBigDecimal();
                        		scan.nextLine(); // consume rest of line
                        		System.out.print("LeadTime - Enter Number (in Days): ");
                        		int leadTime = 0;
                        		while (leadTime <= 0) {
	                        		while (!scan.hasNextInt()) { // validate input value
		                                System.out.print("Please enter a positive integer: ");
		                                scan.nextLine();
		                            }
	                        		leadTime = scan.nextInt();
	                        		scan.nextLine(); // consume rest of line
                        		}
                        		
                        		System.out.print("Category Type: ");
                        		String categoryType = scan.nextLine();
                        		System.out.print("Category Number: ");
                        		while (!scan.hasNextInt()) { // validate input value
	                                System.out.print("Please enter a positive integer: ");
	                                scan.nextLine();
	                            }
                        		int categoryNumber = scan.nextInt();
                        		scan.nextLine(); // consume rest of line
                        		
                        		insertIntoInventory.setInt(1, itemID);
                        		insertIntoInventory.setBigDecimal(2, cost);
                        		insertIntoInventory.setInt(3, leadTime); // FIXME: leadTime breaks
                        		insertIntoInventory.setString(4, categoryType);
                        		insertIntoInventory.setInt(5, categoryNumber);
                        		insertIntoInventory.executeUpdate();
                            } else if (answer.equals("2")) {
	                            System.out.print("Enter Item ID of entry to modify: ");
	                            while (!scan.hasNextInt()) { // validate input value
	                                System.out.print("Please enter a positive integer: ");
	                                scan.nextLine();
	                            }
	                            int itemID = scan.nextInt();
	                            scan.nextLine(); // consume rest of line
	                            
	                            PreparedStatement updateInventory = null;
	                            String userAnswer = "";
	                            
	                            do {
	                            	System.out.println("\nOptions:");
	                            	System.out.println("(1) Item ID");
	                            	System.out.println("(2) Cost");
	                            	System.out.println("(3) Lead Time");
	                            	System.out.println("(4) Category Type");
	                            	System.out.println("(5) Category Number");
	                            	System.out.println("(6) Quit");
	                            	System.out.print("Which field to modify? (Type number): ");
	                            	userAnswer = scan.nextLine();
	                            	
	                            	switch (userAnswer) {
		                            	case "1":
		                            		System.out.print("Item ID: ");
		                            		while (!scan.hasNextInt()) { // validate input value
		    	                                System.out.print("Please enter a positive integer: ");
		    	                                scan.nextLine();
		    	                            }
		                            		int newItemID = scan.nextInt();
		                            		scan.nextLine(); // consume rest of line
		                            		
		                            		updateInventory = engineerConn.prepareStatement(
		                            			"UPDATE Inventory SET ItemID = ? WHERE ItemID = ?");
		                            		updateInventory.setInt(1, newItemID);
		                            		updateInventory.setInt(2, itemID);
		                            		updateInventory.executeUpdate();
		                            		itemID = newItemID;
		                            		break;
		                            	case "2":
		                            		System.out.print("Cost: ");
		                            		while (!scan.hasNextBigDecimal()) { // validate input value
		                                        System.out.print("Please enter a valid cost: ");
		                                        scan.nextLine();
		                                    }
		                            		BigDecimal cost = scan.nextBigDecimal();
		                            		scan.nextLine(); // consume rest of line
		                            		
		                            		updateInventory = engineerConn.prepareStatement(
		                            			"UPDATE Inventory SET Cost = ? WHERE ItemID = ?");
		                            		updateInventory.setBigDecimal(1, cost);
		                            		updateInventory.setInt(2, itemID);
		                            		updateInventory.executeUpdate();
		                            		break;
		                            	case "3":
		                            		System.out.print("LeadTime - Enter Number (in Days): ");
		                            		int leadTime = 0;
		                            		while (leadTime <= 0) {
		    	                        		while (!scan.hasNextInt()) { // validate input value
		    		                                System.out.print("Please enter a positive integer: ");
		    		                                scan.nextLine();
		    		                            }
		    	                        		leadTime = scan.nextInt();
		    	                        		scan.nextLine(); // consume rest of line
		                            		}
		                            		
		                            		updateInventory = engineerConn.prepareStatement(
		                            			"UPDATE Inventory SET LeadTime = INTERVAL '?' DAY "
		                            			+ "WHERE ItemID = ?");
		                            		updateInventory.setInt(1, leadTime);
		                            		updateInventory.setInt(2, itemID);
		                            		updateInventory.executeUpdate();
		                            		break;
		                            	case "4":
		                            		System.out.print("Category Type: ");
		                            		String categoryType = scan.nextLine();
		                            		
		                            		updateInventory = engineerConn.prepareStatement(
		                            			"UPDATE Inventory SET CategoryType = ? WHERE ItemID = ?");
		                            		updateInventory.setString(1, categoryType);
		                            		updateInventory.setInt(2, itemID);
		                            		updateInventory.executeUpdate();
		                            		break;
		                            	case "5":
		                            		System.out.print("Category Number: ");
		                            		while (!scan.hasNextInt()) { // validate input value
		    	                                System.out.print("Please enter a positive integer: ");
		    	                                scan.nextLine();
		    	                            }
		                            		int categoryNumber = scan.nextInt();
		                            		scan.nextLine(); // consume rest of line
		                            		
		                            		updateInventory = engineerConn.prepareStatement(
		                            			"UPDATE Inventory SET CategoryNumber = ? WHERE ItemID = ?");
		                            		updateInventory.setInt(1, categoryNumber);
		                            		updateInventory.setInt(2, itemID);
		                            		updateInventory.executeUpdate();
		                            		break;
		                            	case "6":
		                            		break;
	                            		default:
	                            			System.out.println("Invalid field option.");
	                            			break;
	                            	}
	                            } while (!userAnswer.equals("6"));
                            } else if (!answer.equals("3")) {
                            	System.out.println("Invalid Option.");
                            }
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
