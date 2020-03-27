# Kubernetes Module

The Chaos Engine Kubernetes Module is able to connect to a Kubernetes cluster and interact with deployed PODs.

## Supported Versions

Chaos Engine supports minimum Kubernetes version 1.9 and currently supports up to version 1.15. Support is driven by the Kubernetes Client SDK version compatibility.


## SDK

The official Kubernetes Java Client is used to interact with the cluster.

| | |
| --- | --- |
| Resource | <https://github.com/kubernetes-client/java> |
| Version | 7.0.0 |
|  Maven Repositories | <https://mvnrepository.com/artifact/io.kubernetes/client-java> |

## Configuration

Environment variables that control how the Chaos Engine interacts with Kubernetes.

| Key Name | Description | Default Value | Mandatory |
| --- | --- | --- | :---: |
| kubernetes | The presence of this key enables Kubernetes module. | N/A |  Yes |
| kubernetes.url | Kubernetes server API url e.g. | None | Yes |
| kubernetes.token | JWT token assigned to service account. You can get the value by running `kubectl describe secret name_of_your_secret` | None | Yes |
| kubernetes.namespace | K8S namespace where experiments should be performed | `default` | Yes |
| kubernetes.debug | Enables debug log of Kubernetes java client | `false` | No |
| kubernetes.validateSSL | Enables validation of sever side certificates | `false` | No |

## Required Kubernetes Cluster Configuration

A service account with a role binding needs to be created in order to access the specific API endpoints required for Kubernetes Experiments

Please replace the {{namespace}} fillers with the appropriate values and apply to your cluster.

You can retrieve the token by running `kubectl describe secret chaos-engine -n {{namespace}}`

**chaos-engine-service-account.yaml**

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: chaos-engine-role
  namespace: {{namespace}}
rules:
- apiGroups:
  - apps
  resources:
  - daemonsets
  - daemonsets/status
  - deployments
  - deployments/status
  - replicasets
  - replicasets/status
  - statefulsets
  - statefulsets/status
  verbs:
  - get
  - list
- apiGroups:
  - ""
  resources:
  - pods
  verbs:
  - delete


- apiGroups:
  - ""
  resources:
  - pods
  - pods/status
  - replicationcontrollers/status
  verbs:
  - get
  - list

- apiGroups:
  - ""
  resources:
  - pods/exec
  verbs:
  - create
  - get

---

apiVersion: v1
kind: ServiceAccount
metadata:
  name: chaos-engine-serviceaccount
  namespace: {{namespace}}

---

apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: chaos-engine-rolebinding
  namespace: {{namespace}}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: chaos-engine-role
subjects:
- kind: ServiceAccount
  name: chaos-engine-serviceaccount
  namespace: {{namespace}}
```

## Node Discovery

### Mechanism

API: [listNamespacedPod]

The Kubernetes Platform generates list of available containers by calling listNamespacedPod API. Filtering is done according to namespace name provided as a platform configuration parameter. Only one namespace can be targeted right now.

!!! note
    ***Job*** and ***Cron Job*** controllers are **not** supported by the Engine. Containers managed by those controllers are considered unhealthy and they are automatically skipped by scheduler.

### Self Awareness

Not yet implemented

## Experiments

### Delete Pod

#### Mechanism

The Engine invokes deleteNamespacedPod API call with zero graceful period. That leads to immediate POD termination. In theory all containers should be backed by a controller so the deleted container should be replaced by brand new container instance created by the controller.

API: [deleteNamespacedPod]


#### Health Check

Experiment is finished when originally targeted container is no more present on the platform and the controller is back to desired number of replicas.

API Check pod: [listNamespacedPod]

API Controllers:

- [readNamespacedReplicationControllerStatus]
- [readNamespacedReplicaSetStatus]
- [readNamespacedStatefulSetStatus]
- [readNamespacedDaemonSetStatus]
- [readNamespacedDeploymentStatus]

#### Self Healing

None

### Shell based experiments

#### Mechanism

Shell base experiments is a suite of shell scripts that are randomly selected and transferred to the targeted Kubernetes container and executed.

API: [connectGetNamespacedPodExec]

#### Health Check

Same as Delete Pod experiment, actual replicas count and container
existence is checked.

#### Self Healing

Target container is deleted with no graceful period.

API: [listNamespacedPod]

#### List of experiments

See [Included Script Experiments] for a list of experiments included with the engine.

[listNamespacedPod]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/CoreV1Api.md#listNamespacedPod
[deleteNamespacedPod]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/CoreV1Api.md#deleteNamespacedPod
[readNamespacedReplicationControllerStatus]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/CoreV1Api.md#readNamespacedReplicationControllerStatus
[readNamespacedReplicaSetStatus]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/AppsV1Api.md#readNamespacedReplicaSetStatus
[readNamespacedStatefulSetStatus]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/AppsV1Api.md#readNamespacedStatefulSetStatus
[readNamespacedDaemonSetStatus]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/AppsV1Api.md#readNamespacedDaemonSetStatus
[readNamespacedDeploymentStatus]: https://github.com/kubernetes-client/java/blob/release-4.0.0/kubernetes/docs/AppsV1Api.md#readNamespacedDeploymentStatus
[connectGetNamespacedPodExec]: https://github.com/kubernetes-client/java/blob/master/kubernetes/docs/CoreV1Api.md#connectGetNamespacedPodExec

[Included Script Experiments]: ./Script_Experiments/included_script_experiments.md