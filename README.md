# JSON Tools - Visualizer & Batch Tester

A JavaFX desktop application for visualizing JSON data and batch-testing HTTP endpoints.

## Features

### JSON Visualizer
- **Load JSON Schema** — Upload a `.json` schema file for validation
- **Fetch from URL** — Provide any JSON API URL and deserialize the response
- **Table View** — Flat table with columns for Key, Type, and Value. Complex objects (objects/arrays) appear as clickable hyperlinks that drill down into nested data
- **Tree View** — Hierarchical tree visualization of the entire JSON structure
- **Rich Tooltips** — Hover over any non-primitive field to see a detailed `toString()` preview (field names, values, counts)
- **Schema Validation** — If a schema is loaded, fetched JSON is validated with error reporting

### Batch URL Tester
- **Load Test File** — Import a JSON file defining multiple HTTP requests with parameters
- **Sequential / Parallel Execution** — Choose execution strategy
- **Error Detection** — Validates actual status codes against expected status codes
- **Performance Statistics** — Min, max, average, median, P95, P99 response times, throughput, success rate

## Requirements

- Java 25+ (included in `jdk-25/` directory)
- Maven 3.8+

## Build & Run

```bash
mvn clean javafx:run
```

Or build and run the JAR:

```bash
mvn clean package
java --module-path <javafx-sdk>/lib --add-modules javafx.controls,javafx.fxml -jar target/json-tools-1.0.0-SNAPSHOT.jar
```

## Quick Start

### Visualizer Tab
1. (Optional) Click **Load Schema** and select a JSON schema file (see `samples/user-schema.json`)
2. Enter a URL like `https://jsonplaceholder.typicode.com/users`
3. Click **Fetch & Visualize**
4. Explore data in Table View (click links to drill into objects) or Tree View
5. Hover over complex objects to see tooltip previews

### Batch Test Tab
1. Click **Load Test File** and select a batch config (see `samples/batch-test-example.json`)
2. Click **Run Sequential** or **Run Parallel**
3. Watch results populate in real-time with pass/fail indicators
4. Review performance statistics in the right panel

## Batch Test File Format

```json
[
  {
    "name": "Get Users",
    "url": "https://api.example.com/users",
    "method": "GET",
    "headers": {"Authorization": "Bearer token"},
    "queryParams": {"page": "1", "limit": "10"},
    "body": null,
    "expectedStatus": 200
  }
]
```

## Project Structure

```
src/main/java/com/jsontools/
├── App.java                    # Application entry point
├── model/
│   ├── JsonNodeWrapper.java    # JSON node with rich toString & navigation
│   ├── BatchTestRequest.java   # HTTP request definition
│   └── BatchTestResult.java    # Test execution result
├── service/
│   ├── HttpService.java        # HTTP client for fetching JSON
│   └── JsonSchemaService.java  # Schema loading & validation
├── testing/
│   ├── BatchTestRunner.java    # Batch execution engine
│   └── TestStatistics.java     # Performance metrics aggregator
└── ui/
    ├── VisualizerTab.java      # Visualizer tab layout & logic
    ├── JsonTableView.java      # Table view with clickable links
    ├── JsonTreeView.java       # Tree view with tooltips
    └── BatchTestTab.java       # Batch testing UI
```
