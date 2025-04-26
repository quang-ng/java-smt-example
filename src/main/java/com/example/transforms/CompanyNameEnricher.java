package com.example.transforms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.Requirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompanyNameEnricher<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final Logger log = LoggerFactory.getLogger(CompanyNameEnricher.class);

    private Connection conn;

    @Override
    public void configure(Map<String, ?> configs) {
        try {
            String url = "jdbc:mysql://mysql:3306/demo";
            String user = "mysqluser";
            String password = "mysqlpw";
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to DB", e);
        }
    }

    @Override
    public R apply(R record) {
        if (record.value() == null) {
            return record;
        }

        // Get the value struct (envelope)
        Struct envelope = Requirements.requireStruct(record.value(), "CompanyNameEnricher");

        log.info("🔍 envelope {}", envelope);
        log.info("Incoming value schema: {}", record.valueSchema());
        log.info("Incoming value: {}", record.value());

        if (!envelope.schema().fields().stream().anyMatch(f -> f.name().equals("after"))) {
            log.warn("'after' field is missing in the record. Skipping.");
            return record; // or return null to skip
        }

        // Extract the "after" struct (the actual row data)
        Struct after = envelope.getStruct("after");

        log.info("🔍 after {}", after);

        if (after == null) {
            return record; // skip tombstone records or deletes
        }

        // Extract fields from "after"
        Integer companyId = after.getInt32("company");
        String name = after.getString("name");

        log.info("🔍 Extracted name: {}, company: {}", name, companyId);

        // Enrich with company name (example)
        String companyName = getCompanyName(companyId);

        log.info("🔍 Enriching with company name: {}", companyName);

        Schema employeeSchema = SchemaBuilder.struct()
                .name("com.example.Employee")
                .field("name", Schema.STRING_SCHEMA)
                .field("company", Schema.INT32_SCHEMA)
                .field("company_name", Schema.OPTIONAL_STRING_SCHEMA)
                .build();

        Struct employee = new Struct(employeeSchema)
                .put("name", name)
                .put("company", companyId)
                .put("company_name", companyName);

        log.info("🔍 employee {}", employee);

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                employee.schema(),
                employee,
                record.timestamp());
    }

    private String getCompanyName(Integer companyId) {
        log.info("🔍 getCompanyName {}", companyId);
        if (companyId == null) {
            return null;
        }

        String query = "SELECT name FROM companies WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query company name for companyId: {}", companyId, e);
        }

        return null;
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef(); // define any config if needed
    }

    @Override
    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("Failed to close DB connection", e);
        }
    }

}
