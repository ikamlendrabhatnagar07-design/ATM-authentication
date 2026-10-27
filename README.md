# Java ATM Management System (MySQL)

A beginner-friendly console ATM application built with Java 17, JDBC, and MySQL. It does **not** use SQLite.

## Features

- Verify account number and PIN (up to 3 attempts)
- Check account balance
- Deposit money
- Withdraw money with insufficient-funds validation
- Store every successful deposit/withdrawal in transaction history
- Use prepared statements and database transactions for banking operations
- View transaction history

> **Learning project note:** The demo uses a plain-text PIN to keep the database example easy to understand. Do not use this authentication design for a real banking application. Real systems should hash PINs securely, add rate limiting, and use stronger authentication and operational controls.

## Requirements

- JDK 17 or newer
- MySQL Server 8.x (or compatible version)
- Maven
- VS Code with Java Extension Pack (optional, but useful)

## 1. Create the database

1. Start MySQL Server.
2. Open MySQL Workbench or the MySQL command-line client.
3. Run the complete script in `database/schema.sql`.

It creates `atm_db`, `accounts`, and `transaction_history`, plus a demo account.

**Demo login**
- Account number: `1001001`
- PIN: `1234`
- Starting balance: ₹5000.00

Use only this sample account for local testing.

## 2. Configure the database connection

The default connection is:

- URL: `jdbc:mysql://localhost:3306/atm_db`
- Username: `root`
- Password: empty

If your MySQL setup uses a different username or password, set environment variables before running the app. Do not commit your real database password to GitHub.

### Windows PowerShell (current terminal only)

```powershell
$env:ATM_DB_USER="root"
$env:ATM_DB_PASSWORD="your_mysql_password"
$env:ATM_DB_URL="jdbc:mysql://localhost:3306/atm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

Replace `your_mysql_password` with your own local MySQL password. If the password is empty, you can skip setting `ATM_DB_PASSWORD`.

## 3. Run the application

Open PowerShell in the project root (the folder containing `pom.xml`) and run:

```powershell
mvn clean compile
mvn exec:java -Dexec.mainClass="com.atm.ATMApplication"
```

If Maven's `exec:java` goal is unavailable because the exec plugin has not been configured, add this plugin inside the `build/plugins` section of `pom.xml`, then run the same command:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
</plugin>
```

Alternatively, run `com.atm.ATMApplication` using the VS Code Java extension after Maven has downloaded dependencies.

## Project structure

```text
atm-java-mysql/
├── database/
│   └── schema.sql
├── src/main/java/com/atm/
│   ├── ATMApplication.java
│   └── Database.java
├── .gitignore
├── pom.xml
└── README.md
```

## How to test

1. Log in with `1001001` / `1234`.
2. Check balance (starts at ₹5000.00).
3. Deposit ₹500; balance should become ₹5500.00.
4. Withdraw ₹700; balance should become ₹4800.00.
5. Try withdrawing more than the balance; it should show insufficient funds.
6. Open transaction history and verify successful transactions appear.
7. Try a wrong PIN three times; the program should exit.

## Push to GitHub

Create a **new empty repository** on GitHub, for example `atm-java-mysql`. Do not add a README or `.gitignore` on GitHub because this project already contains them.

From PowerShell, in this project's root folder:

```powershell
git init
git add .
git commit -m "Build Java ATM application with MySQL"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/atm-java-mysql.git
git push -u origin main
```

Replace `YOUR-USERNAME` with your GitHub username. GitHub may open a browser for authentication. Never put a GitHub password or token inside your Java code.

If Git says `remote origin already exists`, use:

```powershell
git remote set-url origin https://github.com/YOUR-USERNAME/atm-java-mysql.git
git push -u origin main
```

## Common problems

- **`mvn` is not recognized:** install Maven, add it to PATH, then reopen PowerShell. Confirm with `mvn -version`.
- **Connection refused:** make sure MySQL Server is running and port `3306` is correct.
- **Access denied:** verify `ATM_DB_USER` and `ATM_DB_PASSWORD`.
- **Unknown database `atm_db`:** run `database/schema.sql` first.
- **Git is not recognized:** install Git for Windows and reopen PowerShell.
