# Telemetry simulator script
import csv
import json
import random
import time
import os
import pika

# CERN Meyrin center point coordinates
CERN_LAT = 46.2344
CERN_LON = 6.0459

def main():
    # File paths
    dir_path = os.path.dirname(os.path.realpath(__file__))
    csv_path = os.path.join(dir_path, "oulu_telemetry.csv")
    
    if not os.path.exists(csv_path):
        print(f"Error: Telemetry CSV file not found at {csv_path}. Please run generate_oulu_data.py first.")
        return

    # 1. Connect to RabbitMQ broker (defaults to local docker-compose port 5673)
    rabbitmq_url = os.environ.get('RABBITMQ_PRIVATE_URL', os.environ.get('RABBITMQ_URL', 'amqp://guest:guest@localhost:5673'))
    # Hide password in logs if present
    display_url = rabbitmq_url.split('@')[-1] if '@' in rabbitmq_url else rabbitmq_url
    print(f"Connecting to RabbitMQ at {display_url}...")
    parameters = pika.URLParameters(rabbitmq_url)
    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()

    # 2. Declare a Topic Exchange
    exchange_name = 'campus.telemetry.exchange'
    channel.exchange_declare(exchange=exchange_name, exchange_type='topic', durable=True)
    print(f"Declared exchange: '{exchange_name}'")

    print("Starting telemetry stream. Press Ctrl+C to stop...")
    
    try:
        with open(csv_path, 'r') as f:
            reader = csv.DictReader(f)
            
            for row in reader:
                # Add random spatial variation around the CERN Meyrin campus bounding box
                lat = CERN_LAT + random.uniform(-0.005, 0.005)
                lon = CERN_LON + random.uniform(-0.008, 0.008)
                
                # Format JSON telemetry packet
                payload = {
                    "timestamp": row["time"],
                    "co2": float(row["co2"]),
                    "temperature": float(row["temperature"]),
                    "humidity": float(row["humidity"]),
                    "motion": int(row["motion"]),
                    "latitude": round(lat, 6),
                    "longitude": round(lon, 6)
                }
                
                message = json.dumps(payload)
                routing_key = 'campus.sensor.telemetry'
                
                # 3. Publish payload to RabbitMQ
                channel.basic_publish(
                    exchange=exchange_name,
                    routing_key=routing_key,
                    body=message,
                    properties=pika.BasicProperties(
                        content_type='application/json',
                        delivery_mode=2 # Make message persistent
                    )
                )
                
                print(f"[x] Sent telemetry: {message}")
                
                # Stream at a rate of 1 message per second
                time.sleep(1.0)
                
    except KeyboardInterrupt:
        print("\nStopping telemetry stream...")
    finally:
        connection.close()
        print("RabbitMQ connection closed.")

if __name__ == '__main__':
    main()

