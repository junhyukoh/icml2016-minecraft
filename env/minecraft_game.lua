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

--[[ Game class that provides an interface for the atari roms.

In general, you would want to use:
    mcwrap.game(gamename)
]]

require 'torch'
local game = torch.class('mcwrap.game')
require 'paths'


--[[
Parameters:

 * `gamename` (string) - one of the rom names without '.bin' extension.
 * `options`  (table) - a table of options

Where `options` has the following keys:

 * `useRGB`   (bool) - true if you want to use RGB pixels.

]]
function game:__init(gamename, options)
    options = options or {}

    self.useRGB   = options.useRGB

    self.name = gamename
    -- local msg, err = pcall(mcwrap.createEnv, gamename)
    --if not msg then
    --    error("Cannot find task: " .. gamename)
    --end
    -- self.env = err
    self.env = mcwrap.createEnv(gamename, {}, options.client, options.img_size)
    self.observations = self.env:envStart()
    self.reward = 0
    self.action = {torch.Tensor{0}}
    self.game_over = false 

    -- setup initial observations by playing a no-action command
    --local x = self:play(0)
    --self.observations[1] = x.data
end

function game:stochastic()
    return false
end


function game:shape()
    return self.observations[1]:size():totable()
end


function game:nObsFeature()
    return torch.prod(torch.Tensor(self:shape()),1)[1]
end


function game:actions()
    return self.env:actions():storage():totable()
end

function game:topology()
    return self.env:topology()
end

function game:goal_id()
    return self.env:goal_id()
end

function game:pos()
    return self.env:pos()
end

function game:lives()
    return self.env:lives()
end

function game:is_game_over() 
    return self.env.mc:isGameOver()
end

function game:state()
    return self.observations[1], self.reward,
            self:is_game_over(), self:lives()
end
--[[
Parameters:
 * `action` (int [0-17]), the action to play

Returns a table containing the result of playing given action, with the
following keys:
 * `reward` - reward obtained
 * `data`   - observations
 * `pixels` - pixel space observations
 * `terminal` - (bool), true if the new state is a terminal state
]]
function game:play(action)
    action = action or 0
    self.action[1][1] = action

    -- take the step in the environment
    self.reward, self.observations = self.env:envStep(self.action)
    local pixels = self.observations[1]
    local data = pixels

    return {reward=self.reward, data=data, pixels=pixels, 
            terminal=self:is_game_over(), lives=self:lives()}
end

