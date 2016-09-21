--[[
Copyright (c) 2014 Google Inc.
See LICENSE file for full terms of limited license.
]]

if not dqn then
    require "util.initenv"
end

local cmd = torch.CmdLine()
cmd:text()
cmd:text('Train Agent in Environment:')
cmd:text()
cmd:text('Options:')

cmd:option('-framework', 'environment.mcwrap', 'name of training framework')
cmd:option('-env', '', 'task name for testing')
cmd:option('-network', '', 'pretrained network file')
cmd:option('-param', '', 'initilaize to the pretrained parameter if specified')
cmd:option('-agent', 'NeuralQLearner', 'name of agent file to use')
cmd:option('-agent_params', '', 'string of agent parameters')
cmd:option('-threads', 1, 'number of BLAS threads')
cmd:option('-best', 1, 'use best model')
cmd:option('-port', 0, 'port number for minecraft: search over [30000,30100] if 0')
cmd:option('-num_play', 30, 'number of plays')
cmd:option('-img_size', 300, 'screen size')
cmd:option('-display', false, 'display screen')
cmd:option('-top_down', false, 'display top-down view')
cmd:option('-gpu', -1, 'gpu id')
cmd:option('-video', '', 'save video/images to the specified folder')
cmd:text()

local opt = cmd:parse(arg)
if opt.video == '' and not opt.display then
    opt.img_size = 0
end
local game_env, game_actions, agent, opt = setup(opt)
if opt.param:sub(-9) == 'params.t7' then
    local pretrained_w = torch.load(opt.param)
    assert(agent.w:nElement() == pretrained_w:nElement())
    agent.w:copy(pretrained_w)
    print("Weights are initialized from " .. opt.param)
end

local win = nil
local total_r = 0
local success = 0
local fail = 0
local rewards = {}
local final_rewards = {}
local py_ret
local path
local screen, reward, terminal = game_env:getState()

if opt.display then
    -- IMPORTANT: must display anything before python call
    -- Otherwise, qtlua gives an error
    win = image.display({image=screen})
end
if opt.video ~= '' then
    os.execute("rm -rf " .. opt.video)
    os.execute("mkdir " .. opt.video)
end

local py, td_viewer
local top_down_img, full_img, screen_img, hist_img
local env_path = 'environment/Forge/eclipse/tasks/' .. opt.env
if opt.top_down and (opt.video ~= '' or opt.display) then
    py = require "fb.python"
    local viewer = py.import("top_down_viewer")
    td_viewer = viewer.create_viewer()
    td_viewer.initialize(env_path .. '/blockTypeInfo.xml', opt.img_size)
end

for iter=1,opt.num_play do
    -- start a new game
    local ep_r = 0
    local step = 0
    local topology_id = game_env:getTopology()
    local goal_id = game_env:getGoalId()
    local pos_y, pos_x, dir = game_env:getPos()
    local video_dir = string.format("%s/%d", opt.video, iter)
    if opt.video ~= '' then
        os.execute("mkdir " .. video_dir)
    end
    if opt.top_down and (opt.video ~= '' or opt.display) then
        td_viewer.draw_topology(string.format("%s/maps/%s/topology.csv",
                env_path, topology_id))
        td_viewer.draw_goal_block(string.format("%s/maps/%s/goal_%s.csv",
                env_path, topology_id, goal_id))
    end
    
    while not terminal do
        step = step + 1
        local action_index = agent:perceive(reward, screen, terminal, true, 0)
        local display_img = screen
        if opt.top_down and (opt.video ~= '' or opt.display) then
            pos_y, pos_x, dir = game_env:getPos()
            py_ret = td_viewer.update_frame(pos_x, pos_y, dir, screen:permute(2, 3, 1))
            display_img = py.eval(py_ret[0]):permute(3, 1, 2)
        end
        if opt.video ~= '' then
            image.save(string.format("%s/%05d.png", video_dir, step-1), display_img)
        end
        if opt.display then
            win.window.size = qt.QSize{width=display_img:size(3), height=display_img:size(2)}
            image.display({image=display_img, win=win, saturate=false})
            os.execute('sleep 0.1')
        end
        screen, reward, terminal = game_env:step(game_actions[action_index], false)
        ep_r = ep_r + reward
    end
    if reward >= 1 then
        success = success + 1
    elseif reward <= -1 then
        fail = fail + 1
    end
    local action_index = agent:perceive(reward, screen, terminal, true, 0)
    local display_img = screen
    if opt.top_down and (opt.video ~= '' or opt.display) then
        pos_y, pos_x, dir = game_env:getPos()
        py_ret = td_viewer.update_frame(pos_x, pos_y, dir, screen:permute(2, 3, 1))
        display_img = py.eval(py_ret[0]):permute(3, 1, 2)
    end
    if opt.video ~= '' then
        image.save(string.format("%s/%05d.png", video_dir, step), display_img)
    end
    if opt.display then
        image.display({image=display_img, win=win, saturate=false})
        os.execute('sleep 0.5')
    end
    total_r = total_r + ep_r
    print(string.format("Episode %d: %.2f (%d steps)", iter, ep_r, step))
    if opt.video ~= '' then
        file_name = video_dir .. ".mp4"
        os.execute("ffmpeg -r 5 -i " .. video_dir .. 
                "/%05d.png -vcodec libx264 -pix_fmt yuv420p " .. file_name)
    end

    rewards[topology_id] = rewards[topology_id] or {}
    rewards[topology_id][goal_id] = rewards[topology_id][goal_id] or {}
    final_rewards[topology_id] = final_rewards[topology_id] or {}
    final_rewards[topology_id][goal_id] = final_rewards[topology_id][goal_id] or {}
    table.insert(rewards[topology_id][goal_id], ep_r)
    table.insert(final_rewards[topology_id][goal_id], reward)
    screen, reward, terminal = game_env:newGame() 
end

function table_length(t)
    local count = 0
    for _ in pairs(t) do count = count + 1 end
    return count
end
function pairsByKeys (t, f)
    local a = {}
    for n in pairs(t) do table.insert(a, n) end
    table.sort(a, f)
    local i = 0      -- iterator variable
    local iter = function ()   -- iterator function
        i = i + 1
        if a[i] == nil then return nil
        else return a[i], t[a[i]]
        end
    end
    return iter
end
for k1, v2 in pairsByKeys(rewards) do
    local reward = 0
    local success = 0
    local fail = 0
    local trial = 0
    for k2,v2 in pairsByKeys(v2) do
        local n = table_length(v2)
        for k3, v3 in pairs(v2) do
            trial = trial + 1
            reward = reward + rewards[k1][k2][k3]
            if final_rewards[k1][k2][k3] >= 1 then 
                success = success + 1
            elseif final_rewards[k1][k2][k3] <= -1 then
                fail = fail + 1
            end
        end
    end
    print(string.format("Topology: %d, Num Trials: %d, " .. 
        "Avg. Reward: %.2f, Success: %.3f, Fail: %.3f",
        k1, trial, reward / trial, success / trial, fail / trial))
end
print(string.format("Num plays: %d, Average reward: %.3f, Average success rate: %.3f", 
        opt.num_play, total_r / opt.num_play, success / opt.num_play))
print("Done.")
