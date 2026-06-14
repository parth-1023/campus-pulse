import csv
import datetime
import random
import os

output_dir = os.path.abspath(os.path.dirname(__file__))
output_path = os.path.join(output_dir, "oulu_telemetry.csv")

# Generate 5000 rows of telemetry data representing ~3 days of readings (every 1 minute)
start_time = datetime.datetime.now() - datetime.timedelta(days=3)
rows = []

print("Generating high-fidelity simulated Oulu campus telemetry data...")

for i in range(5000):
    curr_time = start_time + datetime.timedelta(minutes=i)
    # Simulate realistic CO2 levels (diurnal pattern + random noise + occasional anomalies)
    hour = curr_time.hour
    is_work_hour = 8 <= hour <= 18
    
    # Base CO2 (higher during work hours, lower at night)
    base_co2 = 650 if is_work_hour else 420
    noise = random.uniform(-50, 50)
    co2 = base_co2 + noise
    
    # Motion probability is higher during work hours
    motion = 1 if (is_work_hour and random.random() < 0.7) or (not is_work_hour and random.random() < 0.05) else 0
    
    # Inject some anomalies (high CO2 and high motion)
    # Day 6 trigger rule: co2 > 1500 and motion == 1
    if random.random() < 0.02:  # 2% chance of anomaly
        co2 = random.uniform(1550, 1900)
        motion = 1
        
    temp = random.uniform(20.5, 23.5) + (1.5 if is_work_hour else -1.0)
    humidity = random.uniform(40.0, 55.0) - (5.0 if is_work_hour else -2.0)
    
    rows.append({
        "time": curr_time.strftime("%Y-%m-%d %H:%M:%S"),
        "co2": round(co2, 1),
        "temperature": round(temp, 1),
        "humidity": round(humidity, 1),
        "motion": motion
    })

with open(output_path, "w", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["time", "co2", "temperature", "humidity", "motion"])
    writer.writeheader()
    writer.writerows(rows)

print(f"Successfully generated {len(rows)} telemetry rows in {output_path}")
