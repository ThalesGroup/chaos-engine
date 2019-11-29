# Run Chaos Engine On Kubernetes

This chapter describes how to deploy and run Chaos Engine as a service on Kubernetes. 
If you search for a manual describing how to run Kubernetes experiments please continue to [Experiment Modules](../Experiment_Modules/kubernetes_experiments.md)

You can apply all steps described below by running example [Chaos Engine Deployment](./files/chaos_engine_example_deployment.yml) YAML template.
Just call `kubectl apply -f chaos_engine_example_deployment.yml` (don't forget to provision Vault).

The script will deploy:

+ Chaos Engine, Vault containers   
+ Vault Service
+ Vault and Chaos Engine load balancer
    

## Step 1: Vault deployment

We need a secure storage for our secrets and configuration variables. Let's use [HashiCorp Vault](https://www.vaultproject.io/).

### Create Vault Secret

Start with creation of Vault secret token. This secret will be used by Chaos Engine to invoke Vault API and for UI access. 

In this example we use `00000000-0000-0000-0000-000000000000`. Replace this dummy token with your secret keyphrase.


```yaml
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: chaos-engine-secrets
  labels:
    app: "chaosengine"
type: Opaque
stringData:
  vault-token: "00000000-0000-0000-0000-000000000000"
EOF
```

### Deploy Vault

Deploy latest version of Vault by running using following template

```yaml
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vault
spec:
  replicas: 1
  selector:
    matchLabels:
      app: vault
  strategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        app: vault
    spec:
      containers:
        - name: vault
          image: vault:latest
          env:
           - name: VAULT_DEV_ROOT_TOKEN_ID
             valueFrom:
              secretKeyRef:
                name: chaos-engine-secrets
                key: vault-token
EOF
```
### Create Vault Service

Expose the Vault container instance to rest of the containers as a cluster service.

```yaml
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: vault
  labels:
    app: vault-chaos
spec:
  ports:
    - port: 8200
      protocol: TCP
      targetPort: 8200
  selector:
    app: vault
EOF
```

### Create Load Balancer

Make the Vault UI accessible from outside the cluster.
```yaml
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: vault-lb
  labels:
    app: vault-lb
spec:
  ports:
    - port: 8200
      targetPort: 8200
  selector:
    app: vault
  type: LoadBalancer
EOF
```

### Add Configuration Properties to Vault

* First of get the IP of the `vault-lb`. To do so run following command.
```bash
kubectl describe services vault-lb | grep 'LoadBalancer\ Ingress' | awk '{print $3}'
```
* Go to `http://$VAULT_LB_IP:8200` and authenticate using `vault-token` you set while creating secrets.
* Click on `Create secrets` and set your required configuration properties. 

!!! Note
    All properties must be created under `chaosengine` path.



## Step 2: Chaos Engine Deployment

### Deploy Chaos Engine

Download and deploy latest Chaos Engine
```yaml
cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: chaosengine
spec:
  replicas: 1
  selector:
    matchLabels:
      app: chaosengine
  strategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        app: chaosengine
    spec:
      containers:
        - name: chaosengine
          image: thalesgroup/chaos-engine:latest
          env:
           - name: VAULT_TOKEN
             valueFrom:
              secretKeyRef:
                name: chaos-engine-secrets
                key: vault-token
           - name: VAULT_SCHEME
             value: http
           - name: VAULT_HOST
             value: vault
           - name: VAULT_PORT
             value: "8200"
           - name: VAULT_10
             value: "true"
           - name: CHAOS_SECURITY_ENABLED
             value: "false"
           - name: automatedMode
             value: "false"
EOF
```

### Create Load Balancer

Expose Chaos Engine REST api.
```yaml
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: chaosengine-lb
  labels:
    app: chaosengine-lb
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: chaosengine
  type: LoadBalancer
EOF
```

### Check The Chaos Engine Health

* Get the IP of the `chaosengine-lb`. To do so run following command.
`kubectl describe services chaosengine-lb | grep 'LoadBalancer\ Ingress' | awk '{print $3}'`
* Go to `http://$CHAOS_ENGINE_LB_IP:/health` and if you see `"OK"` your Chaos Engine instance is ready to run your experiments.

