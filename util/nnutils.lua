--[[
Copyright (c) 2014 Google Inc.

See LICENSE file for full terms of limited license.
]]

require "torch"
c = require "trepl.colorize"

function recursive_map(module, field, func, w_map)
    local str = ""
    if module[field] then
        local s_map = w_map[torch.pointer(module[field]:storage())]
        if s_map == nil or s_map[module[field]:storageOffset()] == nil then
            str = str .. torch.typename(module) .. ": "
            str = str .. c.blue(func(module[field]))
            if s_map == nil then
                w_map[torch.pointer(module[field]:storage())] = {}
                s_map = w_map[torch.pointer(module[field]:storage())]
            end
            s_map[module[field]:storageOffset()] = 1
        end
    end
    if module.modules then
        str = str .. torch.typename(module) .. ": "
        str = str .. "["
        for i, submodule in ipairs(module.modules) do
            local submodule_str = recursive_map(submodule, field, func, w_map)
            str = str .. submodule_str
            if i < #module.modules and string.len(submodule_str) > 0 then
                str = str .. " "
            end
        end
        str = str .. "]"
    end

    return str
end

function abs_mean(w)
    return torch.mean(torch.abs(w:clone():float()))
end

function abs_max(w)
    return torch.abs(w:clone():float()):max()
end

-- Build a string of average absolute weight values for the modules in the
-- given network.
function get_weight_norms(module)
    return "Weight norms:\n" .. recursive_map(module, "weight", abs_mean, {}) ..
            "\nWeight max:\n" .. recursive_map(module, "weight", abs_max, {})
end

-- Build a string of average absolute weight gradient values for the modules
-- in the given network.
function get_grad_norms(module)
    return "Weight grad norms:\n" ..
        recursive_map(module, "gradWeight", abs_mean, {}) ..
        "\nWeight grad max:\n" .. recursive_map(module, "gradWeight", abs_max, {})
end

function get_unique_parameters(net)
    local parameters = net:parameters()
    local unique_params = {}
    local storages = {}
    for k = 1,#parameters do
        local param = parameters[k]
        local storageKey = torch.pointer(param:storage())
        local offset = param:storageOffset()
        if storages[storageKey] then
            if not storages[storageKey][offset] then
                storages[storageKey][offset] = param
                table.insert(unique_params, param)
            end
        else
            storages[storageKey] = {}
            storages[storageKey][offset] = param
            table.insert(unique_params, param)
        end
    end
    return unique_params
end

function share_weights(src, dst)
    local src_params = get_unique_parameters(src)
    local dst_params = get_unique_parameters(dst)
    local dst_total_params = dst:parameters()
    assert(#src_params == #dst_params, string.format("#src_param (%d) != #dst_param (%d)", 
            #src_params, #dst_params))
    local check = {}

    for i=1,#dst_params do
        local dst_param = dst_params[i]
        for j=1,#src_params do
            local src_param = src_params[j]
            local copied = false
            if not check[j] and dst_param:dim() == src_param:dim() and 
                    dst_param:nElement() == src_param:nElement() then
                for k=1,dst_param:dim() do
                    if dst_param:size()[k] ~= src_param:size()[k] then
                        break
                    end
                    if k == dst_param:dim() then
                        local key = torch.pointer(dst_param:storage())
                        local off = dst_param:storageOffset()
                        for l=1,#dst_total_params do
                            local key_l = torch.pointer(dst_total_params[l]:storage())
                            local off_l = dst_total_params[l]:storageOffset()
                            if key_l == key and off_l == off then
                                dst_total_params[l]:set(src_param)
                            end
                        end
                        check[j] = true
                        copied = true
                    end
                end
            end
            if copied then
                assert(src_param:nElement() == dst_param:nElement())
                assert(torch.abs(src_param:norm() - dst_param:norm()) <= 1e-5)
                break
            else
                assert(j < #src_params, "Corresponding weight is not found")
            end
        end
    end
    assert(#check == #src_params)
    -- sanity check
    --[[
    local src_w, src_dw = src:getParameters()
    local dst_w, dst_dw = dst:getParameters()
    assert(src_w:nElement() == dst_w:nElement())
    assert(src_w:norm() == dst_w:norm())
    print(string.format("%d weights are shared", #check))
    --]]
    collectgarbage()
end
