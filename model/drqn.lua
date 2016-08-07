require 'model.net'

local DRQN, parent = torch.class('DRQN', 'Net')
function DRQN:build_init_states(args)
    local state = {}
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- c
    table.insert(state, torch.Tensor(1, args.lstm_dim)) -- h
    return state
end

function DRQN:build_model(args)
    local c0 = nn.Identity()()
    local h0 = nn.Identity()()
    local input = nn.Identity()()
    local h, c = self:build_lstm(args, input, c0, h0, testing)
    local q = nn.Linear(args.lstm_dim, args.n_actions)(h)
    return nn.gModule({input, c0, h0}, {q})
end

function DRQN:build_lstm(args, input, c0, h0)
    local T = args.hist_len
    local lstm_dim = args.lstm_dim
    local cnn_feature = self:build_cnn_with_gate(args, input, T)
    local x_gates = {nn.SplitTable(1, 2)(cnn_feature):split(T)}

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

function DRQN:build_cnn_with_gate(args, input, T)
    local reshape_input = nn.View(-1, unpack(args.image_dims))(input)
    local conv, conv_nl = {}, {}
    local prev_dim = args.ncols
    local prev_input = reshape_input
    for i=1,#args.n_units do
        conv[i] = nn.SpatialConvolution(prev_dim, args.n_units[i],
                            args.filter_size[i], args.filter_size[i],
                            args.filter_stride[i], args.filter_stride[i],
                            args.pad[i], args.pad[i])(prev_input)
        conv_nl[i] = nn.ReLU()(conv[i])
        prev_dim = args.n_units[i]
        prev_input = conv_nl[i]
    end

    local nel = 4096
    local conv_flat = nn.View(-1):setNumInputDims(3)(conv_nl[#args.n_units])
    local fc = nn.Linear(nel, args.n_hid_enc)(conv_flat)
    local fc_nl = nn.ReLU()(fc)
    local lstm_input = nn.Linear(args.n_hid_enc, 4*args.lstm_dim)(fc_nl)
    return nn.View(-1, T, 4*args.lstm_dim):setNumInputDims(2)(lstm_input)
end
