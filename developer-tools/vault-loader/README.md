# Vault Loader

The Vault Loader is used to easily ensure the Chaos Engine starts up with the needed data in vault.

It depends on a file that is in `.gitignore`, `vault-secrets.json` to exist in this directory.

That file should contain a JSON version of the secrets needed for testing.

## Example vault-secrets.json

```json
{
  "holidays": "CAN",
  "kubernetes": "",
  "kubernetes.averageMillisPerExperiment": "300000",
  "kubernetes.token": "shhhItsASecret",
  "kubernetes.url": "https://127.0.0.1/"
}
```