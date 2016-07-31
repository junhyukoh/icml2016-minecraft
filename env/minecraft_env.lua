--[[ Copyright 2014 Google Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
]]


function mcwrap.createEnv(task, extraConfig, client, img_size)
    return mcwrap.MCEnv(task, extraConfig, client, img_size)
end

-- Copies values from src to dst.
local function update(dst, src)
    for k, v in pairs(src) do
        dst[k] = v
    end
end

-- Copies the config. An error is raised on unknown params.
local function updateDefaults(dst, src)
    for k, v in pairs(src) do
        if dst[k] == nil then
            _print_usage(dst)
            error("unsupported param: " .. k)
        end
    end
    update(dst, src)
end

local Env = torch.class('mcwrap.MCEnv')
function Env:__init(task, extraConfig, client, img_size)
    self.config = {
        -- An additional reward signal can be provided
        -- after the end of one game.
        -- Note that many games don't change the score
        -- when loosing or gaining a life.
        gameOverReward=0,
        -- Screen display can be enabled.
        display=false,
    }
    updateDefaults(self.config, extraConfig)

    self.win = nil
    self.mc = mcwrap.newMC(task, client, img_size)
    self.channels = self.mc:getScreenChannels()
    self.height = self.mc:getScreenHeight()
    self.width = self.mc:getScreenWidth()
    self.obs = torch.ByteTensor(self.channels, self.height, self.width)
    local obsShapes = {{self.height, self.width}}
    self.envSpec = {
        nActions=18,
        obsShapes=obsShapes,
    }
end

-- Returns a description of the observation shapes
-- and of the possible actions.
function Env:getEnvSpec()
    return self.envSpec
end

-- Returns a list of observations.
-- The integer pmctte values are returned as the observation.
function Env:envStart()
    self.mc:resetGame()
    return self:_generateObservations()
end

-- Does the specified actions and returns the (reward, observations) pair.
-- Valid actions:
--     {torch.Tensor(zeroBasedAction)}
-- The action number should be an integer from 0 to 17.
function Env:envStep(actions)
    assert(#actions == 1, "one action is expected")
    assert(actions[1]:nElement() == 1, "one discrete action is expected")

    if self.mc:isGameOver() then
        self.mc:resetGame()
        -- The first screen of the game will be also
        -- provided as the observation.
        return self.config.gameOverReward, self:_generateObservations()
    end

    local reward = self.mc:act(actions[1][1])
    return reward, self:_generateObservations()
end


function Env:_createObs()
    require 'image'
    -- The torch.data() function is provided by torchffi.

    self.obs:resize(self.height, self.width, self.channels)
    self.mc:fillObs(torch.data(self.obs), self.obs:nElement())
    self.obs = self.obs:permute(3, 1, 2)
    self.obs = image.vflip(self.obs)
    return self.obs
end

function Env:_display(obs)
    require 'image'
    --local width = self.mc:getScreenWidth()
    --local height = self.mc:getScreenHeight()
    --local channels = self.mc:getScreenChannels()
    --local obs2 = torch.ByteTensor(channels, height, width)
    --image.rotate(obs2, obs, math.pi / 2) 
    self.win = image.display({image=obs, win=self.win})
end

-- Generates the observations for the current step.
function Env:_generateObservations()
    local obs = self:_createObs()
    if self.config.display then
        self:_display(obs)
        os.execute("sleep " .. tonumber(0.1))
    end
    return {obs}
end

function Env:actions()
    local nactions = self.mc:numActions()
    local actions = torch.IntTensor(nactions)
    self.mc:actions(torch.data(actions), actions:nElement())
    return actions
end

function Env:topology()
    return self.mc:topology()
end

function Env:goal_id()
    return self.mc:goal_id()
end

function Env:pos()
    return self.mc:pos()
end

function Env:lives()
    return self.mc:lives()
end

function Env:getScreenWidth()
  return self.mc:getScreenWidth()
end

function Env:getScreenHeight()
  return self.mc:getScreenHeight()
end

