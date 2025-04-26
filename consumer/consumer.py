import json
import time

import mysql.connector
from kafka import KafkaConsumer

TARGET_DB_CONFIG = {
    "host": "target-db",
    "port": 3306,
    "user": "mysqluser",
    "password": "mysqlpw",
    "database": "demo"
}



def consumer_message():
    time.sleep(20)
    consumer = KafkaConsumer(
        'mysqlserver.demo.employee',
        bootstrap_servers='kafka:9092',
        auto_offset_reset='earliest',
        value_deserializer=lambda m: json.loads(m.decode('utf-8')),
        group_id='consumer-group-1'
    )


    print("👂 Listening for messages 22...")
    for msg in consumer:
        event = msg.value
        print("event", event)
    

        name = event.get('name')
        company_id = event.get('company')
        company_name = event.get('company_name', 'N/A')

        print(f"👤 {name} works at Company ID {company_id} company name {company_name}")

        if company_name == 'N/A':
            print("Company name not found, skipping insert.")
            continue

        conn = mysql.connector.connect(**TARGET_DB_CONFIG)
        cursor = conn.cursor()

        try:
            # Check if the company already exists in the database
            cursor.execute("SELECT id FROM company WHERE name = %s", (company_name,))
            result = cursor.fetchone()

            target_company_id = None
            if result:
                print(f"Company '{company_name}' already exists with ID {result[0]}.")
                target_company_id = result[0]
            else:
                print(f"Company '{company_name}' not found, inserting into database.")
                cursor.execute("INSERT INTO company (name) VALUES (%s)", (company_name,))
                conn.commit()
                print(f"Inserted company '{company_name}' with ID {cursor.lastrowid}.")
                target_company_id = cursor.lastrowid
            # Insert the employee into the database
            cursor.execute(
                "INSERT INTO employee (name, company) VALUES (%s, %s)",
                (name, target_company_id)
            )
            conn.commit()
            print(f"Inserted employee '{name}' with ID {cursor.lastrowid}.")
        except mysql.connector.Error as err:
            print(f"Error: {err}")
        finally:
            cursor.close()
            conn.close()
            print("Connection closed.")
            print("Cursor closed.")

if __name__ == "__main__":
    consumer_message()
