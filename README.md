# JAVE-SMT-EXMPLE

This project implements a custom Kafka Connect Single Message Transform (SMT) named `CompanyNameEnricher`. Its purpose is to enrich CDC (Change Data Capture) events from a MySQL `employee` table by adding the corresponding `company_name`, which is fetched in real time from the same MySQL database.

## **Architecture**

The solution consists of the following components:

- **MySQL Database**: Source for both change events and the lookup table `company`.
- **Debezium MySQL Connector**: Captures CDC events from the MySQL `employee` table and pushes them to a Kafka topic.
- **Kafka**: Messaging system that stores CDC events and distributes them to consumers.
- **Kafka Connect**: Hosts the custom SMT and enriches events as they pass through.
- **Custom SMT (`CompanyNameEnricher`)**: Java-based SMT that performs a database lookup to append the `company_name`.
- **Kafka Consumer**: Python script that prints enriched messages.

# **Project Structure**

The project is organized as follows:

```
realtime-data-pull/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── CompanyNameEnricher.java  # Custom SMT implementation
├── scripts/
│   └── consumer.py  # Python script to consume enriched messages
├── README.md  # Project documentation
├── pom.xml  # Maven configuration for building the Java project
└── Dockerfile  # Docker configuration for deploying the SMT
```

### Key Files and Directories

- **`src/main/java/com/example/CompanyNameEnricher.java`**: Contains the implementation of the custom Kafka Connect SMT.
- **`scripts/consumer.py`**: A Python script to consume enriched messages from Kafka topics.
- **`resources/application.properties`**: Configuration file for database connection and Kafka settings.
- **`Dockerfile`**: Used to containerize the SMT for deployment in a Kafka Connect cluster.

## Custom SMT: CompanyNameEnricher

The `CompanyNameEnricher` is a Kafka Connect **Single Message Transform (SMT)** designed to **enrich records** from a Kafka topic (usually coming from Debezium) by adding a `company_name` field. It looks up the company name **in real-time** from a **MySQL database** using the `company` ID already present in the CDC record. The SMT sits in the **middle**, enhancing the data *before* it lands in the topic.

### Assumptions

- **Company Name is Unique**: It is assumed that the `company_name` field in the `company` table is unique. This ensures that there are no ambiguities when performing lookups based on `company_id`.
- **Low Latency Database Access**: The MySQL database is expected to handle real-time queries with low latency to avoid bottlenecks in the enrichment process.
- **CDC Events are Well-Formatted**: The incoming CDC events from Debezium are expected to have a valid `company_id` field.

### Example Workflow

Imagine a Kafka Connect pipeline that:

1. Gets data from MySQL using **Debezium.** Example message:
    
    ```json
    {
        "id": 5,
        "name": "Washi",
        "company_id": 2
    }
    ```
    
2. Sends it through your **custom SMT (`CompanyNameEnricher`)**. In this step, the SMT queries the source database to fetch the company name based on `company_id` and updates the record.

3. Publishes it to a Kafka topic with the updated record. The new record looks like this:
    
    ```json
    {
        "id": 5,
        "name": "Washi",
        "company_name": "Acme Corp"
    }
    ```

## **Kafka Consumer**

- Subscribes to a topic (like `mysqlserver.demo.employee`).
- Reads enriched records (messages) like this:
    
    ```json
    {
        "id": 5,
        "name": "Washi",
        "company_name": "Acme Corp"
    }
    ```
    
- Finds the company in the target database by `company_name`.
- Inserts a new employee into the target database with the corresponding company.

## **Error Handling**

- If the `company_id` does not exist in the `company` table, the SMT logs an error and skips the record.
- If the database query fails due to connectivity issues, the SMT retries a configurable number of times before failing.
