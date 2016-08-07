require 'model.net'

local DQN, parent = torch.class('DQN', 'Net')
function DQN:build_model(args)
    local net = nn.Sequential()
    net:add(nn.Reshape(unpack(args.input_dims)))
    local prev_dim = args.hist_len * args.ncols
    for i=1,#args.n_units do
        net:add(args.convLayer(prev_dim, args.n_units[i],
                            args.filter_size[i], args.filter_size[i],
                            args.filter_stride[i], args.filter_stride[i],
                            args.pad[i], args.pad[i]))
        net:add(args.nl())
        prev_dim = args.n_units[i]
    end

    local nel
    local feature_map
    if args.gpu >= 0 then
        local zero_input = torch.zeros(1,unpack(args.input_dims))
        feature_map = net:cuda():forward(zero_input:cuda())
    else
        feature_map = net:forward(torch.zeros(1,unpack(args.input_dims)))
    end
    nel = feature_map:nElement()
    net:add(nn.Reshape(nel))
    net:add(nn.Linear(nel, args.n_hid_enc))
    net:add(args.nl())
    net:add(nn.Linear(args.n_hid_enc, args.n_actions))
    return net
end

