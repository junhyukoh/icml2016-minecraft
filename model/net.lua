local Net = torch.class('Net')
function Net:__init(args)
    self.args = args
    self.share_list = {}
    self.init_states = self:build_init_states(args)
    self:reset_init_states()
    self.recurrent = #self.init_states > 0
    self.net = self:build_model(args) 

    if args.gpu > 0 then
        self:cuda()
        cudnn.convert(self.net, cudnn)
    end
    -- IMPORTANT! do weight sharing after model is in cuda
    for k, v in pairs(self.share_list) do
        local m1 = v[1].data.module
        for j = 2,#v do
            local m2 = v[j].data.module
            m2:share(m1,'weight','bias','gradWeight','gradBias')
        end
    end
end

function Net:build_init_states(args)
    return {}
end   

function Net:reset_init_states(batch_size)
    for j=1,#self.init_states do
        local size = self.init_states[j]:size()
        size[1] = batch_size or 1
        self.init_states[j]:resize(size)
        self.init_states[j]:fill(0)
    end
end

function Net:forward(x)
    if self.recurrent then
        local input = {x}
        self:reset_init_states(x:size(1))
        for i = 1, #self.init_states do
            table.insert(input, self.init_states[i])
        end
        return self.net:forward(input)
    else
        return self.net:forward(x)
    end
end

function Net:backward(x, gradOutput)
    if self.recurrent then
        local input = {x}
        for i = 1, #self.init_states do
            table.insert(input, self.init_states[i])
        end
        return self.net:backward(input, gradOutput)
    else
        return self.net:backward(x, gradOutput)
    end
end

function Net:getParameters()
    return self.net:getParameters() 
end

function Net:clone()
    local clone = g_create_network(self.args)
    clone.net = self.net:clone()
    return clone
end

function Net:cuda()
    self.net = self.net:cuda()
    for i=1,#self.init_states do
        self.init_states[i] = self.init_states[i]:cuda()
    end
    return self
end

function Net:float()
    self.net = self.net:float()
    for i=1,#self.init_states do
        self.init_states[i] = self.init_states[i]:float()
    end
    return self
end

function Net:training()
    self.net:training()
    return self
end

function Net:evaluate()
    self.net:evaluate()
    return self
end

function Net:share_module(name, node) 
    if self.share_list[name] == nil then
        self.share_list[name] = {node}
    else
        table.insert(self.share_list[name], node)
    end
end
