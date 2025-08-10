#!/bin/bash
# Stop Minikube and clean up DNS

echo "Stopping Minikube..."
minikube stop

echo "Cleaning up DNS configuration..."
if [ -f /etc/NetworkManager/dnsmasq.d/minikube.conf ]; then
    sudo rm /etc/NetworkManager/dnsmasq.d/minikube.conf
    sudo systemctl reload NetworkManager
    echo "DNS configuration removed."
else
    echo "No DNS configuration file found to remove."
fi

echo "Cluster is stopped."
