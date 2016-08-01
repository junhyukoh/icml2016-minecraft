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
    end
    -- IMPORTANT! do weight sharing after model is in cuda
    for k, v in pairs(self.share_list) do
        local m1 = v[1].data.module
        if #v > 2 then
            print(string.format("[%s] %d modules are shared", k, #v))
        end
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
        table.insert(input, x)
        self:reset_init_states(x:size(1))
        local input = {}
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
        local input = {}
        for i = 1, #self.init_states do
            table.insert(input, self.init_states[i])
        end
        table.insert(input, x)
        return self.net:backward(input, gradOutput)
    else
        return self.net:backward(x, gradOutput)
    end
end

function Net:getParameters()
    return self.net:getParameters() 
end

function Net:clone()
    local clone = self.new(self.args)
    clone.net = self.net:clone()
    return clone
end

function Net:cuda()
    self.net:cuda()
    for i=1,#self.init_states do
        self.init_states[i] = self.init_states[i]:cuda()
    end
    return self
end

function Net:float()
    self.net:float()
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

function Net:share_weight_from(m)
    for name, node in pairs(m) do
        if self.m[name] then
            local dst_module = self.m[name].data.module
            local src_module = node.data.module
            dst_module:share(src_module, 'weight','bias','gradWeight','gradBias')
        end
    end
end

function Net:copy_weight_from(m, log)
    for name, node in pairs(m) do
        if self.m[name] then
            local src_module = node.data.module
            local dst_module = self.m[name].data.module
            if src_module.weight then
                assert(dst_module.weight, name)
                assert(src_module.weight:nElement() == dst_module.weight:nElement(), 
                    name .. "source: " .. src_module.weight:nElement() ..
                    "dest: " .. dst_module.weight:nElement())
                dst_module.weight:copy(src_module.weight)
                if log then
                    print(name, "copied")
                end
            end
            if src_module.bias then
                assert(dst_module.bias)
                assert(src_module.bias:nElement() == dst_module.bias:nElement())
                dst_module.bias:copy(src_module.bias)
            end
        end
    end
end
