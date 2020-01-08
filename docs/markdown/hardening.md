# Hardening Guidelines

## Chaos Engine Framework

### Chaos Engine Source Code

If you build your own Chaos Engine images make sure that your source code was downloaded from official [Gemalto repository in GitHub](https://github.com/gemalto).

### Docker Images

#### Docker registry

Official public repository is [in DockerHub.](https://cloud.docker.com/u/thalesgroup/repository/docker/thalesgroup/chaos-engine)
There are two important tags:

1.  1.  stable – containing latest stable release of the framework
    2.  latest – representing latest development snapshot

It's recommended to run ***master*** branch based images in production environments

### Remove development configuration options

Activate production profile, make sure that following environment variables are set:

1.  1.  *SPRING\_PROFILES\_ACTIVE=PRODUCTION*
    2.  *DEPLOYMENT\_ENVIRONMENT=PROD*

Verify that the HTTPS scheme is used for communication with Vault. Environment variable *VAULT\_SCHEME* must be set to *HTTPS* or left empty.

### Secure Chaos Engine REST API
[Follow REST Security documentation](Configuration/security.md)

## Vault

Apply following rules:

1.  Avoid usage of the dev Vault token (*00000000-0000-0000-0000-000000000000*).  Generate new Vault token.
2.  Do not use root tokens
3.  Enable SSL
4.  Provision Vault securely
5.  [Advance Hardening Recommendations](https://learn.hashicorp.com/vault/operations/production-hardening)

### How to enable SSL

[Securing Vault](https://medium.com/@dwdraju/securing-hashicorp-vault-with-lets-encrypt-ssl-19cad1eb294)

### How to create new Vault token

[Token Creation](https://www.vaultproject.io/docs/commands/token/create.html)

### How to securely preload Vault with secrets and config

#### The Engine started using docker-compose

If you start the Chaos Engine using *docker-compose.yml* script located in the root of the git repo, the Vault server will be automatically provision with secrets. Those secrets are loaded from *vault-secrets.json* located in *./developer-tools/vault-loader.* Delete the **vault-secrets.json when the Engine start up is completed.**

#### The Vault Running as a stand alone service

1.  Download [Vault binary](https://www.vaultproject.io/downloads.html)
2.  Run following commands

``` bash
VAULT_TOKEN=$(cat /path/to/token)
export VAULT_TOKEN
export VAULT_ADDR='https://$VAULT_HOST:$VAULT_PORT';
./vault auth $VAULT_TOKEN ;
vault kv put secret/chaosengine - < vault-secrets.json
```

### Vault alternatives

If you deploy the Chaos Engine to K8S the Vault can be replaced by Kubernetes Secrets.

## DataDog

Generate a [new API key](https://docs.datadoghq.com/account_management/api-app-keys/) dedicated to your Chaos Engine instance and provision the DataDog agent with that new key.

## Slack

1.  Create a new Slack channel that will be used as a dumping group for Chaos Engine notifications.
2.  Create a [new Slack token](https://slack.com/intl/en-de/help/articles/215770388-create-and-regenerate-api-tokens) and link the token with the channel created in previous step.
