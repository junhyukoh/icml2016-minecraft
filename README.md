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

The full description of `train.lua` is:
```
Options:
  -framework         name of training framework [environment.mcwrap]
  -env               task name for training []
  -test_env          task names for testing (comma-separated) []
  -test_hist_len     history length for testing [30]
  -env_params        string of environment parameters []
  -actrep            how many times to repeat action [1]
  -save_name         filename used for saving network and training history []
  -network           name of architecture or the filename of pretrained model []
  -agent             name of agent file to use [NeuralQLearner]
  -agent_params      string of agent parameters []
  -seed              random seed [1]
  -saveNetworkParams saves the parameter in a separate file [true]
  -save_freq         the model is saved every save_freq steps [100000]
  -eval_freq         frequency of greedy evaluation [100000]
  -eval_steps        number of evaluation steps [10000]
  -steps             number of training steps [15000000]
  -verbose           the level of debug prints [2]
  -threads           number of BLAS threads [4]
  -gpu               gpu id [-1]
  -port              port number for minecraft: search over [30000,30100] if 0 [0]
```

# Testing
```
./test [task] [network_file] [gpu] ...
```
  * Options
    * `-display`: display the first-person-view of the agent.
    * `-top_down_view`: display with top-down-view (fbpython is needed).
    * `-video [folder]`: save game play images into [folder].

The full description of `test.lua` is:
```
Options:
  -framework    name of training framework [environment.mcwrap]
  -env          task name for testing []
  -network      pretrained network file []
  -param        initilaize to the pretrained parameter if specified []
  -agent        name of agent file to use [NeuralQLearner]
  -agent_params string of agent parameters []
  -threads      number of BLAS threads [1]
  -best         use best model [1]
  -port         port number for minecraft: search over [30000,30100] if 0 [0]
  -num_play     number of plays [30]
  -img_size     screen size [300]
  -display      display screen [false]
  -top_down     display top-down view [false]
  -gpu          gpu id [-1]
  -video        save video/images to the specified folder []
```
# Notes
* The Minecraft instance does not display anything, because we turned off Minecraft display for efficient computation. You can see the game play only through the test script.
