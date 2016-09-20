# Introduction
This repository implements the Minecraft domain and the method presented in the following paper ([Project website](https://sites.google.com/a/umich.edu/junhyuk-oh/icml2016-minecraft)):
  * Junhyuk Oh, Valliappa Chockalingam, Satinder Singh, Honglak Lee, **"Control of Memory, Active Perception, and Action in Minecraft**"
    _Proceedings of the 33rd International Conference on Machine Learning (ICML)_, 2016.

```
@inproceedings{Oh2016ControlOM,
  title={Control of Memory, Active Perception, and Action in Minecraft},
  author={Junhyuk Oh and Valliappa Chockalingam and Satinder P. Singh and Honglak Lee},
  booktitle={ICML},
  year={2016}
}
```

# Installation
* JRE or JDK: http://www.oracle.com/technetwork/java/javase/downloads/index.html
* Torch7: http://torch.ch/docs/getting-started.html
* Additional torch packages
  * luasocket (`luarocks install luasocket`)
  * fbpython (optional): https://github.com/facebook/fblualib
    * This is used for top-down view image generation

* Tasks and maps need to be decompressed as follows.
```
cd environment/Forge/eclipse/
tar -zxvf tasks.tar.gz
```

# Minecraft
The following command creates a minecraft instance.
```
./run_minecraft
```
You should run a minecraft instance for each task. For example, the training script on random mazes (`train_maze`) requires 3 mincraft instances for training maps, unseen maps, and larger maps.

# Training
The following scripts are provided for reproducing the main result of the paper:
```
./train_imaze [arch_name] [gpu]  : train [arch_name] on I-Maze task.
./train_matching [arch_name] [gpu] : train [arch_name] on Pattern Matching task.
./train_maze [task] [arch_name] [gpu] : train [arch_name] on random mazes.

[arch_name]: [dqn | drqn | mqn | rmqn | frmqn]
[task]: [Single | Seq | SingleIndicator | SeqIndicator]
```

# Testing
```
test [task] [network_file] [gpu] ...
```
  * Options
    * `-display`: display the first-person-view of the agent.
    * `-top_down_view`: display with top-down-view (fbpython is needed).
    * `-video [folder]`: save game play images into [folder].

## Virtual Display Installation (optional)
* to be updated
