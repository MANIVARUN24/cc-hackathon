# Manufacturing Plant Monitor
## AWS Direct Connect Design Study for a Manufacturing Plant

---

## Project Overview

This project is an **end-to-end IoT monitoring system** for a manufacturing plant. It demonstrates:

1. **Real-time sensor data collection** using an Arduino UNO with physical sensors
2. **Cloud connectivity** via AWS IoT Core (MQTT over TLS)
3. **Backend data processing and storage** using Spring Boot and MySQL
4. **Live web dashboard** using React

The system is the **prototype implementation** for a larger AWS Direct Connect production architecture study. See the [Direct Connect Production Architecture](#direct-connect-production-architecture) section for the distinction between the prototype and production design.

---

## Architecture Diagram

### Prototype (This Implementation)

```
Arduino UNO (COM9)
    │ USB Serial @ 9600 baud
    ▼
Python Gateway
    │ MQTT over TLS (port 8883)
    ▼
AWS IoT Core
Topic: sdk/test/python
Endpoint: a13zqwx75mhfqk-ats.iot.us-east-1.amazonaws.com
    │ MQTT subscription
    ▼
Spring Boot Backend (localhost:8080)
    │ JDBC
    ▼
MySQL Database
manufacturing_monitor.sensor_readings
    │ REST API
    ▼
React Dashboard (localhost:5173)
```

---

## Arduino Sensors

| Sensor     | Model    | Pin(s)           | Data                        |
|------------|----------|------------------|-----------------------------|
| Gas/Smoke  | MQ-2     | Analog A0, D7    | Raw value (0–1023), ALERT/NORMAL |
| Temp/Humid | DHT11    | D2               | Temperature °C, Humidity %  |
| Distance   | HC-SR04  | TRIG D9, ECHO D10| Distance in cm              |
| Display    | 16x2 LCD | I2C (0x27)       | Local display only          |

---

## Arduino Serial Protocol

The Arduino outputs serial data at **9600 baud** on **COM9** in this exact format:

```
Gas Value: 635
Gas Status: ALERT
Temperature: 31.8 C
Humidity: 70.0 %
Distance: 6.29 cm
```

**Gas Status Logic:**
- MQ-2 digital pin **LOW** → `Gas Status: ALERT`
- MQ-2 digital pin **HIGH** → `Gas Status: NORMAL`

---

## Python Gateway

The existing Python gateway:
1. Connects to Arduino at COM9 (9600 baud)
2. Parses the serial output
3. Creates JSON payload
4. Publishes to AWS IoT Core via MQTT over TLS

**MQTT Topic:** `sdk/test/python`

**JSON payload format:**
```json
{
  "device": "ManufacturingPlantMonitor",
  "plant": "Manufacturing Plant",
  "gas_value": 635,
  "gas_status": "ALERT",
  "temperature_c": 31.8,
  "humidity_percent": 70.0,
  "distance_cm": 6.29,
  "timestamp": "2026-10-03 19:07:36"
}
```

---

## AWS IoT Core

| Setting     | Value                                              |
|-------------|----------------------------------------------------|
| Endpoint    | `a13zqwx75mhfqk-ats.iot.us-east-1.amazonaws.com`  |
| Region      | `us-east-1`                                        |
| Topic       | `sdk/test/python`                                  |
| Python clientId | `basicPubSub`                                  |
| Backend clientId | `SpringBootManufacturingBackend`              |
| Port        | `8883` (MQTT over TLS)                             |

**Certificate files (existing, at `C:\Users\maniv\Downloads\connect_device_package\`):**
```
ManufacturingPlantMonitor.cert.pem
ManufacturingPlantMonitor.private.key
root-CA.crt
```

---

## Spring Boot Backend

- **Language:** Java 17
- **Framework:** Spring Boot 3.2
- **Build:** Maven
- **Port:** 8080
- **Package:** `com.manufacturing.backend`

### How it works

1. On startup, connects to AWS IoT Core using mutual TLS (mTLS)
2. Subscribes to `sdk/test/python`
3. On each message: deserializes JSON → validates → saves to MySQL
4. Exposes REST API for the React dashboard

### Key files

| File | Purpose |
|------|---------|
| `config/MqttConfig.java` | TLS connection to AWS IoT Core |
| `mqtt/MqttSensorListener.java` | Receives and processes MQTT messages |
| `service/SensorService.java` | Validation and database persistence |
| `controller/SensorController.java` | REST API endpoints |
| `controller/HealthController.java` | Health check endpoint |
| `entity/SensorReading.java` | JPA entity / MySQL table |

---

## MySQL Database

- **Database:** `manufacturing_monitor`
- **Table:** `sensor_readings`

| Column             | Type         | Notes                        |
|--------------------|--------------|------------------------------|
| id                 | BIGINT PK AI | Auto-generated               |
| device             | VARCHAR(100) | e.g. "ManufacturingPlantMonitor" |
| plant              | VARCHAR(150) | e.g. "Manufacturing Plant"   |
| gas_value          | INT          | MQ-2 raw value (0–1023)      |
| gas_status         | VARCHAR(30)  | "ALERT" or "NORMAL"          |
| temperature_c      | DOUBLE       | DHT11 temperature            |
| humidity_percent   | DOUBLE       | DHT11 humidity               |
| distance_cm        | DOUBLE       | HC-SR04 distance             |
| reading_timestamp  | DATETIME     | Timestamp from Python gateway |
| created_at         | TIMESTAMP    | DB insertion time            |

---

## React Dashboard

- **Framework:** React + Vite
- **Port:** 5173
- **Charts:** Recharts (real data only)
- **Polling:** Latest reading every 5 seconds, history every 15 seconds

### Pages & Components

| Component | Purpose |
|-----------|---------|
| `Header` | Title, subtitle, MQTT/DB/Backend status |
| `AlertPanel` | Prominent warning when `gas_status = ALERT` |
| `SensorCard` | Individual metric card (temp, humidity, gas, distance) |
| `SensorCharts` | Time-series charts for all 4 sensors |
| `SensorTable` | Historical readings table |
| `StatusBadge` | Inline ALERT/NORMAL badge |

---

## REST API Reference

All endpoints are on `http://localhost:8080/api`.

### `GET /api/health`
System health status.
```json
{
  "status": "UP",
  "mqtt": "CONNECTED",
  "database": "CONNECTED",
  "mqttMessagesReceived": 42
}
```

### `GET /api/sensors/latest`
Most recent sensor reading.

### `GET /api/sensors?limit=50`
Recent readings (default 50, max 500).

### `GET /api/sensors/history?limit=200`
Historical readings, newest first (default 200, max 1000).

### `GET /api/sensors/alerts?limit=100`
Readings where `gas_status = 'ALERT'`.

### `GET /api/sensors/stats`
Aggregate statistics:
```json
{
  "totalReadings": 150,
  "alertCount": 23,
  "normalCount": 127,
  "alertPercentage": 15,
  "averageTemperatureC": 31.4,
  "averageHumidityPercent": 68.2,
  "maxGasValue": 635,
  "averageDistanceCm": 12.4,
  "latestGasStatus": "ALERT",
  "latestReadingTimestamp": "2026-10-03 19:07:36"
}
```

---

## Installation

### Prerequisites

- Java 17 (JDK)
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+
- Python 3.x with existing gateway dependencies
- Arduino IDE (for the existing Arduino code)

### 1. Database Setup

```sql
-- Option A: Use the provided schema script
mysql -u root -p < database/schema.sql

-- Option B: Spring Boot will auto-create the table on first run
-- (spring.jpa.hibernate.ddl-auto=update)
```

For UI testing without hardware (optional):
```sql
mysql -u root -p manufacturing_monitor < database/sample-data.sql
```

### 2. Backend Configuration

**Option A: Use defaults (certificates at the default path)**

The `application.properties` file already points to:
```
C:/Users/maniv/Downloads/connect_device_package/
```
If your certificates are there, no changes needed.

**Option B: Use environment variables**

Set these before starting Spring Boot:
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:AWS_IOT_CERTIFICATE = "C:/path/to/ManufacturingPlantMonitor.cert.pem"
$env:AWS_IOT_PRIVATE_KEY  = "C:/path/to/ManufacturingPlantMonitor.private.key"
$env:AWS_IOT_ROOT_CA      = "C:/path/to/root-CA.crt"
```

**Option C: Create application-local.properties (gitignored)**

```properties
# manufacturing-backend/src/main/resources/application-local.properties
spring.datasource.password=your_mysql_password
aws.iot.certificate=C:/path/to/cert.pem
aws.iot.private-key=C:/path/to/key.pem
aws.iot.root-ca=C:/path/to/root-CA.crt
```

Then run with: `mvn spring-boot:run -Dspring-boot.run.profiles=local`

---

## AWS IoT Policy Requirement

The Spring Boot backend uses a **different client ID** (`SpringBootManufacturingBackend`) from the Python gateway (`basicPubSub`).

Your existing AWS IoT policy must allow this client ID to:
- `iot:Connect` with clientId `SpringBootManufacturingBackend`
- `iot:Subscribe` to `sdk/test/python`
- `iot:Receive` from `sdk/test/python`

**Example policy addition:**
```json
{
  "Effect": "Allow",
  "Action": ["iot:Connect"],
  "Resource": "arn:aws:iot:us-east-1:*:client/SpringBootManufacturingBackend"
},
{
  "Effect": "Allow",
  "Action": ["iot:Subscribe"],
  "Resource": "arn:aws:iot:us-east-1:*:topicfilter/sdk/test/python"
},
{
  "Effect": "Allow",
  "Action": ["iot:Receive"],
  "Resource": "arn:aws:iot:us-east-1:*:topic/sdk/test/python"
}
```

If your existing policy uses a wildcard client (`*`), no changes are needed.

---

## Running the System

Start components **in this exact order**:

### Step 1 — Arduino
1. Connect Arduino UNO to **COM9** via USB
2. Open Arduino IDE, verify/upload the existing sketch
3. Open Serial Monitor at **9600 baud**
4. Confirm output:
   ```
   Gas Value: 635
   Gas Status: ALERT
   Temperature: 31.8 C
   Humidity: 70.0 %
   Distance: 6.29 cm
   ```

### Step 2 — Python Gateway
```bash
python your_gateway_script.py
```
Confirm it connects and publishes to `sdk/test/python`.

### Step 3 — Verify AWS IoT Core
In AWS Console → IoT Core → MQTT Test Client:
- Subscribe to `sdk/test/python`
- Confirm JSON messages appear

### Step 4 — MySQL
```powershell
# Start MySQL service (if not already running)
net start MySQL80
```
Ensure `manufacturing_monitor` database exists (run schema.sql if needed).

### Step 5 — Spring Boot Backend
```powershell
cd manufacturing-backend
mvn spring-boot:run
```

Expected startup log:
```
Connecting to AWS IoT Core: ssl://a13zqwx75mhfqk-ats.iot.us-east-1.amazonaws.com:8883
Successfully connected to AWS IoT Core
Subscribed to MQTT topic: sdk/test/python
```

### Step 6 — React Dashboard
```powershell
cd manufacturing-frontend
npm run dev
```
Open: **http://localhost:5173**

### Step 7 — Verify End-to-End
- Move an object near the HC-SR04 sensor
- Observe the gas or temperature changing
- Watch the React dashboard update within ~5 seconds

---

## Troubleshooting

### MQTT Connection Failed
```
Failed to connect to AWS IoT Core
```
- Check certificate file paths in `application.properties`
- Verify the files exist: `ManufacturingPlantMonitor.cert.pem`, `.private.key`, `root-CA.crt`
- Check AWS IoT policy allows `SpringBootManufacturingBackend` to connect
- Verify port 8883 is not blocked by firewall

### MySQL Connection Failed
```
Failed to configure a DataSource
```
- Ensure MySQL is running: `net start MySQL80`
- Verify credentials: `DB_USERNAME` and `DB_PASSWORD`
- Run `database/schema.sql` to create the database

### React Shows "Backend unavailable"
- Ensure Spring Boot is running on port 8080
- Check for CORS issues (only localhost:5173 is allowed)
- Test manually: `curl http://localhost:8080/api/health`

### No Data in Dashboard
- Verify Python gateway is publishing messages
- Check Spring Boot logs for "Saved sensor reading"
- Query MySQL directly:
  ```sql
  SELECT * FROM manufacturing_monitor.sensor_readings ORDER BY id DESC LIMIT 5;
  ```

### Private Key Format Error
AWS IoT private keys are typically in PKCS#8 format. If you get a key loading error:
```powershell
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt `
  -in ManufacturingPlantMonitor.private.key `
  -out ManufacturingPlantMonitor.private.pkcs8.key
```
Then update `aws.iot.private-key` to point to the `.pkcs8.key` file.

---

## Security

**NEVER commit to version control:**
- `*.pem` — certificates
- `*.key` — private keys
- `*.crt` — root CA certificates
- `.env` — environment files
- `application-local.properties` — local overrides

These are all in `.gitignore`.

Always use environment variables or gitignored config files for credentials.

---

## Direct Connect Production Architecture

> **Important distinction:** This project prototype connects via the public internet (MQTT over TLS). **AWS Direct Connect is the production network design being studied**, not what the Arduino physically uses.

### Prototype (This System)
```
Arduino → Python Gateway → Internet → AWS IoT Core → Spring Boot → MySQL → React
```

### Production Architecture (AWS Direct Connect Study)
```
Manufacturing Plant
    │
    ▼
Plant LAN (switches, routers)
    │
    ▼
Edge Router (at plant premises)
    │
    ▼ (dedicated private fiber, not public internet)
AWS Direct Connect Location (colocation facility)
    │
    ▼
AWS Direct Connect Gateway
    │
    ▼
Transit Gateway / Virtual Private Gateway
    │
    ▼
AWS VPC (Manufacturing-VPC: 10.10.0.0/16)
    ├── Manufacturing-Public-Subnet  (10.10.1.0/24)
    └── Manufacturing-Private-Subnet (10.10.2.0/24)
    │
    ▼
Cloud Applications (IoT Core, EC2, RDS, etc.)
```

### Why Direct Connect for Production?

| Feature | Internet (Prototype) | Direct Connect (Production) |
|---------|---------------------|-----------------------------|
| Bandwidth | Variable | Dedicated (1Gbps or 10Gbps) |
| Latency | Variable, higher | Consistent, lower |
| Security | TLS encryption | Private network (no public internet) |
| Reliability | Best-effort | SLA-backed |
| Cost model | Per GB (data transfer) | Port hours + data transfer |
| Use case | Development/prototype | Production manufacturing |

Direct Connect is appropriate for manufacturing plants that:
- Send continuous high-volume sensor data
- Have strict latency requirements for real-time control
- Require compliance with data sovereignty requirements
- Cannot risk internet outages affecting operations

### Existing AWS VPC
```
VPC:               Manufacturing-VPC (10.10.0.0/16)
Public Subnet:     Manufacturing-Public-Subnet (10.10.1.0/24)
Private Subnet:    Manufacturing-Private-Subnet (10.10.2.0/24)
Internet Gateway:  Manufacturing-IGW
```
This VPC would be the target for the Direct Connect connection in production.

---

## Project Limitations

1. **Prototype uses public internet** — not AWS Direct Connect (which is the study subject)
2. **Single-device prototype** — production would support many plant devices
3. **Local MySQL** — production would use Amazon RDS in the private subnet
4. **Local Spring Boot** — production would run on EC2 or ECS in the VPC
5. **No authentication** on REST API — production would add Cognito/API Gateway
6. **Single sensor node** — production architecture would aggregate multiple nodes
7. **AWS IoT Core free tier** — production volumes may incur costs

---

## Project Structure

```
cc-hackathon/
│
├── manufacturing-backend/           # Spring Boot Java backend
│   ├── src/main/java/com/manufacturing/backend/
│   │   ├── ManufacturingBackendApplication.java
│   │   ├── config/
│   │   │   ├── MqttConfig.java      # AWS IoT TLS connection
│   │   │   └── CorsConfig.java      # CORS for React dev server
│   │   ├── mqtt/
│   │   │   └── MqttSensorListener.java  # MQTT message handler
│   │   ├── controller/
│   │   │   ├── SensorController.java    # REST API endpoints
│   │   │   └── HealthController.java    # /api/health
│   │   ├── service/
│   │   │   └── SensorService.java       # Business logic
│   │   ├── entity/
│   │   │   └── SensorReading.java       # JPA entity
│   │   ├── repository/
│   │   │   └── SensorReadingRepository.java
│   │   ├── dto/
│   │   │   └── SensorReadingDTO.java    # JSON mapping
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   └── src/main/resources/
│       └── application.properties
│
├── manufacturing-frontend/          # React + Vite dashboard
│   └── src/
│       ├── components/
│       │   ├── Header.jsx
│       │   ├── SensorCard.jsx
│       │   ├── StatusBadge.jsx
│       │   ├── SensorTable.jsx
│       │   ├── AlertPanel.jsx
│       │   └── SensorCharts.jsx
│       ├── pages/
│       │   └── Dashboard.jsx
│       ├── services/
│       │   └── api.js
│       └── styles/
│           └── dashboard.css
│
├── database/
│   ├── schema.sql                   # MySQL schema
│   └── sample-data.sql              # Test data (hardware not required)
│
├── .gitignore                       # Excludes certs, keys, .env
└── README.md
```
