# ParkingSpaceDetector

The ParkingSpaceDetector is a backend application developed using Spring Boot. It serves as the server-side component for the [Parking Space Detector Frontend](https://github.com/sajiniho07/parking-space-detector-frontend) application, providing APIs for image and video processing to detect parking space occupancy.

## Features

- **API Services**: Offers endpoints for uploading and processing images and videos related to parking spaces.
- **Image Processing**: Analyzes images to determine the status of parking slots (occupied or vacant).
- **Model Management**: Manages machine learning models to enhance detection accuracy.

## Prerequisites

Before setting up the project, ensure you have the following installed:

- [Java Development Kit (JDK) 11 or higher](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
- [Apache Maven](https://maven.apache.org/)
- [Nginx](https://www.nginx.com/)

## Installation and Setup

1. **Clone the Repository**:

   ```bash
   git clone https://github.com/sajiniho07/ParkingSpaceDetector.git
   cd ParkingSpaceDetector
   ```

2. **Build and Run the Application**:

   ```bash
   ./mvnw spring-boot:run
   ```

   By default, the application will run on port 8080.

3. **Install and Configure Nginx**:

    - **Install Nginx**:

      For Debian/Ubuntu-based systems:

      ```bash
      sudo apt update
      sudo apt install nginx
      ```

    - **Configure Nginx as a Reverse Proxy**:

      Open the default Nginx configuration file, typically located at `/etc/nginx/sites-available/default`, and modify the server block as follows:

      ```nginx
      server {
        listen       80;
        server_name  localhost;

        location / {
            root   html;
            index  index.html index.htm;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
 
        # Serve static files from the backend
        location /media/ {
          # alias "<some server loc>";
            autoindex on;  # Optional: Enable directory listing
        }

        # Proxy requests to the API server
        location /api/ {
            proxy_pass http://localhost:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
      }
      ```

    - **Restart Nginx**:

      ```bash
      sudo systemctl restart nginx
      ```

   Nginx will now act as a reverse proxy, forwarding requests from port 80 to the Spring Boot application running on port 8080.

## Building for Production

To build the application for production use:

```bash
./mvnw clean package
```

The resulting JAR file will be located in the `target` directory.

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch:

   ```bash
   git checkout -b feature/YourFeatureName
   ```

3. Make your changes and commit them.
4. Push to your branch:

   ```bash
   git push origin feature/YourFeatureName
   ```

5. Open a Pull Request.
 