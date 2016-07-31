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

-- Creates a new ALEInterface instance.
function mcwrap.newMC(task, client, img_size)
    return mcwrap.MC(task, client, img_size)
end

local MC = torch.class('mcwrap.MC')

function to_list(str)
    local list = {}
    for s in string.gmatch(str, '([^,]+)') do
        local element = tonumber(s)
        if element == nil then
          element = string.byte(s)
        end
        table.insert(list, element)
    end
    return list
end

function MC:__init(task_name, client, img_size)
    require('socket')
    local color = require "trepl.colorize"
    print("Loading " .. color.green(task_name))
    self.client = client
    self.channels, self.height, self.width =
        self:send_receive('set_task_' .. task_name)
    if img_size and img_size > 0 then
        self:send_receive('set_screen 3 ' .. img_size .. ' ' .. img_size)
        self.channels = 3
        self.height = img_size
        self.width = img_size
    else
        self.channels = tonumber(self.channels)
        self.height = tonumber(self.height)
        self.width = tonumber(self.width)
    end
    self.num_action = 0
    self.terminated = false
    print("Initiailzed Minecraft.")
    print("Screen Size: " .. self.height .. "x" .. self.width)
end

function MC:send_receive(msg)
    -- print('send_socket: ' .. msg)
    self.client:send(msg .. '\n')
    local line, status
    line, status = self.client:receive()
    -- print('receive_socket: ' .. line)
    if status == "closed" then
      assert(false, "Socket is closed.\n" ..
          'The final message was' .. msg .. '\n')
    elseif status == "timeout" then
      assert(false, "Socket is timeout.\n" ..
          'The final message was' .. msg .. '\n')
    end

    local values = {}
    for token in string.gmatch(line, "[^%s]+") do
      table.insert(values, token)
    end
    return unpack(values)
end

function MC:actions(raw_data, num_elements)
   local acts = self:send_receive('get_actions')
   local act_list = to_list(acts)
   assert(table.getn(act_list) == num_elements,
      "The number of actions doesn't match.\n")
    for i=0,num_elements-1 do
      raw_data[i] = act_list[i+1]
    end
end

function MC:numActions()
    if self.num_action == 0 then
      self.num_action = self:send_receive('num_actions')
      self.num_action = tonumber(self.num_action)
    end
    return self.num_action
end

function MC:topology()
    return self:send_receive('get_topology')
end

function MC:goal_id()
    return self:send_receive('get_goal_id')
end

function MC:pos()
    local pos_x, pos_z, dir = self:send_receive('get_pos')
    return tonumber(pos_x), tonumber(pos_z), tonumber(dir)
end

function MC:getScreenWidth()
    return self.width
end

function MC:getScreenHeight()
    return self.height
end

function MC:getScreenChannels()
    return self.channels
end

function MC:fillObs(raw_data, num_elements)
    local msg = 'get_screen'
    local screen, status
    -- print('send_socket: get_screen')
    self.client:send(msg .. '\n')
    screen, status = self.client:receive(self.channels * self.height * self.width)
    if status == "closed" then
      assert(false, "Socket is closed.\n" ..
          'The final message was' .. msg .. '\n')
    end
    assert(string.len(screen) == self.channels * self.height * self.width,
        "The size of screen does not match: "
        .. tostring(string.len(screen))
        .. " vs. " .. tostring(self.channels * self.height * self.width))
    for i=0,num_elements-1 do
      raw_data[i] = string.byte(screen, i+1)
    end
end

function MC:isGameOver()
    return self.terminated
end

function MC:resetGame()
    self:send_receive('reset_game')
    self.terminated = false
end

function MC:lives()
    return 1
end

function MC:act(act)
    local reward, terminated
    reward, terminated = self:send_receive('act ' .. tostring(act) .. '\n')
    reward = tonumber(reward)
    terminated = tonumber(terminated)
    -- print(reward, terminated)
    self.terminated = terminated == 1
    return reward
end
