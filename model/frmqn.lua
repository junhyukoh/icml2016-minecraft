require 'model.rmqn'

local FRMQN, parent = torch.class('FRMQN', 'RMQN')
function FRMQN:build_init_states(args)
    local state = {}
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- c
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- h
    table.insert(state, torch.Tensor(1, args.edim)) -- o
    return state
end

function FRMQN:build_retrieval(args, key_blocks, val_blocks, cnn_features, conv_dim, c0, h0, o0)
    local edim = args.edim
    local T = args.hist_len

    local x_flat = nn.View(-1):setNumInputDims(1)(cnn_features)
    local x_gate_flat = nn.Linear(conv_dim, 4*edim)(x_flat)
    local x_gate = nn.View(-1, T, 4*edim):setNumInputDims(2)(x_gate_flat)
    local x_gates = {nn.SplitTable(1, 2)(x_gate):split(T)}

    local c = {c0}
    local h = {h0}
    local o = {o0}
    for t = 1, T do
        local input = nn.Reshape(4*edim)(x_gates[t])
        local prev_c = c[t]
        local prev_h = h[t]
        local prev_o = o[t]

        local prev_hr = nn.JoinTable(2)({prev_h, prev_o})
        local h2h = nn.Linear(2*edim, 4*edim)(prev_hr)
        local all_input_sums = nn.CAddTable()({input, h2h})
        local reshaped = nn.View(-1, 4, edim):setNumInputDims(2)(all_input_sums)
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

        -- Retrieve a memory block
        if t == 1 then
            o[t+1] = o0
        else
            local memsize = math.min(t-1, args.memsize)
            local key_blocks_t = nn.Narrow(2, t - memsize, memsize)(key_blocks)
            local val_blocks_t = nn.Narrow(2, t - memsize, memsize)(val_blocks)
            local hid = nn.View(1, -1):setNumInputDims(1)(next_h)
            local MM_key = nn.MM(false, true)
            local key_out = MM_key({hid, key_blocks_t})
            local key_out2dim = nn.View(-1):setNumInputDims(2)(key_out)
            local P = nn.SoftMax()(key_out2dim)
            local probs3dim = nn.View(1, -1):setNumInputDims(1)(P)
            local MM_val = nn.MM(false, false)
            local val_out = MM_val({probs3dim, val_blocks_t})
            local next_o = nn.View(-1):setNumInputDims(1)(val_out)
            if args.gpu and args.gpu > 0 then
                MM_key = MM_key:cuda()
                MM_val = MM_val:cuda()
            end
            o[t+1] = next_o
        end

        self:share_module("h2h", h2h)
    end

    return h[T+1], o[T+1]
end
