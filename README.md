# Zero-Trust mTLS Microservices Demo

A deliberately vulnerable e-commerce order pipeline that demonstrates why service-to-service identity and encryption matter. The project progresses from plaintext HTTP, through a rogue-service interception scenario, to mutual TLS (mTLS) protection in Kubernetes.

## What This Demonstrates

- **Scenario 1: Plain HTTP** - traffic between services is unencrypted, so sensitive order data can be observed on the network.
- **Scenario 2: Rogue service** - a service with the same payment route can receive and fake payment responses when traffic is redirected to it.
- **Scenario 3: mTLS protected** - trusted services use certificates issued by the cluster CA; the rogue service has no certificate and cannot complete the TLS handshake.

The demo is intentionally educational. Do not use the sample card data, credentials, certificates, or insecure modes in a production system.

## Architecture

```text
Customer/browser
       |
       v
API Gateway :8080
       |
       v
Order Service :8081
       |
       +--> Payment Service :8082
       |
       +--> Inventory Service :8083

Rogue Service :8084
  Deliberately mimics the payment API; it receives no trusted certificate.
```

The backend consists of five independent Spring Boot applications:

| Service | Directory | Port | Responsibility |
|---|---|---:|---|
| API Gateway | `gateway/` | 8080 | External entry point and request forwarding |
| Order Service | `order/` | 8081 | Coordinates order placement |
| Payment Service | `payment/` | 8082 | Processes payment requests |
| Inventory Service | `inventory/` | 8083 | Checks item stock |
| Rogue Service | `rogue/` | 8084 | Attack simulation with the payment API shape |

The `zero trust frontend/` directory contains a static browser UI for switching scenarios, checking health, viewing the request flow, and submitting demo orders.

## Prerequisites

- Java 25
- Maven 3.9+ (or the Maven Wrapper included in each service)
- Docker
- Minikube and kubectl
- cert-manager installed in the target Kubernetes cluster for the certificate manifests
- PowerShell on Windows, or equivalent shell commands on another operating system

The Maven projects use Spring Boot 3.5.12 and Java 25. Confirm the selected Java version before building:

```powershell
java -version
mvn -version
```

## Run Services Locally

Start each service in a separate terminal. The default local ports are listed above.

```powershell
cd gateway
.\mvnw.cmd spring-boot:run
```

Repeat with `order`, `payment`, `inventory`, and `rogue` in separate terminals. The default properties use Kubernetes DNS names for downstream calls, so local execution may require overriding the service URLs. For example, from the repository root:

```powershell
cd gateway
.\mvnw.cmd spring-boot:run `
  "-Dspring-boot.run.arguments=--services.order.url=http://localhost:8081"
```

For the order service, use `--services.payment.url=http://localhost:8082` and `--services.inventory.url=http://localhost:8083` when running outside Kubernetes.

## Kubernetes Demo With Minikube

The manifests expect locally built images and deploy everything into the `zerotrust` namespace.

### 1. Build images inside Minikube's Docker daemon

```powershell
minikube start
minikube -p minikube docker-env --shell powershell | Out-String | Invoke-Expression

$images = @{
  gateway = 'api-gateway'
  order = 'order-service'
  payment = 'payment-service'
  inventory = 'inventory-service'
  rogue = 'rogue-service'
}
foreach ($service in $images.Keys) {
  Push-Location $service
  .\mvnw.cmd clean package -DskipTests -q
  docker build -t "zerotrust/$($images[$service]):1.0.0" .
  Pop-Location
}
```

The resulting image names match the Kubernetes manifests: `zerotrust/api-gateway:1.0.0`, `zerotrust/order-service:1.0.0`, `zerotrust/payment-service:1.0.0`, `zerotrust/inventory-service:1.0.0`, and `zerotrust/rogue-service:1.0.0`.

### 2. Install cert-manager and create the namespace

Install cert-manager using its official installation method for your cluster, then apply the namespace and certificate issuer resources:

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/certs/ca-issuer.yaml
kubectl apply -f k8s/certs/certificates.yaml
kubectl wait --for=condition=Ready certificate --all -n zerotrust --timeout=180s
```

The certificate manifests create a self-signed bootstrap CA, a CA issuer, and certificates for the gateway, order, payment, and inventory services. The rogue service intentionally receives no certificate.

### 3. Deploy the services

```powershell
kubectl apply -f k8s/inventory-service.yaml
kubectl apply -f k8s/payment-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/api-gateway.yaml
kubectl apply -f k8s/rogue-service.yaml

kubectl get pods -n zerotrust
kubectl get services -n zerotrust
```

Get the gateway URL and open it in the frontend's **BASE URL** field:

```powershell
minikube service api-gateway -n zerotrust --url
```

Serve the frontend from its directory so browser requests work consistently:

```powershell
cd "zero trust frontend"
python -m http.server 5500
```

Open `http://localhost:5500` in a browser. Set the frontend base URL to the gateway URL returned by Minikube. The gateway's health endpoint is available at `/api/gateway/health`; Kubernetes actuator probes use the management port at `/actuator/health`.

## API

The main demo request is:

```http
POST /api/gateway/order
Content-Type: application/json
```

Example body:

```json
{
  "customerId": "CUST-001",
  "itemId": "ITEM-001",
  "quantity": 2,
  "cardNumber": "4111111111111111",
  "cvv": "123",
  "expiryDate": "12/27"
}
```

Useful direct endpoints include:

| Endpoint | Purpose |
|---|---|
| `GET /api/gateway/health` | Gateway health check |
| `POST /api/orders/place` | Order service entry point |
| `POST /api/payment/process` | Legitimate payment endpoint |
| `POST /api/inventory/check` | Inventory check |
| `GET /api/inventory/stock/{itemId}` | Query item stock |
| `GET /actuator/health` | Spring Boot health details on management port `8090` |

## Observing the Demo

View service logs:

```powershell
kubectl logs -n zerotrust deployment/api-gateway -f
kubectl logs -n zerotrust deployment/order-service -f
kubectl logs -n zerotrust deployment/payment-service -f
kubectl logs -n zerotrust deployment/inventory-service -f
```

To simulate the rogue routing condition, the existing payment service can be redirected temporarily:

```powershell
kubectl patch service payment-service -n zerotrust --type=merge -p '{"spec":{"selector":{"app":"rogue-service"}}}'
```

Restore the legitimate selector afterwards:

```powershell
kubectl patch service payment-service -n zerotrust --type=merge -p '{"spec":{"selector":{"app":"payment-service"}}}'
kubectl rollout restart deployment -n zerotrust payment-service order-service api-gateway
```

## Cleanup

```powershell
kubectl delete namespace zerotrust
minikube stop
```

## Repository Layout

```text
├── gateway/              API Gateway Spring Boot service
├── order/                Order orchestration service
├── payment/              Legitimate payment service
├── inventory/            Inventory service
├── rogue/                Rogue payment-compatible service
├── k8s/                  Kubernetes deployments, services, and cert-manager resources
├── zero trust frontend/  Static demonstration UI
└── commands.txt          Original command notes
```

## License

No license has been specified for this repository.
