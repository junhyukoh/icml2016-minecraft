require 'nn'
require 'nngraph'
require 'util.LinearNB'
require 'model.dqn'
require 'model.drqn'
--require 'model.mqn'
--require 'model.rmqn'
--require 'model.frmqn'

function g_create_network(args)
    args.n_units            = args.n_units or {32, 64}
    args.filter_size        = args.filter_size or {4, 4}
    args.filter_stride      = args.filter_stride or {2, 2}
    args.pad                = args.pad or {1, 1}
    args.n_hid_enc          = args.edim or 256
    args.memsize            = args.memsize or (args.hist_len - 1)
    args.lindim             = args.lindim or args.n_hid_enc / 2
    args.lstm               = args.lstm or false
    args.lstm_dim           = args.edim or 256
    args.Linear             = nn.LinearNB
    if args.gpu >= 0 then
        args.softmax        = cudnn.SoftMax
        args.nl             = cudnn.ReLU
        args.convLayer      = cudnn.SpatialConvolution
    else
        args.softmax        = nn.SoftMax
        args.nl             = nn.ReLU
        args.convLayer      = nn.SpatialConvolution
    end

    if args.name == "dqn" then
        return DQN.new(args)
    elseif args.name == "drqn" then
        return DRQN.new(args)
    end
end
