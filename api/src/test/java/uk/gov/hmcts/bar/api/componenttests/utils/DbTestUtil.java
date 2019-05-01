package uk.gov.hmcts.bar.api.componenttests.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.auth.checker.spring.useronly.UserDetails;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class DbTestUtil {

    private static final String PROPERTY_KEY_RESET_SQL_TEMPLATE = "test.reset.sql.template";
    private static final String PROPERTY_KEY_INSERT_USER_SQL_TAMPLATE = "test.user.sql.template";

    private static final String INSERT_PI_QUERY_PI_STATS =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub) VALUES (1,'John Doe',null,'POSTAL_ORDER',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',true);\n" +
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub,payhub_error) VALUES (2,'John Doe',null,'POSTAL_ORDER',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',false,'some error');";
    private static final String INSERT_PIS_QUERY_PI_STATS =
        "INSERT INTO payment_instruction_status values (1,'TTB','1234',CURRENT_TIMESTAMP);\n" +
        "INSERT INTO payment_instruction_status values (2,'TTB','1234',CURRENT_TIMESTAMP);";

    private static final String INSERT_CARD_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub) VALUES (1,'John Doe',null,'CARD',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',true);";
    private static final String INSERT_FR_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub,remission_reference) VALUES (1,'John Doe',null,'CARD',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',true,'01234567891');\n"+
        "INSERT INTO case_fee_detail (case_fee_id,payment_instruction_id,fee_code,amount,fee_description,fee_version,case_reference,remission_amount,remission_benefiter,remission_authorisation,refund_amount) VALUES (1,1,'X0165',55000,'Filing an application for a divorce, nullity or civil partnership dissolution – fees order 1.2.','1','12345',null,null,null,null);";
    private static final String INSERT_PO_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub) VALUES (1,'John Doe',null,'POSTAL_ORDER',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',true);\n"+
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub) VALUES (2,'John Doe',null,'POSTAL_ORDER',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y610',1,'Process','1234','123456',true);";
    private static final String INSERT_CARD_PI_QUERY_TRANSFERRED_TO_PAYHUB_FAIL =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub,payhub_error) VALUES (1,'John Doe',null,'CARD',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Process','1234','123456',false,'some error');";
    private static final String INSERT_BGC_QUERY =
        "INSERT INTO bank_giro_credit values (123456,'BR01',CURRENT_TIMESTAMP);";
    private static final String INSERT_BAR_USER =
        "INSERT INTO bar_user values ('1234','1234-fn','1234-ln','post.clerk@hmcts.net','bar_post_clerk');";
    private static final String INSERT_PIS_QUERY =
        "INSERT INTO payment_instruction_status values (1,'D','1234',CURRENT_TIMESTAMP);";
    private static final String INSERT_PI_QUERY =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id) VALUES (1,'John Doe',null,'CARD',{ts '2018-03-25 23:32:23.871'},50000,'GBP',null,'TTB',null,'Y431',1,'Process','1234');\n" +
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id) VALUES (2,'John Doe',null,'CARD',{ts '2018-03-25 23:36:11.207'},50000,'GBP','123','P',null,'Y431',2,null,'4321');\n" +
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id) VALUES (3,'John Doe',null,'CASH',{ts '2018-03-25 23:36:11.207'},50000,'GBP',null,'TTB',null,'Y431',3,'Process','4321');\n" +
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id) VALUES (4,'Jane Doe',null,'CHEQUE',{ts '2018-03-25 23:36:11.207'},50000,'GBP',null,'RDM',null,'Y431',3,'Process','5678');\n" +
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,authorization_code,user_id,bgc_number,transferred_to_payhub,payhub_error,report_date,action_reason,action_comment) VALUES (891,'Brett','312323','CHEQUE',{ts '2018-11-08 17:08:34.391'},55000,'GBP',null,'TTB',null,'Y431',2,'Process',null,'4321','123456',false,null,null,null,null);" +
        "INSERT INTO case_fee_detail (case_fee_id,payment_instruction_id,fee_code,amount,fee_description,fee_version,case_reference,remission_amount,remission_benefiter,remission_authorisation,refund_amount) VALUES (1,1,'X0165',55000,'Filing an application for a divorce, nullity or civil partnership dissolution – fees order 1.2.','1','12345',5000,'sdfsf','dsfsf',null);";
    private static final String INSERT_CARD_PI_QUERY_RETURNED_PAYMENT =
        "INSERT INTO payment_instruction (id,payer_name,cheque_number,payment_type_id,payment_date,amount,currency,all_pay_transaction_id,status,postal_order_number,site_id,daily_sequence_id,action,user_id,bgc_number,transferred_to_payhub) VALUES (1,'John Doe',null,'CARD',{ts '2018-03-25 23:32:23.871'},600,'GBP',null,'TTB',null,'Y431',1,'Return','1234','123456',false);";

    private static final String INSERT_STATUS_HISTORY =
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (1,'TTB','1234',{ts '2019-01-21 20:48:40.0'});" +
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (2,'P','1234',{ts '2019-01-21 20:48:40.0'});" +
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (3,'TTB','1234',{ts '2019-01-21 20:48:40.0'});" +
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (4,'R','1234',{ts '2019-01-21 20:48:40.0'});" +
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (4,'RDM','dm-manager',{ts '2019-01-21 20:48:40.0'});" +
        "INSERT INTO payment_instruction_status (payment_instruction_id,status,bar_user_id,update_time) VALUES (891,'TTB','1234',{ts '2019-01-21 20:48:40.0'});";
    /**
     * This method reads the invoked SQL statement template from a properties file, creates
     * the invoked SQL statements, and invokes them.
     *
     * @param applicationContext    The application context that is used by our tests.
     * @param tableNames            The names of the database tables which auto-increment column will be reseted.
     * @throws SQLException         If a SQL statement cannot be invoked for some reason.
     */
    public static void resetAutoIncrementColumns(ApplicationContext applicationContext,
                                                 String... tableNames) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        String resetSqlTemplate = getSqlTemplate(applicationContext, PROPERTY_KEY_RESET_SQL_TEMPLATE);
        try (Connection dbConnection = dataSource.getConnection()) {
            //Create SQL statements that reset the auto-increment columns and invoke
            //the created SQL statements.
            for (String resetSqlArgument: tableNames) {
                try (Statement statement = dbConnection.createStatement()) {
                    String resetSql = String.format(resetSqlTemplate, resetSqlArgument);
                    statement.execute(resetSql);
                }
            }
        }
    }

    private static String getSqlTemplate(ApplicationContext applicationContext, String key) {
        //Read the SQL template from the properties file
        Environment environment = applicationContext.getBean(Environment.class);
        return environment.getRequiredProperty(key);
    }

    public static void setTestUser(ApplicationContext applicationContext, UserDetails userDteails) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        String insertUserSqlTemplate = getSqlTemplate(applicationContext, PROPERTY_KEY_INSERT_USER_SQL_TAMPLATE);
        try (Connection dbConnection = dataSource.getConnection();Statement stmt = dbConnection.createStatement() ) {

            String[] args1 = new String[]{"'" + userDteails.getUsername() + "-fn'",
                "'" + userDteails.getUsername() + "-ln'", "'" + userDteails.getUsername() + "'", "'" + userDteails.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(", "))  + "'"};
            String[] args2 = Arrays.copyOf(args1, args1.length);
            args2[0] = "'Jane'";
            args2[2] = "'4321'";
            String query = "truncate table bar_user;";
            query += String.format(insertUserSqlTemplate, args1);
            query += String.format(insertUserSqlTemplate, args2);
            stmt.executeQuery(query);
        }
    }

    public static void addTestUser(ApplicationContext applicationContext, UserDetails userDetails) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        String insertUserSqlTemplate = getSqlTemplate(applicationContext, PROPERTY_KEY_INSERT_USER_SQL_TAMPLATE);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {

            String[] columnValues = new String[] { "'" + userDetails.getUsername() + "-fn'",
                "'" + userDetails.getUsername() + "-ln'", "'" + userDetails.getUsername() + "'",
                "'" + userDetails.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(", "))
                    + "'" , "'" + userDetails.getUsername() + "@hmcts.net'" };
            stmt.executeQuery(String.format(insertUserSqlTemplate, columnValues));
        }
    }

    public static void insertPaymentInstructions(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {

            emptyTable(applicationContext, "case_fee_detail");
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "payment_instruction");
            insertBGCNumber(applicationContext);
            stmt.executeQuery(INSERT_PI_QUERY);
            stmt.executeQuery(INSERT_STATUS_HISTORY);
        }
    }

    public static void insertBarUser(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "bar_user");
            stmt.executeQuery(INSERT_BAR_USER);
        }
    }

    public static void insertCardPaymentInstructionWhichIsSentToPayhub(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_CARD_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY);
        }
    }
    public static void insertCardPaymentInstructionWhichIsSentToPayhubAndFailed(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_CARD_PI_QUERY_TRANSFERRED_TO_PAYHUB_FAIL);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY);
        }
    }

    public static void insertCardPaymentInstructionWithActionReturned(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_CARD_PI_QUERY_RETURNED_PAYMENT);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY);
        }
    }

    public static void insertPOPaymentInstructionWhichIsSenttoPayhub(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_PO_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY);
        }
    }

    public static void insertPaymentInstructionForPIStats(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_PI_QUERY_PI_STATS);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY_PI_STATS);
        }
    }

    public static void insertFRPaymentInstructionWhichIsSentToPayhub(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {
            emptyTable(applicationContext, "payment_instruction_status");
            emptyTable(applicationContext, "bar_user");
            emptyTable(applicationContext, "payment_instruction");

            stmt.executeQuery(INSERT_FR_PI_QUERY_TRANSFERRED_TO_PAYHUB_YES);
            stmt.executeQuery(INSERT_BAR_USER);
            stmt.executeQuery(INSERT_PIS_QUERY);
        }
    }

    public static void insertBGCNumber(ApplicationContext applicationContext) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection(); Statement stmt = dbConnection.createStatement()) {

            emptyTable(applicationContext, "bank_giro_credit");
            stmt.executeQuery(INSERT_BGC_QUERY);
        }
    }

    public static void emptyTable(ApplicationContext applicationContext, String tableName) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection();Statement stmt = dbConnection.createStatement()) {
            stmt.executeQuery("truncate table " + tableName);
        }
    }

    public static void toggleSendToPayhub(ApplicationContext applicationContext, boolean enabled) throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        try (Connection dbConnection = dataSource.getConnection();Statement stmt = dbConnection.createStatement()) {
            stmt.executeQuery("update ff4j_features set enable = " + (enabled ? 1 : 0) + " where feat_uid = 'send-to-payhub';");
        }
    }
}

