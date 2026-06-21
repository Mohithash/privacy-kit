-- Same idea as value_override.lua, but the setting is stored as a string
-- and needs converting to a Lua number before setResult() - the hooked
-- method returns a primitive double, so the boxed value passed back has to
-- actually be numeric, not a string.

function after(hookId, param)
    local fake = param:getSetting(hookId)
    if fake == nil then
        return false
    end

    local number = tonumber(fake)
    if number == nil then
        return false
    end

    param:setResult(number)
    return true
end
