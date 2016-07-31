--[[
Copyright (c) 2014 Google Inc.

See LICENSE file for full terms of limited license.
]]

if not dqn then
    require "util.initenv"
end
require "xlua"
local color = require "trepl.colorize"
local cmd = torch.CmdLine()
cmd:text()
cmd:text('Train Agent in Environment:')
cmd:text()
cmd:text('Options:')

cmd:option('-framework', 'env.mcwrap', 'name of training framework')
cmd:option('-env', '', 'name of environment to use')
cmd:option('-test_env', '', 'name of test environment to use')
cmd:option('-test_hist_len', 30, 'history length for testing')
cmd:option('-test_freq', 1e5, 'frequency of test')
cmd:option('-test_steps', 1e4, 'number of evaluation steps for test environment')
cmd:option('-env_params', '', 'string of environment parameters')
cmd:option('-actrep', 1, 'how many times to repeat action')
cmd:option('-save_name', '', 'filename used for saving network and training history')
cmd:option('-network', '', 'name of architecture or the filename of pretrained model')
cmd:option('-agent', 'NeuralQLearner', 'name of agent file to use')
cmd:option('-agent_params', '', 'string of agent parameters')
cmd:option('-seed', 1, 'fixed input seed for repeatable experiments')
cmd:option('-saveNetworkParams', true, 'saves the agent network in a separate file')
cmd:option('-prog_freq', 1e4, 'frequency of progress output')
cmd:option('-save_freq', 1e5, 'the model is saved every save_freq steps')
cmd:option('-eval_freq', 1e5, 'frequency of greedy evaluation')
cmd:option('-steps', 15e6, 'number of training steps to perform')
cmd:option('-eval_steps', 1e4, 'number of evaluation steps')
cmd:option('-verbose', 2,
           'the higher the level, the more information is printed to screen')
cmd:option('-threads', 4, 'number of BLAS threads')
cmd:option('-gpu', -1, 'gpu flag')
cmd:option('-port', 0, 'port number')
cmd:option('-num_save', 10, 'num of saved network params')
cmd:text()
local opt = cmd:parse(arg)

-- evaluate agent
function eval_agent(env, agent, steps)
    local screen, reward, terminal
    local total_reward = 0
    local nepisodes = 0
    local episode_reward = 0
    screen, reward, terminal = env:newGame()
    local estep = 1
    while true do
        xlua.progress(math.min(estep, steps), steps)
        local action_index = agent:perceive(reward, screen, terminal, true, 0.0)
        screen, reward, terminal = env:step(agent.actions[action_index])
        if estep % 1000 == 0 then collectgarbage() end
        episode_reward = episode_reward + reward
        if terminal then
            total_reward = total_reward + episode_reward
            episode_reward = 0
            nepisodes = nepisodes + 1
            action_index = agent:perceive(reward, screen, terminal, true, 0.0)
            if estep >= steps then
                break
            end
            screen, reward, terminal = env:newGame()
        end
        estep = estep + 1
    end
    return nepisodes, total_reward
end

-- General setup
local game_env, game_actions, agent, opt = setup(opt)
local train_env = opt.env

