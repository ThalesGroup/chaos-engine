# SSH Based Experiments

The SSH module can interact with remote SSH servers and perform specific set of experiments on the targeted platform.

## Implementation Details

The Chaos Engine SSH Module uses the [SSHJ Library] for SSH Session management, including all ciphers and key exchange algorithms in the library. Each Platform has their own mechanism for determining the SSH Endpoint and Authentication Mechanism. 

!!! info
    The SSH Module uses _PromiscuousVerifier_ so it accepts any platform fingerprint.
    

## Custom Script-Based Experiments

Chaos Engine automatically loads any scripts in the classpath under **ssh/experiments/**. 


## Included Script-Based Experiments




[SSHJ Library]: https://github.com/hierynomus/sshj