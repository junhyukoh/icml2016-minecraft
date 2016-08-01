require 'model.mqn'

local RMQN, parent = torch.class('RMQN', 'MQN')
function RMQN:build_init_states(args)
    local state = {}
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- c
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- h
    return state
end

function RMQN:build_context(args, x, xdim, edim, c0, h0)
    local T = args.hist_len
    local lstm_dim = args.lstm_dim
    local x_flat = nn.View(-1):setNumInputDims(1)(x)
    local x_gate_flat = nn.Linear(xdim, 4*lstm_dim)(x_flat)
    local x_gate = nn.View(-1, T, 4*lstm_dim):setNumInputDims(2)(x_gate_flat)
    local x_gates = {nn.SplitTable(1, 2)(x_gate):split(T)}
    local c = {c0}
    local h = {h0}
    for t = 1, T do
        local x = nn.Reshape(4*lstm_dim)(x_gates[t])
        local prev_c = c[t]
        local prev_h = h[t]

        local h2h = nn.Linear(lstm_dim, 4*lstm_dim)(prev_h)
        local all_input_sums = nn.CAddTable()({x, h2h})
        local reshaped = nn.View(-1, 4, lstm_dim):setNumInputDims(2)(all_input_sums)
        local n1, n2, n3, n4 = nn.SplitTable(2)(reshaped):split(4)

        local in_gate = nn.Sigmoid()(n1)
        local forget_gate = nn.Sigmoid()(n2)
        local in_transform = nn.Tanh()(n4)
        local next_c = nn.CAddTable()({
            nn.CMulTable()({forget_gate, prev_c}),
            nn.CMulTable()({in_gate, in_transform})
          })
        local out_gate = nn.Sigmoid()(n3)
        local next_h = nn.CMulTable()({out_gate, nn.Tanh()(next_c)})

        c[t+1] = next_c
        h[t+1] = next_h
        self:share_module("h2h", h2h)
    end
    return h[T+1], c[T+1]
end
