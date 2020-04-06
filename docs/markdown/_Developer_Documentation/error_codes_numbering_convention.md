# Error Codes Numbering Convention

## Description

Error codes for Chaos Engine are five digit numbers that can be used to reference similar errors in different situations. 
The first two digits of the error code depend on where in the application the error occurred. 
The third digit may be used to further specify a subcomponent by the individual module (i.e., AWS EC2 vs AWS RDS).
 It may also be grouped with the remaining digits to form a unique code for the error.
 
```text
1 2 3 4 5
| | |  \+-- Specific to the individual error
| |  \-- May be used by a module to further divide errors
|  \-- Specific to the package in the application where the error occurred (i.e., Platform, Container, ShellClient)
 \-- Specific to the module of the application where the error occurred (i.e., Core, Kubernetes, AWS EC2)
```

## Numbering Convention

### Module Digit

The first digit corresponds to the module where the error occurred. 
The following numbers are reserved. **0** is not valid, as the number range is 10000-99999

| Digit| Module |
| :---: | :---: | 
| 1 | Core |
| 2 | Amazon Web Services |
| 3 | Cloud Foundry |
| 4 | Kubernetes |
| 5 | Google Cloud Platform |
| 6 | - |
| 7 | - |
| 8 | - |
| 9 | - |

### Package Digit

The second digit corresponds to the package in the module where the error occurred. 
The following numbers are reserved.

| Digit| Package |
| :---: | :---: | 
| 0 | Generic. Either package independent or small packages. |
| 1 | Platform package |
| 2 | Container package |
| 3 | Experiment package |
| 4 | - |
| 5 | ShellClient package |
| 6 | - |
| 7 | - |
| 8 | Notification package |
| 9 | Admin package |



### Error Specific Digits

Error specific digits are uniquely assigned to new errors as needed.


The digits **x00** digits should be reserved for generic errors.
