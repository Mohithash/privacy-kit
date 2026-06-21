-- Same idea as value_override.lua, for methods returning primitive boolean.
-- The setting is stored as the string "true"/"false" and needs converting
-- to an actual Lua boolean before setResult() - passing the string itself
-- back wouldn't unbox correctly against a boolean-returning method.

function after(hookId, param)
    local fake = param:getSetting(hookId)
    if fake == nil then
        return false
    end

    param:setResult(fake == "true")
    return true
end
