# Vault Integration

## Description

Chaos Engine makes use of Vault, both to securely store information. This information can be rotated in Vault and automatically refresh in runtime in Chaos Engine.

## SDK

Given the already large use of Spring Framework, [Spring Cloud Vault](https://cloud.spring.io/spring-cloud-vault/) is used to seamlessly integrate with Vault.

## Configuration

The vault endpoint is configured through environment variables.

| Variable | Description | Default Value |
| --- | --- | --- |
| VAULT_TOKEN | Token for access into the Vault | 00000000-0000-0000-0000-000000000000 |
| VAULT_SCHEME | HTTP/HTTPS scheme for access into Vault | http |
| VAULT_HOST | FQDN of the Vault Host | localhost |
| VAULT_PORT | Port of the Vault Host| 8200 |
| VAULT_10 | Used to enable support for HashiCorp Vault v 0.10.x KV Version 2 | false |
 
**SDK Native Variables**

| Variable | Mapped Variable | Description | Default |
| --- | --- | --- | --- |
| spring.cloud.vault.token | VAULT_TOKEN | Token used for authentication when the authentication mechanism is TOKEN | 00000000-0000-0000-0000-000000000000 |
| spring.cloud.vault.port | VAULT_PORT | Port of the Vault Host | 8200 |
| spring.cloud.vault.uri | N/A | The full URI of {scheme}://{host}:{port}/ | Derived from host/port/scheme. |
| spring.cloud.vault.fail-fast | N/A | If set, application startup will fail if Vault is required and cannot be accessed. | false |
| spring.cloud.vault.kv.enabled | VAULT_10 | Enables support for Key/Version Version 2, introduced in HashiCorp Vault 0.10 | false |
| spring.cloud.vault.scheme | VAULT_SCHEME | HTTP/HTTPS scheme for access into Vault | https |
| spring.cloud.vault.host | VAULT_HOST | FQDN of the Vault Host | localhost |
| spring.cloud.vault.authentication | N/A | The Authentication Mechanism used to authenticate against Vault | TOKEN |
| spring.cloud.vault.enabled | N/A | Flag to enable/disable Vault Integration | true |

**Feeding Vault With Data Example**
```
export VAULT_ADDR='http://$VAULT_HOST:8200'
export VAULT_DEV_ROOT_TOKEN_ID=00000000-0000-0000-0000-000000000000
vault auth $VAULT_DEV_ROOT_TOKEN_ID
vault kv put secret/chaosengine aws.accessKeyId=$AWS_ACCESS_KEY_ID aws.secretAccessKey=$AWS_SECRET_ACCESS_KEY aws.region=eu-west-1  AWS_FILTER_KEYS='Chaos Victim' AWS_FILTER_VALUES=true holidays=DUM aws.ec2=true
vault kv get secret/chaosengine
```

## Triggering a Refresh

The Java Objects are not automatically refreshed when values are changed in vault. Instead, an API must be called to trigger a reload. This should be done after ALL values have changed, as calling it with only some values changed may have unpredictable errors (i.e., Access Key ID changed but Secret Key is unchanged may cause errors).

In order to trigger the request, a **HTTP POST** request needs to be made against **http://{chaosengine}/refresh** .

## Spring Profiles

The Spring Cloud Vault integration makes use of the Spring Profile option to allow for different variables to be accessible from different environments.

By default, Chaos Engine will look in **/secret/chaosengine** for the appropriate resource name. However, if run with a specified Spring Profile Name, it will look in **/secret/chaosengine/{Profile Name}/** first, allowing for distinct secrets to be given in different environments.

## Development Notes

### ConfigurationProperties

Classes that need to use many values should use the **\@ConfigurationProperties** annotation. This can be specified with a prefix. When done, any variables named **{prefix}.{variable}** will automatically be assigned using a Setter method for that variable.

**Example of \@ConfigurationProperties**
```java
@Component
@ConfigurationProperties(prefix = "chaos")
public class MyClass {
    private String myString; // This will inherit chaos.myString's value
    private Integer myInteger; // This will inherit chaos.myInteger's value

    /*
    Setters are needed for the variables in order for ConfigurationProperties to inject them.
     */
    public void setMyString (String myString) {
        this.myString = myString;
    }

    public void setMyInteger (Integer myInteger) {
        this.myInteger = myInteger;
    }

    /*
    Beans need a RefreshScope annotation in order to be reinitialized when changed.
     */
    @Bean
    @RefreshScope
    MyBean myBean () {
        return new MyBean(myString, myInteger);
    }
}
```

### RefreshScope

Spring Beans that use values from ConfigurationProperties can be flagged for reinitializing with a **\@RefreshScope** annotation. Spring does this by adding an interpretation layer between where the bean is Autowired and where it is created, extending the class of the bean to do so. This does mean that it will not work with beans of a **final** class. See above for an example.
