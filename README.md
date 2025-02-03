# IoT Data Store

## Overview

IoT Data Store is a lightweight Vert.x-based REST API for managing IoT device data with PostgreSQL.

## Features

* **CRUD operations** for IoT device records

* **JSON-based request handling**

* **PostgreSQL** database integration

* **Connection pooling** for efficiency



## API Endpoints

**GET /api/:deviceType/:id** - Retrieve device data

**POST /api/:deviceType/:id** - Create a new device record

**PUT /api/:deviceType/:id** - Update an existing device record

**DELETE /api/:deviceType/:id** - Delete a device record (204 No Content on success)

## Setup

1. Install dependencies

2. Configure PostgreSQL connection in MainVerticle.java

3. Run the application:

`mvn clean compile exec:java -Dexec.mainClass="com.example.starter.MainVerticle"`


## Configuration

Modify database credentials and server settings in MainVerticle.java.


