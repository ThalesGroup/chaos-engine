# Administrative State

The Administrative State of Chaos Engine controls the workflow of Experiments. The Administrative State can be controlled using the [REST API].

### Available States
| State | Description |
| --- | --- |
| STARTING | The Chaos Engine has not completed starting yet. |
| STARTED | Chaos Engine is up and running. Experiments are in full lifecycle. |
| PAUSED | Experiments are paused. No external actions are performed (no new experiments, no self-healing, no finalization) |
| DRAIN | New experiments are paused, but existing experiments are allowed to complete, including self-healing and finalization |
| ABORT | New experiments are paused. Existing experiments are immediately advanced to their self-healing phase. |

For more information on Experiment Lifecycles, see the [Experiment Manager] documentation. 

[Experiment Manager]: ../Core_Modules/experiment_manager.md
[REST API]: /rest