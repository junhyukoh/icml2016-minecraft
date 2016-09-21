local gameEnv = torch.class('mcwrap.GameEnvironment')
function gameEnv:__init(_opt)
    local _opt = _opt or {}
    -- defaults to emulator speed
    self.verbose        = _opt.verbose or 0
    self._actrep        = _opt.actrep or 1
    self._random_starts = _opt.random_starts or 1
    self:reset(_opt.env, _opt.env_params, _opt.gpu, _opt.port, _opt.img_size)
    return self
end


function gameEnv:_updateState(frame, reward, terminal, lives)
    self._state.observation  = frame
    self._state.reward       = reward
    self._state.terminal     = terminal
    self._state.prev_lives   = self._state.lives or lives
    self._state.lives        = lives
    return self
end


function gameEnv:getState()
    return self._state.observation, self._state.reward, self._state.terminal
end


function gameEnv:reset(_env, _params, _gpu, _port, _img_size)
    local env
    local params = _params or {useRGB=true}
    -- if no game name given use previous name if available
    if self.game then
        env = self.game.name
    end
    env = _env or env or 'default'

    local port
    if _port and _port > 0 then
        self.client = socket.connect("0.0.0.0", _port)
        port = _port
    else
        local connected = false
        print("Searching for available Minecraft instance..")
        while not connected do
            for i=30000,30100 do
                self.client = socket.connect("0.0.0.0", i)
                if self.client then
                    port = i
                    connected = true
                    break
                end
                -- os.execute("sleep 0.0001")
            end
            os.execute("sleep 1")
        end
    end
    assert(self.client, "Socket connection failed")
    print("Connected to Port:", port)
    params.client = self.client
    params.img_size = _img_size
    self.game       = mcwrap.game(env, params)
    params.client = nil
    self._actions   = self:getActions()

    -- start the game
    if self.mc_proc and self.verbose > 2 then
        io.write(self.mc_proc:stdout())
        io.flush()
        print('\nPlaying:', self.game.name)
    end

    self:_resetState()
    self:_updateState(self.game:state())
    return self
end

function gameEnv:_resetState()
    self._state = self._state or {}
    return self
end


-- Function plays `action` in the game and return game state.
function gameEnv:_step(action)
    assert(action)
    local x = self.game:play(action)
    return x.data, x.reward, x.terminal, x.lives
end


-- Function plays one random action in the game and return game state.
function gameEnv:_randomStep()
    return self:_step(self._actions[torch.random(#self._actions)])
end


function gameEnv:step(action, training)
    -- accumulate rewards over actrep action repeats
    local cumulated_reward = 0
    local frame, reward, terminal, lives
    for i=1,self._actrep do
        -- Take selected action; ATARI games' actions start with action "0".
        frame, reward, terminal, lives = self:_step(action)

        -- accumulate instantaneous reward
        cumulated_reward = cumulated_reward + reward

        -- Loosing a life will trigger a terminal signal in training mode.
        -- We assume that a "life" IS an episode during training, but not during testing
        if training and lives and lives < self._state.lives then
            terminal = true
        end

        -- game over, no point to repeat current action
        if terminal then break end
    end
    self:_updateState(frame, cumulated_reward, terminal, lives)
    return self:getState()
end


--[[ Function advances the emulator state until a new game starts and returns
this state. The new game may be a different one, in the sense that playing back
the exact same sequence of actions will result in different outcomes.
]]
function gameEnv:newGame()
    local obs, reward, terminal
    terminal = self._state.terminal
    while not terminal do
        obs, reward, terminal, lives = self:_randomStep()
    end
    -- take one null action in the new game
    return self:_updateState(self:_step(0)):getState()
end


--[[ Function advances the emulator state until a new (random) game starts and
returns this state.
]]
function gameEnv:nextRandomGame(k)
    local obs, reward, terminal = self:newGame()
    k = k or torch.random(self._random_starts)
    for i=1,k-1 do
        obs, reward, terminal, lives = self:_step(0)
        if terminal then
            print(string.format('WARNING: Terminal signal received after %d 0-steps', i))
        end
    end
    return self:_updateState(self:_step(0)):getState()
end


--[[ Function returns the number total number of pixels in one frame/observation
from the current game.
]]
function gameEnv:nObsFeature()
    return self.game:nObsFeature()
end


-- Function returns a table with valid actions in the current game.
function gameEnv:getActions()
    return self.game:actions()
end

function gameEnv:getTopology()
    return self.game:topology()
end

function gameEnv:getGoalId()
    return self.game:goal_id()
end

function gameEnv:getPos()
    return self.game:pos()
end