-- Load testing environments
local test_env_names = {}
local test_env = {}
local test_agent
for s in string.gmatch(opt.test_env, '([^,]+)') do
    table.insert(test_env_names, s)
    opt.env = s
    test_env[#test_env + 1], game_actions = create_env(opt)
end
if #test_env > 0 then
    local agent_param = {}
    for k, v in pairs(opt.agent_params) do
        agent_param[k] = v
    end
    agent_param.actions = game_actions
    agent_param.hist_len = opt.test_hist_len
    agent_param.minibatch_size = 1 
    agent_param.target_q = nil
    agent_param.replay_memory = 50000
    test_agent = create_agent(opt, agent_param)
    test_agent.network.net:share(agent.network.net, 'weight', 'bias')
end

local learn_start = agent.learn_start
local start_time = sys.clock()
local reward_counts = {}
local episode_counts = {}
local v_history = {}
local qmax_history = {}
local td_history = {}
local reward_history = {}
local test_reward_history = {}
local best_history = {}
local step = 0 
for i=1,#test_env do
    test_reward_history[i] = {}
end

local total_reward, nrewards, nepisodes
local screen, reward, terminal = game_env:getState()
local epoch_time = sys.clock()

local ep_reward = 0
local ep_step = 0
local ev_flag = false
local prog_flag = false
local test_flag = false
os.execute("mkdir -p save")
while step < opt.steps do
    step = step + 1
    if step % opt.eval_freq == 0 and step > learn_start then ev_flag = true end
    if step % opt.test_freq == 0 and step > learn_start then test_flag = true end
    if step % opt.prog_freq == 0 then prog_flag = true end
    local action_index = agent:perceive(reward, screen, terminal)
    screen, reward, terminal = game_env:step(game_actions[action_index], true)
    ep_reward = ep_reward + reward
    ep_step = ep_step + 1

    if terminal then
        step = step + 1
        local epoch = torch.floor(step/opt.eval_freq)
        local action_index = agent:perceive(reward, screen, terminal)
        if step % opt.eval_freq == 0 and step > learn_start then ev_flag = true end
        if step % opt.test_freq == 0 and step > learn_start then test_flag = true end
        if step % opt.prog_freq == 0 then prog_flag = true end
        screen, reward, terminal = game_env:newGame()
        ep_reward = 0
        ep_step = 0
        if ev_flag then
            print("Evalutating the agent on the training environment: " 
                    .. color.green(train_env))
            ev_flag = false
            nepisodes, nrewards, total_reward = eval_agent(game_env, agent, opt.eval_steps)
            local ind = #reward_history+1
            total_reward = total_reward/math.max(1, nepisodes)
            if agent.v_avg then
                v_history[ind] = agent.v_avg
                td_history[ind] = agent.tderr_avg
                qmax_history[ind] = agent.q_max
            end
            print("Epoch:", epoch, "Reward:", total_reward, "num. ep.:", nepisodes)
            reward_history[ind] = total_reward
            reward_counts[ind] = nrewards
            episode_counts[ind] = nepisodes
            screen, reward, terminal = game_env:newGame()
        end

        if test_flag and #test_env > 0 then
            test_flag = false
            if not agent.best_test_network then
                agent.best_test_network = {}
            end
            for test_id=1,#test_env do
                local ind = #test_reward_history[test_id]+1
                print("Evalutating the agent on the test environment: " 
                        .. color.green(test_env_names[test_id]))
                nepisodes, total_reward = eval_agent(test_env[test_id], test_agent, opt.test_steps)
                total_reward = total_reward/math.max(1, nepisodes)
                if #test_reward_history[test_id] == 0 or 
                        total_reward > torch.Tensor(test_reward_history[test_id]):max() then
                    agent.best_test_network[test_id] = test_agent.network:clone()
                end
                test_reward_history[test_id][ind] = total_reward
                print("Epoch:", epoch, "Reward:", total_reward, "num. ep.:", nepisodes)
            end

            -- Maintain and save only top K best models
            if opt.saveNetworkParams then
                local test_reward = test_reward_history[1][#test_reward_history[1]]
                local min_key = -1
                local min_instance = nil
                for k, v in pairs(best_history) do
                    if min_instance == nil or min_instance.reward > v.reward then
                        min_key = k
                        min_instance = v
                    end
                end
                if #best_history < opt.num_save or min_instance.reward < test_reward then
                    if min_instance and #best_history >= opt.num_save then
                        os.execute("rm " .. string.format("save/%s_%03d.params.t7", 
                            opt.save_name, min_instance.epoch))
                        table.remove(best_history, min_key)
                    end

                    local test_instance = {}
                    test_instance.epoch = epoch
                    test_instance.reward = test_reward
                    table.insert(best_history, test_instance)
                end
                torch.save(string.format('save/%s_%03d.params.t7', opt.save_name, epoch), 
                    agent.w:clone():float())
            end
            collectgarbage()
        end
    end

    if prog_flag then
        prog_flag = false
        epoch_time = sys.clock() - epoch_time
        assert(step==agent.numSteps, 'trainer step: ' .. step ..
                ' & agent.numSteps: ' .. agent.numSteps)
        print("Steps:", step, "Step/Sec:",  math.floor(opt.prog_freq / epoch_time))
        if opt.verbose > 2 then
            agent:report()
        end
        epoch_time = sys.clock()
    end

    if step%1000 == 0 then collectgarbage() end
    if step % opt.save_freq == 0 or step == opt.steps then
        local w, dw, g, g2, deltas, tmp, target_w = agent.w, agent.dw,
            agent.g, agent.g2,  agent.deltas, agent.tmp, agent.target_w
        agent.w, agent.dw, agent.g, agent.g2, agent.deltas, agent.tmp, agent.target_w = 
            nil, nil, nil, nil, nil, nil, nil

        local filename = opt.save_name
        filename = filename
        torch.save('save/' .. filename .. ".t7", {agent = agent,
                                model = agent.network,
                                best_model = agent.best_test_network,
                                test_reward_history = test_reward_history,
                                reward_history = reward_history,
                                reward_counts = reward_counts,
                                episode_counts = episode_counts,
                                v_history = v_history,
                                td_history = td_history,
                                qmax_history = qmax_history,
                                test_history = test_history,
                                arguments=opt,
                                step=step})
        agent.w, agent.dw, agent.g, agent.g2, agent.deltas, agent.tmp, agent.target_w = 
                w, dw, g, g2, deltas, tmp, target_w
        print('Saved:', filename .. '.t7')
        io.flush()
        collectgarbage()
    end
end
