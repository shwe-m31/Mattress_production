#!/bin/sh

# Check if the DB_CA_CERT environment variable is set
if [ -n "$DB_CA_CERT" ]; then
    echo "CA certificate found in DB_CA_CERT. Generating truststore..."
    
    # Save the certificate to ca.pem
    echo "$DB_CA_CERT" > /app/ca.pem
    
    # Import the certificate into a PKCS12 keystore
    keytool -importcert \
            -alias aiven-mysql \
            -file /app/ca.pem \
            -keystore /app/truststore.p12 \
            -storetype PKCS12 \
            -storepass changeit \
            -noprompt
            
    echo "Aiven truststore generated at /app/truststore.p12"
else
    echo "DB_CA_CERT environment variable is empty. Standard SSL connection mode will be used."
fi

# Start the Spring Boot application
exec java -jar app.jar
