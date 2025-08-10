#!/bin/bash
# Start Minikube and configure DNS

echo "Starting Minikube..."
minikube start --cpus=12 --memory=32g --disk-size=100g --mount --mount-string="/data:/data"

if [ $? -eq 0 ]; then
    echo "Minikube started successfully. Configuring DNS..."
    MINIKUBE_IP=$(minikube ip)
    if [ -n "$MINIKUBE_IP" ]; then
        echo "server=/deplatform.local/$MINIKUBE_IP" | sudo tee /etc/NetworkManager/dnsmasq.d/minikube.conf
        sudo systemctl reload NetworkManager
        echo "DNS configured for deplatform.local at $MINIKUBE_IP"
        echo "Cluster is ready."
    else
        echo "Could not get Minikube IP. DNS not configured."
    fi
else
    echo "Minikube failed to start."
fi
